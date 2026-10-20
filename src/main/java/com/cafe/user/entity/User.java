package com.cafe.user.entity;

import com.cafe.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class User extends BaseTimeEntity {

    @Id
    @Column(length = 50)
    private String userId;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(length = 512)
    private String refreshToken;

    public static User createGuest() {
        User user = new User();
        user.userId = "GUEST_" + UUID.randomUUID().toString();
        user.email = user.userId + "@guest.local";
        user.password = UUID.randomUUID().toString();
        user.role = Role.GUEST;
        return user;
    }

    public static User create(String userId, String email, String encodedPassword){
        User user = new User();
        user.userId = userId;
        user.email = email;
        user.password = encodedPassword;
        user.role = Role.USER;
        return user;
    }

    public void updateRefreshToken(String refreshToken){
        this.refreshToken = refreshToken;
    }

    public boolean isGuest(){
        return this.role == Role.GUEST;
    }

}
