package com.zeta.business.entities.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByStudentNo(String studentNo);

    boolean existsByUsername(String username);

    boolean existsByStudentNo(String studentNo);

    boolean existsByStudentNoAndIdNot(String studentNo, Long id);

    List<User> findAllByOrderByCreatedAtAsc();

    List<User> findAllByRoleOrderByCreatedAtAsc(UserRole role);
}
