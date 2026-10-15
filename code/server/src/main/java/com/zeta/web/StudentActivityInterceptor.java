package com.zeta.web;

import com.zeta.business.auth.JwtTokenService;
import com.zeta.business.entities.user.UserRole;
import com.zeta.business.service.StudentActivityService;
import java.time.Instant;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class StudentActivityInterceptor implements HandlerInterceptor {
  private static final Logger log = LoggerFactory.getLogger(StudentActivityInterceptor.class);
  private final JwtTokenService tokens;
  private final StudentActivityService activities;

  public StudentActivityInterceptor(JwtTokenService tokens, StudentActivityService activities) {
    this.tokens = tokens;
    this.activities = activities;
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
      Object handler, Exception ex) {
    if (!"interactive".equalsIgnoreCase(request.getHeader("X-Student-Activity"))) return;
    if (response.getStatus() >= 500 || response.getStatus() == 401 || response.getStatus() == 403) return;
    String token = AuthHeaderUtils.extractBearerToken(request.getHeader("Authorization"));
    if (token == null) return;
    try {
      JwtTokenService.AccessTokenClaims claims = tokens.parseAccessToken(token);
      if (claims.getRole() != UserRole.STUDENT) return;
      activities.registerActivity(claims.getUserId(), request.getHeader("X-Device-Bind-Id"), Instant.now());
    } catch (RuntimeException e) {
      log.warn("登记学员活动失败：{} {}", request.getMethod(), request.getRequestURI(), e);
    }
  }
}
