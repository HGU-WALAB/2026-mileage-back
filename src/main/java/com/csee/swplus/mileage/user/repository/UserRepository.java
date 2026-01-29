package com.csee.swplus.mileage.user.repository;

import com.csee.swplus.mileage.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Users, Long> {
    Optional<Users> findByUniqueId(String studentId);
}