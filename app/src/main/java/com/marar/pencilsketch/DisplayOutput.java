package com.marar.pencilsketch;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import org.jetbrains.annotations.NotNull;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;

public class DisplayOutput extends AppCompatActivity {

    private ImageView output_image;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_display_output);
        Intent intent = getIntent();
        String file_path = intent.getStringExtra("file_path");
        String original_file = intent.getStringExtra("original_file");
        Bitmap bitmap = BitmapFactory.decodeFile(file_path);

        ConstraintLayout output_root = (ConstraintLayout) findViewById(R.id.output_root);
        output_image = (ImageView) findViewById(R.id.output_image);
        ExtendedFloatingActionButton share_button = (ExtendedFloatingActionButton) findViewById(R.id.share_button);
        ExtendedFloatingActionButton delete_button = (ExtendedFloatingActionButton) findViewById(R.id.delete_button);
        Slider contrast_slider = (Slider)findViewById(R.id.contrast_slider);

        output_image.setImageBitmap(bitmap);
        Snackbar.make(output_image, R.string.image_generated_message, Snackbar.LENGTH_LONG)
                .show();

        contrast_slider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull @NotNull Slider slider) {

                performConversion(original_file, Math.round(slider.getValue()));
                Log.d("Contrast Progress", Math.round(slider.getValue()) + "");
            }

            @Override
            public void onStopTrackingTouch(@NonNull @NotNull Slider slider) {

            }
        });

        share_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri file_uri = FileProvider.getUriForFile(getApplicationContext(),
                        BuildConfig.APPLICATION_ID + ".provider",
                        new File(file_path));

                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, file_uri);
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                shareIntent.setType("image/*");
                startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.app_name)));

            }
        });

        delete_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(DisplayOutput.this);
                builder.setTitle(R.string.app_name);
                builder.setMessage(R.string.delete_image_question);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        File file = new File(file_path);
                        file.delete();
                        new SingleMediaScanner(getApplicationContext(), file);
                        Toast.makeText(getApplicationContext(), R.string.image_deleted_message, Toast.LENGTH_SHORT);
                        finish();
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.show();
            }
        });
    }

    public boolean performConversion(String file_path, int gaussian_contrast){
        final String relativeLocation = Environment.DIRECTORY_DCIM + File.separator + getApplicationContext().getResources().getString(R.string.app_name);
        Log.d("APP FOLDER", relativeLocation);
        boolean done = false;
        Mat image = Imgcodecs.imread(file_path);

        Log.d("IMAGEEXIST", new File(file_path).exists() + "");
        Mat inverted = new Mat();

        Log.d("IMAGE EMPTY STATUS", image.empty() + "");
        Imgproc.cvtColor(image, inverted, Imgproc.COLOR_RGB2GRAY);
        Mat gaussian_blurred = new Mat();
        Imgproc.GaussianBlur(inverted, gaussian_blurred,
                new Size(21,21),gaussian_contrast);
        Mat final_image = new Mat();
        Core.divide(inverted, gaussian_blurred, final_image, 256.0);
        String file_name = new File(file_path).getName();

        String image_path = getApplicationFolder()
                + File.separator + getApplicationContext().getResources().getString(R.string.app_name)
                + " - " + file_name;

        done = Imgcodecs.imwrite(image_path, final_image);
        Log.d("Done", "Success");
        Uri file_uri = FileProvider.getUriForFile(getApplicationContext(),
                BuildConfig.APPLICATION_ID + ".provider",
                new File(image_path));

        new SingleMediaScanner(getApplicationContext(), new File(image_path));
        Bitmap bitmap = BitmapFactory.decodeFile(image_path);
        output_image.setImageBitmap(bitmap);

        return done;
    }

    public String getApplicationFolder(){
        String folder_path = Environment.getExternalStorageDirectory().getAbsolutePath() +
                File.separator + Environment.DIRECTORY_PICTURES + File.separator +
                getApplicationContext().getResources().getString(R.string.app_name);

        if(!(new File(folder_path).exists()))
        {
            new File(folder_path).mkdir();
        }
        return folder_path;

    }
}