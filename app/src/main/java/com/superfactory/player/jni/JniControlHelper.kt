package com.superfactory.player.jni

/**
 * Created by vicky on 2017.11.08.
 */

class JniControlHelper private constructor() {

    fun release() {
        JniControlHelper.mHelper = null
    }

    external fun stringFromJNI(): String

    companion object {
        var mHelper: JniControlHelper? = null;
        fun getHelper(): JniControlHelper {
            if (mHelper == null) {
                mHelper = JniControlHelper()
            }
            return mHelper as JniControlHelper
        }

        init {
            System.loadLibrary("native-lib")
        }

        /**
         * A native method that is implemented by the 'native-lib' native library,
         * which is packaged with this application.
         */
    }
}