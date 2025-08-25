package com.example.codestreak;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Calendar;

public class EnhancedContributionAdapter extends RecyclerView.Adapter<EnhancedContributionAdapter.ContributionViewHolder> {
    
    private ArrayList<Integer> contributionData;
    private int[] contributionColors;
    private Calendar monthCalendar;
    
    public EnhancedContributionAdapter(ArrayList<Integer> contributionData, Calendar monthCalendar) {
        this.contributionData = contributionData;
        this.monthCalendar = (Calendar) monthCalendar.clone();
        this.contributionColors = new int[5];
    }
    
    @NonNull
    @Override
    public ContributionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contribution_enhanced, parent, false);
        
        // Initialize contribution colors - use fallback colors if resources fail
        try {
            contributionColors[0] = parent.getContext().getColor(R.color.contribution_level_0);
            contributionColors[1] = parent.getContext().getColor(R.color.contribution_level_1);
            contributionColors[2] = parent.getContext().getColor(R.color.contribution_level_2);
            contributionColors[3] = parent.getContext().getColor(R.color.contribution_level_3);
            contributionColors[4] = parent.getContext().getColor(R.color.contribution_level_4);
            System.out.println("DEBUG: Loaded colors from resources successfully");
        } catch (Exception e) {
            System.out.println("DEBUG: Failed to load color resources, using fallback colors");
        }
        
        // FORCE hardcoded colors to ensure they work
        contributionColors[0] = Color.parseColor("#161B22"); // Dark gray
        contributionColors[1] = Color.parseColor("#0E4429"); // Light green
        contributionColors[2] = Color.parseColor("#006D32"); // Medium green
        contributionColors[3] = Color.parseColor("#26A641"); // Bright green
        contributionColors[4] = Color.parseColor("#39D353"); // Brightest green
        System.out.println("DEBUG: Forced hardcoded colors");
        
        // Debug all loaded colors
        for (int i = 0; i < contributionColors.length; i++) {
            System.out.println("DEBUG: Level " + i + " color: " + Integer.toHexString(contributionColors[i]));
        }
        
        return new ContributionViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ContributionViewHolder holder, int position) {
        int problems = contributionData.get(position);
        
        // Debug every cell to see what data we're getting
        if (position < 5 || problems > 0) {
            System.out.println("DEBUG: Position " + position + " has " + problems + " problems");
        }
        
        if (problems == -1) {
            // Empty cell (days not in current month)
            holder.contributionSquare.setVisibility(View.INVISIBLE);
            holder.dayNumber.setVisibility(View.INVISIBLE);
        } else {
            holder.contributionSquare.setVisibility(View.VISIBLE);
            holder.dayNumber.setVisibility(View.VISIBLE);
            
            int level = getContributionLevel(problems);
            
            // Debug color application for every cell
            System.out.println("DEBUG: Position " + position + " -> Level " + level);
            
            // USE DRAWABLE RESOURCES WITH ROUNDED CORNERS
            int drawableRes;
            switch (level) {
                case 0:
                    drawableRes = R.drawable.contribution_level_0;
                    break;
                case 1:
                    drawableRes = R.drawable.contribution_level_1;
                    break;
                case 2:
                    drawableRes = R.drawable.contribution_level_2;
                    break;
                case 3:
                    drawableRes = R.drawable.contribution_level_3;
                    break;
                case 4:
                default:
                    drawableRes = R.drawable.contribution_level_4;
                    break;
            }
            
            holder.contributionSquare.setBackgroundResource(drawableRes);
            System.out.println("DEBUG: Applied drawable resource: " + drawableRes);
            
            // Calculate and set day number
            int dayNumber = getDayNumberForPosition(position);
            if (dayNumber > 0) {
                holder.dayNumber.setText(String.valueOf(dayNumber));
                // Adjust text color based on background
                if (level == 0) {
                    holder.dayNumber.setTextColor(Color.parseColor("#A0A0A0")); // Gray text for dark bg
                } else {
                    holder.dayNumber.setTextColor(Color.WHITE); // White text for green bg
                }
            } else {
                holder.dayNumber.setText("");
            }
        }
    }
    
    @Override
    public int getItemCount() {
        return contributionData.size();
    }
    
    public void updateData(ArrayList<Integer> newData, Calendar newMonthCalendar) {
        this.contributionData = newData;
        this.monthCalendar = (Calendar) newMonthCalendar.clone();
        notifyDataSetChanged();
    }
    
    private int getDayNumberForPosition(int position) {
        Calendar cal = (Calendar) monthCalendar.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        
        // Calculate day number based on position
        int dayNumber = position - firstDayOfWeek + 2;
        
        if (dayNumber > 0 && dayNumber <= daysInMonth) {
            return dayNumber;
        }
        
        return -1; // Invalid day
    }
    
    private int getContributionLevel(int problems) {
        if (problems == 0) return 0;      // Dark gray background (still visible)
        if (problems <= 2) return 1;     // Light green
        if (problems <= 4) return 2;     // Medium green  
        if (problems <= 6) return 3;     // Bright green
        return 4;                        // Brightest green
    }
    
    static class ContributionViewHolder extends RecyclerView.ViewHolder {
        View contributionSquare;
        TextView dayNumber;
        
        public ContributionViewHolder(@NonNull View itemView) {
            super(itemView);
            contributionSquare = itemView.findViewById(R.id.contributionSquare);
            dayNumber = itemView.findViewById(R.id.dayNumber);
        }
    }
}
