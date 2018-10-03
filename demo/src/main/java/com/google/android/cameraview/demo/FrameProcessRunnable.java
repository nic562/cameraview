package com.google.android.cameraview.demo;

import android.util.Log;

import com.google.android.cameraview.Frame;

/**
 * Created by Nic on 2018/10/1.
 */
public abstract class FrameProcessRunnable implements Runnable{
    private FrameQueue queue;

    private boolean running = true;

    private boolean pause = false;

    FrameProcessRunnable(FrameQueue queue) {
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
            Log.i(tag, "start frame process!");
            try {
                Frame frame = queue.poll();
                if (frame == null){
                    continue;
                }
                processFrame(frame);
            } catch (Exception e){
                Log.e(tag, "draw frame error", e);
            }
            Log.i(tag, "end frame process!");
        }
    }

    abstract void processFrame(Frame frame);
}
