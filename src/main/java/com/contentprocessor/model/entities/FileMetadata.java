package com.contentprocessor.model.entities;

import com.contentprocessor.model.enums.FileType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "files")
public class FileMetadata {

    @Id
    private String id;
    private String originalFileName;
    private String storedFileName; // Unique name in storage
    private FileType fileType;
    private String localFilePath;
    private String cloudinaryUrl; // Or S3 Key
    private String cloudinaryPublicId;
    private Long fileSize;
    private String contentType;

    @DBRef
    private User uploadedBy;

    @Builder.Default
    private boolean processed = false;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;


    public String getFilePath() {
        if (cloudinaryUrl != null) {
            return cloudinaryUrl;
        }
        return localFilePath;
    }

    public boolean isCloudStored() {
        return cloudinaryUrl != null;
    }

    public boolean isLocalStored() {
        return localFilePath != null;
    }
}