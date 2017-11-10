package com.superfactory.player.app.Danmu

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.shuyu.gsyvideoplayer.GSYVideoManager
import com.shuyu.gsyvideoplayer.utils.Debuger
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer
import com.shuyu.gsyvideoplayer.video.base.GSYBaseVideoPlayer
import com.shuyu.gsyvideoplayer.video.base.GSYVideoPlayer
import com.superfactory.player.R
import com.superfactory.player.app.Utils.FFmpegUtils
import master.flame.danmaku.controller.IDanmakuView
import master.flame.danmaku.danmaku.loader.IllegalDataException
import master.flame.danmaku.danmaku.loader.android.DanmakuLoaderFactory
import master.flame.danmaku.danmaku.model.BaseDanmaku
import master.flame.danmaku.danmaku.model.DanmakuTimer
import master.flame.danmaku.danmaku.model.IDisplayer
import master.flame.danmaku.danmaku.model.android.DanmakuContext
import master.flame.danmaku.danmaku.model.android.Danmakus
import master.flame.danmaku.danmaku.model.android.SpannedCacheStuffer
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser
import master.flame.danmaku.ui.widget.DanmakuView
import java.io.InputStream
import java.util.*

/**
 * Created by guoshuyu on 2017/2/16.
 *
 *
 * 配置弹幕使用的播放器，目前使用的是本地模拟数据。
 *
 *
 * 模拟数据的弹幕时常比较短，后面的时长点是没有数据的。
 *
 *
 * 注意：这只是一个例子，演示如何集合弹幕，需要完善如弹出输入弹幕等的，可以自行完善。
 * 注意：b站的弹幕so只有v5 v7 x86、没有64，所以记得配置上ndk过滤。
 */

class DanmakuVideoPlayer : StandardGSYVideoPlayer {

    var parser: BaseDanmakuParser? = null
        private set//解析器对象
    var danmakuView: IDanmakuView? = null
        private set//弹幕view
    var danmakuContext: DanmakuContext? = null
        private set

    private var mSendDanmaku: TextView? = null
    private var mToogleDanmaku: TextView? = null

    var danmakuStartSeekPosition: Long = -1

    var danmaKuShow = true

    constructor(context: Context, fullFlag: Boolean?) : super(context, fullFlag!!) {}

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    override fun getLayoutId(): Int {
        return R.layout.danmaku_layout
    }


    override fun init(context: Context) {
        super.init(context)
        danmakuView = findViewById<View>(R.id.danmaku_view) as DanmakuView
        mSendDanmaku = findViewById(R.id.send_danmaku)
        mToogleDanmaku = findViewById(R.id.toogle_danmaku)
        //        mFullscreenButton = findViewById(R.id.fullscreen);

        //        setRotateViewAuto(true);
        //初始化弹幕显示
        initDanmaku()

        mSendDanmaku!!.setOnClickListener(this)
        mToogleDanmaku!!.setOnClickListener(this)


    }

    override fun onPrepared() {
        super.onPrepared()
        onPrepareDanmaku(this)
    }

    override fun onVideoPause() {
        super.onVideoPause()
        if (danmakuView != null && danmakuView!!.isPrepared) {
            danmakuView!!.pause()
        }
    }

    override fun onVideoResume() {
        super.onVideoResume()
        if (danmakuView != null && danmakuView!!.isPrepared && danmakuView!!.isPaused) {
            danmakuView!!.resume()
        }
    }


    override fun onCompletion() {
        releaseDanmaku(this)
    }


    override fun onSeekComplete() {
        super.onSeekComplete()
        val time = mProgressBar.progress * duration / 100
        //如果已经初始化过的，直接seek到对于位置
        if (mHadPlay && danmakuView != null && danmakuView!!.isPrepared) {
            resolveDanmakuSeek(this, time.toLong())
        } else if (mHadPlay && danmakuView != null && !danmakuView!!.isPrepared) {
            //如果没有初始化过的，记录位置等待
            danmakuStartSeekPosition = time.toLong()
        }
    }

