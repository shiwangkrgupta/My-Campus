package com.example.my_campus.Fragments;

import com.bumptech.glide.Glide;
import com.example.my_campus.R;
import com.example.my_campus.activityCampusActivity;
import com.example.my_campus.activityHelp;
import com.example.my_campus.loginState;
import com.example.my_campus.noticeListAdapter;
import com.example.my_campus.noticeListItems;
import com.example.my_campus.utility;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class fragmentHomepage extends Fragment {
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private ArrayList<noticeListItems> noticeItemsArrayList = new ArrayList<>();
    private noticeListAdapter adapter;
    private TextView classActivityMessage, classActivityUpdated, question, questionReply, likeCount;
    private utility ut = new utility();
    private String helpDocId;
    private String classActivityCollection;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_homepage, container, false);

        Context context = getContext();
        if (context == null) return view;

        TextView campusActivityMessage = view.findViewById(R.id.campusActivityMessage);
        TextView campusActivityUpdated = view.findViewById(R.id.campusActivityUpdated);
        ImageView btnCampusInfo = view.findViewById(R.id.btnCampusInfo);
        classActivityMessage = view.findViewById(R.id.classActivityMessage);
        classActivityUpdated = view.findViewById(R.id.classActivityUpdated);
        ImageView btnClassInfo = view.findViewById(R.id.btnClassInfo);

        Map<String, String> branchMap = new HashMap<>();
        branchMap.put("Computer Science & Engineering", "cse");
        branchMap.put("Civil Engineering", "civil");
        branchMap.put("Automobile Engineering", "auto");
        branchMap.put("Electrical Engineering", "electrical");
        branchMap.put("Electronics Engineering", "electronic");
        branchMap.put("Mechanical Engineering", "mech");

        Map<String, String> yearMap = new HashMap<>();
        yearMap.put("First Year", "first");
        yearMap.put("Second Year", "second");
        yearMap.put("Third Year", "third");

        classActivityCollection = "class activity " + branchMap.get(loginState.getUserBranch(context)) + " " + yearMap.get(loginState.getUserYear(context));

        firestore.collection("campus activity")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || getContext() == null) return;

                    if (snapshot != null && !snapshot.getDocuments().isEmpty()) {
                        DocumentSnapshot messageDoc = snapshot.getDocuments().get(0);
                        if (messageDoc != null && messageDoc.exists()) {
                            btnCampusInfo.setVisibility(View.VISIBLE);
                            campusActivityMessage.setText(messageDoc.getString("message"));
                            campusActivityUpdated.setText(messageDoc.getString("sentTime"));
                            String sender = messageDoc.getString("sentBy");

                            firestore.collection("users").document(sender)
                                    .addSnapshotListener((snap, ex) -> {
                                        if (ex != null || getContext() == null) return;
                                        if (snap != null && snap.contains("profileImage")) {
                                            Context ctx = getContext();
                                            if (ctx != null) {
                                                Glide.with(ctx)
                                                        .load(snap.getString("profileImage"))
                                                        .circleCrop()
                                                        .error(R.drawable.ic_default_user)
                                                        .placeholder(R.drawable.ic_default_user)
                                                        .into(btnCampusInfo);
                                            }
                                        }
                                    });

                            btnCampusInfo.setOnClickListener(click -> {
                                Context ctx = getContext();
                                if (ctx != null) ut.showProfile(ctx, sender);
                            });
                        }
                    } else {
                        btnCampusInfo.setVisibility(View.GONE);
                        campusActivityMessage.setText("No message yet");
                        campusActivityUpdated.setText("");
                    }
                });

        firestore.collection(classActivityCollection)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || getContext() == null) return;

                    if (snapshot != null && !snapshot.getDocuments().isEmpty()) {
                        DocumentSnapshot messageDoc = snapshot.getDocuments().get(0);
                        if (messageDoc != null && messageDoc.exists()) {
                            btnClassInfo.setVisibility(View.VISIBLE);
                            classActivityMessage.setText(messageDoc.getString("message"));
                            classActivityUpdated.setText(messageDoc.getString("sentTime"));
                            String sender = messageDoc.getString("sentBy");

                            firestore.collection("users").document(sender)
                                    .addSnapshotListener((snap, ex) -> {
                                        if (ex != null || getContext() == null) return;
                                        if (snap != null && snap.contains("profileImage")) {
                                            Context ctx = getContext();
                                            if (ctx != null) {
                                                Glide.with(ctx)
                                                        .load(snap.getString("profileImage"))
                                                        .circleCrop()
                                                        .error(R.drawable.ic_default_user)
                                                        .placeholder(R.drawable.ic_default_user)
                                                        .into(btnClassInfo);
                                            }
                                        }
                                    });

                            btnClassInfo.setOnClickListener(click -> {
                                Context ctx = getContext();
                                if (ctx != null) ut.showProfile(ctx, sender);
                            });
                        }
                    } else {
                        btnClassInfo.setVisibility(View.GONE);
                        classActivityMessage.setText("No message yet");
                        classActivityUpdated.setText("");
                    }
                });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Context context = getContext();
        if (context == null) return;

        ListView noticeListView = view.findViewById(R.id.noticeListView);
        LinearLayout helpTile = view.findViewById(R.id.helpTile);
        LinearLayout campusActivityTile = view.findViewById(R.id.campusActivityTile);
        LinearLayout classActivityTile = view.findViewById(R.id.classActivityTile);
        TextView editTextHelpHome = view.findViewById(R.id.editTextHelpHome);
        question = view.findViewById(R.id.tvQuestion);
        questionReply = view.findViewById(R.id.tvQuestionReply);
        likeCount = view.findViewById(R.id.questionLikeCount);
        ImageView btnLikeQuestion = view.findViewById(R.id.btnLikeQuestion);
        TextView noticeIsEmpty = view.findViewById(R.id.noticeIsEmpty);

        adapter = new noticeListAdapter(context, noticeItemsArrayList);
        noticeListView.setAdapter(adapter);

        editTextHelpHome.setOnClickListener(click -> {
            Intent intent = new Intent(context, activityHelp.class);
            intent.putExtra("key", "clickedOnEditText");
            startActivity(intent);
        });

        campusActivityTile.setOnClickListener(click -> {
            Intent intent = new Intent(context, activityCampusActivity.class);
            intent.putExtra("key", "campus_activity");
            startActivity(intent);
        });

        classActivityTile.setOnClickListener(click -> {
            Intent intent = new Intent(context, activityCampusActivity.class);
            intent.putExtra("key", "class_activity");
            startActivity(intent);
        });

        helpTile.setOnClickListener(click -> {
            Intent intent = new Intent(context, activityHelp.class);
            startActivity(intent);
        });

        firestore.collection("notice pdfs")
                .orderBy("uploadId", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || getContext() == null) return;

                    if (snapshots != null) {
                        noticeItemsArrayList.clear();
                        if (snapshots.isEmpty()) {
                            noticeIsEmpty.setVisibility(View.VISIBLE);
                        } else {
                            noticeIsEmpty.setVisibility(View.GONE);
                            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                                noticeItemsArrayList.add(new noticeListItems(
                                        doc.getString("fileName"),
                                        doc.getString("uploadDate"),
                                        doc.getString("downloadUrl"),
                                        doc.getId(),
                                        doc.getString("uploadTime"),
                                        doc.getString("uploadedBy"),
                                        doc.getString("fileSize")
                                ));
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });

        firestore.collection("help")
                .orderBy("questionId", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null || getContext() == null) return;

                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        DocumentSnapshot helpDoc = querySnapshot.getDocuments().get(0);
                        if (helpDoc.exists()) {
                            likeCount.setVisibility(View.VISIBLE);
                            btnLikeQuestion.setVisibility(View.VISIBLE);
                            question.setText(helpDoc.getString("question"));
                            likeCount.setText(String.valueOf(helpDoc.getLong("likeCount")));
                            helpDocId = helpDoc.getId();

                            firestore.collection("help reply").document(helpDocId)
                                    .addSnapshotListener((snapshot, ex) -> {
                                        if (ex != null || getContext() == null) return;
                                        if (snapshot != null && snapshot.exists()) {
                                            long replyCount = snapshot.getLong("replyCount");
                                            if (replyCount > 0) {
                                                questionReply.setText("Reply : " + snapshot.getString("reply" + replyCount));
                                            } else {
                                                questionReply.setText("No reply yet");
                                            }
                                        }
                                    });

                            List<String> likedBy = (List<String>) helpDoc.get("likedBy");
                            String userEmail = loginState.getUserEmail(context);

                            if (likedBy != null && likedBy.contains(userEmail)) {
                                btnLikeQuestion.setColorFilter(Color.BLUE, PorterDuff.Mode.SRC_IN);
                                btnLikeQuestion.setOnClickListener(click -> {
                                    removeUserFromLike(helpDoc.getId(), context);
                                    long count = helpDoc.getLong("likeCount");
                                    firestore.collection("help").document(helpDoc.getId()).update("likeCount", count - 1);
                                });
                            } else {
                                btnLikeQuestion.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
                                btnLikeQuestion.setOnClickListener(click -> {
                                    addUserToLike(helpDoc.getId(), context);
                                    long count = helpDoc.getLong("likeCount");
                                    firestore.collection("help").document(helpDoc.getId()).update("likeCount", count + 1);
                                });
                            }
                        }
                    } else {
                        likeCount.setVisibility(View.INVISIBLE);
                        btnLikeQuestion.setVisibility(View.GONE);
                        question.setText("No question yet");
                        questionReply.setText("No reply yet");
                    }
                });
    }

    private void addUserToLike(String documentId, Context context) {
        firestore.collection("help").document(documentId)
                .update("likedBy", FieldValue.arrayUnion(loginState.getUserEmail(context)))
                .addOnSuccessListener(unused -> Toast.makeText(context, "Like added", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void removeUserFromLike(String documentId, Context context) {
        firestore.collection("help").document(documentId)
                .update("likedBy", FieldValue.arrayRemove(loginState.getUserEmail(context)))
                .addOnSuccessListener(unused -> Toast.makeText(context, "Like removed", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}