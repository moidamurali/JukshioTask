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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
    private List<String> fileNames = new ArrayList<>();
    boolean isFirstImage = false;

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
                StorageReference ref = storageReference.child("images/" + UUID.randomUUID().toString());
                uploadImage(ref);
            }
        });


        binding.downloadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fileDownload();
            }
        });


    }

    /**
     * Method for to download image from cloud
     */
    private void fileDownload() {
        StorageReference gsReference = storage.getReferenceFromUrl("gs://imagetask-82ca9.appspot.com");
        // Create a reference with an initial file path and name
        StorageReference pathReference = gsReference.child( fileNames.get(0));

        pathReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                // Got the download URL for 'users/me/profile.png'
                Log.v("Downloaded File URL:::::" , " "+ uri.toString());
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.v("Downloaded File URL:::::" , " "+ exception);
            }
        });

    }

    /**
     * Used for to upload image into google cloud store
     * @param ref
     */
    private void uploadImage(StorageReference ref) {
        if (fileNames != null) {
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading...");
            progressDialog.show();

            for(int i=0; i<fileNames.size(); i++) {

                ref.putFile(Uri.parse(fileNames.get(i)))
                        .addOnSuccessListener(taskSnapshot -> {
                            progressDialog.dismiss();
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
            fileNames.add(filePath.toString());
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), filePath);
                if(!isFirstImage) {
                    binding.capturedImageOne.setImageBitmap(bitmap);
                    isFirstImage = true;
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
            //Bitmap selectedImage = (Bitmap) data.getExtras().get("data");
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
                    startActivityForResult(intent, RESULT_REQUEST_CODE);
                }
                else if(optionsMenu[i].equals("Choose from Gallery")){
                    // choose from  external storage
                    chooseImage();
                }
            }
        });
        builder.show();
    }
}
