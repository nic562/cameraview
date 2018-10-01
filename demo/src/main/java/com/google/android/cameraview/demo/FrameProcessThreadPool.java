package com.google.android.cameraview.demo;

import java.util.ArrayList;

/**
 * Created by Nic on 2018/10/1.
 */
public class FrameProcessThreadPool {
    private ArrayList<Thread> threads;

    private FrameProcessRunnable runnable;

    private boolean isRunning = false;

    FrameProcessThreadPool(int size, FrameProcessRunnable runnable) {
        threads = new ArrayList<>();
        this.runnable = runnable;

        for (int i =0; i< size; i++){
            threads.add(new Thread(runnable, "frameProcessThread_"+i));
        }
    }

    public void start(){
        if (!isRunning){
            for (Thread t: threads){
                t.start();
            }
            isRunning = true;
        }
    }

    public void stop(){
        runnable.stop();
    }

    public void pause(){
        runnable.pause();
    }

    public void resume(){
        runnable.resume();
    }
}
