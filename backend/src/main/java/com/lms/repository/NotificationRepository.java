package com.lms.repository;

import com.lms.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByEventId(String eventId);

    Page<Notification> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    Page<Notification> findBySeenFalseOrderByCreatedAtDescIdDesc(Pageable pageable);

    long countBySeenFalse();

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.seen = true WHERE n.seen = false")
    int markAllSeen();
}
