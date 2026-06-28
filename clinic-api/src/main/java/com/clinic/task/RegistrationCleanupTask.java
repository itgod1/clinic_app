package com.clinic.task;

import com.clinic.mapper.RegistrationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class RegistrationCleanupTask {

    private final RegistrationMapper registrationMapper;

    /**
     * 每天凌晨2点执行，清理一个月前的挂号记录
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldRegistrations() {
        try {
            LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
            log.info("开始清理挂号记录，删除创建时间早于 {} 的记录", oneMonthAgo);

            int deletedCount = registrationMapper.deleteByCreatedAtBefore(oneMonthAgo);

            log.info("挂号记录清理完成，共删除 {} 条记录", deletedCount);
        } catch (Exception e) {
            log.error("清理挂号记录时发生错误", e);
        }
    }
}
