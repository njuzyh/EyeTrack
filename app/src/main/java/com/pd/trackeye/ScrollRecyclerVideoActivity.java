package com.pd.trackeye;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.face.FaceDetector;
import com.yc.pagerlib.recycler.OnPagerListener;
import com.yc.pagerlib.recycler.PagerLayoutManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.objdetect.CascadeClassifier;
import org.yczbj.ycvideoplayerlib.manager.VideoPlayerManager;
import org.yczbj.ycvideoplayerlib.player.VideoPlayer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ScrollRecyclerVideoActivity extends CameraActivity implements CameraBridgeViewBase.CvCameraViewListener2{
    private static final String TAG = "OCVSample::Activity";
    private RecyclerView recyclerView;
//    CameraSource cameraSource;
//    protected static Bitmap mBitmap;
//    protected static Bitmap mEyeBitmap;
//    protected static int[] mDebugArray;
//    protected static byte[] mFrameArray;
//    protected static FaceDetector faceDetector = null;
//    //protected static GazeDetector gazeDetector = null;
//    protected int eyeRegionWidth = 120;
//    protected int eyeRegionHeight = 80;
//    protected int mDownSampleScale = 2;
//    protected int mUpSampleScale = 4;
//    protected int mDThresh = 10;
//    protected double mGradThresh = 25.0;
//    protected int iris_pixel = 0;
//    protected int mUpThreshold = 8;
//    protected int mDownThreshold = -4;
//    protected int mLeftThreshold = 6;
//    protected int mRightThreshold = -6;
//    private int direction = -1;
//    private int last_direction = -1;
//    private double curTime = -1;
    private CameraBridgeViewBase mOpenCvCameraView;
    private CascadeClassifier leftClassifier;
    private CascadeClassifier rightClassifier;
    private CascadeClassifier faceClassifier;
    private Mat mRgba; //图像容器
    private Mat mGray;
    private Rect[] leftEyeArray;
    private Rect[] rightEyeArray;
    private Rect[] faceArray;
    private Size m65Size;
    private Size mDefault;
    private Bitmap mBitmap;
    protected static int[] mDebugArray;
    protected int mDThresh = 10;
    protected double mGradThresh = 25.0;
    protected int iris_pixel = 0;
    private int left_iris_x, left_iris_y, right_iris_x, right_iris_y;
    private int pre_left_x, pre_left_y, pre_right_x, pre_right_y;
    private int num = 0;
    private int leftDir = 4, rightDir = 4, bothDir = 4;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.e(TAG, "OpenCV loaded successfully");
                    initFace();
                    initLeftEye();
                    initRightEye();
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected List<CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }

    @Override
    protected void onStop() {
        super.onStop();
        VideoPlayerManager.instance().suspendVideoPlayer();
        //cameraSource.stop();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onBackPressed() {
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        if (VideoPlayerManager.instance().onBackPressed()){
            return;
        }else {
            //销毁页面
            VideoPlayerManager.instance().releaseVideoPlayer();
        }
        super.onBackPressed();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        //从后台切换到前台，当视频暂停时或者缓冲暂停时，调用该方法重新开启视频播放
        VideoPlayerManager.instance().resumeVideoPlayer();
        mOpenCvCameraView.enableView();
//        if (cameraSource != null) {
//            try {
//                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
//                    // TODO: Consider calling
//                    //    ActivityCompat#requestPermissions
//                    // here to request the missing permissions, and then overriding
//                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                    //                                          int[] grantResults)
//                    // to handle the case where the user grants the permission. See the documentation
//                    // for ActivityCompat#requestPermissions for more details.
//                    return;
//                }
//                cameraSource.start();
//            }
//            catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        if (cameraSource!=null) {
//            cameraSource.release();
//        }
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        VideoPlayerManager.instance().releaseVideoPlayer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        VideoPlayerManager.instance().resumeVideoPlayer();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_recyclerview);
        recyclerView = findViewById(R.id.recyclerView);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_view);
        mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mDefault = new Size(mOpenCvCameraView.getWidth(), mOpenCvCameraView.getHeight());
        m65Size = new Size(100, 60);
        requestPermission();
        //createCameraSource();
        initRecyclerView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.e("Camera", "started");
        mRgba = new Mat();
        mGray = new Mat();
    }

    @Override
    public void onCameraViewStopped() {
        Log.e("Camera", "ended");
        mRgba.release();
        mGray.release();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Log.e("Camera", "Start handle images");
        mRgba = inputFrame.rgba(); //RGBA
        mGray = inputFrame.gray(); //单通道灰度图
        //解决  前置摄像头旋转显示问题
        Core.flip(mRgba, mRgba, 1); //旋转,变成镜像

        MatOfRect faces = new MatOfRect();
        faceClassifier.detectMultiScale(mGray, faces, 1.1, 2, 2, m65Size, mDefault);
        faceArray = faces.toArray();
        detectFace(faceArray);

        bothDir = leftDir;
        doAction(bothDir);

        return mRgba;
    }

    private void initRecyclerView() {
        PagerLayoutManager viewPagerLayoutManager = new PagerLayoutManager(
                this, OrientationHelper.VERTICAL);
        List<Video> list = new ArrayList<>();
        for (int a = 0; a < DataProvider.VideoPlayerList.length ; a++){
            Video video = new Video(DataProvider.VideoPlayerTitle[a],
                    10,"",DataProvider.VideoPlayerList[a]);
            list.add(video);
        }
        VideoAdapter mAdapter = new VideoAdapter(this, list);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(viewPagerLayoutManager);
        viewPagerLayoutManager.setOnViewPagerListener(new OnPagerListener() {
            @Override
            public void onInitComplete() {
                Log.e("OnPagerListener", "onInitComplete--"+"初始化完成");
            }

            @Override
            public void onPageRelease(boolean isNext, int position) {
                Log.e("OnPagerListener", "onPageRelease--"+position+"-----"+isNext);
            }

            @Override
            public void onPageSelected(int position, boolean isBottom) {
                Log.e("OnPagerListener", "onPageSelected--" + position + "-----" + isBottom);
            }
        });
        recyclerView.setRecyclerListener(new RecyclerView.RecyclerListener() {
            @Override
            public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
                VideoPlayer videoPlayer = ((VideoAdapter.VideoViewHolder) holder).mVideoPlayer;
                if (videoPlayer == VideoPlayerManager.instance().getCurrentVideoPlayer()) {
                    VideoPlayerManager.instance().releaseVideoPlayer();
                }
            }
        });
    }

    private void requestPermission() {
        int permission = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // 请求权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},10);
        }
        permission = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }
    }

    private void initLeftEye() {
        try {
            InputStream is = getResources().openRawResource(R.raw.haarcascade_lefteye_2splits);
//            InputStream is = getResources().openRawResource(R.raw.haarcascade_eye_tree_eyeglasses);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "haarcascade_lefteye_2splits.xml");
//            File mCascadeFile = new File(cascadeDir, "haarcascade_eye_tree_eyeglasses.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();
            leftClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
            if (leftClassifier.empty()) {
                Log.e(TAG, "Failed to load cascade classifier");
                leftClassifier = null;
            } else
                Log.e(TAG, "Loaded left eye cascade classifier from " + mCascadeFile.getAbsolutePath());
            cascadeDir.delete();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
        }
    }

    private void initRightEye() {
        try {
            InputStream is = getResources().openRawResource(R.raw.haarcascade_righteye_2splits);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "haarcascade_righteye_2splits.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();
            rightClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
            if (rightClassifier.empty()) {
                Log.e(TAG, "Failed to load cascade classifier");
                rightClassifier = null;
            } else
                Log.e(TAG, "Loaded left eye cascade classifier from " + mCascadeFile.getAbsolutePath());
            cascadeDir.delete();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
        }
    }

    private void initFace(){
        try {
//            InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_alt_tree);
            InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_default);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
//            File mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_alt_tree.xml");
            File mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_default.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();
            faceClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
            if (faceClassifier.empty()) {
                Log.e(TAG, "Failed to load cascade classifier");
                faceClassifier = null;
            } else
                Log.e(TAG, "Loaded face cascade classifier from " + mCascadeFile.getAbsolutePath());
            cascadeDir.delete();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void detectFace(Rect[] Face) {
        Log.e(TAG, "find Face" + Face.length);
        int i = 1;
        for (Rect rect : Face) {
            if(i > 1)
                break;
            left_iris_x = rect.x;
            left_iris_y = rect.y;
            right_iris_x = rect.x;
            right_iris_y = rect.y;

            rect.height = Double.valueOf(rect.height / 1.8).intValue();
            Mat mat = new Mat(mGray, rect);
            Bitmap bmp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.RGB_565);
            Utils.matToBitmap(mat, bmp);
            mBitmap = bmp;
//            saveImageToGallery(mBitmap);
            //Log.e(TAG, "Face saved successfully ");

            MatOfRect eyes = new MatOfRect();
            leftClassifier.detectMultiScale(mat, eyes, 1.1, 2, 2, m65Size, mDefault);
            leftEyeArray = eyes.toArray();
            detectEye(leftEyeArray, mat, 0);

//            rightClassifier.detectMultiScale(mat, eyes, 1.1, 2, 2, m65Size, mDefault);
//            rightEyeArray = eyes.toArray();
//            detectEye(rightEyeArray, mat, 1);

            i++;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void detectEye(Rect[] EyeArray, Mat mat, int flag) {
        Log.e(TAG, "detect Eyes" + EyeArray.length);
        int i = 1;
        for (Rect rect : EyeArray) {
            if(i > 1)
                break;
            Mat tmp = new Mat(mat, rect);
            Bitmap bmp = Bitmap.createBitmap(tmp.cols(), tmp.rows(), Bitmap.Config.RGB_565);
            Utils.matToBitmap(tmp, bmp);
            mBitmap = bmp;
            iris_pixel = calculateEyeCenter(mBitmap, mGradThresh, mDThresh);
            int x_gaze = iris_pixel % mBitmap.getWidth();
            int y_gaze = iris_pixel / mBitmap.getWidth();
            if (flag == 0)
            {
                left_iris_x += (rect.x + x_gaze);
                left_iris_y += (rect.y + y_gaze);
                Log.e("Eye", "Left Eye saved successfully " + left_iris_x + " " + left_iris_y);
                leftDir = getDirection(left_iris_x, left_iris_y, pre_left_x, pre_left_y);
                pre_left_x = left_iris_x;
                pre_left_y = left_iris_y;
                mBitmap = Bitmap.createBitmap(mGray.width(), mGray.height(), Bitmap.Config.RGB_565);
                Utils.matToBitmap(mGray, mBitmap);
                mBitmap = getIris(mBitmap, left_iris_x, left_iris_y, leftDir);
                saveImageToGallery(mBitmap);
            }
            else {
                right_iris_x += (rect.x + x_gaze);
                right_iris_y += (rect.y + y_gaze);
                Log.e("Eye", "Right Eye saved successfully " + right_iris_x + " " + right_iris_y);
                rightDir = getDirection(right_iris_x, right_iris_y, pre_right_x, pre_right_y);
                pre_right_x = right_iris_x;
                pre_right_y = right_iris_y;
                mBitmap = Bitmap.createBitmap(mGray.width(), mGray.height(), Bitmap.Config.RGB_565);
                Utils.matToBitmap(mGray, mBitmap);
                mBitmap = getIris(mBitmap, right_iris_x, right_iris_y, rightDir);
                saveImageToGallery(mBitmap);
            }
            //getDirection(x_gaze, y_gaze);
            //getEyeCorner();
//            mBitmap = bmp;
//            mBitmap = getIris(mBitmap, x_gaze, y_gaze);
//            saveImageToGallery(mBitmap);
            i++;
        }
    }

    private void doAction(int direction)
    {
        switch (direction)
        {
            case 0:
            {
                if (VideoPlayerManager.instance().getCurrentVideoPlayer().isPlaying())
                    VideoPlayerManager.instance().getCurrentVideoPlayer().pause();
                break;
            }
            case 1:
            {
                if(!VideoPlayerManager.instance().getCurrentVideoPlayer().isPlaying())
                    VideoPlayerManager.instance().getCurrentVideoPlayer().restart();
                break;
            }
            case 2:
            {
                recyclerView.post(new Runnable() {
                    @Override
                    public void run() {
                        recyclerView.scrollBy(0, -recyclerView.getHeight());
                        VideoPlayerManager.instance().resumeVideoPlayer();
                    }
                });
                break;
            }
            case 3:
            {
                recyclerView.post(new Runnable() {
                    @Override
                    public void run() {
                        recyclerView.scrollBy(0, recyclerView.getHeight());
                        VideoPlayerManager.instance().resumeVideoPlayer();
                    }
                });
                break;
            }
        }
    }

    private int getDirection(int x1, int y1, int x2, int y2) {
        if(x1 == x2 && y1 == y2)
            return 4;
        int width = mBitmap.getWidth();
        int height = mBitmap.getHeight();
        int RowThreshold = width / 5;
        int ColThreshold = height / 4;
        int direction;
        if(x2 - x1 > RowThreshold && Math.abs(y1 - y2) <= ColThreshold)
            direction = 0;
        else if(x1 - x2 > RowThreshold && Math.abs(y1 - y2) <= ColThreshold)
            direction = 1;
        else if(y2 - y1 > ColThreshold && Math.abs(x1 - x2) <= RowThreshold)
            direction = 2;
        else if(y1 - y2 > ColThreshold && Math.abs(x1 - x2) <= RowThreshold)
            direction = 3;
        else
            direction = 4;
        return direction;
    }

    private Bitmap getIris(Bitmap bmp, float x, float y, int direction){
        Bitmap grayscale = Bitmap.createBitmap(bmp);
        Canvas c = new Canvas(grayscale);
        Paint paint = new Paint();
        switch (direction)
        {
            case 0:
                paint.setColor(Color.RED);
                break;
            case 1:
                paint.setColor(Color.GREEN);
                break;
            case 2:
                paint.setColor(Color.YELLOW);
                break;
            case 3:
                paint.setColor(Color.BLUE);
                break;
            default:
                paint.setColor(Color.MAGENTA);
                break;
        }
        c.drawBitmap(bmp, new Matrix(), null);
        c.drawCircle(x, y, 5, paint);
        return grayscale;
    }

    private int calculateEyeCenter(Bitmap eyeMap, double gradientThreshold, int d_thresh) {
        // TODO(fyordan): Shouldn't use mImageWidth and mImageHeight, but grayData dimensions.
        // Calculate gradients.
        // Ignore edges of image to not deal with boundaries.

        Log.e("CalculateEyeCenter", "Well it entered");
        int imageWidth = eyeMap.getWidth();
        int imageHeight = eyeMap.getHeight();
        int[] grayData = new int[imageWidth*imageHeight];
        double[] mags = new double[(imageWidth-2)*(imageHeight-2)];
        Log.e("CalculateEyeCenter", "Size is : " + imageWidth*imageHeight);
        eyeMap.getPixels(grayData, 0, imageWidth, 0, 0, imageWidth, imageHeight);
        double[][] gradients = new double[(imageWidth-2)*(imageHeight-2)][2];
        int k = 0;
        int magCount = 0;
        mDebugArray = new int[(imageWidth-2)*(imageHeight-2)];
        for(int j=1; j < imageHeight-1; j++) {
            for (int i=1; i < imageWidth-1; i++) {
                int n = j*imageWidth + i;
                gradients[k][0] = (grayData[n+1] & 0xff) - (grayData[n] & 0xff);
                gradients[k][1] = (grayData[n + imageWidth] & 0xff) - (grayData[n] & 0xff);
                double mag = Math.sqrt(Math.pow(gradients[k][0],2) + Math.pow(gradients[k][1],2));
                mags[k] = mag;
                mDebugArray[k] = grayData[n];
                if (mag > gradientThreshold) {
                    gradients[k][0] /= mag;
                    gradients[k][1] /= mag;
                    magCount++;
                    mDebugArray[k] = 0xffffffff;
                } else {
                    gradients[k][0] = 0;
                    gradients[k][1] = 0;
                }
                k++;
            }
        }
        Log.e("CalculateEyeCenter", "mags above threshold: " + magCount);
        Log.e("CalculateEyeCenter", "Now we need to iterate through them all again");
        // For all potential centers
        int c_n = gradients.length/2;
        double max_c = 0;
        for (int i=1; i < imageWidth-1; i++) {
            for (int j=1; j < imageHeight-1; j++) {
                int n = j*imageWidth + i;
                int k_left = Math.max(0, i - d_thresh - 1);
                int k_right= Math.min(imageWidth-2, i+d_thresh-1);
                int k_top = Math.max(0, j - d_thresh-1);
                int k_bottom = Math.min(imageHeight-2, j+d_thresh-1);
                double sumC = 0;
                for (int k_h = k_top; k_h < k_bottom; ++k_h) {
                    for (int k_w = k_left; k_w < k_right; ++k_w) {
                        k = k_w + k_h*(imageWidth-2);
                        if ((gradients[k][0] == 0 && gradients[k][1] == 0)) continue;
                        double d_i = k_w - i;
                        double d_j = k_h - j;
                        if (Math.abs(d_i) > d_thresh || Math.abs(d_j) > d_thresh) continue;
                        double mag = Math.sqrt(Math.pow(d_i, 2) + Math.pow(d_j, 2));
                        if (mag > d_thresh) continue;
                        mag = mag == 0 ? 1 : mag;
                        d_i /= mag;
                        d_j /= mag;
                        sumC += Math.pow(d_i * gradients[k][0] + d_j * gradients[k][1], 2);
                    }
                }
                // TODO(fyordan): w_c should be the value in a gaussian filtered graydata
                sumC /= (grayData[n] & 0xff);
                if (sumC > max_c) {
                    c_n = n;
                    max_c = sumC;
                }
            }
        }
        return c_n;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void saveImageToGallery(Bitmap bmp) {
        //生成路径
        @SuppressLint("SdCardPath") String appDir = "/sdcard/DCIM/Camera/";

        //文件名为时间
        long timeStamp = System.currentTimeMillis();
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String sd = sdf.format(new Date(timeStamp));
        String fileName = sd + ".jpg";

        //获取文件
        File file = new File(appDir, fileName);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            //通知系统相册刷新
            this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    Uri.fromFile(new File(file.getPath()))));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

//    public void createCameraSource() {
//        faceDetector = new FaceDetector.Builder(this)
//                .setTrackingEnabled(true)
//                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
//                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
//                .setMode(FaceDetector.FAST_MODE)
//                .build();
//
//        gazeDetector = new GazeDetector(faceDetector);
//
//        gazeDetector.setProcessor(new MultiProcessor.Builder(new FaceTrackerFactory()).build());
//
//        cameraSource = new CameraSource.Builder(this, gazeDetector)
//                .setRequestedPreviewSize(1024, 768)
//                .setFacing(CameraSource.CAMERA_FACING_FRONT)
//                .setRequestedFps(30.0f)
//                .build();
//
//        try {
//            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
//                // TODO: Consider calling
//                //    ActivityCompat#requestPermissions
//                // here to request the missing permissions, and then overriding
//                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                //                                          int[] grantResults)
//                // to handle the case where the user grants the permission. See the documentation
//                // for ActivityCompat#requestPermissions for more details.
//                return;
//            }
//            cameraSource.start();
//        }
//        catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    //This class will use google vision api to detect eyes
//    private class EyesTracker extends Tracker<Face> {
//        private final float OPEN_THRESHOLD = 0.85f;
//        private final float CLOSE_THRESHOLD = 0.15f;
//        private int state = 0;
//
//        private EyesTracker() {
//
//        }
//
//        private Bitmap toGrayscale(Bitmap bmp){
//            Bitmap grayscale = createBitmap(bmp.getWidth(), bmp.getHeight(), Bitmap.Config.ARGB_8888);
//            Canvas c = new Canvas(grayscale);
//            Paint paint = new Paint();
//            ColorMatrix cm = new ColorMatrix();
//            cm.setSaturation(0);
//            ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
//            paint.setColorFilter(f);
//            c.drawBitmap(bmp, 0, 0, paint);
//            return grayscale;
//        }
//
//        private Bitmap getPoint(Bitmap bmp, float x, float y){
//            Bitmap grayscale = createBitmap(bmp.getWidth(), bmp.getHeight(), Bitmap.Config.ARGB_8888);
//            Canvas c = new Canvas(grayscale);
//            Paint paint = new Paint();
//            paint.setColor(Color.RED);
//            c.drawBitmap(bmp, new Matrix(), null);
//            c.drawPoint(x, y, paint);
//            return grayscale;
//        }
//
//        private int calculateEyeCenter(Bitmap eyeMap, double gradientThreshold, int d_thresh) {
//            // TODO(fyordan): Shouldn't use mImageWidth and mImageHeight, but grayData dimensions.
//            // Calculate gradients.
//            // Ignore edges of image to not deal with boundaries.
//
//            Log.e("CalculateEyeCenter", "Well it entered");
//            int imageWidth = eyeMap.getWidth();
//            int imageHeight = eyeMap.getHeight();
//            int[] grayData = new int[imageWidth*imageHeight];
//            double[] mags = new double[(imageWidth-2)*(imageHeight-2)];
//            Log.e("CalculateEyeCenter", "Size is : " + imageWidth*imageHeight);
//            eyeMap.getPixels(grayData, 0, imageWidth, 0, 0, imageWidth, imageHeight);
//            double[][] gradients = new double[(imageWidth-2)*(imageHeight-2)][2];
//            int k = 0;
//            int magCount = 0;
//            mDebugArray = new int[(imageWidth-2)*(imageHeight-2)];
//            for(int j=1; j < imageHeight-1; j++) {
//                for (int i=1; i < imageWidth-1; i++) {
//                    int n = j*imageWidth + i;
//                    gradients[k][0] = (grayData[n+1] & 0xff) - (grayData[n] & 0xff);
//                    gradients[k][1] = (grayData[n + imageWidth] & 0xff) - (grayData[n] & 0xff);
//                    double mag = Math.sqrt(Math.pow(gradients[k][0],2) + Math.pow(gradients[k][1],2));
//                    mags[k] = mag;
//                    mDebugArray[k] = grayData[n];
//                    if (mag > gradientThreshold) {
//                        gradients[k][0] /= mag;
//                        gradients[k][1] /= mag;
//                        magCount++;
//                        mDebugArray[k] = 0xffffffff;
//                    } else {
//                        gradients[k][0] = 0;
//                        gradients[k][1] = 0;
//                    }
//                    k++;
//                }
//            }
//            Log.e("CalculateEyeCenter", "mags above threshold: " + magCount);
//            Log.e("CalculateEyeCenter", "Now we need to iterate through them all again");
//            // For all potential centers
//            int c_n = gradients.length/2;
//            double max_c = 0;
//            for (int i=1; i < imageWidth-1; i++) {
//                for (int j=1; j < imageHeight-1; j++) {
//                    int n = j*imageWidth + i;
//                    int k_left = Math.max(0, i - d_thresh - 1);
//                    int k_right= Math.min(imageWidth-2, i+d_thresh-1);
//                    int k_top = Math.max(0, j - d_thresh-1);
//                    int k_bottom = Math.min(imageHeight-2, j+d_thresh-1);
//                    double sumC = 0;
//                    for (int k_h = k_top; k_h < k_bottom; ++k_h) {
//                        for (int k_w = k_left; k_w < k_right; ++k_w) {
//                            k = k_w + k_h*(imageWidth-2);
//                            if ((gradients[k][0] == 0 && gradients[k][1] == 0)) continue;
//                            double d_i = k_w - i;
//                            double d_j = k_h - j;
//                            if (Math.abs(d_i) > d_thresh || Math.abs(d_j) > d_thresh) continue;
//                            double mag = Math.sqrt(Math.pow(d_i, 2) + Math.pow(d_j, 2));
//                            if (mag > d_thresh) continue;
//                            mag = mag == 0 ? 1 : mag;
//                            d_i /= mag;
//                            d_j /= mag;
//                            sumC += Math.pow(d_i * gradients[k][0] + d_j * gradients[k][1], 2);
//                        }
//                    }
//                    // TODO(fyordan): w_c should be the value in a gaussian filtered graydata
//                    sumC /= (grayData[n] & 0xff);
//                    if (sumC > max_c) {
//                        c_n = n;
//                        max_c = sumC;
//                    }
//                }
//            }
//            return c_n;
//        }
//
//        @RequiresApi(api = Build.VERSION_CODES.N)
//        private void updateFace(Face face) {
//            if (face == null) {
//                return;
//            }
//
//            //mBitmap = getPoint(mBitmap, landmark.getPosition().x, landmark.getPosition().y);
//            saveImageToGallery(mBitmap);
//
//            for (Landmark landmark : face.getLandmarks()) {
//                int landmark_type = landmark.getType();
//
//                if (landmark_type == Landmark.LEFT_EYE) {
//
//                    // TODO(fyordan): These numbers are arbitray, probably should be proportional to face dimensions.
//                    Log.e("eyePos", "left_eye_x" + landmark.getPosition().x + "left_eye_y" + landmark.getPosition().y);
//                    Log.e("bitmap", "width" + mBitmap.getWidth() + " height" + mBitmap.getHeight());
//                    int eye_region_left = (int) landmark.getPosition().x - eyeRegionHeight / 2;
//                    int eye_region_top = (int) landmark.getPosition().y - eyeRegionWidth / 2;
//
//                    saveImageToGallery(mBitmap);
//
//                    if (eye_region_left >= 0 && eye_region_top >= 0 && eye_region_left + eyeRegionHeight <= mBitmap.getHeight() && eye_region_top + eyeRegionWidth <= mBitmap.getWidth()) {
//                        mBitmap = createBitmap(mBitmap,
//                                eye_region_top,
//                                eye_region_left,
//                                eyeRegionWidth, eyeRegionHeight);
//                        saveImageToGallery(mBitmap);
//                        mEyeBitmap = toGrayscale(mBitmap);
//                        mEyeBitmap = createScaledBitmap(mEyeBitmap,
//                                eyeRegionWidth / mDownSampleScale,
//                                eyeRegionHeight / mDownSampleScale,
//                                true);
//                        iris_pixel = calculateEyeCenter(mEyeBitmap, mGradThresh, mDThresh);
//                    }
////                    Log.w("bitmap", "width" + mBitmap.getWidth() + " height" + mBitmap.getHeight());
////                    Log.w("bitmap", "left" + eye_region_left + " top" + eye_region_top);
////                    Log.w("bitmap", "width" + mEyeBitmap.getWidth() + " height" + mEyeBitmap.getHeight());
//                }
//                else if(landmark_type == Landmark.RIGHT_EYE)
//                {
//                    Log.e("eyePos", "right_eye_x" + landmark.getPosition().x + "right_eye_y" + landmark.getPosition().y);
//                    int eye_region_left = (int) landmark.getPosition().x - eyeRegionHeight / 2;
//                    int eye_region_top = (int) landmark.getPosition().y - eyeRegionWidth / 2;
//
//                    saveImageToGallery(mBitmap);
//
//                    if (eye_region_left >= 0 && eye_region_top >= 0 && eye_region_left + eyeRegionHeight <= mBitmap.getHeight() && eye_region_top + eyeRegionWidth <= mBitmap.getWidth()) {
//                        mBitmap = createBitmap(mBitmap,
//                                eye_region_top,
//                                eye_region_left,
//                                eyeRegionWidth, eyeRegionHeight);
//                        saveImageToGallery(mBitmap);
//                        mEyeBitmap = toGrayscale(mBitmap);
//                        mEyeBitmap = createScaledBitmap(mEyeBitmap,
//                                eyeRegionWidth / mDownSampleScale,
//                                eyeRegionHeight / mDownSampleScale,
//                                true);
//                        iris_pixel = calculateEyeCenter(mEyeBitmap, mGradThresh, mDThresh);
//                    }
//                }
//            }
//            if (mEyeBitmap != null) {
//                int x_gaze = iris_pixel % mEyeBitmap.getWidth() - mEyeBitmap.getWidth()/2;
//                int y_gaze = mEyeBitmap.getHeight()/2 - iris_pixel/mEyeBitmap.getWidth();
//                if (x_gaze > mLeftThreshold) {
//                    Log.e("EyeGaze", "Left");
//                    direction = 2;
//                }
//                else if (x_gaze < mRightThreshold) {
//                    Log.e("EyeGaze", "Right");
//                    direction = 3;
//                }
//                else if (y_gaze > mUpThreshold) {
//                    Log.e("EyeGaze", "Up");
//                    direction = 0;
//                }
//                else if (y_gaze < mDownThreshold) {
//                    Log.e("EyeGaze", "Down");
//                    direction = 1;
//                }
//                Log.e("EyePixelVector", "X: " + x_gaze + "  Y: " + y_gaze);
//            }
//        }
//
//        @RequiresApi(api = Build.VERSION_CODES.N)
//        @Override
//        public void onUpdate(Detector.Detections<Face> detections, Face face) {
//            updateFace(face);
//            if(VideoPlayerManager.instance().getCurrentVideoPlayer() == null)
//                return ;
//            if(VideoPlayerManager.instance().getCurrentVideoPlayer().isCompleted())
//            return ;
//            if(curTime == -1)
//                curTime = System.currentTimeMillis();
//            if(last_direction == -1)
//                last_direction = direction;
//            if(direction != last_direction)
//            {
//                curTime = System.currentTimeMillis();
//                last_direction = direction;
//            }
//            else if(System.currentTimeMillis() - curTime > 300)
//            {
//                if(direction == 0)
//                {
//                    recyclerView.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            recyclerView.scrollBy(0, -recyclerView.getHeight());
//                            VideoPlayerManager.instance().resumeVideoPlayer();
//                        }
//                    });
//                }
//                if(direction == 1)
//                {
//                    recyclerView.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            recyclerView.scrollBy(0, recyclerView.getHeight());
//                            VideoPlayerManager.instance().resumeVideoPlayer();
//                        }
//                    });
//                }
//                else if(direction == 2)
//                {
//                    if (VideoPlayerManager.instance().getCurrentVideoPlayer().isPlaying())
//                        VideoPlayerManager.instance().getCurrentVideoPlayer().pause();
//                }
//                else if(direction == 3)
//                {
//                    if(!VideoPlayerManager.instance().getCurrentVideoPlayer().isPlaying())
//                        VideoPlayerManager.instance().getCurrentVideoPlayer().restart();
//                }
//            }
//            //float THRESHOLD = 0.75f;
//            float left = face.getIsLeftEyeOpenProbability();
//            float right = face.getIsRightEyeOpenProbability();
////            if (left > THRESHOLD || right > THRESHOLD) {
////                //Log.i(TAG, "onUpdate: Eyes Detected");
////                //showStatus("Eyes Detected and open, so video continues");
////                if(!VideoPlayerManager.instance().getCurrentVideoPlayer().isPlaying())
////                    VideoPlayerManager.instance().getCurrentVideoPlayer().restart();
////
////            }
////            else {
////                if (VideoPlayerManager.instance().getCurrentVideoPlayer().isPlaying())
////                    VideoPlayerManager.instance().getCurrentVideoPlayer().pause();
////
////                //showStatus("Eyes Detected and closed, so video paused");
////            }
////            switch (state) {
////                case 0:
////                    if ((left > OPEN_THRESHOLD) && (right > OPEN_THRESHOLD)) {
////                        // Both eyes are initially open
////                        state = 1;
////                    }
////                    break;
////
////                case 1:
////                    if ((left < CLOSE_THRESHOLD) && (right < CLOSE_THRESHOLD)) {
////                        // Both eyes become closed
////                        state = 2;
////                    }
////                    break;
////
////                case 2:
////                    if ((left > OPEN_THRESHOLD) && (right > OPEN_THRESHOLD)) {
////                        // Both eyes are open again
////                        Log.i("BlinkTracker", "blink occurred!");
////                        if(!VideoPlayerManager.instance().getCurrentVideoPlayer().isPlaying())
////                            VideoPlayerManager.instance().getCurrentVideoPlayer().restart();
////                        else
////                            VideoPlayerManager.instance().getCurrentVideoPlayer().pause();
////                        state = 0;
////                    }
////                    break;
////            }
//        }
//
//        @Override
//        public void onMissing(Detector.Detections<Face> detections) {
//            super.onMissing(detections);
//            //showStatus("Face Not Detected yet!");
//        }
//
//        @Override
//        public void onDone() {
//            super.onDone();
//        }
//    }
//
//    private class FaceTrackerFactory implements MultiProcessor.Factory<Face> {
//
//        private FaceTrackerFactory() {
//
//        }
//
//        @Override
//        public Tracker<Face> create(Face face) {
//            return new EyesTracker();
//        }
//    }
//
//    private static class GazeDetector extends Detector<Face> {
//        private Detector<Face> mDelegate;
//
//        GazeDetector(Detector<Face> delegate) {
//            mDelegate = delegate;
//        }
//
//        public SparseArray<Face> detect(Frame frame) {
//            int w = frame.getMetadata().getWidth();
//            int h = frame.getMetadata().getHeight();
//            YuvImage yuvimage = new YuvImage(frame.getGrayscaleImageData().array(), ImageFormat.NV21, w, h, null);
//            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//            yuvimage.compressToJpeg(
//                    new Rect(0, 0, w, h), 100, baos); // Where 100 is the quality of the generated jpeg
//            mFrameArray = baos.toByteArray();
//            mBitmap = BitmapFactory.decodeByteArray(mFrameArray, 0, mFrameArray.length);
//            //mBitmap.recycle();
//            return mDelegate.detect(frame);
//        }
//
//        public boolean isOperational() {
//            return mDelegate.isOperational();
//        }
//
//        public boolean setFocus(int id) {
//            return mDelegate.setFocus(id);
//        }
//    }
}
