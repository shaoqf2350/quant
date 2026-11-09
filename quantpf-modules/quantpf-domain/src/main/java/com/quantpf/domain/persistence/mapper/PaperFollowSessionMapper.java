package com.quantpf.domain.persistence.mapper;

import com.quantpf.domain.persistence.record.PaperFollowSessionRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PaperFollowSessionMapper {
    PaperFollowSessionRow selectByStartRequestId(@Param("startRequestId") String startRequestId);
    PaperFollowSessionRow selectByStopRequestId(@Param("stopRequestId") String stopRequestId);
    PaperFollowSessionRow selectRunning(@Param("accountId") Long accountId,
                                        @Param("strategyId") Long strategyId,
                                        @Param("mode") String mode);
    PaperFollowSessionRow selectBySessionIdForUpdate(@Param("sessionId") String sessionId);
    PaperFollowSessionRow selectRunningForUpdateNowait(@Param("accountId") Long accountId, @Param("strategyId") Long strategyId, @Param("mode") String mode);
    int insert(PaperFollowSessionRow row);
    int stop(@Param("sessionId") String sessionId,
             @Param("stopRequestId") String stopRequestId,
             @Param("stoppedBy") String stoppedBy,
             @Param("stoppedAt") java.time.Instant stoppedAt,
             @Param("stopReason") String stopReason);
    String selectAccountType(@Param("accountId") Long accountId);
    String selectAccountStatus(@Param("accountId") Long accountId);
    String selectStrategyStatus(@Param("strategyId") Long strategyId);
}
