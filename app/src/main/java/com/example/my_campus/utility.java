package com.example.my_campus;

import static android.app.PendingIntent.getActivity;


import static com.google.common.net.MediaType.JWT;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.auth0.jwt.algorithms.Algorithm;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.downloader.OnDownloadListener;
import com.downloader.PRDownloader;
import com.example.my_campus.Fragments.fragmentManageRoutines;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.json.JSONObject;

import java.io.File;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class utility {

    private AlertDialog bufferingDialog;

    public interface DialogCallback {
        void onConfirm();
        void onCancel();
    }

    public interface DownloadCallback{
        void onDownloadComplete();
    }

    public void dialogBox(Context context, String message, DialogCallback callback){
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null.");
        }

        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_box);
        Objects.requireNonNull(dialog.getWindow()).setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        ConstraintLayout btnConfirm = dialog.findViewById(R.id.btnConfirm);
        ConstraintLayout btnCancel = dialog.findViewById(R.id.btnHide);
        TextView dialogMassage = dialog.findViewById(R.id.dialogMassage);

        dialogMassage.setText(message);

        btnConfirm.setOnClickListener(view -> {
            dialog.dismiss();
            callback.onConfirm(); // Call the onConfirm callback
        });

        btnCancel.setOnClickListener(view -> {
            dialog.dismiss();
            callback.onCancel();
        });
        dialog.show();
    }

    public boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            Network network = connectivityManager.getActiveNetwork();
            if (network != null) {
                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(network);
                return networkCapabilities != null &&
                        (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
            }
        }
        return false;
    }

    public String getDateTime(){
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM hh:mm a");
        return dateFormat.format(calendar.getTime());
    }

    public String getDate(){
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM");
        return dateFormat.format(calendar.getTime());
    }

    public void clickAnimation(View v){
        v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).withEndAction(new Runnable() {
            @Override
            public void run() {
                v.animate().scaleX(1f).scaleY(1f).setDuration(100);
            }
        });
    }

    public void showUploadProgressDialog(Context context, UploadTask uploadTask){
        // Create and configure the dialog
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.progress_dialog);
        dialog.setCancelable(false);
        Objects.requireNonNull(dialog.getWindow()).setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        ProgressBar progressBar = dialog.findViewById(R.id.progressBar);
        TextView tvProgress = dialog.findViewById(R.id.tvProgress);
        ConstraintLayout btnCancel = dialog.findViewById(R.id.btnCancel);
        ConstraintLayout btnHide = dialog.findViewById(R.id.btnHide);

        btnCancel.setOnClickListener(v -> {
            if (uploadTask != null) {
                uploadTask.cancel();
                tvProgress.setText("Upload Canceled");// Cancel the upload task
                new Handler().postDelayed(dialog::dismiss, 2000);    // Dismiss the dialog
            }
        });

        btnHide.setOnClickListener(v -> {
            dialog.dismiss(); // Just close the dialog, but let the upload continue
        });

        // Show the dialog
        dialog.show();

        uploadTask.addOnProgressListener(snapshot -> {
            double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
            progressBar.setProgress((int) progress);
            tvProgress.setText(String.format("Uploaded: %.2f%%", progress));
        }).addOnSuccessListener(snapshot -> {
            tvProgress.setText("Upload Complete!");
            new Handler().postDelayed(dialog::dismiss, 1000); // Dismiss after 1 second
        }).addOnFailureListener(e -> {
            if (e instanceof StorageException && ((StorageException) e).getErrorCode() == StorageException.ERROR_CANCELED) {
                tvProgress.setText("Upload Canceled");
            } else {
                tvProgress.setText("Upload Failed: " + e.getMessage());
            }
            new Handler().postDelayed(dialog::dismiss, 2000); // Dismiss after 2 seconds
        });

    }

    public void showBufferingDialog(Context context, String message) {
        // Inflate custom layout for the dialog
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_buffering, null);

        // Initialize views in the custom layout
        ProgressBar progressBar = dialogView.findViewById(R.id.progressBar);
        TextView textViewMessage = dialogView.findViewById(R.id.tvMessage);

        // Set the message
        textViewMessage.setText(message);

        // Create the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(dialogView);
        builder.setCancelable(false); // Prevent dialog from being dismissed by back press or touch outside

        bufferingDialog = builder.create();
        Objects.requireNonNull(bufferingDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        bufferingDialog.show();
    }

    public void dismissBufferingDialog() {
        if (bufferingDialog != null && bufferingDialog.isShowing()) {
            bufferingDialog.dismiss();
        }
    }

    public void showProfile(Context context, String userEmail){
        View dialogView = LayoutInflater.from(context).inflate(R.layout.show_profile, null);

        ImageView profileImage = dialogView.findViewById(R.id.profileImage);
        TextView name = dialogView.findViewById(R.id.tvName);
        TextView branch = dialogView.findViewById(R.id.tvBranch);
        TextView year = dialogView.findViewById(R.id.tvYear);
        TextView email = dialogView.findViewById(R.id.tvEmail);
        TextView phone = dialogView.findViewById(R.id.tvPhone);
        TextView rollNo = dialogView.findViewById(R.id.tvRollNo);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userEmail)
                .addSnapshotListener((snapshot, e)-> {
                    if (e != null){
                        return;
                    }
                    if (snapshot!= null && snapshot.exists()){
                        String userName = snapshot.getString("name");
                        String userBranch = snapshot.getString("branch");
                        String userYear = snapshot.getString("year");
                        String userPhone = snapshot.getString("mobileNumber");
                        String userImage = snapshot.getString("profileImage");
                        String userRoll = snapshot.getString("rollNo");
                        name.setText(userName);
                        branch.setText(userBranch);
                        year.setText(userYear);
                        email.setText(userEmail);
                        phone.setText(userPhone);
                        rollNo.setText(userRoll);
                        setProfileImage(context, userImage, profileImage);

                        profileImage.setOnClickListener( click -> {
                            navigateToProfileImage(context, userImage, userName, "noticeInfo");
                        });
                    }
                });

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(dialogView);
        AlertDialog profileDialog = builder.create();
        Objects.requireNonNull(profileDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        profileDialog.show();
    }

    public void navigateToProfileImage(Context context, int profileImageRes, String imageName, String categoryCode) {
        Intent intent = new Intent(context, activityProfileImage.class);
        intent.putExtra("profileImageRes", profileImageRes);
        intent.putExtra("imageName", imageName);
        intent.putExtra("categoryCode", categoryCode);
        context.startActivity(intent);
    }
    public void navigateToProfileImage(Context context, String imageUrl, String imageName, String categoryCode) {
        Intent intent = new Intent(context, activityProfileImage.class);
        intent.putExtra("imageUrl", imageUrl);
        intent.putExtra("imageName", imageName);
        intent.putExtra("categoryCode", categoryCode);
        context.startActivity(intent);
    }

    public void setProfileImage(Context context, String imageUrl, ImageView imageView) {
        // Load and cache the image using Glide
        Glide.with(context)
                .load(imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)// Cache both original and resized image
                .placeholder(R.drawable.ic_default_user)
                .error(R.drawable.default_icon)
                .fitCenter()
                .into(imageView);
    }

    public void replaceFragment (FragmentManager fragmentManager, Fragment targetFragment, int containerId){
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(
                R.anim.slide_in_right,  // enter animation
                R.anim.slide_out_left,  // exit animation
                R.anim.slide_in_left,   // pop enter (when returning back)
                R.anim.slide_out_right );
        transaction.replace(containerId, targetFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public String getFileName(Context context, Uri uri) {
        String fileName = null;
        if (context != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            }
        }

        if (fileName == null) {
            fileName = uri.getLastPathSegment();// Fallback to the last segment if no name is found
        }
        return fileName;
    }

    public String getFileSize(Context context, Uri uri) {
        long fileSizeBytes = 0;

        // Check if the Uri is not null
        if (uri != null) {
            // Use a Cursor to retrieve file size
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                try {
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (sizeIndex != -1) {
                        cursor.moveToFirst();
                        fileSizeBytes = cursor.getLong(sizeIndex);
                    }
                } finally {
                    cursor.close();
                }
            }
        }

        // Format the size to KB or MB
        if (fileSizeBytes > 0) {
            if (fileSizeBytes < 1024) {
                return fileSizeBytes + " B"; // Bytes if less than 1 KB
            } else if (fileSizeBytes < 1024 * 1024) {
                return (fileSizeBytes / 1024) + " KB"; // In KB
            } else {
                double sizeInMB = fileSizeBytes / (1024.0 * 1024.0); // In MB
                return String.format("%.2f MB", sizeInMB);
            }
        } else {
            return "Unknown size";
        }
    }

    public void playSentSound(Context context) {
        SoundPool soundPool = new SoundPool(1, AudioManager.STREAM_NOTIFICATION, 0);
        int soundId = soundPool.load(context, R.raw.sent_sound_effect, 1);

        soundPool.setOnLoadCompleteListener((sp, id, status) -> {
            if (status == 0) {
                sp.play(soundId, 1, 1, 1, 0, 1f);
            }
        });
    }



