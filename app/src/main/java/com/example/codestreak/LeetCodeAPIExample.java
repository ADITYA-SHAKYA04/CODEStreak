package com.example.codestreak;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * LeetCodeAPIExample - Comprehensive guide on how to use LeetCode API
 * 
 * This class demonstrates various ways to fetch and use data from LeetCode's GraphQL API
 */
public class LeetCodeAPIExample {
    
    private LeetCodeAPI leetCodeAPI;
    
    public LeetCodeAPIExample() {
        leetCodeAPI = new LeetCodeAPI();
    }
    
    /**
     * Example 1: Get User Profile and Statistics
     * This fetches a user's solved problems count by difficulty
     */
    public void getUserStatsExample(String username) {
        leetCodeAPI.getUserProfile(username, new LeetCodeAPI.LeetCodeCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    JSONObject data = jsonResponse.getJSONObject("data");
                    JSONObject matchedUser = data.getJSONObject("matchedUser");
                    JSONObject submitStats = matchedUser.getJSONObject("submitStats");
                    JSONArray acSubmissionNum = submitStats.getJSONArray("acSubmissionNum");
                    
                    System.out.println("=== User Statistics for " + username + " ===");
                    
                    for (int i = 0; i < acSubmissionNum.length(); i++) {
                        JSONObject submission = acSubmissionNum.getJSONObject(i);
                        String difficulty = submission.getString("difficulty");
                        int count = submission.getInt("count");
                        
                        System.out.println(difficulty + " problems solved: " + count);
                    }
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            @Override
            public void onError(Exception error) {
                System.out.println("Error: " + error.getMessage());
            }
        });
    }
    
    /**
     * Example 2: Get Daily Coding Challenge
     */
    public void getDailyChallengeExample() {
        leetCodeAPI.getDailyCodingChallenge(new LeetCodeAPI.LeetCodeCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    JSONObject data = jsonResponse.getJSONObject("data");
                    JSONObject challenge = data.getJSONObject("activeDailyCodingChallengeQuestion");
                    JSONObject question = challenge.getJSONObject("question");
                    
                    String title = question.getString("title");
                    String difficulty = question.getString("difficulty");
                    String titleSlug = question.getString("titleSlug");
                    double acRate = question.getDouble("acRate");
                    
                    System.out.println("=== Today's Daily Challenge ===");
                    System.out.println("Title: " + title);
                    System.out.println("Difficulty: " + difficulty);
                    System.out.println("Acceptance Rate: " + String.format("%.1f%%", acRate));
                    System.out.println("URL: https://leetcode.com/problems/" + titleSlug + "/");
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            @Override
            public void onError(Exception error) {
                System.out.println("Error fetching daily challenge: " + error.getMessage());
            }
        });
    }
    
    /**
     * Example 3: Get Problems by Difficulty
     */
    public void getEasyProblemsExample() {
        leetCodeAPI.getProblemsByDifficulty("Easy", new LeetCodeAPI.LeetCodeCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    JSONObject data = jsonResponse.getJSONObject("data");
                    JSONObject problemsetQuestionList = data.getJSONObject("problemsetQuestionList");
                    JSONArray questions = problemsetQuestionList.getJSONArray("questions");
                    
                    System.out.println("=== Easy Problems (Free) ===");
                    
                    for (int i = 0; i < questions.length(); i++) {
                        JSONObject question = questions.getJSONObject(i);
                        String title = question.getString("title");
                        String titleSlug = question.getString("titleSlug");
                        boolean isPaidOnly = question.getBoolean("paidOnly");
                        String frontendQuestionId = question.getString("frontendQuestionId");
                        
                        // Only show free problems
                        if (!isPaidOnly) {
                            System.out.println(frontendQuestionId + ". " + title);
                            System.out.println("   URL: https://leetcode.com/problems/" + titleSlug + "/");
                        }
                    }
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            @Override
            public void onError(Exception error) {
                System.out.println("Error: " + error.getMessage());
            }
        });
    }
    
    /**
     * Example 4: Get Random Problem for Daily Practice
     */
    public void getRandomProblemExample() {
        leetCodeAPI.getRandomProblem("Medium", new LeetCodeAPI.LeetCodeCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    JSONObject data = jsonResponse.getJSONObject("data");
                    JSONObject problemsetQuestionList = data.getJSONObject("problemsetQuestionList");
                    JSONArray questions = problemsetQuestionList.getJSONArray("questions");
                    
                    if (questions.length() > 0) {
                        JSONObject question = questions.getJSONObject(0);
                        String title = question.getString("title");
                        String titleSlug = question.getString("titleSlug");
                        String difficulty = question.getString("difficulty");
                        double acRate = question.getDouble("acRate");
                        
                        System.out.println("=== Random Problem for Practice ===");
                        System.out.println("Title: " + title);
                        System.out.println("Difficulty: " + difficulty);
                        System.out.println("Acceptance Rate: " + String.format("%.1f%%", acRate));
                        System.out.println("URL: https://leetcode.com/problems/" + titleSlug + "/");
                        
                        // Get topic tags
                        JSONArray topicTags = question.getJSONArray("topicTags");
                        System.out.print("Topics: ");
                        for (int i = 0; i < topicTags.length(); i++) {
                            JSONObject tag = topicTags.getJSONObject(i);
                            System.out.print(tag.getString("name"));
                            if (i < topicTags.length() - 1) System.out.print(", ");
                        }
                        System.out.println();
                    }
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            @Override
            public void onError(Exception error) {
                System.out.println("Error: " + error.getMessage());
            }
        });
    }
    
    /**
     * Example 5: Get Problems by Topic
     */
    public void getArrayProblemsExample() {
        leetCodeAPI.getProblemsByTopic("array", 10, new LeetCodeAPI.LeetCodeCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    JSONObject data = jsonResponse.getJSONObject("data");
                    JSONObject problemsetQuestionList = data.getJSONObject("problemsetQuestionList");
                    JSONArray questions = problemsetQuestionList.getJSONArray("questions");
                    
                    System.out.println("=== Array Problems ===");
                    
                    for (int i = 0; i < questions.length(); i++) {
                        JSONObject question = questions.getJSONObject(i);
                        String title = question.getString("title");
                        String difficulty = question.getString("difficulty");
                        String titleSlug = question.getString("titleSlug");
                        boolean isPaidOnly = question.getBoolean("paidOnly");
                        
                        if (!isPaidOnly) {
                            System.out.println(title + " (" + difficulty + ")");
                            System.out.println("   https://leetcode.com/problems/" + titleSlug + "/");
                        }
                    }
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            @Override
            public void onError(Exception error) {
                System.out.println("Error: " + error.getMessage());
            }
        });
    }
    
    // How to use these examples in your MainActivity:
    /*
    
    // In onCreate() or wherever you want to fetch data:
    LeetCodeAPIExample apiExample = new LeetCodeAPIExample();
    
    // Get user statistics
    apiExample.getUserStatsExample("your-leetcode-username");
    
    // Get today's daily challenge
    apiExample.getDailyChallengeExample();
    
    // Get easy problems for beginners
    apiExample.getEasyProblemsExample();
    
    // Get a random medium problem
    apiExample.getRandomProblemExample();
    
    // Get array-related problems
    apiExample.getArrayProblemsExample();
    
    */
}
