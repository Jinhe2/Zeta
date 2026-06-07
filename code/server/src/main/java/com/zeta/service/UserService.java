package com.zeta.service;

import com.zeta.domain.User;
import com.zeta.domain.UserRole;
import com.zeta.repository.UserRepository;
import com.zeta.web.dto.CreateUserRequest;
import com.zeta.web.dto.UpdateUserRequest;
import com.zeta.web.dto.UserSummaryResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserSummaryResponse> listUsers(UserRole role) {
        if (role == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请指定用户角色");
        }
        return userRepository.findAllByRoleOrderByCreatedAtAsc(role).stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    public UserSummaryResponse getUser(Long id) {
        return toSummary(findUser(id));
    }

    public UserSummaryResponse createUser(CreateUserRequest request) {
        String username = normalizeUsername(request.getUsername());
        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(request.getPassword());
        user.setDisplayName(request.getDisplayName().trim());
        user.setRole(request.getRole());
        user.setCreatedAt(Instant.now());
        return toSummary(userRepository.save(user));
    }

    public UserSummaryResponse updateUser(Long id, UpdateUserRequest request) {
        User user = findUser(id);
        if (user.getRole() != request.getRole()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持变更用户角色，请在对应角色管理中操作");
        }
        user.setDisplayName(request.getDisplayName().trim());
        return toSummary(userRepository.save(user));
    }

    public void resetPassword(Long id, String password) {
        User user = findUser(id);
        user.setPassword(password);
        userRepository.save(user);
    }

    public void deleteUser(Long id, User operator) {
        if (operator.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能删除当前登录账号");
        }
        User user = findUser(id);
        userRepository.delete(user);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
    }

    private String normalizeUsername(String username) {
        if (username == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入用户名");
        }
        String normalized = username.trim().toLowerCase();
        if (normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入用户名");
        }
        return normalized;
    }

    private UserSummaryResponse toSummary(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole(),
                user.getCreatedAt());
    }
}
