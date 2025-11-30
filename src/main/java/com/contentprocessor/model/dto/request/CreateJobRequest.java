package com.contentprocessor.model.dto.request;

import com.contentprocessor.model.enums.SourceType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateJobRequest {
    @NotNull(message = "Source type is required")
    private SourceType sourceType;

    private String fileId;
    private String url;
    private String textContent;
    private String title;
    private String notes;
}

