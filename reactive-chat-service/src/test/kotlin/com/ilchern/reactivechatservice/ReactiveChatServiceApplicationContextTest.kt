package com.ilchern.reactivechatservice

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("local")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ReactiveChatServiceApplicationContextTest {

  @Test
  fun contextLoads() {
  }
}
