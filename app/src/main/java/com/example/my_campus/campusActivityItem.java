package com.example.my_campus;

public class campusActivityItem {
    private String senderEmail;
    private String messageBody;
    private String sentTime;
    private String docID;
    private String replyMessage;
    private String replySender;

    // Constructor without reply (for older messages)
    public campusActivityItem(String senderEmail, String messageBody, String sentTime, String docID) {
        this.senderEmail = senderEmail;
        this.messageBody = messageBody;
        this.sentTime = sentTime;
        this.docID = docID;
    }

    // Constructor with reply
    public campusActivityItem(String senderEmail, String messageBody, String sentTime, String docID, String replyMessage, String replySender) {
        this.senderEmail = senderEmail;
        this.messageBody = messageBody;
        this.sentTime = sentTime;
        this.docID = docID;
        this.replyMessage = replyMessage;
        this.replySender = replySender;
    }

    // Original getters
    public String getSenderEmail() {
        return senderEmail;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public String getSentTime() {
        return sentTime;
    }

    public String getDocID() {
        return docID;
    }

    public String getReplyMessage() {
        return replyMessage;
    }

    public String getReplySender() {
        return replySender;
    }

    // ✅ Additional methods to support getMessage() and getSentBy()
    public String getMessage() {
        return messageBody;
    }

    public String getSentBy() {
        return senderEmail;
    }
}
