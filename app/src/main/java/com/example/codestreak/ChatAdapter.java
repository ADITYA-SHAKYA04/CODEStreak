package com.example.codestreak;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import io.noties.markwon.Markwon;
import io.noties.markwon.SoftBreakAddsNewLinePlugin;
import io.noties.markwon.core.MarkwonTheme;
import io.noties.markwon.html.HtmlPlugin;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    
    private final List<ChatMessage> messages;
    private final Context context;
    private final Markwon markwon;
    
    public ChatAdapter(Context context) {
        this.context = context;
        this.messages = new ArrayList<>();
        
        // Check if dark theme is enabled
        boolean isDarkTheme = (context.getResources().getConfiguration().uiMode 
            & android.content.res.Configuration.UI_MODE_NIGHT_MASK) 
            == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        
        int codeBlockBgColor = ContextCompat.getColor(context, R.color.code_background);
        int codeTextColor = ContextCompat.getColor(context, 
            isDarkTheme ? R.color.text_primary : R.color.code_text);
        
        // Initialize Markwon with enhanced styling for code blocks
        int paddingPx = (int) (12 * context.getResources().getDisplayMetrics().density);
        
        this.markwon = Markwon.builder(context)
            .usePlugin(HtmlPlugin.create())
            .usePlugin(SoftBreakAddsNewLinePlugin.create())
            .usePlugin(new io.noties.markwon.AbstractMarkwonPlugin() {
                @Override
                public void configureTheme(@NonNull MarkwonTheme.Builder builder) {
                    builder
                        // Code block styling with enhanced visibility
                        .codeBlockBackgroundColor(codeBlockBgColor)
                        .codeBackgroundColor(codeBlockBgColor)
                        .codeTextColor(codeTextColor)
                        .codeTypeface(Typeface.MONOSPACE)
                        .codeBlockTypeface(Typeface.MONOSPACE)
                        .codeTextSize((int) (14 * context.getResources().getDisplayMetrics().scaledDensity))
                        .codeBlockTextSize((int) (14 * context.getResources().getDisplayMetrics().scaledDensity))
                        // Margins and spacing
                        .blockMargin((int) (16 * context.getResources().getDisplayMetrics().density))
                        .codeBlockMargin((int) (16 * context.getResources().getDisplayMetrics().density))
                        // Padding for code blocks
                        .listItemColor(codeTextColor)
                        .linkColor(ContextCompat.getColor(context, R.color.accent_primary))
                        .headingBreakHeight(0);
                }
            })
            .build();
    }
    
    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getType();
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ChatMessage.TYPE_USER) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_user, parent, false);
            return new UserMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_ai, parent, false);
            return new AIMessageViewHolder(view);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        
        if (holder instanceof UserMessageViewHolder) {
            ((UserMessageViewHolder) holder).bind(message);
        } else if (holder instanceof AIMessageViewHolder) {
            ((AIMessageViewHolder) holder).bind(message);
        }
    }
    
    @Override
    public int getItemCount() {
        return messages.size();
    }
    
    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }
    
    public void updateLastMessage(String content) {
        if (!messages.isEmpty()) {
            ChatMessage lastMessage = messages.get(messages.size() - 1);
            lastMessage.setContent(content);
            notifyItemChanged(messages.size() - 1);
        }
    }
    
    public void clear() {
        messages.clear();
        notifyDataSetChanged();
    }
    
    // User message ViewHolder
    static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView timeText;
        
        UserMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.tv_user_message);
            timeText = itemView.findViewById(R.id.tv_user_time);
        }
        
        void bind(ChatMessage message) {
            messageText.setText(message.getContent());
            timeText.setText(message.getFormattedTime());
        }
    }
    
    // AI message ViewHolder
    class AIMessageViewHolder extends RecyclerView.ViewHolder {
        View aiAvatar;
        TextView messageText;
        TextView timeText;
        com.google.android.material.button.MaterialButton copyButton;
        com.google.android.material.button.MaterialButton copyCodeButton;
        
        AIMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            aiAvatar = itemView.findViewById(R.id.iv_ai_avatar);
            messageText = itemView.findViewById(R.id.tv_ai_message);
            timeText = itemView.findViewById(R.id.tv_ai_time);
            copyButton = itemView.findViewById(R.id.btn_copy_message);
            copyCodeButton = itemView.findViewById(R.id.btn_copy_code);
        }
        
        void bind(ChatMessage message) {
            // Preprocess message content to handle escape sequences
            String processedContent = preprocessMessageContent(message.getContent());
            
            // Use Markwon to render markdown formatted message
            markwon.setMarkdown(messageText, processedContent);
            timeText.setText(message.getFormattedTime());
            
            // Show/hide copy code button based on content
            if (message.isCode()) {
                copyCodeButton.setVisibility(View.VISIBLE);
                copyCodeButton.setOnClickListener(v -> copyCodeToClipboard(processedContent));
            } else {
                copyCodeButton.setVisibility(View.GONE);
            }
            
            // Copy full message button
            copyButton.setOnClickListener(v -> copyToClipboard(processedContent));
        }
        
        private String preprocessMessageContent(String content) {
            if (content == null) return "";
            
            // Replace escaped newlines with actual newlines
            content = content.replace("\\n", "\n");
            // Replace escaped tabs with actual tabs
            content = content.replace("\\t", "    ");
            // Remove any double escaping
            content = content.replace("\\\\", "\\");
            
            return content;
        }
        
        private void copyCodeToClipboard(String message) {
            // Extract code from message
            Pattern codePattern = Pattern.compile("```(?:java|python|cpp|javascript)?\\n?([\\s\\S]*?)```");
            Matcher matcher = codePattern.matcher(message);
            
            StringBuilder codeBuilder = new StringBuilder();
            while (matcher.find()) {
                if (codeBuilder.length() > 0) {
                    codeBuilder.append("\n\n");
                }
                codeBuilder.append(matcher.group(1).trim());
            }
            
            if (codeBuilder.length() > 0) {
                copyToClipboard(codeBuilder.toString());
                Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "No code found to copy", Toast.LENGTH_SHORT).show();
            }
        }
        
        private void copyToClipboard(String text) {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("AI Response", text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show();
        }
    }
}
