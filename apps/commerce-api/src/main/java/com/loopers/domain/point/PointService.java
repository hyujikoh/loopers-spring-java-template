package com.loopers.domain.point;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.user.UserEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

/**
 * @author hyunjikoh
 * @since 2025. 10. 29.
 */
@Component
@RequiredArgsConstructor
public class PointService {
    private final PointRepository pointRepository;
    private final PointHistoryRepository pointHistoryRepository;

    @Transactional(readOnly = true)
    public PointEntity getByUsername(String username) {
        return pointRepository.findByUsername(username);
    }

    /**
     * 사용자의 포인트 이력을 조회합니다.
     * TODO : 페이징이 필요할 경우 별도 메소드 추가
     *
     * @param username 사용자명
     * @return 포인트 이력 목록 (최신순)
     */
    @Transactional(readOnly = true)
    public List<PointHistoryEntity> getPointHistories(String username) {
        PointEntity point = getByUsername(username);

        if (point == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 사용자입니다.");
        }

        return pointHistoryRepository.findByPointOrderByCreatedAtDesc(point);
    }

    /**
     * 신규 사용자에 대해 포인트엔티티를 생성합니다.
     *
     * @param user
     */
    @Transactional
    public void createPointForNewUser(UserEntity user) {
        PointEntity existingPoint = getByUsername(user.getUsername());

        // 이미 포인트 엔티티가 존재하는 경우 생성하지 않음
        if (existingPoint == null) {
            PointEntity pointEntity = PointEntity.createPointEntity(user);
            pointRepository.save(pointEntity);
        }
    }

    /**
     * 사용자에게 포인트를 충전합니다.
     *
     * @param username
     * @param amount
     * @return
     */
    @Transactional
    public BigDecimal charge(String username, BigDecimal amount) {
        PointEntity point = getByUsername(username);

        if (point == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 사용자입니다.");
        }

        point.charge(amount);
        pointRepository.save(point);

        return point.getAmount();
    }
}
