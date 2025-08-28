package com.example.codestreak;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import okhttp3.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ProblemDetailActivity extends BaseActivity {
    
    private TextView problemTitle;
    private TextView problemNumber;
    private TextView difficultyBadge;
    private TextView acceptanceRate;
    private TextView problemDescription;
    private TextView exampleInput;
    private TextView constraints;
    private ChipGroup topicChipGroup;
    private TextView companyTags;
    private MaterialButton solveButton;
    private ImageView backButton;
    private LinearLayout hintsContainer;
    private TextView hintsTitle;
    private ProgressBar loadingIndicator;
    private ScrollView contentContainer;
    
    private String titleSlug;
    private OkHttpClient client;
    private static final String LEETCODE_GRAPHQL_URL = "https://leetcode.com/graphql/";
    
    // Helper class to hold parsed content sections
    private static class ContentSections {
        String description = "";
        String examples = "";
        String constraints = "";
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_problem_detail);
        
        initializeViews();
        setupClickListeners();
        initializeHttpClient();
        getProblemDataFromIntent();
        fetchProblemDetails();
    }
    
    private void initializeViews() {
        problemTitle = findViewById(R.id.problemTitle);
        problemNumber = findViewById(R.id.problemNumber);
        difficultyBadge = findViewById(R.id.difficultyBadge);
        acceptanceRate = findViewById(R.id.acceptanceRate);
        problemDescription = findViewById(R.id.problemDescription);
        exampleInput = findViewById(R.id.exampleInput);
        constraints = findViewById(R.id.constraints);
        topicChipGroup = findViewById(R.id.topicChipGroup);
        companyTags = findViewById(R.id.companyTags);
        solveButton = findViewById(R.id.solveButton);
        backButton = findViewById(R.id.backButton);
        hintsContainer = findViewById(R.id.hintsContainer);
        hintsTitle = findViewById(R.id.hintsTitle);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        contentContainer = findViewById(R.id.contentContainer);
    }
    
    private void initializeHttpClient() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }
    
    private void getProblemDataFromIntent() {
        Intent intent = getIntent();
        int problemId = intent.getIntExtra("problem_id", -1);
        String title = intent.getStringExtra("problem_title");
        titleSlug = intent.getStringExtra("problem_title_slug");
        String difficulty = intent.getStringExtra("problem_difficulty");
        double acceptance = intent.getDoubleExtra("problem_acceptance", 0.0);
        String companies = intent.getStringExtra("problem_companies");
        
        // Set basic info immediately
        problemNumber.setText(String.valueOf(problemId));
        problemTitle.setText(title);
        setDifficultyBadge(difficulty);
        acceptanceRate.setText(String.format("%.1f%% Acceptance Rate", acceptance));
        companyTags.setText(companies);
    }
    
    private void fetchProblemDetails() {
        showLoading(true);
        
        // Add logging for debugging
        android.util.Log.d("ProblemDetail", "Fetching details for titleSlug: " + titleSlug);
        
        if (titleSlug == null || titleSlug.isEmpty()) {
            showError("Problem title slug is missing");
            return;
        }
        
        // Enhanced GraphQL query with correct fields
        String graphqlQuery = "{\n" +
                "  question(titleSlug: \"" + titleSlug + "\") {\n" +
                "    questionId\n" +
                "    title\n" +
                "    content\n" +
                "    difficulty\n" +
                "    exampleTestcases\n" +
                "    topicTags {\n" +
                "      name\n" +
                "    }\n" +
                "    hints\n" +
                "    stats\n" +
                "    acRate\n" +
                "    likes\n" +
                "    dislikes\n" +
                "  }\n" +
                "}";
        
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("query", graphqlQuery);
        
        // Add variables for better query structure
        JsonObject variables = new JsonObject();
        variables.addProperty("titleSlug", titleSlug);
        requestBody.add("variables", variables);
        
        RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.get("application/json; charset=utf-8")
        );
        
        Request request = new Request.Builder()
                .url(LEETCODE_GRAPHQL_URL)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .addHeader("Accept", "application/json")
                .addHeader("Referer", "https://leetcode.com/")
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    showError("Network error: " + e.getMessage());
                    // Fall back to sample data for now
                    displayFallbackData();
                });
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                
                runOnUiThread(() -> {
                    showLoading(false);
                    
                    if (response.isSuccessful()) {
                        if (!responseBody.isEmpty()) {
                            try {
                                // Log the response for debugging
                                android.util.Log.d("ProblemDetail", "Response: " + responseBody);
                                parseProblemDetails(responseBody);
                            } catch (Exception e) {
                                android.util.Log.e("ProblemDetail", "Parse error: " + e.getMessage());
                                showError("Error parsing response: " + e.getMessage());
                                displayFallbackData();
                            }
                        } else {
                            showError("Empty response from server");
                            displayFallbackData();
                        }
                    } else {
                        android.util.Log.e("ProblemDetail", "HTTP Error: " + response.code() + " - " + responseBody);
                        showError("Server error: " + response.code());
                        displayFallbackData();
                    }
                });
            }
        });
    }
    
    private void parseProblemDetails(String responseBody) {
        try {
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
            
            // Check for GraphQL errors
            if (jsonResponse.has("errors")) {
                JsonArray errors = jsonResponse.getAsJsonArray("errors");
                String errorMessage = errors.size() > 0 ? 
                    errors.get(0).getAsJsonObject().get("message").getAsString() : 
                    "Unknown GraphQL error";
                android.util.Log.e("ProblemDetail", "GraphQL Error: " + errorMessage);
                showError("API Error: " + errorMessage);
                displayFallbackData();
                return;
            }
            
            if (!jsonResponse.has("data") || jsonResponse.get("data").isJsonNull()) {
                android.util.Log.e("ProblemDetail", "No data in response");
                showError("No data received from API");
                displayFallbackData();
                return;
            }
            
            JsonObject data = jsonResponse.getAsJsonObject("data");
            if (!data.has("question") || data.get("question").isJsonNull()) {
                android.util.Log.e("ProblemDetail", "No question data found for titleSlug: " + titleSlug);
                
                // Try to fetch by problem ID instead if titleSlug fails
                int problemId = getIntent().getIntExtra("problem_id", -1);
                if (problemId > 0 && problemId <= 100) {
                    android.util.Log.d("ProblemDetail", "Trying to find alternative titleSlug for problem ID: " + problemId);
                    // For now, show error - but could implement ID-based lookup
                }
                
                showError("Problem not found - this might be a generated problem not available in LeetCode");
                displayFallbackData();
                return;
            }
            
            JsonObject question = data.getAsJsonObject("question");
            
            // Parse content (description with examples)
            if (question.has("content") && !question.get("content").isJsonNull()) {
                String content = question.get("content").getAsString();
                android.util.Log.d("ProblemDetail", "Raw content: " + content.substring(0, Math.min(200, content.length())) + "...");
                
                String cleanContent = parseHtmlContent(content);
                android.util.Log.d("ProblemDetail", "Clean content: " + cleanContent.substring(0, Math.min(200, cleanContent.length())) + "...");
                
                // Test HTML entity cleaning
                String testText = "Test &nbsp; &amp; &lt; &gt; &quot; &#39; entities";
                String cleanedTest = cleanHtmlEntities(testText);
                android.util.Log.d("ProblemDetail", "HTML Entity Test - Before: " + testText + " | After: " + cleanedTest);
                
                // Extract different sections from content
                ContentSections sections = extractContentSections(cleanContent);
                
                android.util.Log.d("ProblemDetail", "Description: " + sections.description.substring(0, Math.min(100, sections.description.length())) + "...");
                android.util.Log.d("ProblemDetail", "Examples: " + (sections.examples.isEmpty() ? "EMPTY" : sections.examples.substring(0, Math.min(100, sections.examples.length())) + "..."));
                android.util.Log.d("ProblemDetail", "Constraints: " + sections.constraints.substring(0, Math.min(100, sections.constraints.length())) + "...");
                
                problemDescription.setText(sections.description);
                constraints.setText(sections.constraints);
                
                // Use extracted examples if available, otherwise fall back to exampleTestcases
                if (!sections.examples.isEmpty()) {
                    exampleInput.setText(sections.examples);
                } else if (question.has("exampleTestcases") && !question.get("exampleTestcases").isJsonNull()) {
                    String examples = question.get("exampleTestcases").getAsString();
                    android.util.Log.d("ProblemDetail", "Using exampleTestcases: " + examples);
                    exampleInput.setText(formatExamples(examples));
                } else {
                    exampleInput.setText("Examples not available");
                }
            } else {
                problemDescription.setText("Problem description not available");
                constraints.setText("Constraints not available");
                exampleInput.setText("Examples not available");
            }
            
            // Parse topic tags
            if (question.has("topicTags") && !question.get("topicTags").isJsonNull()) {
                JsonArray topicTags = question.getAsJsonArray("topicTags");
                displayTopicTags(topicTags);
            } else {
                // Add default topic tags
                topicChipGroup.removeAllViews();
                Chip chip = new Chip(this);
                chip.setText("Algorithm");
                chip.setChipBackgroundColorResource(R.color.chip_background);
                chip.setTextColor(ContextCompat.getColor(this, R.color.chip_text));
                chip.setClickable(false);
                topicChipGroup.addView(chip);
            }
            
            // Parse hints
            if (question.has("hints") && !question.get("hints").isJsonNull()) {
                JsonArray hints = question.getAsJsonArray("hints");
                displayHints(hints);
            } else {
                // Show message that hints will be loaded
                hintsContainer.removeAllViews();
                TextView noHintsText = new TextView(this);
                noHintsText.setText("No hints available for this problem");
                noHintsText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
                hintsContainer.addView(noHintsText);
            }
            
            // Parse stats for acceptance rate
            if (question.has("acRate") && !question.get("acRate").isJsonNull()) {
                double acRate = question.get("acRate").getAsDouble();
                acceptanceRate.setText(String.format("%.1f%% Acceptance Rate", acRate));
            } else if (question.has("stats") && !question.get("stats").isJsonNull()) {
                String stats = question.get("stats").getAsString();
                parseAcceptanceRate(stats);
            }
            
        } catch (Exception e) {
            android.util.Log.e("ProblemDetail", "Parsing error: " + e.getMessage(), e);
            showError("Error parsing problem details: " + e.getMessage());
            displayFallbackData();
        }
    }
    
    private void displayFallbackData() {
        // Display sample data when API fails
        String title = problemTitle.getText().toString();
        
        problemDescription.setText(getSampleDescription(title));
        exampleInput.setText(getSampleExample(title));
        constraints.setText(getSampleConstraints(title));
        
        // Add sample topic tags
        topicChipGroup.removeAllViews();
        String[] sampleTopics = {"Array", "Algorithm", "Data Structure"};
        for (String topic : sampleTopics) {
            Chip chip = new Chip(this);
            chip.setText(topic);
            chip.setChipBackgroundColorResource(R.color.chip_background);
            chip.setTextColor(ContextCompat.getColor(this, R.color.chip_text));
            chip.setClickable(false);
            topicChipGroup.addView(chip);
        }
        
        // Show sample hints
        displaySampleHints(title);
    }
    
    private String parseHtmlContent(String htmlContent) {
        // First, handle specific HTML elements that need special treatment
        String processed = htmlContent
                // Convert <pre> tags to preserve formatting
                .replaceAll("<pre[^>]*>", "\n```\n")
                .replaceAll("</pre>", "\n```\n")
                // Convert <code> tags 
                .replaceAll("<code[^>]*>", "`")
                .replaceAll("</code>", "`")
                // Convert <strong> and <b> tags
                .replaceAll("<(strong|b)[^>]*>", "**")
                .replaceAll("</(strong|b)>", "**")
                // Convert <em> and <i> tags
                .replaceAll("<(em|i)[^>]*>", "*")
                .replaceAll("</(em|i)>", "*")
                // Handle paragraph breaks
                .replaceAll("<p[^>]*>", "\n")
                .replaceAll("</p>", "\n")
                // Handle line breaks
                .replaceAll("<br[^>]*>", "\n")
                // Handle list items
                .replaceAll("<li[^>]*>", "• ")
                .replaceAll("</li>", "\n")
                // Remove all other HTML tags
                .replaceAll("<[^>]+>", "")
                // Decode HTML entities - comprehensive list
                .replaceAll("&nbsp;", " ")          // Non-breaking space
                .replaceAll("&amp;", "&")           // Ampersand
                .replaceAll("&lt;", "<")            // Less than
                .replaceAll("&gt;", ">")            // Greater than
                .replaceAll("&quot;", "\"")         // Quote
                .replaceAll("&#39;", "'")           // Apostrophe
                .replaceAll("&#x27;", "'")          // Apostrophe (hex)
                .replaceAll("&apos;", "'")          // Apostrophe
                .replaceAll("&ldquo;", "\"")        // Left double quote
                .replaceAll("&rdquo;", "\"")        // Right double quote
                .replaceAll("&lsquo;", "'")         // Left single quote
                .replaceAll("&rsquo;", "'")         // Right single quote
                .replaceAll("&ndash;", "-")         // En dash
                .replaceAll("&mdash;", "—")         // Em dash
                .replaceAll("&hellip;", "...")      // Ellipsis
                .replaceAll("&#\\d+;", "")          // Remove any remaining numeric entities
                .replaceAll("&[a-zA-Z]+;", "")      // Remove any remaining named entities
                // Clean up excessive whitespace and spaces
                .replaceAll("\\s+", " ")            // Multiple spaces to single space
                .replaceAll("\\n\\s*\\n\\s*\\n", "\n\n")  // Multiple newlines to double newline
                .replaceAll("^\\s+", "")            // Leading whitespace
                .replaceAll("\\s+$", "")            // Trailing whitespace
                .trim();
        
        return processed;
    }
    
    private String[] extractDescriptionAndConstraints(String content) {
        // This method is deprecated - use extractContentSections instead
        ContentSections sections = extractContentSections(content);
        return new String[]{sections.description, sections.constraints};
    }
    
    private ContentSections extractContentSections(String content) {
        ContentSections sections = new ContentSections();
        
        // Split content by common section patterns
        String[] lines = content.split("\n");
        StringBuilder currentSection = new StringBuilder();
        String currentSectionType = "description";
        
        for (String line : lines) {
            String lowerLine = line.toLowerCase().trim();
            
            // Check if this line starts a new section
            if (lowerLine.startsWith("example") && (lowerLine.contains(":") || lowerLine.matches("example\\s+\\d+"))) {
                // Save previous section
                saveSectionContent(sections, currentSectionType, currentSection.toString().trim());
                currentSection = new StringBuilder();
                currentSectionType = "examples";
                currentSection.append(line).append("\n");
            }
            else if (lowerLine.startsWith("input:") || lowerLine.startsWith("output:")) {
                // Continue with examples section
                currentSection.append(line).append("\n");
            }
            else if (lowerLine.startsWith("constraints:") || lowerLine.startsWith("constraint:")) {
                // Save previous section
                saveSectionContent(sections, currentSectionType, currentSection.toString().trim());
                currentSection = new StringBuilder();
                currentSectionType = "constraints";
                currentSection.append(line).append("\n");
            }
            else if (lowerLine.startsWith("note:") || lowerLine.startsWith("follow") || lowerLine.startsWith("hint:")) {
                // Save previous section
                saveSectionContent(sections, currentSectionType, currentSection.toString().trim());
                currentSection = new StringBuilder();
                currentSectionType = "constraints";
                currentSection.append(line).append("\n");
            }
            else {
                // Continue with current section
                currentSection.append(line).append("\n");
            }
        }
        
        // Save the last section
        saveSectionContent(sections, currentSectionType, currentSection.toString().trim());
        
        // Clean up sections
        sections.description = cleanDescriptionText(sections.description);
        sections.examples = cleanExamplesText(sections.examples);
        sections.constraints = cleanConstraintsText(sections.constraints);
        
        // If no examples were found in content, leave it empty to use exampleTestcases
        if (sections.examples.isEmpty()) {
            sections.examples = "";
        }
        
        // If no constraints found, provide default message
        if (sections.constraints.isEmpty()) {
            sections.constraints = "Constraints information not found in problem content";
        }
        
        return sections;
    }
    
    private void saveSectionContent(ContentSections sections, String sectionType, String content) {
        if (content.isEmpty()) return;
        
        switch (sectionType) {
            case "description":
                sections.description = content;
                break;
            case "examples":
                if (!sections.examples.isEmpty()) {
                    sections.examples += "\n\n" + content;
                } else {
                    sections.examples = content;
                }
                break;
            case "constraints":
                if (!sections.constraints.isEmpty()) {
                    sections.constraints += "\n\n" + content;
                } else {
                    sections.constraints = content;
                }
                break;
        }
    }
    
    private String cleanDescriptionText(String text) {
        if (text.isEmpty()) return "Problem description not available";
        
        // Remove example sections that might have leaked into description
        String[] examplePatterns = {
            "(?i)example\\s+\\d+:.*",
            "(?i)input:.*",
            "(?i)output:.*"
        };
        
        for (String pattern : examplePatterns) {
            text = text.replaceAll(pattern, "").trim();
        }
        
        // Additional cleanup for any remaining HTML entities
        text = cleanHtmlEntities(text);
        
        return text.replaceAll("\n{3,}", "\n\n").trim();
    }
    
    private String cleanExamplesText(String text) {
        if (text.isEmpty()) return "";
        
        // Clean HTML entities from examples
        text = cleanHtmlEntities(text);
        
        // Format examples nicely
        return text.replaceAll("\n{3,}", "\n\n").trim();
    }
    
    private String cleanConstraintsText(String text) {
        if (text.isEmpty()) return "Constraints information not available";
        
        // Clean HTML entities from constraints
        text = cleanHtmlEntities(text);
        
        return text.replaceAll("\n{3,}", "\n\n").trim();
    }
    
    private String cleanHtmlEntities(String text) {
        return text
                // Remove any remaining HTML entities
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&quot;", "\"")
                .replaceAll("&#39;", "'")
                .replaceAll("&#x27;", "'")
                .replaceAll("&apos;", "'")
                .replaceAll("&ldquo;", "\"")
                .replaceAll("&rdquo;", "\"")
                .replaceAll("&lsquo;", "'")
                .replaceAll("&rsquo;", "'")
                .replaceAll("&ndash;", "-")
                .replaceAll("&mdash;", "—")
                .replaceAll("&hellip;", "...")
                // Remove any remaining numeric or named entities
                .replaceAll("&#\\d+;", "")
                .replaceAll("&[a-zA-Z]+;", "")
                // Clean up extra spaces
                .replaceAll("\\s+", " ")
                .trim();
    }
    
    private String formatExamples(String examples) {
        // Clean HTML entities first
        examples = cleanHtmlEntities(examples);
        
        String[] testCases = examples.split("\\n");
        StringBuilder formatted = new StringBuilder();
        
        for (int i = 0; i < testCases.length; i++) {
            if (i % 2 == 0) {
                formatted.append("Example ").append((i / 2) + 1).append(":\n");
                formatted.append("Input: ").append(testCases[i].trim()).append("\n");
                if (i + 1 < testCases.length) {
                    formatted.append("Output: ").append(testCases[i + 1].trim()).append("\n\n");
                }
            }
        }
        
        return formatted.toString().trim();
    }
    
    private void displayTopicTags(JsonArray topicTags) {
        topicChipGroup.removeAllViews();
        
        for (int i = 0; i < topicTags.size(); i++) {
            JsonObject topic = topicTags.get(i).getAsJsonObject();
            String topicName = topic.get("name").getAsString();
            
            Chip chip = new Chip(this);
            chip.setText(topicName);
            chip.setChipBackgroundColorResource(R.color.chip_background);
            chip.setTextColor(ContextCompat.getColor(this, R.color.chip_text));
            chip.setClickable(false);
            topicChipGroup.addView(chip);
        }
    }
    
    private void displayHints(JsonArray hints) {
        if (hints.size() == 0) {
            hintsTitle.setVisibility(View.GONE);
            return;
        }
        
        hintsContainer.removeAllViews();
        for (int i = 0; i < hints.size(); i++) {
            String hint = hints.get(i).getAsString();
            String cleanHint = parseHtmlContent(hint);
            
            View hintView = getLayoutInflater().inflate(R.layout.item_hint, hintsContainer, false);
            TextView hintNumber = hintView.findViewById(R.id.hintNumber);
            TextView hintText = hintView.findViewById(R.id.hintText);
            
            hintNumber.setText(String.valueOf(i + 1));
            hintText.setText(cleanHint);
            
            hintsContainer.addView(hintView);
        }
    }
    
    private void parseAcceptanceRate(String stats) {
        try {
            JsonObject statsJson = JsonParser.parseString(stats).getAsJsonObject();
            if (statsJson.has("acRate")) {
                String acRate = statsJson.get("acRate").getAsString();
                acceptanceRate.setText(acRate + " Acceptance Rate");
            }
        } catch (Exception e) {
            // Keep the original acceptance rate if parsing fails
        }
    }
    
    private void setDifficultyBadge(String difficulty) {
        difficultyBadge.setText(difficulty);
        switch (difficulty.toLowerCase()) {
            case "easy":
                difficultyBadge.setBackground(ContextCompat.getDrawable(this, R.drawable.difficulty_badge_easy));
                break;
            case "medium":
                difficultyBadge.setBackground(ContextCompat.getDrawable(this, R.drawable.difficulty_badge_medium));
                break;
            case "hard":
                difficultyBadge.setBackground(ContextCompat.getDrawable(this, R.drawable.difficulty_badge_hard));
                break;
        }
    }
    
    private void showLoading(boolean show) {
        loadingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
        contentContainer.setVisibility(show ? View.GONE : View.VISIBLE);
    }
    
    private void showError(String message) {
        problemDescription.setText("Error: " + message);
        exampleInput.setText("Unable to load examples");
        constraints.setText("Unable to load constraints");
    }
    
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
        
        solveButton.setOnClickListener(v -> {
            android.widget.Toast.makeText(this, "Navigate to coding environment", android.widget.Toast.LENGTH_SHORT).show();
        });
        
        hintsTitle.setOnClickListener(v -> {
            if (hintsContainer.getVisibility() == View.VISIBLE) {
                hintsContainer.setVisibility(View.GONE);
                hintsTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_expand_more, 0);
            } else {
                hintsContainer.setVisibility(View.VISIBLE);
                hintsTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_expand_less, 0);
            }
        });
    }
    
    // Fallback methods for when API fails
    private String getSampleDescription(String title) {
        if (title.contains("Two Sum")) {
            return "Given an array of integers nums and an integer target, return indices of the two numbers such that they add up to target.\n\nYou may assume that each input would have exactly one solution, and you may not use the same element twice.\n\nYou can return the answer in any order.";
        } else if (title.contains("Duplicate")) {
            return "Given an integer array nums, return true if any value appears at least twice in the array, and return false if every element is distinct.";
        } else {
            return "Problem description will be loaded from LeetCode API. If you're seeing this message, there might be a network issue or the problem data is not available.";
        }
    }
    
    private String getSampleExample(String title) {
        if (title.contains("Two Sum")) {
            return "Example 1:\nInput: nums = [2,7,11,15], target = 9\nOutput: [0,1]\nExplanation: Because nums[0] + nums[1] == 9, we return [0, 1].";
        } else if (title.contains("Duplicate")) {
            return "Example 1:\nInput: nums = [1,2,3,1]\nOutput: true\nExplanation: The element 1 appears at index 0 and 3.";
        } else {
            return "Examples will be loaded from LeetCode API. Please check your network connection.";
        }
    }
    
    private String getSampleConstraints(String title) {
        return "• Constraints will be loaded from LeetCode API\n• Please check your network connection\n• API might be temporarily unavailable";
    }
    
    private void displaySampleHints(String title) {
        hintsContainer.removeAllViews();
        String[] hints = {"Hint 1: API data will be loaded automatically", "Hint 2: Check network connection if content is missing"};
        
        for (int i = 0; i < hints.length; i++) {
            View hintView = getLayoutInflater().inflate(R.layout.item_hint, hintsContainer, false);
            TextView hintNumber = hintView.findViewById(R.id.hintNumber);
            TextView hintText = hintView.findViewById(R.id.hintText);
            
            hintNumber.setText(String.valueOf(i + 1));
            hintText.setText(hints[i]);
            
            hintsContainer.addView(hintView);
        }
    }
}
