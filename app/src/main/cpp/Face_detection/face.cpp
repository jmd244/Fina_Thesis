#include "face.h"

#include <opencv2/core/core.hpp>
#include <opencv2/calib3d/calib3d.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include "cpu.h"

static inline float intersection_area(const Object& a, const Object& b)
{
    cv::Rect_<float> inter = a.rect & b.rect;
    return inter.area();
}

static void qsort_descent_inplace(std::vector<Object>& faceobjects, int left, int right)
{
    int i = left;
    int j = right;
    float p = faceobjects[(left + right) / 2].score;

    while (i <= j)
    {
        while (faceobjects[i].score > p)
            i++;

        while (faceobjects[j].score < p)
            j--;

        if (i <= j)
        {
            // swap
            std::swap(faceobjects[i], faceobjects[j]);

            i++;
            j--;
        }
    }

#pragma omp parallel sections
    {
#pragma omp section
        {
            if (left < j) qsort_descent_inplace(faceobjects, left, j);
        }
#pragma omp section
        {
            if (i < right) qsort_descent_inplace(faceobjects, i, right);
        }
    }
}

static void qsort_descent_inplace(std::vector<Object>& faceobjects)
{
    if (faceobjects.empty())
        return;

    qsort_descent_inplace(faceobjects, 0, faceobjects.size() - 1);
}

static void nms_sorted_bboxes(const std::vector<Object>& faceobjects, std::vector<int>& picked, float nms_threshold)
{
    picked.clear();

    const int n = faceobjects.size();

    std::vector<float> areas(n);
    for (int i = 0; i < n; i++)
    {
        areas[i] = faceobjects[i].rect.area();
    }

    for (int i = 0; i < n; i++)
    {
        const Object& a = faceobjects[i];

        int keep = 1;
        for (int j = 0; j < (int)picked.size(); j++)
        {
            const Object& b = faceobjects[picked[j]];

            // intersection over union
            float inter_area = intersection_area(a, b);
            float union_area = areas[i] + areas[picked[j]] - inter_area;
            // float IoU = inter_area / union_area
            if (inter_area / union_area > nms_threshold)
                keep = 0;
        }

        if (keep)
            picked.push_back(i);
    }
}

static inline float sigmoid(float x)
{
    return static_cast<float>(1.f / (1.f + exp(-x)));
}

static void generate_proposals(const ncnn::Mat& anchors, int stride, const ncnn::Mat& in_pad, const ncnn::Mat& feat_blob, float prob_threshold, std::vector<Object>& objects, const cv::Mat& rgb)
{
    const int num_grid = feat_blob.h;
    int num_grid_x;
    int num_grid_y;
    if (in_pad.w > in_pad.h)
    {
        num_grid_x = in_pad.w / stride;
        num_grid_y = num_grid / num_grid_x;
    }
    else
    {
        num_grid_y = in_pad.h / stride;
        num_grid_x = num_grid / num_grid_y;
    }

    const int num_class = feat_blob.w - 15;

    const int num_anchors = anchors.w / 2;

    for (int q = 0; q < num_anchors; q++)
    {
        const float anchor_w = anchors[q * 2];
        const float anchor_h = anchors[q * 2 + 1];

        const ncnn::Mat feat = feat_blob.channel(q);

        for (int i = 0; i < num_grid_y; i++)
        {
            for (int j = 0; j < num_grid_x; j++)
            {
                const float* featptr = feat.row(i * num_grid_x + j);

                // find class index with max class score
                int class_index = 0;
                float class_score = -FLT_MAX;
                for (int k = 0; k < num_class; k++)
                {
                    float score = featptr[15 + k];
                    if (score > class_score)
                    {
                        class_index = k;
                        class_score = score;
                    }
                }

                float box_score = featptr[4];

                float confidence = sigmoid(box_score);// *sigmoid(class_score);

                if (confidence >= prob_threshold)
                {
                    float dx = sigmoid(featptr[0]);
                    float dy = sigmoid(featptr[1]);
                    float dw = sigmoid(featptr[2]);
                    float dh = sigmoid(featptr[3]);

                    float pb_cx = (dx * 2.f - 0.5f + j) * stride;
                    float pb_cy = (dy * 2.f - 0.5f + i) * stride;

                    float pb_w = pow(dw * 2.f, 2) * anchor_w;
                    float pb_h = pow(dh * 2.f, 2) * anchor_h;

                    float x0 = pb_cx - pb_w * 0.5f;
                    float y0 = pb_cy - pb_h * 0.5f;
                    float x1 = pb_cx + pb_w * 0.5f;
                    float y1 = pb_cy + pb_h * 0.5f;

                    Object obj;
                    obj.rect.x = x0;
                    obj.rect.y = y0;
                    obj.rect.width = x1 - x0;
                    obj.rect.height = y1 - y0;
                    obj.score = confidence;
                    for (int l = 0; l < 5; l++)
                    {
                        float x = featptr[2 * l + 5] * anchor_w + j * stride;
                        float y = featptr[2 * l + 1 + 5] * anchor_h + i * stride;
                        obj.pts.push_back(cv::Point2f(x, y));
                    }
                    objects.push_back(obj);
                }
            }
        }
    }
}
static float normalize_radians(float angle)
{
    return angle - 2 * M_PI * std::floor((angle - (-M_PI)) / (2 * M_PI));
}

