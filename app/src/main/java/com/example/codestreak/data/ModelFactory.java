package com.example.codestreak.data;

import com.example.codestreak.data.model.Model;
import com.example.codestreak.data.model.Task;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ModelFactory - Creates models exactly like Google AI Edge Gallery structure
 */
public class ModelFactory {
    
    /**
     * Creates AI coding models exactly matching Google's pattern
     */
    public static List<Model> createCodingModels(String accessToken) {
        List<Model> models = new ArrayList<>();
        
        // Gemma 2B IT Model - exactly like Google's structure
        models.add(new Model(
            "Gemma 2B IT",
            "gemma_2b_it",
            "https://www.kaggle.com/api/v1/models/google/gemma/gemmaCpp/2b-it-q4_0/download",
            "2b-it-q4_0",
            "model.task",
            false,
            null,
            1200000000L, // ~1.2GB
            accessToken,
            new ArrayList<>()
        ));
        
        // Gemma 2B IT GGUF Format
        models.add(new Model(
            "Gemma 2B IT GGUF",
            "gemma_2b_it_gguf",
            "https://huggingface.co/google/gemma-2b-it-q4_0-gguf/resolve/main/gemma-2b-it-q4_0.gguf",
            "main",
            "gemma-2b-it-q4_0.gguf",
            false,
            null,
            1100000000L, // ~1.1GB
            accessToken,
            new ArrayList<>()
        ));
        
        // TensorFlow Hub Model
        models.add(new Model(
            "Gemma 2B TensorFlow",
            "gemma_2b_tensorflow",
            "https://tfhub.dev/google/gemma/2b/1?tf-hub-format=compressed",
            "1",
            "model.tar.gz",
            true,
            "extracted_model",
            1500000000L, // ~1.5GB
            accessToken,
            Arrays.asList(
                new Model.ExtraDataFile(
                    "https://tfhub.dev/google/gemma/2b/1/assets/tokenizer.model",
                    "tokenizer.model",
                    2000000L // ~2MB
                )
            )
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
     * Gets default model URLs exactly matching our current fallback structure
     */
    public static String[] getDefaultModelUrls() {
        return new String[] {
            "https://www.kaggle.com/api/v1/models/google/gemma/gemmaCpp/2b-it-q4_0/download",
            "https://tfhub.dev/google/gemma/2b/1?tf-hub-format=compressed",
            "https://huggingface.co/google/gemma-2b-it-resolve/main/model.task",
            "https://storage.googleapis.com/mediapipe-models/text_classifier/bert_classifier/float32/1/bert_classifier.tflite"
        };
    }
}
