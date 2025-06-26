package com.example.my_campus.Fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.my_campus.CustomListAdapter;
import com.example.my_campus.HostellListItem;
import com.example.my_campus.ListItem;
import com.example.my_campus.R;
import com.example.my_campus.utility;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collection;

public class fragmentBranchFaculty extends Fragment {

    private TextView branchName;
    private RecyclerView recyclerView;
    private FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private CustomListAdapter adapter;
    private ArrayList<ListItem> facultyList;
    private utility ut = new utility();


    public fragmentBranchFaculty() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_branch_faculty, container, false);

        branchName = view.findViewById(R.id.branch);
        // adding items to cse faculties array list
        recyclerView = view.findViewById(R.id.facultyrecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        facultyList = new ArrayList<>();
        adapter = new CustomListAdapter(getContext(), facultyList);
        recyclerView.setAdapter(adapter);

        // getting bundle containing branch name from previous fragment
        Bundle bundle = getArguments();
        if (bundle != null) {
            String branch = bundle.getString(("branch"));
            if (branch!=null) {
                branchName.setText(getBranchFullName(branch));
                loadFacultyData(branch);
            }
        }
        return view;
    }

    private void loadFacultyData(String branch) {
        // Show the buffering dialog
        ut.showBufferingDialog(requireContext(), "Loading files"); // Assuming 'ut' is an existing utility class

        CollectionReference branchRef = firestore.collection("Faculties").document(branch).collection("People");

        branchRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot snapshot, FirebaseFirestoreException error) {
                // Dismiss the buffering dialog after data fetch is complete (either success or failure)
                ut.dismissBufferingDialog();

                if (error != null) {
                    showToast("Error fetching data: " + error.getMessage());
                    return;
                }

                facultyList.clear();
                for (QueryDocumentSnapshot document : snapshot) {
                    String name = document.getString("name");
                    String designation = document.getString("designation");
                    String phoneNumber = document.getString("phoneNumber");
                    String email = document.getString("email");
                    String iconUrl = document.getString("icon");

                    if (name != null && designation != null && phoneNumber != null) {
                        if (iconUrl != null && !iconUrl.isEmpty()) {
                            // 🔄 Preload icon image to Glide cache
                            Glide.with(requireContext())
                                    .load(iconUrl)
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .preload();
                        }
                        facultyList.add(new ListItem(name, iconUrl, designation, phoneNumber, email));
                    }
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    private String getBranchFullName(String branch) {
        switch (branch) {
            case "automobile": return "Automobile Engineering";
            case "cse": return "Computer Science & Engineering";
            case "civil": return "Civil Engineering";
            case "electrical": return "Electrical Engineering";
            case "electronics": return "Electronics Engineering";
            case "mechanical": return "Mechanical Engineering";
            default: return "Unknown Branch";

        }
    }
    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}