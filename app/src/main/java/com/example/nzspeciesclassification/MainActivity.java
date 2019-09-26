package com.example.nzspeciesclassification;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import android.Manifest;
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
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import com.example.nzspeciesclassification.env.Logger;

import java.io.ByteArrayOutputStream;
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
    ImageButton cameraButton;
    ImageButton fileButton;
    Button btnChoosePic;
    ImageView imageView;
    ListView predictions;
    String pathToFile;
    private Classifier classifier;
    private Model model = Model.FLOAT;
    private Device device = Device.CPU;
    public static final int PICK_IMAGE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraButton = findViewById(R.id.cameraButton);
        fileButton = findViewById(R.id.fileButton);


        if(Build.VERSION.SDK_INT >= 23){
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);

        }

        fileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickFromGallery();
            }
        });
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchPictureTakenAction();
            }
        });


        imageView = findViewById(R.id.imageView);
    }
    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
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
                File file = new File(pathToFile);
                Bitmap bitmap = BitmapFactory.decodeFile(pathToFile);
                try {
                    ExifInterface ei = new ExifInterface(pathToFile);
                    int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_UNDEFINED);

                    Bitmap rotatedBitmap = null;
                    switch(orientation) {

                        case ExifInterface.ORIENTATION_ROTATE_90:
                            rotatedBitmap = rotateImage(bitmap, 90);
                            break;

                        case ExifInterface.ORIENTATION_ROTATE_180:
                            rotatedBitmap = rotateImage(bitmap, 180);
                            break;

                        case ExifInterface.ORIENTATION_ROTATE_270:
                            rotatedBitmap = rotateImage(bitmap, 270);
                            break;

                        case ExifInterface.ORIENTATION_NORMAL:
                        default:
                            rotatedBitmap = bitmap;
                    }


                    imageView.setImageBitmap(rotatedBitmap);
                    Bitmap scaled = Bitmap.createScaledBitmap(rotatedBitmap, classifier.getImageSizeX(),classifier.getImageSizeY(), false);


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        final List<Recognition> results = classifier.recognizeImage(scaled.copy(Bitmap.Config.ARGB_8888,false));
                        ArrayAdapter adapter = new ArrayAdapter<Recognition>(this,
                                android.R.layout.simple_list_item_1,
                                results);
                        predictions = findViewById(R.id.predictions);
                        predictions.setAdapter(adapter);

                    }
//                    ByteArrayOutputStream _bs = new ByteArrayOutputStream();
//                    rotatedBitmap.compress(Bitmap.CompressFormat.PNG, 40, _bs);
//                    Intent k = new Intent(MainActivity.this, ResultsActivity.class);
//                    k.putExtra("image", _bs.toByteArray());
//                    System.out.println("NOOO GOOO???");
//                    startActivity(k);


                }catch(Exception e){

                }
            }else if (requestCode == PICK_IMAGE){
                Uri uri = data.getData();

                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                    ImageView imageView = findViewById(R.id.imageView);
                    imageView.setImageBitmap(bitmap);
                    Bitmap scaled = Bitmap.createScaledBitmap(bitmap, classifier.getImageSizeX(),classifier.getImageSizeY(), false);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        final List<Recognition> results = classifier.recognizeImage(scaled.copy(Bitmap.Config.ARGB_8888,false));
                        ArrayAdapter adapter = new ArrayAdapter<Recognition>(this,
                                android.R.layout.simple_list_item_1,
                                results);
                        predictions = (ListView) findViewById(R.id.predictions);
                        predictions.setAdapter(adapter);

                        System.out.println(results);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }



            }
        }
    }
    private void classifyImage(){
        //TODO: Will have a button that says classify and will trigger this method

    }
    private void pickFromGallery(){
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        File photoFile = null;

        photoFile = createPhotoFile();
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
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
