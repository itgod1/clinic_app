package com.clinic.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clinic.entity.OperationLog;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志服务
 */
public interface OperationLogService {

    /**
     * 分页查询操作日志
     */
    IPage<OperationLog> pageQuery(Page<OperationLog> page, Long clinicId, String module,
                                   Integer operationType, String username,
                                   LocalDateTime startTime, LocalDateTime endTime, Integer status);

    /**
     * 保存操作日志
     */
    void saveLog(OperationLog log);

    /**
     * 批量删除日志
     */
    void deleteByIds(List<Long> ids);

    /**
     * 清理指定日期之前的日志
     */
    void cleanLogsBefore(LocalDateTime before);

    /**
     * 获取用户最近的登录日志
     */
    List<OperationLog> getRecentLoginLogs(Long userId, Integer limit);
}
