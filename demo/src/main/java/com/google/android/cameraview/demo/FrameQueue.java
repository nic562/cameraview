package com.google.android.cameraview.demo;

import com.google.android.cameraview.Frame;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Nic on 2018/10/1.
 */
public class FrameQueue extends LinkedBlockingQueue<Frame> {

    private final Object lock = new Object();

    FrameQueue(int size) {
        super(size);
    }

    @Override
    public boolean add(Frame frame) {
        synchronized (lock) {
            if (!offer(frame)){
                // 如果无法加入，则去除队列第一个元素再加入
                poll();
                return offer(frame);
            }
            return true;
        }
    }

    @Override
    public Frame poll() {
        synchronized (lock) {
            return super.poll();
        }
    }
}
