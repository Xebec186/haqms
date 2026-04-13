package com.haqms.repository;

import com.haqms.entity.HealthcareProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HealthcareProviderRepository extends JpaRepository<HealthcareProvider, Long> {

    List<HealthcareProvider> findByDepartmentDepartmentIdAndIsActiveTrueOrderByLastNameAsc(
            Long departmentId);

    List<HealthcareProvider> findAllByOrderByLastNameAsc();

    boolean existsByLicenseNumber(String licenseNumber);
}
