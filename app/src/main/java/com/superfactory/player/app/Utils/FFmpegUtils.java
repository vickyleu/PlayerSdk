package com.superfactory.player.app.Utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Environment;

import com.shuyu.gsyvideoplayer.utils.Debuger;
import com.superfactory.player.R;
import com.superfactory.player.app.Danmu.DanmakuVideoPlayer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * Created by vicky on 2017.11.10.
 */

public class FFmpegUtils {
    public static void getBitmapsFromVideo(String dataPath) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(dataPath);
        // 取得视频的长度(单位为毫秒)
        String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        // 取得视频的长度(单位为秒)
        int seconds = Integer.valueOf(time) / 1000;
        // 得到每一秒时刻的bitmap比如第一秒,第二秒
        for (int i = 1; i <= seconds; i++) {
            Bitmap bitmap = retriever.getFrameAtTime(i * 1000 * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            String path = Environment.getExternalStorageDirectory() + File.separator + i + ".jpg";
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(path);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static Bitmap getRtmpShot(DanmakuVideoPlayer player, String path) {
        ProcessBuilder builder = new ProcessBuilder();
        int w = player.getWidth();
        int h = player.getHeight();
        String filename = new Md5FileNameGenerator().generate(path);
        builder.command("ffmpeg",
                "-i", path,//流媒体地址
               /* "-y",//覆盖文件,文件已经存在的话，不经提示就覆盖掉*/
               /* "-t","0.001",//设置纪录时间 hh:mm:ss[.xxx]格式的记录时间也支持*/
                "-f", "image2",//以图片格式保存
                "-ss", "0", //从指定的时间(s)开始
                "-vframes", String.format(Locale.CHINA, "%dx%d", w, h),//设置获取1桢的视频
                "-v ",//verbose 啰嗦模式
                "-s", "1"    //指定分辨率,-s参数不写,则输出大小与输入一样
               /* String.format(Locale.CHINA, "tmp/superfactory/PlaySdk/%s.jpg", filename)*/
        );
        builder.redirectErrorStream(false);

        Bitmap rlt=null;
        try {
            Process process = builder.start();
            InputStream in = process.getInputStream();
            Debuger.printfLog("正在进行截图，请稍候=======================");
            if (in.available() > 1) {
                Bitmap bm = BitmapFactory.decodeStream(in);
                if (bm != null) {
                    in.close();
                    rlt=bm;
                    return rlt;
                }
            }
            InputStream errorStream = process.getErrorStream();
            if (errorStream != null && errorStream.read() > 0) {
                rlt=DrawableUtils.drawableToBitmap(player.getContext(), R.drawable.custom_enlarge);
                String str = InputStreamUtil.convertStreamToString(errorStream);
                Debuger.printfLog("错误："+str);

            }
            in.close();
        } catch (IOException e) {
            Debuger.printfLog("错误：");
            e.printStackTrace();
            try {
                rlt=DrawableUtils.drawableToBitmap(player.getContext(), R.drawable.custom_enlarge);
            }catch (Exception ignored){
            }
        }
        return rlt;
    }


}