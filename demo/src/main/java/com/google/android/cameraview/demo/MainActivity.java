/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.cameraview.demo;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.cameraview.AspectRatio;
import com.google.android.cameraview.CameraView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;


/**
 * This demo app saves the taken picture to a constant file.
 * $ adb pull /sdcard/Android/data/com.google.android.cameraview.demo/files/Pictures/picture.jpg
 */
public class MainActivity extends AppCompatActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback,
        AspectRatioFragment.Listener {

    private static final String TAG = "MainActivity";

    private static final int REQUEST_CAMERA_PERMISSION = 1;

    private static final String[] permissions = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static final String FRAGMENT_DIALOG = "dialog";

    private static final int[] FLASH_OPTIONS = {
            CameraView.FLASH_AUTO,
            CameraView.FLASH_OFF,
            CameraView.FLASH_ON,
    };

    private static final int[] FLASH_ICONS = {
            R.drawable.ic_flash_auto,
            R.drawable.ic_flash_off,
            R.drawable.ic_flash_on,
    };

    private static final int[] FLASH_TITLES = {
            R.string.flash_auto,
            R.string.flash_off,
            R.string.flash_on,
    };

    private int mCurrentFlash;

    private CameraView mCameraView;

    private Handler mBackgroundHandler;

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.take_picture:
                    if (mCameraView != null) {
                        mCameraView.takePicture();
                    }
                    break;
            }
        }
    };

    private byte[] frame;

    private int frameWidth;
    private int frameHeight;

    private boolean drawFramePause = false;
    private boolean drawFrameRun = false;

    private Runnable frameDrawer = new Runnable() {
        @Override
        public void run() {
            drawFrameRun = true;
            while (drawFrameRun) {
                if(drawFramePause){
                    continue;
                }
                if (frame == null) {
                    continue;
                }
                Log.i(TAG, "start frame process!");
                //// 以下部分逻辑效率很低，时间消耗很高
                try {
                    int w = frameWidth;
                    int h = frameHeight;
                    YuvImage img = new YuvImage(frame, ImageFormat.NV21, w, h, null);
                    ByteArrayOutputStream os = new ByteArrayOutputStream(frame.length);
                    if (!img.compressToJpeg(new Rect(0, 0, w, h), 100, os)){
                        Log.w(TAG, "format frame data failed!!");
                        continue;
                    }
                    byte[] tmp = os.toByteArray();
                    Bitmap bmp = BitmapFactory.decodeByteArray(tmp, 0, tmp.length);
                    drawFrame(bmp);
                    bmp.recycle();
                } catch (Exception e){
                    Log.e(TAG, "draw frame error", e);
                }
                Log.i(TAG, "end frame process!");
            }
            Log.d(TAG, "frame process thread exit!");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCameraView = findViewById(R.id.camera);
        if (mCameraView != null) {

            /*
            * ******************
            * 设置是否单独处理每一帧图像
            */
            mCameraView.setHandleFrame(true);

            mCameraView.addCallback(mCallback);

            Thread frameProcessThread = new Thread(frameDrawer);
            frameProcessThread.start();
        }
        FloatingActionButton fab = findViewById(R.id.take_picture);
        if (fab != null) {
            fab.setOnClickListener(mOnClickListener);
        }
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }
    }


    private boolean checkPermissions(String[] pms) {
        for (String p: pms) {
            if (ContextCompat.checkSelfPermission(this, p)
                    == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

    private boolean shouldShowRequestPermissionRationale(String[] pms) {
        for (String p: pms) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    p)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        drawFramePause = false;
        if (checkPermissions(permissions)) {
            mCameraView.start();
        } else if (shouldShowRequestPermissionRationale(permissions)) {
            ConfirmationDialogFragment
                    .newInstance(R.string.camera_permission_confirmation,
                            permissions,
                            REQUEST_CAMERA_PERMISSION,
                            R.string.camera_permission_not_granted)
                    .show(getSupportFragmentManager(), FRAGMENT_DIALOG);
        } else {
            ActivityCompat.requestPermissions(this, permissions,
                    REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    protected void onPause() {
        mCameraView.stop();
        drawFramePause = true;
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        drawFrameRun = false;
        super.onDestroy();
        if (mBackgroundHandler != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mBackgroundHandler.getLooper().quitSafely();
            } else {
                mBackgroundHandler.getLooper().quit();
            }
            mBackgroundHandler = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION:
                if (permissions.length != grantResults.length) {
                    Toast.makeText(this, "Error on requesting camera permission.", Toast.LENGTH_LONG).show();
                }
                for (int g: grantResults) {
                    if (g != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, R.string.camera_permission_not_granted,
                                Toast.LENGTH_SHORT).show();
                    }
                }
                // No need to start camera here; it is handled by onResume
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.aspect_ratio:
                FragmentManager fragmentManager = getSupportFragmentManager();
                if (mCameraView != null
                        && fragmentManager.findFragmentByTag(FRAGMENT_DIALOG) == null) {
                    final Set<AspectRatio> ratios = mCameraView.getSupportedAspectRatios();
                    final AspectRatio currentRatio = mCameraView.getAspectRatio();
                    AspectRatioFragment.newInstance(ratios, currentRatio)
                            .show(fragmentManager, FRAGMENT_DIALOG);
                }
                return true;
            case R.id.switch_flash:
                if (mCameraView != null) {
                    mCurrentFlash = (mCurrentFlash + 1) % FLASH_OPTIONS.length;
                    item.setTitle(FLASH_TITLES[mCurrentFlash]);
                    item.setIcon(FLASH_ICONS[mCurrentFlash]);
                    mCameraView.setFlash(FLASH_OPTIONS[mCurrentFlash]);
                }
                return true;
            case R.id.switch_camera:
                if (mCameraView != null) {
                    int facing = mCameraView.getFacing();
                    mCameraView.setFacing(facing == CameraView.FACING_FRONT ?
                            CameraView.FACING_BACK : CameraView.FACING_FRONT);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAspectRatioSelected(@NonNull AspectRatio ratio) {
        if (mCameraView != null) {
            Toast.makeText(this, ratio.toString(), Toast.LENGTH_SHORT).show();
            mCameraView.setAspectRatio(ratio);
        }
    }

    private Handler getBackgroundHandler() {
        if (mBackgroundHandler == null) {
            HandlerThread thread = new HandlerThread("background");
            thread.start();
            mBackgroundHandler = new Handler(thread.getLooper());
        }
        return mBackgroundHandler;
    }

    private void drawFrame(Bitmap bmp) {
        if (bmp == null || bmp.isRecycled()) {
            return;
        }

        Log.i(TAG,"start drawFrame on rotation:" + mCameraView.getCameraRotation());
        Canvas canvas = mCameraView.getPreview().lockCanvas();
        Matrix matrix = new Matrix();
        if (mCameraView.getCameraRotation() == 0 || mCameraView.getCameraRotation() == 180){
            matrix.postScale(canvas.getWidth()*1f / bmp.getWidth(), canvas.getHeight()*1f / bmp.getHeight());
            if (mCameraView.getCameraRotation() == 180){
                matrix.postRotate(mCameraView.getCameraRotation());
            }
        } else {
            matrix.postScale(canvas.getWidth()*1f / bmp.getHeight(), canvas.getHeight()*1f / bmp.getWidth());
            matrix.postRotate(mCameraView.getCameraRotation());
        }
        Bitmap newBmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);

        Log.i(TAG, "canvas["+ canvas.getWidth() + "x" + canvas.getHeight() + "] draw frame bitmap size: " +
                bmp.getWidth() + "x" + bmp.getHeight() + " ==> " +
                newBmp.getWidth() + "x" + newBmp.getHeight());
        canvas.drawBitmap(newBmp,
                (canvas.getWidth() - mCameraView.getPreview().getWidth()) * 1f / 2,
                (canvas.getHeight() - mCameraView.getPreview().getHeight()) * 1f / 2,
                null
        );
        drawSomethingOnCanvas(canvas);
        mCameraView.getPreview().unlockCanvasAndPost(canvas);
        Log.i(TAG,"end drawFrame");
    }

    private void drawSomethingOnCanvas(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(Color.YELLOW);
        paint.setAlpha(100);
        paint.setTextSize(60);
        Path path = new Path();
        path.moveTo(400, 200);
        path.lineTo(550, 260);
        path.lineTo(700, 380);
        path.lineTo(780, 460);
        path.lineTo(880, 520);
        path.lineTo(680, 600);
        path.lineTo(500, 660);
        path.lineTo(600, 800);
        canvas.drawTextOnPath("Hello World", path, 100, 50, paint);
    }

    private CameraView.Callback mCallback
            = new CameraView.Callback() {

        @Override
        public void onCameraOpened(CameraView cameraView) {
            Log.d(TAG, "onCameraOpened");
        }

        @Override
        public void onCameraClosed(CameraView cameraView) {
            Log.d(TAG, "onCameraClosed");
        }

        @Override
        public void onCameraFrame(CameraView cameraView, byte[] data, int width, int height) {
            Log.d(TAG, "on frame data: " + data.length + "[" + width + "x" + height + "]");
            if (data.length == 0){
                return;
            }
            frame = data;
            frameWidth = width;
            frameHeight = height;
        }

        @Override
        public void onPreviewStart(CameraView cameraView, int width, int height) {
            Log.d(TAG, "on preview start: " + width + "x" + height);
            frameWidth = width;
            frameHeight = height;
        }

        @Override
        public void onCameraError(CameraView cameraView, int error) {
            Log.e(TAG, "camera error: "+ error);
        }

        @Override
        public void onPictureTaken(final CameraView cameraView, final byte[] data) {
            Log.d(TAG, "onPictureTaken " + data.length);
            final File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    System.currentTimeMillis() + ".jpg");
            Log.i(TAG, "taken picture saving to " + file.getAbsolutePath());
            Toast.makeText(cameraView.getContext(), getString(R.string.picture_taken) + ":" + file.getAbsolutePath(), Toast.LENGTH_SHORT)
                    .show();
            getBackgroundHandler().post(new Runnable() {
                @Override
                public void run() {

                    OutputStream os = null;
                    try {
                        os = new FileOutputStream(file);

                        if(cameraView.isHandleFrame()) {
                            // drawing something for result image
                            Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length)
                                    .copy(Bitmap.Config.ARGB_8888, true);
                            if (mCameraView.getCameraRotation() != 0) {
                                Matrix matrix = new Matrix();
                                matrix.postRotate(mCameraView.getCameraRotation());
                                Bitmap newBmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
                                bmp.recycle();
                                bmp = newBmp;
                            }
                            Canvas canvas = new Canvas(bmp);
                            drawSomethingOnCanvas(canvas);
                            // draw end!
                            bmp.compress(Bitmap.CompressFormat.JPEG, 100, os);
                            bmp.recycle();
                        } else {
                            os.write(data);
                        }

                    } catch (IOException e) {
                        Log.w(TAG, "Cannot write to " + file, e);
                    } finally {
                        if (os != null) {
                            try {
                                os.close();
                            } catch (IOException e) {
                                // Ignore
                            }
                        }
                    }
                }
            });
        }

    };

    public static class ConfirmationDialogFragment extends DialogFragment {

        private static final String ARG_MESSAGE = "message";
        private static final String ARG_PERMISSIONS = "permissions";
        private static final String ARG_REQUEST_CODE = "request_code";
        private static final String ARG_NOT_GRANTED_MESSAGE = "not_granted_message";

        public static ConfirmationDialogFragment newInstance(@StringRes int message,
                String[] permissions, int requestCode, @StringRes int notGrantedMessage) {
            ConfirmationDialogFragment fragment = new ConfirmationDialogFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_MESSAGE, message);
            args.putStringArray(ARG_PERMISSIONS, permissions);
            args.putInt(ARG_REQUEST_CODE, requestCode);
            args.putInt(ARG_NOT_GRANTED_MESSAGE, notGrantedMessage);
            fragment.setArguments(args);
            return fragment;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Bundle args = getArguments();
            return new AlertDialog.Builder(getActivity())
                    .setMessage(args.getInt(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String[] permissions = args.getStringArray(ARG_PERMISSIONS);
                                    if (permissions == null) {
                                        throw new IllegalArgumentException();
                                    }
                                    ActivityCompat.requestPermissions(getActivity(),
                                            permissions, args.getInt(ARG_REQUEST_CODE));
                                }
                            })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Toast.makeText(getActivity(),
                                            args.getInt(ARG_NOT_GRANTED_MESSAGE),
                                            Toast.LENGTH_SHORT).show();
                                }
                            })
                    .create();
        }

    }

}
