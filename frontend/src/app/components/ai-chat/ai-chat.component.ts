import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../services/api.service';
import { ChatMessage } from '../../models/models';

@Component({
  selector: 'app-ai-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ai-chat.component.html',
  styleUrls: ['./ai-chat.component.css']
})
export class AiChatComponent implements OnInit {

  @ViewChild('chatBody') chatBody!: ElementRef;

  messages: ChatMessage[] = [];
  userInput = '';
  isLoading = false;
  isOpen = false;

  suggestedQuestions = [
    'How many books are available?',
    'Which members have books issued?',
    'How many members do we have?',
    'Show me all fantasy books',
    'Who has the most books issued?'
  ];

  constructor(private api: ApiService) {}

  ngOnInit() {
    // Welcome message
    this.messages.push({
      role: 'bot',
      text: '👋 Hi! I am LibraryBot. Ask me anything about the library — members, books, availability, or anything else!',
      time: this.getTime()
    });
  }

  toggleChat() {
    this.isOpen = !this.isOpen;
  }

  sendMessage(text?: string) {
    const message = text || this.userInput.trim();
    if (!message || this.isLoading) return;

    // Add user message
    this.messages.push({
      role: 'user',
      text: message,
      time: this.getTime()
    });

    this.userInput = '';
    this.isLoading = true;
    this.scrollToBottom();

    // Call AI
    this.api.chatWithAI(message).subscribe({
      next: res => {
        this.messages.push({
          role: 'bot',
          text: res.reply,
          time: this.getTime()
        });
        this.isLoading = false;
        this.scrollToBottom();
      },
      error: () => {
        this.messages.push({
          role: 'bot',
          text: 'Sorry, I could not connect to the AI right now. Please try again.',
          time: this.getTime()
        });
        this.isLoading = false;
        this.scrollToBottom();
      }
    });
  }

  onKeyPress(event: KeyboardEvent) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  clearChat() {
    this.messages = [{
      role: 'bot',
      text: '👋 Chat cleared! How can I help you?',
      time: this.getTime()
    }];
  }

  private scrollToBottom() {
    setTimeout(() => {
      if (this.chatBody) {
        this.chatBody.nativeElement.scrollTop =
          this.chatBody.nativeElement.scrollHeight;
      }
    }, 100);
  }

  private getTime(): string {
    return new Date().toLocaleTimeString([], {
      hour: '2-digit', minute: '2-digit'
    });
  }
}