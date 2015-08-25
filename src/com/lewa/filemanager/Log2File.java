//版权所有©2011,盛大网络
//说明：记录程序log的共用模块
package com.lewa.filemanager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import android.content.Context;
import android.os.Environment;

public class Log2File
{
    private static boolean  logInit;
    private static BufferedWriter writer;

    private Log2File()
    {

    }

    /**
     * 初始化Log,创建log文件
     * @param ctx
     * @param fileName
     * @return
     */
    public static boolean init(Context ctx, String fileName)
    {
        if(!logInit)
        {
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state))
            {
                File sdDir = Environment.getExternalStorageDirectory();
                File logDir = new File(sdDir.getAbsolutePath() + "/lewalog/" +
                        ctx.getPackageName() + "/");

                try {
                    if(!logDir.exists())
                    {
                        logDir.mkdirs();
                    }

                    File logFile = new File(logDir, fileName);
                    logFile.createNewFile();

                    writer = new BufferedWriter(new FileWriter(logFile, true));
                    logInit = true;
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }

        }

        return logInit;
    }

    /**
     * 写一条log
     * @param msg
     */
    public static void w(String msg)
    {
        if(logInit)
        {
            try {
                Date date = new Date();
                writer.write("[" + date.toLocaleString() + "] " + msg);
                writer.newLine();
                writer.flush();
            } catch (IOException e) {
                // TODO Auto-generated catch block
            }
        }
    }

    /**
     * 关闭log
     */
    public static void close()
    {
        if(logInit)
        {
            try {
                writer.close();
                writer = null;

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            logInit = false;
        }
    }
}