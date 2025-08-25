package com.example.codestreak;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Calendar;

public class WorkingContributionAdapter extends RecyclerView.Adapter<WorkingContributionAdapter.ViewHolder> {
    
    private ArrayList<Integer> contributionData;
    private Calendar currentCalendar;

    public WorkingContributionAdapter(ArrayList<Integer> data, Calendar calendar) {
        this.contributionData = data != null ? data : new ArrayList<>();
        this.currentCalendar = calendar;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View contributionSquare;
        TextView dayNumber;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            contributionSquare = itemView.findViewById(R.id.contributionSquare);
            dayNumber = itemView.findViewById(R.id.dayNumber);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contribution_enhanced, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int problems = position < contributionData.size() ? contributionData.get(position) : 0;
        
        if (problems == -1) {
            // Empty cell (days not in current month)
            holder.contributionSquare.setVisibility(View.INVISIBLE);
            holder.dayNumber.setVisibility(View.INVISIBLE);
        } else {
            holder.contributionSquare.setVisibility(View.VISIBLE);
            holder.dayNumber.setVisibility(View.VISIBLE);
            
            int level = getContributionLevel(problems);
            
            // USE THE WORKING COLOR APPROACH - EXPLICIT HEX COLORS
            int color;
            switch (level) {
                case 0:
                    color = 0xFF161B22; // Dark gray
                    break;
                case 1:
                    color = 0xFF0E4429; // Dark green
                    break;
                case 2:
                    color = 0xFF006D32; // Medium green
                    break;
                case 3:
                    color = 0xFF26A641; // Light green
                    break;
                case 4:
                default:
                    color = 0xFF39D353; // Bright green
                    break;
            }
            
            holder.contributionSquare.setBackgroundColor(color);
            
            // Calculate and set day number
            int dayNumber = getDayNumberForPosition(position);
            if (dayNumber > 0) {
                holder.dayNumber.setText(String.valueOf(dayNumber));
                holder.dayNumber.setTextColor(level == 0 ? 0xFFA0A0A0 : 0xFFFFFFFF);
            } else {
                holder.dayNumber.setText("");
            }
            
            System.out.println("WORKING: Position " + position + " -> Day " + dayNumber + " -> " + problems + " problems -> Level " + level + " -> Color: " + Integer.toHexString(color));
        }
    }

    @Override
    public int getItemCount() {
        return Math.max(42, contributionData.size()); // Ensure 6 weeks minimum
    }

    private int getContributionLevel(int problemCount) {
        if (problemCount == 0) return 0;
        if (problemCount <= 2) return 1;
        if (problemCount <= 4) return 2;
        if (problemCount <= 6) return 3;
        return 4;
    }

    private int getDayNumberForPosition(int position) {
        if (currentCalendar == null) return 0;
        
        Calendar cal = (Calendar) currentCalendar.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
        
        if (position < firstDayOfWeek) {
            return 0; // Empty cell before month starts
        }
        
        int dayOfMonth = position - firstDayOfWeek + 1;
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        
        return (dayOfMonth <= daysInMonth) ? dayOfMonth : 0;
    }

    public void updateData(ArrayList<Integer> newData, Calendar newCalendar) {
        this.contributionData = newData != null ? newData : new ArrayList<>();
        this.currentCalendar = newCalendar;
        notifyDataSetChanged();
        System.out.println("WORKING ADAPTER: Updated with " + contributionData.size() + " data points");
    }
}
