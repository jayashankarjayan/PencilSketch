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
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.ImageView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.io.File;

public class DisplayOutput extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_display_output);
        Intent intent = getIntent();
        String file_path = intent.getStringExtra("file_path");
        Bitmap bitmap = BitmapFactory.decodeFile(file_path);

        ConstraintLayout output_root = (ConstraintLayout) findViewById(R.id.output_root);
        ImageView output_image = (ImageView) findViewById(R.id.output_image);
        ExtendedFloatingActionButton share_button = (ExtendedFloatingActionButton) findViewById(R.id.share_button);
        ExtendedFloatingActionButton delete_button = (ExtendedFloatingActionButton) findViewById(R.id.delete_button);

        output_image.setImageBitmap(bitmap);
        Snackbar.make(output_root, R.string.image_generated_message, Snackbar.LENGTH_LONG)
                .show();

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
                        finish();
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.show();
            }
        });
    }
}