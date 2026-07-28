package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Tìm kiếm bằng username HOẶC email HOẶC số điện thoại
    Optional<User> findByUsernameOrEmailOrPhoneNumber(String username, String email, String phoneNumber);

	Optional<User> findByUsername(String username);
	Optional<User> findByEmail(String email);
	Optional<User> findByPhoneNumber(String phoneNumber);

	boolean existsByUserId(String newId);

	boolean existsByUsername(String string);
}