    override fun onClick(v: View) {
        super.onClick(v)
        when (v.id) {
            R.id.send_danmaku -> addDanmaku(true)
            R.id.toogle_danmaku -> {
                danmaKuShow = !danmaKuShow
                resolveDanmakuShow()
            }
            R.id.fullscreen -> if (!mIfCurrentIsFullscreen)
                startWindowFullscreen(context, false, true)
            else
                onBackFullscreen()
        }
    }

    /**
     * 处理播放器在全屏切换时，弹幕显示的逻辑
     * 需要格外注意的是，因为全屏和小屏，是切换了播放器，所以需要同步之间的弹幕状态
     */
    override fun startWindowFullscreen(context: Context, actionBar: Boolean, statusBar: Boolean): GSYBaseVideoPlayer? {
        val gsyBaseVideoPlayer = super.startWindowFullscreen(context, actionBar, statusBar)
        //        gsyBaseVideoPlayer.isTouchWigetFull()
        if (gsyBaseVideoPlayer != null) {
            val gsyVideoPlayer = gsyBaseVideoPlayer as DanmakuVideoPlayer
            //对弹幕设置偏移记录
            gsyVideoPlayer.danmakuStartSeekPosition = currentPositionWhenPlaying.toLong()
            gsyVideoPlayer.danmaKuShow = danmaKuShow
            onPrepareDanmaku(gsyVideoPlayer)
        }
        return gsyBaseVideoPlayer
    }

    /**
     * 处理播放器在退出全屏时，弹幕显示的逻辑
     * 需要格外注意的是，因为全屏和小屏，是切换了播放器，所以需要同步之间的弹幕状态
     */
    override fun resolveNormalVideoShow(oldF: View?, vp: ViewGroup, gsyVideoPlayer: GSYVideoPlayer?) {
        super.resolveNormalVideoShow(oldF, vp, gsyVideoPlayer)
        if (gsyVideoPlayer != null) {
            val gsyDanmaVideoPlayer = gsyVideoPlayer as DanmakuVideoPlayer?
            danmaKuShow = gsyDanmaVideoPlayer!!.danmaKuShow
            if (gsyDanmaVideoPlayer.danmakuView != null && gsyDanmaVideoPlayer.danmakuView!!.isPrepared) {
                resolveDanmakuSeek(this, gsyDanmaVideoPlayer.currentPositionWhenPlaying.toLong())
                resolveDanmakuShow()
                releaseDanmaku(gsyDanmaVideoPlayer)
            }
        }
    }


    private fun initDanmaku() {
        // 设置最大显示行数
        val maxLinesPair = HashMap<Int, Int>()
        maxLinesPair.put(BaseDanmaku.TYPE_SCROLL_RL, 5) // 滚动弹幕最大显示5行
        // 设置是否禁止重叠
        val overlappingEnablePair = HashMap<Int, Boolean>()
        overlappingEnablePair.put(BaseDanmaku.TYPE_SCROLL_RL, true)
        overlappingEnablePair.put(BaseDanmaku.TYPE_FIX_TOP, true)

        val danamakuAdapter = DanmakuAdapter(danmakuView)
        danmakuContext = DanmakuContext.create()
        danmakuContext!!.setDanmakuStyle(IDisplayer.DANMAKU_STYLE_STROKEN, (3).toFloat()).setDuplicateMergingEnabled(false).setScrollSpeedFactor(1.2f).setScaleTextSize(1.2f)
                .setCacheStuffer(SpannedCacheStuffer(), danamakuAdapter) // 图文混排使用SpannedCacheStuffer
                .setMaximumLines(maxLinesPair)
                .preventOverlapping(overlappingEnablePair)
        if (danmakuView != null) {
            //todo 替换成你的数据流
            parser = createParser(this.resources.openRawResource(R.raw.comments))
            danmakuView!!.setCallback(object : master.flame.danmaku.controller.DrawHandler.Callback {
                override fun updateTimer(timer: DanmakuTimer) {}

                override fun drawingFinished() {

                }

                override fun danmakuShown(danmaku: BaseDanmaku) {}

                override fun prepared() {
                    if (danmakuView != null) {
                        danmakuView!!.start()
                        if (danmakuStartSeekPosition != (-1).toLong()) {
                            resolveDanmakuSeek(this@DanmakuVideoPlayer, danmakuStartSeekPosition)
                            danmakuStartSeekPosition = -1
                        }
                        resolveDanmakuShow()
                    }
                }
            })
            danmakuView!!.enableDanmakuDrawingCache(true)
        }
    }

