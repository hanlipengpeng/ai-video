package com.ai.comic.project.service;

import com.ai.comic.common.BusinessException;
import com.ai.comic.common.PageResult;
import com.ai.comic.common.ProjectStatus;
import com.ai.comic.project.entity.Project;
import com.ai.comic.project.mapper.ProjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 项目服务（文档 7.2.2）。
 * <p>
 * 负责项目 CRUD 与状态流转。
 */
@Service
public class ProjectService extends ServiceImpl<ProjectMapper, Project> {

    /**
     * 创建项目。
     */
    public Project create(Long userId, String title, String stylePreset, String aspectRatio, String sourceText) {
        Project p = new Project();
        p.setUserId(userId);
        p.setTitle(title == null || title.isBlank() ? "未命名项目" : title);
        p.setStylePreset(stylePreset == null || stylePreset.isBlank() ? "anime_jp" : stylePreset);
        p.setAspectRatio(aspectRatio == null || aspectRatio.isBlank() ? "9:16" : aspectRatio);
        p.setStatus(ProjectStatus.DRAFT);
        p.setSourceText(sourceText);
        baseMapper.insert(p);
        return p;
    }

    /**
     * 查询当前用户的项目（分页）。
     */
    public PageResult<Project> pageByUser(Long userId, int page, int size) {
        Page<Project> p = new Page<>(page, size);
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<Project>()
                .eq(Project::getUserId, userId)
                .orderByDesc(Project::getCreatedAt);
        IPage<Project> result = baseMapper.selectPage(p, wrapper);
        return new PageResult<>(page, size, result.getTotal(), result.getRecords());
    }

    /**
     * 获取项目详情，并校验归属当前用户。
     */
    public Project getOwned(Long projectId, Long userId) {
        Project p = baseMapper.selectById(projectId);
        if (p == null) {
            throw new BusinessException(404, "项目不存在");
        }
        if (!p.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权访问该项目");
        }
        return p;
    }

    /**
     * 更新项目（标题、画风、原文）。
     */
    public Project update(Long projectId, Long userId, String title, String stylePreset,
                          String aspectRatio, String sourceText) {
        Project p = getOwned(projectId, userId);
        if (title != null) p.setTitle(title);
        if (stylePreset != null) p.setStylePreset(stylePreset);
        if (aspectRatio != null) p.setAspectRatio(aspectRatio);
        if (sourceText != null) p.setSourceText(sourceText);
        baseMapper.updateById(p);
        return p;
    }

    /**
     * 删除项目。
     */
    public void delete(Long projectId, Long userId) {
        Project p = getOwned(projectId, userId);
        baseMapper.deleteById(p.getId());
    }

    /**
     * 切换项目状态（文档 4.3 状态机）。
     */
    public void transitStatus(Long projectId, String newStatus) {
        Project p = baseMapper.selectById(projectId);
        if (p == null) {
            throw new BusinessException(404, "项目不存在");
        }
        p.setStatus(newStatus);
        baseMapper.updateById(p);
    }

    /**
     * 保存剧本 JSON 到项目。
     */
    public void saveScript(Long projectId, String scriptJson) {
        Project p = baseMapper.selectById(projectId);
        if (p == null) {
            throw new BusinessException(404, "项目不存在");
        }
        p.setScriptContent(scriptJson);
        baseMapper.updateById(p);
    }

    /**
     * 保存最终视频 URL 与时长。
     */
    public void saveFinalVideo(Long projectId, String videoUrl, Integer durationSec) {
        Project p = baseMapper.selectById(projectId);
        if (p == null) {
            return;
        }
        p.setFinalVideoUrl(videoUrl);
        p.setDurationSec(durationSec);
        p.setStatus(ProjectStatus.READY);
        baseMapper.updateById(p);
    }
}
