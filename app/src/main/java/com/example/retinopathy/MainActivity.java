package com.example.retinopathy;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.tensorflow.lite.Interpreter;
import org.opencv.imgproc.Imgproc;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MainActivity extends AppCompatActivity {

    private static final int MY_CAMERA_REQUEST_CODE = 100;
    private static final int RESULT_CAPTURE_IMG = 1;
    private static final int RESULT_LOAD_IMG = 2;
    private Interpreter interpreter3;

    private String dateReaderFile = "final_model.tflite";

    private ByteBuffer imgData;
    private int[] intValues;

    TextView result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        OpenCVLoader.initDebug();

        try{
            interpreter3 = new Interpreter(loadModelFile(this, dateReaderFile));
        }catch (Exception e){
            Toast.makeText(this,e.toString(),Toast.LENGTH_LONG).show();
        }

        result = findViewById(R.id.results);


        Button btnGallery =  findViewById(R.id.gallery);
        btnGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, RESULT_LOAD_IMG);
            }
        });


        Button btnCamera = findViewById(R.id.camera);
        btnCamera.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_REQUEST_CODE);
                }else{

                    Intent cameraIntent = new Intent(getApplicationContext(),camera.class);
                    startActivityForResult(cameraIntent, RESULT_CAPTURE_IMG);
                }
            }
        });
    }


    Bitmap preprocess(Bitmap bitmap,int desired_size){

        Mat src = new Mat();
        Mat dst = new Mat();
        Mat tmp = new Mat();
        Utils.bitmapToMat(bitmap,src);
        Imgproc.cvtColor(src,tmp,Imgproc.COLOR_RGB2GRAY);
        Bitmap bitmap1 = bitmap.copy(bitmap.getConfig(),true);
        Utils.matToBitmap(tmp,bitmap1);
        int width = bitmap1.getWidth();
        int height = bitmap1.getHeight();

        int size = bitmap1.getRowBytes() * bitmap1.getHeight();
        ByteBuffer byteBuffer = ByteBuffer.allocate(size);
        bitmap1.copyPixelsToBuffer(byteBuffer);
        byte[] byteArray = byteBuffer.array();

        int startx=0;
        int stopx=height;
        int count=0;
        boolean startfound=false;
        for(int i = 0;i<width;i++) {
            boolean flag = false;
            for (int j = 0; j < height; j++) {
                if (byteArray[count++] > 40)
                    flag = true;
            }
            if(flag==false){
                if(startfound==false)
                    startx=i;
                else {
                    stopx = i;
                    break;
                }
            }else {
                startfound = true;
            }
        }

        int starty=0;
        int stopy=height;
        count=0;
        startfound=false;
        for(int i = 0;i<height;i++) {
            boolean flag = false;
            for (int j = 0; j < width; j++) {
                if (byteArray[count++] > 40)
                    flag = true;
            }
            if(flag==false){
                if(startfound==false)
                    starty=i;
                else {
                    stopy = i;
                    break;
                }
            }else {
                startfound = true;
            }
        }


        bitmap = Bitmap.createBitmap(bitmap,startx,starty,stopx-startx,stopy-starty);

        bitmap = getResizedBitmap(bitmap,desired_size,desired_size);
        Imgproc.resize(src,dst,new Size(desired_size,desired_size));
        src = dst;
        Imgproc.GaussianBlur(src,tmp,new Size(0,0),desired_size/30);
        Core.addWeighted(src,4,tmp,-4,128,dst);
        Utils.matToBitmap(dst,bitmap);
        return bitmap;
    }


    @SuppressLint("ResourceAsColor")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_CAPTURE_IMG && resultCode == RESULT_OK) {
            String file_path = data.getStringExtra("image");
            File pictureFile = new File(file_path);
            Bitmap bitmap = BitmapFactory.decodeFile(file_path);
            int height = bitmap.getHeight();
            int width = bitmap.getWidth();
            int x = (int) (width*0.2);
            int y = (int) (height*0.05);
            height = (int) (height-height*0.1);
            width = (int) (width-width*0.4);
            bitmap = Bitmap.createBitmap(bitmap,x,y,width,height);
            bitmap = preprocess(bitmap,320);

            int detection= getCode(bitmap);

            if(detection==0){
                result.setText("بیمار نیست");
                result.setTextColor(Color.parseColor("#00ff00"));
            }else {
                result.setText("بیمار هست");
                result.setTextColor(Color.parseColor("#ff0000"));
            }
            bitmap.recycle();
            pictureFile.delete();
        } else if (requestCode == RESULT_LOAD_IMG && resultCode == RESULT_OK) {
            try {
                final Uri imageUri = data.getData();
                final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                Bitmap bitmap = preprocess(selectedImage,320);
                int detection= getCode(bitmap);

                if(detection==0){
                    result.setText("بیمار نیست");
                    result.setTextColor(Color.parseColor("#00ff00"));
                }else {
                    result.setText("بیمار هست");
                    result.setTextColor(Color.parseColor("#ff0000"));
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

        }
    }



    public static Bitmap getResizedBitmap(Bitmap bitmap, int newWidth, int newHeight){
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;

        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, width, height, matrix, false);
        return resizedBitmap;
    }


    private int getCode(Bitmap bitmapCode) {
        int xSize = bitmapCode.getWidth();
        int ySize = bitmapCode.getHeight();

        imgData = ByteBuffer.allocateDirect(xSize * ySize * 3*4);
        imgData.order(ByteOrder.nativeOrder());
        intValues = new int[xSize * ySize];
        float[][][][] floats = new float[1][xSize][ySize][3];

        bitmapCode.getPixels(intValues, 0, bitmapCode.getWidth(), 0, 0, bitmapCode.getWidth(), bitmapCode.getHeight());

        imgData.clear();
        imgData.rewind();

        for (int i = 0; i < xSize; ++i) {
            for (int j = 0; j < ySize; ++j) {

                int pixelValue = intValues[i * ySize + j];
                floats[0][i][j][0] = ((((pixelValue >> 16) & 0xFF) ));
                floats[0][i][j][1] = ((((pixelValue >> 8) & 0xFF) ));
                floats[0][i][j][2] =(((pixelValue & 0xFF) ));
            }
        }


        float[][] outputCode = new float[1][5];


        interpreter3.run(floats,outputCode);


        float best_threshold = (float) 0.20;
        int sum =0;
        for(int i=0 ; i < 5; i++){
            if(outputCode[0][i]>best_threshold)
                sum++;
        }
        sum--;
        return sum;
    }


    private MappedByteBuffer loadModelFile(Activity activity, String MODEL_FILE) throws
            IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(MODEL_FILE);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

}
