package com.heypudu.ms_auth.repository;

import com.heypudu.ms_auth.model.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuthUserRepository extends JpaRepository<AuthUser, UUID> {

    Optional<AuthUser> findByCognitoSub(UUID cognitoSub);

    boolean existsByCognitoSub(UUID cognitoSub);

    Optional<AuthUser> findByEmail(String email);

    boolean existsByEmail(String email);
}