package com.example.codestreak;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class RevisionActivity extends AppCompatActivity {
    
    private RecyclerView recyclerView;
    private LinearLayout emptyStateLayout;
    private TextView problemCountText;
    private ImageView backButton;
    private ProblemsActivity.ProblemsAdapter adapter;
    private List<ProblemsActivity.Problem> starredProblems;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_revision);
        
        initializeViews();
        loadStarredProblems();
        setupRecyclerView();
        updateProblemCount();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the list when returning from problem detail
        loadStarredProblems();
        adapter.notifyDataSetChanged();
        updateProblemCount();
    }
    
    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerView);
        emptyStateLayout = findViewById(R.id.emptyStateText);
        problemCountText = findViewById(R.id.problemCountText);
        backButton = findViewById(R.id.backButton);
        
        backButton.setOnClickListener(v -> finish());
    }
    
    private void loadStarredProblems() {
        starredProblems = new ArrayList<>();
        SharedPreferences prefs = getSharedPreferences("CodeStreakPrefs", MODE_PRIVATE);
        
        String starredList = prefs.getString("starred_problems_list", "");
        
        if (!starredList.isEmpty()) {
            String[] problemIds = starredList.split(",");
            
            for (String idStr : problemIds) {
                try {
                    int problemId = Integer.parseInt(idStr.trim());
                    
                    // Retrieve problem details from SharedPreferences
                    String title = prefs.getString("starred_title_" + problemId, "Unknown Problem");
                    String difficulty = prefs.getString("starred_difficulty_" + problemId, "Medium");
                    float acceptance = prefs.getFloat("starred_acceptance_" + problemId, 0.0f);
                    String titleSlug = prefs.getString("starred_slug_" + problemId, "");
                    String companies = prefs.getString("starred_companies_" + problemId, "");
                    String topicsStr = prefs.getString("starred_topics_" + problemId, "");
                    
                    // Parse topics from comma-separated string
                    List<String> topics = new ArrayList<>();
                    if (!topicsStr.isEmpty()) {
                        String[] topicsArray = topicsStr.split(",");
                        for (String topic : topicsArray) {
                            topics.add(topic.trim());
                        }
                    }
                    
                    // Create Problem object with titleSlug constructor
                    ProblemsActivity.Problem problem = new ProblemsActivity.Problem(
                        problemId,
                        title,
                        titleSlug,
                        difficulty,
                        acceptance,
                        companies,
                        topics
                    );
                    
                    starredProblems.add(problem);
                } catch (NumberFormatException e) {
                    android.util.Log.e("RevisionActivity", "Invalid problem ID: " + idStr);
                }
            }
        }
        
        // Show or hide empty state
        if (starredProblems.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
        }
    }
    
    private void setupRecyclerView() {
        adapter = new ProblemsActivity.ProblemsAdapter(starredProblems);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }
    
    private void updateProblemCount() {
        int count = starredProblems.size();
        if (count == 0) {
            problemCountText.setText("No problems in revision");
        } else if (count == 1) {
            problemCountText.setText("1 problem in revision");
        } else {
            problemCountText.setText(count + " problems in revision");
        }
    }
}
