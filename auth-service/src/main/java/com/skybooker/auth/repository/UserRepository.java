package com.skybooker.auth.repository;

import com.skybooker.auth.entity.User;
import com.skybooker.auth.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUserId(String userId);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    boolean existsByPassportNumber(String passportNumber);
    List<User> findAllByRole(Role role);
    Optional<User> findByPhone(String phone);
    Optional<User> findByPassportNumber(String passportNumber);
    void deleteByUserId(String userId);
}
