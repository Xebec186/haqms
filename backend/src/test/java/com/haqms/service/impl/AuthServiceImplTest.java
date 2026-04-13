package com.haqms.service.impl;

import com.haqms.dto.request.LoginRequest;
import com.haqms.dto.request.RegisterRequest;
import com.haqms.dto.response.AuthResponse;
import com.haqms.dto.response.PatientResponse;
import com.haqms.entity.*;
import com.haqms.exception.ConflictException;
import com.haqms.exception.ResourceNotFoundException;
import com.haqms.repository.PatientRepository;
import com.haqms.repository.RoleRepository;
import com.haqms.repository.SystemUserRepository;
import com.haqms.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthServiceImpl.
 *
 * Tests cover login (JWT issuance, lastLogin timestamp, token content),
 * and registration (conflict detection for phone, Ghana Card, username, email).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl")
class AuthServiceImplTest {

    @Mock private AuthenticationManager  authenticationManager;
    @Mock private SystemUserRepository   userRepository;
    @Mock private PatientRepository      patientRepository;
    @Mock private RoleRepository         roleRepository;
    @Mock private PasswordEncoder        passwordEncoder;
    @Mock private JwtUtil                jwtUtil;

    @InjectMocks
    private AuthServiceImpl service;

    private SystemUser user;
    private Role       patientRole;
    private Patient    patient;

