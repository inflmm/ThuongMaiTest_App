package com.example.demo.repository;

import com.example.demo.model.Notes;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NotesRepository extends JpaRepository<Notes, Long> {
    Optional<Notes> findByIdAndDeletedFalse(Long id);

    List<Notes> findByDeletedFalse();
}
