package xyz.prathamgandhi.cameratest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.Image;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import xyz.prathamgandhi.cameratest.APIInterface.RetrofitRegisterAPI;
import xyz.prathamgandhi.cameratest.models.RegisterModel;


public class RegisterPhoto extends AppCompatActivity {

    public static final String TAG = "RegisterPhoto Activity";
    public static Retrofit retrofit = null;
    PreviewView previewView;
    ExecutorService cameraExecutor;
    ImageCapture imageCapture;
    ProcessCameraProvider cameraProvider;

    FaceDetector faceDetector;

    DrawBoundingBox drawBoundingBox;

    RelativeLayout back_dim_layout;

    Button capturePhoto;
    Bundle extras;
    PopupWindow popupWindow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_photo);

        extras = getIntent().getExtras();



        back_dim_layout = findViewById(R.id.bac_dim_layout);


        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setContourMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                        .build();

        faceDetector = FaceDetection.getClient(options);

        drawBoundingBox = findViewById(R.id.register_bounding_box);
        previewView = findViewById(R.id.viewFinder);

        cameraExecutor = Executors.newSingleThreadExecutor();


        startCamera();

        capturePhoto = findViewById(R.id.capturePhoto);

        capturePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(imageCapture == null) {
                    Toast.makeText(RegisterPhoto.this, "failed...", Toast.LENGTH_SHORT).show();
                    return;
                }

                imageCapture.takePicture(ContextCompat.getMainExecutor(RegisterPhoto.this), new ImageCapture.OnImageCapturedCallback() {
                    @SuppressLint("UnsafeOptInUsageError")
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy image) {
                        super.onCaptureSuccess(image);
                        Bitmap bmp = toBitmap(image.getImage());
                        Matrix matrix = new Matrix();
                        matrix.postScale(-1, 1, bmp.getWidth() / 2f, bmp.getHeight() / 2f);
                        bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
                        showPopup(bmp);
                        image.close();
                        capturePhoto.setVisibility(View.GONE);
                    }
                });
            }
        });



        Toast.makeText(this, "Take a suitable photograph", Toast.LENGTH_SHORT).show();

    }

    private void showPopup(Bitmap bmp) {
        LayoutInflater inflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.photo_check_popup, null);

        // create the popup window
        FrameLayout frameLayout = findViewById(R.id.frameLayout);

        int width = (int) (frameLayout.getWidth() * 0.8);
        int height = (int) (frameLayout.getHeight());
        boolean focusable = false; // avoids dismissal of popup by clicking outside

        popupWindow = new PopupWindow(popupView, width, height, focusable);

        // show the popup window at the frameLayout view
        popupWindow.setOutsideTouchable(false);
        popupWindow.showAtLocation(frameLayout, Gravity.CENTER, 0, 0);


        ImageView imageView = popupView.findViewById(R.id.checkImage);
        imageView.setImageBitmap(bmp);

        AppCompatButton rejectButton = popupView.findViewById(R.id.reject_button);
        AppCompatButton acceptButton = popupView.findViewById(R.id.accept_button);
        rejectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupWindow.dismiss();
                capturePhoto.setVisibility(View.VISIBLE);
            }
        });

        acceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ByteArrayOutputStream Baos = new ByteArrayOutputStream();
                        bmp.compress(Bitmap.CompressFormat.JPEG, 50, Baos);
                        byte[] byteArray = Baos.toByteArray();
                        String base64Image = Base64.encodeToString(byteArray, Base64.DEFAULT);
                        uploadRegisterData(base64Image, extras);
                    }
                });
                thread.start();
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                popupWindow.dismiss();

            }
        });
    }

    private void uploadRegisterData(String imageBase64, Bundle extras){
        Retrofit retrofit = new Retrofit.Builder().baseUrl(getString(R.string.BASE_URL)).addConverterFactory(GsonConverterFactory.create()).build();

        RetrofitRegisterAPI registerAPI = retrofit.create(RetrofitRegisterAPI.class);

        RegisterModel registerModel = new RegisterModel(imageBase64, extras);

        Call<RegisterModel> call = registerAPI.createPost(registerModel);

        for (String key: extras.keySet())
        {
            Log.d ("myApplication", key + " is a key in the bundle");
        }

        call.enqueue(new Callback<RegisterModel>() {
            @Override
            public void onResponse(Call<RegisterModel> call, Response<RegisterModel> response) {
                Toast.makeText(RegisterPhoto.this, "Data added", Toast.LENGTH_SHORT).show();
                if(response.code() == 201) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(RegisterPhoto.this, HomePage.class);
                            startActivity(intent);
                        }
                    });
                }
                else{
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(RegisterPhoto.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                System.out.println(response.code());
            }

            @Override
            public void onFailure(Call<RegisterModel> call, Throwable t) {
                Toast.makeText(RegisterPhoto.this, "Something failed", Toast.LENGTH_SHORT).show();
                System.out.println(t.getMessage());
            }
        });
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

                imageCapture = new ImageCapture.Builder().setTargetRotation(this.getWindowManager().getDefaultDisplay().getRotation()).build();

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
                imageAnalysis.setAnalyzer(cameraExecutor, new BoundingBoxFrameAnalyzer(faceDetector, drawBoundingBox));

                try {
                    cameraProvider.unbindAll();
                    cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imageAnalysis, imageCapture);
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

    private Bitmap toBitmap(Image image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        buffer.rewind();
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        byte[] clonedBytes = bytes.clone();
        return BitmapFactory.decodeByteArray(clonedBytes, 0, clonedBytes.length);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();

        if(popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }

    }

}