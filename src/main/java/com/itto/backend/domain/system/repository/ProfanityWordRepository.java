package com.itto.backend.domain.system.repository;

import com.itto.backend.domain.system.entity.ProfanityWord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProfanityWordRepository extends JpaRepository<ProfanityWord, Long> {

    /**
     * Find all active profanity words
     */
    List<ProfanityWord> findByIsActiveTrue();

    /**
     * Check if a specific word exists in the profanity dictionary
     */
    boolean existsByWord(String word);
}
