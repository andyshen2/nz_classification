package com.example.nzspeciesclassification;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;

import com.example.nzspeciesclassification.tflite.Classifier;
import com.example.nzspeciesclassification.env.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class ResultsActivity extends AppCompatActivity {
    ImageButton cameraButton;
    ImageButton fileButton;
    private Classifier classifier;
    private Classifier.Model model = Classifier.Model.FLOAT;
    private Classifier.Device device = Classifier.Device.CPU;
    public static final int PICK_IMAGE = 2;
    ListView predictions;

    ImageView imageView;
    String pathToFile;
    private static final Logger LOGGER = new Logger();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_results);


        Intent intent = getIntent();
        Bitmap bitmap = (Bitmap) intent.getParcelableExtra("BitmapImage");
        Bundle bundle = getIntent().getExtras();
        boolean mainScreen = bundle.getBoolean("mainScreen");
        if(mainScreen) {
            try {
                dispatchPictureTakenAction();
            } catch (Exception e) {
                System.err.println(e);
            }
        }else{
            try{
                pickFromGallery();

            }catch (Exception e){
                System.err.println(e);
            }
        }

        cameraButton = findViewById(R.id.cameraButton);
        fileButton = findViewById(R.id.fileButton);

        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try{
                    dispatchPictureTakenAction();
                }catch (Exception e){
                    System.err.println(e);
                }
            }
        });

        fileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try{
                    pickFromGallery();

                }catch (Exception e){
                    System.err.println(e);
                }
            }
        });

        imageView = findViewById(R.id.imageView);

    }
    String currentPhotoPath;

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        currentPhotoPath = image.getAbsolutePath();
        return image;
    }
    private void pickFromGallery() throws IOException{
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);

        createImageFile();
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
    }
    private void dispatchPictureTakenAction() throws IOException {
        Intent takePic = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if(takePic.resolveActivity(getPackageManager()) != null){
            File photoFile = null;

            photoFile = createImageFile();
            if(photoFile != null){
                pathToFile = photoFile.getAbsolutePath();
                Uri photoURI = FileProvider.getUriForFile(ResultsActivity.this, "com.example.nzspeciesclassification.fileprovider", photoFile);
                takePic.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePic, 1);
            }
        }
    }
    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    private void classifyImage(Bitmap bitmap){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            final List<Classifier.Recognition> results = classifier.recognizeImage(bitmap.copy(Bitmap.Config.ARGB_8888,false));
            int listLength = results.size();
            List<String> speciesName = new ArrayList<>();
            for( int i = 0 ; i < listLength; i++){
                String[]splitNames = results.get(i).toString().split("_");
                String [] species = splitNames[0].split(" ", 2);
                String [] confidence = splitNames[1].split(" ");
                speciesName.add(species[1] + " " + confidence[confidence.length-1]);
            }
            ArrayAdapter adapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_list_item_1,
                    speciesName);
            predictions = findViewById(R.id.predictions);
            predictions.setAdapter(adapter);
        }

    }
    public static Bitmap changeOrientation(int orientation, Bitmap bitmap){

        switch(orientation) {

            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateImage(bitmap, 90);


            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(bitmap, 180);

            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateImage(bitmap, 270);

            case ExifInterface.ORIENTATION_NORMAL:
            default:
                return bitmap;
        }

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            LOGGER.d(
                    "Creating classifier (model=%s, device=%s, numThreads=%d)", model, device, 4);
            classifier = Classifier.create(this, model, device, 4);
        } catch (IOException e) {
            LOGGER.e(e, "Failed to create classifier.");
        }

        if(resultCode == RESULT_OK){
            if(requestCode == 1){
                Bitmap bitmap = BitmapFactory.decodeFile(pathToFile);
                try {
                    ExifInterface ei = new ExifInterface(pathToFile);
                    int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_UNDEFINED);

                    Bitmap rotatedBitmap = changeOrientation(orientation, bitmap);

                    imageView.setImageBitmap(rotatedBitmap);

                    Bitmap scaled = Bitmap.createScaledBitmap(bitmap, classifier.getImageSizeX(),classifier.getImageSizeY(), false);
                    classifyImage(scaled);

                }catch(Exception e){
                    System.err.println(e);
                }
            }else if (requestCode == PICK_IMAGE){
                Uri uri = data.getData();
                try {

                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                    InputStream inputStream = getContentResolver().openInputStream(uri);
                    ExifInterface ei = new ExifInterface(inputStream);
                    int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_UNDEFINED);

                    Bitmap rotatedBitmap = changeOrientation(orientation, bitmap);

                    ImageView imageView = findViewById(R.id.imageView);
                    imageView.setImageBitmap(rotatedBitmap);
                    Bitmap scaled = Bitmap.createScaledBitmap(bitmap, classifier.getImageSizeX(),classifier.getImageSizeY(), false);
                    classifyImage(scaled);
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
        }
    }
    
}
