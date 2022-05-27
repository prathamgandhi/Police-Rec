package xyz.prathamgandhi.cameratest;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.graphics.RectF;

import com.google.mlkit.vision.face.FaceDetector;
import android.media.Image;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;

import java.util.List;

class BoundingBoxFrameAnalyzer implements ImageAnalysis.Analyzer {

    FaceDetector faceDetector;
    DrawBoundingBox drawBoundingBox;

    BoundingBoxFrameAnalyzer(FaceDetector faceDetector, DrawBoundingBox drawBoundingBox) {
            this.faceDetector = faceDetector;
            this.drawBoundingBox = drawBoundingBox;
    }


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

                                                // scaling is required because the image dimension and dimension of camera preview are different
                                                float scaleY = drawBoundingBox.getHeight()/(float)mediaImage.getWidth();
                                                float scaleX = drawBoundingBox.getWidth()/(float)mediaImage.getHeight();

                                                // mirror the coordinates because of use of front camera
                                                float left = drawBoundingBox.getWidth() - (float) bounds.right * scaleX;
                                                float right = drawBoundingBox.getWidth() - (float) bounds.left * scaleX;

                                                //draw the rectangle
                                                drawBoundingBox.rect = new RectF(left, (float) bounds.top * scaleY, right, (float) bounds.bottom * scaleY);
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