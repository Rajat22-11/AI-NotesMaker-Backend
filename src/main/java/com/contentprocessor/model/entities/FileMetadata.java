package com.contentprocessor.model.entities;

import com.contentprocessor.model.enums.FileType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "files")
public class FileMetadata {

    @Id
    private String id;
    private String originalFilename;
    private String storedFilename; // Unique name in storage
    private FileType fileType;
    private String cloudinaryUrl; // Or S3 Key
    private long fileSize;
    private String mimeType;
    private String userId;
    private boolean processed = false;

    @CreatedDate
    private Instant uploadedAt;
}