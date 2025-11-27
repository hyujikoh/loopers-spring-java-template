package com.loopers.application.product;

import lombok.Getter;

/**
 * 배치 업데이트 결과 DTO
 *
 * <p>MV 배치 동기화 작업의 결과를 담는 불변 객체입니다.</p>
 *
 * @author hyunjikoh
 * @since 2025. 11. 27.
 */
@Getter
public class BatchUpdateResult {

    private final boolean success;
    private final int createdCount;
    private final int updatedCount;
    private final long durationMs;
    private final String errorMessage;

    private BatchUpdateResult(
            boolean success,
            int createdCount,
            int updatedCount,
            long durationMs,
            String errorMessage
    ) {
        this.success = success;
        this.createdCount = createdCount;
        this.updatedCount = updatedCount;
        this.durationMs = durationMs;
        this.errorMessage = errorMessage;
    }

    /**
     * 성공 결과를 생성합니다.
     *
     * @param createdCount 생성된 레코드 수
     * @param updatedCount 갱신된 레코드 수
     * @param durationMs   소요 시간 (밀리초)
     * @return 성공 결과
     */
    public static BatchUpdateResult success(int createdCount, int updatedCount, long durationMs) {
        return new BatchUpdateResult(true, createdCount, updatedCount, durationMs, null);
    }

    /**
     * 실패 결과를 생성합니다.
     *
     * @param errorMessage 에러 메시지
     * @param durationMs   소요 시간 (밀리초)
     * @return 실패 결과
     */
    public static BatchUpdateResult failure(String errorMessage, long durationMs) {
        return new BatchUpdateResult(false, 0, 0, durationMs, errorMessage);
    }

    /**
     * 변경 사항이 있는지 확인합니다.
     *
     * @return 생성 또는 갱신된 레코드가 있으면 true
     */
    public boolean hasChanges() {
        return success && (createdCount > 0 || updatedCount > 0);
    }

    /**
     * 전체 처리 건수를 반환합니다.
     *
     * @return 생성 + 갱신 건수
     */
    public int getTotalCount() {
        return createdCount + updatedCount;
    }

    @Override
    public String toString() {
        if (success) {
            return String.format(
                    "BatchUpdateResult{success=true, created=%d, updated=%d, duration=%dms}",
                    createdCount, updatedCount, durationMs
            );
        } else {
            return String.format(
                    "BatchUpdateResult{success=false, error='%s', duration=%dms}",
                    errorMessage, durationMs
            );
        }
    }
}
