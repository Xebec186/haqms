package com.haqms.runner;

import com.haqms.entity.*;
import com.haqms.enums.Gender;
import com.haqms.repository.*;
import com.haqms.util.CsvLogger;
import lombok.RequiredArgsConstructor;
import net.datafaker.Faker;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;


@Component
@RequiredArgsConstructor
@Profile("dev")   // Only runs in development profile
public class DataSeeder implements ApplicationRunner {

    private final DepartmentRepository deptRepo;
    private final HealthcareProviderRepository providerRepo;
    private final PatientRepository patientRepo;
    private final ProviderScheduleRepository scheduleRepo;
    private final RoleRepository roleRepo;
    private final SystemUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    List<String[]> patientRows = new ArrayList<>();
    List<String[]> providerRows = new ArrayList<>();


    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (roleRepo.count() > 0) return; // Already seeded

        // Seed roles
        Role adminRole = roleRepo.save(new Role(null, "ADMIN", "Full access"));
        Role provider = roleRepo.save(new Role(null, "PROVIDER", "Clinical staff"));
        Role patient = roleRepo.save(new Role(null, "PATIENT", "Patient access"));

        // ── Default admin account ─────────────────────────────────────────────
        if (!userRepository.existsByUsername("admin")) {
            SystemUser admin = SystemUser.builder()
                    .role(adminRole)
                    .username("admin")
                    .passwordHash(passwordEncoder.encode("Admin@1234"))
                    .email("admin@haqms.gh")
                    .isActive(true)
                    .build();
            userRepository.save(admin);
        }

        // Seed 10 departments
        List<String> deptNames = List.of("Outpatient", "Cardiology", "Maternity",
                "Paediatrics", "Emergency", "Orthopaedics",
                "Ophthalmology", "Dermatology", "ENT", "General Surgery");
        List<Department> depts = deptNames.stream()
                .map(n -> deptRepo.save(Department.builder().name(n).isActive(true).build()))
                .toList();

        // Seed 100 providers (10 per department)
        Faker faker = new Faker();
        List<HealthcareProvider> providers = new ArrayList<>();
        for (Department dept : depts) {
            for (int i = 0; i < 10; i++) {
                String license = "GH-MED-" + faker.number().digits(6);
                HealthcareProvider p = HealthcareProvider.builder()
                        .department(dept)
                        .firstName(faker.name().firstName())
                        .lastName(faker.name().lastName())
                        .licenseNumber(license)
                        .specialisation(dept.getName() + " Specialist")
                        .isActive(true)
                        .build();
                providers.add(providerRepo.save(p));

                // Persist linked system user
                SystemUser user = SystemUser.builder()
                        .role(provider)
                        .username(p.getFirstName().toLowerCase() + p.getLastName().toLowerCase())
                        .passwordHash(passwordEncoder.encode("Password@123"))
                        .email(p.getFirstName().toLowerCase() + p.getLastName().toLowerCase() + "@gmail.com")
                        .provider(p)
                        .isActive(true)
                        .build();

                userRepository.save(user);

                // logging to csv file
                providerRows.add(new String[]{
                        user.getUserId().toString(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getRole().getRoleName()
                });
                CsvLogger.log("logs/providers.csv", "id, username, email, role", providerRows);
            }
        }

        // Seed schedules: each provider gets Mon–Sat for 2 weeks
        LocalDate today = LocalDate.now();
        for (HealthcareProvider prov : providers) {
            for (int d = 0; d < 14; d++) {
                LocalDate date = today.plusDays(d);
                scheduleRepo.save(ProviderSchedule.builder()
                        .provider(prov)
                        .scheduleDate(date)
                        .startTime(LocalTime.of(0, 0))
                        .endTime(LocalTime.of(0, 0))
                        .maxSlots(20)
                        .isAvailable(true)
                        .build());
            }
        }

        // Seed 10,000 patients with realistic Ghanaian names
        // (abbreviated — full loop runs in production seeder script)
        for (int i = 0; i < 5; i++) {  // Abbreviated for seeder bean; full script uses Python Faker
            Patient pat = Patient.builder()
                    .firstName(faker.name().firstName())
                    .lastName(faker.name().lastName())
                    .dateOfBirth(LocalDate.now().minusYears(20 + faker.number().numberBetween(0, 50)))
                    .gender(i % 2 == 0 ? Gender.MALE : Gender.FEMALE)
                    .phoneNumber("+233" + faker.number().digits(9))
                    .isActive(true)
                    .build();
            patientRepo.save(pat);

            // Persist linked system user
            SystemUser user = SystemUser.builder()
                    .role(patient)
                    .username(pat.getFirstName().toLowerCase() + pat.getLastName().toLowerCase())
                    .passwordHash(passwordEncoder.encode("Password@123"))
                    .email(pat.getFirstName().toLowerCase() + pat.getLastName().toLowerCase() + "@gmail.com")
                    .patient(pat)
                    .isActive(true)
                    .build();

            userRepository.save(user);

            // logging to csv file
            patientRows.add(new String[]{
                    user.getUserId().toString(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getRole().getRoleName()
            });
            CsvLogger.log("logs/patients.csv", "id, username, email, role", patientRows);
        }
    }
}
