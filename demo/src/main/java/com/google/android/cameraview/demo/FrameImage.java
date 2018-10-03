package com.google.android.cameraview.demo;

import android.graphics.Bitmap;

/**
 * Created by Nic on 2018/10/2.
 */
public class FrameImage {
    public final long millis;
    public final Bitmap bitmap;

    public FrameImage(long millis, Bitmap bitmap) {
        this.millis = millis;
        this.bitmap = bitmap;
    }
}
