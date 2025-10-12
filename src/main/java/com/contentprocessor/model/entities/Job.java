package com.contentprocessor.model.entities;

import com.contentprocessor.model.enums.JobStatus;
import com.contentprocessor.model.enums.SourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "jobs")
public class Job {

    @Id
    private String id;

    @DBRef
    private User user;

    private SourceType sourceType;
    private String sourceInput; // URL or text content
    private String fileId; // Reference to FileMetadata if uploaded

    private JobStatus status;
    private Integer progress = 0; // Percentage 0-100

    private String oneNotePageUrl; // The final result
    private String errorMessage;

    @CreatedDate
    private Instant createdAt;
    private Instant completedAt;

    @LastModifiedDate
    private Instant updatedAt;
}