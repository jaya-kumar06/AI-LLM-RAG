package com.example.ragchatbot.repository;

import com.example.ragchatbot.model.DocumentHash;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentHashRepository extends JpaRepository<DocumentHash, Long> {
    
    /**
     * Check if document with this hash already exists
     */
    boolean existsByContentHash(String contentHash);
    
    /**
     * Find document by hash
     */
    Optional<DocumentHash> findByContentHash(String contentHash);
    
    /**
     * Find document by filename
     */
    Optional<DocumentHash> findByFilename(String filename);
}
