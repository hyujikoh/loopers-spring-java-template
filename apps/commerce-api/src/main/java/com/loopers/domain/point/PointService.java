package com.loopers.domain.point;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
     * 사용자의 포인트 이력을 페이징하여 조회합니다.
     *
     * @param username 사용자명
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 포인트 이력 페이지
     */
    @Transactional(readOnly = true)
    public Page<PointHistoryEntity> getPointHistories(String username, int page, int size) {
        PointEntity point = getByUsername(username);

        if (point == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 사용자입니다.");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return pointHistoryRepository.findByPoint(point, pageable);
    }

    @Transactional
    public void createPointForNewUser(UserEntity user) {
        PointEntity existingPoint = getByUsername(user.getUsername());

        if (existingPoint != null) {
            // 이미 포인트가 존재하는 경우 아무 작업도 수행하지 않음
            return;
        }

        PointEntity pointEntity = PointEntity.createPointEntity(user);
        pointRepository.save(pointEntity);
    }

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
