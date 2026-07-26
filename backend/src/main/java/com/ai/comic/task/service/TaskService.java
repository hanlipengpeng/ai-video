package com.ai.comic.task.service;

import com.ai.comic.common.BusinessException;
import com.ai.comic.common.PageResult;
import com.ai.comic.common.TaskConstants;
import com.ai.comic.task.entity.TaskLog;
import com.ai.comic.task.mapper.TaskLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务日志服务。
 * <p>
 * 提供 aigc 层记录调用日志的便捷方法，以及任务查询/重试。
 */
@Slf4j
@Service
public class TaskService extends ServiceImpl<TaskLogMapper, TaskLog> {

    /**
     * 创建一条 PENDING 任务日志，返回主键 ID。
     */
    public TaskLog createTask(Long userId, Long projectId, String bizType, Long bizId, String model, String input) {
        TaskLog task = new TaskLog();
        task.setUserId(userId);
        task.setProjectId(projectId);
        task.setBizType(bizType);
        task.setBizId(bizId);
        task.setModel(model);
        task.setInput(input);
        task.setStatus(TaskConstants.STATUS_PENDING);
        task.setProgress(0);
        task.setCreatedAt(LocalDateTime.now());
        baseMapper.insert(task);
        return task;
    }

    /**
     * 标记任务为 RUNNING，并记录使用的 Key 与 provider task id。
     */
    public void markRunning(Long taskId, Long apiKeyId, String providerTaskId) {
        TaskLog task = baseMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        task.setStatus(TaskConstants.STATUS_RUNNING);
        task.setApiKeyId(apiKeyId);
        if (providerTaskId != null) {
            task.setProviderTaskId(providerTaskId);
        }
        task.setStartedAt(LocalDateTime.now());
        baseMapper.updateById(task);
    }

    /**
     * 更新进度。
     */
    public void updateProgress(Long taskId, int progress) {
        TaskLog task = baseMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        task.setProgress(progress);
        baseMapper.updateById(task);
    }

    /**
     * 标记成功，记录输出。
     */
    public void markSuccess(Long taskId, String output) {
        TaskLog task = baseMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        task.setStatus(TaskConstants.STATUS_SUCCESS);
        task.setProgress(100);
        task.setOutput(output);
        task.setFinishedAt(LocalDateTime.now());
        baseMapper.updateById(task);
    }

    /**
     * 标记失败，记录错误信息。
     */
    public void markFailed(Long taskId, String errorMsg) {
        TaskLog task = baseMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        task.setStatus(TaskConstants.STATUS_FAILED);
        task.setErrorMsg(errorMsg);
        task.setFinishedAt(LocalDateTime.now());
        baseMapper.updateById(task);
    }

    /**
     * 查询指定业务类型与状态的任务列表。
     */
    public List<TaskLog> listByBizTypeAndStatus(String bizType, String status) {
        return baseMapper.selectByBizTypeAndStatus(bizType, status);
    }

    /**
     * 分页查询任务（可按用户与项目过滤）。
     */
    public PageResult<TaskLog> page(int page, int size, Long userId, Long projectId) {
        Page<TaskLog> p = new Page<>(page, size);
        LambdaQueryWrapper<TaskLog> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(TaskLog::getUserId, userId);
        }
        if (projectId != null) {
            wrapper.eq(TaskLog::getProjectId, projectId);
        }
        wrapper.orderByDesc(TaskLog::getCreatedAt);
        IPage<TaskLog> result = baseMapper.selectPage(p, wrapper);
        return new PageResult<>(page, size, result.getTotal(), result.getRecords());
    }

    /**
     * 重试任务：仅将状态改回 PENDING（具体业务重试由各模块触发）。
     */
    public TaskLog retry(Long taskId) {
        TaskLog task = baseMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(404, "任务不存在");
        }
        if (!TaskConstants.STATUS_FAILED.equals(task.getStatus())) {
            throw new BusinessException(400, "仅失败任务可重试");
        }
        task.setStatus(TaskConstants.STATUS_PENDING);
        task.setProgress(0);
        task.setErrorMsg(null);
        task.setFinishedAt(null);
        task.setStartedAt(null);
        baseMapper.updateById(task);
        return task;
    }
}
