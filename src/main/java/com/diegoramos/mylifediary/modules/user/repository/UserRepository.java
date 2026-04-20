package com.diegoramos.mylifediary.modules.user.repository;

import com.diegoramos.mylifediary.modules.user.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    
}
