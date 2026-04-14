package org.example.ailoldraftingcheck.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * One row per ChatGPT call. Lets you (or the teacher) inspect the H2 console
 * and see how much the project has cost.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class ApiUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String endpoint;        // "draft" or "coach"
    private int promptTokens;
    private int completionTokens;
    private int totalTokens;

    @CreationTimestamp
    private LocalDateTime created;

    public ApiUsage(String endpoint, int promptTokens, int completionTokens, int totalTokens) {
        this.endpoint = endpoint;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
    }
}
