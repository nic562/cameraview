package com.google.android.cameraview;

/**
 * Created by Nic on 2018/10/1.
 */
public class Frame {
    public final byte[] data;
    public final int width;
    public final int height;
    Frame(final byte[] d, final int w, final int h){
        data = d;
        width = w;
        height = h;
    }
}
