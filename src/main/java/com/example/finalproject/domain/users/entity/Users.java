package com.example.finalproject.domain.users.entity;

import com.example.finalproject.domain.users.UserRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Users {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false, unique = true, length = 100)

    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 100)

    private String name;

    @Column(nullable = false, length = 100)
    private String nickname;

    @Column(name = "phone_number", length = 15)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(name = "social_login", nullable = false)
    private boolean socialLogin = false;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @Column(nullable = false, length = 100)
    private String address;

    @Column(name = "address_detail", length = 255)
    private String addressDetail;

    @Column(name = "zip_code", length = 10)
    private String zipCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Users(String email, String password, String name, String nickname,
                 String phoneNumber, UserRole role, boolean socialLogin, boolean deleted,
                 String address, String addressDetail, String zipCode) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.nickname = nickname;
        this.phoneNumber = phoneNumber;
        this.role = role;
        this.socialLogin = socialLogin;
        this.deleted = deleted;
        this.address = address;
        this.addressDetail = addressDetail;
        this.zipCode = zipCode;
    }

    @PrePersist
    private void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.role == null) this.role = UserRole.USER;
    }

    @PreUpdate
    private void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 편의 getter (기존 코드 호환)
    public Long getUserId() { return id; }
    public String getUsername() { return email; }

    // ==== 도메인 변경 메서드 ====
    public void changePassword(String encodedPassword) { this.password = encodedPassword; }
    public void changeNickname(String nickname)        { this.nickname = nickname; }
    public void changePhoneNumber(String phoneNumber)  { this.phoneNumber = phoneNumber; }
    public void changeAddress(String address)          { this.address = address; }
    public void changeAddressDetail(String detail)     { this.addressDetail = detail; }
    public void changeZipCode(String zipCode)          { this.zipCode = zipCode; }
}
