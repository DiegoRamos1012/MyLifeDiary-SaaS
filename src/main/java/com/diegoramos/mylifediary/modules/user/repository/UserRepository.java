package com.diegoramos.mylifediary.modules.user.repository;

import com.diegoramos.mylifediary.modules.user.domain.entity.User;
import com.diegoramos.mylifediary.modules.user.domain.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmailIgnoreCase(String email);

    Page<User> findByFullNameContainingIgnoreCase(String fullName, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update User u
               set u.status = :inactiveStatus
             where u.status = :pendingStatus
               and u.deletionRequestedAt is not null
               and u.deletionRequestedAt < :threshold
            """)
    int markPendingDeletionUsersAsInactive(
            @Param("pendingStatus") UserStatus pendingStatus,
            @Param("inactiveStatus") UserStatus inactiveStatus,
            @Param("threshold") Instant threshold
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from User u
             where u.status = :inactiveStatus
               and u.deletionRequestedAt is not null
               and u.deletionRequestedAt < :threshold
            """)
    int hardDeleteInactiveUsersBefore(
            @Param("inactiveStatus") UserStatus inactiveStatus,
            @Param("threshold") Instant threshold
    );
}
