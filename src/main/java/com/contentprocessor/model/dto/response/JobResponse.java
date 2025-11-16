package com.contentprocessor.model.dto.response;

import com.contentprocessor.model.enums.JobStatus;
import com.contentprocessor.model.enums.SourceType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for returning job information to the client.
 * This hides internal entity details and only exposes what the client needs.
 *
 * Example response:
 * {
 *   "id": "672bf8a5e4b0c1234567890a",
 *   "sourceType": "VIDEO_FILE",
 *   "status": "PROCESSING",
 *   "title": "Lecture on Spring Boot",
 *   "progress": 45,
 *   "fileId": "672bf8a5e4b0c1234567890b",
 *   "createdAt": "2025-10-25T10:30:00",
 *   "updatedAt": "2025-10-25T10:35:00"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobResponse {

    private String id;
    private SourceType sourceType;
    private JobStatus status;
    private String title;
    private String notes;

    /**
     * Progress percentage (0-100)
     */
    private Integer progress;

    /**
     * Reference to uploaded file (if applicable)
     */
    private String fileId;

    /**
     * URL for web-based content (if applicable)
     */
    private String url;

    /**
     * Error message if job failed
     */
    private String errorMessage;

    /**
     * URL to the generated OneNote page (once completed)
     */
    private String oneNotePageUrl;

    /**
     * Generated notes content (once completed)
     */
    private String generatedNotes;

    /**
     * Transcribed text (for audio/video jobs)
     */
    private String transcribedText;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}