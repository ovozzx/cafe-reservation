package com.cafe.global.security.jwt;

import com.cafe.user.entity.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. 헤더에서 토큰 꺼내기
        String token = resolveToken(request);

        // 2. 토큰이 있고 유효하면
        if (token != null && jwtTokenProvider.validateToken(token)) {

            // 3. 토큰에서 userId, role 꺼내기
            String userId = jwtTokenProvider.getUserId(token);
            Role role = jwtTokenProvider.getRole(token);
            if(role != null){
                // 4. 인증 객체 만들기
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
                        );

                // 5. SecurityContext에 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }


        }

        // 6. 다음 필터로 넘기기 (토큰이 없거나 틀려도 무조건 실행)
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }


}
