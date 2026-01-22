package com.smart_ecomernce_api.smart_ecomernce_api.modules.user.repository;

import com.smart_ecomernce_api.Smart_ecommerce_api.modules.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
    @Query("SELECT u FROM User u WHERE u.isActive = true AND u.id = :id")
    Optional<User> findActiveById(@Param("id") Long id);

    @Query("SELECT u FROM User u WHERE " +
            "(:firstName IS NULL OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :firstName, '%'))) AND " +
            "(:lastName IS NULL OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :lastName, '%'))) AND " +
            "(:role IS NULL OR u.role = :role)")
    Page<User> findUsersWithFilters(@Param("firstName") String firstName,
                                    @Param("lastName") String lastName,
                                    @Param("role") String role,
                                    Pageable pageable);

    boolean existsByUsername(String username);

    Optional<User> findByUsername(String username);

    Long countByIsActive(boolean isActive);
}