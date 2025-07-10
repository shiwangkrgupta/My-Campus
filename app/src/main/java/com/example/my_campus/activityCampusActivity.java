package com.example.my_campus;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class activityCampusActivity extends AppCompatActivity {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private utility ut = new utility();
    private List<campusActivityItem> campusActivityItemsList = new ArrayList<>();
    private ConstraintLayout btnSend, editIndicator;
    private EditText messageInput;
    private ImageView btnCancelEdit;
    private String editMessageDocID;
    private String intentKey, classActivityCollection, replySender, reply;
    private boolean isReplying;
    private  CollectionReference messageCollection;
    public static final String classActivity = "class_activity";
    public static final String campusActivity = "campus_activity";
    private campusActivityAdapter adapter;
    private DocumentReference editMessageDoc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_campus);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        ImageView btnBack = findViewById(R.id.btnBackCampus);
        btnSend = findViewById(R.id.btnSend);
        RecyclerView messageView = findViewById(R.id.campusActivityRecyclerView);
        ConstraintLayout messageInputParent = findViewById(R.id.messageInputParent);
        messageInput = findViewById(R.id.messageInputCampus);
        editIndicator = findViewById(R.id.editIndicator);
        btnCancelEdit = findViewById(R.id.btnCancelEdit);
        TextView activityTitle = findViewById(R.id.activityTitle);

        btnBack.setOnClickListener( click -> finish());

        Intent intent = getIntent();
        intentKey = intent.getStringExtra("key");




        if (intentKey.equals("campus_activity")){
            activityTitle.setText("Campus Activity");
            //Setting Message input section visibility only for admins
            db.collection("users").document(loginState.getUserEmail(this))
                    .addSnapshotListener( (snapshot, e) -> {
                        if (e != null){
                            Log.d("campusInputSection", Objects.requireNonNull(e.getMessage()));
                            return;
                        }
                        if (snapshot != null && snapshot.exists()){
                            if (snapshot.getString("role").equals("admin")){
                                messageInputParent.setVisibility(View.VISIBLE);
                            }
                            else {
                                messageInputParent.setVisibility(View.GONE);
                            }
                        }
                    });

        }
        else {
            activityTitle.setText("Class Activity");
            Map <String, String> branchMap = new HashMap<>();
            branchMap.put("Computer Science & Engineering", "cse");
            branchMap.put("Civil Engineering", "civil");
            branchMap.put("Automobile Engineering", "auto");
            branchMap.put("Electrical Engineering", "electrical");
            branchMap.put("Electronics Engineering", "electronic");
            branchMap.put("Mechanical Engineering", "mech");

            Map <String, String> yearMap = new HashMap<>();
            yearMap.put("First Year", "first");
            yearMap.put("Second Year", "second");
            yearMap.put("Third Year", "third");

            classActivityCollection = "class activity " + branchMap.get(loginState.getUserBranch(this))+ " " + yearMap.get(loginState.getUserYear(this));
        }

        btnSend.setOnClickListener( click -> {
            ut.clickAnimation(click);
            if (editIndicator.getVisibility() == View.VISIBLE){
                sendEditedMessage(editMessageDocID);
            }
            else {
                sendMessage();
            }

        });

        btnCancelEdit.setOnClickListener( click -> {
            ut.clickAnimation(click);
            editIndicator.setVisibility(View.GONE);
            messageInput.setText("");
        });


        //Set status bar and navigation bar color
        Window window = getWindow();
        // Set status bar color
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.lightGrey));
        // Set navigation bar color
        window.setNavigationBarColor(ContextCompat.getColor(this, R.color.lightGrey));



        //Setting Recycler view adapter
        if (intentKey.equals("campus_activity")){
            adapter = new campusActivityAdapter(this, campusActivityItemsList, campusActivity, editMessageDocID ->{
                this.editMessageDocID = editMessageDocID;
                getMessageToEdit(editMessageDocID);
            });
        }
        else {
            adapter = new campusActivityAdapter(this, campusActivityItemsList, classActivity, classActivityCollection, editMessageDocID ->{
                this.editMessageDocID = editMessageDocID;
                getMessageToEdit(editMessageDocID);
            });

        }
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        messageView.setLayoutManager(layoutManager);
        messageView.setAdapter(adapter);
        if (adapter.getItemCount() > 0) {
            messageView.scrollToPosition(adapter.getItemCount() - 1);
        }

        // Swipe to Reply setup
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder){
                return 0.2f; //Just 20% swipe needed
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                campusActivityItem item = campusActivityItemsList.get(position);

                // Set reply values
                reply = item.getMessage();
                String replySenderEmail = item.getSentBy();
                isReplying = true;

                // Input field focus and keyboard show
                messageInput.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(messageInput, InputMethodManager.SHOW_IMPLICIT);
                }

                // Swipe reset (so item doesn't disappear)
                adapter.notifyItemChanged(position);

                // Fetch sender name from Firestore
                db.collection("users").document(replySenderEmail)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            replySender = documentSnapshot.exists() && documentSnapshot.getString("name") != null
                                    ? documentSnapshot.getString("name")
                                    : replySenderEmail;

                        })
                        .addOnFailureListener(e -> {
                            replySender = replySenderEmail;
                        });
            }

        };
        new ItemTouchHelper(simpleCallback).attachToRecyclerView(messageView);


        //Fetching message documents
        if (intentKey.equals("campus_activity")){
            messageCollection =  db.collection("campus activity");
        }
        else {
            messageCollection = db.collection(classActivityCollection);
        }

                messageCollection.orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener( (queryDocumentSnapshots, e) -> {
                    if (e != null){
                        Log.d("activityMessage", e.getMessage());
                        return;
                    }
                    if (queryDocumentSnapshots != null){
                        int previousSize = campusActivityItemsList.size();
                        campusActivityItemsList.clear();
                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            String message = document.getString("message");
                            String sentBy = document.getString("sentBy");
                            String sentTime = document.getString("sentTime");
                            String docId = document.getId();
                            String replyMessage = document.getString("replyMessage");
                            String replySender = document.getString("replySender");

                            campusActivityItemsList.add(new campusActivityItem(sentBy, message, sentTime, docId, replyMessage, replySender));
                        }
                        adapter.notifyDataSetChanged();
                        // Scroll only if the new list size is greater (indicating an addition)
                        if (campusActivityItemsList.size() > previousSize) {
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                if (adapter.getItemCount() > 0) {
                                    messageView.smoothScrollToPosition(adapter.getItemCount() - 1);
                                }
                            }, 700); // Delay of 700ms
                        }
                    }
                });

    }

    private void sendMessage(){
        if (!ut.isNetworkAvailable(this)){
            return;
        }
        String message = messageInput.getText().toString();
        if (message.isEmpty()){
            return;
        }
        btnSend.setEnabled(false);
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("message", message);
        messageData.put("sentBy", loginState.getUserEmail(this));
        messageData.put("sentTime", ut.getDateTime());
        messageData.put("timestamp", System.currentTimeMillis());

        if (isReplying && reply != null && !reply.isEmpty()) {
            messageData.put("replyMessage", reply); // This should be the message text being replied to
            messageData.put("replySender", replySender); // The original sender of the message
            isReplying = false;
        }

        if (intentKey.equals("campus_activity")){
            db.collection("campus activity").add(messageData)
                    .addOnSuccessListener( unused -> {
                        messageInput.setText("");
                        ut.playSentSound(this);
                        btnSend.setEnabled(true);
                        Toast.makeText(this, "Sent", Toast.LENGTH_SHORT).show();
                        sendNotificationToAllUsers(messageData.get("message").toString());
                    })
                    .addOnFailureListener(e -> {
                        btnSend.setEnabled(true);
                        Toast.makeText(this, "Error while sending message", Toast.LENGTH_SHORT).show();
                    });
        }
        else {
            db.collection(classActivityCollection).add(messageData)
                    .addOnSuccessListener( unused -> {
                        messageInput.setText("");
                        ut.playSentSound(this);
                        btnSend.setEnabled(true);
                        Toast.makeText(this, "Sent", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        btnSend.setEnabled(true);
                        Toast.makeText(this, "Error while sending message", Toast.LENGTH_SHORT).show();
                    });
        }

    }

    private void getMessageToEdit(String editMessageDocID){
        if (intentKey.equals("campus_activity")){
            editMessageDoc = db.collection("campus activity").document(editMessageDocID);
        }
        else {
            editMessageDoc = db.collection(classActivityCollection).document(editMessageDocID);
        }
                editMessageDoc.get()
                .addOnSuccessListener( snap -> {
                    if (snap!= null && snap.exists()){
                        messageInput.setText(snap.getString("message"));
                        messageInput.requestFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                        imm.showSoftInput(messageInput, InputMethodManager.SHOW_IMPLICIT);
                        editIndicator.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void sendEditedMessage(String docId){
        if (!ut.isNetworkAvailable(this)){
            return;
        }
        String message = messageInput.getText().toString();
        if (message.isEmpty()){
            return;
        }
        btnSend.setEnabled(false);
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("message", message);
        messageData.put("sentTime", "edited "+ut.getDateTime());

                editMessageDoc.update(messageData)
                .addOnSuccessListener( unused -> {
                    messageInput.setText("");
                    ut.playSentSound(this);
                    btnSend.setEnabled(true);
                    editIndicator.setVisibility(View.GONE);
                    Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener( e -> {
                    btnSend.setEnabled(true);
                    Toast.makeText(this, "Error while updating message", Toast.LENGTH_SHORT).show();
                });
    }

    private void sendNotificationToAllUsers(String messageText) {
        FirebaseFirestore.getInstance().collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String token = doc.getString("token");
                        if (token != null) {
                            sendFCMNotification(token, messageText);
                        }
                    }
                });
    }

    private void sendFCMNotification(String token, String messageText) {
        OkHttpClient client = new OkHttpClient();

        JSONObject json = new JSONObject();
        JSONObject notification = new JSONObject();

        try {
            notification.put("title", "New Message");
            notification.put("body", messageText);
            json.put("to", token);
            json.put("notification", notification);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url("https://fcm.googleapis.com/fcm/send")
                    .addHeader("Authorization", "key=YOUR_SERVER_KEY_HERE")
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e("FCM_API", "Notification failed", e);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    Log.d("FCM_API", "FCM API: " + response.body().string());
                }
            });

        } catch (Exception e) {
            Log.d("FCM_API_ERROR", "sendFCMNotification: "+ e.getMessage());
        }
    }

}