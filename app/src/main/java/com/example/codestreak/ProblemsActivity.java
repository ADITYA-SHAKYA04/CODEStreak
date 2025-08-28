package com.example.codestreak;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.CheckBox;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import okhttp3.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ProblemsActivity extends BaseActivity {
    
    private RecyclerView problemsRecyclerView;
    private EditText searchEditText;
    private TextView problemCountText;
    private TextView selectedTopicsText;
    private LinearLayout filterTopicsButton;
    private TextView sortButton; // Changed from LinearLayout to TextView
    private TextView sortText; // Can be removed as we're using sortButton for both
    
    // Skeleton loading
    private ViewStub skeletonStub;
    private View skeletonView;
    private LinearLayout contentContainer;
    
    private ProblemsAdapter problemsAdapter;
    
    private List<Problem> allProblems;
    private List<Problem> filteredProblems;
    private List<Topic> topics;
    private Set<String> selectedTopics = new HashSet<>();
    private Set<String> selectedDifficulties = new HashSet<>();
    private String sortOrder = "Default"; // Default, Easy->Hard, Hard->Easy, A-Z, Z-A
    
    // Dynamic problem loading
    private OkHttpClient httpClient;
    private List<Problem> realLeetCodeProblems = new ArrayList<>();
    private boolean realProblemsLoaded = false;
    
    // Infinite scrolling variables
    private boolean isLoading = false;
    private int currentPage = 1;
    private final int PROBLEMS_PER_PAGE = 20;
    private final int TOTAL_PROBLEMS = 5000; // Support up to 5000 problems (mix of real + generated)
    private LinearLayoutManager layoutManager;
    private boolean showingAllTopics = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_problems);
        
        initViews();
        setupRecyclerViews();
        loadData();
        setupSearch();
        setupBottomNavigation();
    }
    
    private void initViews() {
        problemsRecyclerView = findViewById(R.id.problemsRecyclerView);
        searchEditText = findViewById(R.id.searchEditText);
        problemCountText = findViewById(R.id.problemCountText);
        selectedTopicsText = findViewById(R.id.selectedTopicsText);
        filterTopicsButton = findViewById(R.id.filterTopicsButton);
        sortButton = findViewById(R.id.sortButton);
        sortText = sortButton; // Use the same TextView for both
        
        // Initialize skeleton loading
        skeletonStub = findViewById(R.id.skeletonStub);
        contentContainer = findViewById(R.id.contentContainer);
        
        // Setup filter topics button
        filterTopicsButton.setOnClickListener(v -> showTopicSelectionDialog());
        
        // Setup sort button
        sortButton.setOnClickListener(v -> showSortDialog());
        
        // Setup back button
        ImageView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            finish(); // Go back to previous activity
        });
    }
    
    private void setupRecyclerViews() {
        // Problems RecyclerView
        layoutManager = new LinearLayoutManager(this);
        problemsRecyclerView.setLayoutManager(layoutManager);
        
        // Add scroll listener for infinite scrolling
        problemsRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                if (dy > 0) { // Only load when scrolling down
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                    int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
                    
                    // Debug logging
                    android.util.Log.d("InfiniteScroll", "Visible: " + visibleItemCount + 
                        ", Total: " + totalItemCount + ", First: " + firstVisibleItemPosition + 
                        ", Last: " + lastVisibleItemPosition + ", isLoading: " + isLoading);
                    
                    // Check if we should load more items (when scrolled to near bottom)
                    if (!isLoading && totalItemCount > 0 && lastVisibleItemPosition >= totalItemCount - 5) {
                        android.util.Log.d("InfiniteScroll", "⚡ Pre-loading more problems...");
                        loadMoreProblems();
                    }
                }
            }
        });
    }
    
    private void loadData() {
        // Show skeleton loading immediately
        showSkeletonLoading(true);
        
        // Initialize HTTP client for API calls with optimized settings
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)    // Reduced from 30s
                .readTimeout(10, TimeUnit.SECONDS)      // Reduced from 30s
                .writeTimeout(5, TimeUnit.SECONDS)      // Added write timeout
                .retryOnConnectionFailure(true)         // Enable retries
                .build();
        
        // Initialize complete topics list with actual LeetCode data
        List<Topic> allTopicsList = Arrays.asList(
            // Top 10 most common topics (initially visible)
            new Topic("Array", 1977, true),
            new Topic("String", 809, false),
            new Topic("Hash Table", 722, false),
            new Topic("Dynamic Programming", 609, false),
            new Topic("Math", 607, false),
            new Topic("Sorting", 467, false),
            new Topic("Greedy", 430, false),
            new Topic("Depth-First Search", 329, false),
            new Topic("Binary Search", 318, false),
            new Topic("Database", 310, false),
            // Additional topics (initially hidden)
            new Topic("Matrix", 263, false),
            new Topic("Tree", 252, false),
            new Topic("Breadth-First Search", 248, false),
            new Topic("Bit Manipulation", 247, false),
            new Topic("Two Pointers", 224, false),
            new Topic("Prefix Sum", 210, false),
            new Topic("Heap (Priority Queue)", 200, false),
            new Topic("Simulation", 187, false),
            new Topic("Binary Tree", 177, false),
            new Topic("Graph", 175, false),
            new Topic("Stack", 172, false),
            new Topic("Counting", 168, false),
            new Topic("Sliding Window", 155, false),
            new Topic("Design", 130, false),
            new Topic("Enumeration", 123, false),
            new Topic("Backtracking", 109, false),
            new Topic("Union Find", 93, false),
            new Topic("Linked List", 81, false),
            new Topic("Number Theory", 80, false),
            new Topic("Ordered Set", 74, false),
            new Topic("Monotonic Stack", 69, false),
            new Topic("Segment Tree", 66, false),
            new Topic("Trie", 58, false),
            new Topic("Combinatorics", 56, false),
            new Topic("Bitmask", 55, false),
            new Topic("Divide and Conquer", 53, false),
            new Topic("Queue", 50, false),
            new Topic("Recursion", 49, false),
            new Topic("Geometry", 44, false),
            new Topic("Binary Indexed Tree", 44, false),
            new Topic("Memoization", 42, false),
            new Topic("Hash Function", 40, false),
            new Topic("Binary Search Tree", 40, false),
            new Topic("Shortest Path", 37, false),
            new Topic("String Matching", 37, false),
            new Topic("Topological Sort", 37, false),
            new Topic("Rolling Hash", 31, false),
            new Topic("Game Theory", 29, false),
            new Topic("Interactive", 23, false),
            new Topic("Data Stream", 21, false),
            new Topic("Monotonic Queue", 20, false),
            new Topic("Brainteaser", 19, false),
            new Topic("Doubly-Linked List", 13, false),
            new Topic("Randomized", 12, false),
            new Topic("Merge Sort", 12, false),
            new Topic("Counting Sort", 11, false),
            new Topic("Iterator", 9, false),
            new Topic("Concurrency", 9, false),
            new Topic("Probability and Statistics", 7, false),
            new Topic("Quickselect", 7, false),
            new Topic("Suffix Array", 7, false),
            new Topic("Line Sweep", 7, false),
            new Topic("Minimum Spanning Tree", 6, false),
            new Topic("Bucket Sort", 6, false),
            new Topic("Shell", 4, false),
            new Topic("Reservoir Sampling", 4, false),
            new Topic("Strongly Connected Component", 3, false),
            new Topic("Eulerian Circuit", 3, false),
            new Topic("Radix Sort", 3, false),
            new Topic("Rejection Sampling", 2, false),
            new Topic("Biconnected Component", 1, false)
        );
        
        // Initially show only top 10 topics
        topics = new ArrayList<>(allTopicsList.subList(0, 10));
        
        // Initialize empty lists
        allProblems = new ArrayList<>();
        filteredProblems = new ArrayList<>();
        
        // Setup adapter
        problemsAdapter = new ProblemsAdapter(filteredProblems);
        problemsRecyclerView.setAdapter(problemsAdapter);
        
        // Load first page immediately with static data for instant UI
        loadMoreProblems();
        
        // Hide skeleton loading once initial content is loaded
        showSkeletonLoading(false);
        
        // Load real LeetCode problems in background (non-blocking)
        loadRealLeetCodeProblemsInBackground();
    }
    
    private void loadRealLeetCodeProblemsInBackground() {
        // Start with a smaller batch for faster initial response
        android.util.Log.d("ProblemsActivity", "Starting background real problem loading...");
        
        // Use a separate thread to avoid blocking UI
        new Thread(() -> {
            try {
                loadRealProblemsProgressive();
            } catch (Exception e) {
                android.util.Log.e("ProblemsActivity", "Background loading failed: " + e.getMessage());
            }
        }).start();
    }
    
    private void loadRealProblemsProgressive() {
        // Start with just the first 50 most popular problems for immediate enhancement
        String query = "{\n" +
                "  allQuestions {\n" +
                "    acRate\n" +
                "    difficulty\n" +
                "    questionId\n" +
                "    title\n" +
                "    titleSlug\n" +
                "    isPaidOnly\n" +
                "    topicTags {\n" +
                "      name\n" +
                "    }\n" +
                "  }\n" +
                "}";
        
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("query", query);
        
        RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.get("application/json; charset=utf-8")
        );
        
        Request request = new Request.Builder()
                .url("https://leetcode.com/graphql/")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .addHeader("Accept", "application/json")
                .addHeader("Referer", "https://leetcode.com/")
                .build();
        
        // Create a client with faster timeouts for this specific request
        OkHttpClient fastClient = httpClient.newBuilder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
        
        try {
            Response response = fastClient.newCall(request).execute();
            String responseBody = response.body() != null ? response.body().string() : "";
            
            if (response.isSuccessful() && !responseBody.isEmpty()) {
                parseAllQuestions(responseBody);
                
                runOnUiThread(() -> {
                    realProblemsLoaded = true;
                    android.util.Log.d("ProblemsActivity", "✅ Background loaded " + realLeetCodeProblems.size() + " real problems");
                    
                    // Gradually replace generated problems with real ones
                    enhanceExistingProblemsWithRealData();
                });
            } else {
                // Fallback to pagination if main API fails
                loadPaginatedProblemsProgressive(0, 100);
            }
        } catch (Exception e) {
            android.util.Log.e("ProblemsActivity", "Main API failed, trying pagination: " + e.getMessage());
            loadPaginatedProblemsProgressive(0, 50); // Start with smaller batch
        }
    }
    
    private void loadPaginatedProblemsProgressive(int skip, int limit) {
        String query = "{\n" +
                "  problems: problemsetQuestionList(\n" +
                "    categorySlug: \"\"\n" +
                "    limit: " + limit + "\n" +
                "    skip: " + skip + "\n" +
                "    filters: {}\n" +
                "  ) {\n" +
                "    questions {\n" +
                "      acRate\n" +
                "      difficulty\n" +
                "      frontendQuestionId\n" +
                "      paidOnly\n" +
                "      title\n" +
                "      titleSlug\n" +
                "      topicTags {\n" +
                "        name\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
        
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("query", query);
        
        RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.get("application/json; charset=utf-8")
        );
        
        Request request = new Request.Builder()
                .url("https://leetcode.com/graphql/")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .addHeader("Accept", "application/json")
                .addHeader("Referer", "https://leetcode.com/")
                .build();
        
        // Create a client with faster timeouts
        OkHttpClient fastClient = httpClient.newBuilder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
        
        try {
            Response response = fastClient.newCall(request).execute();
            String responseBody = response.body() != null ? response.body().string() : "";
            
            if (response.isSuccessful()) {
                int newProblems = parsePaginatedProblems(responseBody);
                android.util.Log.d("ProblemsActivity", "📦 Loaded batch: " + newProblems + " problems (total: " + realLeetCodeProblems.size() + ")");
                
                runOnUiThread(() -> {
                    realProblemsLoaded = true;
                    enhanceExistingProblemsWithRealData();
                });
                
                // Continue loading more in background if successful and we got a full batch
                if (newProblems >= limit && realLeetCodeProblems.size() < 500) {
                    // Add delay to avoid rate limiting
                    Thread.sleep(100);
                    loadPaginatedProblemsProgressive(skip + limit, Math.min(limit, 100));
                }
            }
        } catch (Exception e) {
            android.util.Log.e("ProblemsActivity", "Pagination failed: " + e.getMessage());
            runOnUiThread(() -> {
                realProblemsLoaded = true; // Use static mappings as fallback
            });
        }
    }
    
    private void enhanceExistingProblemsWithRealData() {
        if (realLeetCodeProblems.isEmpty()) return;
        
        // Replace generated problems with real ones where available
        boolean hasChanges = false;
        for (int i = 0; i < allProblems.size(); i++) {
            Problem existingProblem = allProblems.get(i);
            
            // Find matching real problem
            Problem realProblem = realLeetCodeProblems.stream()
                    .filter(rp -> rp.getId() == existingProblem.getId())
                    .findFirst()
                    .orElse(null);
            
            if (realProblem != null) {
                allProblems.set(i, realProblem);
                hasChanges = true;
            }
        }
        
        if (hasChanges) {
            // Update filtered problems too
            filterProblems(searchEditText.getText().toString());
            android.util.Log.d("ProblemsActivity", "🔄 Enhanced existing problems with real data");
        }
    }
    
    private void loadRealLeetCodeProblems() {
        // Try to get all questions using a simpler API approach
        String query = "{\n" +
                "  allQuestions {\n" +
                "    acRate\n" +
                "    difficulty\n" +
                "    questionId\n" +
                "    title\n" +
                "    titleSlug\n" +
                "    isPaidOnly\n" +
                "    topicTags {\n" +
                "      name\n" +
                "    }\n" +
                "  }\n" +
                "}";
        
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("query", query);
        
        RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.get("application/json; charset=utf-8")
        );
        
        Request request = new Request.Builder()
                .url("https://leetcode.com/graphql/")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .addHeader("Accept", "application/json")
                .addHeader("Referer", "https://leetcode.com/")
                .build();
        
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                android.util.Log.e("ProblemsActivity", "Failed to load real problems: " + e.getMessage());
                runOnUiThread(() -> {
                    // Fall back to generated problems
                    loadMoreProblems();
                });
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                
                try {
                    parseAllQuestions(responseBody);
                    runOnUiThread(() -> {
                        realProblemsLoaded = true;
                        android.util.Log.d("ProblemsActivity", "Loaded " + realLeetCodeProblems.size() + " real problems from allQuestions API");
                        // Load first page which will now use real data
                        loadMoreProblems();
                    });
                } catch (Exception e) {
                    android.util.Log.e("ProblemsActivity", "Error parsing real problems, falling back to alternative API: " + e.getMessage());
                    // Try alternative pagination approach
                    loadPaginatedProblems(0);
                }
            }
        });
    }
    
    private void parseAllQuestions(String responseBody) {
        JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
        
        if (!jsonResponse.has("data")) {
            throw new RuntimeException("No data in response");
        }
        
        JsonObject data = jsonResponse.getAsJsonObject("data");
        if (!data.has("allQuestions")) {
            throw new RuntimeException("No allQuestions in response");
        }
        
        JsonArray questions = data.getAsJsonArray("allQuestions");
        realLeetCodeProblems.clear();
        
        int problemIdCounter = 1; // For problems that don't have proper IDs
        
        for (int i = 0; i < questions.size(); i++) {
            JsonObject questionObj = questions.get(i).getAsJsonObject();
            
            // Skip paid-only problems
            if (questionObj.has("isPaidOnly") && questionObj.get("isPaidOnly").getAsBoolean()) {
                continue;
            }
            
            // Get problem ID - try different field names
            int id = problemIdCounter++;
            if (questionObj.has("questionId") && !questionObj.get("questionId").isJsonNull()) {
                try {
                    id = Integer.parseInt(questionObj.get("questionId").getAsString());
                } catch (Exception e) {
                    // Use counter if parsing fails
                }
            }
            
            String title = questionObj.get("title").getAsString();
            String titleSlug = questionObj.get("titleSlug").getAsString();
            String difficulty = questionObj.get("difficulty").getAsString();
            double acRate = questionObj.get("acRate").getAsDouble();
            
            // Parse topic tags
            List<String> topics = new ArrayList<>();
            if (questionObj.has("topicTags")) {
                JsonArray topicTags = questionObj.getAsJsonArray("topicTags");
                for (int j = 0; j < topicTags.size(); j++) {
                    JsonObject topicObj = topicTags.get(j).getAsJsonObject();
                    topics.add(topicObj.get("name").getAsString());
                }
            }
            
            String companies = "Amazon, Google, Microsoft"; // Default companies for real problems
            
            Problem realProblem = new Problem(id, title, titleSlug, difficulty, acRate, companies, topics);
            realLeetCodeProblems.add(realProblem);
        }
        
        // Sort by problem ID
        realLeetCodeProblems.sort((p1, p2) -> Integer.compare(p1.getId(), p2.getId()));
    }
    
    private void loadPaginatedProblems(int skip) {
        // Alternative approach using pagination if allQuestions fails
        android.util.Log.d("ProblemsActivity", "Trying paginated approach, skip: " + skip);
        
        String query = "{\n" +
                "  problems: problemsetQuestionList(\n" +
                "    categorySlug: \"\"\n" +
                "    limit: 100\n" +
                "    skip: " + skip + "\n" +
                "    filters: {}\n" +
                "  ) {\n" +
                "    questions {\n" +
                "      acRate\n" +
                "      difficulty\n" +
                "      frontendQuestionId\n" +
                "      paidOnly\n" +
                "      title\n" +
                "      titleSlug\n" +
                "      topicTags {\n" +
                "        name\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
        
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("query", query);
        
        RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.get("application/json; charset=utf-8")
        );
        
        Request request = new Request.Builder()
                .url("https://leetcode.com/graphql/")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .addHeader("Accept", "application/json")
                .addHeader("Referer", "https://leetcode.com/")
                .build();
        
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                android.util.Log.e("ProblemsActivity", "Paginated API also failed: " + e.getMessage());
                runOnUiThread(() -> {
                    realProblemsLoaded = true; // Set to true to use static mappings
                    loadMoreProblems();
                });
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                
                try {
                    int newProblems = parsePaginatedProblems(responseBody);
                    android.util.Log.d("ProblemsActivity", "Loaded " + newProblems + " problems from pagination. Total: " + realLeetCodeProblems.size());
                    
                    // Continue fetching if we got a full page
                    if (newProblems >= 100 && realLeetCodeProblems.size() < 2000) {
                        loadPaginatedProblems(skip + 100);
                    } else {
                        runOnUiThread(() -> {
                            realProblemsLoaded = true;
                            android.util.Log.d("ProblemsActivity", "Finished loading. Total real problems: " + realLeetCodeProblems.size());
                            loadMoreProblems();
                        });
                    }
                } catch (Exception e) {
                    android.util.Log.e("ProblemsActivity", "Error in pagination: " + e.getMessage());
                    runOnUiThread(() -> {
                        realProblemsLoaded = true;
                        loadMoreProblems();
                    });
                }
            }
        });
    }
    
    private int parsePaginatedProblems(String responseBody) {
        JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
        
        if (!jsonResponse.has("data")) return 0;
        
        JsonObject data = jsonResponse.getAsJsonObject("data");
        if (!data.has("problems")) return 0;
        
        JsonObject problems = data.getAsJsonObject("problems");
        if (!problems.has("questions")) return 0;
        
        JsonArray questions = problems.getAsJsonArray("questions");
        int count = 0;
        
        for (int i = 0; i < questions.size(); i++) {
            JsonObject questionObj = questions.get(i).getAsJsonObject();
            
            // Skip paid-only problems
            if (questionObj.has("paidOnly") && questionObj.get("paidOnly").getAsBoolean()) {
                continue;
            }
            
            int id = questionObj.get("frontendQuestionId").getAsInt();
            
            // Skip if we already have this problem
            boolean alreadyExists = realLeetCodeProblems.stream()
                    .anyMatch(p -> p.getId() == id);
            if (alreadyExists) continue;
            
            String title = questionObj.get("title").getAsString();
            String titleSlug = questionObj.get("titleSlug").getAsString();
            String difficulty = questionObj.get("difficulty").getAsString();
            double acRate = questionObj.get("acRate").getAsDouble();
            
            // Parse topic tags
            List<String> topics = new ArrayList<>();
            if (questionObj.has("topicTags")) {
                JsonArray topicTags = questionObj.getAsJsonArray("topicTags");
                for (int j = 0; j < topicTags.size(); j++) {
                    JsonObject topicObj = topicTags.get(j).getAsJsonObject();
                    topics.add(topicObj.get("name").getAsString());
                }
            }
            
            String companies = "Amazon, Google, Microsoft";
            
            Problem realProblem = new Problem(id, title, titleSlug, difficulty, acRate, companies, topics);
            realLeetCodeProblems.add(realProblem);
            count++;
        }
        
        // Sort by problem ID after each batch
        realLeetCodeProblems.sort((p1, p2) -> Integer.compare(p1.getId(), p2.getId()));
        return count;
    }
    
    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProblems(s.toString());
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    
    private void onTopicSelected(String topicName, boolean isSelected) {
        if (isSelected) {
            selectedTopics.add(topicName);
        } else {
            selectedTopics.remove(topicName);
        }
        filterProblems(searchEditText.getText().toString());
        updateSelectedTopicsDisplay();
    }
    
    private void showTopicSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_topic_selection, null);
        builder.setView(dialogView);
        
        AlertDialog dialog = builder.create();
        
        // Initialize dialog components
        RecyclerView topicsDialogRecyclerView = dialogView.findViewById(R.id.topicsDialogRecyclerView);
        EditText topicSearchEditText = dialogView.findViewById(R.id.topicSearchEditText);
        TextView selectedTopicsCount = dialogView.findViewById(R.id.selectedTopicsCount);
        TextView clearAllButton = dialogView.findViewById(R.id.clearAllButton);
        TextView cancelButton = dialogView.findViewById(R.id.cancelButton);
        TextView applyButton = dialogView.findViewById(R.id.applyButton);
        
        // Setup RecyclerView
        topicsDialogRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        List<Topic> dialogTopics = new ArrayList<>();
        for (Topic topic : topics) {
            dialogTopics.add(new Topic(topic.getName(), topic.getCount(), selectedTopics.contains(topic.getName())));
        }
        
        TopicDialogAdapter dialogAdapter = new TopicDialogAdapter(dialogTopics, selectedTopicsCount);
        topicsDialogRecyclerView.setAdapter(dialogAdapter);
        
        // Search functionality
        topicSearchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                dialogAdapter.filter(s.toString());
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Clear All button
        clearAllButton.setOnClickListener(v -> {
            dialogAdapter.clearAll();
        });
        
        // Cancel button
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        
        // Apply button
        applyButton.setOnClickListener(v -> {
            selectedTopics.clear();
            selectedTopics.addAll(dialogAdapter.getSelectedTopics());
            filterProblems(searchEditText.getText().toString());
            updateSelectedTopicsDisplay();
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    private void updateSelectedTopicsDisplay() {
        if (selectedTopics.isEmpty()) {
            selectedTopicsText.setText("All Topics");
        } else if (selectedTopics.size() == 1) {
            selectedTopicsText.setText(selectedTopics.iterator().next());
        } else {
            selectedTopicsText.setText(selectedTopics.size() + " topics selected");
        }
    }
    
    private void showSortDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_sort_options, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        
        // Initialize views
        CheckBox easyCheckBox = dialogView.findViewById(R.id.easyCheckBox);
        CheckBox mediumCheckBox = dialogView.findViewById(R.id.mediumCheckBox);
        CheckBox hardCheckBox = dialogView.findViewById(R.id.hardCheckBox);
        RadioGroup sortOrderRadioGroup = dialogView.findViewById(R.id.sortOrderRadioGroup);
        TextView clearSortButton = dialogView.findViewById(R.id.clearSortButton);
        TextView cancelSortButton = dialogView.findViewById(R.id.cancelSortButton);
        TextView applySortButton = dialogView.findViewById(R.id.applySortButton);
        
        // Set current selections
        easyCheckBox.setChecked(selectedDifficulties.contains("Easy"));
        mediumCheckBox.setChecked(selectedDifficulties.contains("Medium"));
        hardCheckBox.setChecked(selectedDifficulties.contains("Hard"));
        
        // Set current sort order
        switch (sortOrder) {
            case "Easy->Hard":
                sortOrderRadioGroup.check(R.id.easyToHardRadio);
                break;
            case "Hard->Easy":
                sortOrderRadioGroup.check(R.id.hardToEasyRadio);
                break;
            case "A-Z":
                sortOrderRadioGroup.check(R.id.aTozRadio);
                break;
            case "Z-A":
                sortOrderRadioGroup.check(R.id.zToARadio);
                break;
            case "Acceptance Asc":
                sortOrderRadioGroup.check(R.id.acceptanceAscRadio);
                break;
            case "Acceptance Desc":
                sortOrderRadioGroup.check(R.id.acceptanceDescRadio);
                break;
            default:
                sortOrderRadioGroup.check(R.id.defaultSortRadio);
                break;
        }
        
        // Clear all button
        clearSortButton.setOnClickListener(v -> {
            selectedDifficulties.clear();
            sortOrder = "Default";
            easyCheckBox.setChecked(false);
            mediumCheckBox.setChecked(false);
            hardCheckBox.setChecked(false);
            sortOrderRadioGroup.check(R.id.defaultSortRadio);
        });
        
        // Cancel button
        cancelSortButton.setOnClickListener(v -> dialog.dismiss());
        
        // Apply button
        applySortButton.setOnClickListener(v -> {
            // Update difficulty selections
            selectedDifficulties.clear();
            if (easyCheckBox.isChecked()) selectedDifficulties.add("Easy");
            if (mediumCheckBox.isChecked()) selectedDifficulties.add("Medium");
            if (hardCheckBox.isChecked()) selectedDifficulties.add("Hard");
            
            // Update sort order
            int selectedRadioId = sortOrderRadioGroup.getCheckedRadioButtonId();
            if (selectedRadioId == R.id.easyToHardRadio) {
                sortOrder = "Easy->Hard";
            } else if (selectedRadioId == R.id.hardToEasyRadio) {
                sortOrder = "Hard->Easy";
            } else if (selectedRadioId == R.id.aTozRadio) {
                sortOrder = "A-Z";
            } else if (selectedRadioId == R.id.zToARadio) {
                sortOrder = "Z-A";
            } else if (selectedRadioId == R.id.acceptanceAscRadio) {
                sortOrder = "Acceptance Asc";
            } else if (selectedRadioId == R.id.acceptanceDescRadio) {
                sortOrder = "Acceptance Desc";
            } else {
                sortOrder = "Default";
            }
            
            // Apply filters and sorting
            filterProblems(searchEditText.getText().toString());
            updateSortButtonText();
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    private void updateSortButtonText() {
        if (selectedDifficulties.isEmpty() && sortOrder.equals("Default")) {
            sortText.setText("Sort");
        } else if (!selectedDifficulties.isEmpty() && sortOrder.equals("Default")) {
            sortText.setText("Filtered");
        } else if (selectedDifficulties.isEmpty() && !sortOrder.equals("Default")) {
            sortText.setText("Sorted");
        } else {
            sortText.setText("Sort+Filter");
        }
    }
    
    private void filterProblems(String searchQuery) {
        filteredProblems.clear();
        
        for (Problem problem : allProblems) {
            boolean matchesSearch = searchQuery.isEmpty() || 
                problem.getTitle().toLowerCase().contains(searchQuery.toLowerCase()) ||
                String.valueOf(problem.getId()).contains(searchQuery);
            
            boolean matchesTopics = selectedTopics.isEmpty() ||
                problem.getTopics().stream().anyMatch(selectedTopics::contains);
            
            boolean matchesDifficulty = selectedDifficulties.isEmpty() ||
                selectedDifficulties.contains(problem.getDifficulty());
            
            if (matchesSearch && matchesTopics && matchesDifficulty) {
                filteredProblems.add(problem);
            }
        }
        
        // Apply sorting
        sortFilteredProblems();
        
        problemsAdapter.notifyDataSetChanged();
        
        // Reset scroll position to top when filtering
        if (!searchQuery.isEmpty() || !selectedTopics.isEmpty() || !selectedDifficulties.isEmpty()) {
            problemsRecyclerView.scrollToPosition(0);
        }
    }
    
    private void sortFilteredProblems() {
        switch (sortOrder) {
            case "Easy->Hard":
                filteredProblems.sort((p1, p2) -> getDifficultyOrder(p1.getDifficulty()) - getDifficultyOrder(p2.getDifficulty()));
                break;
            case "Hard->Easy":
                filteredProblems.sort((p1, p2) -> getDifficultyOrder(p2.getDifficulty()) - getDifficultyOrder(p1.getDifficulty()));
                break;
            case "A-Z":
                filteredProblems.sort((p1, p2) -> p1.getTitle().compareToIgnoreCase(p2.getTitle()));
                break;
            case "Z-A":
                filteredProblems.sort((p1, p2) -> p2.getTitle().compareToIgnoreCase(p1.getTitle()));
                break;
            case "Acceptance Asc":
                filteredProblems.sort((p1, p2) -> Double.compare(p1.getAcceptanceRate(), p2.getAcceptanceRate()));
                break;
            case "Acceptance Desc":
                filteredProblems.sort((p1, p2) -> Double.compare(p2.getAcceptanceRate(), p1.getAcceptanceRate()));
                break;
            default: // "Default"
                filteredProblems.sort((p1, p2) -> Integer.compare(p1.getId(), p2.getId()));
                break;
        }
    }
    
    private int getDifficultyOrder(String difficulty) {
        switch (difficulty) {
            case "Easy": return 1;
            case "Medium": return 2;
            case "Hard": return 3;
            default: return 0;
        }
    }
    
    private void sortProblems(List<Problem> problems) {
        switch (sortOrder) {
            case "Easy->Hard":
                problems.sort((p1, p2) -> getDifficultyOrder(p1.getDifficulty()) - getDifficultyOrder(p2.getDifficulty()));
                break;
            case "Hard->Easy":
                problems.sort((p1, p2) -> getDifficultyOrder(p2.getDifficulty()) - getDifficultyOrder(p1.getDifficulty()));
                break;
            case "A-Z":
                problems.sort((p1, p2) -> p1.getTitle().compareToIgnoreCase(p2.getTitle()));
                break;
            case "Z-A":
                problems.sort((p1, p2) -> p2.getTitle().compareToIgnoreCase(p1.getTitle()));
                break;
            case "Acceptance Asc":
                problems.sort((p1, p2) -> Double.compare(p1.getAcceptanceRate(), p2.getAcceptanceRate()));
                break;
            case "Acceptance Desc":
                problems.sort((p1, p2) -> Double.compare(p2.getAcceptanceRate(), p1.getAcceptanceRate()));
                break;
            default: // "Default"
                problems.sort((p1, p2) -> Integer.compare(p1.getId(), p2.getId()));
                break;
        }
    }
    
    private void loadMoreProblems() {
        if (isLoading || allProblems.size() >= TOTAL_PROBLEMS) {
            return;
        }
        
        isLoading = true;
        problemsAdapter.setLoading(true);
        
        // Immediate loading with minimal delay for smooth UX
        problemsRecyclerView.postDelayed(() -> {
            List<Problem> newProblems;
            
            // Use real problems if loaded and available, otherwise use smart generation
            if (realProblemsLoaded && !realLeetCodeProblems.isEmpty()) {
                newProblems = getRealProblemsForPage(currentPage);
            } else {
                // Use enhanced generation with static mappings for first 200 problems
                newProblems = generateOptimizedProblemsForPage(currentPage);
            }
            
            int oldFilteredSize = filteredProblems.size();
            allProblems.addAll(newProblems);
            
            // Apply current filters to new problems and add to filtered list
            List<Problem> newFilteredProblems = new ArrayList<>();
            for (Problem problem : newProblems) {
                String searchQuery = searchEditText.getText().toString();
                boolean matchesSearch = searchQuery.isEmpty() || 
                    problem.getTitle().toLowerCase().contains(searchQuery.toLowerCase()) ||
                    String.valueOf(problem.getId()).contains(searchQuery);
                
                boolean matchesTopics = selectedTopics.isEmpty() ||
                    problem.getTopics().stream().anyMatch(selectedTopics::contains);
                
                boolean matchesDifficulty = selectedDifficulties.isEmpty() ||
                    selectedDifficulties.contains(problem.getDifficulty());
                
                if (matchesSearch && matchesTopics && matchesDifficulty) {
                    newFilteredProblems.add(problem);
                }
            }
            
            // Sort new filtered problems according to current sort order
            if (!newFilteredProblems.isEmpty()) {
                sortProblems(newFilteredProblems);
                
                // Insert sorted problems in the correct position
                filteredProblems.addAll(newFilteredProblems);
                sortFilteredProblems(); // Re-sort the entire list to maintain order
            }
            
            problemsAdapter.setLoading(false);
            
            // Only notify about new items that match the filter
            if (newFilteredProblems.size() > 0) {
                problemsAdapter.notifyItemRangeInserted(oldFilteredSize, newFilteredProblems.size());
            }
            
            currentPage++;
            isLoading = false;
            
            // Update problem count
            updateProblemCount();
            
        android.util.Log.d("InfiniteScroll", "⚡ Fast loaded page " + (currentPage-1) + 
            ", total problems: " + allProblems.size() + 
            ", filtered: " + filteredProblems.size() + 
            ", new filtered: " + newFilteredProblems.size() +
            ", using real data: " + realProblemsLoaded);
        }, 150); // Reduced from 800ms to 150ms for faster loading
    }
    
    private List<Problem> generateOptimizedProblemsForPage(int page) {
        List<Problem> problems = new ArrayList<>();
        int startId = (page - 1) * PROBLEMS_PER_PAGE + 1;
        
        for (int i = 0; i < PROBLEMS_PER_PAGE && startId + i <= TOTAL_PROBLEMS; i++) {
            int problemId = startId + i;
            
            // Try to use real problem from static mappings first (problems 1-200)
            Problem realProblem = createProblemWithStaticMapping(problemId);
            if (realProblem != null) {
                problems.add(realProblem);
            } else {
                // Generate optimized problem
                problems.add(createOptimizedGeneratedProblem(problemId));
            }
        }
        
        return problems;
    }
    
    private Problem createProblemWithStaticMapping(int problemId) {
        // Use the real title mappings we have for problems 1-200
        String[] realTitles = {
            "Two Sum", "Add Two Numbers", "Longest Substring Without Repeating Characters", 
            "Median of Two Sorted Arrays", "Longest Palindromic Substring", "Zigzag Conversion",
            "Reverse Integer", "String to Integer (atoi)", "Palindrome Number", "Regular Expression Matching",
            "Container With Most Water", "Integer to Roman", "Roman to Integer", "Longest Common Prefix",
            "3Sum", "3Sum Closest", "Letter Combinations of a Phone Number", "4Sum",
            "Remove Nth Node From End of List", "Valid Parentheses", "Merge Two Sorted Lists",
            "Generate Parentheses", "Merge k Sorted Lists", "Swap Nodes in Pairs"
            // ... can extend this array
        };
        
        if (problemId <= 200) {
            String title = problemId <= realTitles.length ? 
                realTitles[problemId - 1] : 
                "LeetCode Problem " + problemId;
            
            String[] difficulties = {"Easy", "Medium", "Hard"};
            String difficulty = difficulties[problemId % 3];
            double acceptanceRate = 25.0 + (problemId % 55); // 25-80% range
            
            List<String> topics = getRealisticTopicsForProblem(problemId);
            String companies = "Google, Amazon, Microsoft";
            
            return new Problem(problemId, title, difficulty, acceptanceRate, companies, topics);
        }
        
        return null; // No static mapping available
    }
    
    private List<String> getRealisticTopicsForProblem(int problemId) {
        List<String> allTopics = Arrays.asList(
            "Array", "String", "Hash Table", "Dynamic Programming", "Math", "Sorting",
            "Greedy", "Tree", "Two Pointers", "Binary Search", "Stack", "Linked List",
            "Graph", "Backtracking", "Bit Manipulation", "Heap", "Trie"
        );
        
        List<String> topics = new ArrayList<>();
        // Problems 1-50: Focus on basics
        if (problemId <= 50) {
            String[] basicTopics = {"Array", "String", "Hash Table", "Two Pointers", "Math"};
            topics.add(basicTopics[problemId % basicTopics.length]);
        }
        // Problems 51-100: Add more complexity
        else if (problemId <= 100) {
            String[] intermediateTopics = {"Dynamic Programming", "Tree", "Graph", "Greedy", "Sorting"};
            topics.add(intermediateTopics[problemId % intermediateTopics.length]);
            if (problemId % 2 == 0) topics.add("Array"); // Add secondary topic
        }
        // Problems 101+: Advanced topics
        else {
            topics.add(allTopics.get(problemId % allTopics.size()));
            topics.add(allTopics.get((problemId + 3) % allTopics.size()));
        }
        
        return topics;
    }
    
    private Problem createOptimizedGeneratedProblem(int id) {
        String[] difficulties = {"Easy", "Medium", "Hard"};
        String difficulty = difficulties[id % 3];
        double acceptanceRate = 20.0 + (id % 60);
        
        // Create more engaging titles based on common algorithm patterns
        String title = generateEngagingTitle(id);
        String companies = "Tech Companies";
        List<String> topics = getRealisticTopicsForProblem(id);
        
        return new Problem(id, title, difficulty, acceptanceRate, companies, topics);
    }
    
    private String generateEngagingTitle(int id) {
        String[] patterns = {
            "Array Manipulation", "String Processing", "Tree Traversal", "Graph Algorithm",
            "Dynamic Programming", "Binary Search", "Two Pointers", "Sliding Window",
            "Backtracking", "Greedy Algorithm", "Heap Operations", "Hash Table Design"
        };
        
        String pattern = patterns[id % patterns.length];
        
        if (id <= 500) {
            return pattern + " " + (id % 100 + 1);
        } else if (id <= 1000) {
            return "Advanced " + pattern + " " + (id % 100 + 1);
        } else {
            return "Expert " + pattern + " " + (id % 100 + 1);
        }
    }
    
    private List<Problem> getRealProblemsForPage(int page) {
        List<Problem> pageProblems = new ArrayList<>();
        int startIndex = (page - 1) * PROBLEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + PROBLEMS_PER_PAGE, realLeetCodeProblems.size());
        
        if (startIndex < realLeetCodeProblems.size()) {
            // Use real problems
            for (int i = startIndex; i < endIndex; i++) {
                pageProblems.add(realLeetCodeProblems.get(i));
            }
            
            // If we don't have enough real problems, fill with generated ones
            if (pageProblems.size() < PROBLEMS_PER_PAGE) {
                int remainingCount = PROBLEMS_PER_PAGE - pageProblems.size();
                int nextId = realLeetCodeProblems.size() + 1;
                
                for (int i = 0; i < remainingCount; i++) {
                    Problem generatedProblem = createGeneratedProblem(nextId + i);
                    pageProblems.add(generatedProblem);
                }
            }
        } else {
            // All real problems exhausted, use generated ones
            pageProblems = generateProblemsForPage(page);
        }
        
        return pageProblems;
    }
    
    private Problem createGeneratedProblem(int id) {
        String[] difficulties = {"Easy", "Medium", "Hard"};
        String difficulty = difficulties[id % 3];
        double acceptanceRate = 20.0 + (id % 60);
        
        // Generate more interesting titles for higher numbered problems
        String title;
        if (id <= 500) {
            title = "Algorithm Challenge " + id;
        } else if (id <= 1000) {
            title = "Advanced Problem " + id;
        } else if (id <= 2000) {
            title = "Expert Challenge " + id;
        } else {
            title = "Master Problem " + id;
        }
        
        String companies = "Various Tech Companies";
        
        // Vary topics based on problem number
        List<String> allTopics = Arrays.asList(
            "Array", "String", "Hash Table", "Dynamic Programming", 
            "Math", "Sorting", "Greedy", "Tree", "Two Pointers", 
            "Binary Search", "Stack", "Linked List", "Graph",
            "Backtracking", "Bit Manipulation", "Heap", "Trie"
        );
        
        List<String> topics = new ArrayList<>();
        // Add 1-3 topics per problem
        int topicCount = 1 + (id % 3);
        for (int i = 0; i < topicCount; i++) {
            String topic = allTopics.get((id + i) % allTopics.size());
            if (!topics.contains(topic)) {
                topics.add(topic);
            }
        }
        
        return new Problem(id, title, difficulty, acceptanceRate, companies, topics);
    }
    
    private void updateProblemCount() {
        int loadedCount = allProblems.size();
        String countText;
        
        if (realProblemsLoaded && !realLeetCodeProblems.isEmpty()) {
            countText = loadedCount + " Problems • " + realLeetCodeProblems.size() + " from LeetCode";
        } else {
            countText = loadedCount + "+ Problems • Loading real data...";
        }
        
        if (loadedCount >= TOTAL_PROBLEMS) {
            countText = TOTAL_PROBLEMS + " Problems";
        }
        problemCountText.setText(countText);
    }
    
    private List<Problem> generateProblemsForPage(int page) {
        List<Problem> problems = new ArrayList<>();
        int startId = (page - 1) * PROBLEMS_PER_PAGE + 1;
        
        String[] difficulties = {"Easy", "Medium", "Hard"};
        String[] companies = {"Amazon, Google, Facebook", "Microsoft, Apple, Netflix", "Bloomberg, Uber, Airbnb", 
                             "Google, Microsoft, Apple", "Amazon, Facebook, Twitter", "Spotify, Dropbox, Slack"};
        String[] problemTitles = {"Two Sum", "Add Two Numbers", "Longest Substring Without Repeating Characters", 
                                "Median of Two Sorted Arrays", "Longest Palindromic Substring", "Reverse Integer", 
                                "String to Integer (atoi)", "Palindrome Number", "Container With Most Water", "3Sum", 
                                "Remove Duplicates from Sorted Array", "Valid Parentheses", "Merge Two Sorted Lists", 
                                "Remove Element", "Find the Index of the First Occurrence in a String", "Search Insert Position", 
                                "Count and Say", "Maximum Subarray", "Length of Last Word", "Plus One", "Add Binary", 
                                "Sqrt(x)", "Climbing Stairs", "Remove Duplicates from Sorted Array II"};
        // Topic weights based on actual LeetCode problem counts
        Map<String, Integer> topicWeights = new HashMap<>();
        topicWeights.put("Array", 1977);
        topicWeights.put("String", 809);
        topicWeights.put("Hash Table", 722);
        topicWeights.put("Dynamic Programming", 696);
        topicWeights.put("Math", 635);
        topicWeights.put("Sorting", 481);
        topicWeights.put("Greedy", 479);
        topicWeights.put("Tree", 454);
        topicWeights.put("Two Pointers", 433);
        topicWeights.put("Binary Search", 393);
        topicWeights.put("Stack", 341);
        topicWeights.put("Linked List", 268);
        topicWeights.put("Graph", 246);
        
        List<String> allTopics = Arrays.asList("Array", "String", "Hash Table", "Dynamic Programming", 
                                             "Math", "Sorting", "Greedy", "Tree", "Two Pointers", 
                                             "Binary Search", "Stack", "Linked List", "Graph");
        
        for (int i = 0; i < PROBLEMS_PER_PAGE && startId + i <= TOTAL_PROBLEMS; i++) {
            int problemId = startId + i;
            String difficulty = difficulties[problemId % 3];
            double acceptanceRate = 20.0 + (problemId % 60); // Random-ish acceptance rate
            String companyTag = companies[problemId % companies.length];
            
            // Generate problem title
            String title;
            if (problemId <= problemTitles.length) {
                // Use real LeetCode problem titles for the first 100 problems
                title = problemTitles[(problemId - 1) % problemTitles.length];
            } else {
                // Generate titles for problems beyond our mapped range
                title = "Algorithm Challenge " + problemId;
            }
            
            // Generate topics based on realistic distribution
            List<String> problemTopics = new ArrayList<>();
            
            // Each problem gets 1-3 topics with probability based on topic weights
            for (String topic : allTopics) {
                int weight = topicWeights.get(topic);
                double probability = Math.min(0.8, (double) weight / 1977); // Max 80% chance for any topic
                
                // Use problemId as seed for consistent topic assignment
                Random random = new Random(problemId * topic.hashCode());
                if (random.nextDouble() < probability) {
                    problemTopics.add(topic);
                }
            }
            
            // Ensure each problem has at least one topic
            if (problemTopics.isEmpty()) {
                problemTopics.add(allTopics.get(problemId % allTopics.size()));
            }
            
            problems.add(new Problem(problemId, title, difficulty, acceptanceRate, companyTag, problemTopics));
        }
        
        return problems;
    }
    
    // Problem class
    public static class Problem {
        private int id;
        private String title;
        private String titleSlug;
        private String difficulty;
        private double acceptanceRate;
        private String companies;
        private List<String> topics;
        
        public Problem(int id, String title, String difficulty, double acceptanceRate, String companies, List<String> topics) {
            this.id = id;
            this.title = title;
            this.titleSlug = generateSlugFromTitle(title);
            this.difficulty = difficulty;
            this.acceptanceRate = acceptanceRate;
            this.companies = companies;
            this.topics = topics;
        }
        
        public Problem(int id, String title, String titleSlug, String difficulty, double acceptanceRate, String companies, List<String> topics) {
            this.id = id;
            this.title = title;
            this.titleSlug = titleSlug;
            this.difficulty = difficulty;
            this.acceptanceRate = acceptanceRate;
            this.companies = companies;
            this.topics = topics;
        }
        
        private String generateSlugFromTitle(String title) {
            // Map common problems to their real LeetCode titleSlugs
            String realSlug = getRealTitleSlug(this.id, title);
            if (realSlug != null) {
                return realSlug;
            }
            
            // Fallback to generated slug for non-mapped problems
            return title.toLowerCase()
                    .replaceAll("[^a-z0-9\\s]", "")
                    .replaceAll("\\s+", "-");
        }
        
        private String getRealTitleSlug(int problemId, String title) {
            // Map some common problem IDs to real LeetCode titleSlugs
            switch (problemId) {
                case 1: return "two-sum";
                case 2: return "add-two-numbers";
                case 3: return "longest-substring-without-repeating-characters";
                case 4: return "median-of-two-sorted-arrays";
                case 5: return "longest-palindromic-substring";
                case 6: return "zigzag-conversion";
                case 7: return "reverse-integer";
                case 8: return "string-to-integer-atoi";
                case 9: return "palindrome-number";
                case 10: return "regular-expression-matching";
                case 11: return "container-with-most-water";
                case 12: return "integer-to-roman";
                case 13: return "roman-to-integer";
                case 14: return "longest-common-prefix";
                case 15: return "3sum";
                case 16: return "3sum-closest";
                case 17: return "letter-combinations-of-a-phone-number";
                case 18: return "4sum";
                case 19: return "remove-nth-node-from-end-of-list";
                case 20: return "valid-parentheses";
                case 21: return "merge-two-sorted-lists";
                case 22: return "generate-parentheses";
                case 23: return "merge-k-sorted-lists";
                case 24: return "swap-nodes-in-pairs";
                case 25: return "reverse-nodes-in-k-group";
                case 26: return "remove-duplicates-from-sorted-array";
                case 27: return "remove-element";
                case 28: return "find-the-index-of-the-first-occurrence-in-a-string";
                case 29: return "divide-two-integers";
                case 30: return "substring-with-concatenation-of-all-words";
                case 31: return "next-permutation";
                case 32: return "longest-valid-parentheses";
                case 33: return "search-in-rotated-sorted-array";
                case 34: return "find-first-and-last-position-of-element-in-sorted-array";
                case 35: return "search-insert-position";
                case 36: return "valid-sudoku";
                case 37: return "sudoku-solver";
                case 38: return "count-and-say";
                case 39: return "combination-sum";
                case 40: return "combination-sum-ii";
                case 41: return "first-missing-positive";
                case 42: return "trapping-rain-water";
                case 43: return "multiply-strings";
                case 44: return "wildcard-matching";
                case 45: return "jump-game-ii";
                case 46: return "permutations";
                case 47: return "permutations-ii";
                case 48: return "rotate-image";
                case 49: return "group-anagrams";
                case 50: return "powx-n";
                case 51: return "n-queens";
                case 52: return "n-queens-ii";
                case 53: return "maximum-subarray";
                case 54: return "spiral-matrix";
                case 55: return "jump-game";
                case 56: return "merge-intervals";
                case 57: return "insert-interval";
                case 58: return "length-of-last-word";
                case 59: return "spiral-matrix-ii";
                case 60: return "permutation-sequence";
                case 61: return "rotate-list";
                case 62: return "unique-paths";
                case 63: return "unique-paths-ii";
                case 64: return "minimum-path-sum";
                case 65: return "valid-number";
                case 66: return "plus-one";
                case 67: return "add-binary";
                case 68: return "text-justification";
                case 69: return "sqrtx";
                case 70: return "climbing-stairs";
                case 71: return "simplify-path";
                case 72: return "edit-distance";
                case 73: return "set-matrix-zeroes";
                case 74: return "search-a-2d-matrix";
                case 75: return "sort-colors";
                case 76: return "minimum-window-substring";
                case 77: return "combinations";
                case 78: return "subsets";
                case 79: return "word-search";
                case 80: return "remove-duplicates-from-sorted-array-ii";
                case 81: return "search-in-rotated-sorted-array-ii";
                case 82: return "remove-duplicates-from-sorted-list-ii";
                case 83: return "remove-duplicates-from-sorted-list";
                case 84: return "largest-rectangle-in-histogram";
                case 85: return "maximal-rectangle";
                case 86: return "partition-list";
                case 87: return "scramble-string";
                case 88: return "merge-sorted-array";
                case 89: return "gray-code";
                case 90: return "subsets-ii";
                case 91: return "decode-ways";
                case 92: return "reverse-linked-list-ii";
                case 93: return "restore-ip-addresses";
                case 94: return "binary-tree-inorder-traversal";
                case 95: return "unique-binary-search-trees-ii";
                case 96: return "unique-binary-search-trees";
                case 97: return "interleaving-string";
                case 98: return "validate-binary-search-tree";
                case 99: return "recover-binary-search-tree";
                case 100: return "same-tree";
                // Extended mappings for 101-200
                case 101: return "symmetric-tree";
                case 102: return "binary-tree-level-order-traversal";
                case 103: return "binary-tree-zigzag-level-order-traversal";
                case 104: return "maximum-depth-of-binary-tree";
                case 105: return "construct-binary-tree-from-preorder-and-inorder-traversal";
                case 106: return "construct-binary-tree-from-inorder-and-postorder-traversal";
                case 107: return "binary-tree-level-order-traversal-ii";
                case 108: return "convert-sorted-array-to-binary-search-tree";
                case 109: return "convert-sorted-list-to-binary-search-tree";
                case 110: return "balanced-binary-tree";
                case 111: return "minimum-depth-of-binary-tree";
                case 112: return "path-sum";
                case 113: return "path-sum-ii";
                case 114: return "flatten-binary-tree-to-linked-list";
                case 115: return "distinct-subsequences";
                case 116: return "populating-next-right-pointers-in-each-node";
                case 117: return "populating-next-right-pointers-in-each-node-ii";
                case 118: return "pascals-triangle";
                case 119: return "pascals-triangle-ii";
                case 120: return "triangle";
                case 121: return "best-time-to-buy-and-sell-stock";
                case 122: return "best-time-to-buy-and-sell-stock-ii";
                case 123: return "best-time-to-buy-and-sell-stock-iii";
                case 124: return "binary-tree-maximum-path-sum";
                case 125: return "valid-palindrome";
                case 126: return "word-ladder-ii";
                case 127: return "word-ladder";
                case 128: return "longest-consecutive-sequence";
                case 129: return "sum-root-to-leaf-numbers";
                case 130: return "surrounded-regions";
                case 131: return "palindrome-partitioning";
                case 132: return "palindrome-partitioning-ii";
                case 133: return "clone-graph";
                case 134: return "gas-station";
                case 135: return "candy";
                case 136: return "single-number";
                case 137: return "single-number-ii";
                case 138: return "copy-list-with-random-pointer";
                case 139: return "word-break";
                case 140: return "word-break-ii";
                case 141: return "linked-list-cycle";
                case 142: return "linked-list-cycle-ii";
                case 143: return "reorder-list";
                case 144: return "binary-tree-preorder-traversal";
                case 145: return "binary-tree-postorder-traversal";
                case 146: return "lru-cache";
                case 147: return "insertion-sort-list";
                case 148: return "sort-list";
                case 149: return "max-points-on-a-line";
                case 150: return "evaluate-reverse-polish-notation";
                case 151: return "reverse-words-in-a-string";
                case 152: return "maximum-product-subarray";
                case 153: return "find-minimum-in-rotated-sorted-array";
                case 154: return "find-minimum-in-rotated-sorted-array-ii";
                case 155: return "min-stack";
                case 156: return "binary-tree-upside-down";
                case 157: return "read-n-characters-given-read4";
                case 158: return "read-n-characters-given-read4-ii-call-multiple-times";
                case 159: return "longest-substring-with-at-most-two-distinct-characters";
                case 160: return "intersection-of-two-linked-lists";
                case 161: return "one-edit-distance";
                case 162: return "find-peak-element";
                case 163: return "missing-ranges";
                case 164: return "maximum-gap";
                case 165: return "compare-version-numbers";
                case 166: return "fraction-to-recurring-decimal";
                case 167: return "two-sum-ii-input-array-is-sorted";
                case 168: return "excel-sheet-column-title";
                case 169: return "majority-element";
                case 170: return "two-sum-iii-data-structure-design";
                case 171: return "excel-sheet-column-number";
                case 172: return "factorial-trailing-zeroes";
                case 173: return "binary-search-tree-iterator";
                case 174: return "dungeon-game";
                case 175: return "combine-two-tables";
                case 176: return "second-highest-salary";
                case 177: return "nth-highest-salary";
                case 178: return "rank-scores";
                case 179: return "largest-number";
                case 180: return "consecutive-numbers";
                case 181: return "employees-earning-more-than-their-managers";
                case 182: return "duplicate-emails";
                case 183: return "customers-who-never-order";
                case 184: return "department-highest-salary";
                case 185: return "department-top-three-salaries";
                case 186: return "reverse-words-in-a-string-ii";
                case 187: return "repeated-dna-sequences";
                case 188: return "best-time-to-buy-and-sell-stock-iv";
                case 189: return "rotate-array";
                case 190: return "reverse-bits";
                case 191: return "number-of-1-bits";
                case 192: return "word-frequency";
                case 193: return "valid-phone-numbers";
                case 194: return "transpose-file";
                case 195: return "tenth-line";
                case 196: return "delete-duplicate-emails";
                case 197: return "rising-temperature";
                case 198: return "house-robber";
                case 199: return "binary-tree-right-side-view";
                case 200: return "number-of-islands";
                default: return null; // Return null for unmapped problems, will use generated slug
            }
        }
        
        // Getters
        public int getId() { return id; }
        public String getTitle() { return title; }
        public String getTitleSlug() { return titleSlug; }
        public String getDifficulty() { return difficulty; }
        public double getAcceptanceRate() { return acceptanceRate; }
        public String getCompanies() { return companies; }
        public List<String> getTopics() { return topics; }
    }
    
    // Topic class
    public static class Topic {
        private String name;
        private int count;
        private boolean isSelected;
        
        public Topic(String name, int count, boolean isSelected) {
            this.name = name;
            this.count = count;
            this.isSelected = isSelected;
        }
        
        // Getters and setters
        public String getName() { return name; }
        public int getCount() { return count; }
        public boolean isSelected() { return isSelected; }
        public void setSelected(boolean selected) { isSelected = selected; }
    }
    
    // Problems Adapter with loading support
    private static class ProblemsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int VIEW_TYPE_PROBLEM = 0;
        private static final int VIEW_TYPE_LOADING = 1;
        
        private List<Problem> problems;
        private boolean isLoading = false;
        
        public ProblemsAdapter(List<Problem> problems) {
            this.problems = problems;
        }
        
        @Override
        public int getItemViewType(int position) {
            return (position == problems.size() && isLoading) ? VIEW_TYPE_LOADING : VIEW_TYPE_PROBLEM;
        }
        
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_LOADING) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_loading, parent, false);
                return new LoadingViewHolder(view);
            } else {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_problem, parent, false);
                return new ProblemViewHolder(view);
            }
        }
        
        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof ProblemViewHolder) {
                Problem problem = problems.get(position);
                ((ProblemViewHolder) holder).bind(problem);
            }
            // LoadingViewHolder doesn't need binding
        }
        
        @Override
        public int getItemCount() {
            return problems.size() + (isLoading ? 1 : 0);
        }
        
        public void setLoading(boolean loading) {
            boolean wasLoading = isLoading;
            isLoading = loading;
            
            if (wasLoading && !loading) {
                notifyItemRemoved(problems.size());
            } else if (!wasLoading && loading) {
                notifyItemInserted(problems.size());
            }
        }
        
        static class LoadingViewHolder extends RecyclerView.ViewHolder {
            public LoadingViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
        
        static class ProblemViewHolder extends RecyclerView.ViewHolder {
            private TextView problemNumber;
            private TextView problemTitle;
            private TextView difficultyBadge;
            private TextView acceptanceRate;
            private TextView companyTags;
            private RecyclerView topicTagsRecyclerView;
            
            public ProblemViewHolder(@NonNull View itemView) {
                super(itemView);
                problemNumber = itemView.findViewById(R.id.problemNumber);
                problemTitle = itemView.findViewById(R.id.problemTitle);
                difficultyBadge = itemView.findViewById(R.id.difficultyBadge);
                acceptanceRate = itemView.findViewById(R.id.acceptanceRate);
                companyTags = itemView.findViewById(R.id.companyTags);
                topicTagsRecyclerView = itemView.findViewById(R.id.topicTagsRecyclerView);
            }
            
            public void bind(Problem problem) {
                problemNumber.setText(problem.getId() + ".");
                problemTitle.setText(problem.getTitle());
                acceptanceRate.setText(String.format("%.1f%%", problem.getAcceptanceRate()));
                companyTags.setText(problem.getCompanies());
                
                // Set difficulty badge
                difficultyBadge.setText(problem.getDifficulty());
                Context context = itemView.getContext();
                switch (problem.getDifficulty().toLowerCase()) {
                    case "easy":
                        difficultyBadge.setBackground(ContextCompat.getDrawable(context, R.drawable.difficulty_badge_easy));
                        break;
                    case "medium":
                        difficultyBadge.setBackground(ContextCompat.getDrawable(context, R.drawable.difficulty_badge_medium));
                        break;
                    case "hard":
                        difficultyBadge.setBackground(ContextCompat.getDrawable(context, R.drawable.difficulty_badge_hard));
                        break;
                }
                
                // Setup topic tags
                LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
                topicTagsRecyclerView.setLayoutManager(layoutManager);
                TopicTagsAdapter topicTagsAdapter = new TopicTagsAdapter(problem.getTopics());
                topicTagsRecyclerView.setAdapter(topicTagsAdapter);
                
                // Add click handler for problem detail
                itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(context, ProblemDetailActivity.class);
                    intent.putExtra("problem_id", problem.getId());
                    intent.putExtra("problem_title", problem.getTitle());
                    intent.putExtra("problem_title_slug", problem.getTitleSlug());
                    intent.putExtra("problem_difficulty", problem.getDifficulty());
                    intent.putExtra("problem_acceptance", problem.getAcceptanceRate());
                    intent.putExtra("problem_companies", problem.getCompanies());
                    context.startActivity(intent);
                });
            }
        }
    }
    
    // Topic Tags Adapter (for individual problem topic tags)
    private static class TopicTagsAdapter extends RecyclerView.Adapter<TopicTagsAdapter.TagViewHolder> {
        private List<String> tags;
        
        public TopicTagsAdapter(List<String> tags) {
            this.tags = tags;
        }
        
        @NonNull
        @Override
        public TagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_topic_tag, parent, false);
            return new TagViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull TagViewHolder holder, int position) {
            String tag = tags.get(position);
            holder.bind(tag);
        }
        
        @Override
        public int getItemCount() {
            return tags.size();
        }
        
        static class TagViewHolder extends RecyclerView.ViewHolder {
            private Chip tagChip;
            
            public TagViewHolder(@NonNull View itemView) {
                super(itemView);
                tagChip = (Chip) itemView;
            }
            
            public void bind(String tag) {
                tagChip.setText(tag);
            }
        }
    }
    
    // Topic Dialog Adapter for the selection popup
    private static class TopicDialogAdapter extends RecyclerView.Adapter<TopicDialogAdapter.DialogTopicViewHolder> {
        private List<Topic> allTopics;
        private List<Topic> filteredTopics;
        private TextView selectedCountText;
        
        public TopicDialogAdapter(List<Topic> topics, TextView selectedCountText) {
            this.allTopics = topics;
            this.filteredTopics = new ArrayList<>(topics);
            this.selectedCountText = selectedCountText;
            updateSelectedCount();
        }
        
        @NonNull
        @Override
        public DialogTopicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_topic_dialog, parent, false);
            return new DialogTopicViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull DialogTopicViewHolder holder, int position) {
            Topic topic = filteredTopics.get(position);
            holder.bind(topic, this::onTopicToggled);
        }
        
        @Override
        public int getItemCount() {
            return filteredTopics.size();
        }
        
        public void filter(String query) {
            filteredTopics.clear();
            if (query.isEmpty()) {
                filteredTopics.addAll(allTopics);
            } else {
                for (Topic topic : allTopics) {
                    if (topic.getName().toLowerCase().contains(query.toLowerCase())) {
                        filteredTopics.add(topic);
                    }
                }
            }
            notifyDataSetChanged();
        }
        
        public void clearAll() {
            for (Topic topic : allTopics) {
                topic.setSelected(false);
            }
            notifyDataSetChanged();
            updateSelectedCount();
        }
        
        public Set<String> getSelectedTopics() {
            Set<String> selected = new HashSet<>();
            for (Topic topic : allTopics) {
                if (topic.isSelected()) {
                    selected.add(topic.getName());
                }
            }
            return selected;
        }
        
        private void onTopicToggled(Topic topic, boolean isSelected) {
            topic.setSelected(isSelected);
            updateSelectedCount();
        }
        
        private void updateSelectedCount() {
            int count = (int) allTopics.stream().mapToInt(t -> t.isSelected() ? 1 : 0).sum();
            selectedCountText.setText(count + " topics selected");
        }
        
        static class DialogTopicViewHolder extends RecyclerView.ViewHolder {
            private CheckBox topicCheckBox;
            private TextView topicNameText;
            private TextView topicCountText;
            
            public DialogTopicViewHolder(@NonNull View itemView) {
                super(itemView);
                topicCheckBox = itemView.findViewById(R.id.topicCheckBox);
                topicNameText = itemView.findViewById(R.id.topicNameText);
                topicCountText = itemView.findViewById(R.id.topicCountText);
            }
            
            public void bind(Topic topic, OnTopicToggleListener listener) {
                topicNameText.setText(topic.getName());
                topicCountText.setText(topic.getCount() + " problems");
                topicCheckBox.setChecked(topic.isSelected());
                
                itemView.setOnClickListener(v -> {
                    boolean newState = !topic.isSelected();
                    topicCheckBox.setChecked(newState);
                    listener.onTopicToggled(topic, newState);
                });
                
                topicCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    listener.onTopicToggled(topic, isChecked);
                });
            }
        }
        
        interface OnTopicToggleListener {
            void onTopicToggled(Topic topic, boolean isSelected);
        }
    }
    
    private void setupBottomNavigation() {
        // Get navigation elements
        LinearLayout navHome = findViewById(R.id.nav_home);
        LinearLayout navProgress = findViewById(R.id.nav_progress);
        LinearLayout navCards = findViewById(R.id.nav_cards);
        LinearLayout navRevision = findViewById(R.id.nav_revision);
        
        // Get indicators and set current active (Problems)
        View homeIndicator = findViewById(R.id.home_indicator);
        View progressIndicator = findViewById(R.id.progress_indicator);
        View cardsIndicator = findViewById(R.id.cards_indicator);
        View revisionIndicator = findViewById(R.id.revision_indicator);
        
        // Set Problems as active
        progressIndicator.setVisibility(View.VISIBLE);
        TextView progressText = findViewById(R.id.progress_text);
        progressText.setTextColor(getResources().getColor(R.color.accent_secondary, getTheme()));
        progressText.setTypeface(null, android.graphics.Typeface.BOLD);
        
        // Set click listeners
        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(ProblemsActivity.this, ModernMainActivity.class);
            startActivity(intent);
            finish();
        });
        
        navProgress.setOnClickListener(v -> {
            // Already on Problems page - do nothing or scroll to top
            problemsRecyclerView.smoothScrollToPosition(0);
        });
        
        navCards.setOnClickListener(v -> {
            Intent intent = new Intent(ProblemsActivity.this, CompanyProblemsActivity.class);
            startActivity(intent);
        });
        
        navRevision.setOnClickListener(v -> {
            // TODO: Navigate to Revision activity when created
        });
    }
    
    private void showSkeletonLoading(boolean show) {
        if (show) {
            // Show skeleton loading
            if (skeletonView == null && skeletonStub != null) {
                skeletonView = skeletonStub.inflate();
                
                // Start shimmer animation for all skeleton views
                startSkeletonAnimation(skeletonView);
            }
            
            if (contentContainer != null) {
                contentContainer.setVisibility(View.GONE);
            }
            if (skeletonView != null) {
                skeletonView.setVisibility(View.VISIBLE);
            }
        } else {
            // Hide skeleton and show content
            if (contentContainer != null) {
                contentContainer.setVisibility(View.VISIBLE);
            }
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
}
