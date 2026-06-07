package com.zeta.business.auth;

import com.zeta.business.user.User;
import com.zeta.business.user.UserRole;
import com.zeta.business.user.UserRepository;
import com.zeta.web.AuthHeaderUtils;
import com.zeta.business.auth.AuthTokenResponse;
import com.zeta.business.user.UserProfileResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenStore refreshTokenStore;

    public AuthService(
            UserRepository userRepository,
            JwtTokenService jwtTokenService,
            RefreshTokenStore refreshTokenStore) {
        this.userRepository = userRepository;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenStore = refreshTokenStore;
    }

    public AuthTokenResponse login(String username, String password) {
        User user = authenticate(username, password);
        return issueTokens(user);
    }

    public AuthTokenResponse refresh(String refreshToken) {
        Long userId = refreshTokenStore.resolve(refreshToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "刷新令牌无效或已过期"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在"));
        String newRefresh = refreshTokenStore.rotate(refreshToken, user.getId());
        return buildTokenResponse(user, jwtTokenService.createAccessToken(user), newRefresh);
    }

    public UserProfileResponse profile(String authorization) {
        User user = requireUser(authorization);
        return new UserProfileResponse(
                user.getUsername(),
                user.getDisplayName(),
                user.getRole(),
                homePath(user.getRole()));
    }

    public User requireUser(String authorization) {
        String accessToken = AuthHeaderUtils.extractBearerToken(authorization);
        if (accessToken == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录或访问令牌缺失");
        }
        JwtTokenService.AccessTokenClaims claims = jwtTokenService.parseAccessToken(accessToken);
        return userRepository.findById(claims.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在"));
    }

    public User requireRole(String authorization, UserRole role) {
        User user = requireUser(authorization);
        if (user.getRole() != role) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权访问该资源");
        }
        return user;
    }

    public void logout(String refreshToken) {
        if (refreshToken != null) {
            refreshTokenStore.revoke(refreshToken);
        }
    }

    public void changePassword(String authorization, String oldPassword, String newPassword) {
        User user = requireUser(authorization);
        if (!user.getPassword().equals(oldPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "原密码不正确");
        }
        if (oldPassword.equals(newPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "新密码不能与原密码相同");
        }
        user.setPassword(newPassword);
        userRepository.save(user);
    }

    private User authenticate(String username, String password) {
        User user = userRepository.findByUsername(username.trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误"));
        if (!user.getPassword().equals(password)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        return user;
    }

    private AuthTokenResponse issueTokens(User user) {
        String accessToken = jwtTokenService.createAccessToken(user);
        String refreshToken = refreshTokenStore.issue(user.getId());
        return buildTokenResponse(user, accessToken, refreshToken);
    }

    private AuthTokenResponse buildTokenResponse(User user, String accessToken, String refreshToken) {
        return new AuthTokenResponse(
                accessToken,
                refreshToken,
                jwtTokenService.accessExpiresInSeconds(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole(),
                homePath(user.getRole()));
    }

    public static String homePath(UserRole role) {
        switch (role) {
            case STUDENT:
                return "/student";
            case TEACHER:
                return "/teacher";
            case ADMIN:
                return "/admin";
            default:
                return "/";
        }
    }
}
