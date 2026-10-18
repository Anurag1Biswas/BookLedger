package com.lms.service;

import com.lms.repository.BookRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.IssueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class AiService {

    @Value("${ollama.base-url}")
    private String baseUrl;

    @Value("${ollama.model}")
    private String model;

    @Autowired private BookRepository bookRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private IssueRepository issueRepository;

    private final WebClient webClient = WebClient.builder().build();

    // =============================================
    // Core method — calls Ollama local API
    // =============================================
    private String callOllama(String systemPrompt, String userMessage) {
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user",   "content", userMessage)
                    ),
                    "stream", false
            );

            Map response = webClient.post()
                    .uri(baseUrl + "/api/chat")
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            Map message = (Map) response.get("message");
            return (String) message.get("content");

        } catch (Exception e) {
            return "Sorry, I could not process your request. Make sure Ollama is running.";
        }
    }

    // =============================================
    // 1. Library Chatbot
    // =============================================
    public String chat(String userMessage) {
        long totalMembers   = memberRepository.findByActiveTrue().size();
        long totalBooks     = bookRepository.count();
        long activeIssues   = issueRepository.findByStatus(1).size();
        long availableBooks = bookRepository.findAll()
                .stream().filter(b -> b.getQuantity() > 0).count();

        String bookList = bookRepository.findAll().stream()
                .map(b -> b.getTitle() + " by " + b.getAuthor()
                        + " (Genre: " + b.getGenre()
                        + ", Qty: "   + b.getQuantity() + ")")
                .reduce("", (a, b) -> a + "\n- " + b);

        String memberList = memberRepository.findByActiveTrue().stream()
                .map(m -> m.getName()
                        + " (Type: "      + m.getMemberType()
                        + ", Books Out: " + m.getBooksIssued() + ")")
                .reduce("", (a, b) -> a + "\n- " + b);

        String systemPrompt =
                "You are LibraryBot, an AI assistant for a library management system. " +
                        "Answer questions accurately and concisely under 150 words. Be friendly.\n\n" +
                        "=== LIVE LIBRARY DATA ===\n" +
                        "Total Members: "   + totalMembers   + "\n" +
                        "Total Books: "     + totalBooks     + "\n" +
                        "Available Books: " + availableBooks + "\n" +
                        "Active Issues: "   + activeIssues   + "\n\n" +
                        "BOOKS:\n"   + bookList   + "\n\n" +
                        "MEMBERS:\n" + memberList;

        return callOllama(systemPrompt, userMessage);
    }

    // =============================================
    // 2. Book Recommendations
    // =============================================
    public String recommendBooks(String bookTitle) {
        String existingBooks = bookRepository.findAll().stream()
                .map(b -> b.getTitle() + " by " + b.getAuthor())
                .reduce("", (a, b) -> a + "\n- " + b);

        String systemPrompt =
                "You are a book recommendation expert. " +
                        "Suggest exactly 3 books similar to the one mentioned. " +
                        "Do NOT recommend these books already in the library:\n" +
                        existingBooks + "\n\n" +
                        "Format as a numbered list: Title by Author — one sentence why. " +
                        "Keep under 100 words.";

        return callOllama(systemPrompt,
                "Recommend books similar to: " + bookTitle);
    }

    // =============================================
    // 3. Auto Genre Classification
    // =============================================
    public String classifyGenre(String title, String author) {
        String systemPrompt =
                "You are a book genre classifier. " +
                        "Reply with ONLY one genre word from this list: " +
                        "Fantasy, Thriller, Classic, Dystopian, Romance, " +
                        "Science Fiction, Horror, Mystery, Biography, History, Self-Help. " +
                        "No explanation. Just the genre word.";

        return callOllama(systemPrompt,
                "Book: " + title + " by " + author).trim();
    }

    // =============================================
    // 4. Overdue Message Generator
    // =============================================
    public String generateOverdueMessage(String memberName,
                                         String bookTitle,
                                         long daysOverdue) {
        String systemPrompt =
                "You are a friendly library assistant. " +
                        "Write a short polite overdue reminder under 60 words. " +
                        "Be firm but friendly.";

        return callOllama(systemPrompt,
                "Member: " + memberName +
                        ", Book: " + bookTitle +
                        ", Days overdue: " + daysOverdue);
    }
}