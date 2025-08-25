package com.example.codestreak;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class ContributionAdapter extends RecyclerView.Adapter<ContributionAdapter.ContributionViewHolder> {
    
    private ArrayList<Integer> contributionData;
    private int[] contributionColors;
    
    public ContributionAdapter(ArrayList<Integer> contributionData) {
        this.contributionData = contributionData;
        this.contributionColors = new int[5];
    }
    
    @NonNull
    @Override
    public ContributionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contribution, parent, false);
        
        // Initialize contribution colors
        contributionColors[0] = parent.getContext().getColor(R.color.contribution_level_0);
        contributionColors[1] = parent.getContext().getColor(R.color.contribution_level_1);
        contributionColors[2] = parent.getContext().getColor(R.color.contribution_level_2);
        contributionColors[3] = parent.getContext().getColor(R.color.contribution_level_3);
        contributionColors[4] = parent.getContext().getColor(R.color.contribution_level_4);
        
        return new ContributionViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ContributionViewHolder holder, int position) {
        int problems = contributionData.get(position);
        
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(4f);
        
        if (problems == -1) {
            // Empty cell (days not in current month)
            drawable.setColor(android.graphics.Color.TRANSPARENT);
            holder.contributionSquare.setVisibility(View.INVISIBLE);
        } else {
            holder.contributionSquare.setVisibility(View.VISIBLE);
            int level = getContributionLevel(problems);
            drawable.setColor(contributionColors[level]);
        }
        
        holder.contributionSquare.setBackground(drawable);
    }
    
    @Override
    public int getItemCount() {
        return contributionData.size();
    }
    
    public void updateData(ArrayList<Integer> newData) {
        this.contributionData = newData;
        notifyDataSetChanged();
    }
    
    private int getContributionLevel(int problems) {
        if (problems == 0) return 0;
        if (problems <= 2) return 1;
        if (problems <= 4) return 2;
        if (problems <= 6) return 3;
        return 4;
    }
    
    static class ContributionViewHolder extends RecyclerView.ViewHolder {
        View contributionSquare;
        
        public ContributionViewHolder(@NonNull View itemView) {
            super(itemView);
            contributionSquare = itemView.findViewById(R.id.contributionSquare);
        }
    }
}