    /**
     * 弹幕的显示与关闭
     */
    private fun resolveDanmakuShow() {
        post {
            if (danmaKuShow) {
                if (!danmakuView!!.isShown)
                    danmakuView!!.show()
                mToogleDanmaku!!.text = "弹幕关"
            } else {
                if (danmakuView!!.isShown) {
                    danmakuView!!.hide()
                }
                mToogleDanmaku!!.text = "弹幕开"
            }
        }
    }

    /**
     * 开始播放弹幕
     */
    private fun onPrepareDanmaku(gsyVideoPlayer: DanmakuVideoPlayer) {
        if (gsyVideoPlayer.danmakuView != null && !gsyVideoPlayer.danmakuView!!.isPrepared) {
            gsyVideoPlayer.danmakuView!!.prepare(gsyVideoPlayer.parser,
                    gsyVideoPlayer.danmakuContext)
        }
    }

    /**
     * 弹幕偏移
     */
    private fun resolveDanmakuSeek(gsyVideoPlayer: DanmakuVideoPlayer, time: Long) {
        if (GSYVideoManager.instance().mediaPlayer != null && mHadPlay
                && gsyVideoPlayer.danmakuView != null && gsyVideoPlayer.danmakuView!!.isPrepared) {
            gsyVideoPlayer.danmakuView!!.seekTo(time)
        }
    }

    /**
     * 创建解析器对象，解析输入流
     *
     * @param stream
     * @return
     */
    private fun createParser(stream: InputStream?): BaseDanmakuParser {

        if (stream == null) {
            return object : BaseDanmakuParser() {

                override fun parse(): Danmakus {
                    return Danmakus()
                }
            }
        }

        val loader = DanmakuLoaderFactory.create(DanmakuLoaderFactory.TAG_BILI)

        try {
            loader.load(stream)
        } catch (e: IllegalDataException) {
            e.printStackTrace()
        }

        val parser = BiliDanmukuParser()
        val dataSource = loader.dataSource
        parser.load(dataSource)
        return parser

    }

    /**
     * 释放弹幕控件
     */
    private fun releaseDanmaku(danmakuVideoPlayer: DanmakuVideoPlayer?) {
        if (danmakuVideoPlayer?.danmakuView != null) {
            Debuger.printfError("release Danmaku!")
            danmakuVideoPlayer.danmakuView!!.release()
        }
    }

    /**
     * 模拟添加弹幕数据
     */
    private fun addDanmaku(isLive: Boolean) {
        val danmaku = danmakuContext!!.mDanmakuFactory.createDanmaku(BaseDanmaku.TYPE_SCROLL_RL)
        if (danmaku == null || danmakuView == null) {
            return
        }
        danmaku.text = "这是一条弹幕 " + currentPositionWhenPlaying
        danmaku.padding = 5
        danmaku.priority = 8  // 可能会被各种过滤器过滤并隐藏显示，所以提高等级
        danmaku.isLive = isLive
        danmaku.time = danmakuView!!.currentTime + 500
        danmaku.textSize = 25f * (parser!!.displayer.density - 0.6f)
        danmaku.textColor = Color.RED
        danmaku.textShadowColor = Color.WHITE
        danmaku.borderColor = Color.GREEN
        danmakuView!!.addDanmaku(danmaku)

    }

    /**
     * 抓取快照
     */
    fun fetchShot() {
        val thumbBitmap = FFmpegUtils.getRtmpShot(this,mUrl);
        //增加封面
        val imageView = ImageView(context)
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        imageView.setImageBitmap(thumbBitmap)
        thumbImageView = imageView
    }

}