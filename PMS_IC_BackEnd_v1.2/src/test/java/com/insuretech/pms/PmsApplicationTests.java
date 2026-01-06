package com.insuretech.pms;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PmsApplicationTests {

    @Test
    void contextLoads() {
        // 기본 스프링 컨텍스트 로드 검증
    }
}
