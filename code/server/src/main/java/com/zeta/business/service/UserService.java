package com.zeta.business.service;

import com.zeta.business.entities.binding.*;
import com.zeta.business.entities.binding.dto.*;
import com.zeta.business.entities.cabinetdisplay.*;
import com.zeta.business.entities.cabinetdisplay.dto.*;
import com.zeta.business.entities.cognitiondevice.*;
import com.zeta.business.entities.cognitiondevice.dto.*;
import com.zeta.business.entities.devicedisplay.*;
import com.zeta.business.entities.devicedisplay.dto.*;
import com.zeta.business.entities.drawinglearning.*;
import com.zeta.business.entities.drawinglearning.dto.*;
import com.zeta.business.entities.learningresource.*;
import com.zeta.business.entities.learningresource.dto.*;
import com.zeta.business.entities.logiclearning.*;
import com.zeta.business.entities.logiclearning.dto.*;
import com.zeta.business.entities.logicnodecognition.*;
import com.zeta.business.entities.logicnodecognition.dto.*;
import com.zeta.business.entities.monitor.*;
import com.zeta.business.entities.snapshot.*;
import com.zeta.business.entities.snapshot.dto.*;
import com.zeta.business.entities.user.*;
import com.zeta.business.entities.user.User;
import com.zeta.business.entities.user.UserRepository;
import com.zeta.business.entities.user.UserRole;
import com.zeta.business.entities.user.dto.*;
import com.zeta.business.entities.user.dto.CreateUserRequest;
import com.zeta.business.entities.user.dto.UpdateUserRequest;
import com.zeta.business.entities.user.dto.UserSummaryResponse;
import com.zeta.business.media.*;
import com.zeta.business.storage.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
        String studentNo = normalizeStudentNo(request.getStudentNo(), request.getRole(), true);
        String username = normalizeUsername(defaultUsername(request.getUsername(), studentNo, request.getRole()));
        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在");
        }
        if (studentNo != null && userRepository.existsByStudentNo(studentNo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "学号已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setStudentNo(studentNo);
        user.setPassword(request.getPassword());
        user.setDisplayName(request.getDisplayName().trim());
        user.setRole(request.getRole());
        user.setCreatedAt(Instant.now());
        return toSummary(userRepository.save(user));
    }

    public BatchImportStudentsResponse batchImportStudents(BatchImportStudentsRequest request) {
        BatchImportUsersResponse result = batchImportUsers(toImportUsersRequest(request), UserRole.STUDENT);
        List<StudentImportResult> studentResults = result.getResults().stream()
                .map(item -> new StudentImportResult(
                        item.getRowNumber(),
                        item.getUsername(),
                        item.getStudentNo(),
                        item.isSuccess(),
                        item.getMessage()))
                .collect(Collectors.toList());
        return new BatchImportStudentsResponse(
                result.getSuccessCount(),
                result.getFailureCount(),
                studentResults);
    }

    public BatchImportUsersResponse batchImportUsers(BatchImportUsersRequest request, UserRole role) {
        if (role == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请指定用户角色");
        }
        List<UserImportResult> results = new ArrayList<>();
        Set<String> importedUsernames = new HashSet<>();
        Set<String> importedStudentNos = new HashSet<>();
        int successCount = 0;

        for (int index = 0; index < request.getUsers().size(); index++) {
            ImportUserRowRequest row = request.getUsers().get(index);
            int rowNumber = index + 2;
            String studentNo = row == null ? "" : safeTrim(row.getStudentNo());
            String username = row == null ? "" : safeTrim(defaultUsername(row.getUsername(), studentNo, role)).toLowerCase();
            try {
                validateImportRow(row, role);
                if (!importedUsernames.add(username)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "导入文件内用户名重复");
                }
                if (role == UserRole.STUDENT && !importedStudentNos.add(studentNo)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "导入文件内学号重复");
                }
                if (userRepository.existsByUsername(username)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在");
                }
                if (role == UserRole.STUDENT && userRepository.existsByStudentNo(studentNo)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "学号已存在");
                }

                User user = new User();
                user.setUsername(username);
                user.setStudentNo(role == UserRole.STUDENT ? studentNo : null);
                user.setDisplayName(row.getDisplayName().trim());
                user.setPassword(row.getPassword());
                user.setRole(role);
                user.setCreatedAt(Instant.now());
                userRepository.save(user);
                successCount++;
                results.add(new UserImportResult(rowNumber, username, role == UserRole.STUDENT ? studentNo : null, true, "导入成功"));
            } catch (ResponseStatusException ex) {
                results.add(new UserImportResult(rowNumber, username, role == UserRole.STUDENT ? studentNo : null, false, ex.getReason()));
            } catch (RuntimeException ex) {
                results.add(new UserImportResult(rowNumber, username, role == UserRole.STUDENT ? studentNo : null, false, "导入失败"));
            }
        }

        return new BatchImportUsersResponse(
                successCount,
                results.size() - successCount,
                results);
    }

    public UserSummaryResponse updateUser(Long id, UpdateUserRequest request) {
        User user = findUser(id);
        if (user.getRole() != request.getRole()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持变更用户角色，请在对应角色管理中操作");
        }
        String studentNo = normalizeStudentNo(request.getStudentNo(), user.getRole(), true);
        if (studentNo != null && userRepository.existsByStudentNoAndIdNot(studentNo, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "学号已存在");
        }
        user.setStudentNo(studentNo);
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

    private BatchImportUsersRequest toImportUsersRequest(BatchImportStudentsRequest request) {
        BatchImportUsersRequest usersRequest = new BatchImportUsersRequest();
        List<ImportUserRowRequest> users = request.getStudents().stream()
                .map(row -> {
                    ImportUserRowRequest user = new ImportUserRowRequest();
                    if (row != null) {
                        user.setUsername(row.getUsername());
                        user.setStudentNo(row.getStudentNo());
                        user.setDisplayName(row.getDisplayName());
                        user.setPassword(row.getPassword());
                    }
                    return user;
                })
                .collect(Collectors.toList());
        usersRequest.setUsers(users);
        return usersRequest;
    }

    private void validateImportRow(ImportUserRowRequest row, UserRole role) {
        if (row == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "数据行不能为空");
        }
        String username = safeTrim(row.getUsername());
        String studentNo = safeTrim(row.getStudentNo());
        String displayName = safeTrim(row.getDisplayName());
        String password = row.getPassword() == null ? "" : row.getPassword();
        if (role == UserRole.STUDENT && username.isEmpty() && !studentNo.isEmpty()) {
            username = studentNo;
        }
        if (username.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入用户名");
        }
        if (username.length() > 64) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名不能超过 64 个字符");
        }
        if (role == UserRole.STUDENT && studentNo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入学号");
        }
        if (studentNo.length() > 64) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "学号不能超过 64 个字符");
        }
        if (displayName.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入显示名称");
        }
        if (displayName.length() > 64) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "显示名称不能超过 64 个字符");
        }
        if (password.length() < 6 || password.length() > 128) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码长度为 6-128 个字符");
        }
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String defaultUsername(String username, String studentNo, UserRole role) {
        String normalized = safeTrim(username);
        if (role == UserRole.STUDENT && normalized.isEmpty()) {
            return studentNo;
        }
        return normalized;
    }

    private String normalizeStudentNo(String studentNo, UserRole role, boolean requiredForStudent) {
        String normalized = safeTrim(studentNo);
        if (role == UserRole.STUDENT) {
            if (requiredForStudent && normalized.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入学号");
            }
            if (normalized.length() > 64) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "学号不能超过 64 个字符");
            }
            return normalized.isEmpty() ? null : normalized;
        }
        return null;
    }

    private UserSummaryResponse toSummary(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getStudentNo(),
                user.getDisplayName(),
                user.getRole(),
                user.getCreatedAt());
    }
}
