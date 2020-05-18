package com.pd.trackeye;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

public class OpenCVCameraService extends Service implements CameraBridgeViewBase.CvCameraViewListener2 {

    WindowManager wm;
    CameraBridgeViewBase mOpenCvCameraView;
    static final String LOG_TAG="MainService";
    //define which camera is going to be opened
    int mCameraID = JavaCameraView.CAMERA_ID_FRONT;// CAMERA_ID_FRONT && CAMERA_ID_BACK
    //define width and height (320x240) (640x480)
    int mCameraWidth=320;//320 or 640
    int mCameraHeight=240;//240 or 480

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public boolean canOverDrawOtherApps(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context);
    }

    public void openDrawOverPermissionSetting(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            if (!canOverDrawOtherApps(this)) {
                openDrawOverPermissionSetting(this);
            }
        }
        else {
            //TODO Ask your parent activity for providing runtime permission
            Toast.makeText(this, "Camera permission not available", Toast.LENGTH_SHORT).show();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(1, 1,
                Build.VERSION.SDK_INT < Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY :
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                 WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT);

        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);	// gets the window service to diaplay opencv's image output

        mOpenCvCameraView = new JavaCameraView(this, mCameraID);// CAMERA_ID_FRONT CAMERA_ID_BACK);
        ViewGroup.LayoutParams params_cam = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mOpenCvCameraView.setLayoutParams(params_cam);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
//        mOpenCvCameraView.setMinimumHeight(mCameraHeight);
//        mOpenCvCameraView.setMinimumWidth(mCameraWidth);
//        mOpenCvCameraView.setMaxFrameSize(mCameraWidth, mCameraHeight);

        mOpenCvCameraView.enableFpsMeter();
        mOpenCvCameraView.setCvCameraViewListener(this);

        wm.addView(mOpenCvCameraView, params);
        initAsync();

        Log.e(LOG_TAG, "add view");
    }

    @Override
    public void onDestroy()
    {
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        wm.removeViewImmediate(mOpenCvCameraView);
        super.onDestroy();
    }

    public void initAsync()
    {
        if (!OpenCVLoader.initDebug()) {
            Log.e(LOG_TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.e(LOG_TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.e(LOG_TAG, "OpenCV loaded successfully");

                    mOpenCvCameraView.enableView();

                    Log.e(LOG_TAG, "Camera enable successfully");

                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.e(LOG_TAG, "CameraViewStarted");
    }

    @Override
    public void onCameraViewStopped() {
        Log.e(LOG_TAG, "onCameraViewStopped");
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat mRgba = inputFrame.rgba();
        Mat mGray = inputFrame.gray();
        if (mCameraID==JavaCameraView.CAMERA_ID_FRONT) {
            Core.flip(mRgba, mRgba, 1);
            Core.flip(mGray, mGray, 1);
        }
        Log.e(LOG_TAG, "NewFrame");

        //process(mRgba,mGray);

        return mRgba;//display this image.
    }
}
