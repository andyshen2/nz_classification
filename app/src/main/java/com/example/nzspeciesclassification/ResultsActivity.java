package com.example.nzspeciesclassification;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;

public class ResultsActivity extends AppCompatActivity {

    Intent intent = getIntent();
    Bitmap bitmap = (Bitmap) intent.getParcelableExtra("image");
    ImageView imageView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
                
        setContentView(R.layout.activity_results);
        imageView = findViewById(R.id.imageView);
//        imageView.setImageBitmap(bitmap);
    }
    
}
