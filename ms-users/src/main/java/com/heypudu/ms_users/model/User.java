package com.heypudu.ms_users.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "cognito_sub", nullable = false, unique = true, updatable = false)
    private UUID cognitoSub;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "bio", length = 300)
    private String bio;

    @Column(name = "avatar_key", length = 500)
    private String avatarKey;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "follower_count", nullable = false)
    private int followerCount;

    @Column(name = "following_count", nullable = false)
    private int followingCount;

    @Column(name = "post_count", nullable = false)
    private int postCount;

    @Column(name = "username_updated_at")
    private Instant usernameUpdatedAt;

    @Column(name = "anonymized_at")
    private Instant anonymizedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public User() {
    }

    public User(UUID cognitoSub,
                String name,
                String username,
                String bio,
                String avatarKey,
                Instant createdAt) {
        this.cognitoSub = cognitoSub;
        this.name = name;
        this.username = username;
        this.bio = bio;
        this.avatarKey = avatarKey;
        this.isVerified = false;
        this.status = "ACTIVE";
        this.followerCount = 0;
        this.followingCount = 0;
        this.postCount = 0;
        this.usernameUpdatedAt = null;
        this.anonymizedAt = null;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCognitoSub() {
        return cognitoSub;
    }

    public void setCognitoSub(UUID cognitoSub) {
        this.cognitoSub = cognitoSub;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getAvatarKey() {
        return avatarKey;
    }

    public void setAvatarKey(String avatarKey) {
        this.avatarKey = avatarKey;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getFollowerCount() {
        return followerCount;
    }

    public void setFollowerCount(int followerCount) {
        this.followerCount = followerCount;
    }

    public int getFollowingCount() {
        return followingCount;
    }

    public void setFollowingCount(int followingCount) {
        this.followingCount = followingCount;
    }

    public int getPostCount() {
        return postCount;
    }

    public void setPostCount(int postCount) {
        this.postCount = postCount;
    }

    public Instant getUsernameUpdatedAt() {
        return usernameUpdatedAt;
    }

    public void setUsernameUpdatedAt(Instant usernameUpdatedAt) {
        this.usernameUpdatedAt = usernameUpdatedAt;
    }

    public Instant getAnonymizedAt() {
        return anonymizedAt;
    }

    public void setAnonymizedAt(Instant anonymizedAt) {
        this.anonymizedAt = anonymizedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}