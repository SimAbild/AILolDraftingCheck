package org.example.ailoldraftingcheck.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
public class ApiUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String endpoint;
    private int promptTokens;
    private int completionTokens;
    private int totalTokens;

    @CreationTimestamp
    LocalDateTime created;

    public ApiUsage() {}

    public ApiUsage(String endpoint, int promptTokens, int completionTokens, int totalTokens) {
        this.endpoint = endpoint;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
    }
}
