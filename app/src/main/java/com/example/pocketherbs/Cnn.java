package com.example.pocketherbs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.example.pocketherbs.ml.CNN;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Timer;
import java.util.TimerTask;

public class Cnn extends AppCompatActivity {

    private static final float MIN_CONFIDENCE_THRESHOLD = 0.5f;
    Button camera, gallery, confiBtn, tryAgnBtn, okErrorBtn, howToUsePlantBtn;
    Dialog mDialog, errorDialog, scanningDialog, howToUseDialog;
    ImageView imageView;
    TextView result, description, dialogTextView, usesTxt, leaveTypeTxt, leafShapeTxt, uses, leaveType, leafShape, descriptionTxt, nameOfPlant, resultUse;
    int imageSize = 32;
    String acapulco,ampalaya, bayabas, katakataka, lagundi, oregano, sambong, acapulDesc, ampDesc, bayaDesc, kataDesc, lagunDesc, oregaDesc, sambDesc, error, acapulUse, ampUse, bayaUse, kataUse, lagunlUse, oregaUse, sambUse ;
    String[] classes;
    float[] confidences;
    private ProgressBar progressBar;
    private TextView scanningText;
    private TextView progressText;
    private int progressStatus = 0;
    private final Handler handler = new Handler();
    LinearLayout linearPlantChar, linearPlantDesc;
    ImageView logo;
    private ViewPager viewPager;
    private ImageSliderAdapter adapter;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.loadingScreenTitleCol));
            window.setNavigationBarColor(getResources().getColor(R.color.buttonColor));
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.buttonColor)));
            SpannableString title = new SpannableString("Pocket Herbs");
            title.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.white)), 0, title.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            getSupportActionBar().setTitle(title);
        }

        setContentView(R.layout.activity_capture_gallery);

        Resources resources = getResources();

        acapulco = resources.getString(R.string.acapulco);
        ampalaya = resources.getString(R.string.amplaya);
        bayabas = resources.getString(R.string.bayabas);
        katakataka = resources.getString(R.string.katakataka);
        lagundi = resources.getString(R.string.lagundi);
        oregano = resources.getString(R.string.oregano);
        sambong = resources.getString(R.string.sambong);
        error = resources.getString(R.string.error);

        acapulDesc = resources.getString(R.string.acapulDesc);
        ampDesc = resources.getString(R.string.ampDesc);
        bayaDesc = resources.getString(R.string.bayaDesc);
        kataDesc = resources.getString(R.string.kataDesc);
        lagunDesc = resources.getString(R.string.lagunDesc);
        oregaDesc = resources.getString(R.string.oregaDesc);
        sambDesc = resources.getString(R.string.sambDesc);

        acapulUse = resources.getString(R.string.acapulUse);
        ampUse = resources.getString(R.string.ampUse);
        bayaUse = resources.getString(R.string.bayaUse);
        kataUse = resources.getString(R.string.kataUse);
        lagunlUse = resources.getString(R.string.lagunlUse);
        oregaUse = resources.getString(R.string.oregaUse);
        sambUse = resources.getString(R.string.sambUse);

        usesTxt = findViewById(R.id.usesTxt);
        usesTxt.setPadding(10, 10, 10, 10);

        leaveTypeTxt = findViewById(R.id.leaveTypeTxt);
        leaveTypeTxt.setPadding(10, 10, 10, 10);

        leafShapeTxt = findViewById(R.id.leafShapeTxt);
        leafShapeTxt.setPadding(10, 10, 10, 10);

        uses = findViewById(R.id.uses);
        leaveType = findViewById(R.id.leaveType);
        leafShape = findViewById(R.id.leafShape);

        description = findViewById(R.id.description);
        description.setPadding(10, 10, 10, 10);

        descriptionTxt = findViewById(R.id.descriptionTxt);
        camera = findViewById(R.id.button);
        gallery = findViewById(R.id.button2);
        confiBtn = findViewById(R.id.confiBtn);

        result = findViewById(R.id.result);
        imageView = findViewById(R.id.imageView);

        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if(checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        startActivityForResult(cameraIntent, 3);
                    }else{
                        requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 100);
                    }
                }
            }
        });

        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent cameraIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(cameraIntent, 1);
            }
        });

        mDialog = new Dialog(this);
        mDialog.setContentView(R.layout.popup);
        mDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialogTextView = mDialog.findViewById(R.id.dialogTextView);

        errorDialog = new Dialog(this);
        errorDialog.setContentView(R.layout.error_dialog);
        errorDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        okErrorBtn = errorDialog.findViewById(R.id.okErrorBtn);
        errorDialog.setCancelable(false);
        errorDialog.setCanceledOnTouchOutside(false);

        confiBtn.setVisibility(View.GONE);
        confiBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dialogTextView != null && confidences != null) {
                    StringBuilder confidenceBuilder = new StringBuilder();
                    for (int i = 0; i < confidences.length; i++) {
                        float confidencePercentage = Math.min(confidences[i] * 10.0f, 100.0f);
                        if (confidencePercentage < 0) {
                            confidencePercentage = 0;
                        }
                        confidenceBuilder.append(classes[i]).append(": ").append(String.format("%.2f", confidencePercentage)).append("%\n");
                    }
                    dialogTextView.setText(confidenceBuilder.toString());
                    mDialog.show();
                } else {
                    // Show a toast message indicating that the confidences are not available
                    Toast.makeText(Cnn.this, "Confidences not available, Please input/take picture first", Toast.LENGTH_SHORT).show();
                }
            }

        });

        tryAgnBtn = findViewById(R.id.tryAgnBtn);
        tryAgnBtn.setVisibility(View.GONE);
        tryAgnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Cnn.this, Cnn.class);
                startActivity(intent);
            }
        });

        scanningDialog = new Dialog(this);
        scanningDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        scanningDialog.setContentView(R.layout.loading_scan_dialog);

        progressBar = scanningDialog.findViewById(R.id.progressBar);
        scanningText = scanningDialog.findViewById(R.id.scanningText);
        progressText = scanningDialog.findViewById(R.id.progressText);

        linearPlantChar = findViewById(R.id.linearPlantChar);
        linearPlantDesc = findViewById(R.id.linearPlantDesc);

        logo = findViewById(R.id.logo);

        viewPager = findViewById(R.id.viewPager);
        adapter = new ImageSliderAdapter(this, new int[]{
                R.drawable.acapulco_slide,
                R.drawable.ampalaya_slider,
                R.drawable.bayabas_slider,
                R.drawable.katakataka_slider,
                R.drawable.lagundi_slider,
                R.drawable.oregano_slider,
                R.drawable.sambong_slider
        }, viewPager);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(adapter.getCount()); // Add this line to preload all pages
        viewPager.setCurrentItem(0); // Set initial item to the first page

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new SliderTimer(), 3000, 3000); // Change the delay values if needed

        howToUseDialog = new Dialog(this);
        howToUseDialog.setContentView(R.layout.uses_of_plant_dialog);
        howToUseDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        howToUsePlantBtn = findViewById(R.id.howToUsePlantBtn);
        howToUsePlantBtn.setVisibility(View.GONE);
        howToUsePlantBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                howToUseDialog.show();
            }
        });

        okErrorBtn = howToUseDialog.findViewById(R.id.okErrorBtn);
        okErrorBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                howToUseDialog.dismiss();
            }
        });

        nameOfPlant = howToUseDialog.findViewById(R.id.nameOfPlant);
        resultUse = howToUseDialog.findViewById(R.id.resultUse);

    }

    private class SliderTimer extends TimerTask {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (viewPager.getCurrentItem() < adapter.getCount() - 1) {
                        viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
                    } else {
                        viewPager.setCurrentItem(0);
                    }
                }
            });
        }
    }

    private void startProgressAnimation() {
        new Thread(() -> {
            while (progressStatus < 100) {
                progressStatus += 1;

                // Update the progress and progress text on the UI thread
                handler.post(() -> {
                    progressBar.setProgress(progressStatus);
                    progressText.setText(progressStatus + "%");
                });

                try {
                    // Delay the progress update to slow down the animation
                    Thread.sleep(30); // Adjust the delay as needed
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            handler.post(() -> {
                scanningText.setText("Scan Complete");
            });

            handler.postDelayed(() -> {
                scanningDialog.dismiss();
            }, 1000);
        }).start();
    }

    private void applyBlinkingAnimation() {
        Animation animation = new AlphaAnimation(1, 0); // Change alpha from fully visible to invisible
        animation.setDuration(500); // Blinking duration (half a second)
        animation.setInterpolator(new LinearInterpolator()); // Use linear interpolation for consistent blinking
        animation.setRepeatCount(Animation.INFINITE); // Repeat animation infinitely
        animation.setRepeatMode(Animation.REVERSE); // Reverse the animation at the end

        scanningText.startAnimation(animation);
    }

    public void classifyImage(Bitmap image) {
        try {
            CNN model = CNN.newInstance(getApplicationContext());

            int imageWidth = image.getWidth();
            int imageHeight = image.getHeight();

            // Determine the minimum dimension for resizing the image
            int dimension = Math.min(imageWidth, imageHeight);

            // Resize the image to the desired input size
            Bitmap resizedImage = ThumbnailUtils.extractThumbnail(image, dimension, dimension);
            Bitmap scaledImage = Bitmap.createScaledBitmap(resizedImage, imageSize, imageSize, false);

            // Create a ByteBuffer with dynamic capacity based on the resized image dimensions
            int bufferSize = 4 * imageSize * imageSize * 3;
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bufferSize);
            byteBuffer.order(ByteOrder.nativeOrder());

            int[] intValues = new int[imageSize * imageSize];
            scaledImage.getPixels(intValues, 0, scaledImage.getWidth(), 0, 0, scaledImage.getWidth(), scaledImage.getHeight());

            // Populate the ByteBuffer with pixel values
            int pixel = 0;
            for (int i = 0; i < imageSize; i++) {
                for (int j = 0; j < imageSize; j++) {
                    int val = intValues[pixel++];
                    byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 1));
                    byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 1));
                    byteBuffer.putFloat((val & 0xFF) * (1.f / 1));
                }
            }
            // Create TensorBuffer and load the ByteBuffer
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, imageSize, imageSize, 3}, DataType.FLOAT32);
            inputFeature0.loadBuffer(byteBuffer);

            // Perform inference and obtain the result
            CNN.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            confidences = outputFeature0.getFloatArray();
            int maxPos = -1;
            float maxConfidence = 0.0f;
            for (int i = 0; i < confidences.length; i++) {
                if (confidences[i] > maxConfidence) {
                    maxConfidence = confidences[i];
                    maxPos = i;
                }
            }
            classes = new String[]{acapulco, ampalaya, bayabas, katakataka, lagundi, oregano, sambong, error};
            String[] classes1 = {acapulDesc, ampDesc, bayaDesc, kataDesc, lagunDesc, oregaDesc, sambDesc, error};
            String [] classes2 = {acapulUse, ampUse, bayaUse, kataUse, lagunlUse, oregaUse, sambUse, error};

            if (maxPos != -1) {
                String className = classes[maxPos];
                String descriptionText = classes1[maxPos];
                String usesTxt = classes2[maxPos];
                result.setText(className);
                description.setText(descriptionText);
                resultUse.setText(usesTxt);
            } else {
                // Show a toast message indicating no match found
                Toast.makeText(Cnn.this, "No match found", Toast.LENGTH_SHORT).show();
                confiBtn.setVisibility(View.INVISIBLE);
                tryAgnBtn.setVisibility(View.INVISIBLE);
            }


            // Find the highest confidence and its corresponding data
            int maxIndex = -1;
            maxConfidence = -1.0f;
            for (int i = 0; i < confidences.length; i++) {
                if (confidences[i] > maxConfidence) {
                    maxConfidence = confidences[i];
                    maxIndex = i;
                }
            }

            if (maxIndex != -1) {
                // Convert confidence to a percentage value
                //int percentage = Math.min(Math.round(maxConfidence * 100), 100);

                // Check if the image matches a certain threshold
                float confidenceThreshold = 80.0f; // Set your desired confidence threshold percentage here (e.g., 70%)
                float maxConfidencePercentage = (maxConfidence * 100.0f); // Calculate the maximum confidence as a percentage
                if (maxConfidencePercentage >= confidenceThreshold) {
                    // Hide the camera and gallery buttons
                    camera.setVisibility(View.GONE);
                    gallery.setVisibility(View.GONE);
                }

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Display the views after the delay
                        howToUsePlantBtn.setVisibility(View.VISIBLE);
                        viewPager.setVisibility(View.GONE);
                        imageView.setVisibility(View.VISIBLE);
                        linearPlantDesc.setVisibility(View.VISIBLE);
                        linearPlantChar.setVisibility(View.VISIBLE);
                        confiBtn.setVisibility(View.VISIBLE);
                        tryAgnBtn.setVisibility(View.VISIBLE);
                        logo.setVisibility(View.GONE);
                    }
                }, 4000);
            } else {
                // Show a toast message indicating no match found
                Toast.makeText(Cnn.this, "No match found", Toast.LENGTH_SHORT).show();
                confiBtn.setVisibility(View.INVISIBLE);
                tryAgnBtn.setVisibility(View.INVISIBLE);
            }

            // Show the custom dialog
            //mDialog.show();
            //usesTxt, leaveTypeTxt,leafShapeTxt


            if (classes[maxPos].equals(acapulco) || classes[maxPos].equals(bayabas)){
                usesTxt.setText("For Cough only");
            }else{
                usesTxt.setText("For Cough and Fever");
            }

            if (classes[maxPos].equals(acapulco) || classes[maxPos].equals(lagundi) || classes[maxPos].equals(bayabas) || classes[maxPos].equals(oregano) ){
                leaveTypeTxt.setText("Outdoor Herb");
            }else {
                leaveTypeTxt.setText("Indoor Herb");
            }
            //acapulco, ampalaya, bayabas, katakataka, lagundi, oregano, sambong
            if (classes[maxPos].equals(acapulco)){
                leafShapeTxt.setText("oval or elliptical");
            }
            else if(classes[maxPos].equals(ampalaya)){
                leafShapeTxt.setText("heart-shaped or ovate with pointed tips");
            }
            else if(classes[maxPos].equals(bayabas)){
                leafShapeTxt.setText("elliptical or oblong in shape with smooth edges");
            }
            else if(classes[maxPos].equals(katakataka)){
                leafShapeTxt.setText("multiple leaflets arranged in pairs");
            }
            else if(classes[maxPos].equals(lagundi)){
                leafShapeTxt.setText("central point like the fingers of a hand");
            }
            else if(classes[maxPos].equals(oregano)){
                leafShapeTxt.setText("small and elongated");
            }
            else if(classes[maxPos].equals(sambong)){
                leafShapeTxt.setText("lanceolate or oblong");
            }
            else if(classes[maxPos].equals(error)){
                linearPlantDesc.setVisibility(View.GONE);
                uses.setVisibility(View.GONE);
                usesTxt.setVisibility(View.GONE);
                leaveTypeTxt.setVisibility(View.GONE);
                leaveType.setVisibility(View.GONE);
                leafShapeTxt.setVisibility(View.GONE);
                leafShape.setVisibility(View.GONE);
                description.setVisibility(View.GONE);
                descriptionTxt.setVisibility(View.GONE);

                /*
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        errorDialog.show();
                    }
                }, 4000);

               okErrorBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(Cnn.this, Cnn.class);
                        startActivity(intent);
                        errorDialog.dismiss();
                    }
                });

                 */

            }

            // Check if the image does not match any expected classes
            if (maxConfidence < MIN_CONFIDENCE_THRESHOLD) {
                result.setText("Unknown");
            }


            model.close();
        } catch (IOException e) {
            // Handle the exception
            e.printStackTrace();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if(resultCode == RESULT_OK){
            if (requestCode == 3){

                Bitmap image = (Bitmap) data.getExtras().get("data");
                int dimension = Math.min(image.getWidth(), image.getHeight());
                image = ThumbnailUtils.extractThumbnail(image, dimension, dimension);
                imageView.setImageBitmap(image);

                image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
                classifyImage(image);

            }else{

                Uri dat = data.getData();
                Bitmap image = null;
                try {
                    image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), dat);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                imageView.setImageBitmap(image);

                image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
                classifyImage(image);

            }

            scanningDialog.show();
            scanningDialog.setCancelable(false);
            scanningDialog.setCanceledOnTouchOutside(false);
            startProgressAnimation();
            applyBlinkingAnimation();

        }


        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        // Create an Intent to start the parent activity
        Intent intent = new Intent(this, MainActivity.class);
        // Add the FLAG_ACTIVITY_CLEAR_TOP flag to clear all activities on top of the parent activity
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        // Start the parent activity
        startActivity(intent);
        // Finish the current activity
        finish();
    }

}