static void compute_rotation(Object& obj)
{
    float x0 = (obj.pts[0].x + obj.pts[1].x) / 2;
    float y0 = (obj.pts[0].y + obj.pts[1].y) / 2;
    float x1 = (obj.pts[3].x + obj.pts[4].x)/2;
    float y1 = (obj.pts[3].y + obj.pts[4].y) / 2;

    float target_angle = M_PI * 0.5f;
    float rotation = target_angle - std::atan2(-(y1 - y0), x1 - x0);

    obj.rotation = normalize_radians(rotation);
}
static void rot_vec(cv::Point2f& vec, float rotation)
{
    float sx = vec.x;
    float sy = vec.y;
    vec.x = sx * std::cos(rotation) - sy * std::sin(rotation);
    vec.y = sx * std::sin(rotation) + sy * std::cos(rotation);
}
static void compute_detect_to_roi(Object& obj, const int& target_size)
{
    float width = obj.rect.width;
    float height = obj.rect.height;
    float palm_cx = obj.rect.x + width * 0.5f;
    float palm_cy = obj.rect.y + height * 0.5f;

    float hand_cx;
    float hand_cy;
    float rotation = obj.rotation;
    float shift_x = 0.0f;
    float shift_y = -0.5f;

    if (rotation == 0.0f)
    {
        hand_cx = palm_cx + (width * shift_x);
        hand_cy = palm_cy + (height * shift_y);
    }
    else
    {
        float dx = (width * shift_x) * std::cos(rotation) -
                   (height * shift_y) * std::sin(rotation);
        float dy = (width * shift_x) * std::sin(rotation) +
                   (height * shift_y) * std::cos(rotation);
        hand_cx = palm_cx + dx;
        hand_cy = palm_cy + dy;
    }

    float long_side = std::max(width, height);
    width = long_side;
    height = long_side;
    float hand_w = width * 1.4f;
    float hand_h = height * 1.4f;

    obj.cx = hand_cx;
    obj.cy = hand_cy;
    obj.w = hand_w;
    obj.h = hand_h;

    float dx = hand_w * 0.5f;
    float dy = hand_h * 0.5f;

    obj.pos[0].x = -dx;  obj.pos[0].y = -dy;
    obj.pos[1].x = +dx;  obj.pos[1].y = -dy;
    obj.pos[2].x = +dx;  obj.pos[2].y = +dy;
    obj.pos[3].x = -dx;  obj.pos[3].y = +dy;

    hand_cx = palm_cx;
    hand_cy = palm_cy;
    for (int i = 0; i < 4; i++)
    {
        rot_vec(obj.pos[i], rotation);
        obj.pos[i].x += hand_cx;
        obj.pos[i].y += hand_cy;
    }
}

