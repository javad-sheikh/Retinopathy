package com.example.retinopathy;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import static android.content.ContentValues.TAG;

public class camera extends Activity implements SurfaceHolder.Callback {
    Camera mCamera;
    SurfaceView mPreview;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        mPreview = findViewById(R.id.preview);
        mPreview.getHolder().addCallback(this);
        mPreview.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        mCamera = Camera.open();

        Camera.Parameters parameters = mCamera.getParameters();

        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

        mCamera.setParameters(parameters);

    }

    @Override
    public void onBackPressed() {
        mCamera.stopPreview();
        Intent returnIntent = new Intent();
        returnIntent.putExtra("image","");
        setResult(RESULT_OK,returnIntent);
        finish();

    }

    @Override
    public void onPause() {
        super.onPause();
        mCamera.stopPreview();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        mCamera.release();
    }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();

        Camera.Size previewSize = previewSizes.get(0);

        for(int i = 1; i < previewSizes.size(); i++){
            if((previewSizes.get(i).width * previewSizes.get(i).height) > (previewSize.width * previewSize.height)){
                previewSize = previewSizes.get(i);
            }
        }
        List<Camera.Size> cameraSizes = parameters.getSupportedPictureSizes();

        Camera.Size bestSize = cameraSizes.get(0);

        for(int i = 1; i < cameraSizes.size(); i++){
            if((cameraSizes.get(i).width * cameraSizes.get(i).height) > (bestSize.width * bestSize.height)){
                bestSize = cameraSizes.get(i);
            }
        }

        parameters.setPictureSize(bestSize.width, bestSize.height);
        parameters.setPreviewSize(previewSize.width, previewSize.height);


        mCamera.setParameters(parameters);

        Display display = ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        if(display.getRotation() == Surface.ROTATION_0) {
            mCamera.setDisplayOrientation(90);
        } else if(display.getRotation() == Surface.ROTATION_270) {
            mCamera.setDisplayOrientation(180);
        }

        mCamera.startPreview();
    }



    private Camera.PictureCallback getJpegCallback(){
        Camera.PictureCallback jpeg=new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {

                File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
                if (pictureFile == null){
                    Log.d(TAG, "Error creating media file, check storage permissions");
                    return;
                }
                try {
                    FileOutputStream fos = new FileOutputStream(pictureFile);
                    fos.write(data);
                    fos.close();
                } catch (FileNotFoundException e) {
                    Log.d(TAG, "File not found: " + e.getMessage());
                } catch (IOException e) {
                    Log.d(TAG, "Error accessing file: " + e.getMessage());
                }

                mCamera.startPreview();
                mCamera.stopPreview();

                Intent returnIntent = new Intent();
                returnIntent.putExtra("image",pictureFile.getAbsolutePath());
                setResult(RESULT_OK,returnIntent);
                finish();
            }

        };
        return jpeg;
    }

    public static final int MEDIA_TYPE_IMAGE = 1;

    /** Create a file Uri for saving an image or video */
    private Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Create a File for saving an image or video */
    private File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(String.valueOf(getApplicationContext().getExternalFilesDir("data")));
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        File mediaFile;

        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                "IMG" + ".jpg");

        return mediaFile;
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mCamera.setPreviewDisplay(mPreview.getHolder());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i("PREVIEW","surfaceDestroyed");
    }


    public void back(View view) {
            mCamera.takePicture(null,  null,getJpegCallback());
    }
}
