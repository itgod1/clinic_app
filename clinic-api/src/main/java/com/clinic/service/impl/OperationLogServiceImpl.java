package com.clinic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.clinic.entity.OperationLog;
import com.clinic.mapper.OperationLogMapper;
import com.clinic.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;

    @Override
    public IPage<OperationLog> pageQuery(Page<OperationLog> page, Long clinicId, String module,
                                         Integer operationType, String username,
                                         LocalDateTime startTime, LocalDateTime endTime, Integer status) {
        return operationLogMapper.selectPageList(page, clinicId, module, operationType, username,
                startTime, endTime, status);
    }

    @Override
    public void saveLog(OperationLog operationLog) {
        operationLogMapper.insert(operationLog);
    }

    @Override
    public void deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        operationLogMapper.deleteBatchIds(ids);
    }

    @Override
    public void cleanLogsBefore(LocalDateTime before) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(OperationLog::getCreatedAt, before);
        operationLogMapper.delete(wrapper);
    }

    @Override
    public List<OperationLog> getRecentLoginLogs(Long userId, Integer limit) {
        return operationLogMapper.selectRecentLoginLogs(userId, limit);
    }
}
