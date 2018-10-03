package com.google.android.cameraview.demo;

import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Created by Nic on 2018/10/2.
 */
public class FrameImageQueue extends PriorityBlockingQueue<FrameImage> {
    private static Comparator<FrameImage> comparator = new Comparator<FrameImage>() {
        @Override
        public int compare(FrameImage o1, FrameImage o2) {
            return (int) (o1.millis - o2.millis);
        }
    };

    public FrameImageQueue(int size) {
        super(size, comparator);
    }
}
