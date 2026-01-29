package com.csee.swplus.mileage.auth.service;

import com.csee.swplus.mileage.setting.entity.SwManagerSetting;
import com.csee.swplus.mileage.setting.service.ManagerService;
import com.csee.swplus.mileage.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import com.csee.swplus.mileage.auth.dto.AuthDto;
import com.csee.swplus.mileage.auth.exception.DoNotExistException;
import com.csee.swplus.mileage.auth.util.JwtUtil;
import com.csee.swplus.mileage.user.entity.Users;
import com.csee.swplus.mileage.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Key;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final ManagerService managerService;
    private final UserService userService;

    @Value("${custom.jwt.secret}")
    private String SECRET_KEY;

    // 기존 메소드
    public Users getLoginUser(String uniqueId) {
        return userRepository
                .findByUniqueId(uniqueId)
                .orElseThrow(() -> new DoNotExistException("해당 유저가 없습니다."));
    }

    @Transactional
    public AuthDto login(AuthDto dto) {
        log.info("Login attempt with AuthDto: {}", dto);
        log.info("dto getStudentId: {}", dto.getStudentId());

        Optional<Users> user = userRepository.findByUniqueId(dto.getStudentId());

        log.info("user: {}", user);

        String currentSemester = managerService.getCurrentSemester();

        Key key = JwtUtil.getSigningKey(SECRET_KEY);

        Users loggedInUser;

        if (!user.isPresent()) {
            // 신규 유저 생성
            loggedInUser = Users.from(dto);
            log.info("users.from loggedInUser uniqueId: {}", loggedInUser.getUniqueId());
            loggedInUser.increaseLoginCount();

            userRepository.save(loggedInUser);
        } else {
            // 기존 유저 정보 업데이트
            loggedInUser = user.get();
            loggedInUser.increaseLoginCount();
            userRepository.save(loggedInUser);
        }

        // ✅ 자체 발급 JWT 사용 (프론트에서 받은 token 사용 ❌)
        String accessToken = JwtUtil.createToken(
                loggedInUser.getUniqueId(),
                loggedInUser.getName(),
                loggedInUser.getEmail(),
                key
        );

        String refreshToken = JwtUtil.createRefreshToken(
                loggedInUser.getUniqueId(),
                loggedInUser.getName(),
                key
        );

        log.info("✅ Generated AccessToken: {}", accessToken);
        log.info("✅ Generated RefreshToken: {}", refreshToken);

        // ✅ dto.getToken() 사용 ❌ → 백엔드에서 생성한 토큰 사용
        return AuthDto.builder()
                .token(accessToken)  // 🚀 새로운 JWT 사용
                .studentId(loggedInUser.getUniqueId())
                .studentName(loggedInUser.getName())
                .studentEmail(loggedInUser.getEmail())
                .department(loggedInUser.getDepartment())
                .major1(loggedInUser.getMajor1())
                .major2(loggedInUser.getMajor2())
                .grade(loggedInUser.getGrade())
                .term(loggedInUser.getSemester())
                .studentType(userService.getUserInfo(loggedInUser.getUniqueId()).getStudentType())
                .currentSemester(currentSemester)
                .build();
    }

    // AccessToken 생성 (매개변수 변경)
    public String createAccessToken(String studentId, String studentName, String studentEmail) {
        Key key = JwtUtil.getSigningKey(SECRET_KEY);
        return JwtUtil.createToken(studentId, studentName, studentEmail, key);
    }

    // RefreshToken 생성 (매개변수 변경)
    public String createRefreshToken(String studentId, String studentName) {
        Key key = JwtUtil.getSigningKey(SECRET_KEY);
        return JwtUtil.createRefreshToken(studentId, studentName, key);
    }
}