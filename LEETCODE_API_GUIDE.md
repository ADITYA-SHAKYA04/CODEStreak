// In MainActivity.java, replace this line:
loadUserData("your-leetcode-username"); // Put your actual username here// In MainActivity.java, replace this line:
loadUserData("your-leetcode-username"); // Put your actual username here// In MainActivity.java, replace this line:
loadUserData("your-leetcode-username"); // Put your actual username here// In MainActivity.java, replace this line:
loadUserData("your-leetcode-username"); // Put your actual username here// In MainActivity.java, replace this line:
loadUserData("your-leetcode-username"); // Put your actual username here// In MainActivity.java, replace this line:
loadUserData("your-leetcode-username"); // Put your actual username here// In MainActivity.java, replace this line:
loadUserData("your-leetcode-username"); // Put your actual username here// In MainActivity.java, replace this line:
loadUserData("your-leetcode-username"); // Put your actual username here// In MainActivity.java, replace this line:
loadUserData("your-leetcode-username"); // Put your actual username here// In MainActivity.java, replace this line:
loadUserData("your-leetcode-username"); // Put your actual username here// In MainActivity.java, replace this line:
loadUserData("your-leetcode-username"); // Put your actual username here// In MainActivity.java, replace this line:
loadUserData("your-leetcode-username"); // Put your actual username here# LeetCode API Integration Guide

This guide shows you exactly how to fetch problems and user data from LeetCode using their GraphQL API.

## 🎯 **Quick Start**

### 1. **Basic Setup**
```java
// Initialize the API client
LeetCodeAPI leetCodeAPI = new LeetCodeAPI();
```

### 2. **Get User Statistics**
```java
leetCodeAPI.getUserProfile("your-username", new LeetCodeAPI.LeetCodeCallback() {
    @Override
    public void onSuccess(String response) {
        // Parse JSON response to get solved problems count
        try {
            JSONObject jsonResponse = new JSONObject(response);
            JSONObject data = jsonResponse.getJSONObject("data");
            JSONObject matchedUser = data.getJSONObject("matchedUser");
            JSONObject submitStats = matchedUser.getJSONObject("submitStats");
            JSONArray acSubmissionNum = submitStats.getJSONArray("acSubmissionNum");
            
            for (int i = 0; i < acSubmissionNum.length(); i++) {
                JSONObject submission = acSubmissionNum.getJSONObject(i);
                String difficulty = submission.getString("difficulty");
                int count = submission.getInt("count");
                
                System.out.println(difficulty + ": " + count + " problems solved");
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
```

## 📊 **Available API Methods**

### **1. getUserProfile(username, callback)**
Fetches user's solved problem statistics by difficulty.

**Response includes:**
- Easy problems count
- Medium problems count  
- Hard problems count
- User ranking

### **2. getProblemsByDifficulty(difficulty, callback)**
Gets a list of problems filtered by difficulty.

**Parameters:**
- `difficulty`: "Easy", "Medium", or "Hard"
- `callback`: Response handler

**Response includes:**
- Problem title and ID
- Acceptance rate
- Topic tags
- Whether it's premium-only

### **3. getDailyCodingChallenge(callback)**
Fetches today's daily coding challenge.

**Response includes:**
- Problem title and difficulty
- Problem URL
- Acceptance rate
- Topic tags

### **4. getRandomProblem(difficulty, callback)**
Gets a random problem of specified difficulty for practice.

### **5. getProblemsByTopic(topicSlug, limit, callback)**
Fetches problems filtered by topic/tag.

**Common topic slugs:**
- "array"
- "dynamic-programming"
- "tree"
- "graph"
- "string"
- "math"

## 🔧 **Practical Examples**

### **Example 1: Update Your App with Real Data**

