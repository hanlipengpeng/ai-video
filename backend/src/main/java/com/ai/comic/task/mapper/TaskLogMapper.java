package com.ai.comic.task.mapper;

import com.ai.comic.task.entity.TaskLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 任务日志 Mapper。
 */
@Mapper
public interface TaskLogMapper extends BaseMapper<TaskLog> {

    /**
     * 查询指定业务类型与状态的任务（用于视频轮询，文档 16.5）。
     */
    @Select("SELECT * FROM task_log WHERE biz_type = #{bizType} AND status = #{status} ORDER BY created_at ASC")
    List<TaskLog> selectByBizTypeAndStatus(@Param("bizType") String bizType, @Param("status") String status);
}
