package com.gabriel.util;

import android.app.Application;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Administrator off 2017/5/10/010.
 */
public class Logger {
    private final static String TAG = "Car_Detect";
    private static int LOG_FILE_SAVE_DAYS = 5; // sd卡中日志文件的最多保存天数
    private static String LOG_FILE_SUFFIX = ".txt"; // 输出的日志文件后缀
    private static String LOG_FILE_DIR = "/storage/emulated/0/com.gabriel/"; // 日志文件的路径
    private static DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
    private static DateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static File logFile;
    public static boolean isShowLog = true;

    public static void E(String e) {
        if(isShowLog){
            Log.e(TAG, e);
            writeLogToFile(e);
        }
    }

    public static void V(String v) {
        if(isShowLog){
            Log.v(TAG, v);
            writeLogToFile(v);
        }
    }

    public static void D(String d) {
        if(isShowLog){
            Log.d(TAG, d);
            writeLogToFile(d);
        }
    }

    public static void I(String i) {
        if(isShowLog){
            Log.i(TAG, i);
            writeLogToFile(i);
        }
    }

    public static void W(String w) {
        if(isShowLog){
            Log.w(TAG, w);
            writeLogToFile(w);
        }
    }

    static {
        String fileName = dateFormat.format(System.currentTimeMillis());

        File logDir = new File(LOG_FILE_DIR);// 如果没有log文件夹则新建该文件夹
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        logFile = new File(LOG_FILE_DIR, fileName + LOG_FILE_SUFFIX);
        try {
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            I("log file: " + logFile.getName() + " exists " + logFile.exists());
        }catch (Exception e){
            E("log file create exception: " + e.getMessage());
        }
    }

    /**
     * 日志写入本地文件
     *
     * @return
     * **/
    private static void writeLogToFile(String logContent) {
        String needWriteMessage = timeFormat.format(System.currentTimeMillis()) + "   " + logContent;
        try{
            FileWriter filerWriter = new FileWriter(logFile, true);// 后面这个参数代表是不是要接上文件中原来的数据，不进行覆盖
            BufferedWriter bufWriter = new BufferedWriter(filerWriter);
            bufWriter.write(needWriteMessage);
            bufWriter.newLine();
            bufWriter.close();
            filerWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 删除5天以前的日志文件
     */
    public static void deleteLogFile(){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - LOG_FILE_SAVE_DAYS);
        String oldDate = dateFormat.format(calendar.getTime());
        I("oldDate: " + oldDate);

        File logFileDir = new File(LOG_FILE_DIR);
        File[] files = logFileDir.listFiles();
        for(int i=0; i<files.length; i++){
            //如果5天前日期大于文件名称日期，则删除旧文件
            int logFileName = -1;
            try {
                logFileName = Integer.parseInt(files[i].getName().replace(".txt", ""));
            }catch (Exception e){
                //文件目录下非日志文件，文件名转为数字抛出异常，不做处理
            }
            if(logFileName > 0 && Integer.parseInt(oldDate) > logFileName){
                files[i].delete();
                I("delete old log file: " + files[i].getName());
            }
        }
    }



}
