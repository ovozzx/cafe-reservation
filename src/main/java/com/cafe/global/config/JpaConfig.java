package com.cafe.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing // 공통 컬럼 자동 주입 활성화
public class JpaConfig {
}
