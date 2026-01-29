package com.csee.swplus.mileage.auth.filter;

import javax.servlet.http.Cookie;

import com.csee.swplus.mileage.auth.exception.WrongTokenException;
import lombok.extern.slf4j.Slf4j;
import com.csee.swplus.mileage.auth.exception.DoNotLoginException;
import com.csee.swplus.mileage.auth.service.AuthService;
import com.csee.swplus.mileage.auth.util.JwtUtil;
import com.csee.swplus.mileage.user.entity.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Key;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {

    private final AuthService authService;
    private final Key SECRET_KEY;

    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
            "/api/mileage/auth/login$",
            "/mileage/api/mileage/auth/login$",
            "/api/mileage/auth/logout$",
            "/mileage/api/mileage/auth/logout$"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        log.debug("🚀 JwtTokenFilter: 요청 URI: {}", requestURI);

        if (isExcludedPath(requestURI)) {
            log.debug("🔸 JwtTokenFilter: 제외된 경로입니다. 필터 체인 계속 진행.");
            filterChain.doFilter(request, response);
            return;
        }

        Cookie[] cookies = request.getCookies();
        String accessToken = null;
        String refreshToken = null;

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    accessToken = cookie.getValue();
                }
                if ("refreshToken".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                }
            }
        }

//        if (accessToken == null) {
//            log.error("❌ JwtTokenFilter: accessToken 쿠키가 존재하지 않습니다. 로그인 필요.");
//            throw new DoNotLoginException();
//        }

        try {
            String userId = JwtUtil.getUserId(accessToken, SECRET_KEY);
            Users loginUser = authService.getLoginUser(userId);

            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(loginUser.getUniqueId(), null, null);
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        } catch (WrongTokenException e) {
            log.info("❗ {}", e.getMessage());

            // accessToken이 만료된 경우, refreshToken으로 재발급 시도
            // JwtTokenFilter.java에서 리프레시 토큰 처리 부분 수정:
            if (refreshToken != null) {
                try {
                    String userId = JwtUtil.getUserId(refreshToken, SECRET_KEY);
                    Users loginUser = authService.getLoginUser(userId);

                    // 새로운 만료 시간으로 액세스 토큰 생성
                    String newAccessToken = authService.createAccessToken(
                            loginUser.getUniqueId(),
                            loginUser.getName(),
                            loginUser.getEmail()
                    );

                    // 새 액세스 토큰을 쿠키로 설정
                    Cookie newAccessTokenCookie = new Cookie("accessToken", newAccessToken);
                    newAccessTokenCookie.setHttpOnly(true);
                    newAccessTokenCookie.setPath("/");
                    newAccessTokenCookie.setMaxAge(7200); // 토큰 만료 시간과 일치 (2시간)
                    response.addCookie(newAccessTokenCookie);

                    // 토큰 리프레시 확인용 로깅 추가
                    log.info("🔄 사용자 {} 액세스 토큰 리프레시 성공", loginUser.getName());

                    // 인증 설정
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(loginUser.getUniqueId(), null, null);
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                } catch (Exception refreshEx) {
                    // 더 상세한 로깅을 포함한 개선된 예외 처리
                    log.error("❌ 토큰 리프레시 실패: {}", refreshEx.getMessage());
                    throw new DoNotLoginException();
                }
            } else {
                log.error("❌ refreshToken이 존재하지 않습니다. 로그인이 필요합니다.");
                throw new DoNotLoginException();
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isExcludedPath(String requestURI) {
        return EXCLUDED_PATHS.stream().anyMatch(requestURI::matches);
    }
}