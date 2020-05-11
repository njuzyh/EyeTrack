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
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class OpenCVCamera extends CameraActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "OCVSample::Activity";

    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean              mIsJavaCamera = true;
    private MenuItem mItemSwitchCamera = null;
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
    private int LeftThreshold, RightThreshold, UpThreshold, DownThreshold;
    private int direction;
    private int left_iris_x, left_iris_y, right_iris_x, right_iris_y;
    private int pre_left_x, pre_left_y, pre_right_x, pre_right_y;

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

    public OpenCVCamera() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.tutorial1_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase)  findViewById(R.id.tutorial1_activity_java_surface_view);

        mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);

        mDefault = new Size(mOpenCvCameraView.getWidth(), mOpenCvCameraView.getHeight());
        m65Size = new Size(100, 60);
        requestPermission();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        Log.e("Camera", "started");
        mRgba = new Mat();
        mGray = new Mat();
    }

    public void onCameraViewStopped() {
        Log.e("Camera", "ended");
        mRgba.release();
        mGray.release();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba(); //RGBA
        mGray = inputFrame.gray(); //单通道灰度图
        //解决  前置摄像头旋转显示问题
        Core.flip(mRgba, mRgba, 1); //旋转,变成镜像

        MatOfRect faces = new MatOfRect();
        faceClassifier.detectMultiScale(mGray, faces, 1.1, 2, 2, m65Size, mDefault);
        faceArray = faces.toArray();
        detectFace(faceArray);

//        rightClassifier.detectMultiScale(mGray, eyes, 1.1, 2, 2, m65Size, mDefault);
//        rightEyeArray = eyes.toArray();
//        detectEye(rightEyeArray, 1);

//        Bitmap bmp = Bitmap.createBitmap(mRgba.width(), mRgba.height(), Bitmap.Config.RGB_565);
//        Utils.matToBitmap(mRgba, bmp);
//        mBitmap = bmp;
//        saveImageToGallery(mBitmap);
//        Log.e("Frame", "OpenCV image saved successfully");
        return mRgba;
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

            rect.height = Double.valueOf(rect.height / 1.5).intValue();
            Mat mat = new Mat(mGray, rect);
            Bitmap bmp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.RGB_565);
            Utils.matToBitmap(mat, bmp);
            mBitmap = bmp;
            //saveImageToGallery(mBitmap);
            //Log.e(TAG, "Face saved successfully ");

            MatOfRect eyes = new MatOfRect();
            leftClassifier.detectMultiScale(mat, eyes, 1.1, 2, 2, m65Size, mDefault);
            leftEyeArray = eyes.toArray();
            detectEye(leftEyeArray, mat, 0);

            rightClassifier.detectMultiScale(mat, eyes, 1.1, 2, 2, m65Size, mDefault);
            rightEyeArray = eyes.toArray();
            detectEye(rightEyeArray, mat, 1);
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
                getDirection(left_iris_x, left_iris_y, pre_left_x, pre_left_y);
                pre_left_x = left_iris_x;
                pre_left_y = left_iris_y;
                mBitmap = Bitmap.createBitmap(mGray.width(), mGray.height(), Bitmap.Config.RGB_565);
                Utils.matToBitmap(mGray, mBitmap);
                mBitmap = getIris(mBitmap, left_iris_x, left_iris_y);
                saveImageToGallery(mBitmap);
            }
            else {
                right_iris_x += (rect.x + x_gaze);
                right_iris_y += (rect.y + y_gaze);
                Log.e("Eye", "Right Eye saved successfully " + right_iris_x + " " + right_iris_y);
                getDirection(right_iris_x, right_iris_y, pre_right_x, pre_right_y);
                pre_right_x = right_iris_x;
                pre_right_y = right_iris_y;
                mBitmap = Bitmap.createBitmap(mGray.width(), mGray.height(), Bitmap.Config.RGB_565);
                Utils.matToBitmap(mGray, mBitmap);
                mBitmap = getIris(mBitmap, right_iris_x, right_iris_y);
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

    private void getEyeCorner() {
        Mat src = new Mat();
        Utils.bitmapToMat(mBitmap, src);//将bitmap转换为Mat
        Imgproc.cvtColor(src, src, Imgproc.COLOR_RGBA2GRAY);//将图像转换为灰度图像
        Imgproc.Canny(src, src, 100, 200);//将图像转换为边缘二值图像
        Mat dst = Mat.zeros(src.size(), CvType.CV_8UC4);//创建黑色背景
        Mat hierarchy = new Mat();
        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(src, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);//轮廓发现
        for (int i = 0; i < contours.size(); i++) {//绘制轮廓
            Imgproc.drawContours(dst, contours, i, new Scalar(255,255,255,255),2,8, hierarchy);
        }
        Utils.matToBitmap(dst, mBitmap);//将Mat转为Bitmap
    }

    private void getDirection(int x1, int y1, int x2, int y2) {
        if(x1 == x2 && y1 == y2)
            return ;
        int width = mBitmap.getWidth();
        int height = mBitmap.getHeight();
        int RowThreshold = width / 4;
        int ColThreshold = height / 3;
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
//        if(x < LeftThreshold)
//            direction = 0;
//        else if(x > RightThreshold)
//            direction = 1;
//        if(y < UpThreshold)
//            direction = 2;
//        else if(y > DownThreshold)
//            direction = 3;
    }

    private Bitmap getIris(Bitmap bmp, float x, float y){
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
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
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
}
