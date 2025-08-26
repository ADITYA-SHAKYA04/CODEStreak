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
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.util.*;

public class ProblemsActivity extends AppCompatActivity {
    
    private RecyclerView problemsRecyclerView;
    private RecyclerView topicsRecyclerView;
    private EditText searchEditText;
    private ChipGroup topicsChipGroup;
    private TextView problemCountText;
    
    private ProblemsAdapter problemsAdapter;
    private TopicsAdapter topicsAdapter;
    
    private List<Problem> allProblems;
    private List<Problem> filteredProblems;
    private List<Topic> topics;
    private Set<String> selectedTopics = new HashSet<>();
    
    // Infinite scrolling variables
    private boolean isLoading = false;
    private int currentPage = 1;
    private final int PROBLEMS_PER_PAGE = 20;
    private final int TOTAL_PROBLEMS = 3000; // LeetCode has ~3000 problems
    private LinearLayoutManager layoutManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_problems);
        
        initViews();
        setupRecyclerViews();
        loadData();
        setupSearch();
    }
    
    private void initViews() {
        problemsRecyclerView = findViewById(R.id.problemsRecyclerView);
        topicsRecyclerView = findViewById(R.id.topicsRecyclerView);
        searchEditText = findViewById(R.id.searchEditText);
        topicsChipGroup = findViewById(R.id.topicsChipGroup);
        problemCountText = findViewById(R.id.problemCountText);
        
        // Setup toolbar
        ImageView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
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
        
        // Topics RecyclerView (horizontal)
        LinearLayoutManager topicsLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        topicsRecyclerView.setLayoutManager(topicsLayoutManager);
    }
    
    private void loadData() {
        // Initialize topics with LeetCode data
        topics = Arrays.asList(
            new Topic("Array", 1732, true),
            new Topic("String", 716, false),
            new Topic("Hash Table", 599, false),
            new Topic("Dynamic Programming", 473, false),
            new Topic("Math", 444, false),
            new Topic("Sorting", 380, false),
            new Topic("Greedy", 378, false),
            new Topic("Depth-First Search", 372, false),
            new Topic("Binary Search", 365, false),
            new Topic("Tree", 362, false),
            new Topic("Breadth-First Search", 313, false),
            new Topic("Two Pointers", 191, false),
            new Topic("Bit Manipulation", 179, false),
            new Topic("Stack", 177, false),
            new Topic("Design", 174, false),
            new Topic("Heap (Priority Queue)", 164, false),
            new Topic("Graph", 160, false),
            new Topic("Prefix Sum", 157, false),
            new Topic("Simulation", 154, false),
            new Topic("Counting", 150, false),
            new Topic("Sliding Window", 142, false),
            new Topic("Union Find", 129, false),
            new Topic("Linked List", 127, false),
            new Topic("Monotonic Stack", 124, false),
            new Topic("Binary Tree", 117, false)
        );
        
        // Initialize empty lists
        allProblems = new ArrayList<>();
        filteredProblems = new ArrayList<>();
        
        // Setup adapters first
        topicsAdapter = new TopicsAdapter(topics, this::onTopicSelected);
        topicsRecyclerView.setAdapter(topicsAdapter);
        
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
    }
    
    private void filterProblems(String searchQuery) {
        filteredProblems.clear();
        
        for (Problem problem : allProblems) {
            boolean matchesSearch = searchQuery.isEmpty() || 
                problem.getTitle().toLowerCase().contains(searchQuery.toLowerCase()) ||
                String.valueOf(problem.getId()).contains(searchQuery);
            
            boolean matchesTopics = selectedTopics.isEmpty() ||
                problem.getTopics().stream().anyMatch(selectedTopics::contains);
            
            if (matchesSearch && matchesTopics) {
                filteredProblems.add(problem);
            }
        }
        
        problemsAdapter.notifyDataSetChanged();
        
        // Reset scroll position to top when filtering
        if (!searchQuery.isEmpty() || !selectedTopics.isEmpty()) {
            problemsRecyclerView.scrollToPosition(0);
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
                
                if (matchesSearch && matchesTopics) {
                    newFilteredProblems.add(problem);
                }
            }
            
            filteredProblems.addAll(newFilteredProblems);
            
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
            
            // Generate 1-3 random topics
            List<String> problemTopics = new ArrayList<>();
            int topicCount = 1 + (problemId % 3);
            for (int j = 0; j < topicCount; j++) {
                String topic = allTopics.get((problemId + j) % allTopics.size());
                if (!problemTopics.contains(topic)) {
                    problemTopics.add(topic);
                }
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
    
    // Topics Adapter
    private static class TopicsAdapter extends RecyclerView.Adapter<TopicsAdapter.TopicViewHolder> {
        private List<Topic> topics;
        private TopicSelectionListener listener;
        
        interface TopicSelectionListener {
            void onTopicSelected(String topicName, boolean isSelected);
        }
        
        public TopicsAdapter(List<Topic> topics, TopicSelectionListener listener) {
            this.topics = topics;
            this.listener = listener;
        }
        
        @NonNull
        @Override
        public TopicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Chip chip = new Chip(parent.getContext());
            chip.setLayoutParams(new ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) chip.getLayoutParams();
            params.rightMargin = 16;
            return new TopicViewHolder(chip);
        }
        
        @Override
        public void onBindViewHolder(@NonNull TopicViewHolder holder, int position) {
            Topic topic = topics.get(position);
            holder.bind(topic, listener);
        }
        
        @Override
        public int getItemCount() {
            return topics.size();
        }
        
        static class TopicViewHolder extends RecyclerView.ViewHolder {
            private Chip chip;
            
            public TopicViewHolder(@NonNull View itemView) {
                super(itemView);
                chip = (Chip) itemView;
            }
            
            public void bind(Topic topic, TopicSelectionListener listener) {
                chip.setText(topic.getName() + " (" + topic.getCount() + ")");
                chip.setCheckable(true);
                chip.setChecked(topic.isSelected());
                
                // Update visual state
                Context context = chip.getContext();
                if (topic.isSelected()) {
                    chip.setChipBackgroundColor(ContextCompat.getColorStateList(context, R.color.accent_primary));
                    chip.setTextColor(Color.WHITE);
                } else {
                    chip.setChipBackgroundColor(ContextCompat.getColorStateList(context, R.color.background_secondary));
                    chip.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
                }
                
                chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    topic.setSelected(isChecked);
                    if (listener != null) {
                        listener.onTopicSelected(topic.getName(), isChecked);
                    }
                    
                    // Update visual state
                    if (isChecked) {
                        chip.setChipBackgroundColor(ContextCompat.getColorStateList(context, R.color.accent_primary));
                        chip.setTextColor(Color.WHITE);
                    } else {
                        chip.setChipBackgroundColor(ContextCompat.getColorStateList(context, R.color.background_secondary));
                        chip.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
                    }
                });
            }
        }
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
}
