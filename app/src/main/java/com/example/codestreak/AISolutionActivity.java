package com.example.codestreak;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import android.widget.TextView;

/**
 * Activity to display AI-generated solutions for LeetCode problems
 */
public class AISolutionActivity extends AppCompatActivity {
    
    public static final String EXTRA_PROBLEM_TITLE = "problem_title";
    public static final String EXTRA_PROBLEM_DESCRIPTION = "problem_description";
    public static final String EXTRA_EXAMPLES = "examples";
    public static final String EXTRA_CONSTRAINTS = "constraints";
    
    private TextView tvProblemTitle;
    private TextView tvLoadingStatus;
    private TextView tvApproach;
    private TextView tvComplexity;
    private TextView tvJavaSolution;
    private TextView tvPythonSolution;
    private TextView tvKeyInsights;
    private TextView tvEdgeCases;
    private TextView tvError;
    
    private MaterialCardView loadingCard;
    private MaterialCardView approachCard;
    private MaterialCardView complexityCard;
    private MaterialCardView javaSolutionCard;
    private MaterialCardView pythonSolutionCard;
    private MaterialCardView insightsCard;
    private MaterialCardView edgeCasesCard;
    private MaterialCardView errorCard;
    
    private MaterialButton btnCopyJava;
    private MaterialButton btnCopyPython;
    private MaterialButton btnRetry;
    
    private AISolutionHelper_backup aiHelper;
    private String problemTitle, problemDescription, examples, constraints;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_solution);
        
        // Get data from intent
        Intent intent = getIntent();
        problemTitle = intent.getStringExtra(EXTRA_PROBLEM_TITLE);
        problemDescription = intent.getStringExtra(EXTRA_PROBLEM_DESCRIPTION);
        examples = intent.getStringExtra(EXTRA_EXAMPLES);
        constraints = intent.getStringExtra(EXTRA_CONSTRAINTS);
        
        initViews();
        setupClickListeners();
        
        // Initialize AI helper
        aiHelper = new AISolutionHelper_backup(this);
        
        // Start generating solution
        generateSolution();
    }
    
    private void initViews() {
        tvProblemTitle = findViewById(R.id.tvProblemTitle);
        tvLoadingStatus = findViewById(R.id.tvLoadingStatus);
        tvApproach = findViewById(R.id.tvApproach);
        tvComplexity = findViewById(R.id.tvComplexity);
        tvJavaSolution = findViewById(R.id.tvJavaSolution);
        tvPythonSolution = findViewById(R.id.tvPythonSolution);
        tvKeyInsights = findViewById(R.id.tvKeyInsights);
        tvEdgeCases = findViewById(R.id.tvEdgeCases);
        tvError = findViewById(R.id.tvError);
        
        loadingCard = findViewById(R.id.loadingCard);
        approachCard = findViewById(R.id.approachCard);
        complexityCard = findViewById(R.id.complexityCard);
        javaSolutionCard = findViewById(R.id.javaSolutionCard);
        pythonSolutionCard = findViewById(R.id.pythonSolutionCard);
        insightsCard = findViewById(R.id.insightsCard);
        edgeCasesCard = findViewById(R.id.edgeCasesCard);
        errorCard = findViewById(R.id.errorCard);
        
        btnCopyJava = findViewById(R.id.btnCopyJava);
        btnCopyPython = findViewById(R.id.btnCopyPython);
        btnRetry = findViewById(R.id.btnRetry);
        
        // Set problem title
        tvProblemTitle.setText(problemTitle);
    }
    
    private void setupClickListeners() {
        btnCopyJava.setOnClickListener(v -> copyToClipboard("Java Solution", tvJavaSolution.getText().toString()));
        btnCopyPython.setOnClickListener(v -> copyToClipboard("Python Solution", tvPythonSolution.getText().toString()));
        btnRetry.setOnClickListener(v -> generateSolution());
    }
    
    private void generateSolution() {
        // Show loading
        showLoading();
        
        aiHelper.generateSolution(problemTitle, problemDescription, examples, constraints, 
            new AISolutionHelper_backup.AISolutionCallback() {
                @Override
                public void onSolutionGenerated(AISolutionHelper_backup.AISolution solution) {
                    runOnUiThread(() -> displaySolution(solution));
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> showError(error));
                }
                
                @Override
                public void onProgress(String status) {
                    runOnUiThread(() -> tvLoadingStatus.setText(status));
                }
            });
    }
    
    private void showLoading() {
        hideAllCards();
        loadingCard.setVisibility(View.VISIBLE);
        tvLoadingStatus.setText("Initializing AI...");
    }
    
    private void displaySolution(AISolutionHelper_backup.AISolution solution) {
        hideAllCards();
        
        if (!solution.isValid()) {
            showError("Generated solution is incomplete. Please try again.");
            return;
        }
        
        // Show approach
        if (solution.approach != null && !solution.approach.isEmpty()) {
            tvApproach.setText(solution.approach);
            approachCard.setVisibility(View.VISIBLE);
        }
        
        // Show complexity
        if (solution.complexity != null && !solution.complexity.isEmpty()) {
            tvComplexity.setText(solution.complexity);
            complexityCard.setVisibility(View.VISIBLE);
        }
        
        // Show Java solution
        if (solution.javaSolution != null && !solution.javaSolution.isEmpty()) {
            tvJavaSolution.setText(solution.javaSolution);
            javaSolutionCard.setVisibility(View.VISIBLE);
        }
        
        // Show Python solution
        if (solution.pythonSolution != null && !solution.pythonSolution.isEmpty()) {
            tvPythonSolution.setText(solution.pythonSolution);
            pythonSolutionCard.setVisibility(View.VISIBLE);
        }
        
        // Show key insights
        if (solution.keyInsights != null && !solution.keyInsights.isEmpty()) {
            tvKeyInsights.setText(solution.keyInsights);
            insightsCard.setVisibility(View.VISIBLE);
        }
        
        // Show edge cases
        if (solution.edgeCases != null && !solution.edgeCases.isEmpty()) {
            tvEdgeCases.setText(solution.edgeCases);
            edgeCasesCard.setVisibility(View.VISIBLE);
        }
    }
    
    private void showError(String error) {
        hideAllCards();
        tvError.setText(error);
        errorCard.setVisibility(View.VISIBLE);
    }
    
    private void hideAllCards() {
        loadingCard.setVisibility(View.GONE);
        approachCard.setVisibility(View.GONE);
        complexityCard.setVisibility(View.GONE);
        javaSolutionCard.setVisibility(View.GONE);
        pythonSolutionCard.setVisibility(View.GONE);
        insightsCard.setVisibility(View.GONE);
        edgeCasesCard.setVisibility(View.GONE);
        errorCard.setVisibility(View.GONE);
    }
    
    private void copyToClipboard(String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, label + " copied to clipboard!", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Static method to start this activity
     */
    public static void start(Context context, String title, String description, String examples, String constraints) {
        Intent intent = new Intent(context, AISolutionActivity.class);
        intent.putExtra(EXTRA_PROBLEM_TITLE, title);
        intent.putExtra(EXTRA_PROBLEM_DESCRIPTION, description);
        intent.putExtra(EXTRA_EXAMPLES, examples);
        intent.putExtra(EXTRA_CONSTRAINTS, constraints);
        context.startActivity(intent);
    }
}
