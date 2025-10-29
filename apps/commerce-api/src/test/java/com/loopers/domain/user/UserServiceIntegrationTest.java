package com.loopers.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.DataIntegrityViolationException;

import com.loopers.utils.DatabaseCleanUp;

/**
 * @author hyunjikoh
 * @since 2025. 10. 27.
 */
@SpringBootTest
public class UserServiceIntegrationTest {
    @Autowired
    private UserService userService;

    @SpyBean
    private UserRepository userRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }


    @DisplayName("회원 가입시 User 저장이 수행된다. (spy 검증)")
    @Test
    void register_spy_success() {
        // given
        UserDomainCreateRequest request = createUserRegisterRequest("testuser", "test@email.com", "1990-01-01");

        // when
        UserEntity result = userService.register(request);

        // then
        assertUserEntityByRequest(result, request);
        verify(userRepository).save(any(UserEntity.class));
    }


    @DisplayName("회원 가입시 User 저장이 수행된다.")
    @Test
    void register_success() {
        // given
        UserDomainCreateRequest request = createUserRegisterRequest("testuser", "test@email.com", "1990-01-01");

        // when
        UserEntity result = userService.register(request);

        // then
        assertUserEntityByRequest(result, request);
    }

    @DisplayName("이미 가입된 사용자명으로 회원가입 시도 시 실패한다.")
    @Test
    void register_fail_when_username_already_exists() {
        // given
        UserDomainCreateRequest existingUser = createUserRegisterRequest("testuser", "existing@email.com", "1990-01-01");
        userRepository.save(UserEntity.createUserEntity(existingUser));

        UserDomainCreateRequest duplicateUser = createUserRegisterRequest("testuser", "new@email.com", "1990-01-02");

        // when & then
        assertThatThrownBy(() -> userService.register(duplicateUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 존재하는 사용자 이름입니다");
    }

    @DisplayName("이미 가입된 사용자명으로 저장 시도시 실패한다.")
    @Test
    void save_fail_when_username_already_exists() {
        // given
        UserDomainCreateRequest existingUser = createUserRegisterRequest("testuser", "existing@email.com", "1990-01-01");
        userRepository.save(UserEntity.createUserEntity(existingUser));

        UserDomainCreateRequest duplicateUser = createUserRegisterRequest("testuser", "new@email.com", "1990-01-02");
        Assertions.assertThatThrownBy(() -> userRepository.save(UserEntity.createUserEntity(duplicateUser)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }


    @Test
    @DisplayName("사용자 아이디로 사용자 정보를 조회한다.")
    void get_user_by_username_success() {
        // given
        UserDomainCreateRequest req = createUserRegisterRequest("testuser", "test@email.com", "1990-01-01");
        UserEntity savedUser = userRepository.save(UserEntity.createUserEntity(req));

        // when
        UserEntity foundUser = userService.getUserByUsername(savedUser.getUsername());

        // then
        verify(userRepository, times(1)).findByUsername(foundUser.getUsername());
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getUsername()).isEqualTo(savedUser.getUsername());
        assertThat(foundUser.getEmail()).isEqualTo(savedUser.getEmail());
        assertThat(foundUser.getBirthdate()).isEqualTo(savedUser.getBirthdate());
        assertThat(foundUser.getId()).isEqualTo(savedUser.getId());
    }

    @Test
    @DisplayName("존재하지 않는 사용자 아이디로 조회시 null을 반환한다.")
    void get_user_by_username_returnNull_whenUserNotFound() {
        // given
        String nonExistentUsername = "nonexistent";

        // when
        UserEntity result = userService.getUserByUsername(nonExistentUsername);

        // then
        assertThat(result).isNull();
    }

    /**
     * 요청 생성 헬퍼 메서드
     *
     * @param username
     * @param email
     * @param birthdate
     * @return
     */
    private UserDomainCreateRequest createUserRegisterRequest(String username, String email, String birthdate) {
        return new UserDomainCreateRequest(username, email, birthdate, Gender.MALE);
    }

    /**
     * 저장된 UserEntity 검증 헬퍼 메서드
     *
     * @param actual
     * @param expected
     */
    private void assertUserEntityByRequest(UserEntity actual, UserDomainCreateRequest expected) {
        assertThat(actual.getId()).isNotNull();
        assertThat(actual.getUsername()).isEqualTo(expected.username());
        assertThat(actual.getEmail()).isEqualTo(expected.email());
        assertThat(actual.getBirthdate().toString()).isEqualTo(expected.birthdate());
        assertThat(actual.getCreatedAt()).isNotNull();
        assertThat(actual.getDeletedAt()).isNull();
    }
}
