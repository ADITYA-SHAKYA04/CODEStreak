package com.example.codestreak.data.model;

/**
 * Task data class - exactly matching Google AI Edge Gallery structure
 */
public class Task {
    public final String id;
    public final String name;
    public final String description;
    
    public Task(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }
}
