package com.loopers.domain.point;

import static org.mockito.Mockito.when;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extensions;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.loopers.domain.user.*;

/**
 * @author hyunjikoh
 * @since 2025. 10. 29.
 */
@SpringBootTest
@ExtendWith(MockitoExtension.class)
public class PointServiceIntegrationTest {
    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private PointService pointService;




    @org.junit.jupiter.api.Test
    @DisplayName("등록된 사용자의 포인트 조회를 성공한다.")
    void get_exist_user_point_amount() {
        // given
        UserRegisterRequest request = createUserRegisterRequest("testuser", "existing@email.com", "1990-01-01");
        UserEntity save = userRepository.save(UserEntity.createUserEntity(request));

        Point pointEntity = Point.createPointEntity(save);
        when(pointService.getByUser(save)).thenReturn(pointEntity);
        // when
        Point point = pointService.getByUser(save);

        // then
        Assertions.assertThat(point.getAmount()).isNotNull();
        Assertions.assertThat(point.getUser()).isEqualTo(save);
        Assertions.assertThat(point.getAmount()).isNotNegative();

    }
    private UserRegisterRequest createUserRegisterRequest(String username, String email, String birthdate) {
        return new UserRegisterRequest(username, email, birthdate, Gender.MALE);
    }

}