int Face::detect(const cv::Mat& rgb, std::vector<Object>& objects,float rectAVG, float prob_threshold, float nms_threshold)
{
    std::chrono::high_resolution_clock::time_point t1 = std::chrono::high_resolution_clock::now();

    int img_w = rgb.cols;
    int img_h = rgb.rows;

    int w = img_w;
    int h = img_h;
    float scale = 1.f;
    if (w > h)
    {
        scale = (float)target_size / w;
        w = target_size;
        h = h * scale;
    }
    else
    {
        scale = (float)target_size / h;
        h = target_size;
        w = w * scale;
    }

    ncnn::Mat in = ncnn::Mat::from_pixels_resize(rgb.data, ncnn::Mat::PIXEL_RGB, img_w, img_h,w, h);

    // pad to target_size rectangle
    int wpad = (w + 31) / 32 * 32 - w;
    int hpad = (h + 31) / 32 * 32 - h;
    ncnn::Mat in_pad;
    ncnn::copy_make_border(in, in_pad, hpad / 2, hpad - hpad / 2, wpad / 2, wpad - wpad / 2, ncnn::BORDER_CONSTANT, 0.f);

    const float norm_vals[3] = {1 / 255.f, 1 / 255.f, 1 / 255.f};
    in_pad.substract_mean_normalize(0, norm_vals);

    ncnn::Extractor ex = blazepalm_net.create_extractor();
    ex.input("data", in_pad);

    std::vector<Object> proposals;

    // stride 8
    {
        ncnn::Mat out;
        ex.extract("stride_8", out);

        ncnn::Mat anchors(6);
        anchors[0] = 5.f;
        anchors[1] = 6.f;
        anchors[2] = 10.f;
        anchors[3] = 13.f;
        anchors[4] = 21.f;
        anchors[5] = 26.f;

        std::vector<Object> objects8;
        generate_proposals(anchors, 8, in, out, prob_threshold, objects8, rgb);

        proposals.insert(proposals.end(), objects8.begin(), objects8.end());
    }

    // stride 16
    {
        ncnn::Mat out;
        ex.extract("stride_16", out);

        ncnn::Mat anchors(6);
        anchors[0] = 55.f;
        anchors[1] = 72.f;
        anchors[2] = 225.f;
        anchors[3] = 304.f;
        anchors[4] = 438.f;
        anchors[5] = 553.f;

        std::vector<Object> objects16;
        generate_proposals(anchors, 16, in, out, prob_threshold, objects16, rgb);

        proposals.insert(proposals.end(), objects16.begin(), objects16.end());
    }

    std::ostringstream oss;
    // sort all proposals by score from highest to lowest
    qsort_descent_inplace(proposals);
    for (int i = 0; i < proposals.size(); ++i) {
        oss << proposals[i].score;
        if (i < i - 1) {
            oss << ", ";  // Add comma separator between elements
        }
    }

    // apply nms with nms_threshold
    std::vector<int> picked;
    nms_sorted_bboxes(proposals, picked, nms_threshold);

    int count = picked.size();

    objects.resize(count);
    for (int i = 0; i < count; i++) {
        objects[i] = proposals[picked[i]];

        // adjust offset to original unpadded
        float x0 = (objects[i].rect.x - (wpad / 2)) / scale;
        float y0 = (objects[i].rect.y - (hpad / 2)) / scale;
        float x1 = (objects[i].rect.x + objects[i].rect.width - (wpad / 2)) / scale;
        float y1 = (objects[i].rect.y + objects[i].rect.height - (hpad / 2)) / scale;
        for (int j = 0; j < 5; j++) {
            float ptx = (objects[i].pts[j].x - (wpad / 2)) / scale;
            float pty = (objects[i].pts[j].y - (hpad / 2)) / scale;
            objects[i].pts[j] = cv::Point2f(ptx, pty);
        }
        // clip
        x0 = std::max(std::min(x0, (float) (img_w - 1)), 0.f);
        y0 = std::max(std::min(y0, (float) (img_h - 1)), 0.f);
        x1 = std::max(std::min(x1, (float) (img_w - 1)), 0.f);
        y1 = std::max(std::min(y1, (float) (img_h - 1)), 0.f);
        objects[i].rect.x = x0;
        objects[i].rect.y = y0;
        objects[i].rect.width = x1 - x0;
        objects[i].rect.height = y1 - y0;

        std::chrono::high_resolution_clock::time_point t2 = std::chrono::high_resolution_clock::now();
        objects[i].fd_ms = t2 - t1;


        if(objects[i].rect.area() < rectAVG - (rectAVG * 0.20)){
            objects.resize(i);
            continue;
        }

        compute_rotation(objects[i]);
        compute_detect_to_roi(objects[i], target_size);
        objects[i].pos[0].x = (objects[i].pos[0].x - (wpad / 2));
        objects[i].pos[0].y = (objects[i].pos[0].y - (hpad / 2));
        objects[i].pos[1].x = (objects[i].pos[1].x - (wpad / 2));
        objects[i].pos[1].y = (objects[i].pos[1].y - (hpad / 2));
        objects[i].pos[2].x = (objects[i].pos[2].x - (wpad / 2));
        objects[i].pos[2].y = (objects[i].pos[2].y - (hpad / 2));
        objects[i].pos[3].x = (objects[i].pos[3].x - (wpad / 2));
        objects[i].pos[3].y = (objects[i].pos[3].y - (hpad / 2));
        cv::Point2f srcPts[4];
        srcPts[0] = objects[i].pos[2];
        srcPts[1] = objects[i].pos[3];
        srcPts[2] = objects[i].pos[0];
        srcPts[3] = objects[i].pos[1];
        cv::Point2f dstPts[4];
        dstPts[0] = cv::Point2f(0, 0);
        dstPts[1] = cv::Point2f(192, 0);
        dstPts[2] = cv::Point2f(192, 192);
        dstPts[3] = cv::Point2f(0, 192);
        cv::Mat trans_mat = cv::getAffineTransform(srcPts, dstPts);
        cv::warpAffine(rgb, objects[i].trans_image, trans_mat, cv::Size(192, 192), 1, 0);
        cv::Mat trans_mat_inv;
        cv::invertAffineTransform(trans_mat, trans_mat_inv);
        t1 = std::chrono::high_resolution_clock::now();
        landmark.detect(objects[i].trans_image, trans_mat_inv, objects[i].skeleton,
                        objects[i].left_eyes, objects[i].right_eyes, objects[i].earleft,
                        objects[i].earright);
        t2 = std::chrono::high_resolution_clock::now();
        objects[i].fl_ms = t2 - t1;
    }

    return 0;
}

