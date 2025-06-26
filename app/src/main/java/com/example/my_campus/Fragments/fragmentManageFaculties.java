package com.example.my_campus.Fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.example.my_campus.R;
import com.example.my_campus.utility;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class fragmentManageFaculties extends Fragment {

    private EditText nameEdit, emailEdit, designationEdit, phoneEdit;
    private Spinner branchEdit;
    private ConstraintLayout uploadBtn;
    private TextView selectedImageName;
    private ImageView btnReset;
    utility ut = new utility();
    private Uri selectedImageUri, croppedImageUri;
    private FirebaseStorage storage;
    private FirebaseFirestore firestore;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_manage_faculties, container, false);

        // Bind views
        nameEdit = view.findViewById(R.id.nameEdit);
        emailEdit = view.findViewById(R.id.emailEdit);
        designationEdit = view.findViewById(R.id.designationEdit);
        phoneEdit = view.findViewById(R.id.phoneEdit);
        branchEdit = view.findViewById(R.id.branchEdit);
        selectedImageName = view.findViewById(R.id.selectedImageName);
        uploadBtn = view.findViewById(R.id.uploadBtn);
        btnReset = view.findViewById(R.id.btnReset);
        storage = FirebaseStorage.getInstance();
        firestore = FirebaseFirestore.getInstance();

        String[] branches = {"Select Branch", "CSE", "Mechanical", "Electrical", "Civil", "Electronics", "Automobile"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, branches);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        branchEdit.setAdapter(adapter);



        btnReset.setOnClickListener(v -> resetSelectedImage());
        if (selectedImageUri == null) btnReset.setVisibility(View.GONE);

        View selectImageBtn = view.findViewById(R.id.selectImageBtn);
        selectImageBtn.setOnClickListener(v -> openImagePicker());
        uploadBtn.setOnClickListener(v -> uploadFaculty());

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            cropImage();
                            btnReset.setVisibility(View.VISIBLE);
                            selectedImageName.setText(getFileNameFromUri(selectedImageUri));
                        }
                    }
                }
        );

        return view;
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        imagePickerLauncher.launch(intent);
    }

    private void resetSelectedImage() {
        selectedImageUri = null;
        croppedImageUri = null;
        selectedImageName.setText("");
        uploadBtn.setVisibility(View.GONE);
        btnReset.setVisibility(View.GONE);
    }

    private void resetAllFields() {
        nameEdit.setText("");
        emailEdit.setText("");
        designationEdit.setText("");
        phoneEdit.setText("");
        branchEdit.setSelection(0); // Reset to "Select Branch"
        resetSelectedImage();       // Clears image name, URI, hides buttons
    }


    private void cropImage() {
        Uri destinationUri = Uri.fromFile(new File(requireContext().getCacheDir(), "cropped.jpg"));
        UCrop.of(selectedImageUri, destinationUri)
                .withAspectRatio(1, 1)
                .start(requireContext(), this);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == UCrop.REQUEST_CROP && resultCode == Activity.RESULT_OK && data != null) {
            croppedImageUri = UCrop.getOutput(data);
            if (croppedImageUri != null) {
                uploadBtn.setVisibility(View.VISIBLE);
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            Throwable error = UCrop.getError(data);
            Toast.makeText(getContext(), "Crop error: " + (error != null ? error.getMessage() : "Unknown"), Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadFaculty() {
        String name = nameEdit.getText().toString().trim();
        String email = emailEdit.getText().toString().trim();
        String designation = designationEdit.getText().toString().trim();
        String phone = phoneEdit.getText().toString().trim();
        String selectedBranch = branchEdit.getSelectedItem().toString().trim().toLowerCase();
        String fullBranchName = getBranchFullName(selectedBranch);

        if (croppedImageUri == null || name.isEmpty() || email.isEmpty() ||
                designation.isEmpty() || phone.isEmpty() || selectedBranch.equals("select branch")) {
            Toast.makeText(getContext(), "All fields including image are required", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = "facultyImage/" + selectedBranch + "/" + name + ".jpg";
        StorageReference imageRef = storage.getReference().child(fileName);
        UploadTask uploadTask = imageRef.putFile(croppedImageUri); // or selectedImageUri if no crop

        // Show progress dialog (assuming ut.showUploadProgressDialog() is your custom utility)
        ut.showUploadProgressDialog(requireContext(), uploadTask);

        uploadTask
                .addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();
                            String path = "Faculties/" + selectedBranch + "/People/" + name;

                            Map<String, Object> data = new HashMap<>();
                            data.put("name", name);
                            data.put("email", email);
                            data.put("designation", designation);
                            data.put("phoneNumber", phone);
                            data.put("branch", fullBranchName);
                            data.put("icon", imageUrl);

                            firestore.document(path)
                                    .set(data)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(getContext(), "Faculty uploaded successfully", Toast.LENGTH_SHORT).show();
                                        resetAllFields();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());

                        }))
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());

    }

    private String getFileNameFromUri(Uri uri) {
        String result = "";
        try (android.database.Cursor cursor = requireContext().getContentResolver()
                .query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            }
        }
        return result;
    }

    private String getBranchFullName(String branch) {
        switch (branch) {
            case "automobile": return "Automobile Engineering";
            case "cse": return "Computer Science & Engineering";
            case "civil": return "Civil Engineering";
            case "electrical": return "Electrical Engineering";
            case "electronics": return "Electronics Engineering";
            case "mechanical": return "Mechanical Engineering";
            default: return branch;
        }
    }
}
