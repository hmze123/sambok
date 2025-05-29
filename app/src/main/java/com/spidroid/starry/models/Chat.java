package com.spidroid.starry.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Chat {
    private String id;
    private List<String> participants;
    private String lastMessage;
    private String lastMessageType;
    private String lastMessageSender;
    
    @ServerTimestamp
    private Date lastMessageTime;
    
    private Map<String, Integer> unreadCounts = new HashMap<>();

    @PropertyName("isGroup")
    private boolean isGroup;

    private String groupName;
    private String groupImage;

    public Chat() {}

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getLastMessageType() {
        return lastMessageType;
    }

    public void setLastMessageType(String lastMessageType) {
        this.lastMessageType = lastMessageType;
    }

    public String getLastMessageSender() {
        return lastMessageSender;
    }

    public void setLastMessageSender(String lastMessageSender) {
        this.lastMessageSender = lastMessageSender;
    }

    public Date getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(Date lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    // Remove the Timestamp setter and use this converter instead
    public void updateTimestamp(Timestamp timestamp) {
        this.lastMessageTime = timestamp.toDate();
    }

    public long getLastMessageTimestamp() {
        return lastMessageTime != null ? lastMessageTime.getTime() : 0L;
    }

    public Map<String, Integer> getUnreadCounts() {
        return unreadCounts;
    }

    public void setUnreadCounts(Map<String, Integer> unreadCounts) {
        this.unreadCounts = unreadCounts;
    }

    @PropertyName("isGroup")
    public boolean isGroup() {
        return isGroup;
    }

    @PropertyName("isGroup")
    public void setGroup(boolean group) {
        isGroup = group;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupImage() {
        return groupImage;
    }

    public void setGroupImage(String groupImage) {
        this.groupImage = groupImage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Chat chat = (Chat) o;
        return Objects.equals(id, chat.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}