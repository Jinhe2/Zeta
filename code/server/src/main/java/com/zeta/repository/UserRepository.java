package com.zeta.repository;

import com.zeta.domain.User;
import com.zeta.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findAllByOrderByCreatedAtAsc();

    List<User> findAllByRoleOrderByCreatedAtAsc(UserRole role);
}
