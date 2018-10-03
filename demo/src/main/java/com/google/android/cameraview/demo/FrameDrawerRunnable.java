package com.google.android.cameraview.demo;

import android.graphics.Bitmap;
import android.util.Log;

/**
 * Created by Nic on 2018/10/2.
 */
public abstract class FrameDrawerRunnable implements Runnable {

    private FrameImageQueue queue;

    private boolean running = true;

    private boolean pause = false;

    private long old = 0;

    private int step = 8;
    private int rest = 2000;
    private int count = 0;

    FrameDrawerRunnable(FrameImageQueue queue) {
        this.queue = queue;
    }

    void stop(){
        running = false;
    }

    void pause(){
        pause = true;
    }

    void resume(){
        pause = false;
    }

    @Override
    public void run() {
        String tag = Thread.currentThread().getName();
        while (running) {
            if (pause) {
                try {
                    Thread.sleep(100);
                    continue;
                } catch (Exception e) {
                    Log.w(tag, "thread sleep error:", e);
                }
            }
            if (count > step){
                try {
                    Thread.sleep(rest);
                    count = 0;
                    continue;
                } catch (Exception e) {
                    Log.w(tag, "thread sleep error:", e);
                }
            }

            FrameImage im = queue.poll();
            if (im == null) {
                continue;
            }
            Log.d(tag, "find valid frame image: " + im.millis);
            if (im.millis > old){
                draw(im.bitmap);
                old = im.millis;
                ++count;
            } else {
                Log.d(tag, "ignore old frame:" + im.millis);
            }
        }
    }

    abstract void draw(Bitmap bitmap);
}
