package com.example.my_campus.Fragments;

import static androidx.core.content.ContextCompat.getSystemService;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.my_campus.R;

import com.example.my_campus.loginState;
import com.example.my_campus.utility;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.content.Context;
import android.net.ConnectivityManager;
import androidx.fragment.app.FragmentActivity;

public class fragmentAdmin extends Fragment {

    utility ut = new utility();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

     View view = inflater.inflate(R.layout.fragment_admin, container, false);
     


     ConstraintLayout btnManageRoutine = view.findViewById(R.id.btnManageRoutine);
     ConstraintLayout btnManageNotices = view.findViewById(R.id.btnManageNotices);
     ConstraintLayout btnManagefaculties = view.findViewById(R.id.btnManageFaculties);
     ConstraintLayout btnManagehostel = view.findViewById(R.id.btnManageHostel);





        // Open Manage Routines Fragment
        btnManageRoutine.setOnClickListener( click -> {
            fragmentManageRoutines fragManageRoutines = new fragmentManageRoutines();
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.setCustomAnimations(
                    R.anim.slide_in_right,  // enter animation
                    R.anim.slide_out_left,  // exit animation
                    R.anim.slide_in_left,   // pop enter (when returning back)
                    R.anim.slide_out_right );
            transaction.replace(R.id.mainLayout, fragManageRoutines);
            transaction.addToBackStack(null);
            transaction.commit();
        });

        // Open Manage Notices Fragment
        btnManageNotices.setOnClickListener( click -> {
            ut.replaceFragment(getParentFragmentManager(), new fragementManageNotice(), R.id.mainLayout);
        });


        // Open Manage Faculties Fragment
        btnManagefaculties.setOnClickListener(click -> {
            fragmentManageFaculties fragManageFaculties = new fragmentManageFaculties();
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
            );
            transaction.replace(R.id.mainLayout, fragManageFaculties);
            transaction.addToBackStack(null);
            transaction.commit();
        });

        // Open Manage Hostel Fragment
        btnManagehostel.setOnClickListener(click -> {
            fragmentManageHostel fragManageHostel = new fragmentManageHostel();
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
            );
            transaction.replace(R.id.mainLayout, fragManageHostel);
            transaction.addToBackStack(null);
            transaction.commit();
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        Fragment fragment = getParentFragmentManager().findFragmentByTag("fragmentHomepage");

                        if (fragment == null || !fragment.isVisible()) {
                            // User is NOT on homepage → go to homepage
                            fragmentHomepage homePageFragment = new fragmentHomepage();
                            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
                            transaction.replace(R.id.mainLayout, homePageFragment, "fragmentHomepage");
                            transaction.commit();
                        } else {
                            // User is already on homepage → exit app
                            requireActivity().finish();
                        }
                    }
                }
        );
    }
}