package com.example.my_campus;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.mindrot.jbcrypt.BCrypt;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class activityChangePassword extends AppCompatActivity {

    private EditText getOtp;
    private EditText getNewPassword;
    private EditText getConfirmPassword;
    private ConstraintLayout btnVerify, btnChange;
    private TextView otpButtonText;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String SentOTP;
    private final utility ut = new utility();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_change_password);

        EditText getEmail = findViewById(R.id.registerEmail);
        getOtp = findViewById(R.id.otp);
        getNewPassword = findViewById(R.id.newPassword);
        getConfirmPassword = findViewById(R.id.confirmPassword);
        otpButtonText = findViewById(R.id.textView32);
        btnVerify = findViewById(R.id.btnVerify);
        btnChange = findViewById(R.id.btnChange);




        btnVerify.setOnClickListener( click -> {
            ut.clickAnimation(btnVerify);
            ut.showBufferingDialog(this, "Sending OTP");
            String userEmail = getEmail.getText().toString();
            if (userEmail.isEmpty()) {
                Toast.makeText(this, "Invalid Email", Toast.LENGTH_SHORT).show();
                getEmail.requestFocus();
                getEmail.setError("Enter a valid registered email");
                return;
            }
            DocumentReference docref = db.collection("users").document(userEmail);
            docref.get().addOnSuccessListener( documentSnapshot -> {
                if (documentSnapshot.exists()){
                    String password = documentSnapshot.getString("password");
                    if (password != null && !password.isEmpty()){
                        sendOtp(userEmail);
                    }else {
                        Toast.makeText(this,"Not Registered", Toast.LENGTH_SHORT).show();
                    }
                }
                else {
                    Toast.makeText(this, "User Not Found, Please Check email", Toast.LENGTH_SHORT).show();
                };
            });
        });

        btnChange.setOnClickListener( click -> {
            ut.clickAnimation(btnChange);
            String otp = getOtp.getText().toString();
            String newPassword = getNewPassword.getText().toString();
            String confirmPassword = getConfirmPassword.getText().toString();
            String email = getEmail.getText().toString();
            
            if (otp.isEmpty()){
                getOtp.requestFocus();
                getOtp.setError("Enter valid 6 digit OTP");
                return;
            }
            
            if (newPassword.length() < 8){
                getNewPassword.requestFocus();
                getNewPassword.setError("Password must be 8 characters long");
                return;
            }

            Pattern alphabetPattern = Pattern.compile("[a-zA-Z]");
            Pattern numberPattern = Pattern.compile("[0-9]");
            Pattern specialCharPattern = Pattern.compile("[!@#$%^&]");

            Matcher hasAlphabet = alphabetPattern.matcher(newPassword);
            Matcher hasNumber = numberPattern.matcher(newPassword);
            Matcher hasSpecialChar = specialCharPattern.matcher(newPassword);

            if (!hasAlphabet.find() || !hasNumber.find() || !hasSpecialChar.find()){
                getNewPassword.setError("Password must contain Alpha-numeric and special characters");
                return;
            }

            if (!newPassword.equals(confirmPassword) || confirmPassword.isEmpty()){
                getConfirmPassword.requestFocus();
                getConfirmPassword.setError("Password did not match");
                return;
            }

            if (!otp.equals(SentOTP)){
                Toast.makeText(this, "Invalid OTP", Toast.LENGTH_SHORT).show();
                return;
            }

            ut.showBufferingDialog(this, "Changing Password");

            DocumentReference docref = db.collection("users").document(email);
            docref.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
                    docref.update("password", hashedPassword).addOnSuccessListener(unused -> {
                        ut.dismissBufferingDialog();
                        Toast.makeText(this, "Password Changed", Toast.LENGTH_SHORT).show();
                    }).addOnFailureListener(e -> {
                        ut.dismissBufferingDialog();
                        Toast.makeText(this, "Unable to change password " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }

//                if (documentSnapshot.exists()){
//                    docref.update("password", newPassword).addOnSuccessListener( unused -> {
//                        ut.dismissBufferingDialog();
//                        Toast.makeText(this, "Password Changed", Toast.LENGTH_SHORT).show();
//                    }).addOnFailureListener(e -> {
//                        ut.dismissBufferingDialog();
//                        Toast.makeText(this, "Unable to change password " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                    });
//                }
            }).addOnFailureListener(e -> {
                ut.dismissBufferingDialog();
                Toast.makeText(this, "Error " + e.getMessage() , Toast.LENGTH_SHORT).show();
            });
        });
    }


    private void sendOtp(String email){
        // Run OTP sending in a separate thread to avoid blocking the UI
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Python py = Python.getInstance();
                    PyObject pyobj = py.getModule("sendOtpOnEmail");
                    PyObject otp = pyobj.callAttr("send_email_otp", email);
                    String sentOtp = otp.toString();

                    // Update UI after OTP is sent or failed
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ut.dismissBufferingDialog();

                            if (!sentOtp.isEmpty()) {
                                SentOTP = sentOtp;
                                otpButtonText.setText("resend otp");

                                Toast.makeText(activityChangePassword.this, " OTP sent to " + email, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(activityChangePassword.this, "Failed to send OTP", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    // Handle any errors in the OTP sending process
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ut.dismissBufferingDialog();
                            Toast.makeText(activityChangePassword.this, "Error sending OTP", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    public static class ImageSliderAdapter extends RecyclerView.Adapter<ImageSliderAdapter.ViewHolder> {

        private final List<Integer> imageList;

        public ImageSliderAdapter(List<Integer> imageList) {
            this.imageList = imageList;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.slider_image_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.imageView.setImageResource(imageList.get(position));
        }

        @Override
        public int getItemCount() {
            return imageList.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.sliderImage);
            }
        }
    }
}