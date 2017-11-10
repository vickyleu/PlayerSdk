package com.superfactory.player.app.Act

import android.support.v7.app.AppCompatActivity

/**
 * Created by vicky on 2017.11.09.
 */
abstract class BaseActivity : AppCompatActivity() {
    override fun onDestroy() {
        CleanLeakUtils.fixInputMethodManagerLeak(this)
        super.onDestroy()
    }
}
