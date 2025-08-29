package com.example.codestreak;

public class ChatMessage {
    public static final int TYPE_USER = 0;
    public static final int TYPE_AI = 1;
    
    private String content;
    private int type;
    private long timestamp;
    private boolean isCode;
    
    public ChatMessage(String content, int type) {
        this.content = content;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        this.isCode = detectCodeContent(content);
    }
    
    private boolean detectCodeContent(String content) {
        return content.contains("```") || 
               content.contains("class ") || 
               content.contains("public ") ||
               content.contains("def ") ||
               content.contains("return ");
    }
    
    // Getters
    public String getContent() { return content; }
    public int getType() { return type; }
    public long getTimestamp() { return timestamp; }
    public boolean isCode() { return isCode; }
    
    // Setters
    public void setContent(String content) { 
        this.content = content;
        this.isCode = detectCodeContent(content);
    }
    
    public boolean isUser() { return type == TYPE_USER; }
    public boolean isAI() { return type == TYPE_AI; }
    
    /**
     * Get formatted timestamp for display
     */
    public String getFormattedTime() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(timestamp));
    }
}
