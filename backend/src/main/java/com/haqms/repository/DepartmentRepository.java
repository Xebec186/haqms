package com.haqms.repository;

import com.haqms.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    List<Department> findByIsActiveTrueOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);
}
