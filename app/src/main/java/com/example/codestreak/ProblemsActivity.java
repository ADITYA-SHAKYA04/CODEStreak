package com.example.codestreak;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.*;

public class ProblemsActivity extends BaseActivity {
    
    private RecyclerView problemsRecyclerView;
    private EditText searchEditText;
    private ChipGroup topicsChipGroup;
    private TextView problemCountText;
    private TextView selectedTopicsText;
    private LinearLayout filterTopicsButton;
    private LinearLayout sortButton;
    private TextView sortText;
    
    private ProblemsAdapter problemsAdapter;
    
    private List<Problem> allProblems;
    private List<Problem> filteredProblems;
    private List<Topic> topics;
    private Set<String> selectedTopics = new HashSet<>();
    private Set<String> selectedDifficulties = new HashSet<>();
    private String sortOrder = "Default"; // Default, Easy->Hard, Hard->Easy, A-Z, Z-A
    
    // Infinite scrolling variables
    private boolean isLoading = false;
    private int currentPage = 1;
    private final int PROBLEMS_PER_PAGE = 20;
    private final int TOTAL_PROBLEMS = 3000; // LeetCode has ~3000 problems
    private LinearLayoutManager layoutManager;
    private boolean showingAllTopics = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_problems_with_nav);
        
        initViews();
        setupRecyclerViews();
        loadData();
        setupSearch();
    }
    
    private void initViews() {
        problemsRecyclerView = findViewById(R.id.problemsRecyclerView);
        searchEditText = findViewById(R.id.searchEditText);
        topicsChipGroup = findViewById(R.id.topicsChipGroup);
        problemCountText = findViewById(R.id.problemCountText);
        selectedTopicsText = findViewById(R.id.selectedTopicsText);
        filterTopicsButton = findViewById(R.id.filterTopicsButton);
        sortButton = findViewById(R.id.sortButton);
        sortText = findViewById(R.id.sortText);
        
        // Setup filter topics button
        filterTopicsButton.setOnClickListener(v -> showTopicSelectionDialog());
        
        // Setup sort button
        sortButton.setOnClickListener(v -> showSortDialog());
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
                    if (!isLoading && totalItemCount > 0 && lastVisibleItemPosition >= totalItemCount - 2) {
                        android.util.Log.d("InfiniteScroll", "Loading more problems...");
                        loadMoreProblems();
                    }
                }
            }
        });
    }
    
    private void loadData() {
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
        
        // Load first page of problems
        loadMoreProblems();
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
        
        // Simulate API call delay
        problemsRecyclerView.postDelayed(() -> {
            List<Problem> newProblems = generateProblemsForPage(currentPage);
            
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
            
        android.util.Log.d("InfiniteScroll", "Loaded page " + (currentPage-1) + 
            ", total problems: " + allProblems.size() + 
            ", filtered: " + filteredProblems.size() + 
            ", new filtered: " + newFilteredProblems.size());
        }, 800); // 800ms delay to simulate network call
    }
    
    private void updateProblemCount() {
        int loadedCount = allProblems.size();
        String countText = loadedCount + "+ Problems";
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
        String[] problemTitles = {"Two Sum", "Add Two Numbers", "Longest Substring", "Median Arrays", 
                                "Palindromic Substring", "Reverse Integer", "String to Integer", 
                                "Palindrome Number", "Container With Water", "3Sum", "Remove Duplicates",
                                "Valid Parentheses", "Merge Lists", "Remove Element", "Implement strStr",
                                "Search Insert", "Count and Say", "Maximum Subarray", "Length of Last Word",
                                "Plus One", "Add Binary", "Sqrt(x)", "Climbing Stairs", "Remove Duplicates II"};
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
            if (i < problemTitles.length) {
                title = problemTitles[i] + " " + (problemId > 100 ? "Advanced" : "");
            } else {
                title = "Problem " + problemId + " - Algorithm Challenge";
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
        private String difficulty;
        private double acceptanceRate;
        private String companies;
        private List<String> topics;
        
        public Problem(int id, String title, String difficulty, double acceptanceRate, String companies, List<String> topics) {
            this.id = id;
            this.title = title;
            this.difficulty = difficulty;
            this.acceptanceRate = acceptanceRate;
            this.companies = companies;
            this.topics = topics;
        }
        
        // Getters
        public int getId() { return id; }
        public String getTitle() { return title; }
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
}
