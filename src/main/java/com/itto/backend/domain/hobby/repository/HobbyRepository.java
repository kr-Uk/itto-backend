package com.itto.backend.domain.hobby.repository;

import com.itto.backend.domain.hobby.entity.Hobby;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HobbyRepository extends JpaRepository<Hobby, Long> {

    List<Hobby> findByIsActiveTrue();

    List<Hobby> findByCategory(String category);
}
