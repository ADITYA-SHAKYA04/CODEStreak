package com.example.codestreak;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.ProgressBar;
import android.widget.Toast;
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
    private MaterialButton aiChatButton;
    private ImageView backButton;
    private ImageView starIcon;
    private LinearLayout hintsContainer;
    private TextView hintsTitle;
    private ProgressBar loadingIndicator;
    private ScrollView contentContainer;
    private ViewStub skeletonStub;
    private View skeletonView;
    
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
        
        // Set status bar color to match activity background
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.background_primary, getTheme()));
        }
        
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
        aiChatButton = findViewById(R.id.aiChatButton);
        backButton = findViewById(R.id.backButton);
        starIcon = findViewById(R.id.starIcon);
        hintsContainer = findViewById(R.id.hintsContainer);
        hintsTitle = findViewById(R.id.hintsTitle);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        contentContainer = findViewById(R.id.contentContainer);
        skeletonStub = findViewById(R.id.skeletonStub);
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
        
        // Setup star icon
        setupStarIcon(problemId, title, difficulty, acceptance, titleSlug);
    }
    
    private void fetchProblemDetails() {
        showLoading(true);
        
        // Show immediate placeholder content while loading
        showPlaceholderContent();
        
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
                    android.util.Log.d("ProblemDetail", "Using content examples: " + sections.examples.substring(0, Math.min(100, sections.examples.length())) + "...");
                    exampleInput.setText(formatContentExamples(sections.examples));
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
        
        // First, try to split content at the first example
        String[] parts = content.split("(?i)(?=example\\s*\\d*\\s*:)", 2);
        
        if (parts.length >= 2) {
            // We found examples - part[0] is description, part[1] onwards are examples
            sections.description = parts[0].trim();
            
            // Extract examples and constraints from the remaining content
            String remainingContent = parts[1];
            String[] exampleConstraintSplit = remainingContent.split("(?i)(?=constraints?\\s*:)", 2);
            
            sections.examples = exampleConstraintSplit[0].trim();
            if (exampleConstraintSplit.length > 1) {
                sections.constraints = exampleConstraintSplit[1].trim();
            }
        } else {
            // No clear example section found, try alternative approach
            String[] lines = content.split("\n");
            StringBuilder currentSection = new StringBuilder();
            String currentSectionType = "description";
            boolean foundFirstExample = false;
            
            for (String line : lines) {
                String lowerLine = line.toLowerCase().trim();
                
                // Check if this line starts a new section
                if (!foundFirstExample && (lowerLine.matches(".*example\\s*\\d*\\s*:.*") || 
                    (lowerLine.startsWith("example") && lowerLine.contains(":")))) {
                    // First example found - save description and start examples
                    sections.description = currentSection.toString().trim();
                    currentSection = new StringBuilder();
                    currentSectionType = "examples";
                    foundFirstExample = true;
                    currentSection.append(line).append("\n");
                }
                else if (foundFirstExample && (lowerLine.startsWith("input:") || lowerLine.startsWith("output:") || lowerLine.startsWith("explanation:"))) {
                    // Continue with examples section
                    currentSection.append(line).append("\n");
                }
                else if (lowerLine.startsWith("constraints:") || lowerLine.startsWith("constraint:")) {
                    // Save previous section and start constraints
                    if (currentSectionType.equals("description")) {
                        sections.description = currentSection.toString().trim();
                    } else if (currentSectionType.equals("examples")) {
                        sections.examples = currentSection.toString().trim();
                    }
                    currentSection = new StringBuilder();
                    currentSectionType = "constraints";
                    currentSection.append(line).append("\n");
                }
                else if (lowerLine.startsWith("note:") || lowerLine.startsWith("follow") || lowerLine.startsWith("hint:")) {
                    // Save previous section and add to constraints
                    if (currentSectionType.equals("description")) {
                        sections.description = currentSection.toString().trim();
                    } else if (currentSectionType.equals("examples")) {
                        sections.examples = currentSection.toString().trim();
                    }
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
            if (currentSectionType.equals("description") && sections.description.isEmpty()) {
                sections.description = currentSection.toString().trim();
            } else if (currentSectionType.equals("examples") && sections.examples.isEmpty()) {
                sections.examples = currentSection.toString().trim();
            } else if (currentSectionType.equals("constraints")) {
                sections.constraints = currentSection.toString().trim();
            }
        }
        
        // Clean up sections
        sections.description = cleanDescriptionText(sections.description);
        sections.examples = cleanExamplesText(sections.examples);
        sections.constraints = cleanConstraintsText(sections.constraints);
        
        // Safety check: if description is empty or too short, try to preserve original content
        if (sections.description.isEmpty() || sections.description.length() < 50) {
            android.util.Log.w("ProblemDetail", "Description too short, using fallback");
            // Take the first meaningful part of the content as description
            String[] contentLines = content.split("\n");
            StringBuilder fallbackDescription = new StringBuilder();
            int lineCount = 0;
            
            for (String line : contentLines) {
                String trimmedLine = line.trim();
                if (!trimmedLine.isEmpty() && lineCount < 20) { // Take first 20 non-empty lines
                    String lowerLine = trimmedLine.toLowerCase();
                    // Skip obvious example/constraint headers
                    if (!lowerLine.startsWith("example ") && 
                        !lowerLine.equals("input:") && 
                        !lowerLine.equals("output:") &&
                        !lowerLine.startsWith("constraints:")) {
                        fallbackDescription.append(trimmedLine).append("\n");
                        lineCount++;
                    }
                }
            }
            
            if (fallbackDescription.length() > 0) {
                sections.description = cleanHtmlEntities(fallbackDescription.toString().trim());
            } else {
                sections.description = "Problem description not available";
            }
        }
        
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
        
        // Remove any example sections that leaked into description
        String[] lines = text.split("\n");
        StringBuilder cleanedText = new StringBuilder();
        boolean skipMode = false;
        
        for (String line : lines) {
            String lowerLine = line.toLowerCase().trim();
            
            // Start skipping when we hit an example
            if (lowerLine.matches(".*example\\s*\\d*\\s*:.*") || 
                (lowerLine.startsWith("example") && lowerLine.contains(":"))) {
                skipMode = true;
                continue;
            }
            
            // Skip input/output/explanation lines that are part of examples
            if (skipMode && (lowerLine.startsWith("input:") || lowerLine.startsWith("output:") || lowerLine.startsWith("explanation:"))) {
                continue;
            }
            
            // If we're in skip mode and hit constraints, stop skipping but don't include constraints in description
            if (skipMode && (lowerLine.startsWith("constraints:") || lowerLine.startsWith("constraint:"))) {
                break;
            }
            
            // If we're not in skip mode, include the line
            if (!skipMode) {
                cleanedText.append(line).append("\n");
            }
        }
        
        String result = cleanedText.toString().trim();
        
        // If we removed too much and have very little content left, be more conservative
        if (result.length() < 50 && text.length() > 100) {
            // Try a more conservative approach - just remove clear example markers
            result = text.replaceAll("(?i)example\\s+\\d+:", "")
                        .replaceAll("(?i)^input:", "")
                        .replaceAll("(?i)^output:", "")
                        .replaceAll("(?i)^explanation:", "")
                        .trim();
        }
        
        // Clean HTML entities and markdown-style formatting
        result = cleanHtmlEntities(result);
        result = cleanMarkdownFormatting(result);
        
        return result.replaceAll("\n{3,}", "\n\n").trim();
    }
    
    private String cleanExamplesText(String text) {
        if (text.isEmpty()) return "";
        
        // Clean HTML entities and markdown formatting from examples
        text = cleanHtmlEntities(text);
        text = cleanMarkdownFormatting(text);
        
        // Format examples nicely
        return text.replaceAll("\n{3,}", "\n\n").trim();
    }
    
    private String cleanConstraintsText(String text) {
        if (text.isEmpty()) return "Constraints information not available";
        
        // Clean HTML entities and markdown formatting from constraints
        text = cleanHtmlEntities(text);
        text = cleanMarkdownFormatting(text);
        
        return text.replaceAll("\n{3,}", "\n\n").trim();
    }
    
    /**
     * Clean markdown-style formatting that appears in LeetCode problem descriptions
     */
    private String cleanMarkdownFormatting(String text) {
        return text
                // Remove double asterisks (bold markers)
                .replaceAll("\\*\\*", "")
                // Remove single asterisks around single characters/words (italic markers)  
                .replaceAll("\\*([0-9a-zA-Z])\\*", "$1")
                // Remove backticks (code markers)
                .replaceAll("`([^`]+)`", "$1")
                // Remove escaped quotes
                .replaceAll("\\\\\"", "\"")
                .replaceAll("\\\\'", "'")
                // Clean up any remaining escape characters
                .replaceAll("\\\\(.)", "$1")
                .trim();
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
        
        // Check if this looks like the LeetCode exampleTestcases format (input data only)
        if (!examples.contains("Example") && !examples.contains("Input:") && !examples.contains("Output:")) {
            return formatExampleTestcases(examples);
        }
        
        // If it already contains formatted examples, just clean it up
        return examples.replaceAll("\\n{3,}", "\n\n").trim();
    }
    
    private String formatExampleTestcases(String testCasesData) {
        // The exampleTestcases field usually contains just the input data separated by newlines
        // We need to parse this and create proper example format
        android.util.Log.d("ProblemDetail", "formatExampleTestcases input: " + testCasesData);
        
        String[] lines = testCasesData.split("\\n");
        StringBuilder formatted = new StringBuilder();
        
        // Group the test case inputs - LeetCode usually provides inputs in groups
        // For problems like "Two Sum", it might be: [2,7,11,15], 9, [3,2,4], 6
        
        int exampleNumber = 1;
        int i = 0;
        
        while (i < lines.length && exampleNumber <= 5) { // Limit to 5 examples
            String currentLine = lines[i].trim();
            
            if (currentLine.isEmpty()) {
                i++;
                continue;
            }
            
            formatted.append("Example ").append(exampleNumber).append(":\n");
            
            // Try to determine the input format based on the problem
            String input = currentLine;
            
            // Handle different input patterns
            if (input.startsWith("[") && input.endsWith("]")) {
                // Array input
                formatted.append("Input: ").append(input);
                
                // Check if next line is another parameter (like target value)
                if (i + 1 < lines.length) {
                    String nextLine = lines[i + 1].trim();
                    if (!nextLine.isEmpty() && !nextLine.startsWith("[")) {
                        // This looks like a second parameter
                        formatted.append(", ").append(nextLine);
                        i++; // Skip the next line as we've consumed it
                    }
                }
            } else if (input.startsWith("\"") && input.endsWith("\"")) {
                // String input
                formatted.append("Input: ").append(input);
            } else if (input.matches("\\d+")) {
                // Numeric input
                formatted.append("Input: ").append(input);
            } else {
                // Generic input
                formatted.append("Input: ").append(input);
            }
            
            formatted.append("\n");
            formatted.append("Output: [Expected output - see problem description]\n\n");
            
            i++;
            exampleNumber++;
        }
        
        // If we only found one example, try to parse it differently
        if (exampleNumber == 2 && lines.length > 2) {
            // Maybe the format is different - try parsing pairs of lines
            formatted = new StringBuilder();
            exampleNumber = 1;
            
            for (int j = 0; j < Math.min(lines.length, 8); j += 2) {
                if (j + 1 < lines.length && !lines[j].trim().isEmpty()) {
                    formatted.append("Example ").append(exampleNumber).append(":\n");
                    formatted.append("Input: ").append(lines[j].trim());
                    
                    if (!lines[j + 1].trim().isEmpty()) {
                        formatted.append(", ").append(lines[j + 1].trim());
                    }
                    
                    formatted.append("\n");
                    formatted.append("Output: [Expected output - see problem description]\n\n");
                    exampleNumber++;
                }
            }
        }
        
        String result = formatted.toString().trim();
        android.util.Log.d("ProblemDetail", "formatExampleTestcases result: " + result);
        return result.isEmpty() ? "No test cases found" : result;
    }
    
    private String formatContentExamples(String contentExamples) {
        // This handles examples extracted from the problem content itself
        android.util.Log.d("ProblemDetail", "formatContentExamples input: " + contentExamples);
        
        if (contentExamples == null || contentExamples.trim().isEmpty()) {
            return "No examples found in content";
        }
        
        // Split by different example patterns
        String[] sections = contentExamples.split("(?i)example\\s*\\d*\\s*:");
        android.util.Log.d("ProblemDetail", "Split into " + sections.length + " sections");
        
        StringBuilder formatted = new StringBuilder();
        int exampleCount = 0;
        
        for (int i = 0; i < sections.length; i++) {
            String section = sections[i].trim();
            
            // Skip empty sections
            if (section.isEmpty()) continue;
            
            // Check if this section contains input/output patterns
            if (section.toLowerCase().contains("input:") || section.toLowerCase().contains("output:")) {
                exampleCount++;
                formatted.append("Example ").append(exampleCount).append(":\n");
                
                // Parse input/output from the section
                String[] lines = section.split("\n");
                
                for (String line : lines) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        String lowerLine = line.toLowerCase();
                        if (lowerLine.startsWith("input:") || 
                            lowerLine.startsWith("output:") ||
                            lowerLine.startsWith("explanation:")) {
                            formatted.append(line).append("\n");
                        } else if (!lowerLine.startsWith("example") && !lowerLine.startsWith("constraint")) {
                            // This might be a continuation of previous line or example data
                            formatted.append(line).append("\n");
                        }
                    }
                }
                
                formatted.append("\n");
            } else if (i > 0) {
                // If we found a section after splitting by "Example" but it doesn't have input/output,
                // it might still contain example data, so include it
                exampleCount++;
                formatted.append("Example ").append(exampleCount).append(":\n");
                formatted.append(section).append("\n\n");
            }
        }
        
        // If no proper examples found, try alternative parsing
        if (exampleCount == 0) {
            android.util.Log.d("ProblemDetail", "No examples found with standard parsing, trying alternative");
            
            // Look for Input: and Output: patterns directly
            String[] lines = contentExamples.split("\n");
            StringBuilder currentExample = new StringBuilder();
            boolean inExample = false;
            exampleCount = 0;
            
            for (String line : lines) {
                String trimmedLine = line.trim();
                String lowerLine = trimmedLine.toLowerCase();
                
                if (lowerLine.startsWith("input:")) {
                    if (inExample && currentExample.length() > 0) {
                        // Save previous example
                        exampleCount++;
                        formatted.append("Example ").append(exampleCount).append(":\n");
                        formatted.append(currentExample.toString()).append("\n");
                        currentExample = new StringBuilder();
                    }
                    inExample = true;
                    currentExample.append(trimmedLine).append("\n");
                } else if (lowerLine.startsWith("output:") || lowerLine.startsWith("explanation:")) {
                    if (inExample) {
                        currentExample.append(trimmedLine).append("\n");
                    }
                } else if (inExample && !trimmedLine.isEmpty() && 
                          !lowerLine.startsWith("constraint") && 
                          !lowerLine.startsWith("note:")) {
                    // Continuation line
                    currentExample.append(trimmedLine).append("\n");
                }
            }
            
            // Add the last example
            if (inExample && currentExample.length() > 0) {
                exampleCount++;
                formatted.append("Example ").append(exampleCount).append(":\n");
                formatted.append(currentExample.toString()).append("\n");
            }
        }
        
        String result = formatted.toString().trim();
        android.util.Log.d("ProblemDetail", "formatContentExamples result: " + result);
        return result.isEmpty() ? "Examples parsing failed" : result;
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
        if (show) {
            // Show skeleton loading
            if (skeletonView == null && skeletonStub != null) {
                skeletonView = skeletonStub.inflate();
                
                // Start shimmer animation for all skeleton views
                startSkeletonAnimation(skeletonView);
            }
            
            loadingIndicator.setVisibility(View.GONE); // Hide progress bar, use skeleton instead
            contentContainer.setVisibility(View.GONE);
            if (skeletonView != null) {
                skeletonView.setVisibility(View.VISIBLE);
            }
        } else {
            // Hide skeleton and show content
            loadingIndicator.setVisibility(View.GONE);
            contentContainer.setVisibility(View.VISIBLE);
            if (skeletonView != null) {
                skeletonView.setVisibility(View.GONE);
                stopSkeletonAnimation(skeletonView);
            }
        }
    }
    
    private void startSkeletonAnimation(View parent) {
        if (parent instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) parent;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof ViewGroup) {
                    startSkeletonAnimation(child);
                } else {
                    // Apply shimmer animation to skeleton elements
                    if (child.getBackground() != null) {
                        android.view.animation.Animation shimmer = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.skeleton_shimmer);
                        child.startAnimation(shimmer);
                    }
                }
            }
        }
    }
    
    private void stopSkeletonAnimation(View parent) {
        if (parent instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) parent;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof ViewGroup) {
                    stopSkeletonAnimation(child);
                } else {
                    child.clearAnimation();
                }
            }
        }
    }
    
    private void showError(String message) {
        problemDescription.setText("Error: " + message);
        exampleInput.setText("Unable to load examples");
        constraints.setText("Unable to load constraints");
    }
    
    private void showPlaceholderContent() {
        // Show immediate placeholder content from intent data while skeleton loads
        Intent intent = getIntent();
        String title = intent.getStringExtra("problem_title");
        String difficulty = intent.getStringExtra("problem_difficulty");
        
        if (title != null && !title.isEmpty()) {
            problemTitle.setText(title);
        }
        
        if (difficulty != null && !difficulty.isEmpty()) {
            difficultyBadge.setText(difficulty);
            // Set difficulty badge background based on difficulty level
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
                default:
                    difficultyBadge.setBackground(ContextCompat.getDrawable(this, R.drawable.difficulty_badge_medium));
                    break;
            }
        }
        
        // Show that content is loading
        problemDescription.setText("Loading problem description...");
        exampleInput.setText("Loading examples...");
        constraints.setText("Loading constraints...");
        acceptanceRate.setText("Loading stats...");
    }
    
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
        
        aiChatButton.setOnClickListener(v -> {
            // Navigate to AI Chat with problem context
            String title = problemTitle.getText().toString();
            String description = problemDescription.getText().toString();
            
            if (description.isEmpty() || description.startsWith("Loading") || description.startsWith("Error:")) {
                description = getSampleDescription(title);
            }
            
            Intent aiChatIntent = new Intent(this, AIChatActivity.class);
            aiChatIntent.putExtra("problem_title", title);
            aiChatIntent.putExtra("problem_description", description);
            startActivity(aiChatIntent);
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
    
    private void setupStarIcon(int problemId, String title, String difficulty, double acceptance, String titleSlug) {
        SharedPreferences prefs = getSharedPreferences("CodeStreakPrefs", MODE_PRIVATE);
        boolean isStarred = prefs.getBoolean("starred_" + problemId, false);
        
        // Update star icon appearance based on starred state
        updateStarIcon(isStarred);
        
        // Set click listener for star icon
        starIcon.setOnClickListener(v -> {
            boolean currentStarred = prefs.getBoolean("starred_" + problemId, false);
            boolean newStarredState = !currentStarred;
            
            SharedPreferences.Editor editor = prefs.edit();
            
            if (newStarredState) {
                // Save problem to revision list
                editor.putBoolean("starred_" + problemId, true);
                editor.putString("starred_title_" + problemId, title);
                editor.putString("starred_difficulty_" + problemId, difficulty);
                editor.putFloat("starred_acceptance_" + problemId, (float) acceptance);
                editor.putString("starred_slug_" + problemId, titleSlug);
                
                // Save companies if available
                String companies = getIntent().getStringExtra("problem_companies");
                if (companies != null) {
                    editor.putString("starred_companies_" + problemId, companies);
                }
                
                // Update starred problems list
                String starredList = prefs.getString("starred_problems_list", "");
                if (!starredList.contains(String.valueOf(problemId))) {
                    if (starredList.isEmpty()) {
                        starredList = String.valueOf(problemId);
                    } else {
                        starredList += "," + problemId;
                    }
                    editor.putString("starred_problems_list", starredList);
                }
                
                editor.apply();
                updateStarIcon(true);
                Toast.makeText(this, "Added to revision list ⭐", Toast.LENGTH_SHORT).show();
            } else {
                // Remove problem from revision list
                editor.remove("starred_" + problemId);
                editor.remove("starred_title_" + problemId);
                editor.remove("starred_difficulty_" + problemId);
                editor.remove("starred_acceptance_" + problemId);
                editor.remove("starred_slug_" + problemId);
                editor.remove("starred_companies_" + problemId);
                editor.remove("starred_topics_" + problemId);
                
                // Update starred problems list
                String starredList = prefs.getString("starred_problems_list", "");
                if (!starredList.isEmpty()) {
                    String[] ids = starredList.split(",");
                    StringBuilder newList = new StringBuilder();
                    for (String id : ids) {
                        if (!id.equals(String.valueOf(problemId))) {
                            if (newList.length() > 0) {
                                newList.append(",");
                            }
                            newList.append(id);
                        }
                    }
                    editor.putString("starred_problems_list", newList.toString());
                }
                
                editor.apply();
                updateStarIcon(false);
                Toast.makeText(this, "Removed from revision list", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void updateStarIcon(boolean isStarred) {
        if (isStarred) {
            starIcon.setImageResource(android.R.drawable.btn_star_big_on);
            starIcon.setColorFilter(android.graphics.Color.parseColor("#FFA116"));
        } else {
            starIcon.setImageResource(android.R.drawable.btn_star_big_off);
            starIcon.setColorFilter(android.graphics.Color.parseColor("#999999"));
        }
    }
}
