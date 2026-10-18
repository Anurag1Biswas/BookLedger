package com.lms.controller;

import com.lms.dto.AiDTOs.*;
import com.lms.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Autowired private AiService aiService;

    // Chatbot endpoint
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest req) {
        String reply = aiService.chat(req.getMessage());
        return ResponseEntity.ok(new ChatResponse(reply, "chat"));
    }

    // Book recommendations endpoint
    @PostMapping("/recommend")
    public ResponseEntity<ChatResponse> recommend(
            @RequestBody RecommendRequest req) {
        String reply = aiService.recommendBooks(req.getBookTitle());
        return ResponseEntity.ok(new ChatResponse(reply, "recommendation"));
    }

    // Auto genre classification endpoint
    @PostMapping("/classify-genre")
    public ResponseEntity<ChatResponse> classifyGenre(
            @RequestBody GenreRequest req) {
        String genre = aiService.classifyGenre(req.getTitle(), req.getAuthor());
        return ResponseEntity.ok(new ChatResponse(genre, "genre"));
    }

    // Overdue message generator endpoint
    @PostMapping("/overdue-message")
    public ResponseEntity<ChatResponse> overdueMessage(
            @RequestParam String memberName,
            @RequestParam String bookTitle,
            @RequestParam long daysOverdue) {
        String msg = aiService.generateOverdueMessage(
                memberName, bookTitle, daysOverdue);
        return ResponseEntity.ok(new ChatResponse(msg, "overdue"));
    }
}