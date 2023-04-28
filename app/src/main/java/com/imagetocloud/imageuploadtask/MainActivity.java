package com.imagetocloud.imageuploadtask;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.imagetocloud.imageuploadtask.databinding.ActivityMainBinding;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    FirebaseStorage storage;
    StorageReference storageReference;
    private Uri filePath;
    private final int PICK_IMAGE_REQUEST = 71;

    private static final int RESULT_REQUEST_CODE = 11;
    private static final int PERMISSION_REQUEST_CODE = 200;
    private List<String> uploadFilesList = new ArrayList<>();
    private List<String> downloadFilesList = new ArrayList<>();
    boolean isFirstImage = false;
    int fileNumber = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();



        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkPermission()) {
                    chooseImage(MainActivity.this);
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{CAMERA, READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
                }
            }
        });


        binding.uploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(binding.capturedImageOne.getDrawable()!=null && binding.capturedImageOne.getDrawable()!=null ) {
                    StorageReference ref = storageReference.child("images/" + UUID.randomUUID().toString());
                    uploadImage(ref);
                    binding.fileOnePath.setVisibility(View.GONE);
                    binding.fileTowPath.setVisibility(View.GONE);
                }else{
                    Toast.makeText(MainActivity.this, "Please select images to upload", Toast.LENGTH_LONG).show();
                }
            }
        });


        binding.downloadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.fileOnePath.setVisibility(View.VISIBLE);
                binding.fileTowPath.setVisibility(View.VISIBLE);
                downloadImagesfromCloud();
            }
        });


    }

    /**
     * Method for to download image from cloud
     */
    private void downloadImagesfromCloud() {
        StorageReference gsReference = storage.getReferenceFromUrl("gs://imagetask-82ca9.appspot.com");
        // Create a reference with an initial file path and name
        for(int i = 0; i< downloadFilesList.size(); i++) {
            StorageReference pathReference = gsReference.child("/images/"+downloadFilesList.get(i));
            int itemNumber = i;

            Log.v("Download URL:::::" , " "+ pathReference.getName());
            pathReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    // Got the download URL for 'users/me/profile.png'
                    Log.v("Downloaded File URL:::::", " " + uri.toString());
                    ImageView imageView;
                    if(itemNumber==0){
                        imageView = binding.capturedImageOne;
                    }else{
                        imageView = binding.capturedImageTwo;
                    }

                    Glide.with(getApplicationContext())
                            .load(uri)
                            .into(imageView);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Log.v("Exception:::::", " " + exception);
                }
            });
        }

    }

    /**
     * Used for to upload image into google cloud store
     * @param ref
     */
    private void uploadImage(StorageReference ref) {
        if (uploadFilesList != null) {
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading...");
            progressDialog.show();

            for(int i = 0; i< uploadFilesList.size(); i++) {
                int finalI = i;
                ref.putFile(Uri.parse(uploadFilesList.get(i)))
                        .addOnSuccessListener(taskSnapshot -> {
                            Log.v("Upload File URL:::::" , " "+ taskSnapshot.toString() + "::::: "+ ref.getName());
                            downloadFilesList.add(ref.getName());
                            progressDialog.dismiss();
                            if(finalI == uploadFilesList.size()-1){
                                binding.capturedImageTwo.setImageBitmap(null);
                            }else{
                                binding.capturedImageOne.setImageBitmap(null);
                            }
                            Toast.makeText(MainActivity.this, "Uploaded", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            progressDialog.dismiss();
                            Toast.makeText(MainActivity.this, "Failed " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        })
                        .addOnProgressListener(taskSnapshot -> {
                            double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                            progressDialog.setMessage("Uploaded " + (int) progress + "%");
                        });
            }
        }
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            filePath = data.getData();
            uploadFilesList.add(filePath.toString());
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                if(!isFirstImage) {
                    binding.capturedImageOne.setImageBitmap(bitmap);
                    isFirstImage = true;
                    fileNumber = 2;
                }else{
                    binding.capturedImageTwo.setImageBitmap(bitmap);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else if(resultCode == RESULT_OK && requestCode == RESULT_REQUEST_CODE) {
            // get String data from Intent
            String returnString = data.getStringExtra("File_Name");
            //Bitmap selectedImage = BitmapFactory.decodeFile(returnString);
            Bitmap selectedImage = (Bitmap) data.getExtras().get("data");
            if(!isFirstImage) {
                binding.capturedImageOne.setImageBitmap(selectedImage);
                isFirstImage = true;
                fileNumber = 2;
            }else{
                binding.capturedImageTwo.setImageBitmap(selectedImage);
            }
            File sharedFile = new File(returnString);
           // fileNames.add(filePath.toString());
            //Uri sharedFileUri = FileProvider.getUriForFile(this,  "com.imagetocloud.imageuploadtask.provider", sharedFile);
           /* Uri sharedFileUri = FileProvider.getUriForFile(this,  "com.imagetocloud.imageuploadtask.fileprovider", sharedFile);

            Bitmap selectedImage = null;
            try {
                selectedImage = BitmapFactory.decodeStream(getContentResolver().openInputStream(sharedFileUri));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            binding.capturedImage.setImageBitmap(selectedImage);
            Toast.makeText(this,":::::" + returnString, Toast.LENGTH_LONG).show();
            Log.v("FilePath:::::" , returnString);*/
            }
    }

    /**
     * Method for Pick image from device
     */
    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), CAMERA);
        int result2 = ContextCompat.checkSelfPermission(getApplicationContext(), READ_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED && result2 == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0) {

                    boolean locationAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean cameraAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (locationAccepted && cameraAccepted && storageAccepted) {
                        chooseImage(this);
                    }else {
                        Toast.makeText(this, "Permission Denied, You cannot access storage data and camera.", Toast.LENGTH_LONG).show();

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (shouldShowRequestPermissionRationale(CAMERA)) {
                                Utils.showMessageOKCancel("You need to allow access to both the permissions",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                    requestPermissions(new String[]{CAMERA, READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE},
                                                            PERMISSION_REQUEST_CODE);
                                                }
                                            }
                                        }, this);
                                return;
                            }
                        }

                    }
                }
                break;
        }
    }

    // function to let's the user to choose image from camera or gallery
    private void chooseImage(Context context){
        final CharSequence[] optionsMenu = {"Take Photo", "Choose from Gallery" }; // create a menuOption Array
        // create a dialog for showing the optionsMenu
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        // set the items in builder
        builder.setItems(optionsMenu, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(optionsMenu[i].equals("Take Photo")){

                    // Start the SecondActivity
                    Intent intent = new Intent(MainActivity.this, CustomCameraView.class);
                    intent.putExtra("File_Number", fileNumber);
                    startActivityForResult(intent, RESULT_REQUEST_CODE);
                }
                else if(optionsMenu[i].equals("Choose from Gallery")){
                    // choose from  external storage
                    chooseImage();
                }
            }
        });
        builder.show();
        builder.setCancelable(false);
    }
}
