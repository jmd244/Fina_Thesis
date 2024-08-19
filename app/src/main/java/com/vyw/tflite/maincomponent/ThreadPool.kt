package com.vyw.tflite.maincomponent

import java.util.*

class ThreadPool {
    var tasks : Queue<Runnable> = LinkedList()
    @Volatile private var isActive : Boolean = true

    init{
        val mainWorker = Thread {
            while(isActive){
                val newTaskToBeExecuted = tasks.poll()
                newTaskToBeExecuted?.run()
            }
        }
        mainWorker.start()
    }

    fun execute(newTask : Runnable){
        tasks.add(newTask)
    }
}