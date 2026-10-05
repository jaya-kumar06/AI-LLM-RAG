package com.example.ragchatbot.repository;

import com.example.ragchatbot.model.ChatHistory;
import com.example.ragchatbot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Long> {
    
    /**
     * Get chat history for a specific session
     */
    List<ChatHistory> findBySessionIdOrderByCreatedAtAsc(String sessionId);
    
    /**
     * Get all sessions for a user (for history sidebar)
     */
    @Query("SELECT DISTINCT ch.sessionId FROM ChatHistory ch WHERE ch.user = :user ORDER BY MAX(ch.createdAt) DESC")
    List<String> findDistinctSessionIdsByUser(@Param("user") User user);
    
    /**
     * Get recent messages for a user (last N messages across all sessions)
     */
    List<ChatHistory> findTop20ByUserOrderByCreatedAtDesc(User user);
    
    /**
     * Get messages for a session with limit
     */
    @Query("SELECT ch FROM ChatHistory ch WHERE ch.sessionId = :sessionId ORDER BY ch.createdAt DESC LIMIT :limit")
    List<ChatHistory> findRecentBySessionId(@Param("sessionId") String sessionId, @Param("limit") int limit);
    
    /**
     * Delete all messages in a session
     */
    void deleteBySessionId(String sessionId);
    
    /**
     * Count messages per user (for analytics)
     */
    long countByUser(User user);
}
