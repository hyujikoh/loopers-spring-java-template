package com.loopers.domain.point;

import java.math.BigDecimal;

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

    @Transactional
    public void createPointForNewUser(UserEntity user) {
        PointEntity existingPoint = getByUsername(user.getUsername());

        if (existingPoint != null) {
            // 이미 포인트가 존재하는 경우 아무 작업도 수행하지 않음
            // TODO : 불필요한 로직인지 고민
            return;
        }

        PointEntity pointEntity = PointEntity.createPointEntity(user);
        pointRepository.save(pointEntity);
    }

    @Transactional
    public BigDecimal charge(String username, BigDecimal amount) {
        PointEntity point = getByUsername(username);
        
        if (point == null) {
            throw new CoreException(ErrorType.NOT_FOUND,"존재하지 않는 사용자입니다.");
        }
        
        point.charge(amount);
        pointRepository.save(point);
        
        // 포인트 내역 저장
        PointHistoryEntity history = new PointHistoryEntity(
            point, 
            PointTransactionType.CHARGE, 
            amount, 
            point.getAmount()
        );
        pointHistoryRepository.save(history);
        
        return point.getAmount();
    }
}
