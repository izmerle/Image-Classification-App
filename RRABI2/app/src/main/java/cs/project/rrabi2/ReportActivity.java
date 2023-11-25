package cs.project.rrabi2;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.InputFilter;
import android.text.method.DigitsKeyListener;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.vishnusivadas.advanced_httpurlconnection.PutData;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;

public class ReportActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private TextView textDate;
    private Button buttonDatePicker, buttonChooseImage;
    TextInputEditText textcontact, textname;
    Spinner locationSpinner, classifiedbreed;
    Button saveButton;
    ImageView imageUpload;
    Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        textname = findViewById(R.id.name);
        textcontact = findViewById(R.id.contact);
        textDate = findViewById(R.id.textDate);
        buttonDatePicker = findViewById(R.id.buttonDatePicker);
        buttonChooseImage = findViewById(R.id.buttonChooseImage);
        saveButton = findViewById(R.id.save_button);
        imageUpload = findViewById(R.id.imageUpload);

        // Set the maximum number of digits allowed for the phone number
        int maxDigits = 11;
        InputFilter[] filters = new InputFilter[1];
        filters[0] = new InputFilter.LengthFilter(maxDigits);
        textcontact.setFilters(filters);

        //Optional: You can also restrict the input to numeric digits only
        textcontact.setKeyListener(DigitsKeyListener.getInstance("0123456789"));

        // Dropdown list for location
        Spinner locationSpinner = findViewById(R.id.locationSpinner);
        CharSequence[] items = getResources().getStringArray(R.array.location_spinner);
        int maxItemsToShow = items.length;

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        locationSpinner.setAdapter(adapter);
        locationSpinner.setSelection(0);
        locationSpinner.setPrompt("Select Location: ");

        // Dropdown list for classified breeds
        Spinner classifiedbreedSpinner = findViewById(R.id.classifiedbreed_spinner);
        CharSequence[] classifiedbreedItems = getResources().getStringArray(R.array.classifiedbreed_spinner);

        ArrayAdapter<CharSequence> classifiedbreedAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, classifiedbreedItems);
        classifiedbreedAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        classifiedbreedSpinner.setAdapter(classifiedbreedAdapter);
        classifiedbreedSpinner.setSelection(0);
        classifiedbreedSpinner.setPrompt("Select Classified Breed: ");

        // Date Picker functionality
        buttonDatePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDatePicker();
            }
        });

        // Image Choose functionality
        buttonChooseImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImageChooser();
            }
        });

        // Save data function
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String name, location, contact, date, classifiedbreed;
                name = String.valueOf(textname.getText());
                location = locationSpinner.getSelectedItem().toString();
                contact = String.valueOf(textcontact.getText());
                date = String.valueOf(textDate.getText());
                classifiedbreed = classifiedbreedSpinner.getSelectedItem().toString();
                String imagePath = getImagePathFromUri(selectedImageUri);

                if (!name.isEmpty() && !location.isEmpty() && !contact.isEmpty() && !date.isEmpty() && !classifiedbreed.isEmpty() && imagePath != null) {
                    Bitmap image = null;
                    try {
                        image = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                        String encodedImage = encodeImageToBase64(image);

                        // Create a new PutData object with the form data and encoded image
                        String[] field = new String[7];
                        field[0] = "name";
                        field[1] = "location";
                        field[2] = "contact";
                        field[3] = "date";
                        field[4] = "classifiedbreed";
                        field[5] = "imagePath";
                        field[6] = "imageData";

                        String[] data = new String[7];
                        data[0] = name;
                        data[1] = location;
                        data[2] = contact;
                        data[3] = date;
                        data[4] = classifiedbreed;
                        data[5] = imagePath;
                        data[6] = encodedImage;

                        PutData putData = new PutData("http://192.168.1.4/loginregister/save_report.php", "POST", field, data);
                        if (putData.startPut()) {
                            if (putData.onComplete()) {
                                String result = putData.getResult();
                                if (result.equals("Report Success")) {
                                    // Will direct you to the MainActivity after a successful report
                                    Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (image != null) {
                            image.recycle();
                        }
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "All fields and image required", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    //Date Calendar
    private void openDatePicker() {
        // Get the current date
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Parse the initial date value from the textDate TextView
        String initialDate = textDate.getText().toString();
        if (!initialDate.isEmpty()) {
            String[] parts = initialDate.split("/");
            if (parts.length == 3) {
                day = Integer.parseInt(parts[0]);
                month = Integer.parseInt(parts[1]) - 1; // Subtract 1 from month, as it is zero-based in Calendar
                year = Integer.parseInt(parts[2]);
            }
        }

        // Create a Date Picker Dialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                ReportActivity.this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        // Update the selected date in the TextView
                        String selectedDate = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year;
                        textDate.setText(selectedDate);
                    }
                },
                year, month, day);

        // Show the Date Picker dialog
        datePickerDialog.show();
    }


    // Image Chooser
    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                imageUpload.setImageBitmap(bitmap);
            } catch (IOException e) {
                Log.e("ReportActivity", "Error selecting image: " + e.getMessage());
            }
        }
    }

    private String getImagePathFromUri(Uri uri) {
        String imagePath = null;
        if (uri != null) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                    imagePath = cursor.getString(columnIndex);
                }
                cursor.close();
            }
        }
        return imagePath;
    }
    private String encodeImageToBase64(Bitmap image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

}
