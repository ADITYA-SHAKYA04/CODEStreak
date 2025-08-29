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
        
        // Get problem data
        problemTitle = getIntent().getStringExtra(EXTRA_PROBLEM_TITLE);
        problemDescription = getIntent().getStringExtra(EXTRA_PROBLEM_DESCRIPTION);
        
        // Initialize Google download system
        downloadManager = new GoogleModelDownloadManager(this);
        
        initializeViews();
        setupChat();
        initializeAI();
        
        // Check for existing models or prompt download
        checkForChatModels();
    }
    
    private void initializeViews() {
        chatRecyclerView = findViewById(R.id.rv_chat_messages);
        editTextMessage = findViewById(R.id.et_message_input);
        buttonSend = findViewById(R.id.btn_send);
        modelStatusText = findViewById(R.id.tv_model_status);
        progressBarDownload = findViewById(R.id.progressBarDownload);
        
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
        new AlertDialog.Builder(this)
                .setTitle("AI Initialization")
                .setMessage(error)
                .setPositiveButton("Continue with Smart Mode", (d, w) -> {
                    modelStatusText.setText("⚡ Smart Fallback Mode");
                    showAIReadyMessage();
                })
                .setNegativeButton("Download Model", (d, w) -> showModelSelectionDialog())
                .show();
    }
    
    private void showModelSelectionDialog() {
        String[] modelNames = AISolutionHelper_backup.getAvailableModelNames();
        String[] modelDescriptions = AISolutionHelper_backup.getAvailableModelDescriptions();
        
        android.util.Log.d("AIChatActivity", "Showing model dialog with " + modelNames.length + " models");
        
        // Create explicit, simple options that are guaranteed to show
        String[] displayOptions = {
            "💡 Smart Fallback (Works immediately)",
            "� Use Existing Google AI Edge Model",
            "�📥 Gemma 1.1 2B IT (Recommended) - 1.2GB",
            "📥 Gemma 2 2B IT - 1.5GB", 
            "📥 Phi-3 Mini 4K - 2.1GB",
            "🔄 Retry Previous Download",
            "❌ Cancel"
        };
        
        android.util.Log.d("AIChatActivity", "Created " + displayOptions.length + " display options");
        for (int i = 0; i < displayOptions.length; i++) {
            android.util.Log.d("AIChatActivity", "Option " + i + ": " + displayOptions[i]);
        }
        
        // Create a simple list dialog
        new AlertDialog.Builder(this)
                .setTitle("🤖 Choose AI Mode")
                .setItems(displayOptions, (dialog, which) -> {
                    android.util.Log.d("AIChatActivity", "Selected option: " + which + " - " + displayOptions[which]);
                    
                    switch (which) {
                        case 0: // Smart Fallback
                            modelStatusText.setText("⚡ Smart Fallback Mode");
                            progressBarDownload.setVisibility(View.GONE);
                            Toast.makeText(this, "Ready to help with intelligent solutions! 🚀", Toast.LENGTH_SHORT).show();
                            
                            // Add welcome message
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
                            break;
                            
                        case 1: // Use Existing Google AI Edge Model
                            modelStatusText.setText("🔍 Searching for existing models...");
                            progressBarDownload.setVisibility(View.VISIBLE);
                            progressBarDownload.setProgress(0);
                            Toast.makeText(this, "🔍 Looking for models downloaded by Google AI Edge Gallery...", Toast.LENGTH_SHORT).show();
                            aiHelper.detectExistingGoogleModels(this, createGoogleDownloadCallback());
                            break;
                            
                        case 2: // Gemma 1.1 2B IT
                            startModelDownload(0, "Gemma 1.1 2B IT (Recommended)");
                            break;
                            
                        case 3: // Gemma 2 2B IT
                            startModelDownload(1, "Gemma 2 2B IT");
                            break;
                            
                        case 4: // Phi-3 Mini 4K
                            startModelDownload(2, "Phi-3 Mini 4K");
                            break;
                            
                        case 5: // Retry Download
                            progressBarDownload.setVisibility(View.VISIBLE);
                            modelStatusText.setText("🔄 Retrying download with Google WorkManager...");
                            // Retry with Google architecture and Hugging Face auth
                            aiHelper.downloadModelWithWorkManager(AIChatActivity.this, createGoogleDownloadCallback());
                            break;
                            
                        case 6: // Cancel
                        default:
                            modelStatusText.setText("⚡ Smart Fallback Mode");
                            progressBarDownload.setVisibility(View.GONE);
                            Toast.makeText(this, "Using Smart Fallback mode", Toast.LENGTH_SHORT).show();
                            break;
                    }
                })
                .setNegativeButton("Use Smart Fallback", (dialog, which) -> {
                    modelStatusText.setText("⚡ Smart Fallback Mode");
                    progressBarDownload.setVisibility(View.GONE);
                    Toast.makeText(this, "Ready to help with smart solutions! 🧠", Toast.LENGTH_SHORT).show();
                })
                .show();
    }
    
    private void startModelDownload(int modelIndex, String modelName) {
        progressBarDownload.setVisibility(View.VISIBLE);
        modelStatusText.setText("Downloading " + modelName + "...");
        Toast.makeText(this, "🚀 Starting Google WorkManager download: " + modelName, Toast.LENGTH_SHORT).show();
        
        // Use the new Google WorkManager architecture with Hugging Face auth
        aiHelper.downloadModelWithWorkManager(this, createGoogleDownloadCallback());
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
                    modelStatusText.setText("✅ Model Ready");
                    Toast.makeText(AIChatActivity.this, "🎉 Model downloaded successfully! Ready to chat!", Toast.LENGTH_LONG).show();
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
            showGoogleModelSelectionDialog();
            return true;
        } else if (item.getItemId() == R.id.action_download_models) {
            showModelDownloadDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * Check for existing chat models or prompt download
     */
    private void checkForChatModels() {
        GoogleChatModel[] availableModels = {
            GoogleChatModel.PHI_3_MINI,
            GoogleChatModel.GEMMA_2B,
            GoogleChatModel.STABLE_CODE_3B,
            GoogleChatModel.GEMMA_7B,
            GoogleChatModel.LLAMA_3_8B
        };
        
        // Check if any model is already downloaded
        GoogleChatModel downloadedModel = null;
        for (GoogleChatModel model : availableModels) {
            if (model.isDownloaded(this)) {
                downloadedModel = model;
                break;
            }
        }
        
        if (downloadedModel != null) {
            currentModel = downloadedModel;
            updateModelStatus("Using " + currentModel.getDisplayName(), false);
        } else {
            updateModelStatus("No chat model available", false);
            showModelDownloadDialog();
        }
    }
    
    /**
     * Show dialog to select from downloaded Google models
     */
    private void showGoogleModelSelectionDialog() {
        GoogleChatModel[] availableModels = {
            GoogleChatModel.PHI_3_MINI,
            GoogleChatModel.GEMMA_2B,
            GoogleChatModel.STABLE_CODE_3B,
            GoogleChatModel.GEMMA_7B,
            GoogleChatModel.LLAMA_3_8B
        };
        
        java.util.List<GoogleChatModel> downloadedModels = new java.util.ArrayList<>();
        java.util.List<String> modelNames = new java.util.ArrayList<>();
        
        for (GoogleChatModel model : availableModels) {
            if (model.isDownloaded(this)) {
                downloadedModels.add(model);
                modelNames.add(model.getDisplayName() + " (" + formatBytes(model.getSizeInBytes()) + ")");
            }
        }
        
        if (downloadedModels.isEmpty()) {
            Toast.makeText(this, "No models downloaded. Use download option to get models.", Toast.LENGTH_LONG).show();
            return;
        }
        
        new AlertDialog.Builder(this)
            .setTitle("🤖 Select Chat Model")
            .setItems(modelNames.toArray(new String[0]), (dialog, which) -> {
                currentModel = downloadedModels.get(which);
                updateModelStatus("Using " + currentModel.getDisplayName(), false);
                Toast.makeText(this, "Switched to " + currentModel.getDisplayName(), Toast.LENGTH_SHORT).show();
            })
            .show();
    }
    
    /**
     * Show dialog to download new models using Google's architecture
     */
    private void showModelDownloadDialog() {
        GoogleChatModel[] availableModels = {
            GoogleChatModel.PHI_3_MINI,
            GoogleChatModel.GEMMA_2B,
            GoogleChatModel.STABLE_CODE_3B,
            GoogleChatModel.GEMMA_7B,
            GoogleChatModel.LLAMA_3_8B
        };
        
        String[] modelDescriptions = new String[availableModels.length];
        for (int i = 0; i < availableModels.length; i++) {
            GoogleChatModel model = availableModels[i];
            String status = model.isDownloaded(this) ? " ✅ Downloaded" : " ⬇️ " + formatBytes(model.getSizeInBytes());
            modelDescriptions[i] = model.getDisplayName() + "\n" + model.getInfo() + status;
        }
        
        new AlertDialog.Builder(this)
            .setTitle("🚀 Download Chat Models")
            .setItems(modelDescriptions, (dialog, which) -> {
                GoogleChatModel selectedModel = availableModels[which];
                if (selectedModel.isDownloaded(this)) {
                    currentModel = selectedModel;
                    updateModelStatus("Using " + currentModel.getDisplayName(), false);
                    Toast.makeText(this, "Model already downloaded!", Toast.LENGTH_SHORT).show();
                } else {
                    startModelDownload(selectedModel);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    /**
     * Start downloading a model using Google's exact architecture
     */
    private void startModelDownload(GoogleChatModel model) {
        updateModelStatus("Downloading " + model.getDisplayName() + "...", true);
        
        downloadManager.downloadChatModel(model, new AISolutionHelper_backup.GoogleDownloadCallback() {
            @Override
            public void onProgress(int progress, String status) {
                runOnUiThread(() -> {
                    updateModelStatus(status, true);
                    if (progressBarDownload != null) {
                        progressBarDownload.setProgress(progress);
                    }
                });
            }
            
            @Override
            public void onComplete(String modelPath) {
                runOnUiThread(() -> {
                    currentModel = model;
                    updateModelStatus("✅ " + model.getDisplayName() + " ready!", false);
                    Toast.makeText(AIChatActivity.this, 
                        "🎉 " + model.getDisplayName() + " downloaded successfully!", 
                        Toast.LENGTH_LONG).show();
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    updateModelStatus("❌ Download failed: " + error, false);
                    Toast.makeText(AIChatActivity.this, "Download failed: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
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
