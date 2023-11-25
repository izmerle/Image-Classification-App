package cs.project.rrabi2;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cs.project.rrabi2.ml.Catbreeds;

public class ClassifyCat extends AppCompatActivity {


    TextView result, confidence;
    ImageView imageView;
    Button saveButton, galleryButton, takePictureButton;
    int imageSize = 224;

    List<String> possibleBreeds;
    String[] catClasses = {"American Curl", "American Shorthair", "Bengal", "British Shorthair",
            "Himalayan Cat", "Persian Cat", "Russian Blue", "Siamese Cat, None"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classifycat);

        result = findViewById(R.id.result);
        confidence = findViewById(R.id.confidence);
        imageView = findViewById(R.id.imageView);
        saveButton = findViewById(R.id.saveButton);
        galleryButton = findViewById(R.id.gallery);
        takePictureButton = findViewById(R.id.takePictureButton);

        possibleBreeds = new ArrayList<>();

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveImageToGallery();
            }
        });
        galleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, 3);
            }
        });

        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onClick(View view) {
                // Launch Camera
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, 1);
                } else {
                    //Request camera permission.
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
                }
            }
        });
    }


    public void classifyImage(Bitmap image) {
        try {
            Catbreeds model = Catbreeds.newInstance(getApplicationContext());

            // Resize the image to match the input size of the model
            Bitmap resizedImage = Bitmap.createScaledBitmap(image, imageSize, imageSize, true);

            // Create input tensor
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            int[] intValues = new int[imageSize * imageSize];
            resizedImage.getPixels(intValues, 0, resizedImage.getWidth(), 0, 0, resizedImage.getWidth(), resizedImage.getHeight());
            int pixel = 0;
            for (int i = 0; i < imageSize; i++) {
                for (int j = 0; j < imageSize; j++) {
                    int val = intValues[pixel++]; //RGB
                    byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255.f));
                    byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255.f));
                    byteBuffer.putFloat((val & 0xFF) * (1.f / 255.f));
                }
            }

            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            Catbreeds.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            // Releases model resources if no longer used.
            model.close();

            float maxConfidence = 0;
            int maxIndex = -1;

            // Find the breed with maximum confidence
            for (int i = 0; i < catClasses.length; i++) {
                float confidence = outputFeature0.getFloatValue(i);
                if (confidence > maxConfidence) {
                    maxConfidence = confidence;
                    maxIndex = i;
                }
            }

            if (maxIndex != catClasses.length - 1) {
                // Update the UI with the classified breed and confidences
                result.setText(catClasses[maxIndex]);

                // Add all breeds with confidence above 0%
                possibleBreeds.clear();
                for (int i = 0; i < catClasses.length; i++) {
                    float confidence = outputFeature0.getFloatValue(i);
                    if (confidence > 0) {
                        possibleBreeds.add(catClasses[i]);
                    }
                }

                StringBuilder confidences = new StringBuilder();
                for (String breed : possibleBreeds) {
                    int breedIndex = getIndexFromClass(breed);
                    float confidence = outputFeature0.getFloatValue(breedIndex);
                    if (confidence > 0 && breedIndex == maxIndex) {
                        confidences.append(breed).append(": ").append(confidence * 100).append("%\n");
                    }
                }
                confidence.setText(confidences.toString());
            } else {
                Toast.makeText(this, "Unable to classify the image as a cat.", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            // Handle the exception here
        }
    }


    // Helper method to get the index of a breed from the classes array
    private int getIndexFromClass(String breed) {
        for (int i = 0; i < catClasses.length; i++) {
            if (catClasses[i].equals(breed)) {
                return i;
            }
        }
        return -1; // Breed not found
    }


     @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            Bitmap thumbnail = ThumbnailUtils.extractThumbnail(photo, 224, 224);
            imageView.setImageBitmap(thumbnail);
            classifyImage(thumbnail);
        }

        if (requestCode == 3 && resultCode == RESULT_OK) {
            Uri selectedImage = data.getData();
            try {
                Bitmap photo = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                Bitmap thumbnail = ThumbnailUtils.extractThumbnail(photo, 224, 224);
                imageView.setImageBitmap(thumbnail);
                classifyImage(thumbnail);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private void saveImageToGallery() {
        // Get the drawable from the ImageView
        Drawable drawable = imageView.getDrawable();
        if (drawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();

            // Save the bitmap to the gallery
            String savedImageURL = MediaStore.Images.Media.insertImage(
                    getContentResolver(),
                    bitmap,
                    "classified_image",
                    "Image classification result"
            );

            if (savedImageURL != null) {
                Toast.makeText(this, "Image saved to gallery", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No image to save", Toast.LENGTH_SHORT).show();
        }
    }
}

