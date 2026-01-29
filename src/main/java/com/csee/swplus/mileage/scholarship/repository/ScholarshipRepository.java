package com.csee.swplus.mileage.scholarship.repository;

import com.csee.swplus.mileage.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ScholarshipRepository extends JpaRepository<Users, Long> {
    Optional<Users> findByUniqueId(String studentId);
}