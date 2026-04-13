package com.haqms.repository;

import com.haqms.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByPhoneNumber(String phoneNumber);

    Optional<Patient> findByGhanaCardNumber(String ghanaCardNumber);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByGhanaCardNumber(String ghanaCardNumber);

    Page<Patient> findByLastNameContainingIgnoreCaseOrFirstNameContainingIgnoreCase(
            String lastName, String firstName, Pageable pageable);

    /**
     * Search patients by name (first or last name) or phone number.
     *
     * @param term     Search term (name or phone)
     * @param pageable Pagination information
     * @return Paginated list of patients matching the search term
     */
    @Query("SELECT p FROM Patient p " +
            "WHERE LOWER(p.firstName) LIKE LOWER(CONCAT('%', :term, '%')) " +
            "   OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :term, '%')) " +
            "   OR p.phoneNumber LIKE CONCAT('%', :term, '%')")
    Page<Patient> searchByNameOrPhone(@Param("term") String term, Pageable pageable);

}
