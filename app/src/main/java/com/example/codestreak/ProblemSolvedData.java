package com.example.codestreak;

import java.util.Date;

public class ProblemSolvedData {
    private Date date;
    private int easyCount;
    private int mediumCount;
    private int hardCount;
    private int totalCount;
    
    public ProblemSolvedData(Date date, int easyCount, int mediumCount, int hardCount) {
        this.date = date;
        this.easyCount = easyCount;
        this.mediumCount = mediumCount;
        this.hardCount = hardCount;
        this.totalCount = easyCount + mediumCount + hardCount;
    }
    
    // Getters
    public Date getDate() { return date; }
    public int getEasyCount() { return easyCount; }
    public int getMediumCount() { return mediumCount; }
    public int getHardCount() { return hardCount; }
    public int getTotalCount() { return totalCount; }
    
    // Setters
    public void setDate(Date date) { this.date = date; }
    public void setEasyCount(int easyCount) { 
        this.easyCount = easyCount; 
        updateTotalCount();
    }
    public void setMediumCount(int mediumCount) { 
        this.mediumCount = mediumCount; 
        updateTotalCount();
    }
    public void setHardCount(int hardCount) { 
        this.hardCount = hardCount; 
        updateTotalCount();
    }
    
    private void updateTotalCount() {
        this.totalCount = this.easyCount + this.mediumCount + this.hardCount;
    }
}
