package com.lms.integration;

import com.lms.dto.LibraryDTOs.IssueRequest;
import com.lms.dto.LibraryDTOs.SubmitRequest;
import com.lms.model.Book;
import com.lms.model.Member;
import com.lms.repository.BookRepository;
import com.lms.repository.MemberRepository;
import com.lms.repository.NotificationRepository;
import com.lms.service.IssueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Real producer -> real (embedded) broker -> real consumer -> real DB write.
 * issueBook / submitBook commit their transaction, the AFTER_COMMIT listener publishes to
 * "book-events", and the consumer persists a notification asynchronously.
 */
@SpringBootTest(properties = {
        "app.kafka.enabled=true",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.auto-offset-reset=earliest"})
@EmbeddedKafka(partitions = 3)
@ActiveProfiles("test")
@DirtiesContext
class KafkaNotificationFlowTest {

    @Autowired IssueService issueService;
    @Autowired BookRepository bookRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired NotificationRepository notificationRepository;

    @Test
    void issuingAndReturningABook_eventuallyCreatesTwoNotifications() {
        Book book = new Book();
        book.setTitle("Kafka Flow Book");
        book.setAuthor("Test");
        book.setQuantity(1);
        book.setStatus("Available");
        book.setPrice(100);
        book = bookRepository.save(book);
        Member member = new Member();
        member.setName("Kafka Flow Member");
        member.setAddress("Test");
        member.setActive(true);
        member.setBooksIssued(0);
        member = memberRepository.save(member);
        long before = notificationRepository.count();

        IssueRequest issue = new IssueRequest();
        issue.setBookId(book.getId());
        issue.setMemberId(member.getId());
        issueService.issueBook(issue);

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertTrue(notificationRepository.count() >= before + 1, "BOOK_ISSUED notification not created"));

        SubmitRequest submit = new SubmitRequest();
        submit.setBookId(book.getId());
        submit.setMemberId(member.getId());
        issueService.submitBook(submit);

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertTrue(notificationRepository.count() >= before + 2, "BOOK_RETURNED notification not created"));
    }
}
