package com.contentprocessor.repository;

import com.contentprocessor.model.entities.FileMetadata;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileMetadataRepository extends MongoRepository<FileMetadata, String> {
    List<FileMetadata> findByUserId(String userId);
    void deleteByIdAndUserId(String id, String userId);
}