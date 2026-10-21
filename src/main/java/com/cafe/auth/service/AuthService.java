package com.cafe.auth.service;

import com.cafe.auth.dto.RegisterRequest;
import com.cafe.global.error.BusinessException;
import com.cafe.global.error.ErrorCode;
import com.cafe.user.entity.User;
import com.cafe.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void register(RegisterRequest registerRequest){
        // 1. userId 중복
        if(userRepository.existsById(registerRequest.userId())){
            throw new BusinessException(ErrorCode.DUPLICATE_USER_ID);
        }
        // 2. email 중복
        if(userRepository.existsByEmail(registerRequest.email())){
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        // 3. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(registerRequest.password());
        // 4. User.create(...)
        User user = User.create(registerRequest.userId(), registerRequest.email(), encodedPassword);
        // 5. 저장
        userRepository.save(user);

    }
}