    @BeforeEach
    void setUp() {
        patientRole = new Role();
        patientRole.setRoleId(3);
        patientRole.setRoleName("PATIENT");

        patient = new Patient();
        patient.setPatientId(100L);
        patient.setFirstName("Kwame");
        patient.setLastName("Mensah");

        user = new SystemUser();
        user.setUserId(1L);
        user.setUsername("testpatient01");
        user.setRole(patientRole);
        user.setPatient(patient);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // login()
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("TC-AUTH-001: Valid credentials return AuthResponse with token and role")
        void validCredentials_returnsAuthResponse() {
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            when(authenticationManager.authenticate(any())).thenReturn(authToken);
            when(jwtUtil.generateToken(user)).thenReturn("mock.jwt.token");
            when(userRepository.save(any())).thenReturn(user);

            LoginRequest request = new LoginRequest();
            request.setUsername("testpatient01");
            request.setPassword("Test@1234");

            AuthResponse response = service.login(request);

            assertThat(response.getToken()).isEqualTo("mock.jwt.token");
            assertThat(response.getRole()).isEqualTo("PATIENT");
            assertThat(response.getUserId()).isEqualTo(1L);
            assertThat(response.getPatientId()).isEqualTo(100L);
            assertThat(response.getProviderId()).isNull(); // patient has no provider
        }

        @Test
        @DisplayName("TC-AUTH-002: lastLogin timestamp updated on successful login")
        void login_updatesLastLoginTimestamp() {
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            when(authenticationManager.authenticate(any())).thenReturn(authToken);
            when(jwtUtil.generateToken(user)).thenReturn("mock.jwt.token");
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            LoginRequest request = new LoginRequest();
            request.setUsername("testpatient01");
            request.setPassword("Test@1234");

            service.login(request);

            ArgumentCaptor<SystemUser> captor = ArgumentCaptor.forClass(SystemUser.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getLastLogin()).isNotNull();
        }

        @Test
        @DisplayName("TC-AUTH-003: Invalid credentials propagate BadCredentialsException from AuthManager")
        void invalidCredentials_throwsBadCredentials() {
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            LoginRequest request = new LoginRequest();
            request.setUsername("wrong");
            request.setPassword("wrong");

            assertThatThrownBy(() -> service.login(request))
                    .isInstanceOf(BadCredentialsException.class);

            verify(jwtUtil, never()).generateToken(any());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-AUTH-004: Provider login returns providerId and null patientId")
        void providerLogin_returnsProviderIdAndNullPatientId() {
            HealthcareProvider provider = new HealthcareProvider();
            provider.setProviderId(10L);

            Role providerRole = new Role();
            providerRole.setRoleId(2);
            providerRole.setRoleName("PROVIDER");

            SystemUser providerUser = new SystemUser();
            providerUser.setUserId(2L);
            providerUser.setRole(providerRole);
            providerUser.setProvider(provider);
            providerUser.setPatient(null);

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(providerUser, null, providerUser.getAuthorities());
            when(authenticationManager.authenticate(any())).thenReturn(authToken);
            when(jwtUtil.generateToken(providerUser)).thenReturn("provider.jwt.token");
            when(userRepository.save(any())).thenReturn(providerUser);

            LoginRequest request = new LoginRequest();
            request.setUsername("provider_001");
            request.setPassword("Test@1234");

            AuthResponse response = service.login(request);

            assertThat(response.getRole()).isEqualTo("PROVIDER");
            assertThat(response.getProviderId()).isEqualTo(10L);
            assertThat(response.getPatientId()).isNull();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // register()
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("register()")
    class Register {

        private RegisterRequest validRequest() {
            RegisterRequest r = new RegisterRequest();
            r.setFirstName("Kwame");
            r.setLastName("Mensah");
            r.setGender("MALE");
            r.setDateOfBirth(java.time.LocalDate.of(1990, 5, 15));
            r.setPhoneNumber("+233241234567");
            r.setEmail("kwame.mensah@mail.gh");
            r.setUsername("kwame.mensah01");
            r.setPassword("Test@1234");
            return r;
        }

        @Test
        @DisplayName("TC-AUTH-005: Valid registration creates patient and system user")
        void validRequest_createsPatientAndUser() {
            when(patientRepository.existsByPhoneNumber(anyString())).thenReturn(false);
            when(patientRepository.existsByGhanaCardNumber(any())).thenReturn(false);
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(patientRepository.save(any())).thenReturn(patient);
            when(roleRepository.findByRoleName("PATIENT")).thenReturn(Optional.of(patientRole));
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedpassword");
            when(userRepository.save(any())).thenReturn(user);

            PatientResponse response = service.register(validRequest());

            assertThat(response).isNotNull();
            verify(patientRepository).save(any(Patient.class));
            verify(userRepository).save(any(SystemUser.class));
        }

        @Test
        @DisplayName("TC-AUTH-006: Duplicate phone number throws ConflictException")
        void duplicatePhone_throwsConflict() {
            when(patientRepository.existsByPhoneNumber("+233241234567")).thenReturn(true);

            assertThatThrownBy(() -> service.register(validRequest()))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("phone number");

            verify(patientRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-AUTH-007: Duplicate Ghana Card number throws ConflictException")
        void duplicateGhanaCard_throwsConflict() {
            RegisterRequest req = validRequest();
            req.setGhanaCardNumber("GHA-123456789-0");
            when(patientRepository.existsByPhoneNumber(anyString())).thenReturn(false);
            when(patientRepository.existsByGhanaCardNumber("GHA-123456789-0")).thenReturn(true);

            assertThatThrownBy(() -> service.register(req))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Ghana Card");
        }

        @Test
        @DisplayName("TC-AUTH-008: Duplicate username throws ConflictException")
        void duplicateUsername_throwsConflict() {
            when(patientRepository.existsByPhoneNumber(anyString())).thenReturn(false);
            when(userRepository.existsByUsername("kwame.mensah01")).thenReturn(true);

            assertThatThrownBy(() -> service.register(validRequest()))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("already taken");

            verify(patientRepository, never()).save(any());
        }

        @Test
        @DisplayName("TC-AUTH-009: Duplicate email throws ConflictException")
        void duplicateEmail_throwsConflict() {
            when(patientRepository.existsByPhoneNumber(anyString())).thenReturn(false);
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail("kwame.mensah@mail.gh")).thenReturn(true);

            assertThatThrownBy(() -> service.register(validRequest()))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("already registered");
        }

        @Test
        @DisplayName("TC-AUTH-010: PATIENT role not found throws ResourceNotFoundException")
        void patientRoleNotFound_throwsResourceNotFound() {
            when(patientRepository.existsByPhoneNumber(anyString())).thenReturn(false);
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(patientRepository.save(any())).thenReturn(patient);
            when(roleRepository.findByRoleName("PATIENT")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.register(validRequest()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Role PATIENT not found");
        }

        @Test
        @DisplayName("TC-AUTH-011: Password is BCrypt-encoded before persisting")
        void register_passwordEncoded() {
            when(patientRepository.existsByPhoneNumber(anyString())).thenReturn(false);
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(patientRepository.save(any())).thenReturn(patient);
            when(roleRepository.findByRoleName("PATIENT")).thenReturn(Optional.of(patientRole));
            when(passwordEncoder.encode("Test@1234")).thenReturn("$2a$10$encodedHash");
            when(userRepository.save(any())).thenReturn(user);

            service.register(validRequest());

            ArgumentCaptor<SystemUser> captor = ArgumentCaptor.forClass(SystemUser.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getPasswordHash()).isEqualTo("$2a$10$encodedHash");
            assertThat(captor.getValue().getPasswordHash()).doesNotContain("Test@1234");
        }
    }
}
