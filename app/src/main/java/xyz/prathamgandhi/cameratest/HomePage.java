package xyz.prathamgandhi.cameratest;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomePage extends AppCompatActivity {

    private static final String TAG = "HomePage";
    BottomNavigationView bottomNavigationView;
    PopupWindow popupWindow;
    HomeFragment homeFragment = new HomeFragment();
    CriminalFragment criminalFragment = new CriminalFragment();
    LostFragment lostFragment = new LostFragment();
    AttendanceFragment attendanceFragment = new AttendanceFragment();

    private final int REQUEST_CODE_PERMISSIONS = 1001;
    private final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";
    private final String[] REQUIRED_PERMISSIONS = new String[]{
            "android.permission.CAMERA",
            "android.permission.WRITE_EXTERNAL_STORAGE"
    };

    PreviewView previewView;
    ImageView captureImage;
    ExecutorService cameraExecutor;
    ImageCapture imageCapture;
    ProcessCameraProvider cameraProvider;

    File myFile;

    private FaceDetector faceDetector;

    DrawBoundingBox drawBoundingBox;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_page);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        cameraExecutor = Executors.newSingleThreadExecutor();

        myFile = new File("Movies/img.png");
        myFile.mkdirs();
        if(allPermissionsGranted()) {

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    showPopup();
                    startCamera();
                }
            }, 100);

        }
        else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.home:
                        getSupportFragmentManager().beginTransaction().replace(R.id.flFragment, homeFragment).commit();
                        return true;
                    case R.id.attendance:
                        getSupportFragmentManager().beginTransaction().replace(R.id.flFragment, attendanceFragment).commit();
                        return true;
                    case R.id.criminals:
                        getSupportFragmentManager().beginTransaction().replace(R.id.flFragment, criminalFragment).commit();
                        return true;
                    case R.id.lost:
                        getSupportFragmentManager().beginTransaction().replace(R.id.flFragment, lostFragment).commit();
                        return true;
                }
                return false;
            }
        });


        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setContourMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                        .build();

        faceDetector = FaceDetection.getClient(options);

    }


    private void showPopup() {
        LayoutInflater inflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_window, null);

        // create the popup window
        int width = 600;
        int height = 1100;
        boolean focusable = false; // avoids dismissal of popup by clicking outside
        PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

        // show the popup window at the fragment frame view
        popupWindow.setOutsideTouchable(false);
        popupWindow.showAtLocation(findViewById(R.id.flFragment), Gravity.CENTER, 0, 0);
        previewView = popupView.findViewById(R.id.viewFinder);
        drawBoundingBox = popupView.findViewById(R.id.boundingbox);

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CODE_PERMISSIONS) {
            if(allPermissionsGranted()) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showPopup();
                        startCamera();
                    }
                }, 300);

            } else {
                Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show();
                this.finish();
            }
        }
    }

    private void startCamera() {
        Toast.makeText(this, "Camera started...", Toast.LENGTH_SHORT).show();
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
                imageAnalysis.setAnalyzer(cameraExecutor, new FrameAnalyzer());

                try {
                    cameraProvider.unbindAll();
                    cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imageAnalysis);
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

    private boolean allPermissionsGranted() {
        for(String permission : REQUIRED_PERMISSIONS) {
            if(ContextCompat.checkSelfPermission(this.getApplicationContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, permission, Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    class FrameAnalyzer implements ImageAnalysis.Analyzer {

        @Override
        public void analyze(@NonNull ImageProxy image) {
            @SuppressLint("UnsafeOptInUsageError") Image mediaImage = image.getImage();
            if(mediaImage != null) {
                InputImage inputImage = InputImage.fromMediaImage(mediaImage, image.getImageInfo().getRotationDegrees());
                Task<List<Face>> result =
                        faceDetector.process(inputImage)
                                .addOnSuccessListener(
                                        new OnSuccessListener<List<Face>>() {
                                            @Override
                                            public void onSuccess(List<Face> faces) {
                                                // Task completed successfully
                                                // ...a
                                                if(faces.isEmpty()) {
                                                    drawBoundingBox.rect = new RectF(0, 0, 0, 0);
                                                    drawBoundingBox.invalidate();
                                                }
                                                for (Face face : faces) {
                                                    Rect bounds = face.getBoundingBox();
//                                                    Bitmap original = Bitmap.createScaledBitmap(toBitmap(mediaImage), previewView.getWidth(), previewView.getHeight(), false);
//                                                    Bitmap faceCrop = Bitmap.createBitmap(original, bounds.left, bounds.top, bounds.width(), bounds.height());
//                                                    try (FileOutputStream out = new FileOutputStream("Movies/img.png")){
//                                                        faceCrop.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
//                                                        Thread.sleep(1000000);
//                                                        // PNG is a lossless format, the compression factor (100) is ignored
//                                                    } catch (IOException | InterruptedException e) {
//                                                        e.printStackTrace();
//                                                    }
                                                    float scaleY = drawBoundingBox.getHeight()/(float)mediaImage.getWidth();
                                                    float scaleX = drawBoundingBox.getWidth()/(float)mediaImage.getHeight();
                                                    drawBoundingBox.rect = new RectF((float) bounds.left * scaleX, (float) bounds.top * scaleY, (float) bounds.right * scaleX, (float) bounds.bottom * scaleY);
                                                    System.out.println(bounds);
                                                    drawBoundingBox.invalidate();
                                                }

                                            }
                                        })
                                .addOnFailureListener(
                                        new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                // Task failed with an exception
                                                // ...
                                            }
                                        });
                result.addOnCompleteListener(results->image.close());

            }
        }
    }
    private Bitmap toBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }
}
