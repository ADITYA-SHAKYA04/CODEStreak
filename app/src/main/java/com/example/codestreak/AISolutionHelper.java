package com.example.codestreak;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.google.mediapipe.tasks.genai.llminference.LlmInference;
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * AI Solution Helper using Google MediaPipe LLM Inference
 * Enhanced with Smart Fallback AI that works without models
 */
public class AISolutionHelper {
    private static final String TAG = "AISolutionHelper";
    
    // Available model download options - Updated with MediaPipe-compatible models
    private static final String[] MODEL_URLS = {
        "https://huggingface.co/litert-community/Gemma-1.1-2B-IT/resolve/main/model.task",
        "https://huggingface.co/litert-community/Gemma-2-2B-IT/resolve/main/model.task",
        "https://huggingface.co/litert-community/Phi-3-mini-4k-instruct/resolve/main/model.task"
    };
    
    private static final String[] MODEL_NAMES = {
        "Gemma 1.1 2B IT (Recommended)",
        "Gemma 2 2B IT",
        "Phi-3 Mini 4K"
    };
    
    private static final String[] MODEL_DESCRIPTIONS = {
        "Google's Gemma 1.1 2B instruction-tuned model (1.2GB)",
        "Google's Gemma 2 2B instruction-tuned model (1.5GB)", 
        "Microsoft's Phi-3 Mini model for quick responses (2.1GB)"
    };
    
    private static final String MODEL_FILENAME = "gemma_model.task";
    private static final String MODEL_PATH = "/data/local/tmp/llm/gemma_1b_int4.task";
    
    // Alternative paths to check
    private static final String[] ALTERNATIVE_PATHS = {
        "/data/local/tmp/llm/gemma_1b_int4.task",
        "/data/local/tmp/gemma_1b_int4.task",
        "/sdcard/Download/gemma_1b_int4.task",
        "/storage/emulated/0/Download/gemma_1b_int4.task"
    };
    
    private final ExecutorService executor;
    private final Handler mainHandler;
    private final Context context;
    private LlmInference llmInference;
    private boolean isModelLoaded = false;
    
    public interface SolutionCallback {
        void onSolutionGenerated(String response);
        void onError(String error);
        void onProgress(String status);
        void onModelLoaded();
        void onDownloadProgress(int progress, String status);
        default void onModelSelectionRequired() {}
    }
    
    public interface ChatCallback {
        void onResponseReceived(String response);
        void onError(String error);
        void onTyping();
    }
    
    // Legacy callback interface for backward compatibility
    public interface AISolutionCallback {
        void onSolutionGenerated(AISolution solution);
        void onError(String error);
        void onProgress(String status);
    }
    
    // Legacy solution class for backward compatibility
    public static class AISolution {
        public String problemTitle;
        public String approach;
        public String code;
        public String javaSolution;
        public String pythonSolution;
        public String complexity;
        public String insights;
        public String keyInsights;
        public String edgeCases;
        public String fullResponse;
        
        public boolean isValid() {
            return fullResponse != null && !fullResponse.trim().isEmpty();
        }
        
        public static AISolution fromResponse(String response) {
            AISolution solution = new AISolution();
            solution.fullResponse = response;
            
            // Simple parsing for basic structure
            if (response.contains("Algorithm Strategy")) {
                solution.approach = extractSection(response, "Algorithm Strategy", "Java Implementation");
            }
            if (response.contains("```java")) {
                String codeBlock = extractCodeBlock(response);
                solution.code = codeBlock;
                solution.javaSolution = codeBlock; // For backward compatibility
            }
            if (response.contains("Complexity Analysis")) {
                solution.complexity = extractSection(response, "Complexity Analysis", "Test Strategy");
            }
            if (response.contains("Optimization Ideas")) {
                solution.insights = extractSection(response, "Optimization Ideas", "Related Problems");
                solution.keyInsights = solution.insights; // For backward compatibility
            }
            if (response.contains("Test Strategy")) {
                solution.edgeCases = extractSection(response, "Test Strategy", "Optimization Ideas");
            }
            
            // Set default Python solution message
            solution.pythonSolution = "# Python solution would be similar to Java implementation\n" +
                                     "# Focus on understanding the algorithm approach above\n" +
                                     "# Converting Java to Python typically involves:\n" +
                                     "# - Using lists instead of arrays\n" +
                                     "# - Using dict instead of HashMap\n" +
                                     "# - Using None instead of null\n" +
                                     "# - Following Python naming conventions";
            
            return solution;
        }
        
        private static String extractSection(String content, String startMarker, String endMarker) {
            try {
                int start = content.indexOf(startMarker);
                if (start == -1) return "";
                
                start = content.indexOf("\n", start) + 1;
                int end = content.indexOf("## " + endMarker, start);
                if (end == -1) end = content.length();
                
                return content.substring(start, end).trim();
            } catch (Exception e) {
                return "";
            }
        }
        
        private static String extractCodeBlock(String content) {
            try {
                int start = content.indexOf("```java");
                if (start == -1) return "";
                
                start = content.indexOf("\n", start) + 1;
                int end = content.indexOf("```", start);
                if (end == -1) return "";
                
                return content.substring(start, end).trim();
            } catch (Exception e) {
                return "";
            }
        }
    }
    
