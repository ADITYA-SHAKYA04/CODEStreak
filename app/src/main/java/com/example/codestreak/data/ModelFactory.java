package com.example.codestreak.data;

import com.example.codestreak.data.model.Model;
import com.example.codestreak.data.model.Task;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ModelFactory - Creates models EXACTLY from Google AI Edge Gallery's model_allowlist.json
 * Note: These models require Google AI Edge Gallery app for downloading due to authentication
 */
public class ModelFactory {
    
    /**
     * Creates AI coding models using EXACT allowlist from Google AI Edge Gallery
     * Source: https://github.com/google-ai-edge/gallery/blob/main/model_allowlist.json
     * 
     * NOTE: Direct downloads require HuggingFace authentication. Users should:
     * 1. Install Google AI Edge Gallery app (handles auth automatically)
     * 2. Download models there, which will be available system-wide
     * 3. Or use Smart Fallback which works immediately without downloads
     */
    public static List<Model> createCodingModels(String accessToken) {
        List<Model> models = new ArrayList<>();
        
        // Gemma3-1B-IT q4 - From litert-community (EXACT match to Gallery allowlist)
        models.add(new Model(
            "Gemma3-1B-IT q4",
            "gemma3_1b_q4",
            "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/Gemma3-1B-IT_multi-prefill-seq_q4_ekv2048.task",
            "main",
            "Gemma3-1B-IT_multi-prefill-seq_q4_ekv2048.task",
            false,
            null,
            554661246L, // 554MB - exact size from allowlist
            null,
            new ArrayList<>()
        ));
        
        // Qwen2.5-1.5B-Instruct q8 - From litert-community (EXACT match to Gallery allowlist)
        models.add(new Model(
            "Qwen2.5-1.5B-Instruct q8",
            "qwen2_5_1_5b_q8",
            "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
            "main",
            "Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
            false,
            null,
            1625493432L, // 1.6GB - exact size from allowlist
            null,
            new ArrayList<>()
        ));
        
        // Gemma-3n-E2B-it-int4 - Google official preview (EXACT match to Gallery allowlist)
        models.add(new Model(
            "Gemma-3n-E2B-it-int4",
            "gemma3n_e2b_int4",
            "https://huggingface.co/google/gemma-3n-E2B-it-litert-preview/resolve/main/gemma-3n-E2B-it-int4.task",
            "main",
            "gemma-3n-E2B-it-int4.task",
            false,
            null,
            3136226711L, // 3.1GB - exact size from allowlist
            null,
            new ArrayList<>()
        ));
        
        return models;
    }
    
    /**
     * Creates coding task exactly like Google's structure
     */
    public static Task createCodingTask() {
        return new Task(
            "coding_assistance",
            "AI Coding Assistant",
            "Advanced AI models for code generation, debugging, and optimization"
        );
    }
    
    /**
     * Gets MediaPipe-compatible model URLs - EXACT URLs from Google AI Edge Gallery allowlist
     * These require HuggingFace authentication via Google AI Edge Gallery app
     */
    public static String[] getDefaultModelUrls() {
        return new String[] {
            "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/Gemma3-1B-IT_multi-prefill-seq_q4_ekv2048.task",
            "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv1280.task",
            "https://huggingface.co/google/gemma-3n-E2B-it-litert-preview/resolve/main/gemma-3n-E2B-it-int4.task"
        };
    }
}
