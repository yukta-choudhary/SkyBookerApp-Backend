package com.skybooker.auth.repository;

import com.skybooker.auth.entity.User;
import com.skybooker.auth.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByPhone(String phone);
    Optional<User> findByPassportNumber(String passportNumber);
    List<User> findAllByRole(Role role);
}