package com.vyw.tflite.onboarding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vyw.tflite.R

class OnboardingItemsAdapter(private val onboardingItems: List<OnboardingItem>) :

    RecyclerView.Adapter<OnboardingItemsAdapter.OnboardingItemViewHolder>()
{
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingItemViewHolder {
        return OnboardingItemViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.itemcontainerfinal ,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: OnboardingItemViewHolder , position: Int) {
        holder.bind(onboardingItems[position])
    }

    override fun getItemCount(): Int {
        return onboardingItems.size
    }

    inner class OnboardingItemViewHolder(view: View): RecyclerView.ViewHolder(view){
        private val imageOnboarding = view.findViewById<ImageView>(R.id.imgonboarding)
        private val textTitle = view.findViewById<TextView>(R.id.texttittlefinal)
        private val textDescription = view.findViewById<TextView>(R.id.textdesc)


        fun bind(onboardingItem: OnboardingItem){
            imageOnboarding.setImageResource(onboardingItem.onboardingimage)
            textTitle.text = onboardingItem.title
            textDescription.text = onboardingItem.description
        }
    }
}
