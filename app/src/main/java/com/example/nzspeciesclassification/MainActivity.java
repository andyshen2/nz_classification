package com.example.nzspeciesclassification;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Picture;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;

import com.example.nzspeciesclassification.env.Logger;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.example.nzspeciesclassification.tflite.Classifier;
import com.example.nzspeciesclassification.tflite.Classifier.Device;
import com.example.nzspeciesclassification.tflite.Classifier.Model;
import com.example.nzspeciesclassification.tflite.Classifier.Recognition;

import static android.os.Environment.getExternalStoragePublicDirectory;

public class MainActivity extends AppCompatActivity {
    private static final Logger LOGGER = new Logger();
    Button btnTakePic;
    ImageView imageView;
    ListView predictions;
    private Bitmap croppedBitmap = null;
    String pathToFile;
    private Classifier classifier;
    private Model model = Model.FLOAT;
    private Device device = Device.CPU;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        btnTakePic = findViewById(R.id.btnTakePic);
        if(Build.VERSION.SDK_INT >= 23){
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
        }
        btnTakePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchPictureTakenAction();
            }
        });
        imageView = findViewById(R.id.imageView);
        //test commit
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

                imageView.setImageBitmap(bitmap);
                Bitmap scaled = Bitmap.createScaledBitmap(bitmap, classifier.getImageSizeX(),classifier.getImageSizeY(), false);
//                croppedBitmap =
//                        Bitmap.createBitmap(
//                                    classifier.getImageSizeX(), classifier.getImageSizeY(), Bitmap.Config.ARGB_8888);

//                if (bitmap.getWidth() >= bitmap.getHeight()){
//
//                    croppedBitmap = Bitmap.createBitmap(
//                            bitmap,
//                            bitmap.getWidth()/2 - bitmap.getHeight()/2,
//                            0,
//                            classifier.getImageSizeX(),
//                            classifier.getImageSizeY()
//                    );
//
//                }else{
//
//                    croppedBitmap = Bitmap.createBitmap(
//                            bitmap,
//                            0,
//                            bitmap.getHeight()/2 - bitmap.getWidth()/2,
//                            classifier.getImageSizeX(),
//                            classifier.getImageSizeY()
//                    );
//                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    final List<Recognition> results = classifier.recognizeImage(scaled.copy(Bitmap.Config.ARGB_8888,false));
                    ArrayAdapter adapter = new ArrayAdapter<Recognition>(this,
                            android.R.layout.simple_list_item_1,
                            results);
                    predictions = (ListView) findViewById(R.id.predictions);
                    predictions.setAdapter(adapter);

                    System.out.println(results);
                }


            }
        }
    }

    private void dispatchPictureTakenAction() {
        Intent takePic = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(takePic.resolveActivity(getPackageManager()) != null){
            File photoFile = null;

            photoFile = createPhotoFile();
            if(photoFile != null){
                pathToFile = photoFile.getAbsolutePath();
                Uri photoURI = FileProvider.getUriForFile(MainActivity.this, "com.example.nzspeciesclassification.fileprovider", photoFile);
                takePic.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePic, 1);
            }
        }
    }

    private File createPhotoFile() {
        String name = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File storageDir = getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = null;
        try {
            image = File.createTempFile(name, ".jpg", storageDir);
        } catch (IOException e) {
            Log.d("mylog", "Error: " + e.toString());
        }
        return image;


    }
}
