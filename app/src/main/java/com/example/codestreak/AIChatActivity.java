package com.example.codestreak;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * AI Chat Activity with Google AI Edge Gallery download integration
 */
public class AIChatActivity extends AppCompatActivity {
    private static final String EXTRA_PROBLEM_TITLE = "problem_title";
    private static final String EXTRA_PROBLEM_DESCRIPTION = "problem_description";
    
    private AISolutionHelper_backup aiHelper;
    private ChatAdapter chatAdapter;
    private RecyclerView chatRecyclerView;
    private EditText editTextMessage;
    private ImageButton buttonSend;
    private TextView modelStatusText;
    private TextView tvModelInfo;
    private ProgressBar progressBarDownload;
    
    // Google download system integration
    private GoogleModelDownloadManager downloadManager;
    private GoogleChatModel currentModel = null;
    
    private String problemTitle;
    private String problemDescription;
    
    public static void start(AppCompatActivity activity, String title, String description) {
        Intent intent = new Intent(activity, AIChatActivity.class);
        intent.putExtra(EXTRA_PROBLEM_TITLE, title);
        intent.putExtra(EXTRA_PROBLEM_DESCRIPTION, description);
        activity.startActivity(intent);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);
        
        // Set status bar color to match activity background
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.background_primary, getTheme()));
        }
        
        // Get problem data
        problemTitle = getIntent().getStringExtra(EXTRA_PROBLEM_TITLE);
        problemDescription = getIntent().getStringExtra(EXTRA_PROBLEM_DESCRIPTION);
        
        // Initialize Google download system
        downloadManager = new GoogleModelDownloadManager(this);
        
        initializeViews();
        setupChat();
        initializeAI();
        
        // Show current model status
        updateCurrentModelStatus();
    }
    
    private void initializeViews() {
        chatRecyclerView = findViewById(R.id.rv_chat_messages);
        editTextMessage = findViewById(R.id.et_message_input);
        buttonSend = findViewById(R.id.btn_send);
        modelStatusText = findViewById(R.id.tv_model_status);
        tvModelInfo = findViewById(R.id.tvModelInfo);
        progressBarDownload = findViewById(R.id.progressBarDownload);
        
        // Make model info clickable to switch models
        tvModelInfo.setOnClickListener(v -> showModelManagementDialog());
        
        // Setup toolbar navigation with menu
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        
        // Setup send button
        buttonSend.setOnClickListener(v -> sendMessage());
        
        // Setup quick action chips
        setupQuickActionChips();
        
        // Enable send button only when there's text
        editTextMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                buttonSend.setEnabled(s.toString().trim().length() > 0);
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    
    private void setupChat() {
        chatAdapter = new ChatAdapter(this);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);
        
        // Add initial context message
        String contextMessage = "🎯 **Problem Context:**\n\n" +
                               "**" + problemTitle + "**\n\n" +
                               problemDescription + "\n\n" +
                               "💬 Ask me anything about this problem - algorithm approaches, code implementation, " +
                               "complexity analysis, test cases, or debugging tips!";
        
        ChatMessage contextChat = new ChatMessage(contextMessage, ChatMessage.TYPE_AI);
        chatAdapter.addMessage(contextChat);
        scrollToBottom();
    }
    
    private void setupQuickActionChips() {
        com.google.android.material.chip.Chip chipExplain = findViewById(R.id.btn_explain_solution);
        com.google.android.material.chip.Chip chipOptimize = findViewById(R.id.btn_optimize);
        com.google.android.material.chip.Chip chipEdgeCases = findViewById(R.id.btn_edge_cases);
        com.google.android.material.chip.Chip chipComplexity = findViewById(R.id.chipComplexity);
        
        chipExplain.setOnClickListener(v -> sendPredefinedMessage("Explain the best approach to solve this problem step by step"));
        chipOptimize.setOnClickListener(v -> sendPredefinedMessage("What are the optimizations I can apply to improve the solution?"));
        chipEdgeCases.setOnClickListener(v -> sendPredefinedMessage("What are the important edge cases and corner cases I should consider for this problem?"));
        chipComplexity.setOnClickListener(v -> sendPredefinedMessage("What is the time and space complexity of the optimal solution?"));
    }
    
    private void sendPredefinedMessage(String message) {
        // Set the message in the input field and send it
        editTextMessage.setText(message);
        sendMessage();
    }
    
    private void initializeAI() {
        aiHelper = new AISolutionHelper_backup(this);
        modelStatusText.setText("🔄 Initializing AI...");
        progressBarDownload.setVisibility(View.VISIBLE);
        
        aiHelper.initializeModel(new AISolutionHelper_backup.SolutionCallback() {
            @Override
            public void onSolutionGenerated(String response) {
                // Not used in this context
            }
            
            @Override
            public void onError(String error) {
                progressBarDownload.setVisibility(View.GONE);
                if (error.contains("Smart AI Ready") || error.contains("Smart AI Mode")) {
                    modelStatusText.setText("⚡ Smart Fallback Mode");
                    showAIReadyMessage();
                } else {
                    modelStatusText.setText("❌ Error");
                    showErrorDialog(error);
                }
            }
            
            @Override
            public void onProgress(String status) {
                modelStatusText.setText(status);
            }
            
            @Override
            public void onModelLoaded() {
                progressBarDownload.setVisibility(View.GONE);
                modelStatusText.setText("✅ MediaPipe Model Ready");
                showModelLoadedMessage();
            }
            
            @Override
            public void onDownloadProgress(int progress, String status) {
                progressBarDownload.setProgress(progress);
                modelStatusText.setText(status);
            }
            
            @Override
            public void onModelSelectionRequired() {
                showModelSelectionDialog();
            }
        });
    }
    
    private void showAIReadyMessage() {
        ChatMessage message = new ChatMessage(
            "🤖 **AI Assistant Ready!**\n\n" +
            "I'm running in Smart Fallback mode and ready to help you with:\n\n" +
            "• **Algorithm Analysis** - Explain different approaches\n" +
            "• **Code Implementation** - Generate Java solutions\n" +
            "• **Complexity Analysis** - Time and space complexity\n" +
            "• **Testing Strategies** - Edge cases and test plans\n" +
            "• **Optimization Tips** - Performance improvements\n" +
            "• **Debugging Help** - Find and fix issues\n\n" +
            "💡 **Try asking:** \"Explain the approach\", \"Show me the code\", or \"What's the complexity?\"",
            ChatMessage.TYPE_AI
        );
        chatAdapter.addMessage(message);
        scrollToBottom();
    }
    
    private void showModelLoadedMessage() {
        ChatMessage message = new ChatMessage(
            "🚀 **Advanced AI Model Loaded!**\n\n" +
            "MediaPipe LLM is now active with enhanced capabilities:\n\n" +
            "• **Deep Analysis** - Comprehensive problem understanding\n" +
            "• **Contextual Responses** - Tailored to your specific problem\n" +
            "• **Advanced Reasoning** - Multi-step problem solving\n" +
            "• **Code Generation** - Optimized implementations\n\n" +
            "Ready to provide expert-level assistance! 🎯",
            ChatMessage.TYPE_AI
        );
        chatAdapter.addMessage(message);
        scrollToBottom();
    }
    
    private void showErrorDialog(String error) {
        // Auto-activate Smart Fallback on any error
        modelStatusText.setText("⚡ Smart Fallback Mode");
        showAIReadyMessage();
    }
    
    private void showModelSelectionDialog() {
        // Show options using Google AI Edge Gallery architecture
        String[] options = {
            "💡 Smart Fallback (Works immediately)",
            "🔍 Use Existing Google AI Edge Model",
            "📥 Download New Model (Google AI Edge)",
            "❌ Cancel"
        };
        
        new AlertDialog.Builder(this)
                .setTitle("🤖 Choose AI Mode")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Smart Fallback
                            activateSmartFallback();
                            break;
                            
                        case 1: // Use Existing Google AI Edge Model
                            modelStatusText.setText("🔍 Searching for existing models...");
                            progressBarDownload.setVisibility(View.VISIBLE);
                            progressBarDownload.setProgress(0);
                            Toast.makeText(this, "🔍 Looking for models downloaded by Google AI Edge Gallery...", Toast.LENGTH_SHORT).show();
                            aiHelper.detectExistingGoogleModels(this, createGoogleDownloadCallback());
                            break;
                            
                        case 2: // Download New Model
                            modelStatusText.setText("📥 Preparing download...");
                            progressBarDownload.setVisibility(View.VISIBLE);
                            progressBarDownload.setProgress(0);
                            Toast.makeText(this, "🚀 Starting Google AI Edge download...", Toast.LENGTH_SHORT).show();
                            aiHelper.downloadModelWithWorkManager(this, createGoogleDownloadCallback());
                            break;
                            
                        case 3: // Cancel
                        default:
                            activateSmartFallback();
                            break;
                    }
                })
                .setNegativeButton("Use Smart Fallback", (dialog, which) -> activateSmartFallback())
                .show();
    }
    
    private void activateSmartFallback() {
        modelStatusText.setText("⚡ Smart Fallback Mode");
        progressBarDownload.setVisibility(View.GONE);
        
        ChatMessage welcomeMessage = new ChatMessage(
            "🎯 **Smart Fallback Mode Activated!**\n\n" +
            "I'm ready to help you with this problem using built-in intelligence.\n\n" +
            "✨ **What I can do:**\n" +
            "• Explain algorithms and approaches\n" +
            "• Generate optimized code solutions\n" +
            "• Analyze time/space complexity\n" +
            "• Provide debugging tips\n" +
            "• Handle edge cases\n\n" +
            "🚀 **Try asking:** \"Explain the approach\" or \"Show me the optimal solution\"",
            ChatMessage.TYPE_AI
        );
        chatAdapter.addMessage(welcomeMessage);
        scrollToBottom();
    }
    
    private void showDownloadInstructions() {
        String instructions = 
            "📲 **Download AI Models via Google AI Edge**\n\n" +
            "**Option 1: Smart Fallback (Recommended)**\n" +
            "• Works immediately without downloads\n" +
            "• Provides intelligent solutions\n" +
            "• No storage space required\n\n" +
            "**Option 2: Google AI Edge Gallery App**\n" +
            "1. Install 'Google AI Edge Gallery' from Play Store\n" +
            "2. Download models (Gemma 2B recommended)\n" +
            "3. Return here and select 'Use Existing Model'\n\n" +
            "**Option 3: Download in App**\n" +
            "• Select 'Download New Model' from menu\n" +
            "• Uses Google's architecture\n" +
            "• May require authentication";
        
        new AlertDialog.Builder(this)
                .setTitle("📲 Download Instructions")
                .setMessage(instructions)
                .setPositiveButton("Download Now", (d, w) -> {
                    aiHelper.downloadModelWithWorkManager(this, createGoogleDownloadCallback());
                })
                .setNeutralButton("Find Existing", (d, w) -> {
                    aiHelper.detectExistingGoogleModels(this, createGoogleDownloadCallback());
                })
                .setNegativeButton("Use Smart Fallback", (d, w) -> activateSmartFallback())
                .show();
    }
    
    private AISolutionHelper_backup.GoogleDownloadCallback createGoogleDownloadCallback() {
        return new AISolutionHelper_backup.GoogleDownloadCallback() {
            @Override
            public void onProgress(int progress, String status) {
                runOnUiThread(() -> {
                    progressBarDownload.setProgress(progress);
                    modelStatusText.setText(status);
                });
            }
            
            @Override
            public void onComplete(String modelPath) {
                runOnUiThread(() -> {
                    progressBarDownload.setVisibility(View.GONE);
                    modelStatusText.setText("🔄 Initializing model...");
                    
                    // Update model info display
                    updateCurrentModelStatus();
                    
                    // Reinitialize the AI helper to use the downloaded model
                    aiHelper.initializeModel(new AISolutionHelper_backup.SolutionCallback() {
                        @Override
                        public void onSolutionGenerated(String response) {
                            // Not used during initialization
                        }
                        
                        @Override
                        public void onError(String error) {
                            modelStatusText.setText("✅ Model Ready (Smart Fallback)");
                            Toast.makeText(AIChatActivity.this, 
                                "🎉 Model downloaded! Using Smart Fallback mode.", 
                                Toast.LENGTH_LONG).show();
                        }
                        
                        @Override
                        public void onProgress(String progress) {
                            modelStatusText.setText(progress);
                        }
                        
                        @Override
                        public void onModelLoaded() {
                            modelStatusText.setText("✅ Model Ready!");
                            Toast.makeText(AIChatActivity.this, 
                                "🎉 Model initialized successfully! Ready to chat!", 
                                Toast.LENGTH_LONG).show();
                        }
                        
                        @Override
                        public void onDownloadProgress(int progress, String status) {
                            // Not used during initialization
                        }
                    });
                    
                    Toast.makeText(AIChatActivity.this, 
                        "🎉 Model downloaded successfully! Initializing...", 
                        Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBarDownload.setVisibility(View.GONE);
                    modelStatusText.setText("❌ Download Failed");
                    Toast.makeText(AIChatActivity.this, "Download failed: " + error, Toast.LENGTH_LONG).show();
                });
            }
        };
    }
    
    private AISolutionHelper_backup.SolutionCallback createModelDownloadCallback() {
        return new AISolutionHelper_backup.SolutionCallback() {
            @Override
            public void onSolutionGenerated(String response) {}
            
            @Override
            public void onError(String error) {
                progressBarDownload.setVisibility(View.GONE);
                modelStatusText.setText("❌ Download Failed");
                
                new AlertDialog.Builder(AIChatActivity.this)
                        .setTitle("Download Failed")
                        .setMessage("Failed to download model:\n\n" + error + "\n\nWould you like to:")
                        .setPositiveButton("Retry", (d, w) -> showModelSelectionDialog())
                        .setNegativeButton("Use Smart Fallback", (d, w) -> {
                            modelStatusText.setText("⚡ Smart Fallback Mode");
                            showAIReadyMessage();
                        })
                        .show();
            }
            
            @Override
            public void onProgress(String status) {
                modelStatusText.setText(status);
            }
            
            @Override
            public void onModelLoaded() {
                progressBarDownload.setVisibility(View.GONE);
                modelStatusText.setText("✅ Model Ready");
                showModelLoadedMessage();
            }
            
            @Override
            public void onDownloadProgress(int progress, String status) {
                progressBarDownload.setProgress(progress);
                modelStatusText.setText(status);
            }
        };
    }
    
    private void sendMessage() {
        String messageText = editTextMessage.getText().toString().trim();
        if (messageText.isEmpty()) return;
        
        android.util.Log.d("AIChatActivity", "Sending message: " + messageText);
        
        // Add user message to chat
        ChatMessage userMessage = new ChatMessage(messageText, ChatMessage.TYPE_USER);
        chatAdapter.addMessage(userMessage);
        scrollToBottom();
        
        // Clear input
        editTextMessage.setText("");
        
        // Add thinking indicator immediately
        ChatMessage thinkingMessage = new ChatMessage("🤔 Thinking...", ChatMessage.TYPE_AI);
        chatAdapter.addMessage(thinkingMessage);
        scrollToBottom();
        
        // Prepare context for AI
        String problemContext = "Problem: " + problemTitle + "\n" + 
                               "Description: " + problemDescription;
        
        android.util.Log.d("AIChatActivity", "Problem context: " + problemContext);
        
        // Send to AI and get response
        aiHelper.sendChatMessage(messageText, problemContext, new AISolutionHelper_backup.ChatCallback() {
            @Override
            public void onResponseReceived(String response) {
                android.util.Log.d("AIChatActivity", "Received response: " + response.substring(0, Math.min(100, response.length())));
                
                // Remove the thinking message and add the actual response
                chatAdapter.updateLastMessage(response);
                scrollToBottom();
            }
            
            @Override
            public void onError(String error) {
                android.util.Log.e("AIChatActivity", "Error in chat: " + error);
                
                // Replace thinking message with error message
                String errorResponse = "❌ **Error generating response**\n\n" + error + "\n\n" +
                    "Please try asking in a different way or check your connection.";
                chatAdapter.updateLastMessage(errorResponse);
                scrollToBottom();
            }
            
            @Override
            public void onTyping() {
                android.util.Log.d("AIChatActivity", "AI is typing...");
                // Already added thinking message above
            }
        });
    }
    
    private void scrollToBottom() {
        if (chatAdapter.getItemCount() > 0) {
            chatRecyclerView.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
        }
    }
    
    // Helper methods for formatting download information
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
    
    private String formatTime(long seconds) {
        if (seconds < 60) return seconds + "s";
        if (seconds < 3600) return String.format("%dm %ds", seconds / 60, seconds % 60);
        return String.format("%dh %dm", seconds / 3600, (seconds % 3600) / 60);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_ai_chat, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_select_model) {
            showModelManagementDialog();
            return true;
        } else if (item.getItemId() == R.id.action_download_models) {
            showModelDownloadDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * Show model management dialog with current model and options
     */
    private void showModelManagementDialog() {
        String currentModelName = aiHelper.getCurrentModelName();
        String currentModelPath = aiHelper.getCurrentModelPath();
        
        StringBuilder message = new StringBuilder();
        message.append("Current Model:\n");
        if (currentModelPath != null && !currentModelPath.isEmpty()) {
            message.append("📱 ").append(currentModelName).append("\n\n");
            message.append("Path: ").append(currentModelPath);
        } else {
            message.append("💡 Smart Fallback (No model downloaded)");
        }
        
        new AlertDialog.Builder(this)
            .setTitle("🤖 AI Model Management")
            .setMessage(message.toString())
            .setPositiveButton("Switch Model", (dialog, which) -> showAvailableModelsDialog())
            .setNeutralButton("Download New Model", (dialog, which) -> showModelDownloadDialog())
            .setNegativeButton("Close", null)
            .show();
    }
    
    /**
     * Show available downloaded models to switch between
     */
    private void showAvailableModelsDialog() {
        modelStatusText.setText("🔍 Searching for models...");
        progressBarDownload.setVisibility(View.VISIBLE);
        aiHelper.detectExistingGoogleModels(this, createGoogleDownloadCallback());
    }
    
    /**
     * Update display to show current active model
     */
    private void updateCurrentModelStatus() {
        String currentModelName = aiHelper.getCurrentModelName();
        String currentModelPath = aiHelper.getCurrentModelPath();
        
        // Update model info display
        if (tvModelInfo != null) {
            tvModelInfo.setText(currentModelName);
        }
        
        if (currentModelPath != null && !currentModelPath.isEmpty()) {
            modelStatusText.setText("📱 Model: " + currentModelName);
        } else {
            modelStatusText.setText("💡 Smart Fallback Active");
        }
        progressBarDownload.setVisibility(View.GONE);
    }
    
    /**
     * Show dialog to select from downloaded Google models
     */
    private void showGoogleModelSelectionDialog() {
        modelStatusText.setText("🔍 Searching for models...");
        progressBarDownload.setVisibility(View.VISIBLE);
        Toast.makeText(this, "🔍 Scanning for downloaded models...", Toast.LENGTH_SHORT).show();
        aiHelper.detectExistingGoogleModels(this, createGoogleDownloadCallback());
    }
    
    /**
     * Show dialog to download new models - uses Google AI Edge architecture
     */
    private void showModelDownloadDialog() {
        modelStatusText.setText("📥 Preparing download...");
        progressBarDownload.setVisibility(View.VISIBLE);
        progressBarDownload.setProgress(0);
        Toast.makeText(this, "🚀 Starting Google AI Edge download...", Toast.LENGTH_LONG).show();
        aiHelper.downloadModelWithWorkManager(this, createGoogleDownloadCallback());
    }
    
    /**
     * Update model status display
     */
    private void updateModelStatus(String status, boolean showProgress) {
        if (modelStatusText != null) {
            modelStatusText.setText(status);
        }
        if (progressBarDownload != null) {
            progressBarDownload.setVisibility(showProgress ? View.VISIBLE : View.GONE);
        }
    }
    
    /**
     * Format bytes for display
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (aiHelper != null) {
            aiHelper.cleanup();
        }
    }
}
