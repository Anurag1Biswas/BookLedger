package com.lms.dto;

public class AiDTOs {

    public static class ChatRequest {
        private String message;

        public ChatRequest() {}
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class ChatResponse {
        private String reply;
        private String type;

        public ChatResponse() {}
        public ChatResponse(String reply, String type) {
            this.reply = reply;
            this.type = type;
        }
        public String getReply() { return reply; }
        public String getType() { return type; }
        public void setReply(String reply) { this.reply = reply; }
        public void setType(String type) { this.type = type; }
    }

    public static class GenreRequest {
        private String title;
        private String author;

        public GenreRequest() {}
        public String getTitle() { return title; }
        public String getAuthor() { return author; }
        public void setTitle(String title) { this.title = title; }
        public void setAuthor(String author) { this.author = author; }
    }

    public static class RecommendRequest {
        private String bookTitle;

        public RecommendRequest() {}
        public String getBookTitle() { return bookTitle; }
        public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }
    }
}