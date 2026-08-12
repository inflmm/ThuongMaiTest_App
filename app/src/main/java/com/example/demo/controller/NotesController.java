package com.example.demo.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.Notes;
import com.example.demo.service.NotesService;

@RestController
@RequestMapping("/api/notes")
public class NotesController {

    private final NotesService notesService;

    public NotesController(NotesService notesService) {
        this.notesService = notesService;
    }

    @GetMapping
    public List<Notes> getAvailableNotes() {
        return notesService.getAllAvailableNotes();
    }

    @PostMapping
    public ResponseEntity<Notes> createNote(@RequestBody Notes note) {
        Notes createdNote = notesService.createNote(note);
        return ResponseEntity.ok(createdNote);
    }

    @PutMapping
    public ResponseEntity<Notes> updateNote(@RequestBody Notes note) {
        Notes updatedNote = notesService.updateNote(note);
        return ResponseEntity.ok(updatedNote);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteNote(@PathVariable Long id) {
        try {
            notesService.deleteNote(id);
            return ResponseEntity.ok("Xóa thành công ghi chú ID = " + id);
        } catch (RuntimeException e) {
            // Trả về 404 Not Found thay vì 200 OK
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}
