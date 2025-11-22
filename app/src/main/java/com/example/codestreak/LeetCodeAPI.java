package com.example.codestreak;

import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.*;

public class LeetCodeAPI {
    
    private static final String LEETCODE_GRAPHQL_URL = "https://leetcode.com/graphql";
    private final OkHttpClient client;
    
    public LeetCodeAPI() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }
    
    public interface LeetCodeCallback {
        void onSuccess(String response);
        void onError(Exception error);
    }
    
    /**
     * Fetch user profile data from LeetCode
     * @param username The LeetCode username
     * @param callback Callback to handle response
     */
    public void getUserProfile(String username, LeetCodeCallback callback) {
        String query = "{\n" +
                "  matchedUser(username: \"" + username + "\") {\n" +
                "    username\n" +
                "    submitStats {\n" +
                "      acSubmissionNum {\n" +
                "        difficulty\n" +
                "        count\n" +
                "      }\n" +
                "    }\n" +
                "    profile {\n" +
                "      ranking\n" +
                "      userAvatar\n" +
                "      realName\n" +
                "    }\n" +
                "    emails {\n" +
                "      email\n" +
                "    }\n" +
                "  }\n" +
                "}";
        
        makeGraphQLRequest(query, callback);
    }
    
    /**
     * Fetch problems by difficulty
     * @param difficulty Easy, Medium, or Hard
     * @param callback Callback to handle response
     */
    public void getProblemsByDifficulty(String difficulty, LeetCodeCallback callback) {
        String query = "{\n" +
                "  problemsetQuestionList: problemsetQuestionList(\n" +
                "    categorySlug: \"\"\n" +
                "    limit: 50\n" +
                "    skip: 0\n" +
                "    filters: {difficulty: \"" + difficulty.toUpperCase() + "\"}\n" +
                "  ) {\n" +
                "    total: totalNum\n" +
                "    questions: data {\n" +
                "      acRate\n" +
                "      difficulty\n" +
                "      freqBar\n" +
                "      frontendQuestionId: questionFrontendId\n" +
                "      isFavor\n" +
                "      paidOnly: isPaidOnly\n" +
                "      status\n" +
                "      title\n" +
                "      titleSlug\n" +
                "      topicTags {\n" +
                "        name\n" +
                "        id\n" +
                "        slug\n" +
                "      }\n" +
                "      hasSolution\n" +
                "      hasVideoSolution\n" +
                "    }\n" +
                "  }\n" +
                "}";
        
        makeGraphQLRequest(query, callback);
    }
    
    /**
     * Get daily coding challenge
     * @param callback Callback to handle response
     */
    public void getDailyCodingChallenge(LeetCodeCallback callback) {
        String query = "{\n" +
                "  activeDailyCodingChallengeQuestion {\n" +
                "    date\n" +
                "    userStatus\n" +
                "    link\n" +
                "    question {\n" +
                "      acRate\n" +
                "      difficulty\n" +
                "      freqBar\n" +
                "      frontendQuestionId: questionFrontendId\n" +
                "      isFavor\n" +
                "      paidOnly: isPaidOnly\n" +
                "      status\n" +
                "      title\n" +
                "      titleSlug\n" +
                "      hasVideoSolution\n" +
                "      hasSolution\n" +
                "      topicTags {\n" +
                "        name\n" +
                "        id\n" +
                "        slug\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
        
        makeGraphQLRequest(query, callback);
    }
    
    /**
     * Get random problem by difficulty for daily practice
     * @param difficulty Easy, Medium, or Hard
     * @param callback Callback to handle response
     */
    public void getRandomProblem(String difficulty, LeetCodeCallback callback) {
        String query = "{\n" +
                "  problemsetQuestionList: problemsetQuestionList(\n" +
                "    categorySlug: \"\"\n" +
                "    limit: 1\n" +
                "    skip: " + (int)(Math.random() * 100) + "\n" +
                "    filters: {difficulty: \"" + difficulty.toUpperCase() + "\", status: \"NOT_STARTED\"}\n" +
                "  ) {\n" +
                "    questions: data {\n" +
                "      acRate\n" +
                "      difficulty\n" +
                "      frontendQuestionId: questionFrontendId\n" +
                "      title\n" +
                "      titleSlug\n" +
                "      topicTags {\n" +
                "        name\n" +
                "      }\n" +
                "      paidOnly: isPaidOnly\n" +
                "    }\n" +
                "  }\n" +
                "}";
        
        makeGraphQLRequest(query, callback);
    }
    
    /**
     * Get problems by topic/tag
     * @param topicSlug The topic slug (e.g., "array", "dynamic-programming")
     * @param limit Number of problems to fetch
     * @param callback Callback to handle response
     */
    public void getProblemsByTopic(String topicSlug, int limit, LeetCodeCallback callback) {
        String query = "{\n" +
                "  problemsetQuestionList: problemsetQuestionList(\n" +
                "    categorySlug: \"" + topicSlug + "\"\n" +
                "    limit: " + limit + "\n" +
                "    skip: 0\n" +
                "    filters: {}\n" +
                "  ) {\n" +
                "    total: totalNum\n" +
                "    questions: data {\n" +
                "      acRate\n" +
                "      difficulty\n" +
                "      frontendQuestionId: questionFrontendId\n" +
                "      title\n" +
                "      titleSlug\n" +
                "      topicTags {\n" +
                "        name\n" +
                "        slug\n" +
                "      }\n" +
                "      paidOnly: isPaidOnly\n" +
                "    }\n" +
                "  }\n" +
                "}";
        
        makeGraphQLRequest(query, callback);
    }
    
    /**
     * Get user submission statistics
     * @param username The LeetCode username
     * @param callback Callback to handle response
     */
    public void getUserSubmissionStats(String username, LeetCodeCallback callback) {
        String query = "{\n" +
                "  matchedUser(username: \"" + username + "\") {\n" +
                "    username\n" +
                "    submitStats {\n" +
                "      acSubmissionNum {\n" +
                "        difficulty\n" +
                "        count\n" +
                "        submissions\n" +
                "      }\n" +
                "      totalSubmissionNum {\n" +
                "        difficulty\n" +
                "        count\n" +
                "        submissions\n" +
                "      }\n" +
                "    }\n" +
                "    submissionCalendar\n" +
                "  }\n" +
                "}";
        
        makeGraphQLRequest(query, callback);
    }
    
    private void makeGraphQLRequest(String query, LeetCodeCallback callback) {
        try {
            JSONObject json = new JSONObject();
            json.put("query", query);
            
            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.get("application/json; charset=utf-8")
            );
            
            Request request = new Request.Builder()
                    .url(LEETCODE_GRAPHQL_URL)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("User-Agent", "CodeStreak-Android-App")
                    .build();
            
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError(e);
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        callback.onSuccess(responseBody);
                    } else {
                        callback.onError(new Exception("HTTP " + response.code() + ": " + response.message()));
                    }
                }
            });
            
        } catch (Exception e) {
            callback.onError(e);
        }
    }
}
