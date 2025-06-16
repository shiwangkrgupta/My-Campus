package com.example.my_campus.Fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.example.my_campus.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class fragmentManageHostel extends Fragment {
    private EditText nameEdit, designationEdit, phoneEdit;
    private Spinner hostelEdit;
    private ConstraintLayout uploadBtn;
    private ImageView selectedImage;
    private Uri selectedImageUri, croppedImageUri;
    private FirebaseStorage storage;
    private FirebaseFirestore firestore;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_hostel, container, false);

        nameEdit = view.findViewById(R.id.nameEdit);
        designationEdit = view.findViewById(R.id.designationEdit);
        phoneEdit = view.findViewById(R.id.phoneEdit);
        hostelEdit = view.findViewById(R.id.hostelEdit);

        // Setup Spinner
        String[] hostel = {"Select Hostel", "aryabhata", "buddha", "chanakya", "godavari"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, hostel);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        hostelEdit.setAdapter(adapter);

        //Buttons
        View selectImageBtn = view.findViewById(R.id.selectImageBtn);
        uploadBtn = view.findViewById(R.id.uploadBtn);
        selectedImage = view.findViewById(R.id.selectedImage);

        storage = FirebaseStorage.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize image picker launcher
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        cropImage();
                    }
                }
        );

        selectImageBtn.setOnClickListener(v -> openImagePicker());
        uploadBtn.setOnClickListener(v -> uploadStaff());
        return view;

    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        imagePickerLauncher.launch(intent);
    }

    private void cropImage() {
        Uri destinationUri = Uri.fromFile(new File(requireContext().getCacheDir(), "cropped.jpg"));
        UCrop.of(selectedImageUri, destinationUri)
                .withAspectRatio(1, 1)
                .start(requireContext(), this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == UCrop.REQUEST_CROP && resultCode == Activity.RESULT_OK && data != null) {
            croppedImageUri = UCrop.getOutput(data);
            if (croppedImageUri != null) {
                selectedImage.setImageURI(croppedImageUri);
                uploadBtn.setVisibility(View.VISIBLE);
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            Throwable error = UCrop.getError(data);
            Toast.makeText(getContext(), "Crop error: " + (error != null ? error.getMessage() : "Unknown"), Toast.LENGTH_SHORT).show();
        }
    }


    private void uploadStaff() {
        String name = nameEdit.getText().toString().trim();
        String designation = designationEdit.getText().toString().trim();
        String phone = phoneEdit.getText().toString().trim();
        String selectedHostel = hostelEdit.getSelectedItem().toString().trim().toLowerCase();
        String fullHostelName = gethostelFullName(selectedHostel);

        if (croppedImageUri == null || name.isEmpty() ||
                designation.isEmpty() || phone.isEmpty()) {
            Toast.makeText(getContext(), "All fields including image are required", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = "staffImage/" + selectedHostel + "/" + name + ".jpg";
        StorageReference imageRef = storage.getReference().child(fileName);

        imageRef.putFile(croppedImageUri)
                .addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();
                            String path = "Hostels/" + selectedHostel + "/People/" + name;

                            Map<String, Object> data = new HashMap<>();
                            data.put("name", name);
                            data.put("designation", designation);
                            data.put("phoneNumber", phone);
                            data.put("icon", imageUrl);

                            firestore.document(path)
                                    .set(data)
                                    .addOnSuccessListener(aVoid ->
                                            Toast.makeText(getContext(), "Staff uploaded successfully", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e ->
                                            Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }))
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private String gethostelFullName(String hostel) {
        switch (hostel) {
            case "aryabhata": return "Aryabhatta Girls Hostel";
            case "buddha": return "Buddha Boys Hostel";
            case "chanakaya": return "Chanakya Boys Hostel";
            case "godavari": return "Godavari Boys Hostel";
            default: return hostel;
        }
    }
}
