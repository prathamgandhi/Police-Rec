package xyz.prathamgandhi.cameratest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import xyz.prathamgandhi.cameratest.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private final int REQUEST_CODE_PERMISSIONS = 1001;
    private final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";
    private final String[] REQUIRED_PERMISSIONS = new String[]{
        "android.permission.CAMERA",
            "android.permission.WRITE_EXTERNAL_STORAGE"
    };
    public static final String TAG = "MyActivity";
    PreviewView previewView;
    ImageView captureImage;
    ExecutorService cameraExecutor;
    ImageCapture imageCapture;
    ProcessCameraProvider cameraProvider;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        previewView = binding.viewfinder;
        System.out.println("Started");

        if(allPermissionsGranted()) {
            startCamera();
        }
        else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        binding.imageCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(imageCapture == null) {
                    Toast.makeText(MainActivity.this, "failed...", Toast.LENGTH_SHORT).show();
                    return;
                }
                String name = new SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis());
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image");

                ImageCapture.OutputFileOptions  outputOptions = new ImageCapture.OutputFileOptions.Builder(getContentResolver(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues).build();
                imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(MainActivity.this), new ImageCapture.OnImageSavedCallback() {

                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Image Saved successfully", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Photo capture failed.. " + exception.getMessage(), exception);
                    }
                });
            }
        });
//        binding.videoCaptureButton.setOnClickListener( takeVideo() );

        cameraExecutor = Executors.newSingleThreadExecutor();

    }

    private boolean allPermissionsGranted() {
        for(String permission : REQUIRED_PERMISSIONS) {
            if(ContextCompat.checkSelfPermission(this.getApplicationContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, permission, Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    private void startCamera() {
        Toast.makeText(this, "Camera started...", Toast.LENGTH_SHORT).show();
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();


                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().setTargetRotation(this.getWindowManager().getDefaultDisplay().getRotation()).build();

                try {
                    cameraProvider.unbindAll();
                    cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imageCapture);
                }
                catch (Exception e){
                    Log.e(TAG, "Use case binding failed", e);
                }


            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        if(imageCapture == null) {
                return;
        }
        String name = new SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image");
        }

        ImageCapture.OutputFileOptions  outputOptions = new ImageCapture.OutputFileOptions.Builder(getContentResolver(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues).build();
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {

            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                String msg = "Photo capture succeeded: " + outputFileResults.getSavedUri();
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                Log.d(TAG, msg);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e(TAG, "Photo capture failed.. " + exception.getMessage(), exception);
            }
        });
    }

    private void takeVideo() {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CODE_PERMISSIONS) {
            if(allPermissionsGranted()) {
                startCamera();
            } else {
                System.out.println("There");
                Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show();
                this.finish();
            }
        }
    }

}