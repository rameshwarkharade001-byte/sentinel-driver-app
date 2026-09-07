package com.sentinel.hudpro;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;

import java.util.Collections;

public class MainActivity extends Activity implements SensorEventListener {

    private TextureView cameraView;
    private HudView hudView;
    private CameraDevice cameraDevice;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Vibrator vibrator;
    private ToneGenerator toneGen;
    private boolean isAlertActive = false;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable telemetryLoop = new Runnable() {
        @Override
        public void run() {
            if (hudView != null) {
                hudView.updateStatus(lastGForce, 68, isAlertActive);
            }
            handler.postDelayed(this, 50); // 20 FPS Smooth Animation Loop
        }
    };

    private float lastGForce = 1.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep Screen Always Awake for Driving
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        toneGen = new ToneGenerator(AudioManager.STREAM_ALARM, 100);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        // 1. Live Native Front Camera Feed
        cameraView = new TextureView(this);
        cameraView.setSurfaceTextureListener(surfaceTextureListener);
        root.addView(cameraView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        // 2. High-Tech Cyber HUD Layer
        hudView = new HudView(this);
        root.addView(hudView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        // 3. Demo Trigger Alert Button (हॅकेथॉन ज्युरींसमोर थेट अलार्म दाखवण्यासाठी)
        Button alertBtn = new Button(this);
        alertBtn.setText("🚨 SIMULATE SLEEP ALERT");
        alertBtn.setTextColor(Color.WHITE);
        alertBtn.setBackgroundColor(Color.parseColor("#CCFF0055"));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        lp.setMargins(0, 0, 0, 45);
        alertBtn.setLayoutParams(lp);
        alertBtn.setPadding(35, 15, 35, 15);
        alertBtn.setOnClickListener(v -> triggerEmergencyAlert());
        root.addView(alertBtn);

        setContentView(root);

        // Request Camera Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 101);
            }
        }

        handler.post(telemetryLoop);
    }

    private void triggerEmergencyAlert() {
        if (isAlertActive) return;
        isAlertActive = true;

        if (vibrator != null) vibrator.vibrate(1200);
        if (toneGen != null) toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 2000);

        handler.postDelayed(() -> isAlertActive = false, 3500);
    }

    private final TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openFrontCamera();
        }
        @Override public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}
        @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) { return false; }
        @Override public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
    };

    private void openFrontCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String targetId = null;
            for (String id : manager.getCameraIdList()) {
                if (manager.getCameraCharacteristics(id).get(android.hardware.camera2.CameraCharacteristics.LENS_FACING)
                        == android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT) {
                    targetId = id;
                    break;
                }
            }
            if (targetId == null && manager.getCameraIdList().length > 0) targetId = manager.getCameraIdList()[0];

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            manager.openCamera(targetId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    cameraDevice = camera;
                    startPreview();
                }
                @Override public void onDisconnected(CameraDevice camera) { camera.close(); }
                @Override public void onError(CameraDevice camera, int error) { camera.close(); }
            }, null);
        } catch (Exception ignored) {}
    }

    private void startPreview() {
        try {
            SurfaceTexture texture = cameraView.getSurfaceTexture();
            Surface surface = new Surface(texture);
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(surface);

            cameraDevice.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.setRepeatingRequest(builder.build(), null, null);
                    } catch (Exception ignored) {}
                }
                @Override public void onConfigureFailed(CameraCaptureSession session) {}
            }, null);
        } catch (Exception ignored) {}
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            lastGForce = (float) Math.sqrt(x * x + y * y + z * z) / 9.8f;

            if (lastGForce > 3.0f) { // Severe Collision / Impact
                triggerEmergencyAlert();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) sensorManager.unregisterListener(this);
        if (cameraDevice != null) cameraDevice.close();
    }
}