Face::Face()
{
    blob_pool_allocator.set_size_compare_ratio(0.f);
    workspace_pool_allocator.set_size_compare_ratio(0.f);
}


int Face::load(AAssetManager* mgr, const char* modeltype, int _target_size, bool use_gpu)
{
    blazepalm_net.clear();
    blob_pool_allocator.clear();
    workspace_pool_allocator.clear();

    ncnn::set_cpu_powersave(2);
    ncnn::set_omp_num_threads(ncnn::get_big_cpu_count());

    blazepalm_net.opt = ncnn::Option();
#if NCNN_VULKAN
    blazepalm_net.opt.use_vulkan_compute = use_gpu;
#endif

    blazepalm_net.opt.num_threads = ncnn::get_big_cpu_count();
    blazepalm_net.opt.blob_allocator = &blob_pool_allocator;
    blazepalm_net.opt.workspace_allocator = &workspace_pool_allocator;

    char parampath[256];
    char modelpath[256];
    sprintf(parampath, "%s.param", modeltype);
    sprintf(modelpath, "%s.bin", modeltype);

    blazepalm_net.load_param(mgr, parampath);
    blazepalm_net.load_model(mgr, modelpath);

    landmark.load(mgr,"Models/face_landmark_with_attention");

    target_size = _target_size;

    return 0;
}

int Face::draw(cv::Mat& rgb, const std::vector<Object>& objects, bool details)
{
    for (int i = 0; i < objects.size(); i++)
    {
        cv::rectangle(rgb, objects[i].rect, cv::Scalar(0, 255, 0));
    }

    return 0;
}