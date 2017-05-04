package facetracker.donlingliang.facetrack;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.IOException;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "FaceKamTracker";
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private static final int REQUEST_STORAGE_PERMISSION = 201;

    private Camera2Source mCamera2Source;
    private CameraSourcePreview mPreview;
    private FaceDetector mPreviewFaceDetector;
    private GraphicOverlay mGraphicOverlay;
    private FaceGraphic mFaceGraphic;
    private Context mContext;

    private boolean wasActivityResumed = false;
    private boolean usingFrontCamera = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mContext = getApplicationContext();
        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);

        if (checkGooglePlayAvailability()) {
            requestPermissionThenOpenCamera();
        }
    }

    private boolean checkGooglePlayAvailability() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(mContext);
        if (resultCode == ConnectionResult.SUCCESS) {
            return true;
        } else {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(MainActivity.this, resultCode, 2404).show();
            }
        }
        return false;
    }

    private void requestPermissionThenOpenCamera() {
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                createCameraSourceFront();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    private void createCameraSourceFront() {
        mPreviewFaceDetector = new FaceDetector.Builder(mContext)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setMode(FaceDetector.ACCURATE_MODE)
                .setProminentFaceOnly(true)
                .setTrackingEnabled(true)
                .build();

        if (mPreviewFaceDetector.isOperational()) {
            mPreviewFaceDetector.setProcessor(new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory()).build());
        } else {
            Toast.makeText(mContext, "FACE DETECTION NOT AVAILABLE", Toast.LENGTH_SHORT).show();
        }

        mCamera2Source = new Camera2Source.Builder(mContext, mPreviewFaceDetector)
                .setFocusMode(Camera2Source.CAMERA_AF_AUTO)
                .setFlashMode(Camera2Source.CAMERA_FLASH_AUTO)
                .setFacing(Camera2Source.CAMERA_FACING_FRONT)
                .build();

        startCameraSource();
    }

    @OnClick(R.id.faceSwitch)
    public void faceSwitch() {
        mGraphicOverlay.clear();
        mFaceGraphic.changeFaceEmojiType(FaceGraphic.FaceEmojiType.DOG);
        mFaceGraphic.postInvalidate();
    }

    @OnClick(R.id.cameraSwitch)
    public void cameraSwitchClicked() {
        createCameraSourceBack();
    }

    private void createCameraSourceBack() {
        mPreviewFaceDetector = new FaceDetector.Builder(mContext)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setMode(FaceDetector.FAST_MODE)
                .setProminentFaceOnly(true)
                .setTrackingEnabled(true)
                .build();

        if (mPreviewFaceDetector.isOperational()) {
            mPreviewFaceDetector.setProcessor(new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory()).build());
        } else {
            Toast.makeText(mContext, "FACE DETECTION NOT AVAILABLE", Toast.LENGTH_SHORT).show();
        }

        mCamera2Source = new Camera2Source.Builder(mContext, mPreviewFaceDetector)
                .setFocusMode(Camera2Source.CAMERA_AF_AUTO)
                .setFlashMode(Camera2Source.CAMERA_FLASH_AUTO)
                .setFacing(Camera2Source.CAMERA_FACING_BACK)
                .build();

        startCameraSource();
    }

    private void startCameraSource() {
        if (mCamera2Source != null) {
            try {
                mPreview.start(mCamera2Source, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source2.", e);
                mCamera2Source.release();
                mCamera2Source = null;
            }
        }
    }

    private void stopCameraSource() {
        mPreview.stop();
    }

    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
        @Override
        public Tracker<Face> create(Face face) {
            return new GraphicFaceTracker(mGraphicOverlay);
        }
    }

    private class GraphicFaceTracker extends Tracker<Face> {
        private GraphicOverlay mOverlay;

        GraphicFaceTracker(GraphicOverlay overlay) {
            mOverlay = overlay;
            mFaceGraphic = new FaceGraphic(overlay, mContext);
        }

        /**
         * Start tracking the detected face instance within the face overlay.
         */
        @Override
        public void onNewItem(int faceId, Face item) {
            mFaceGraphic.setId(faceId);
        }

        /**
         * Update the position/characteristics of the face within the overlay.
         */
        @Override
        public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
            mOverlay.add(mFaceGraphic);
            mFaceGraphic.updateFace(face);
        }

        /**
         * Hide the graphic when the corresponding face was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         */
        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
            mFaceGraphic.goneFace();
            mOverlay.remove(mFaceGraphic);
        }

        /**
         * Called when the face is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         */
        @Override
        public void onDone() {
            mFaceGraphic.goneFace();
            mOverlay.remove(mFaceGraphic);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestPermissionThenOpenCamera();
            } else {
                Toast.makeText(MainActivity.this, "CAMERA PERMISSION REQUIRED", Toast.LENGTH_LONG).show();
                finish();
            }
        }
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestPermissionThenOpenCamera();
            } else {
                Toast.makeText(MainActivity.this, "STORAGE PERMISSION REQUIRED", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (wasActivityResumed) {
            startCameraSource();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        wasActivityResumed = true;
        stopCameraSource();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCameraSource();
        if (mPreviewFaceDetector != null) {
            mPreviewFaceDetector.release();
        }
    }
}


