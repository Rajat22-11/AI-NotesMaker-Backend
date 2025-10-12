package com.contentprocessor.repository;

import com.contentprocessor.model.entities.Job;
import com.contentprocessor.model.enums.JobStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends MongoRepository<Job, String> {
    List<Job> findByUserIdOrderByCreatedAtDesc(String userId);
    List<Job> findByStatus(JobStatus status);
    Optional<Job> findByIdAndUserId(String id, String userId);
}