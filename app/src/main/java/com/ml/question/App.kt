package com.ml.question

import android.app.Application

/**
 * Author: Menglong Ma
 * Email: mml2015@126.com
 * Date: 12/21/20 3:24 PM
 * Description: This is App
 * Package: com.ml.question
 * Project: Question
 */
class App:Application() {
    companion object{
        lateinit var context:Application
        private set
    }

    override fun onCreate() {
        super.onCreate()
        context = this
    }
}