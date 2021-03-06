package com.pd.trackeye;

import android.Manifest;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.androidhiddencamera.HiddenCameraFragment;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import org.opencv.android.OpenCVLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    VideoView videoView;
    //EditText textView;
//    Button start;
//    private HiddenCameraFragment mHiddenCameraFragment;

    //For looking logs
//    ArrayAdapter adapter;
//    ArrayList<String> list = new ArrayList<>();

//    CameraSource cameraSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermission();

        findViewById(R.id.start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ScrollRecyclerVideoActivity.class));
//                if (isServiceRunning("com.pd.trackeye.OpenCVCameraService")) {
//                    Log.e("MainActivity", "have service already");
//                    return;
//                }
//                if (mHiddenCameraFragment != null) {    //Remove fragment from container if present
//                    getSupportFragmentManager()
//                            .beginTransaction()
//                            .remove(mHiddenCameraFragment)
//                            .commit();
//                    mHiddenCameraFragment = null;
//                }
//
//                startService(new Intent(MainActivity.this, BackCameraService.class));
            }
        });

        videoView = findViewById(R.id.videoView);
        //textView = findViewById(R.id.textView);
//        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, list);
        videoView.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.videoplayback));
        videoView.start();
        //createCameraSource();
    }

//    private boolean isServiceRunning(String className) {
//        ActivityManager am = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
//        List<ActivityManager.RunningServiceInfo> serviceInfos=
//                am.getRunningServices(Integer.MAX_VALUE);
//        for (int i = 0; i < serviceInfos.size(); i++) {
//            if (serviceInfos.get(i).service.getClassName().equals(className)) {
////                Log.e("ClassName", serviceInfos.get(i).service.getClassName());
//                return true;
//            }
//        }
//        return false;
//    }

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

    //This class will use google vision api to detect eyes
//    private class EyesTracker extends Tracker<Face> {
//
//        private final float THRESHOLD = 0.75f;
//
//        private EyesTracker() {
//
//        }
//
//        @Override
//        public void onUpdate(Detector.Detections<Face> detections, Face face) {
//            if (face.getIsLeftEyeOpenProbability() > THRESHOLD || face.getIsRightEyeOpenProbability() > THRESHOLD) {
//                Log.i(TAG, "onUpdate: Eyes Detected");
//                //showStatus("Eyes Detected and open, so video continues");
//                if (!videoView.isPlaying())
//                    videoView.start();
//
//            }
//            else {
//                if (videoView.isPlaying())
//                    videoView.pause();
//
//                //showStatus("Eyes Detected and closed, so video paused");
//            }
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

//    public void createCameraSource() {
//        FaceDetector detector = new FaceDetector.Builder(this)
//                .setTrackingEnabled(true)
//                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
//                .setMode(FaceDetector.FAST_MODE)
//                .build();
//        detector.setProcessor(new MultiProcessor.Builder(new FaceTrackerFactory()).build());
//
//        cameraSource = new CameraSource.Builder(this, detector)
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

    @Override
    protected void onResume() {
        super.onResume();
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
    protected void onPause() {
        super.onPause();
//        if (cameraSource!=null) {
//            cameraSource.stop();
//        }
//        if (videoView.isPlaying()) {
//            videoView.pause();
//        }
    }

//    public void showStatus(final String message) {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                textView.setText(message);
//            }
//        });
//    }

    @Override
    protected void onDestroy() {
//        if (mHiddenCameraFragment != null) {    //Remove fragment from container if present
//            getSupportFragmentManager()
//                    .beginTransaction()
//                    .remove(mHiddenCameraFragment)
//                    .commit();
//            mHiddenCameraFragment = null;
//        }
        super.onDestroy();
//        if (cameraSource!=null) {
//            cameraSource.release();
//        }
    }

    @Override
    public void onBackPressed() {
//        if (mHiddenCameraFragment != null) {    //Remove fragment from container if present
//            getSupportFragmentManager()
//                    .beginTransaction()
//                    .remove(mHiddenCameraFragment)
//                    .commit();
//            mHiddenCameraFragment = null;
//        }
        super.onBackPressed();
    }
}