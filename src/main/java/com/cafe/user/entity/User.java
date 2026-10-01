package com.cafe.user.entity;

import com.cafe.global.entity.BaseTimeEntity;
import jakarta.persistence.*;

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
        return user;
    }

}
