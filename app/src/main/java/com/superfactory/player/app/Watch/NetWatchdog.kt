package com.superfactory.player.app.Watch

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkInfo

/**
 * @Description: 网络监听
 */

class NetWatchdog(private val mActivity: Activity) {
    private var mNetChangeListener: NetChangeListener? = null
    private var lastNetStatus: LastNetStatus? = null

    private val mNetIntentFilter = IntentFilter()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            //获取手机的连接服务管理器，这里是连接管理器类
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            val wifiNetworkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
            val mobileNetworkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)

            var wifiState: NetworkInfo.State = NetworkInfo.State.UNKNOWN
            var mobileState: NetworkInfo.State = NetworkInfo.State.UNKNOWN

            if (wifiNetworkInfo != null) {
                wifiState = wifiNetworkInfo.state
            }
            if (mobileNetworkInfo != null) {
                mobileState = mobileNetworkInfo.state
            }

            if (NetworkInfo.State.CONNECTED != wifiState && NetworkInfo.State.CONNECTED == mobileState) {
                if (lastNetStatus != LastNetStatus.Mobile) {
                    lastNetStatus = LastNetStatus.Mobile
                    if (mNetChangeListener != null) {
                        mNetChangeListener!!.onWifiTo4G()
                    }
                }

            } else if (NetworkInfo.State.CONNECTED == wifiState && NetworkInfo.State.CONNECTED != mobileState) {
                if (lastNetStatus != LastNetStatus.Wifi) {
                    lastNetStatus = LastNetStatus.Wifi
                    if (mNetChangeListener != null) {
                        mNetChangeListener!!.on4GToWifi()
                    }
                }
            } else if (NetworkInfo.State.CONNECTED != wifiState && NetworkInfo.State.CONNECTED != mobileState) {
                if (lastNetStatus != LastNetStatus.DisConnect) {
                    lastNetStatus = LastNetStatus.DisConnect
                    if (mNetChangeListener != null) {
                        mNetChangeListener!!.onNetDisconnected()
                    }
                }
            }
        }
    }

    private enum class LastNetStatus {
        DisConnect, Mobile, Wifi
    }

    init {
        mNetIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
    }

    interface NetChangeListener {
        fun onWifiTo4G()

        fun on4GToWifi()

        fun onNetDisconnected()
    }

    fun setNetChangeListener(l: NetChangeListener) {
        mNetChangeListener = l
    }

    fun startWatch() {
        try {
            mActivity.registerReceiver(receiver, mNetIntentFilter)
        } catch (e: Exception) {

        }

    }

    fun stopWatch() {

        try {
            mActivity.unregisterReceiver(receiver)
        } catch (e: Exception) {

        }

    }

}
