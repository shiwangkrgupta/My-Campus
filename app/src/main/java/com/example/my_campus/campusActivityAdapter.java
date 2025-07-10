package com.example.my_campus;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class campusActivityAdapter extends RecyclerView.Adapter<campusActivityAdapter.MessageViewHolder> {

    private Context context;
    private List<campusActivityItem> messageList;
    private FirebaseFirestore db;
    private utility ut = new utility();
    private editMessageCallback callback;
    private String adapterMode;
    private String classActivityCollection;

    public campusActivityAdapter(Context context, List<campusActivityItem> messageList, String adapterMode, editMessageCallback callback) {
        setHasStableIds(true);
        this.context = context;
        this.messageList = messageList;
        this.db = FirebaseFirestore.getInstance();
        this.callback = callback;
        this.adapterMode = adapterMode;
    }

    public campusActivityAdapter(Context context, List<campusActivityItem> messageList, String adapterMode, String classActivityCollection, editMessageCallback callback) {
        setHasStableIds(true);
        this.context = context;
        this.messageList = messageList;
        this.db = FirebaseFirestore.getInstance();
        this.callback = callback;
        this.adapterMode = adapterMode;
        this.classActivityCollection = classActivityCollection;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.campus_activity_list_item, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        campusActivityItem currentItem = messageList.get(position);
        holder.msgBody.setText(currentItem.getMessageBody());
        holder.sentTime.setText(currentItem.getSentTime());

        if (isUserSender(currentItem.getSenderEmail())) {
            holder.messageLayout.setBackgroundResource(R.drawable.registration_details_background);
            holder.messageLayoutParent.setGravity(Gravity.END);
        } else {
            holder.messageLayout.setBackgroundResource(R.drawable.rounded_ractangle_1);
            holder.messageLayoutParent.setGravity(Gravity.START);
        }

        if (position > 0) {
            campusActivityItem previousItem = messageList.get(position - 1);
            if (previousItem.getSenderEmail().equals(currentItem.getSenderEmail()) || isUserSender(currentItem.getSenderEmail())) {
                holder.senderName.setVisibility(View.GONE);
                holder.imageLayout.setVisibility(View.INVISIBLE);
                holder.imageLayout.setEnabled(false);
            } else {
                holder.senderName.setVisibility(View.VISIBLE);
                holder.imageLayout.setVisibility(View.VISIBLE);
                holder.imageLayout.setEnabled(true);
            }
        } else {
            holder.senderName.setVisibility(isUserSender(currentItem.getSenderEmail()) ? View.GONE : View.VISIBLE);
            holder.imageLayout.setVisibility(isUserSender(currentItem.getSenderEmail()) ? View.GONE : View.VISIBLE);
        }

        holder.imageLayout.setOnClickListener(click -> {
            ut.clickAnimation(click);
            ut.showProfile(context, currentItem.getSenderEmail());
        });

        holder.itemView.setOnLongClickListener(view -> {
            showContextMenu(view, currentItem.getSenderEmail(), currentItem.getDocID());
            return true;
        });

        db.collection("users").document(currentItem.getSenderEmail())
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.d("campusActivityAdapter", "onBindViewHolder: " + e.getMessage());
                        return;
                    }
                    if (snapshot != null && snapshot.exists()) {
                        holder.senderName.setText(isUserSender(currentItem.getSenderEmail()) ? "You" : snapshot.getString("name"));
                        Glide.with(context)
                                .load(snapshot.getString("profileImage"))
                                .placeholder(R.drawable.ic_default_user)
                                .error(R.drawable.ic_default_user)
                                .into(holder.senderProfileImage);
                    }
                });

        // Bind reply message preview if available
        if (currentItem.getReplyMessage() != null && !currentItem.getReplyMessage().isEmpty()) {
            holder.replyLayout.setVisibility(View.VISIBLE);
            holder.replyMessage.setText(currentItem.getReplyMessage());
            if (currentItem.getReplySender() != null) {
                holder.replySender.setText(currentItem.getReplySender().equals(loginState.getUserEmail(context)) ? "You" : currentItem.getReplySender());
            } else {
                holder.replySender.setText("Unknown");
            }
        } else {
            holder.replyLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    @Override
    public long getItemId(int position) {
        return messageList.get(position).getDocID().hashCode();
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView senderName, msgBody, sentTime, replySender, replyMessage;
        ImageView senderProfileImage;
        ConstraintLayout messageLayout;
        LinearLayout messageLayoutParent, replyLayout;
        CardView imageLayout;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            senderName = itemView.findViewById(R.id.senderName);
            msgBody = itemView.findViewById(R.id.msgBody);
            sentTime = itemView.findViewById(R.id.sentTime);
            senderProfileImage = itemView.findViewById(R.id.senderProfileImage);
            messageLayoutParent = itemView.findViewById(R.id.messageLayoutParent);
            messageLayout = itemView.findViewById(R.id.messageLayout);
            imageLayout = itemView.findViewById(R.id.profile);
            replyLayout = itemView.findViewById(R.id.replyLayout);
            replySender = itemView.findViewById(R.id.replySender);
            replyMessage = itemView.findViewById(R.id.replyMessage);
        }
    }

    private boolean isUserSender(String senderEmail) {
        return senderEmail.equals(loginState.getUserEmail(context));
    }

    private void showContextMenu(View view, String sender, String docId) {
        PopupMenu popupMenu = new PopupMenu(context, view);
        popupMenu.inflate(R.menu.campus_context_menu);

        if (sender.equals(loginState.getUserEmail(context)) && loginState.getUserRole(context).equals("admin")) {
            MenuItem deleteItem = popupMenu.getMenu().findItem(R.id.menu_delete);
            MenuItem editItem = popupMenu.getMenu().findItem(R.id.menu_edit);
            if (deleteItem != null) {
                deleteItem.setVisible(true);
                deleteItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }
            if (editItem != null) {
                editItem.setVisible(true);
                editItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }
        }

        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_delete) {
                deleteMessage(docId);
                return true;
            }
            if (item.getItemId() == R.id.menu_edit) {
                callback.passMessageDocId(docId);
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void deleteMessage(String docId) {
        if (adapterMode.equals(activityCampusActivity.campusActivity)) {
            db.collection("campus activity").document(docId)
                    .delete()
                    .addOnSuccessListener(unused ->
                            Toast.makeText(context, "deleted", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(context, "Error While Deleting", Toast.LENGTH_SHORT).show());
        } else {
            db.collection(classActivityCollection).document(docId)
                    .delete()
                    .addOnSuccessListener(unused ->
                            Toast.makeText(context, "deleted", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(context, "Error While Deleting", Toast.LENGTH_SHORT).show());
        }
    }

    public interface editMessageCallback {
        void passMessageDocId(String docID);
    }
}