//    public void playSentSound(Context context) {
//        MediaPlayer mediaPlayer = MediaPlayer.create(context, R.raw.sent_sound_effect);
//        mediaPlayer.start();
//
//        // Release the media player after playback finishes
//        mediaPlayer.setOnCompletionListener(mp -> {
//            mp.release();
//        });
//    }



    public long downloadFile(Context context, String fileUrl, File file) {
        File directory = file.getParentFile();
        if (directory != null && !directory.exists()) {
            directory.mkdirs();
        }
        long downloadID = 0;
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(fileUrl));
        request.setTitle(file.getName());
        request.setDescription("Downloading...");
        request.setDestinationUri(Uri.fromFile(file));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager != null) {
            downloadID = downloadManager.enqueue(request);
            Toast.makeText(context, "Downloading...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Download Manager not available", Toast.LENGTH_SHORT).show();
        }
        return downloadID;
    }

    public void openPdf(Context context, File file) {
        Uri fileUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
        if (file.exists()) {
            Log.d("PDF_URI", "File Exists: " + file.getAbsolutePath());
        } else {
            Log.e("PDF_ERROR", "File Does Not Exist at: " + file.getAbsolutePath());
        }

        Log.d("PDF URI", "Opening file: " + fileUri.toString()); // Debugging

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(fileUri, "application/pdf");
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NO_HISTORY);

        context.startActivity(intent);

    }

    public void openFile(Context context, File file) {
        Uri fileUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
        if (file.exists()) {
            Log.d("FILE_URI", "File Exists: " + file.getAbsolutePath());
        } else {
            Log.e("FILE_ERROR", "File Does Not Exist at: " + file.getAbsolutePath());
            return;
        }
        // Get MIME type automatically
        String mimeType = context.getContentResolver().getType(fileUri);
        if (mimeType == null) {
            mimeType = "application/octet-stream"; // Default fallback
        }
        Log.d("FILE URI", "Opening file: " + fileUri.toString()); // Debugging

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(fileUri, mimeType);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NO_HISTORY);
        try{
            context.startActivity(intent);
        }
        catch (Exception e){
            Toast.makeText(context, "No apps found to handle the request", Toast.LENGTH_SHORT).show();
        }

    }

    public void downloadFilePr(Context context, File file, String url, DownloadCallback callback) {
        // Extract directory path and filename from the File object
        String directoryPath = file.getParent();
        String fileName = file.getName();

        PRDownloader.download(url, directoryPath, fileName)
                .build()
                .start(new OnDownloadListener() {
                    @Override
                    public void onDownloadComplete() {
                        callback.onDownloadComplete();
                        openFile(context, file);
                    }

                    @Override
                    public void onError(com.downloader.Error error) {
                        Toast.makeText(context, "Download failed!", Toast.LENGTH_SHORT).show();
                        Log.e("DOWNLOAD", "onError: " + error.getConnectionException() );
                    }
                });
    }


}
