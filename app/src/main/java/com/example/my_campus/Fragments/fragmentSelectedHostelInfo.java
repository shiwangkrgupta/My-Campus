package com.example.my_campus.Fragments;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.my_campus.HostelListAdapter;
import com.example.my_campus.HostellListItem;
import com.example.my_campus.ImageSliderAdapter;
import com.example.my_campus.R;
import com.example.my_campus.utility;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class fragmentSelectedHostelInfo extends Fragment {
    private TextView hostelName;
    private RecyclerView recyclerView;
    private ViewPager2 imageSlider;
    private FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private HostelListAdapter adapter;
    private ArrayList<HostellListItem> hostelList;
    private Handler sliderHandler = new Handler();
    private Runnable sliderRunnable;
    private utility ut = new utility();

    public fragmentSelectedHostelInfo() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_selected_hostel_info, container, false);

        hostelName = view.findViewById(R.id.hostel);
        recyclerView = view.findViewById(R.id.recyclerView);
        imageSlider = view.findViewById(R.id.imageSlider);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        hostelList = new ArrayList<>();
        adapter = new HostelListAdapter(getContext(), hostelList);
        recyclerView.setAdapter(adapter);

        Bundle bundle = getArguments();
        if (bundle != null) {
            String hostel = bundle.getString("hostel");
            if (hostel != null) {
                hostelName.setText(getHostelFullName(hostel));
                loadHostelData(hostel);
                loadImagesForHostel(hostel);
            }
        }
        return view;
    }

    private void loadHostelData(String hostel) {
        ut.showBufferingDialog(requireContext(), "Loading files");
        CollectionReference hostelRef = firestore.collection("Hostels").document(hostel).collection("People");

        hostelRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot snapshot, FirebaseFirestoreException error) {
                ut.dismissBufferingDialog();
                if (error != null) {
                    showToast("Error fetching data: " + error.getMessage());
                    return;
                }

                hostelList.clear();
                for (QueryDocumentSnapshot document : snapshot) {
                    String name = document.getString("name");
                    String designation = document.getString("designation");
                    String phoneNumber = document.getString("phoneNumber");
                    String iconUrl = document.getString("icon");

                    if (name != null && designation != null && phoneNumber != null) {
                        if (iconUrl != null && !iconUrl.isEmpty()) {
                            // 🔄 Preload icon image to Glide cache
                            Glide.with(requireContext())
                                    .load(iconUrl)
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .preload();
                        }
                        hostelList.add(new HostellListItem(name, iconUrl, designation, phoneNumber));
                    }
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void loadImagesForHostel(String hostel) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("hostel images").document(hostel);

        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                List<String> imageList = new ArrayList<>();
                imageList.add(documentSnapshot.getString("image1"));
                imageList.add(documentSnapshot.getString("image2"));
                imageList.add(documentSnapshot.getString("image3"));

                // Remove null or empty URLs
                imageList.removeAll(Collections.singleton(null));
                imageList.removeAll(Collections.singleton(""));

                // Preload all images into cache
                for (String url : imageList) {
                    Glide.with(requireContext())
                            .load(url)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .preload();
                }

                // Set up the slider adapter
                ImageSliderAdapter adapter = new ImageSliderAdapter(imageList);
                imageSlider.setAdapter(adapter);
                setFancyPageTransformer();
                startAutoSlide(imageList.size());
            }
        });
    }

    private void startAutoSlide(int size) {
        if (size == 0) return;

        sliderRunnable = new Runnable() {
            @Override
            public void run() {
                int currentItem = imageSlider.getCurrentItem();
                int nextItem = (currentItem + 1) % size;
                imageSlider.setCurrentItem(nextItem, true);
                sliderHandler.postDelayed(this, 5000);
            }
        };
        sliderHandler.postDelayed(sliderRunnable, 5000);
    }

    private void setFancyPageTransformer() {
        imageSlider.setPageTransformer(new ViewPager2.PageTransformer() {
            @Override
            public void transformPage(@NonNull View page, float position) {
                if (position < -1 || position > 1) {
                    page.setAlpha(0f); // Off-screen pages invisible
                } else {
                    // Zoom out and fade effect
                    page.setScaleX(0.85f + (1 - Math.abs(position)) * 0.15f);
                    page.setScaleY(0.85f + (1 - Math.abs(position)) * 0.15f);
                    page.setAlpha(0.5f + (1 - Math.abs(position)) * 0.5f);
                }
            }
        });
    }



    private String getHostelFullName(String hostel) {
        switch (hostel) {
            case "aryabhata": return "Aryabhatta Girls Hostel";
            case "chanakya": return "Chanakya Boys Hostel";
            case "buddha": return "Buddha Boys Hostel";
            case "godavari": return "Godavari Boys Hostel";
            default: return "Unknown Hostel";
        }
    }

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}
