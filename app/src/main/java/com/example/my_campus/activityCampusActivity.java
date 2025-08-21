package com.example.my_campus;

import android.content.Intent;
import android.graphics.Canvas;
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
import com.google.firebase.messaging.FirebaseMessaging;

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

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final utility ut = new utility();

    private List<campusActivityItem> campusActivityItemsList = new ArrayList<>();
    private campusActivityAdapter adapter;

    private ConstraintLayout btnSend, editIndicator;

    // 🔽 Reply preview (bottom bar) views from activity_campus.xml
    private ConstraintLayout replyIndicator;        // id: replyIndicatorCampus
    private TextView tvReplyTo;                      // id: tvReplyTo
    private TextView tvReplyMessage;                 // id: tvReplyQuestion
    private ImageView btnCancelReply;                // id: btnCancelReply

    private EditText messageInput;
    private ImageView btnCancelEdit;

    private String editMessageDocID;
    private String intentKey, classActivityCollection;

    // Current reply data
    private boolean isReplying = false;
    private String replyText = null;         // message you're replying to
    private String replySenderName = null;   // name/email of original sender

    private CollectionReference messageCollection;
    public static final String classActivity = "class_activity";
    public static final String campusActivity = "campus_activity";

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

        // ✅ Reply preview init (use IDs from activity_campus.xml)
        replyIndicator   = findViewById(R.id.replyIndicatorCampus);
        tvReplyTo        = findViewById(R.id.tvReplyTo);
        tvReplyMessage   = findViewById(R.id.tvReplyQuestion);
        btnCancelReply   = findViewById(R.id.btnCancelReply);

        btnBack.setOnClickListener(click -> finish());

        Intent intent = getIntent();
        intentKey = intent.getStringExtra("key");

        if ("campus_activity".equals(intentKey)) {
            activityTitle.setText("Campus Activity");

            db.collection("users").document(loginState.getUserEmail(this))
                    .addSnapshotListener((snapshot, e) -> {
                        if (e != null) {
                            Log.d("campusInputSection", Objects.requireNonNull(e.getMessage()));
                            return;
                        }
                        if (snapshot != null && snapshot.exists()) {
                            if ("admin".equals(snapshot.getString("role"))) {
                                messageInputParent.setVisibility(View.VISIBLE);
                            } else {
                                messageInputParent.setVisibility(View.GONE);
                            }
                        }
                    });

        } else {
            activityTitle.setText("Class Activity");

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

            classActivityCollection = "class activity " +
                    branchMap.get(loginState.getUserBranch(this)) + " " +
                    yearMap.get(loginState.getUserYear(this));
        }

        btnSend.setOnClickListener(click -> {
            ut.clickAnimation(click);
            if (editIndicator.getVisibility() == View.VISIBLE) {
                sendEditedMessage(editMessageDocID);
            } else {
                sendMessage();
            }
        });

        btnCancelEdit.setOnClickListener(click -> {
            ut.clickAnimation(click);
            editIndicator.setVisibility(View.GONE);
            messageInput.setText("");
        });

        // Cancel reply preview
        btnCancelReply.setOnClickListener(v -> {
            hideReplyPreview();
            isReplying = false;
            replyText = null;
            replySenderName = null;
        });

        // System bars
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.lightGrey));
        window.setNavigationBarColor(ContextCompat.getColor(this, R.color.lightGrey));

        // RecyclerView + adapter
        if ("campus_activity".equals(intentKey)) {
            adapter = new campusActivityAdapter(this, campusActivityItemsList, campusActivity, editMessageDocID -> {
                this.editMessageDocID = editMessageDocID;
                getMessageToEdit(editMessageDocID);
            });
        } else {
            adapter = new campusActivityAdapter(this, campusActivityItemsList, classActivity, classActivityCollection, editMessageDocID -> {
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

        // Swipe-to-reply
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            @Override public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder vh) { return 0.4f; }
            @Override public float getSwipeEscapeVelocity(float d) { return Float.MAX_VALUE; }
            @Override public float getSwipeVelocityThreshold(float d) { return Float.MAX_VALUE; }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                float max = vh.itemView.getWidth() * 0.2f;
                if (dX > max) dX = max;
                super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive);
            }

            @Override public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder v1, @NonNull RecyclerView.ViewHolder v2) { return false; }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                campusActivityItem item = campusActivityItemsList.get(position);

                // collect reply info
                replyText = item.getMessage();
                String replySenderEmail = item.getSentBy();
                isReplying = true;

                // show preview
                tvReplyMessage.setText(replyText);
                replyIndicator.setVisibility(View.VISIBLE);

                db.collection("users").document(replySenderEmail)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists() && documentSnapshot.contains("name")) {
                                replySenderName = documentSnapshot.getString("name");
                            } else {
                                replySenderName = replySenderEmail;
                            }
                            tvReplyTo.setText(replySenderName);
                        })
                        .addOnFailureListener(e -> {
                            replySenderName = replySenderEmail;
                            tvReplyTo.setText(replySenderName);
                        });

                // focus input + keyboard
                messageInput.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(messageInput, InputMethodManager.SHOW_IMPLICIT);

                // reset swipe visual
                adapter.notifyItemChanged(position);
            }
        };
        new ItemTouchHelper(simpleCallback).attachToRecyclerView(messageView);

        // Load messages
        if ("campus_activity".equals(intentKey)) {
            messageCollection = db.collection("campus activity");
        } else {
            messageCollection = db.collection(classActivityCollection);
        }

        messageCollection.orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.d("activityMessage", e.getMessage());
                        return;
                    }
                    if (snapshots != null) {
                        int prev = campusActivityItemsList.size();
                        campusActivityItemsList.clear();
                        for (DocumentSnapshot document : snapshots.getDocuments()) {
                            String message = document.getString("message");
                            String sentBy = document.getString("sentBy");
                            String sentTime = document.getString("sentTime");
                            String docId = document.getId();
                            String rMsg = document.getString("replyMessage");
                            String rSender = document.getString("replySender");

                            campusActivityItemsList.add(new campusActivityItem(sentBy, message, sentTime, docId, rMsg, rSender));
                        }
                        adapter.notifyDataSetChanged();
                        if (campusActivityItemsList.size() > prev) {
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                if (adapter.getItemCount() > 0) {
                                    messageView.smoothScrollToPosition(adapter.getItemCount() - 1);
                                }
                            }, 700);
                        }
                    }
                });

        // Save FCM token
        saveFCMTokenToFirestore();
    }

    private void hideReplyPreview() {
        replyIndicator.setVisibility(View.GONE);
        tvReplyTo.setText("");
        tvReplyMessage.setText("");
    }

    private void sendMessage() {
        if (!ut.isNetworkAvailable(this)) return;

        String message = messageInput.getText().toString().trim();
        if (message.isEmpty()) return;

        btnSend.setEnabled(false);
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("message", message);
        messageData.put("sentBy", loginState.getUserEmail(this));
        messageData.put("sentTime", ut.getDateTime());
        messageData.put("timestamp", System.currentTimeMillis());

        // attach reply metadata if any
        if (isReplying && replyText != null && !replyText.isEmpty()) {
            messageData.put("replyMessage", replyText);
            messageData.put("replySender", replySenderName);
            isReplying = false;
            hideReplyPreview();
            replyText = null;
            replySenderName = null;
        }

        if ("campus_activity".equals(intentKey)) {
            db.collection("campus activity").add(messageData)
                    .addOnSuccessListener(unused -> {
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
        } else {
            db.collection(classActivityCollection).add(messageData)
                    .addOnSuccessListener(unused -> {
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

    private void getMessageToEdit(String editMessageDocID) {
        if ("campus_activity".equals(intentKey)) {
            editMessageDoc = db.collection("campus activity").document(editMessageDocID);
        } else {
            editMessageDoc = db.collection(classActivityCollection).document(editMessageDocID);
        }
        editMessageDoc.get()
                .addOnSuccessListener(snap -> {
                    if (snap != null && snap.exists()) {
                        messageInput.setText(snap.getString("message"));
                        messageInput.requestFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                        if (imm != null) imm.showSoftInput(messageInput, InputMethodManager.SHOW_IMPLICIT);
                        editIndicator.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void sendEditedMessage(String docId) {
        if (!ut.isNetworkAvailable(this)) return;

        String message = messageInput.getText().toString().trim();
        if (message.isEmpty()) return;

        btnSend.setEnabled(false);
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("message", message);
        messageData.put("sentTime", "edited " + ut.getDateTime());

        editMessageDoc.update(messageData)
                .addOnSuccessListener(unused -> {
                    messageInput.setText("");
                    ut.playSentSound(this);
                    btnSend.setEnabled(true);
                    editIndicator.setVisibility(View.GONE);
                    Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
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

    private void saveFCMTokenToFirestore() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String token = task.getResult();
                        String userEmail = loginState.getUserEmail(this);
                        if (userEmail != null && !userEmail.isEmpty()) {
                            FirebaseFirestore.getInstance().collection("users")
                                    .document(userEmail)
                                    .update("token", token)
                                    .addOnSuccessListener(aVoid -> Log.d("FCM_TOKEN", "Token saved"))
                                    .addOnFailureListener(e -> Log.e("FCM_TOKEN", "Error: " + e.getMessage()));
                        }
                    } else {
                        Log.e("FCM_TOKEN", "Token fetch failed");
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
                @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e("FCM_API", "Notification failed", e);
                }
                @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    Log.d("FCM_API", "FCM API: " + response.body().string());
                }
            });

        } catch (Exception e) {
            Log.d("FCM_API_ERROR", "sendFCMNotification: " + e.getMessage());
        }
    }
}
