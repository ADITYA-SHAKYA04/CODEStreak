package com.example.codestreak;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class UltraSimpleAdapter extends RecyclerView.Adapter<UltraSimpleAdapter.SimpleViewHolder> {
    
    private List<Integer> data;
    private Calendar currentCalendar;
    private OnDayClickListener onDayClickListener;
    
    // Interface for day click events
    public interface OnDayClickListener {
        void onDayClicked(int dayNumber, int problemCount, View clickedView);
    }

    public UltraSimpleAdapter() {
        this.data = new ArrayList<>();
        // No hardcoded pattern - only show colors for real submissions
    }
    
    public UltraSimpleAdapter(ArrayList<Integer> realData, Calendar calendar) {
        this.data = realData != null ? realData : new ArrayList<>();
        this.currentCalendar = calendar;
        
        // Don't create hardcoded patterns - only show real data
        System.out.println("ULTRA: Constructor with " + this.data.size() + " data points");
    }
    
    public void setOnDayClickListener(OnDayClickListener listener) {
        this.onDayClickListener = listener;
    }
    
    // No hardcoded patterns - removed createHardcodedPattern method

    static class SimpleViewHolder extends RecyclerView.ViewHolder {
        View square;
        TextView dayNumber;

        SimpleViewHolder(@NonNull View itemView) {
            super(itemView);
            square = itemView.findViewById(R.id.contributionSquare);
            dayNumber = itemView.findViewById(R.id.dayNumber);
        }
    }

    @NonNull
    @Override
    public SimpleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contribution_enhanced, parent, false);
        return new SimpleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SimpleViewHolder holder, int position) {
        int problems = position < data.size() ? data.get(position) : 0;
        
        // Handle empty cells
        if (problems == -1) {
            holder.square.setVisibility(View.INVISIBLE);
            holder.dayNumber.setVisibility(View.INVISIBLE);
            holder.square.setOnClickListener(null); // Remove click listener for empty cells
            return;
        }
        
        holder.square.setVisibility(View.VISIBLE);
        holder.dayNumber.setVisibility(View.VISIBLE);
        
        int level = getContributionLevel(problems);
        
        // SUPER EXPLICIT COLOR APPLICATION - THIS WORKS!
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
        
        holder.square.setBackgroundColor(color);
        
        // Show day number or level for debugging
        int dayNumber = getDayNumberForPosition(position);
        if (dayNumber > 0) {
            holder.dayNumber.setText(String.valueOf(dayNumber));
        } else {
            holder.dayNumber.setText(String.valueOf(level)); // Show level for debugging
        }
        holder.dayNumber.setTextColor(Color.WHITE);
        
        // Add click listener for squares with activity (level > 0)
        if (level > 0 && onDayClickListener != null && dayNumber > 0) {
            holder.square.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onDayClickListener.onDayClicked(dayNumber, problems, v);
                }
            });
        } else {
            holder.square.setOnClickListener(null); // Remove click listener for inactive days
        }
        
        System.out.println("ULTRA DEBUG: Position " + position + " -> Day " + dayNumber + " -> " + problems + " problems -> Level " + level + " -> Color: " + Integer.toHexString(color));
    }

    @Override
    public int getItemCount() {
        return Math.max(42, data.size());
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
    
    public void updateData(List<Integer> newData) {
        this.data = newData;
        
        // Count active cells (real submissions)
        int activeCount = 0;
        for (Integer value : newData) {
            if (value != null && value > 0) {
                activeCount++;
            }
        }
        
        System.out.println("ULTRA_ADAPTER: Received " + newData.size() + " data points with " + activeCount + " active days");
        
        // No need to enhance with patterns - we only want real data to be colored
        
        notifyDataSetChanged();
    }
}