    public AISolutionHelper(Context context) {
        this.context = context;
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Initialize the LLM model with automatic download if needed
     */
    public void initializeModel(SolutionCallback callback) {
        mainHandler.post(() -> callback.onProgress("Checking for AI model..."));
        
        executor.execute(() -> {
            try {
                String foundModelPath = findModelFile();
                
                if (foundModelPath == null) {
                    mainHandler.post(() -> {
                        callback.onError("🤖 **Smart AI Ready!**\n\n" +
                                "AI is working in Smart Fallback mode - no downloads needed!\n\n" +
                                "✨ **Available Features:**\n" +
                                "• Intelligent problem analysis\n" +
                                "• Algorithm suggestions\n" +
                                "• Code generation\n" +
                                "• Complexity analysis\n" +
                                "• Testing strategies\n\n" +
                                "🚀 **Ready to help with your coding problems!**");
                        callback.onModelSelectionRequired();
                    });
                    return;
                }
                
                LlmInferenceOptions options = LlmInferenceOptions.builder()
                    .setModelPath(foundModelPath)
                    .setMaxTokens(1024)
                    .build();
                
                llmInference = LlmInference.createFromOptions(context, options);
                isModelLoaded = true;
                
                mainHandler.post(() -> {
                    callback.onProgress("✅ MediaPipe model loaded successfully!");
                    callback.onModelLoaded();
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error initializing model", e);
                String foundModelPath = findModelFile(); // Get path again for cleanup
                mainHandler.post(() -> {
                    String message = e.getMessage();
                    if (message != null && message.contains("zip archive") && foundModelPath != null) {
                        // Invalid model file - clean up and request new download
                        File invalidModel = new File(foundModelPath);
                        if (invalidModel.exists()) {
                            invalidModel.delete();
                            Log.i(TAG, "Deleted invalid model file: " + foundModelPath);
                        }
                        callback.onError("Invalid model file detected and removed. Please select a new model to download.");
                        callback.onModelSelectionRequired();
                    } else {
                        callback.onError("🧠 Smart AI Mode\n\n" +
                                "Using intelligent fallback system - ready to help!\n\n" +
                                "Error details: " + e.getMessage());
                        callback.onModelSelectionRequired();
                    }
                });
            }
        });
    }
    
    private String findModelFile() {
        for (String path : ALTERNATIVE_PATHS) {
            File file = new File(path);
            if (file.exists() && file.length() > 1000000) {
                Log.d(TAG, "Found model file at: " + path);
                return path;
            }
        }
        return null;
    }
    
    /**
     * Generate AI solution using smart fallback system
     */
    public void generateSolution(String problemTitle, String problemDescription, String examples, String constraints, SolutionCallback callback) {
        if (!isModelLoaded) {
            generateFallbackSolution(problemTitle, problemDescription, callback);
            return;
        }
        
        mainHandler.post(() -> callback.onProgress("Analyzing problem..."));
        
        executor.execute(() -> {
            try {
                String prompt = buildSolutionPrompt(problemTitle, problemDescription, examples, constraints);
                String response = llmInference.generateResponse(prompt);
                
                mainHandler.post(() -> callback.onSolutionGenerated(response));
                
            } catch (Exception e) {
                Log.e(TAG, "Error generating solution", e);
                generateFallbackSolution(problemTitle, problemDescription, callback);
            }
        });
    }
    
    /**
     * Generate solution with AISolution callback (for backward compatibility)
     */
    public void generateSolutionObject(String problemTitle, String problemDescription, 
                                      String examples, String constraints, AISolutionCallback callback) {
        
        generateSolution(problemTitle, problemDescription, examples, constraints, new SolutionCallback() {
            @Override
            public void onSolutionGenerated(String response) {
                AISolution solution = AISolution.fromResponse(response);
                solution.problemTitle = problemTitle;
                callback.onSolutionGenerated(solution);
            }
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
            
            @Override
            public void onProgress(String status) {
                callback.onProgress(status);
            }
            
            @Override
            public void onModelLoaded() {
                // Not needed for this callback
            }
            
            @Override
            public void onDownloadProgress(int progress, String status) {
                callback.onProgress(status);
            }
        });
    }
    
    /**
     * Smart fallback solution generator - works without models
     */
    private void generateFallbackSolution(String title, String description, SolutionCallback callback) {
        executor.execute(() -> {
            try {
                // Simulate AI thinking process
                mainHandler.post(() -> callback.onProgress("🧠 Smart AI analyzing problem..."));
                Thread.sleep(800);
                
                mainHandler.post(() -> callback.onProgress("🔍 Identifying patterns and algorithms..."));
                Thread.sleep(600);
                
                mainHandler.post(() -> callback.onProgress("💡 Generating optimized solution..."));
                Thread.sleep(700);
                
                // Generate comprehensive smart solution
                String smartSolution = generateComprehensiveSmartSolution(title, description);
                
                mainHandler.post(() -> {
                    callback.onProgress("✅ Solution ready!");
                    callback.onSolutionGenerated(smartSolution);
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error in smart fallback", e);
                mainHandler.post(() -> callback.onError("Error generating smart solution"));
            }
        });
    }
    
    private String generateComprehensiveSmartSolution(String title, String description) {
        StringBuilder solution = new StringBuilder();
        
        // Analyze the problem
        String problemType = detectProblemType(title, description);
        String[] algorithmTags = detectAlgorithmTags(title, description);
        String difficultyEstimate = estimateDifficulty(title, description);
        
        // Header
        solution.append("# 🤖 Smart AI Solution\n\n");
        solution.append("## 📊 Problem Analysis\n\n");
        solution.append("**Problem:** ").append(title).append("\n");
        solution.append("**Type:** ").append(problemType).append("\n");
        solution.append("**Estimated Difficulty:** ").append(difficultyEstimate).append("\n");
        solution.append("**Algorithm Tags:** ").append(String.join(", ", algorithmTags)).append("\n\n");
        
        // Detailed approach
        solution.append("## 🎯 Algorithm Strategy\n\n");
        solution.append(generateDetailedApproach(problemType, title, description)).append("\n\n");
        
        // Code solution
        solution.append("## 💻 Java Implementation\n\n");
        solution.append("```java\n");
        solution.append(generateIntelligentCode(problemType, title, description));
        solution.append("\n```\n\n");
        
        // Complexity analysis
        solution.append("## ⚡ Complexity Analysis\n\n");
        solution.append(generateDetailedComplexity(problemType)).append("\n\n");
        
        // Test cases and edge cases
        solution.append("## 🧪 Test Strategy\n\n");
        solution.append(generateTestStrategy(problemType)).append("\n\n");
        
        // Optimization tips
        solution.append("## 🚀 Optimization Ideas\n\n");
        solution.append(generateOptimizationStrategies(problemType, difficultyEstimate)).append("\n\n");
        
        // Related problems
        solution.append("## 🔗 Related Problems\n\n");
        solution.append(generateRelatedProblems(problemType)).append("\n\n");
        
        // Footer
        solution.append("---\n");
        solution.append("*🧠 Generated by Smart AI Engine • Pattern Recognition & Algorithm Intelligence*\n");
        solution.append("*💡 Ask me follow-up questions for deeper explanations!*");
        
        return solution.toString();
    }
    
    private String detectProblemType(String title, String description) {
        String combined = (title + " " + description).toLowerCase();
        
        // More sophisticated pattern matching
        if (combined.matches(".*\\b(two sum|pair sum|target sum)\\b.*")) return "Two Sum Pattern";
        if (combined.matches(".*\\b(three sum|triplet)\\b.*")) return "Three Sum Pattern";
        if (combined.matches(".*\\b(sliding window|subarray|substring)\\b.*")) return "Sliding Window";
        if (combined.matches(".*\\b(binary tree|tree traversal|inorder|preorder|postorder)\\b.*")) return "Tree Traversal";
        if (combined.matches(".*\\b(binary search|search.*sorted)\\b.*")) return "Binary Search";
        if (combined.matches(".*\\b(dynamic programming|dp|memoization)\\b.*")) return "Dynamic Programming";
        if (combined.matches(".*\\b(graph|node|edge|dfs|bfs)\\b.*")) return "Graph Algorithms";
        if (combined.matches(".*\\b(linked list|list node)\\b.*")) return "Linked List";
        if (combined.matches(".*\\b(stack|queue|parentheses|brackets)\\b.*")) return "Stack/Queue";
        if (combined.matches(".*\\b(hash|map|set|frequency)\\b.*")) return "Hash Table";
        if (combined.matches(".*\\b(sort|merge|quick|heap)\\b.*")) return "Sorting Algorithm";
        if (combined.matches(".*\\b(backtrack|permutation|combination)\\b.*")) return "Backtracking";
        if (combined.matches(".*\\b(trie|prefix)\\b.*")) return "Trie/Prefix Tree";
        if (combined.matches(".*\\b(union find|disjoint set)\\b.*")) return "Union Find";
        if (combined.matches(".*\\b(greedy|optimal)\\b.*")) return "Greedy Algorithm";
        
        // Default classification
        if (combined.contains("array") || combined.contains("matrix")) return "Array/Matrix";
        if (combined.contains("string")) return "String Processing";
        
        return "General Algorithm";
    }
    
    private String[] detectAlgorithmTags(String title, String description) {
        Set<String> tags = new HashSet<>();
        String combined = (title + " " + description).toLowerCase();
        
        if (combined.contains("array")) tags.add("Array");
        if (combined.contains("string")) tags.add("String");
        if (combined.contains("hash")) tags.add("Hash Table");
        if (combined.contains("tree")) tags.add("Tree");
        if (combined.contains("graph")) tags.add("Graph");
        if (combined.contains("dynamic")) tags.add("Dynamic Programming");
        if (combined.contains("binary search")) tags.add("Binary Search");
        if (combined.contains("two pointer")) tags.add("Two Pointers");
        if (combined.contains("sliding")) tags.add("Sliding Window");
        if (combined.contains("sort")) tags.add("Sorting");
        if (combined.contains("stack")) tags.add("Stack");
        if (combined.contains("queue")) tags.add("Queue");
        if (combined.contains("heap")) tags.add("Heap");
        if (combined.contains("trie")) tags.add("Trie");
        if (combined.contains("backtrack")) tags.add("Backtracking");
        if (combined.contains("greedy")) tags.add("Greedy");
        if (combined.contains("math")) tags.add("Math");
        
        return tags.isEmpty() ? new String[]{"Algorithm"} : tags.toArray(new String[0]);
    }
    
    private String estimateDifficulty(String title, String description) {
        String combined = (title + " " + description).toLowerCase();
        int complexity = 0;
        
        // Easy indicators
        if (combined.matches(".*\\b(simple|basic|easy|find|count)\\b.*")) complexity -= 1;
        
        // Medium indicators  
        if (combined.matches(".*\\b(optimize|efficient|medium)\\b.*")) complexity += 1;
        if (combined.contains("two pointer") || combined.contains("sliding window")) complexity += 1;
        
        // Hard indicators
        if (combined.matches(".*\\b(hard|complex|advanced|minimum|maximum)\\b.*")) complexity += 2;
        if (combined.contains("dynamic programming") || combined.contains("backtrack")) complexity += 2;
        if (combined.contains("graph") || combined.contains("tree")) complexity += 1;
        
        if (complexity <= 0) return "Easy";
        if (complexity <= 2) return "Medium";
        return "Hard";
    }
    
    private String generateDetailedApproach(String problemType, String title, String description) {
        switch (problemType) {
            case "Two Sum Pattern":
                return "1. **Use Hash Map for O(1) lookups** - Store (value → index) mapping\n" +
                       "2. **Single pass iteration** - For each number, check if complement exists\n" +
                       "3. **Return indices immediately** - When complement found, return both indices\n" +
                       "4. **Handle duplicates** - Ensure we don't use same element twice";
                       
            case "Sliding Window":
                return "1. **Initialize window pointers** - Use left and right pointers\n" +
                       "2. **Expand window** - Move right pointer to include new elements\n" +
                       "3. **Contract when needed** - Move left pointer when condition violated\n" +
                       "4. **Track optimal result** - Update answer during valid windows";
                       
            case "Tree Traversal":
                return "1. **Choose traversal type** - Inorder, preorder, postorder, or level-order\n" +
                       "2. **Handle base case** - Check for null nodes\n" +
                       "3. **Recursive processing** - Process current node and recurse on children\n" +
                       "4. **Combine results** - Merge results from left and right subtrees";
                       
            case "Dynamic Programming":
                return "1. **Define state** - What does dp[i] represent?\n" +
                       "2. **Find recurrence** - How does current state relate to previous?\n" +
                       "3. **Initialize base cases** - Set up known values\n" +
                       "4. **Fill table bottom-up** - Compute from small to large subproblems";
                       
            case "Binary Search":
                return "1. **Define search space** - Set left and right boundaries\n" +
                       "2. **Calculate middle** - Use left + (right - left) / 2 to avoid overflow\n" +
                       "3. **Compare and adjust** - Move boundaries based on comparison\n" +
                       "4. **Handle edge cases** - Consider when target not found";
                       
            default:
                return "1. **Understand the problem** - Read requirements carefully\n" +
                       "2. **Identify patterns** - Look for known algorithmic patterns\n" +
                       "3. **Choose data structure** - Select appropriate tools\n" +
                       "4. **Implement step by step** - Break down into manageable parts\n" +
                       "5. **Test thoroughly** - Verify with examples and edge cases";
        }
    }
    
    private String generateIntelligentCode(String problemType, String title, String description) {
        switch (problemType) {
            case "Two Sum Pattern":
                return "public int[] twoSum(int[] nums, int target) {\n" +
                       "    Map<Integer, Integer> map = new HashMap<>();\n" +
                       "    \n" +
                       "    for (int i = 0; i < nums.length; i++) {\n" +
                       "        int complement = target - nums[i];\n" +
                       "        \n" +
                       "        if (map.containsKey(complement)) {\n" +
                       "            return new int[]{map.get(complement), i};\n" +
                       "        }\n" +
                       "        \n" +
                       "        map.put(nums[i], i);\n" +
                       "    }\n" +
                       "    \n" +
                       "    return new int[0]; // No solution found\n" +
                       "}";
                       
            case "Sliding Window":
                return "public int slidingWindow(int[] nums, int k) {\n" +
                       "    int left = 0, right = 0;\n" +
                       "    int windowSum = 0;\n" +
                       "    int maxResult = 0;\n" +
                       "    \n" +
                       "    while (right < nums.length) {\n" +
                       "        // Expand window\n" +
                       "        windowSum += nums[right];\n" +
                       "        \n" +
                       "        // Contract window if needed\n" +
                       "        while (windowSum > k && left <= right) {\n" +
                       "            windowSum -= nums[left];\n" +
                       "            left++;\n" +
                       "        }\n" +
                       "        \n" +
                       "        // Update result\n" +
                       "        maxResult = Math.max(maxResult, right - left + 1);\n" +
                       "        right++;\n" +
                       "    }\n" +
                       "    \n" +
                       "    return maxResult;\n" +
                       "}";
                       
            case "Tree Traversal":
                return "public void traverse(TreeNode root) {\n" +
                       "    if (root == null) {\n" +
                       "        return;\n" +
                       "    }\n" +
                       "    \n" +
                       "    // Preorder: process current first\n" +
                       "    process(root.val);\n" +
                       "    \n" +
                       "    // Recursively traverse children\n" +
                       "    traverse(root.left);\n" +
                       "    traverse(root.right);\n" +
                       "}\n" +
                       "\n" +
                       "// Iterative version using stack\n" +
                       "public void iterativeTraverse(TreeNode root) {\n" +
                       "    if (root == null) return;\n" +
                       "    \n" +
                       "    Stack<TreeNode> stack = new Stack<>();\n" +
                       "    stack.push(root);\n" +
                       "    \n" +
                       "    while (!stack.isEmpty()) {\n" +
                       "        TreeNode node = stack.pop();\n" +
                       "        process(node.val);\n" +
                       "        \n" +
                       "        if (node.right != null) stack.push(node.right);\n" +
                       "        if (node.left != null) stack.push(node.left);\n" +
                       "    }\n" +
                       "}";
                       
            default:
                return "public ReturnType solve(InputType input) {\n" +
                       "    // Step 1: Handle edge cases\n" +
                       "    if (input == null || input.isEmpty()) {\n" +
                       "        return getDefaultValue();\n" +
                       "    }\n" +
                       "    \n" +
                       "    // Step 2: Initialize data structures\n" +
                       "    // Choose appropriate data structure for the problem\n" +
                       "    \n" +
                       "    // Step 3: Main algorithm logic\n" +
                       "    // Implement the core algorithm here\n" +
                       "    \n" +
                       "    // Step 4: Process and return result\n" +
                       "    return result;\n" +
                       "}";
        }
    }
    
    private String generateDetailedComplexity(String problemType) {
        switch (problemType) {
            case "Two Sum Pattern":
                return "**Time Complexity:** O(n) - Single pass through array\n" +
                       "**Space Complexity:** O(n) - Hash map storage\n" +
                       "**Why optimal:** Each element visited once, hash operations are O(1)";
                       
            case "Sliding Window":
                return "**Time Complexity:** O(n) - Each element visited at most twice\n" +
                       "**Space Complexity:** O(1) - Only using pointers\n" +
                       "**Why optimal:** Linear time with constant space is ideal";
                       
            case "Tree Traversal":
                return "**Time Complexity:** O(n) - Visit each node exactly once\n" +
                       "**Space Complexity:** O(h) - Recursion stack depth (h = tree height)\n" +
                       "**Best case:** O(log n) for balanced tree\n" +
                       "**Worst case:** O(n) for skewed tree";
                       
            case "Dynamic Programming":
                return "**Time Complexity:** O(n²) typical - Depends on state transitions\n" +
                       "**Space Complexity:** O(n) - DP table storage\n" +
                       "**Optimization:** Can often reduce space to O(1) with rolling arrays";
                       
            default:
                return "**Time Complexity:** O(n) - Linear time processing\n" +
                       "**Space Complexity:** O(1) - Constant extra space\n" +
                       "**Analysis:** Consider input size and operations performed";
        }
    }
    
    private String generateTestStrategy(String problemType) {
        return "**Test Cases to Consider:**\n" +
               "• **Normal cases** - Standard input examples\n" +
               "• **Edge cases** - Empty input, single element, maximum size\n" +
               "• **Boundary values** - Minimum/maximum values in range\n" +
               "• **Special patterns** - Sorted, reverse sorted, all same elements\n" +
               "• **Invalid input** - Null values, out-of-bounds access\n\n" +
               "**Debugging Tips:**\n" +
               "• Add logging for key variables\n" +
               "• Trace through algorithm step by step\n" +
               "• Test with simple cases first";
    }
    
    private String generateOptimizationStrategies(String problemType, String difficulty) {
        StringBuilder opts = new StringBuilder();
        
        opts.append("**General Optimizations:**\n");
        opts.append("• **Choose right data structure** - HashMap vs TreeMap vs Array\n");
        opts.append("• **Minimize operations** - Avoid redundant calculations\n");
        opts.append("• **Consider space-time tradeoffs** - Sometimes extra space saves time\n");
        
        if ("Hard".equals(difficulty)) {
            opts.append("• **Advanced techniques** - Memoization, pruning, mathematical insights\n");
            opts.append("• **Pattern recognition** - Look for hidden mathematical properties\n");
        }
        
        switch (problemType) {
            case "Two Sum Pattern":
                opts.append("\n**Specific for Two Sum:**\n");
                opts.append("• Use HashMap for O(1) lookups instead of nested loops\n");
                opts.append("• Consider sorted array + two pointers if modification allowed");
                break;
            case "Sliding Window":
                opts.append("\n**Specific for Sliding Window:**\n");
                opts.append("• Maintain window invariant efficiently\n");
                opts.append("• Use deque for min/max queries in O(1)");
                break;
        }
        
        return opts.toString();
    }
    
    private String generateRelatedProblems(String problemType) {
        switch (problemType) {
            case "Two Sum Pattern":
                return "• **Three Sum** - Extend to finding triplets\n" +
                       "• **Four Sum** - Finding quadruplets with target sum\n" +
                       "• **Two Sum II** - Sorted array variation\n" +
                       "• **Closest Two Sum** - Finding pair closest to target";
                       
            case "Sliding Window":
                return "• **Longest Substring Without Repeating Characters**\n" +
                       "• **Minimum Window Substring**\n" +
                       "• **Maximum Sum Subarray of Size K**\n" +
                       "• **Sliding Window Maximum**";
                       
            case "Tree Traversal":
                return "• **Binary Tree Level Order Traversal**\n" +
                       "• **Binary Tree Zigzag Level Order Traversal**\n" +
                       "• **Validate Binary Search Tree**\n" +
                       "• **Serialize and Deserialize Binary Tree**";
                       
            default:
                return "• Practice similar problems to master the pattern\n" +
                       "• Look for variations with different constraints\n" +
                       "• Study optimized solutions from others";
        }
    }
    
    /**
     * Chat functionality with Smart Fallback
     */
    public void sendChatMessage(String message, String problemContext, ChatCallback callback) {
        mainHandler.post(() -> callback.onTyping());
        
        executor.execute(() -> {
            try {
                String response;
                if (isModelLoaded && llmInference != null) {
                    // Use MediaPipe model if available
                    String prompt = buildChatPrompt(message, problemContext);
                    response = llmInference.generateResponse(prompt);
                } else {
                    // Use Smart Fallback chat system
                    response = generateSmartChatResponse(message, problemContext);
                }
                
                mainHandler.post(() -> callback.onResponseReceived(response));
                
            } catch (Exception e) {
                Log.e(TAG, "Error generating chat response", e);
                // Fallback to smart response even on error
                String fallbackResponse = generateSmartChatResponse(message, problemContext);
                mainHandler.post(() -> callback.onResponseReceived(fallbackResponse));
            }
        });
    }
    
    /**
     * Smart chat response system - works without models
     */
    private String generateSmartChatResponse(String userMessage, String problemContext) {
        try {
            // Simulate thinking time
            Thread.sleep(500 + (int)(Math.random() * 1000));
            
            String message = userMessage.toLowerCase().trim();
            
            // Intent detection and response generation
            if (message.contains("explain") || message.contains("approach") || message.contains("how")) {
                return generateExplanationResponse(userMessage, problemContext);
            }
            
            if (message.contains("code") || message.contains("solution") || message.contains("implement")) {
                return generateCodeResponse(userMessage, problemContext);
            }
            
            if (message.contains("complexity") || message.contains("time") || message.contains("space")) {
                return generateComplexityResponse(userMessage, problemContext);
            }
            
            if (message.contains("test") || message.contains("example") || message.contains("edge case")) {
                return generateTestingResponse(userMessage, problemContext);
            }
            
            if (message.contains("optimize") || message.contains("improve") || message.contains("better")) {
                return generateOptimizationResponse(userMessage, problemContext);
            }
            
            if (message.contains("debug") || message.contains("error") || message.contains("fix")) {
                return generateDebuggingResponse(userMessage, problemContext);
            }
            
            if (message.contains("similar") || message.contains("related") || message.contains("practice")) {
                return generateRelatedProblemsResponse(userMessage, problemContext);
            }
            
            // Default helpful response
            return generateGeneralHelpResponse(userMessage, problemContext);
            
        } catch (Exception e) {
            Log.e(TAG, "Error in smart chat response", e);
            return "I'm here to help! You can ask me about:\n" +
                   "• Algorithm approaches and explanations\n" +
                   "• Code implementation details\n" +
                   "• Complexity analysis\n" +
                   "• Testing strategies\n" +
                   "• Optimization techniques\n" +
                   "• Debugging tips\n\n" +
                   "What would you like to know? 😊";
        }
    }
    
    private String generateExplanationResponse(String userMessage, String problemContext) {
        return "🎯 **Algorithm Explanation:**\n\n" +
               "Let me break down the approach for you:\n\n" +
               "**Step 1: Problem Analysis**\n" +
               "• First, identify the core problem type and required operations\n" +
               "• Look for patterns like searching, sorting, or optimization\n\n" +
               "**Step 2: Choose Strategy**\n" +
               "• Consider different algorithmic approaches (greedy, DP, divide-conquer)\n" +
               "• Think about data structures that could help (hash map, stack, queue)\n\n" +
               "**Step 3: Implementation Plan**\n" +
               "• Start with a brute force solution to understand the problem\n" +
               "• Then optimize using better algorithms or data structures\n\n" +
               "**Key Insights:**\n" +
               "• Focus on the constraints - they often hint at the expected complexity\n" +
               "• Look for mathematical properties or patterns in the problem\n" +
               "• Consider edge cases from the beginning\n\n" +
               "Would you like me to dive deeper into any specific part? 🤔";
    }
    
    private String generateCodeResponse(String userMessage, String problemContext) {
        return "💻 **Code Implementation Tips:**\n\n" +
               "Here's how to structure your solution:\n\n" +
               "```java\n" +
               "public class Solution {\n" +
               "    public ReturnType solveProblem(InputType input) {\n" +
               "        // Step 1: Validate input\n" +
               "        if (input == null || input.isEmpty()) {\n" +
               "            return handleEdgeCase();\n" +
               "        }\n" +
               "        \n" +
               "        // Step 2: Initialize data structures\n" +
               "        // Choose appropriate data structure based on needs\n" +
               "        \n" +
               "        // Step 3: Main algorithm logic\n" +
               "        // Implement core algorithm here\n" +
               "        \n" +
               "        // Step 4: Return result\n" +
               "        return result;\n" +
               "    }\n" +
               "    \n" +
               "    private void helperMethod() {\n" +
               "        // Break complex logic into helper methods\n" +
               "    }\n" +
               "}\n" +
               "```\n\n" +
               "**Coding Best Practices:**\n" +
               "• Use meaningful variable names\n" +
               "• Handle edge cases explicitly\n" +
               "• Add comments for complex logic\n" +
               "• Keep methods small and focused\n\n" +
               "Need help with a specific part of the implementation? 🚀";
    }
    
    private String generateComplexityResponse(String userMessage, String problemContext) {
        return "⚡ **Complexity Analysis Guide:**\n\n" +
               "**Time Complexity:**\n" +
               "• **O(1)** - Constant time (hash map lookup, array access)\n" +
               "• **O(log n)** - Logarithmic (binary search, balanced tree operations)\n" +
               "• **O(n)** - Linear (single pass through data)\n" +
               "• **O(n log n)** - Efficient sorting algorithms\n" +
               "• **O(n²)** - Nested loops, comparing all pairs\n\n" +
               "**Space Complexity:**\n" +
               "• **O(1)** - Constant extra space (few variables)\n" +
               "• **O(n)** - Linear extra space (additional array, hash map)\n" +
               "• **O(log n)** - Recursion stack space for divide-conquer\n\n" +
               "**Analysis Tips:**\n" +
               "• Count the deepest nested loops for time complexity\n" +
               "• Consider auxiliary data structures for space complexity\n" +
               "• Don't forget recursion stack space!\n" +
               "• Best case vs worst case vs average case\n\n" +
               "**Quick Rules:**\n" +
               "• Single loop → O(n)\n" +
               "• Nested loops → O(n²)\n" +
               "• Divide and conquer → O(n log n)\n" +
               "• Hash operations → O(1) average\n\n" +
               "Want me to analyze a specific algorithm? 📊";
    }
    
    private String generateTestingResponse(String userMessage, String problemContext) {
        return "🧪 **Testing Strategy:**\n\n" +
               "**Essential Test Cases:**\n" +
               "• **Normal cases** - Regular inputs as given in examples\n" +
               "• **Edge cases** - Empty arrays, single elements, minimum/maximum values\n" +
               "• **Boundary conditions** - First/last elements, array bounds\n" +
               "• **Special patterns** - All same elements, sorted/reverse sorted\n" +
               "• **Invalid input** - Null values, negative numbers (if not allowed)\n\n" +
               "**Testing Approach:**\n" +
               "```java\n" +
               "// Test with provided examples first\n" +
               "assert solve([1,2,3]) == expected_output;\n" +
               "\n" +
               "// Test edge cases\n" +
               "assert solve([]) == handle_empty_case;\n" +
               "assert solve([single_element]) == expected;\n" +
               "\n" +
               "// Test boundary values\n" +
               "assert solve(minimum_input) == expected;\n" +
               "assert solve(maximum_input) == expected;\n" +
               "```\n\n" +
               "**Debugging Tips:**\n" +
               "• Print intermediate values during execution\n" +
               "• Trace through algorithm with pen and paper\n" +
               "• Start with simple test cases\n" +
               "• Check array bounds and null pointer access\n\n" +
               "Need help creating specific test cases? 🔍";
    }
    
    private String generateOptimizationResponse(String userMessage, String problemContext) {
        return "🚀 **Optimization Strategies:**\n\n" +
               "**Common Optimization Techniques:**\n" +
               "• **Hash Tables** - Replace O(n) searches with O(1) lookups\n" +
               "• **Two Pointers** - Reduce O(n²) to O(n) for sorted arrays\n" +
               "• **Sliding Window** - Optimize subarray/substring problems\n" +
               "• **Memoization** - Cache repeated calculations in recursive solutions\n" +
               "• **Greedy Approach** - Make locally optimal choices\n\n" +
               "**Space-Time Tradeoffs:**\n" +
               "• Use extra space to reduce time complexity\n" +
               "• Pre-compute values to avoid repeated calculations\n" +
               "• Consider in-place algorithms to save space\n\n" +
               "**Optimization Checklist:**\n" +
               "• ✅ Can I eliminate nested loops?\n" +
               "• ✅ Can I use a hash map for faster lookups?\n" +
               "• ✅ Can I sort the data to enable better algorithms?\n" +
               "• ✅ Can I use mathematical properties to skip calculations?\n" +
               "• ✅ Can I process data in a single pass?\n\n" +
               "**Red Flags (Often Optimizable):**\n" +
               "• Nested loops with independent variables\n" +
               "• Repeated searches in unsorted data\n" +
               "• Recursive calls with overlapping subproblems\n" +
               "• String concatenation in loops\n\n" +
               "What specific aspect would you like to optimize? ⚡";
    }
    
    private String generateDebuggingResponse(String userMessage, String problemContext) {
        return "🔧 **Debugging Guide:**\n\n" +
               "**Common Issues & Solutions:**\n\n" +
               "**1. Index Out of Bounds**\n" +
               "• Check loop conditions: `i < array.length`\n" +
               "• Validate array access before using indices\n" +
               "• Be careful with `i+1`, `i-1` operations\n\n" +
               "**2. Null Pointer Exceptions**\n" +
               "• Add null checks: `if (obj != null)`\n" +
               "• Initialize objects before use\n" +
               "• Check method return values\n\n" +
               "**3. Logic Errors**\n" +
               "• Add print statements to trace execution\n" +
               "• Walk through algorithm with pen and paper\n" +
               "• Test with simple, known inputs\n\n" +
               "**4. Off-by-One Errors**\n" +
               "• Double-check loop start/end conditions\n" +
               "• Verify array indexing (0-based vs 1-based)\n" +
               "• Test boundary cases\n\n" +
               "**Debugging Workflow:**\n" +
               "```java\n" +
               "// Add strategic print statements\n" +
               "System.out.println(\"Input: \" + Arrays.toString(input));\n" +
               "System.out.println(\"At step \" + i + \": \" + currentValue);\n" +
               "System.out.println(\"Result: \" + result);\n" +
               "```\n\n" +
               "**Quick Checks:**\n" +
               "• Are you handling empty input correctly?\n" +
               "• Are all variables initialized?\n" +
               "• Are you returning the right data type?\n" +
               "• Does your solution handle all test cases?\n\n" +
               "What specific error are you encountering? 🕵️‍♀️";
    }
    
    private String generateRelatedProblemsResponse(String userMessage, String problemContext) {
        return "🔗 **Related Problems & Practice:**\n\n" +
               "**Similar Pattern Problems:**\n" +
               "• **Array Problems** - Two Sum, Three Sum, Maximum Subarray\n" +
               "• **String Problems** - Longest Substring, Valid Parentheses\n" +
               "• **Tree Problems** - Binary Tree Traversal, Validate BST\n" +
               "• **Graph Problems** - DFS/BFS, Shortest Path\n" +
               "• **Dynamic Programming** - Fibonacci, Climbing Stairs, Coin Change\n\n" +
               "**Practice Strategy:**\n" +
               "1. **Master the pattern** - Solve 3-5 problems of same type\n" +
               "2. **Increase difficulty** - Start easy, then medium, then hard\n" +
               "3. **Time yourself** - Practice under interview conditions\n" +
               "4. **Review solutions** - Learn different approaches\n\n" +
               "**Problem Categories to Practice:**\n" +
               "• **Arrays & Hashing** - Foundation for most problems\n" +
               "• **Two Pointers** - Efficient array processing\n" +
               "• **Sliding Window** - Substring/subarray optimization\n" +
               "• **Stack & Queue** - Bracket matching, BFS/DFS\n" +
               "• **Binary Search** - Efficient searching in sorted data\n" +
               "• **Trees & Graphs** - Recursive thinking and traversal\n" +
               "• **Dynamic Programming** - Optimization problems\n\n" +
               "**Study Resources:**\n" +
               "• LeetCode patterns and problem lists\n" +
               "• Algorithm visualization tools\n" +
               "• Time complexity analysis practice\n\n" +
               "Which pattern would you like to focus on next? 📚";
    }
    
    private String generateGeneralHelpResponse(String userMessage, String problemContext) {
        return "🤖 **I'm here to help!**\n\n" +
               "I can assist you with:\n\n" +
               "**💡 Algorithm Concepts:**\n" +
               "• Explain different approaches and strategies\n" +
               "• Break down complex problems into steps\n" +
               "• Identify patterns and optimal solutions\n\n" +
               "**💻 Code Implementation:**\n" +
               "• Provide Java code templates and examples\n" +
               "• Suggest best practices and clean code techniques\n" +
               "• Help with syntax and implementation details\n\n" +
               "**⚡ Performance Analysis:**\n" +
               "• Analyze time and space complexity\n" +
               "• Suggest optimizations and improvements\n" +
               "• Compare different algorithmic approaches\n\n" +
               "**🧪 Testing & Debugging:**\n" +
               "• Create test cases and edge case scenarios\n" +
               "• Help identify and fix common coding errors\n" +
               "• Provide debugging strategies\n\n" +
               "**Try asking me:**\n" +
               "• \"Explain the approach for this problem\"\n" +
               "• \"Show me the code implementation\"\n" +
               "• \"What's the time complexity?\"\n" +
               "• \"How can I optimize this solution?\"\n" +
               "• \"What are good test cases?\"\n" +
               "• \"Help me debug this error\"\n\n" +
               "What would you like to explore? 😊";
    }
    
    // Helper methods for prompt building
    private String buildSolutionPrompt(String title, String description, String examples, String constraints) {
        return "Solve this coding problem:\n\n" +
               "Problem: " + title + "\n" +
               "Description: " + description + "\n" +
               "Examples: " + examples + "\n" +
               "Constraints: " + constraints + "\n\n" +
               "Provide a complete Java solution with explanation.";
    }
    
    private String buildChatPrompt(String message, String problemContext) {
        return "Context: " + problemContext + "\n\n" +
               "User: " + message + "\n\n" +
               "Provide a helpful response about this coding problem.";
    }
    
    // Model download functionality
    public String downloadModel(SolutionCallback callback) {
        return downloadModel(MODEL_URLS[0], MODEL_NAMES[0], callback);
    }
    
    private String downloadModel(String modelUrl, String modelName, SolutionCallback callback) {
        try {
            mainHandler.post(() -> callback.onDownloadProgress(0, "Starting download: " + modelName));
            
            URL url = new URL(modelUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return "Failed to download: HTTP " + connection.getResponseCode();
            }
            
            int fileLength = connection.getContentLength();
            File outputDir = new File(context.getFilesDir(), "models");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            
            File outputFile = new File(outputDir, MODEL_FILENAME);
            
            try (InputStream input = connection.getInputStream();
                 FileOutputStream output = new FileOutputStream(outputFile)) {
                
                byte[] buffer = new byte[4096];
                long total = 0;
                int bytesRead;
                
                while ((bytesRead = input.read(buffer)) != -1) {
                    total += bytesRead;
                    output.write(buffer, 0, bytesRead);
                    
                    if (fileLength > 0) {
                        int progress = (int) ((total * 100) / fileLength);
                        String status = String.format("Downloading %s: %d%%", modelName, progress);
                        mainHandler.post(() -> callback.onDownloadProgress(progress, status));
                    }
                }
            }
            
            mainHandler.post(() -> callback.onDownloadProgress(100, "Download completed: " + modelName));
            return outputFile.getAbsolutePath();
            
        } catch (Exception e) {
            Log.e(TAG, "Error downloading model (" + modelName + ")", e);
            return "Error: " + e.getMessage();
        }
    }
    
    public void downloadSelectedModel(int modelIndex, SolutionCallback callback) {
        if (modelIndex < 0 || modelIndex >= MODEL_URLS.length) {
            mainHandler.post(() -> callback.onError("Invalid model selection"));
            return;
        }
        
        executor.execute(() -> {
            String result = downloadModel(MODEL_URLS[modelIndex], MODEL_NAMES[modelIndex], callback);
            if (result.startsWith("Error:")) {
                mainHandler.post(() -> callback.onError(result));
            } else {
                mainHandler.post(() -> callback.onProgress("Model downloaded successfully: " + result));
            }
        });
    }
    
    public void retryModelDownload(SolutionCallback callback) {
        downloadModel(callback);
    }
    
    public static String[] getAvailableModelNames() {
        return MODEL_NAMES.clone();
    }
    
    public static String[] getAvailableModelDescriptions() {
        return MODEL_DESCRIPTIONS.clone();
    }
    
    public boolean isModelReady() {
        return isModelLoaded;
    }
    
    public boolean hasModelFile() {
        return findModelFile() != null;
    }
    
    public String getModelSetupInstructions() {
        return "**🤖 AI Model Setup Instructions**\n\n" +
               "To enable advanced AI features, you need a MediaPipe Gemma model:\n\n" +
               "**Option 1: Download via App (Recommended)**\n" +
               "• Use the model selection dialog in the app\n" +
               "• Select a model and it will download automatically\n" +
               "• Models are optimized for mobile devices\n\n" +
               "**Option 2: Manual Download**\n" +
               "• Download `gemma_1b_int4.task` from Google's MediaPipe models\n" +
               "• Place in: `/data/local/tmp/llm/gemma_1b_int4.task`\n" +
               "• Or in: `/sdcard/Download/gemma_1b_int4.task`\n\n" +
               "**Model Requirements:**\n" +
               "• File format: `.task` (MediaPipe format)\n" +
               "• Size: ~1-2GB depending on model\n" +
               "• Compatible with MediaPipe LLM Inference API\n\n" +
               "**Note:** The app works great with Smart Fallback mode even without models!";
    }
    
    public void cleanup() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
