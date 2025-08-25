package com.example.codestreak;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class SimpleTestAdapter extends RecyclerView.Adapter<SimpleTestAdapter.TestViewHolder> {
    
    private ArrayList<Integer> testData;
    private int[] testColors;
    
    public SimpleTestAdapter() {
        // Create simple test data
        this.testData = new ArrayList<>();
        for (int i = 0; i < 42; i++) {
            if (i % 7 == 0) testData.add(1); // Every 7th cell has activity
            else if (i % 10 == 0) testData.add(3); // Every 10th cell has more activity
            else testData.add(0); // Others have no activity
        }
        
        // Simple hardcoded colors
        this.testColors = new int[5];
        testColors[0] = Color.parseColor("#161B22"); // Dark
        testColors[1] = Color.parseColor("#0E4429"); // Light green
        testColors[2] = Color.parseColor("#006D32"); // Medium green
        testColors[3] = Color.parseColor("#26A641"); // Bright green
        testColors[4] = Color.parseColor("#39D353"); // Brightest green
        
        System.out.println("DEBUG: SimpleTestAdapter created with hardcoded colors");
    }
    
    @NonNull
    @Override
    public TestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contribution_enhanced, parent, false);
        return new TestViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull TestViewHolder holder, int position) {
        int problems = testData.get(position);
        int level = problems == 0 ? 0 : Math.min(problems, 4);
        int color = testColors[level];
        
        System.out.println("DEBUG: SimpleTest Position " + position + " -> Problems: " + problems + " -> Level: " + level + " -> Color: " + Integer.toHexString(color));
        
        // Force visibility
        holder.contributionSquare.setVisibility(View.VISIBLE);
        
        // Set background color directly
        holder.contributionSquare.setBackgroundColor(color);
        
        // Set day number
        if (holder.dayNumber != null) {
            holder.dayNumber.setText(String.valueOf((position % 31) + 1));
            holder.dayNumber.setTextColor(level == 0 ? Color.GRAY : Color.WHITE);
        }
    }
    
    @Override
    public int getItemCount() {
        return testData.size();
    }
    
    static class TestViewHolder extends RecyclerView.ViewHolder {
        View contributionSquare;
        android.widget.TextView dayNumber;
        
        public TestViewHolder(@NonNull View itemView) {
            super(itemView);
            contributionSquare = itemView.findViewById(R.id.contributionSquare);
            dayNumber = itemView.findViewById(R.id.dayNumber);
        }
    }
}
