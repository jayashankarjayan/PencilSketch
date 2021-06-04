package com.marar.pencilsketch;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private static final int CREATE_PENCIL_SKETCH = 1;
    private static final int DISPLAY_INPUT_IMAGE = 5;
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 2;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 3;
    private static final int REQUEST_ACCESS_MEDIA_LOCATION = 4;
    private static final String MY_PREFERENCES = "MyPreferences";
    private static final String HAS_RESTARTED = "has_restarted";
    Mat image = null;
    String selected_image_path = null;
    ExtendedFloatingActionButton select_image_button;
    ExtendedFloatingActionButton generate_sketch_button;
    ConstraintLayout constraintLayout;
    ImageView input_image;
    ProgressBar progress_circular;
    SharedPreferences sharedpreferences;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedpreferences = getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE);
        editor = sharedpreferences.edit();
        editor.putBoolean(HAS_RESTARTED, false);


        input_image = (ImageView)findViewById(R.id.input_image);
        progress_circular = (ProgressBar)findViewById(R.id.progress_circular);
        constraintLayout = (ConstraintLayout)findViewById(R.id.root_layout);
        select_image_button = (ExtendedFloatingActionButton)findViewById(R.id.select_image_button);
        generate_sketch_button = (ExtendedFloatingActionButton)findViewById(R.id.generate_sketch_button);
        requestStoragePermission();

        select_image_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), DISPLAY_INPUT_IMAGE);
            }
        });

        generate_sketch_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(input_image.getDrawable() == null || selected_image_path == null){
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle(R.string.app_name);
                    builder.setMessage(R.string.select_an_image);
                    builder.setNeutralButton(android.R.string.ok, null);
                    builder.show();
                }
                else{
                    progress_circular.setVisibility(View.VISIBLE);
                    performConversion(selected_image_path);
                    progress_circular.setVisibility(View.INVISIBLE
                    );
                }
            }
        });
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.d("verify",String.valueOf(OpenCVLoader.initDebug()));
                    Log.i("TAG", "OpenCV loaded successfully");
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == DISPLAY_INPUT_IMAGE) {
            Uri uri = data.getData();
//            operatePostIntent(uri);
            String file_path = getPathToImageForProcessing(uri);
            selected_image_path = file_path;

            if(file_path == null){
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(R.string.app_name);
                builder.setMessage(R.string.image_not_rendered_message);
                builder.setNeutralButton(android.R.string.ok, null);
                builder.show();
            }
            else{
                if(!(new File(file_path).exists())){
                    try{
                        file_path = getPathFromUri(getApplicationContext(), uri);
                        selected_image_path = file_path;
                    }
                    catch (IllegalArgumentException exception){
                        Log.d("GETPATHRUI ERR", exception.getMessage());
                        Log.d("GETPATHRUI ERR", uri.getPath());

                        file_path = uri.toString();
                    }
                }
                Bitmap bitmap = BitmapFactory.decodeFile(file_path);
                if(bitmap == null){
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle(R.string.app_name);
                    builder.setMessage(R.string.image_not_rendered_message);
                    builder.setNeutralButton(android.R.string.ok, null);
                    builder.show();
                }else{
                    input_image.setImageBitmap(bitmap);
                }
            }
        }
    }

    private String getPathToImageForProcessing(Uri uri) {
        String file_path = null;

        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = getApplicationContext().getContentResolver().query(uri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            file_path =  cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (android.os.Build.VERSION.SDK_INT != Build.VERSION_CODES.Q){
            if(isExternalStorageDocument(uri)) {
                file_path = getFilePath(uri);
                if(!(new File(file_path).exists())){
                    file_path = getFilePath(uri);
                }
            }
            else{
                if(file_path == null || !(new File(file_path).exists())){
                    file_path = getFilePath(uri);
                }
            }
        }
        else{
            String temp_path = uri.getPath();
            try{
                String document_id = temp_path.split(":")[1];

                String storage = Environment.getExternalStorageDirectory().getAbsolutePath();
                file_path = storage + File.separator + document_id;
                if(!(new File(file_path).exists())){
                    file_path = getFilePath(uri);
                }
            }
            catch(IndexOutOfBoundsException ind){
                Log.d("ERR", ind.getMessage());
            }
        }

        return file_path;
    }

    private void operatePostIntent(Uri uri) {
        String file_path = null;

        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = getApplicationContext().getContentResolver().query(uri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            file_path =  cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (android.os.Build.VERSION.SDK_INT != Build.VERSION_CODES.Q){
            if(isExternalStorageDocument(uri)) {
                file_path = getFilePath(uri);
                Log.d("SKJDFSD", new File(file_path).exists() + "");
                performConversion(file_path);
            }
            else{
                String file_name = new File(file_path).getName();
                String destination_path = Environment.getDataDirectory() + File.separator + file_name;
                try {
                    File source_file = new File(file_path);
                    if (source_file.exists()){
                        copy(source_file, new File(destination_path));
                        performConversion(destination_path);
                    }
                    else{
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle(R.string.app_name);
                        builder.setMessage(R.string.image_not_rendered_message);
                        builder.setNeutralButton(android.R.string.ok, null);
                        builder.show();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d("COPY FAIL", e.getMessage());
                }
            }
        }
        else{
            String temp_path = uri.getPath();
            String document_id = temp_path.split(":")[1];
            String storage = Environment.getExternalStorageDirectory().getAbsolutePath();
            file_path = storage + File.separator + document_id;
            if(!(new File(file_path).exists())){
                file_path = getFilePath(uri);
            }
            performConversion(file_path);
        }
    }

    private String getFilePath(Uri uri) {
        String complete_path;
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DOCUMENT_ID };
            cursor = getApplicationContext().getContentResolver().query(uri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DOCUMENT_ID);
            cursor.moveToFirst();

            String file_path = cursor.getString(column_index).replace(":", File.separator);

            complete_path = Environment.getStorageDirectory().getAbsolutePath() + File.separator + file_path;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return complete_path;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("TAG", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d("TAG", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    private void requestStoragePermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                showExplanation("Permission Needed", "The application needs access to your phone storage to function properly", Manifest.permission.READ_EXTERNAL_STORAGE, REQUEST_READ_EXTERNAL_STORAGE);
            } else {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_EXTERNAL_STORAGE);
            }
        }

        int writePermissionCheck = ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (writePermissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                showExplanation("Permission Needed", "The application needs access to your phone storage to function properly", Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_WRITE_EXTERNAL_STORAGE);
            } else {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE);
            }
        }

        int accessMediaLocationCheck = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_MEDIA_LOCATION);

        if (accessMediaLocationCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_MEDIA_LOCATION)) {
                showExplanation("Permission Needed", "The application needs access to your phone storage to function properly", Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_WRITE_EXTERNAL_STORAGE);
            } else {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_MEDIA_LOCATION}, REQUEST_ACCESS_MEDIA_LOCATION);
            }
        }
    }

    private void showExplanation(String title,
                                 String message,
                                 final String permission,
                                 final int permissionRequestCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        requestPermission(permission, permissionRequestCode);
                    }
                });
        builder.create().show();
    }

    private void requestPermission(String permissionName, int permissionRequestCode) {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, permissionRequestCode);
    }

    public MainActivity() {
        Log.i("TAG", "Instantiated new " + this.getClass());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("READ PERMISSION", "Read Permission Granted!");
                } else {
                    Log.d("READ PERMISSION", "Read Permission Denied!");
                }
            case REQUEST_WRITE_EXTERNAL_STORAGE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("WRITE PERMISSION", "Write Permission Granted!");
                    if(!sharedpreferences.getBoolean(HAS_RESTARTED, false)){
                        Intent intent = getIntent();
                        editor.putBoolean(HAS_RESTARTED, true);
                        editor.commit();
                        finish();
                        startActivity(intent);
                    }
                } else {
                    Log.d("WRITE PERMISSION", "Write Permission Denied!");
                }
        }
    }

    public boolean performConversion(String file_path){
        final String relativeLocation = Environment.DIRECTORY_DCIM + File.separator + getApplicationContext().getResources().getString(R.string.app_name);
        Log.d("APP FOLDER", relativeLocation);
        boolean done = false;
        image = Imgcodecs.imread(file_path);

        Log.d("IMAGEEXIST", new File(file_path).exists() + "");
        Mat inverted = new Mat();
        if(!image.empty()){
            Log.d("IMAGE EMPTY STATUS", image.empty() + "");
            Imgproc.cvtColor(image, inverted, Imgproc.COLOR_RGB2GRAY);
            Mat gaussian_blurred = new Mat();
            Imgproc.GaussianBlur(inverted, gaussian_blurred,
                    new Size(21,21),0);
            Mat final_image = new Mat();
            Core.divide(inverted, gaussian_blurred, final_image, 256.0);
            String file_name = new File(file_path).getName();

            String image_path = getApplicationFolder()
                        + File.separator + getApplicationContext().getResources().getString(R.string.app_name)
                        + " - " + file_name;

            done = Imgcodecs.imwrite(image_path, final_image);
            if(done){
                Log.d("Done", "Success");
                Uri file_uri = FileProvider.getUriForFile(getApplicationContext(),
                        BuildConfig.APPLICATION_ID + ".provider",
                        new File(image_path));

                new SingleMediaScanner(getApplicationContext(), new File(image_path));

                Intent intent = new Intent(this, DisplayOutput.class);
                intent.putExtra("file_path", image_path);
                input_image.setImageDrawable(null);
                startActivity(intent);
            }
            else{
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(R.string.app_name);
                builder.setMessage(R.string.sketch_not_generated_message);
                builder.setNeutralButton("Try again", null);
                builder.show();
            }
        }
        else {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(R.string.app_name);
            builder.setMessage(R.string.sketch_not_generated_message);
            builder.setNeutralButton("Try again", null);
            builder.show();
        }

        return done;

    }

    public static void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        try {
            OutputStream out = new FileOutputStream(dst);
            try {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    public static String getPathFromUri(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + File.separator + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                Uri contentUri = null;
                try {
                    contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"), Long.parseLong(id));
                }
                catch (NumberFormatException e){
                    contentUri =
                    Uri.parse("content://downloads/public_downloads" + id);
                }

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
//            if (isGooglePhotosUri(uri))
//                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
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