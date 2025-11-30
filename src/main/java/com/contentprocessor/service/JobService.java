package com.contentprocessor.service;

import com.contentprocessor.exception.BadRequestException;
import com.contentprocessor.exception.ResourceNotFoundException;
import com.contentprocessor.model.dto.request.CreateJobRequest;
import com.contentprocessor.model.dto.response.JobResponse;
import com.contentprocessor.model.entities.FileMetadata;
import com.contentprocessor.model.entities.Job;
import com.contentprocessor.model.entities.User;
import com.contentprocessor.model.enums.JobStatus;
import com.contentprocessor.repository.FileMetadataRepository;
import com.contentprocessor.repository.JobRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final FileMetadataRepository fileMetadataRepository;

    public JobService(JobRepository jobRepository, FileMetadataRepository fileMetadataRepository) {
        this.jobRepository = jobRepository;
        this.fileMetadataRepository = fileMetadataRepository;
    }

    public JobResponse createJob(CreateJobRequest request, User user) {
        // Validate based on source type
        switch (request.getSourceType()) {
            case VIDEO_FILE, AUDIO_FILE, PDF_FILE:
                if (request.getFileId() == null || request.getFileId().isEmpty()) {
                    throw new BadRequestException("Video/Audio/PDF source requires a fileId");
                }
                // Load and validate file ownership
                FileMetadata fileMetadata = fileMetadataRepository.findById(request.getFileId())
                        .orElseThrow(() -> new ResourceNotFoundException("File not found"));

                if (!fileMetadata.getUploadedBy().getId().equals(user.getId())) {
                    throw new BadRequestException("You don't have permission to use this file");
                }

                Job job = Job.builder()
                        .user(user)
                        .sourceType(request.getSourceType())
                        .fileMetadata(fileMetadata)
                        .title(request.getTitle())
                        .status(JobStatus.PENDING)
                        .progress(0)
                        .build();

                return mapToResponse(jobRepository.save(job));

            case YOUTUBE:
                if (request.getUrl() == null || request.getUrl().isEmpty()) {
                    throw new BadRequestException("YouTube source requires a URL");
                }
                if (!isValidUrl(request.getUrl())) {
                    throw new BadRequestException("Invalid URL format");
                }

                Job youtubeJob = Job.builder()
                        .user(user)
                        .sourceType(request.getSourceType())
                        .url(request.getUrl())
                        .title(request.getTitle())
                        .status(JobStatus.PENDING)
                        .progress(0)
                        .build();

                return mapToResponse(jobRepository.save(youtubeJob));

            case TEXT:
                if (request.getTextContent() == null || request.getTextContent().isEmpty()) {
                    throw new BadRequestException("Text source requires textContent");
                }

                Job textJob = Job.builder()
                        .user(user)
                        .sourceType(request.getSourceType())
                        .textContent(request.getTextContent())
                        .title(request.getTitle())
                        .status(JobStatus.PENDING)
                        .progress(0)
                        .build();

                return mapToResponse(jobRepository.save(textJob));

            default:
                throw new BadRequestException("Unknown source type: " + request.getSourceType());
        }
    }

    public JobResponse getJobById(String jobId, User user) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        if (!job.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You don't have permission to access this job");
        }

        return mapToResponse(job);
    }

    public Page<JobResponse> getUserJobs(User user, Pageable pageable) {
        Page<Job> jobs = jobRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        return jobs.map(this::mapToResponse);
    }

    public Page<JobResponse> getUserJobsByStatus(User user, JobStatus status, Pageable pageable) {
        Page<Job> jobs = jobRepository.findByUserAndStatusOrderByCreatedAtDesc(user, status, pageable);
        return jobs.map(this::mapToResponse);
    }

    public void updateJobStatus(String jobId, JobStatus newStatus, int progress) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        job.setStatus(newStatus);
        job.setProgress(progress);
        jobRepository.save(job);
    }

    public void markJobAsFailed(String jobId, String errorMessage) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        job.setStatus(JobStatus.FAILED);
        job.setErrorMessage(errorMessage);
        job.setProgress(0);
        jobRepository.save(job);
    }

    public void deleteJob(String jobId, User user) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        if (!job.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You don't have permission to delete this job");
        }

        jobRepository.delete(job);
    }

    private JobResponse mapToResponse(Job job) {
        return JobResponse.builder()
                .id(job.getId())
                .sourceType(job.getSourceType())
                .status(job.getStatus())
                .title(job.getTitle())
                .progress(job.getProgress())
                .fileId(job.getFileMetadata() != null ? job.getFileMetadata().getId() : null)
                .url(job.getUrl())
                .errorMessage(job.getErrorMessage())
                .oneNotePageUrl(job.getOneNotePageUrl())
                .build();
    }

    private boolean isValidUrl(String url) {
        try {
            new java.net.URL(url).toURI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
