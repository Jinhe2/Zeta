package com.zeta.business.entities.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import javax.persistence.LockModeType;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForActivityUpdate(@Param("id") Long id);

    Optional<User> findByStudentNo(String studentNo);

    boolean existsByUsername(String username);

    boolean existsByStudentNo(String studentNo);

    boolean existsByStudentNoAndIdNot(String studentNo, Long id);

    List<User> findAllByOrderByCreatedAtAsc();

    List<User> findAllByRoleOrderByCreatedAtAsc(UserRole role);
}
