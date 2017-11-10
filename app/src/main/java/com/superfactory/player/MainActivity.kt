package com.superfactory.player

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import com.shuyu.gsyvideoplayer.utils.OrientationUtils
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer
import com.shuyu.gsyvideoplayer.video.base.GSYVideoPlayer
import com.superfactory.player.app.Act.BaseActivity
import com.superfactory.player.app.Danmu.DanmakuVideoPlayer
import com.superfactory.player.app.Danmu.SampleListener
import com.superfactory.player.app.Utils.FFmpegUtils
import kotlinx.android.synthetic.main.danmaku_layout.*


/**
 * Created by vicky on 2017.11.09.
 */
class MainActivity : BaseActivity() {
    private var mUrl = "rtmp://live.hkstv.hk.lxdns.com/live/hks"
    //    private var mUrl = "http://player.alicdn.com/video/aliyunmedia.mp4"
    private var orientationUtils: OrientationUtils? = null
    private var isPlay: Boolean = false
    private var isPause: Boolean = false
    private var danmakuVideoPlayer: DanmakuVideoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        danmakuVideoPlayer = findViewById(R.id.gsy_player)

        //使用自定义的全屏切换图片，!!!注意xml布局中也需要设置为一样的
        //必须在setUp之前设置
        danmakuVideoPlayer!!.shrinkImageRes = R.drawable.custom_shrink;
        danmakuVideoPlayer!!.enlargeImageRes = R.drawable.custom_enlarge;

        danmakuVideoPlayer!!.setUp(mUrl, true, null, "测试视频");

        //外部辅助的旋转，帮助全屏
        orientationUtils = OrientationUtils(this, danmakuVideoPlayer)
        //初始化不打开外部的旋转
        orientationUtils!!.isEnable = false


        danmakuVideoPlayer!!.setIsTouchWiget(true)
        //关闭自动旋转
        danmakuVideoPlayer!!.isRotateViewAuto = false
        danmakuVideoPlayer!!.isLockLand = false
        danmakuVideoPlayer!!.isShowFullAnimation = false
        danmakuVideoPlayer!!.isNeedLockFull = true

        //detailPlayer.setOpenPreView(true);
        danmakuVideoPlayer!!.fullscreenButton.setOnClickListener({
            //直接横屏
            orientationUtils!!.resolveByClick()
            //第一个true是否需要隐藏actionbar，第二个true是否需要隐藏statusbar
            danmakuVideoPlayer!!.startWindowFullscreen(this@MainActivity, true, true)
        })

        danmakuVideoPlayer!!.setStandardVideoAllCallBack(object : SampleListener() {
            override fun onPrepared(url: String, vararg objects: Any) {
                super.onPrepared(url, objects)
                //开始播放了才能旋转和全屏
                orientationUtils!!.isEnable = true
                isPlay = true
            }

            override fun onQuitFullscreen(url: String, vararg objects: Any) {
                super.onQuitFullscreen(url, objects)
                if (orientationUtils != null) {
                    orientationUtils!!.backToProtVideo()
                }
            }
        })

        danmakuVideoPlayer!!.setLockClickListener { _, lock ->
            if (orientationUtils != null) {
                //配合下方的onConfigurationChanged
                orientationUtils!!.isEnable = !lock
            }
        }
        danmakuVideoPlayer!!.fetchShot()
    }


    override fun onBackPressed() {

        if (orientationUtils != null) {
            orientationUtils!!.backToProtVideo()
        }

        if (StandardGSYVideoPlayer.backFromWindowFull(this)) {
            return
        }
        super.onBackPressed()
    }


    override fun onPause() {
        getCurPlay().onVideoPause()
        super.onPause()
        isPause = true
    }

    override fun onResume() {
        getCurPlay().onVideoResume()
        super.onResume()
        isPause = false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isPlay) {
            getCurPlay().release()
        }
        //GSYPreViewManager.instance().releaseMediaPlayer();
        if (orientationUtils != null)
            orientationUtils!!.releaseListener()
    }


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        //如果旋转了就全屏
        if (isPlay && !isPause) {
            danmakuVideoPlayer!!.onConfigurationChanged(this, newConfig, orientationUtils)
        }
    }


    private fun resolveNormalVideoUI() {
        //增加title
        danmakuVideoPlayer!!.titleTextView.visibility = View.GONE
        danmakuVideoPlayer!!.backButton.visibility = View.GONE
    }

    private fun getCurPlay(): GSYVideoPlayer {
        return if (danmakuVideoPlayer!!.fullWindowPlayer != null) {
            danmakuVideoPlayer!!.fullWindowPlayer
        } else danmakuVideoPlayer!!
    }

}
