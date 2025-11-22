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
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.CheckBox;
import android.widget.RadioButton;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;
import okhttp3.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class CompanyProblemsActivity extends BaseActivity {
    
    private RecyclerView problemsRecyclerView;
    private EditText searchEditText;
    private TextView problemCountText;
    private TextView sortButton;
    private TabLayout companyTabLayout;
    private ImageButton filterButton;
    private TextView selectedCompanyText;
    private TextView selectedCompanyCount;
    private TextView companyProblemsTitle;
    private com.google.android.material.card.MaterialCardView bottomNavCard;
    
    // Skeleton loading
    private ViewStub skeletonStub;
    private View skeletonView;
    private LinearLayout contentContainer;
    
    private CompanyProblemsAdapter problemsAdapter;
    
    private List<CompanyProblem> allProblems;
    private List<CompanyProblem> filteredProblems;
    private String currentCompany = "All";
    private String sortOrder = "Default"; // Default, Easy->Hard, Hard->Easy, A-Z, Z-A
    private Set<String> selectedDifficulties = new HashSet<>();
    
    // Company tabs data - Top companies first, then load more dynamically
    private String[] topCompanies = {
        "All", "Google", "Meta", "Amazon", "Microsoft", "Apple", "Netflix", "Uber", "Adobe", "Tesla"
    };
    
    // Extended companies list for "Show More" functionality
    private String[] allCompanies = {
        "All", "Meta", "Google", "Uber", "Amazon", "Microsoft", "TikTok", "Apple", "Oracle", "LinkedIn", 
        "Bloomberg", "Adobe", "Citadel", "IBM", "Salesforce", "Goldman Sachs", "Cisco", "Visa", 
        "Walmart Labs", "Databricks", "J.P. Morgan", "Roblox", "Nvidia", "Airbnb", "DoorDash", 
        "Atlassian", "Pinterest", "Capital One", "Snap", "Snowflake", "PayPal", "Palantir Technologies", 
        "Netflix", "Intuit", "TCS", "ByteDance", "DE Shaw", "eBay", "Flipkart", "Yandex", "Expedia", 
        "Coupang", "Zoho", "Infosys", "ServiceNow", "Jane Street", "Tesla", "Yahoo", "Morgan Stanley", 
        "Accenture", "OpenAI", "X", "SAP", "Lyft", "Rippling", "Anduril", "Cognizant", 
        "Hudson River Trading", "Samsung", "Two Sigma", "Qualcomm", "MathWorks", "Palo Alto Networks", 
        "Akuna Capital", "PhonePe", "Coinbase", "BlackRock", "Datadog", "Agoda", "Deloitte", "Rubrik", 
        "Nutanix", "Affirm", "Wells Fargo", "Stripe", "MongoDB", "Waymo", "Qualtrics", "Capgemini", 
        "Dropbox", "American Express", "Optiver", "Robinhood", "Arista Networks", "Block", "SoFi", 
        "Karat", "Booking.com", "Barclays", "ZScaler", "Sprinklr", "Reddit", "VMware", "Twilio", 
        "Spotify", "Squarepoint Capital", "Intel", "Millennium", "Yelp", "Shopify", "Huawei", "Paytm", 
        "IMC", "Alibaba", "Hubspot", "Deutsche Bank", "Meesho", "Docusign", "Zillow", "Wayfair", 
        "Jump Trading", "Instacart", "Grammarly", "SIG", "Axon", "Grab", "EPAM Systems", "Zeta", 
        "Media.net", "Arcesium", "Swiggy", "Tencent", "Upstart", "Rivian", "Roku", "Zomato", 
        "Mastercard", "Epic Systems", "Nuro", "AMD", "Cohesity", "Verkada", "Quora", "Workday", 
        "Autodesk", "Asana", "DRW", "Dell", "Cloudflare", "Juspay", "Okta", "Shopee", "MakeMyTrip", 
        "Myntra", "Disney", "BNY Mellon", "Box", "Confluent", "Tinkoff", "Citigroup", "Fidelity", 
        "CrowdStrike", "GoDaddy", "Siemens", "Splunk", "Wipro", "Pure Storage", "Virtusa", "Coursera", 
        "Twitch", "Chime", "Turing", "Audible", "Tekion", "Josh Technology", "Zoox", "UBS", "HSBC", 
        "Akamai", "IXL", "General Motors", "UiPath", "Tripadvisor", "Virtu Financial", "The Trade Desk", 
        "ThoughtSpot", "Commvault", "Rakuten", "Razorpay", "SOTI", "Tinder", "Nagarro", "Verily", 
        "Bank of America", "Remitly", "Flexport", "Indeed", "Zoom", "HPE", "Lucid", "Point72", 
        "Veeva Systems", "Riot Games", "HCL", "NetApp", "Avito", "Tower Research Capital", "Chewy", 
        "LiveRamp", "Optum", "Electronic Arts", "BitGo", "OpenText", "C3.ai", "Aurora", "ZS Associates", 
        "Duolingo", "Texas Instruments", "Zalando", "FreshWorks", "Ripple", "Trilogy", "Nordstrom", 
        "WarnerMedia", "Netskope", "Samsara", "McKinsey", "Nextdoor", "Bolt", "PwC", "PayPay", 
        "GE Healthcare", "RBC", "KLA", "Sigmoid", "Info Edge", "Athenahealth", "Gusto", "Amdocs", 
        "Comcast", "Squarespace", "Ozon", "CRED", "Grubhub", "Cadence", "Sony", "Toast", "Faire", 
        "Motive", "Nokia"
    };
    
    // Company filter constants
    private boolean showingAllCompanies = false;
    
    // Company problem mappings
    private Map<String, List<CompanyProblem>> companyProblems;
    
    // Dynamic loading
    private OkHttpClient httpClient;
    private boolean isLoading = false;
    private int currentPage = 1;
    private final int PROBLEMS_PER_PAGE = 20;
    private LinearLayoutManager layoutManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_company_problems);
        
        // Set status bar color to match activity background
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.background_primary, getTheme()));
        }
        
        // Apply theme-based styling
        applyTheme();
        
        initViews();
        setupRecyclerView();
        loadData();
        setupCompanyFilter();
        setupSearch();
        setupBottomNavigation();
    }
    
    private void initViews() {
        problemsRecyclerView = findViewById(R.id.problemsRecyclerView);
        searchEditText = findViewById(R.id.searchEditText);
        problemCountText = findViewById(R.id.problemCountText);
        sortButton = findViewById(R.id.sortButton);
        filterButton = findViewById(R.id.filterButton);
        selectedCompanyText = findViewById(R.id.selectedCompanyText);
        selectedCompanyCount = findViewById(R.id.selectedCompanyCount);
        companyProblemsTitle = findViewById(R.id.companyProblemsTitle);
        bottomNavCard = findViewById(R.id.bottomNavCard);
        
        // Initialize skeleton loading
        skeletonStub = findViewById(R.id.skeletonStub);
        contentContainer = findViewById(R.id.contentContainer);
        
        // Ensure bottom navigation stays fixed at bottom and is visible
        if (bottomNavCard != null) {
            bottomNavCard.setVisibility(View.VISIBLE);
            bottomNavCard.bringToFront();
            bottomNavCard.setTranslationZ(100f); // Ensure it's above other views
            
            // Post a runnable to ensure layout is complete before positioning
            bottomNavCard.post(() -> {
                bottomNavCard.bringToFront();
                bottomNavCard.setTranslationZ(100f);
            });
        }
        
        // Setup sort button
        sortButton.setOnClickListener(v -> {
            android.util.Log.d("CompanyProblems", "Sort button clicked");
            showSortDialog();
        });
        
        // Setup filter button
        filterButton.setOnClickListener(v -> showCompanyFilterDialog());
        
        // Setup back button
        ImageView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
    }
    
    private void setupRecyclerView() {
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
                    int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
                    
                    // Check if we should load more items
                    if (!isLoading && totalItemCount > 0 && lastVisibleItemPosition >= totalItemCount - 5) {
                        loadMoreProblems();
                    }
                }
            }
        });
    }
    
    private void updateCompanyFilterCounts() {
        // Recalculate company filter button text based on current data
        setupCompanyFilter();
    }
    
    private void deduplicateProblems() {
        // Create a map to group problems by a unique key (ID + Title)
        Map<String, CompanyProblem> uniqueProblems = new HashMap<>();
        
        // Go through all company problems and merge duplicates
        for (Map.Entry<String, List<CompanyProblem>> entry : companyProblems.entrySet()) {
            String companyName = entry.getKey();
            List<CompanyProblem> problems = entry.getValue();
            
            for (CompanyProblem problem : problems) {
                // Create a unique key using both ID and title to ensure proper deduplication
                String uniqueKey = problem.getId() + "_" + problem.getTitle().toLowerCase().replaceAll("\\s+", "_");
                
                if (uniqueProblems.containsKey(uniqueKey)) {
                    // Problem already exists, add this company to it
                    uniqueProblems.get(uniqueKey).addCompany(companyName);
                } else {
                    // New problem, create a fresh copy and add it to the map
                    CompanyProblem newProblem = new CompanyProblem(
                        problem.getId(), 
                        problem.getTitle(), 
                        problem.getDifficulty(), 
                        problem.getAcceptanceRate(), 
                        companyName, 
                        problem.getTopics()
                    );
                    uniqueProblems.put(uniqueKey, newProblem);
                }
            }
        }
        
        // Update all problems list with deduplicated problems
        allProblems.clear();
        allProblems.addAll(uniqueProblems.values());
        
        // Debug: Log how many unique problems we have
        android.util.Log.d("CompanyProblems", "Deduplication complete. Unique problems: " + allProblems.size());
        
        // Count Two Sum instances
        int twoSumCount = 0;
        for (CompanyProblem p : allProblems) {
            if (p.getId() == 1) {
                twoSumCount++;
                android.util.Log.d("CompanyProblems", "Two Sum found with companies: " + p.getCompanies());
            }
        }
        android.util.Log.d("CompanyProblems", "Two Sum instances after deduplication: " + twoSumCount);
        
        // Sort by problem ID for consistent ordering
        allProblems.sort((p1, p2) -> Integer.compare(p1.getId(), p2.getId()));
        
        // Update filtered problems and notify adapter
        filteredProblems.clear();
        filteredProblems.addAll(allProblems);
        problemsAdapter.notifyDataSetChanged();
    }

    private void setupCompanyFilter() {
        // Initialize with all companies
        currentCompany = "All";
        selectedCompanyText.setText("All Companies");
        
        // Calculate total problems across all companies
        int totalProblems = 0;
        if (companyProblems != null) {
            for (List<CompanyProblem> problemList : companyProblems.values()) {
                totalProblems += problemList.size();
            }
        }
        
        selectedCompanyCount.setText(totalProblems + " problems");
        updateCompanyTitle();
        
        // Load and filter problems immediately (only if data is ready)
        if (filteredProblems != null && companyProblems != null && !companyProblems.isEmpty()) {
            filterProblems("");
        }
    }
    
    private void showCompanyFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Filter by Company");
        
        // Create list of companies that have problems available
        List<String> availableCompanies = new ArrayList<>();
        List<String> displayNames = new ArrayList<>();
        
        // Add "All Companies" option first
        availableCompanies.add("All Companies");
        int totalProblemCount = 0;
        for (List<CompanyProblem> problemList : companyProblems.values()) {
            totalProblemCount += problemList.size();
        }
        final int finalTotalProblemCount = totalProblemCount; // Make it final for lambda
        displayNames.add("All Companies (" + totalProblemCount + " problems)");
        
        // Add companies that have problems with their counts
        for (String company : allCompanies) {
            if (!company.equals("All")) {
                List<CompanyProblem> companyProblemList = companyProblems.get(company);
                if (companyProblemList != null && !companyProblemList.isEmpty()) {
                    availableCompanies.add(company);
                    displayNames.add(company + " (" + companyProblemList.size() + " problems)");
                }
            }
        }
        
        String[] displayArray = displayNames.toArray(new String[0]);
        
        // Find current selection
        int currentSelection = 0;
        String currentCompanyDisplay = currentCompany.equals("All") ? "All Companies" : currentCompany;
        for (int i = 0; i < availableCompanies.size(); i++) {
            if (availableCompanies.get(i).equals(currentCompanyDisplay)) {
                currentSelection = i;
                break;
            }
        }
        
        builder.setSingleChoiceItems(displayArray, currentSelection, (dialog, which) -> {
            String selectedCompany = availableCompanies.get(which);
            
            if (selectedCompany.equals("All Companies")) {
                currentCompany = "All";
                selectedCompanyText.setText("All Companies");
                selectedCompanyCount.setText(finalTotalProblemCount + " problems");
            } else {
                currentCompany = selectedCompany;
                selectedCompanyText.setText(selectedCompany);
                
                // Update problem count for selected company
                List<CompanyProblem> companyProblemList = this.companyProblems.get(selectedCompany);
                int problemCount = companyProblemList != null ? companyProblemList.size() : 0;
                selectedCompanyCount.setText(problemCount + " problems");
            }
            
            updateCompanyTitle();
            filterProblems(searchEditText.getText().toString());
            dialog.dismiss();
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updateCompanyTitle() {
        if (currentCompany.equals("All")) {
            companyProblemsTitle.setText("All Company Problems");
        } else {
            companyProblemsTitle.setText(currentCompany + " Problems");
        }
    }    private void loadData() {
        // Show skeleton loading immediately
        showSkeletonLoading(true);
        
        // Initialize HTTP client
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
        
        // Initialize data structures
        allProblems = new ArrayList<>();
        filteredProblems = new ArrayList<>();
        companyProblems = new HashMap<>();
        
        // Setup adapter
        problemsAdapter = new CompanyProblemsAdapter(filteredProblems);
        problemsRecyclerView.setAdapter(problemsAdapter);
        
        // Initialize company problem mappings
        initializeCompanyProblems();
        updateCompanyFilterCounts();
        
        // Deduplicate problems after loading all company data
        deduplicateProblems();
        
        // Now that company problems are loaded and deduplicated, trigger initial filtering
        filterProblems("");
        
        // Load first batch of problems
        loadMoreProblems();
        
        // Hide skeleton loading
        showSkeletonLoading(false);
    }
    
    private void initializeCompanyProblems() {
        // Initialize with comprehensive data for major companies
        
        // Meta problems - Expanded list with 100+ problems
        List<CompanyProblem> metaProblems = Arrays.asList(
            // Easy Problems
            new CompanyProblem(1, "Two Sum", "Easy", 49.1, "Meta", Arrays.asList("Array", "Hash Table")),
            new CompanyProblem(67, "Add Binary", "Easy", 51.0, "Meta", Arrays.asList("Math", "String", "Bit Manipulation", "Simulation")),
            new CompanyProblem(125, "Valid Palindrome", "Easy", 44.1, "Meta", Arrays.asList("Two Pointers", "String")),
            new CompanyProblem(157, "Read N Characters Given Read4", "Easy", 37.2, "Meta", Arrays.asList("String", "Interactive", "Simulation")),
            new CompanyProblem(161, "One Edit Distance", "Medium", 32.1, "Meta", Arrays.asList("Two Pointers", "String")),
            new CompanyProblem(206, "Reverse Linked List", "Easy", 73.4, "Meta", Arrays.asList("Linked List", "Recursion")),
            new CompanyProblem(234, "Palindrome Linked List", "Easy", 50.1, "Meta", Arrays.asList("Linked List", "Two Pointers", "Stack")),
            new CompanyProblem(252, "Meeting Rooms", "Easy", 57.2, "Meta", Arrays.asList("Array", "Sorting")),
            new CompanyProblem(268, "Missing Number", "Easy", 64.0, "Meta", Arrays.asList("Array", "Hash Table", "Math")),
            new CompanyProblem(283, "Move Zeroes", "Easy", 60.4, "Meta", Arrays.asList("Array", "Two Pointers")),
            new CompanyProblem(303, "Range Sum Query - Immutable", "Easy", 56.8, "Meta", Arrays.asList("Array", "Design", "Prefix Sum")),
            new CompanyProblem(339, "Nested List Weight Sum", "Medium", 83.2, "Meta", Arrays.asList("Hash Table", "Depth-First Search", "Breadth-First Search")),
            new CompanyProblem(346, "Moving Average from Data Stream", "Easy", 74.9, "Meta", Arrays.asList("Array", "Design", "Sliding Window")),
            new CompanyProblem(408, "Valid Word Abbreviation", "Easy", 35.2, "Meta", Arrays.asList("Two Pointers", "String")),
            new CompanyProblem(680, "Valid Palindrome II", "Easy", 39.4, "Meta", Arrays.asList("Two Pointers", "String")),
            
            // Medium Problems  
            new CompanyProblem(2, "Add Two Numbers", "Medium", 38.9, "Meta", Arrays.asList("Linked List", "Math", "Recursion")),
            new CompanyProblem(3, "Longest Substring Without Repeating Characters", "Medium", 33.8, "Meta", Arrays.asList("Hash Table", "String", "Sliding Window")),
            new CompanyProblem(8, "String to Integer (atoi)", "Medium", 16.6, "Meta", Arrays.asList("String")),
            new CompanyProblem(15, "3Sum", "Medium", 32.1, "Meta", Arrays.asList("Array", "Two Pointers", "Sorting")),
            new CompanyProblem(17, "Letter Combinations of a Phone Number", "Medium", 58.1, "Meta", Arrays.asList("Hash Table", "String", "Backtracking")),
            new CompanyProblem(31, "Next Permutation", "Medium", 37.9, "Meta", Arrays.asList("Array", "Two Pointers")),
            new CompanyProblem(33, "Search in Rotated Sorted Array", "Medium", 38.9, "Meta", Arrays.asList("Array", "Binary Search")),
            new CompanyProblem(43, "Multiply Strings", "Medium", 38.5, "Meta", Arrays.asList("Math", "String", "Simulation")),
            new CompanyProblem(49, "Group Anagrams", "Medium", 67.6, "Meta", Arrays.asList("Array", "Hash Table", "String", "Sorting")),
            new CompanyProblem(50, "Pow(x, n)", "Medium", 33.2, "Meta", Arrays.asList("Math", "Recursion")),
            new CompanyProblem(56, "Merge Intervals", "Medium", 46.5, "Meta", Arrays.asList("Array", "Sorting")),
            new CompanyProblem(71, "Simplify Path", "Medium", 39.9, "Meta", Arrays.asList("String", "Stack")),
            new CompanyProblem(75, "Sort Colors", "Medium", 59.7, "Meta", Arrays.asList("Array", "Two Pointers", "Sorting")),
            new CompanyProblem(76, "Minimum Window Substring", "Hard", 40.4, "Meta", Arrays.asList("Hash Table", "String", "Sliding Window")),
            new CompanyProblem(78, "Subsets", "Medium", 75.0, "Meta", Arrays.asList("Array", "Backtracking", "Bit Manipulation")),
            new CompanyProblem(79, "Word Search", "Medium", 40.1, "Meta", Arrays.asList("Array", "Backtracking", "Matrix")),
            new CompanyProblem(80, "Remove Duplicates from Sorted Array II", "Medium", 52.2, "Meta", Arrays.asList("Array", "Two Pointers")),
            new CompanyProblem(88, "Merge Sorted Array", "Easy", 46.5, "Meta", Arrays.asList("Array", "Two Pointers", "Sorting")),
            new CompanyProblem(91, "Decode Ways", "Medium", 32.0, "Meta", Arrays.asList("String", "Dynamic Programming")),
            new CompanyProblem(102, "Binary Tree Level Order Traversal", "Medium", 64.4, "Meta", Arrays.asList("Tree", "Breadth-First Search")),
            new CompanyProblem(103, "Binary Tree Zigzag Level Order Traversal", "Medium", 56.9, "Meta", Arrays.asList("Tree", "Breadth-First Search")),
            new CompanyProblem(121, "Best Time to Buy and Sell Stock", "Easy", 54.2, "Meta", Arrays.asList("Array", "Dynamic Programming")),
            new CompanyProblem(139, "Word Break", "Medium", 45.0, "Meta", Arrays.asList("Hash Table", "String", "Dynamic Programming")),
            new CompanyProblem(146, "LRU Cache", "Medium", 40.5, "Meta", Arrays.asList("Hash Table", "Linked List", "Design")),
            new CompanyProblem(158, "Read N Characters Given Read4 II - Call multiple times", "Hard", 38.7, "Meta", Arrays.asList("String", "Interactive", "Simulation")),
            new CompanyProblem(173, "Binary Search Tree Iterator", "Medium", 71.5, "Meta", Arrays.asList("Stack", "Tree", "Design")),
            new CompanyProblem(199, "Binary Tree Right Side View", "Medium", 61.5, "Meta", Arrays.asList("Tree", "Depth-First Search", "Breadth-First Search")),
            new CompanyProblem(200, "Number of Islands", "Medium", 57.0, "Meta", Arrays.asList("Array", "Depth-First Search", "Breadth-First Search")),
            new CompanyProblem(208, "Implement Trie (Prefix Tree)", "Medium", 64.9, "Meta", Arrays.asList("Hash Table", "String", "Design", "Trie")),
            new CompanyProblem(209, "Minimum Size Subarray Sum", "Medium", 46.1, "Meta", Arrays.asList("Array", "Binary Search", "Sliding Window")),
            new CompanyProblem(215, "Kth Largest Element in an Array", "Medium", 66.7, "Meta", Arrays.asList("Array", "Divide and Conquer", "Sorting")),
            new CompanyProblem(236, "Lowest Common Ancestor of a Binary Tree", "Medium", 59.7, "Meta", Arrays.asList("Tree", "Depth-First Search")),
            new CompanyProblem(238, "Product of Array Except Self", "Medium", 64.8, "Meta", Arrays.asList("Array", "Prefix Sum")),
            new CompanyProblem(253, "Meeting Rooms II", "Medium", 50.5, "Meta", Arrays.asList("Array", "Two Pointers", "Greedy")),
            new CompanyProblem(269, "Alien Dictionary", "Hard", 35.1, "Meta", Arrays.asList("Array", "String", "Depth-First Search")),
            new CompanyProblem(270, "Closest Binary Search Tree Value", "Easy", 52.4, "Meta", Arrays.asList("Tree", "Depth-First Search", "Binary Search")),
            new CompanyProblem(271, "Encode and Decode Strings", "Medium", 36.8, "Meta", Arrays.asList("Array", "String", "Design")),
            new CompanyProblem(278, "First Bad Version", "Easy", 42.9, "Meta", Arrays.asList("Binary Search", "Interactive")),
            new CompanyProblem(285, "Inorder Successor in BST", "Medium", 46.9, "Meta", Arrays.asList("Tree", "Depth-First Search", "Binary Search Tree")),
            new CompanyProblem(297, "Serialize and Deserialize Binary Tree", "Hard", 55.2, "Meta", Arrays.asList("String", "Tree", "Depth-First Search")),
            new CompanyProblem(311, "Sparse Matrix Multiplication", "Medium", 65.5, "Meta", Arrays.asList("Array", "Hash Table", "Matrix")),
            new CompanyProblem(314, "Binary Tree Vertical Order Traversal", "Medium", 51.9, "Meta", Arrays.asList("Hash Table", "Tree", "Depth-First Search", "Breadth-First Search")),
            new CompanyProblem(325, "Maximum Size Subarray Sum Equals k", "Medium", 49.0, "Meta", Arrays.asList("Array", "Hash Table", "Prefix Sum")),
            new CompanyProblem(344, "Reverse String", "Easy", 78.1, "Meta", Arrays.asList("Two Pointers", "String")),
            new CompanyProblem(347, "Top K Frequent Elements", "Medium", 64.5, "Meta", Arrays.asList("Array", "Hash Table", "Divide and Conquer")),
            new CompanyProblem(348, "Design Tic-Tac-Toe", "Medium", 58.1, "Meta", Arrays.asList("Array", "Hash Table", "Design")),
            new CompanyProblem(349, "Intersection of Two Arrays", "Easy", 72.4, "Meta", Arrays.asList("Array", "Hash Table", "Two Pointers")),
            new CompanyProblem(350, "Intersection of Two Arrays II", "Easy", 55.6, "Meta", Arrays.asList("Array", "Hash Table", "Two Pointers")),
            new CompanyProblem(362, "Design Hit Counter", "Medium", 66.2, "Meta", Arrays.asList("Array", "Hash Table", "Binary Search")),
            new CompanyProblem(380, "Insert Delete GetRandom O(1)", "Medium", 52.3, "Meta", Arrays.asList("Array", "Hash Table", "Math", "Design")),
            new CompanyProblem(398, "Random Pick Index", "Medium", 65.6, "Meta", Arrays.asList("Hash Table", "Math", "Reservoir Sampling")),
            new CompanyProblem(415, "Add Strings", "Easy", 52.7, "Meta", Arrays.asList("Math", "String", "Simulation")),
            new CompanyProblem(426, "Convert Binary Search Tree to Sorted Doubly Linked List", "Medium", 63.1, "Meta", Arrays.asList("Linked List", "Stack", "Tree")),
            new CompanyProblem(438, "Find All Anagrams in a String", "Medium", 48.7, "Meta", Arrays.asList("Hash Table", "String", "Sliding Window")),
            new CompanyProblem(443, "String Compression", "Medium", 47.8, "Meta", Arrays.asList("Two Pointers", "String")),
            new CompanyProblem(461, "Hamming Distance", "Easy", 73.7, "Meta", Arrays.asList("Bit Manipulation")),
            new CompanyProblem(468, "Validate IP Address", "Medium", 25.9, "Meta", Arrays.asList("String")),
            new CompanyProblem(498, "Diagonal Traverse", "Medium", 58.3, "Meta", Arrays.asList("Array", "Matrix", "Simulation")),
            new CompanyProblem(523, "Continuous Subarray Sum", "Medium", 28.1, "Meta", Arrays.asList("Array", "Hash Table", "Math")),
            new CompanyProblem(525, "Contiguous Array", "Medium", 47.4, "Meta", Arrays.asList("Array", "Hash Table", "Prefix Sum")),
            new CompanyProblem(528, "Random Pick with Weight", "Medium", 45.9, "Meta", Arrays.asList("Math", "Binary Search", "Prefix Sum")),
            new CompanyProblem(535, "Encode and Decode TinyURL", "Medium", 84.4, "Meta", Arrays.asList("Hash Table", "String", "Design")),
            new CompanyProblem(543, "Diameter of Binary Tree", "Easy", 55.8, "Meta", Arrays.asList("Tree", "Depth-First Search", "Binary Tree")),
            new CompanyProblem(560, "Subarray Sum Equals K", "Medium", 43.4, "Meta", Arrays.asList("Array", "Hash Table", "Prefix Sum")),
            new CompanyProblem(567, "Permutation in String", "Medium", 44.6, "Meta", Arrays.asList("Hash Table", "Two Pointers", "String")),
            new CompanyProblem(611, "Valid Triangle Number", "Medium", 50.1, "Meta", Arrays.asList("Array", "Two Pointers", "Binary Search")),
            new CompanyProblem(621, "Task Scheduler", "Medium", 56.9, "Meta", Arrays.asList("Array", "Hash Table", "Greedy")),
            new CompanyProblem(636, "Exclusive Time of Functions", "Medium", 63.8, "Meta", Arrays.asList("Array", "Stack")),
            new CompanyProblem(647, "Palindromic Substrings", "Medium", 67.5, "Meta", Arrays.asList("String", "Dynamic Programming")),
            new CompanyProblem(653, "Two Sum IV - Input is a BST", "Easy", 60.4, "Meta", Arrays.asList("Hash Table", "Two Pointers", "Tree")),
            new CompanyProblem(670, "Maximum Swap", "Medium", 47.6, "Meta", Arrays.asList("Math", "Greedy")),
            new CompanyProblem(674, "Longest Continuous Increasing Subsequence", "Easy", 49.1, "Meta", Arrays.asList("Array")),
            new CompanyProblem(721, "Accounts Merge", "Medium", 56.6, "Meta", Arrays.asList("Array", "String", "Depth-First Search")),
            new CompanyProblem(791, "Custom Sort String", "Medium", 69.2, "Meta", Arrays.asList("Hash Table", "String", "Sorting")),
            new CompanyProblem(827, "Making A Large Island", "Hard", 46.0, "Meta", Arrays.asList("Array", "Depth-First Search", "Breadth-First Search")),
            new CompanyProblem(953, "Verifying an Alien Dictionary", "Easy", 52.1, "Meta", Arrays.asList("Array", "Hash Table", "String")),
            new CompanyProblem(973, "K Closest Points to Origin", "Medium", 65.5, "Meta", Arrays.asList("Array", "Math", "Divide and Conquer")),
            new CompanyProblem(987, "Vertical Order Traversal of a Binary Tree", "Hard", 45.8, "Meta", Arrays.asList("Hash Table", "Tree", "Depth-First Search")),
            new CompanyProblem(1004, "Max Consecutive Ones III", "Medium", 62.2, "Meta", Arrays.asList("Array", "Binary Search", "Sliding Window")),
            new CompanyProblem(1249, "Minimum Remove to Make Valid Parentheses", "Medium", 65.7, "Meta", Arrays.asList("String", "Stack")),
            new CompanyProblem(1266, "Minimum Time Visiting All Points", "Easy", 79.4, "Meta", Arrays.asList("Array", "Math", "Geometry")),
            new CompanyProblem(1283, "Find the Smallest Divisor Given a Threshold", "Medium", 54.8, "Meta", Arrays.asList("Array", "Binary Search")),
            new CompanyProblem(1480, "Running Sum of 1d Array", "Easy", 88.4, "Meta", Arrays.asList("Array", "Prefix Sum")),
            new CompanyProblem(1539, "Kth Missing Positive Number", "Easy", 58.7, "Meta", Arrays.asList("Array", "Binary Search")),
            new CompanyProblem(1762, "Buildings With an Ocean View", "Medium", 82.5, "Meta", Arrays.asList("Array", "Stack", "Monotonic Stack")),
            new CompanyProblem(1868, "Product of Two Run-Length Encoded Arrays", "Medium", 57.8, "Meta", Arrays.asList("Array", "Two Pointers")),
            
            // Hard Problems
            new CompanyProblem(10, "Regular Expression Matching", "Hard", 27.9, "Meta", Arrays.asList("String", "Dynamic Programming", "Recursion")),
            new CompanyProblem(23, "Merge k Sorted Lists", "Hard", 47.6, "Meta", Arrays.asList("Linked List", "Divide and Conquer", "Heap")),
            new CompanyProblem(25, "Reverse Nodes in k-Group", "Hard", 56.1, "Meta", Arrays.asList("Linked List", "Recursion")),
            new CompanyProblem(41, "First Missing Positive", "Hard", 36.9, "Meta", Arrays.asList("Array", "Hash Table")),
            new CompanyProblem(42, "Trapping Rain Water", "Hard", 58.4, "Meta", Arrays.asList("Array", "Two Pointers", "Dynamic Programming")),
            new CompanyProblem(68, "Text Justification", "Hard", 34.9, "Meta", Arrays.asList("Array", "String", "Simulation")),
            new CompanyProblem(124, "Binary Tree Maximum Path Sum", "Hard", 38.0, "Meta", Arrays.asList("Dynamic Programming", "Tree", "Depth-First Search")),
            new CompanyProblem(127, "Word Ladder", "Hard", 36.5, "Meta", Arrays.asList("Hash Table", "String", "Breadth-First Search")),
            new CompanyProblem(128, "Longest Consecutive Sequence", "Medium", 47.7, "Meta", Arrays.asList("Array", "Hash Table", "Union Find")),
            new CompanyProblem(140, "Word Break II", "Hard", 45.0, "Meta", Arrays.asList("Array", "Hash Table", "String")),
            new CompanyProblem(149, "Max Points on a Line", "Hard", 21.6, "Meta", Arrays.asList("Array", "Hash Table", "Math")),
            new CompanyProblem(212, "Word Search II", "Hard", 37.7, "Meta", Arrays.asList("Array", "String", "Backtracking", "Trie")),
            new CompanyProblem(224, "Basic Calculator", "Hard", 41.4, "Meta", Arrays.asList("Math", "String", "Stack", "Recursion")),
            new CompanyProblem(273, "Integer to English Words", "Hard", 28.8, "Meta", Arrays.asList("Math", "String", "Recursion")),
            new CompanyProblem(282, "Expression Add Operators", "Hard", 39.3, "Meta", Arrays.asList("Math", "String", "Backtracking")),
            new CompanyProblem(295, "Find Median from Data Stream", "Hard", 51.1, "Meta", Arrays.asList("Two Pointers", "Design", "Sorting")),
            new CompanyProblem(301, "Remove Invalid Parentheses", "Hard", 46.4, "Meta", Arrays.asList("String", "Backtracking", "Breadth-First Search")),
            new CompanyProblem(312, "Burst Balloons", "Hard", 55.8, "Meta", Arrays.asList("Array", "Dynamic Programming")),
            new CompanyProblem(329, "Longest Increasing Path in a Matrix", "Hard", 51.8, "Meta", Arrays.asList("Array", "Dynamic Programming", "Depth-First Search")),
            new CompanyProblem(632, "Smallest Range Covering Elements from K Lists", "Hard", 60.5, "Meta", Arrays.asList("Array", "Hash Table", "Greedy"))
        );
        
        // Google problems (2097 problems)
        List<CompanyProblem> googleProblems = Arrays.asList(
            new CompanyProblem(1, "Two Sum", "Easy", 49.1, "Google", Arrays.asList("Array", "Hash Table")),
            new CompanyProblem(2, "Add Two Numbers", "Medium", 38.9, "Google", Arrays.asList("Linked List", "Math")),
            new CompanyProblem(3, "Longest Substring Without Repeating Characters", "Medium", 33.8, "Google", Arrays.asList("Hash Table", "String", "Sliding Window")),
            new CompanyProblem(15, "3Sum", "Medium", 32.1, "Google", Arrays.asList("Array", "Two Pointers", "Sorting")),
            new CompanyProblem(20, "Valid Parentheses", "Easy", 40.7, "Google", Arrays.asList("String", "Stack")),
            new CompanyProblem(42, "Trapping Rain Water", "Hard", 58.4, "Google", Arrays.asList("Array", "Two Pointers", "Dynamic Programming")),
            new CompanyProblem(70, "Climbing Stairs", "Easy", 51.5, "Google", Arrays.asList("Math", "Dynamic Programming", "Memoization")),
            new CompanyProblem(121, "Best Time to Buy and Sell Stock", "Easy", 54.2, "Google", Arrays.asList("Array", "Dynamic Programming")),
            new CompanyProblem(146, "LRU Cache", "Medium", 40.5, "Google", Arrays.asList("Hash Table", "Linked List", "Design")),
            new CompanyProblem(200, "Number of Islands", "Medium", 57.0, "Google", Arrays.asList("Array", "Depth-First Search", "Breadth-First Search")),
            new CompanyProblem(208, "Implement Trie (Prefix Tree)", "Medium", 64.9, "Google", Arrays.asList("Hash Table", "String", "Design", "Trie")),
            new CompanyProblem(212, "Word Search II", "Hard", 37.7, "Google", Arrays.asList("Array", "String", "Backtracking", "Trie")),
            new CompanyProblem(218, "The Skyline Problem", "Hard", 39.1, "Google", Arrays.asList("Array", "Divide and Conquer", "Binary Indexed Tree")),
            new CompanyProblem(224, "Basic Calculator", "Hard", 41.4, "Google", Arrays.asList("Math", "String", "Stack", "Recursion")),
            new CompanyProblem(297, "Serialize and Deserialize Binary Tree", "Hard", 55.2, "Google", Arrays.asList("String", "Tree", "Depth-First Search"))
        );
        
        // Amazon problems (1885 problems)
        List<CompanyProblem> amazonProblems = Arrays.asList(
            new CompanyProblem(1, "Two Sum", "Easy", 49.1, "Amazon", Arrays.asList("Array", "Hash Table")),
            new CompanyProblem(5, "Longest Palindromic Substring", "Medium", 32.8, "Amazon", Arrays.asList("String", "Dynamic Programming")),
            new CompanyProblem(8, "String to Integer (atoi)", "Medium", 16.6, "Amazon", Arrays.asList("String")),
            new CompanyProblem(11, "Container With Most Water", "Medium", 54.0, "Amazon", Arrays.asList("Array", "Two Pointers", "Greedy")),
            new CompanyProblem(21, "Merge Two Sorted Lists", "Easy", 62.4, "Amazon", Arrays.asList("Linked List", "Recursion")),
            new CompanyProblem(53, "Maximum Subarray", "Medium", 50.1, "Amazon", Arrays.asList("Array", "Divide and Conquer", "Dynamic Programming")),
            new CompanyProblem(102, "Binary Tree Level Order Traversal", "Medium", 64.4, "Amazon", Arrays.asList("Tree", "Breadth-First Search")),
            new CompanyProblem(139, "Word Break", "Medium", 45.0, "Amazon", Arrays.asList("Hash Table", "String", "Dynamic Programming")),
            new CompanyProblem(167, "Two Sum II - Input Array Is Sorted", "Medium", 59.0, "Amazon", Arrays.asList("Array", "Two Pointers", "Binary Search")),
            new CompanyProblem(238, "Product of Array Except Self", "Medium", 64.8, "Amazon", Arrays.asList("Array", "Prefix Sum")),
            new CompanyProblem(347, "Top K Frequent Elements", "Medium", 64.5, "Amazon", Arrays.asList("Array", "Hash Table", "Divide and Conquer")),
            new CompanyProblem(380, "Insert Delete GetRandom O(1)", "Medium", 52.3, "Amazon", Arrays.asList("Array", "Hash Table", "Math", "Design")),
            new CompanyProblem(387, "First Unique Character in a String", "Easy", 58.7, "Amazon", Arrays.asList("Hash Table", "String", "Queue")),
            new CompanyProblem(460, "LFU Cache", "Hard", 41.4, "Amazon", Arrays.asList("Hash Table", "Linked List", "Design")),
            new CompanyProblem(937, "Reorder Data in Log Files", "Easy", 54.3, "Amazon", Arrays.asList("Array", "String", "Sorting"))
        );
        
        // Microsoft problems (1239 problems)
        List<CompanyProblem> microsoftProblems = Arrays.asList(
            new CompanyProblem(4, "Median of Two Sorted Arrays", "Hard", 37.4, "Microsoft", Arrays.asList("Array", "Binary Search", "Divide and Conquer")),
            new CompanyProblem(13, "Roman to Integer", "Easy", 58.7, "Microsoft", Arrays.asList("Hash Table", "Math", "String")),
            new CompanyProblem(14, "Longest Common Prefix", "Easy", 41.1, "Microsoft", Arrays.asList("String", "Trie")),
            new CompanyProblem(26, "Remove Duplicates from Sorted Array", "Easy", 53.2, "Microsoft", Arrays.asList("Array", "Two Pointers")),
            new CompanyProblem(48, "Rotate Image", "Medium", 71.5, "Microsoft", Arrays.asList("Array", "Math", "Matrix")),
            new CompanyProblem(56, "Merge Intervals", "Medium", 46.5, "Microsoft", Arrays.asList("Array", "Sorting")),
            new CompanyProblem(88, "Merge Sorted Array", "Easy", 46.5, "Microsoft", Arrays.asList("Array", "Two Pointers", "Sorting")),
            new CompanyProblem(103, "Binary Tree Zigzag Level Order Traversal", "Medium", 56.9, "Microsoft", Arrays.asList("Tree", "Breadth-First Search")),
            new CompanyProblem(125, "Valid Palindrome", "Easy", 44.1, "Microsoft", Arrays.asList("Two Pointers", "String")),
            new CompanyProblem(206, "Reverse Linked List", "Easy", 73.4, "Microsoft", Arrays.asList("Linked List", "Recursion")),
            new CompanyProblem(215, "Kth Largest Element in an Array", "Medium", 66.7, "Microsoft", Arrays.asList("Array", "Divide and Conquer", "Sorting")),
            new CompanyProblem(236, "Lowest Common Ancestor of a Binary Tree", "Medium", 59.7, "Microsoft", Arrays.asList("Tree", "Depth-First Search")),
            new CompanyProblem(283, "Move Zeroes", "Easy", 60.4, "Microsoft", Arrays.asList("Array", "Two Pointers")),
            new CompanyProblem(394, "Decode String", "Medium", 57.8, "Microsoft", Arrays.asList("String", "Stack", "Recursion")),
            new CompanyProblem(560, "Subarray Sum Equals K", "Medium", 43.4, "Microsoft", Arrays.asList("Array", "Hash Table", "Prefix Sum"))
        );
        
        // Apple problems (537 problems)
        List<CompanyProblem> appleProblems = Arrays.asList(
            new CompanyProblem(7, "Reverse Integer", "Medium", 27.5, "Apple", Arrays.asList("Math")),
            new CompanyProblem(9, "Palindrome Number", "Easy", 54.1, "Apple", Arrays.asList("Math")),
            new CompanyProblem(17, "Letter Combinations of a Phone Number", "Medium", 58.1, "Apple", Arrays.asList("Hash Table", "String", "Backtracking")),
            new CompanyProblem(22, "Generate Parentheses", "Medium", 71.8, "Apple", Arrays.asList("String", "Dynamic Programming", "Backtracking")),
            new CompanyProblem(28, "Find the Index of the First Occurrence in a String", "Easy", 37.4, "Apple", Arrays.asList("Two Pointers", "String", "String Matching")),
            new CompanyProblem(49, "Group Anagrams", "Medium", 67.6, "Apple", Arrays.asList("Array", "Hash Table", "String", "Sorting")),
            new CompanyProblem(66, "Plus One", "Easy", 43.4, "Apple", Arrays.asList("Array", "Math")),
            new CompanyProblem(118, "Pascal's Triangle", "Easy", 70.2, "Apple", Arrays.asList("Array", "Dynamic Programming")),
            new CompanyProblem(141, "Linked List Cycle", "Easy", 48.3, "Apple", Arrays.asList("Hash Table", "Linked List", "Two Pointers")),
            new CompanyProblem(242, "Valid Anagram", "Easy", 63.2, "Apple", Arrays.asList("Hash Table", "String", "Sorting")),
            new CompanyProblem(268, "Missing Number", "Easy", 64.0, "Apple", Arrays.asList("Array", "Hash Table", "Math")),
            new CompanyProblem(344, "Reverse String", "Easy", 78.1, "Apple", Arrays.asList("Two Pointers", "String")),
            new CompanyProblem(383, "Ransom Note", "Easy", 60.5, "Apple", Arrays.asList("Hash Table", "String", "Counting")),
            new CompanyProblem(647, "Palindromic Substrings", "Medium", 67.5, "Apple", Arrays.asList("String", "Dynamic Programming")),
            new CompanyProblem(704, "Binary Search", "Easy", 54.8, "Apple", Arrays.asList("Array", "Binary Search"))
        );
        
        // Store in map with more companies
        companyProblems.put("Meta", metaProblems);
        companyProblems.put("Google", googleProblems);
        companyProblems.put("Amazon", amazonProblems);
        companyProblems.put("Microsoft", microsoftProblems);
        companyProblems.put("Apple", appleProblems);
        
        // Add more companies with sample problems
        addMoreCompanyProblems();
        
        // Try to load real company data from LeetCode API in background
        loadRealCompanyProblemsInBackground();
    }
    
    private void addMoreCompanyProblems() {
        // Netflix problems
        companyProblems.put("Netflix", Arrays.asList(
            new CompanyProblem(128, "Longest Consecutive Sequence", "Medium", 47.7, "Netflix", Arrays.asList("Array", "Hash Table", "Union Find")),
            new CompanyProblem(155, "Min Stack", "Medium", 51.4, "Netflix", Arrays.asList("Stack", "Design")),
            new CompanyProblem(236, "Lowest Common Ancestor of a Binary Tree", "Medium", 59.7, "Netflix", Arrays.asList("Tree", "Depth-First Search", "Binary Tree")),
            new CompanyProblem(295, "Find Median from Data Stream", "Hard", 51.1, "Netflix", Arrays.asList("Two Pointers", "Design", "Sorting"))
        ));
        
        // Uber problems  
        companyProblems.put("Uber", Arrays.asList(
            new CompanyProblem(36, "Valid Sudoku", "Medium", 59.5, "Uber", Arrays.asList("Array", "Hash Table", "Matrix")),
            new CompanyProblem(54, "Spiral Matrix", "Medium", 42.4, "Uber", Arrays.asList("Array", "Matrix", "Simulation")),
            new CompanyProblem(79, "Word Search", "Medium", 40.1, "Uber", Arrays.asList("Array", "Backtracking", "Matrix")),
            new CompanyProblem(289, "Game of Life", "Medium", 66.0, "Uber", Arrays.asList("Array", "Matrix", "Simulation"))
        ));
        
        // TikTok problems
        companyProblems.put("TikTok", Arrays.asList(
            new CompanyProblem(224, "Basic Calculator", "Hard", 41.4, "TikTok", Arrays.asList("Math", "String", "Stack")),
            new CompanyProblem(227, "Basic Calculator II", "Medium", 42.4, "TikTok", Arrays.asList("Math", "String", "Stack")),
            new CompanyProblem(636, "Exclusive Time of Functions", "Medium", 63.8, "TikTok", Arrays.asList("Array", "Stack")),
            new CompanyProblem(1249, "Minimum Remove to Make Valid Parentheses", "Medium", 65.7, "TikTok", Arrays.asList("String", "Stack"))
        ));
        
        // Adobe problems
        companyProblems.put("Adobe", Arrays.asList(
            new CompanyProblem(65, "Valid Number", "Hard", 18.8, "Adobe", Arrays.asList("String")),
            new CompanyProblem(68, "Text Justification", "Hard", 34.9, "Adobe", Arrays.asList("Array", "String", "Simulation")),
            new CompanyProblem(72, "Edit Distance", "Hard", 54.9, "Adobe", Arrays.asList("String", "Dynamic Programming")),
            new CompanyProblem(85, "Maximal Rectangle", "Hard", 46.3, "Adobe", Arrays.asList("Array", "Dynamic Programming", "Stack"))
        ));
        
        // Tesla problems
        companyProblems.put("Tesla", Arrays.asList(
            new CompanyProblem(101, "Symmetric Tree", "Easy", 54.6, "Tesla", Arrays.asList("Tree", "Depth-First Search", "Breadth-First Search")),
            new CompanyProblem(104, "Maximum Depth of Binary Tree", "Easy", 74.4, "Tesla", Arrays.asList("Tree", "Depth-First Search", "Breadth-First Search")),
            new CompanyProblem(111, "Minimum Depth of Binary Tree", "Easy", 46.5, "Tesla", Arrays.asList("Tree", "Depth-First Search", "Breadth-First Search")),
            new CompanyProblem(226, "Invert Binary Tree", "Easy", 76.3, "Tesla", Arrays.asList("Tree", "Depth-First Search", "Breadth-First Search"))
        ));

        // Add sample problems for other major companies
        String[] majorCompanies = {
            "Oracle", "LinkedIn", "Bloomberg", "Citadel", "IBM", "Goldman Sachs", "PayPal", 
            "Stripe", "Square", "Shopify", "Reddit", "Pinterest", "Discord", "Zoom", "Slack",
            "ByteDance", "Airbnb", "Spotify", "Salesforce", "Twitter", "Dropbox", "Snap",
            "VMware", "Intuit", "Coinbase", "Robinhood", "DoorDash", "Lyft", "Twitch",
            "Palantir", "Roblox", "Atlassian", "ServiceNow", "Snowflake", "Databricks",
            "Twilio", "Okta", "Zendesk", "HubSpot", "MongoDB", "Elastic", "Splunk",
            "Tableau", "DocuSign", "Workday", "Box", "CrowdStrike", "Zscaler"
        };
        for (String company : majorCompanies) {
            companyProblems.put(company, Arrays.asList(
                new CompanyProblem(1, "Two Sum", "Easy", 49.1, company, Arrays.asList("Array", "Hash Table")),
                new CompanyProblem(21, "Merge Two Sorted Lists", "Easy", 62.4, company, Arrays.asList("Linked List", "Recursion")),
                new CompanyProblem(121, "Best Time to Buy and Sell Stock", "Easy", 54.2, company, Arrays.asList("Array", "Dynamic Programming"))
            ));
        }
    }
    
    private void loadRealCompanyProblemsInBackground() {
        // This method would implement actual LeetCode API calls to fetch real company data
        // For now, we'll use the static mappings above
        android.util.Log.d("CompanyProblems", "Real-time company problem loading would be implemented here");
        
        // TODO: Implement GraphQL queries to LeetCode API for company-specific problems
        // Example query structure:
        // {
        //   problemsetQuestionList(
        //     categorySlug: ""
        //     limit: 50
        //     skip: 0
        //     filters: {
        //       companyTag: "google"
        //     }
        //   ) {
        //     questions {
        //       title
        //       titleSlug
        //       difficulty
        //       acRate
        //       topicTags {
        //         name
        //       }
        //     }
        //   }
        // }
    }
    
    private void loadMoreProblems() {
        if (isLoading) return;
        
        isLoading = true;
        problemsAdapter.setLoading(true);
        
        // Simulate loading delay for smooth UX
        problemsRecyclerView.postDelayed(() -> {
            List<CompanyProblem> newProblems = getProblemsForPage(currentPage);
            
            if (!newProblems.isEmpty()) {
                int oldSize = filteredProblems.size();
                // Don't add to allProblems here - allProblems should only contain deduplicated problems
                
                // Filter new problems based on current selection
                List<CompanyProblem> newFilteredProblems = new ArrayList<>();
                for (CompanyProblem problem : newProblems) {
                    if (shouldIncludeProblem(problem)) {
                        newFilteredProblems.add(problem);
                    }
                }
                
                if (!newFilteredProblems.isEmpty()) {
                    filteredProblems.addAll(newFilteredProblems);
                    sortFilteredProblems();
                    problemsAdapter.notifyItemRangeInserted(oldSize, newFilteredProblems.size());
                }
                
                currentPage++;
                updateProblemCount();
            }
            
            problemsAdapter.setLoading(false);
            isLoading = false;
        }, 200);
    }
    
    private List<CompanyProblem> getProblemsForPage(int page) {
        List<CompanyProblem> pageProblems = new ArrayList<>();
        
        // Use deduplicated allProblems instead of raw companyProblems
        // Filter based on current company selection
        for (CompanyProblem problem : allProblems) {
            boolean matchesCompany = currentCompany.equals("All") || 
                problem.getCompanies().contains(currentCompany);
            
            if (matchesCompany) {
                pageProblems.add(problem);
            }
        }
        
        // Sort by problem ID and return a page
        pageProblems.sort((p1, p2) -> Integer.compare(p1.getId(), p2.getId()));
        
        int startIndex = (page - 1) * PROBLEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + PROBLEMS_PER_PAGE, pageProblems.size());
        
        if (startIndex < pageProblems.size()) {
            return pageProblems.subList(startIndex, endIndex);
        }
        
        return new ArrayList<>();
    }
    
    private boolean shouldIncludeProblem(CompanyProblem problem) {
        String searchQuery = searchEditText.getText().toString();
        boolean matchesSearch = searchQuery.isEmpty() || 
            problem.getTitle().toLowerCase().contains(searchQuery.toLowerCase()) ||
            String.valueOf(problem.getId()).contains(searchQuery);
            
        boolean matchesCompany = currentCompany.equals("All") || 
            problem.getCompany().equals(currentCompany);
            
        return matchesSearch && matchesCompany;
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
    
    private void filterProblems(String searchQuery) {
        // Safety check - ensure filteredProblems and allProblems are initialized
        if (filteredProblems == null || allProblems == null || allProblems.isEmpty()) {
            android.util.Log.d("CompanyProblems", "filterProblems: Early return - lists not ready");
            return;
        }
        
        android.util.Log.d("CompanyProblems", "filterProblems: currentCompany=" + currentCompany + ", searchQuery='" + searchQuery + "'");
        android.util.Log.d("CompanyProblems", "filterProblems: allProblems size=" + allProblems.size());
        
        filteredProblems.clear();
        
        int twoSumMatches = 0;
        int totalMatches = 0;
        // Filter problems based on current company selection and search query
        for (CompanyProblem problem : allProblems) {
            // Check if problem matches current company filter
            boolean matchesCompany = currentCompany.equals("All") || 
                problem.getCompanies().contains(currentCompany);
            
            // Check if problem matches search query
            boolean matchesSearch = searchQuery.isEmpty() || 
                problem.getTitle().toLowerCase().contains(searchQuery.toLowerCase()) ||
                String.valueOf(problem.getId()).contains(searchQuery) ||
                problem.getDifficulty().toLowerCase().contains(searchQuery.toLowerCase());
            
            if (matchesCompany && matchesSearch) {
                filteredProblems.add(problem);
                totalMatches++;
                
                if (problem.getTitle().toLowerCase().contains("sum")) {
                    android.util.Log.d("CompanyProblems", "Found problem with 'sum': " + problem.getTitle() + " (ID: " + problem.getId() + ") Companies: " + problem.getCompanies());
                }
                
                if (problem.getId() == 1) {
                    twoSumMatches++;
                    android.util.Log.d("CompanyProblems", "Two Sum added to filtered list. Companies: " + problem.getCompanies());
                }
            }
        }
        
        android.util.Log.d("CompanyProblems", "filterProblems: filteredProblems size=" + filteredProblems.size() + ", Two Sum matches=" + twoSumMatches + ", Total matches=" + totalMatches);
        
        sortFilteredProblems();
        
        // Safely notify adapter if it exists
        if (problemsAdapter != null) {
            problemsAdapter.notifyDataSetChanged();
        }
        
        if (!searchQuery.isEmpty() && problemsRecyclerView != null) {
            problemsRecyclerView.scrollToPosition(0);
        }
        
        updateProblemCount();
    }
    
    private void sortFilteredProblems() {
        android.util.Log.d("CompanyProblems", "sortFilteredProblems called with sortOrder: " + sortOrder);
        android.util.Log.d("CompanyProblems", "filteredProblems size before sort: " + filteredProblems.size());
        
        switch (sortOrder) {
            case "Easy->Hard":
                android.util.Log.d("CompanyProblems", "Sorting Easy->Hard");
                filteredProblems.sort((p1, p2) -> getDifficultyOrder(p1.getDifficulty()) - getDifficultyOrder(p2.getDifficulty()));
                break;
            case "Hard->Easy":
                android.util.Log.d("CompanyProblems", "Sorting Hard->Easy");
                filteredProblems.sort((p1, p2) -> getDifficultyOrder(p2.getDifficulty()) - getDifficultyOrder(p1.getDifficulty()));
                break;
            case "A-Z":
                android.util.Log.d("CompanyProblems", "Sorting A-Z");
                filteredProblems.sort((p1, p2) -> p1.getTitle().compareToIgnoreCase(p2.getTitle()));
                break;
            case "Z-A":
                android.util.Log.d("CompanyProblems", "Sorting Z-A");
                filteredProblems.sort((p1, p2) -> p2.getTitle().compareToIgnoreCase(p1.getTitle()));
                break;
            case "Acceptance Asc":
                android.util.Log.d("CompanyProblems", "Sorting Acceptance Asc");
                filteredProblems.sort((p1, p2) -> Double.compare(p1.getAcceptanceRate(), p2.getAcceptanceRate()));
                break;
            case "Acceptance Desc":
                android.util.Log.d("CompanyProblems", "Sorting Acceptance Desc");
                filteredProblems.sort((p1, p2) -> Double.compare(p2.getAcceptanceRate(), p1.getAcceptanceRate()));
                break;
            default: // "Default"
                android.util.Log.d("CompanyProblems", "Sorting Default (by ID)");
                filteredProblems.sort((p1, p2) -> Integer.compare(p1.getId(), p2.getId()));
                break;
        }
        
        android.util.Log.d("CompanyProblems", "Sort completed");
    }
    
    private int getDifficultyOrder(String difficulty) {
        switch (difficulty) {
            case "Easy": return 1;
            case "Medium": return 2;
            case "Hard": return 3;
            default: return 0;
        }
    }
    
    private void filterProblems() {
        android.util.Log.d("CompanyProblems", "filterProblems called with selectedDifficulties: " + selectedDifficulties);
        
        filteredProblems.clear();
        
        for (CompanyProblem problem : allProblems) {
            boolean matchesDifficulty = selectedDifficulties.isEmpty() ||
                selectedDifficulties.contains(problem.getDifficulty());
            
            if (matchesDifficulty) {
                filteredProblems.add(problem);
            }
        }
        
        android.util.Log.d("CompanyProblems", "After filtering: " + filteredProblems.size() + " problems");
        
        // Apply sorting
        sortFilteredProblems();
        
        // Update UI
        problemsAdapter.notifyDataSetChanged();
        updateProblemCount();
        
        // Reset scroll position to top when filtering
        if (!selectedDifficulties.isEmpty()) {
            problemsRecyclerView.scrollToPosition(0);
        }
    }
    
    private void showSortDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_sort_options, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        
        // Get views from dialog
        RadioGroup sortOrderRadioGroup = dialogView.findViewById(R.id.sortOrderRadioGroup);
        CheckBox easyCheckBox = dialogView.findViewById(R.id.easyCheckBox);
        CheckBox mediumCheckBox = dialogView.findViewById(R.id.mediumCheckBox);
        CheckBox hardCheckBox = dialogView.findViewById(R.id.hardCheckBox);
        TextView applyButton = dialogView.findViewById(R.id.applySortButton);
        TextView cancelButton = dialogView.findViewById(R.id.cancelSortButton);
        TextView clearButton = dialogView.findViewById(R.id.clearSortButton);
        
        // Set current sort selection - use setChecked instead of performClick
        switch (sortOrder) {
            case "Default":
                ((RadioButton) dialogView.findViewById(R.id.defaultSortRadio)).setChecked(true);
                break;
            case "Easy->Hard":
                ((RadioButton) dialogView.findViewById(R.id.easyToHardRadio)).setChecked(true);
                break;
            case "Hard->Easy":
                ((RadioButton) dialogView.findViewById(R.id.hardToEasyRadio)).setChecked(true);
                break;
            case "A-Z":
                ((RadioButton) dialogView.findViewById(R.id.aTozRadio)).setChecked(true);
                break;
            case "Z-A":
                ((RadioButton) dialogView.findViewById(R.id.zToARadio)).setChecked(true);
                break;
            case "Acceptance Asc":
                ((RadioButton) dialogView.findViewById(R.id.acceptanceAscRadio)).setChecked(true);
                break;
            case "Acceptance Desc":
                ((RadioButton) dialogView.findViewById(R.id.acceptanceDescRadio)).setChecked(true);
                break;
        }
        
        // Set current difficulty filter selections
        easyCheckBox.setChecked(selectedDifficulties.contains("Easy"));
        mediumCheckBox.setChecked(selectedDifficulties.contains("Medium"));
        hardCheckBox.setChecked(selectedDifficulties.contains("Hard"));
        
        // Apply button click
        applyButton.setOnClickListener(v -> {
            android.util.Log.d("CompanyProblems", "Apply button clicked");
            
            // Update difficulty selections
            selectedDifficulties.clear();
            if (easyCheckBox.isChecked()) selectedDifficulties.add("Easy");
            if (mediumCheckBox.isChecked()) selectedDifficulties.add("Medium");
            if (hardCheckBox.isChecked()) selectedDifficulties.add("Hard");
            
            android.util.Log.d("CompanyProblems", "Selected difficulties: " + selectedDifficulties);
            
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
            
            android.util.Log.d("CompanyProblems", "Sort order: " + sortOrder);
            
            // Apply filters and sorting
            filterProblems();
            
            dialog.dismiss();
        });        // Cancel button click
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        
        // Clear button click
        clearButton.setOnClickListener(v -> {
            // Reset selections
            selectedDifficulties.clear();
            sortOrder = "Default";
            
            // Apply filters and sorting
            filterProblems();
            
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    private void updateProblemCount() {
        int totalCount = filteredProblems.size();
        String countText;
        
        if (currentCompany.equals("All")) {
            countText = totalCount + " Company Problems";
        } else {
            countText = totalCount + " " + currentCompany + " Problems";
        }
        
        problemCountText.setText(countText);
    }
    
    private void setupBottomNavigation() {
        // Define navigation colors
        int activeColor = getColor(R.color.accent_secondary);
        int inactiveColor = getColor(R.color.text_secondary);
        
        // Get navigation elements
        LinearLayout navHome = findViewById(R.id.nav_home);
        LinearLayout navProgress = findViewById(R.id.nav_progress);
        LinearLayout navCards = findViewById(R.id.nav_cards);
        LinearLayout navRevision = findViewById(R.id.nav_revision);
        
        // Set click listeners
        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(CompanyProblemsActivity.this, ModernMainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("navigate_to_home", true);
            startActivity(intent);
            finish();
        });
        
        navProgress.setOnClickListener(v -> {
            Intent intent = new Intent(CompanyProblemsActivity.this, ProblemsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            finish();
        });
        
        navCards.setOnClickListener(v -> {
            // Already on Company Problems page - scroll to top
            problemsRecyclerView.smoothScrollToPosition(0);
        });
        
        navRevision.setOnClickListener(v -> {
            Intent intent = new Intent(CompanyProblemsActivity.this, RevisionActivity.class);
            startActivity(intent);
        });
        
        // Set Companies (Cards) as the selected item
        selectNavItem(2);
    }
    
    private void resetAllNavItems() {
        int inactiveColor = getColor(R.color.text_secondary);
        
        // Hide all indicators
        findViewById(R.id.home_indicator).setVisibility(View.GONE);
        findViewById(R.id.progress_indicator).setVisibility(View.GONE);
        findViewById(R.id.cards_indicator).setVisibility(View.GONE);
        findViewById(R.id.revision_indicator).setVisibility(View.GONE);
        
        // Get all icons and text elements
        ImageView homeIcon = findViewById(R.id.home_icon);
        ImageView progressIcon = findViewById(R.id.progress_icon);
        ImageView cardsIcon = findViewById(R.id.cards_icon);
        ImageView revisionIcon = findViewById(R.id.revision_icon);
        
        TextView homeText = findViewById(R.id.home_text);
        TextView progressText = findViewById(R.id.progress_text);
        TextView cardsText = findViewById(R.id.cards_text);
        TextView revisionText = findViewById(R.id.revision_text);
        
        // Reset all icons to inactive color
        if (homeIcon != null) homeIcon.setColorFilter(inactiveColor);
        if (progressIcon != null) progressIcon.setColorFilter(inactiveColor);
        if (cardsIcon != null) cardsIcon.setColorFilter(inactiveColor);
        if (revisionIcon != null) revisionIcon.setColorFilter(inactiveColor);
        
        // Reset all text to inactive color and normal weight
        if (homeText != null) {
            homeText.setTextColor(inactiveColor);
            homeText.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
        if (progressText != null) {
            progressText.setTextColor(inactiveColor);
            progressText.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
        if (cardsText != null) {
            cardsText.setTextColor(inactiveColor);
            cardsText.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
        if (revisionText != null) {
            revisionText.setTextColor(inactiveColor);
            revisionText.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }
    
    private void selectNavItem(int index) {
        // Reset all items to inactive state
        resetAllNavItems();
        
        int activeColor = getColor(R.color.accent_secondary);
        
        switch (index) {
            case 0: // Home
                findViewById(R.id.home_indicator).setVisibility(View.VISIBLE);
                ImageView homeIcon = findViewById(R.id.home_icon);
                TextView homeText = findViewById(R.id.home_text);
                if (homeIcon != null) homeIcon.setColorFilter(activeColor);
                if (homeText != null) {
                    homeText.setTextColor(activeColor);
                    homeText.setTypeface(null, android.graphics.Typeface.BOLD);
                }
                break;
            case 1: // Progress
                findViewById(R.id.progress_indicator).setVisibility(View.VISIBLE);
                ImageView progressIcon = findViewById(R.id.progress_icon);
                TextView progressText = findViewById(R.id.progress_text);
                if (progressIcon != null) progressIcon.setColorFilter(activeColor);
                if (progressText != null) {
                    progressText.setTextColor(activeColor);
                    progressText.setTypeface(null, android.graphics.Typeface.BOLD);
                }
                break;
            case 2: // Companies
                findViewById(R.id.cards_indicator).setVisibility(View.VISIBLE);
                ImageView cardsIcon = findViewById(R.id.cards_icon);
                TextView cardsText = findViewById(R.id.cards_text);
                if (cardsIcon != null) cardsIcon.setColorFilter(activeColor);
                if (cardsText != null) {
                    cardsText.setTextColor(activeColor);
                    cardsText.setTypeface(null, android.graphics.Typeface.BOLD);
                }
                break;
            case 3: // Revision
                findViewById(R.id.revision_indicator).setVisibility(View.VISIBLE);
                ImageView revisionIcon = findViewById(R.id.revision_icon);
                TextView revisionText = findViewById(R.id.revision_text);
                if (revisionIcon != null) revisionIcon.setColorFilter(activeColor);
                if (revisionText != null) {
                    revisionText.setTextColor(activeColor);
                    revisionText.setTypeface(null, android.graphics.Typeface.BOLD);
                }
                break;
        }
    }
    
    private void showSkeletonLoading(boolean show) {
        if (show) {
            if (skeletonView == null && skeletonStub != null) {
                skeletonView = skeletonStub.inflate();
                startSkeletonAnimation(skeletonView);
            }
            
            if (contentContainer != null) {
                contentContainer.setVisibility(View.GONE);
            }
            if (skeletonView != null) {
                skeletonView.setVisibility(View.VISIBLE);
            }
        } else {
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
    
    // CompanyProblem class
    public static class CompanyProblem {
        private int id;
        private String title;
        private String titleSlug;
        private String difficulty;
        private double acceptanceRate;
        private List<String> companies; // Changed from single company to list of companies
        private List<String> topics;
        
        public CompanyProblem(int id, String title, String difficulty, double acceptanceRate, String company, List<String> topics) {
            this.id = id;
            this.title = title;
            this.titleSlug = generateSlugFromTitle(title);
            this.difficulty = difficulty;
            this.acceptanceRate = acceptanceRate;
            this.companies = new ArrayList<>(Arrays.asList(company)); // Initialize with single company
            this.topics = topics;
        }
        
        // Constructor for multiple companies
        public CompanyProblem(int id, String title, String difficulty, double acceptanceRate, List<String> companies, List<String> topics) {
            this.id = id;
            this.title = title;
            this.titleSlug = generateSlugFromTitle(title);
            this.difficulty = difficulty;
            this.acceptanceRate = acceptanceRate;
            this.companies = new ArrayList<>(companies);
            this.topics = topics;
        }
        
        // Method to add a company to this problem
        public void addCompany(String company) {
            if (!this.companies.contains(company)) {
                this.companies.add(company);
            }
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
                case 7: return "reverse-integer";
                case 8: return "string-to-integer-atoi";
                case 9: return "palindrome-number";
                case 10: return "regular-expression-matching";
                case 11: return "container-with-most-water";
                case 13: return "roman-to-integer";
                case 14: return "longest-common-prefix";
                case 15: return "3sum";
                case 17: return "letter-combinations-of-a-phone-number";
                case 20: return "valid-parentheses";
                case 21: return "merge-two-sorted-lists";
                case 22: return "generate-parentheses";
                case 23: return "merge-k-sorted-lists";
                case 26: return "remove-duplicates-from-sorted-array";
                case 28: return "find-the-index-of-the-first-occurrence-in-a-string";
                case 31: return "next-permutation";
                case 33: return "search-in-rotated-sorted-array";
                case 36: return "valid-sudoku";
                case 42: return "trapping-rain-water";
                case 43: return "multiply-strings";
                case 48: return "rotate-image";
                case 49: return "group-anagrams";
                case 53: return "maximum-subarray";
                case 54: return "spiral-matrix";
                case 56: return "merge-intervals";
                case 66: return "plus-one";
                case 67: return "add-binary";
                case 70: return "climbing-stairs";
                case 79: return "word-search";
                case 88: return "merge-sorted-array";
                case 91: return "decode-ways";
                case 102: return "binary-tree-level-order-traversal";
                case 103: return "binary-tree-zigzag-level-order-traversal";
                case 118: return "pascals-triangle";
                case 121: return "best-time-to-buy-and-sell-stock";
                case 125: return "valid-palindrome";
                case 127: return "word-ladder";
                case 128: return "longest-consecutive-sequence";
                case 139: return "word-break";
                case 141: return "linked-list-cycle";
                case 146: return "lru-cache";
                case 155: return "min-stack";
                case 157: return "read-n-characters-given-read4";
                case 167: return "two-sum-ii-input-array-is-sorted";
                case 200: return "number-of-islands";
                case 206: return "reverse-linked-list";
                case 236: return "lowest-common-ancestor-of-a-binary-tree";
                case 238: return "product-of-array-except-self";
                case 242: return "valid-anagram";
                case 273: return "integer-to-english-words";
                default: return null; // Return null for unmapped problems, will use generated slug
            }
        }
        
        // Getters
        public int getId() { return id; }
        public String getTitle() { return title; }
        public String getTitleSlug() { return titleSlug; }
        public String getDifficulty() { return difficulty; }
        public double getAcceptanceRate() { return acceptanceRate; }
        public List<String> getCompanies() { return companies; }
        public String getCompany() { return companies.isEmpty() ? "" : companies.get(0); } // For backward compatibility
        public List<String> getTopics() { return topics; }
    }
    
    // Company Problems Adapter
    private static class CompanyProblemsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int VIEW_TYPE_PROBLEM = 0;
        private static final int VIEW_TYPE_LOADING = 1;
        
        private List<CompanyProblem> problems;
        private boolean isLoading = false;
        
        public CompanyProblemsAdapter(List<CompanyProblem> problems) {
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
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_company_problem, parent, false);
                return new CompanyProblemViewHolder(view);
            }
        }
        
        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof CompanyProblemViewHolder) {
                CompanyProblem problem = problems.get(position);
                ((CompanyProblemViewHolder) holder).bind(problem);
            }
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
        
        static class CompanyProblemViewHolder extends RecyclerView.ViewHolder {
            private TextView problemNumber;
            private TextView problemTitle;
            private TextView difficultyBadge;
            private TextView acceptanceRate;
            private TextView companyTag;
            private RecyclerView topicTagsRecyclerView;
            
            public CompanyProblemViewHolder(@NonNull View itemView) {
                super(itemView);
                problemNumber = itemView.findViewById(R.id.problemNumber);
                problemTitle = itemView.findViewById(R.id.problemTitle);
                difficultyBadge = itemView.findViewById(R.id.difficultyBadge);
                acceptanceRate = itemView.findViewById(R.id.acceptanceRate);
                companyTag = itemView.findViewById(R.id.companyTag);
                topicTagsRecyclerView = itemView.findViewById(R.id.topicTagsRecyclerView);
            }
            
            public void bind(CompanyProblem problem) {
                problemNumber.setText(problem.getId() + ".");
                problemTitle.setText(problem.getTitle());
                acceptanceRate.setText(String.format("%.1f%%", problem.getAcceptanceRate()));
                
                // Display multiple companies
                List<String> companies = problem.getCompanies();
                if (companies.size() == 1) {
                    companyTag.setText(companies.get(0));
                } else if (companies.size() <= 3) {
                    // Show up to 3 companies
                    companyTag.setText(String.join(", ", companies));
                } else {
                    // Show first 2 companies and count
                    String companiesText = companies.get(0) + ", " + companies.get(1) + " +" + (companies.size() - 2) + " more";
                    companyTag.setText(companiesText);
                }
                
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
                    intent.putExtra("problem_companies", problem.getCompany());
                    context.startActivity(intent);
                });
            }
        }
    }
    
    // Topic Tags Adapter (reused from ProblemsActivity)
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
    
    private void applyTheme() {
        // Get root view
        View rootView = findViewById(android.R.id.content);
        
        if (isDarkTheme) {
            // Apply dark theme colors
            rootView.setBackgroundColor(getResources().getColor(R.color.leetcode_dark_bg, getTheme()));
            
            // Update bottom navigation card
            if (bottomNavCard != null) {
                bottomNavCard.setCardBackgroundColor(getResources().getColor(R.color.leetcode_card_bg, getTheme()));
            }
            
        } else {
            // Apply light theme colors  
            rootView.setBackgroundColor(getResources().getColor(R.color.background_primary, getTheme()));
            
            // Update bottom navigation card
            if (bottomNavCard != null) {
                        bottomNavCard.setCardBackgroundColor(getResources().getColor(R.color.surface_primary, getTheme()));
            }
        }
        
        // Refresh RecyclerView to apply theme to items
        if (problemsRecyclerView != null && problemsRecyclerView.getAdapter() != null) {
            problemsRecyclerView.getAdapter().notifyDataSetChanged();
        }
    }
    
    @Override
    public void onBackPressed() {
        // When back is pressed, return to home and ensure home nav is selected
        Intent intent = new Intent(CompanyProblemsActivity.this, ModernMainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("navigate_to_home", true);
        startActivity(intent);
        finish();
    }
}
