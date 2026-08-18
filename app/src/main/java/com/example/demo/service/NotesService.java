package com.example.demo.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.Notes;
import com.example.demo.repository.NotesRepository;

import jakarta.transaction.Transactional;

@Service
public class NotesService {
    @Autowired
    private NotesRepository notesRepository;

    /**
     * Lấy tất cả các ghi chú có sẵn (chưa bị xóa)
     * 
     * @return danh sách các ghi chú có sẵn
     */
    public List<Notes> getAllAvailableNotes() {
        return notesRepository.findByDeletedFalse();
    }

    @Transactional
    public Notes createNote(Notes note) {
        return notesRepository.save(note);
    }

    @Transactional
    public Notes updateNote(Notes note) {
        return notesRepository.save(note);
    }

    @Transactional
    public void deleteNote(Long id) {
        Notes note = notesRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Note với ID: " + id));

        note.setDeleted(true);
        notesRepository.save(note);
    }
}