```java
private void loadRealUserData() {
    String username = "your-leetcode-username"; // Replace with actual username
    
    leetCodeAPI.getUserProfile(username, new LeetCodeAPI.LeetCodeCallback() {
        @Override
        public void onSuccess(String response) {
            runOnUiThread(() -> {
                try {
                    // Parse the response
                    JSONObject data = new JSONObject(response)
                        .getJSONObject("data")
                        .getJSONObject("matchedUser")
                        .getJSONObject("submitStats");
                    
                    JSONArray submissions = data.getJSONArray("acSubmissionNum");
                    
                    int easy = 0, medium = 0, hard = 0;
                    
                    for (int i = 0; i < submissions.length(); i++) {
                        JSONObject sub = submissions.getJSONObject(i);
                        String diff = sub.getString("difficulty");
                        int count = sub.getInt("count");
                        
                        switch (diff) {
                            case "Easy": easy = count; break;
                            case "Medium": medium = count; break;
                            case "Hard": hard = count; break;
                        }
                    }
                    
                    // Update your UI
                    updateProblemCounts(easy, medium, hard);
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        
        @Override
        public void onError(Exception error) {
            // Handle error - maybe show sample data
            System.out.println("Error: " + error.getMessage());
        }
    });
}

private void updateProblemCounts(int easy, int medium, int hard) {
    easyProblems = easy;
    mediumProblems = medium;
    hardProblems = hard;
    totalProblems = easy + medium + hard;
    
    // Refresh pie chart and stats
    setupPieChart();
    updateStats();
}
```

### **Example 2: Show Daily Challenge**

```java
private void loadDailyChallenge() {
    leetCodeAPI.getDailyCodingChallenge(new LeetCodeAPI.LeetCodeCallback() {
        @Override
        public void onSuccess(String response) {
            runOnUiThread(() -> {
                try {
                    JSONObject challenge = new JSONObject(response)
                        .getJSONObject("data")
                        .getJSONObject("activeDailyCodingChallengeQuestion")
                        .getJSONObject("question");
                    
                    String title = challenge.getString("title");
                    String difficulty = challenge.getString("difficulty");
                    String slug = challenge.getString("titleSlug");
                    
                    // Show in your UI
                    showDailyChallengeNotification(title, difficulty, slug);
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        
        @Override
        public void onError(Exception error) {
            System.out.println("Daily challenge error: " + error.getMessage());
        }
    });
}
```

### **Example 3: Get Practice Problems**

```java
private void loadPracticeProblems() {
    // Get 10 random easy problems
    leetCodeAPI.getProblemsByDifficulty("Easy", new LeetCodeAPI.LeetCodeCallback() {
        @Override
        public void onSuccess(String response) {
            try {
                JSONArray questions = new JSONObject(response)
                    .getJSONObject("data")
                    .getJSONObject("problemsetQuestionList")
                    .getJSONArray("questions");
                
                System.out.println("=== Practice Problems ===");
                
                for (int i = 0; i < Math.min(questions.length(), 5); i++) {
                    JSONObject q = questions.getJSONObject(i);
                    
                    if (!q.getBoolean("paidOnly")) {
                        String title = q.getString("title");
                        String slug = q.getString("titleSlug");
                        
                        System.out.println((i+1) + ". " + title);
                        System.out.println("   https://leetcode.com/problems/" + slug + "/");
                    }
                }
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        @Override
        public void onError(Exception error) {
            System.out.println("Error loading problems: " + error.getMessage());
        }
    });
}
```

## 🔒 **Important Notes**

### **Authentication**
- Most queries work without authentication
- For user-specific data, you need a valid LeetCode username
- Some features might require cookies/sessions for private data

### **Rate Limiting**
- LeetCode may have rate limits on their API
- Add delays between requests if needed
- Cache responses when possible

### **Error Handling**
- Always include proper error handling
- Have fallback sample data
- Show user-friendly error messages

## 🚀 **Integration in Your App**

### **Step 1: Add to MainActivity**
```java
// In onCreate()
LeetCodeAPI leetCodeAPI = new LeetCodeAPI();

// Load user data (replace with actual username)
loadUserData("your-username");
```

### **Step 2: Update UI Method**
```java
private void loadUserData(String username) {
    leetCodeAPI.getUserProfile(username, new LeetCodeAPI.LeetCodeCallback() {
        @Override
        public void onSuccess(String response) {
            runOnUiThread(() -> parseAndUpdateUI(response));
        }
        
        @Override
        public void onError(Exception error) {
            // Use sample data or show error
        }
    });
}
```

### **Step 3: Add Internet Permission**
Make sure you have this in `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.INTERNET" />
```

## 📱 **Testing the API**

1. **Test with your username**: Replace `"your-username"` with your actual LeetCode username
2. **Check logcat**: Use `System.out.println()` or `Log.d()` to see API responses
3. **Handle errors gracefully**: Always have fallback data
4. **Test on device**: Make sure internet permission works

---

**Ready to integrate real LeetCode data into your app!** 🎯
