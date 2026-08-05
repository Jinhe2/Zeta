package com.zeta.business.user;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    @Test
    void batchImportStudentsReturnsPerRowResultsAndForcesStudentRole() {
        UserRepository repository = mock(UserRepository.class);
        when(repository.existsByUsername("existing")).thenReturn(true);
        UserService service = new UserService(repository);

        BatchImportStudentsRequest request = new BatchImportStudentsRequest();
        request.setStudents(Arrays.asList(
                row(" Existing ", "已存在", "123456"),
                row("new_student", "新学员", "123456"),
                row("short_password", "密码错误", "123")));

        BatchImportStudentsResponse response = service.batchImportStudents(request);

        assertThat(response.getSuccessCount()).isEqualTo(1);
        assertThat(response.getFailureCount()).isEqualTo(2);
        assertThat(response.getResults()).extracting(StudentImportResult::getRowNumber)
                .containsExactly(2, 3, 4);
        assertThat(response.getResults()).extracting(StudentImportResult::isSuccess)
                .containsExactly(false, true, false);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(repository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getUsername()).isEqualTo("new_student");
        assertThat(userCaptor.getValue().getRole()).isEqualTo(UserRole.STUDENT);
    }

    private ImportStudentRowRequest row(String username, String displayName, String password) {
        ImportStudentRowRequest row = new ImportStudentRowRequest();
        row.setUsername(username);
        row.setDisplayName(displayName);
        row.setPassword(password);
        return row;
    }
}
