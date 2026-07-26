package com.ai.comic.creation.service;

import com.ai.comic.common.BusinessException;
import com.ai.comic.creation.entity.Storyboard;
import com.ai.comic.creation.mapper.StoryboardMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 分镜服务。
 */
@Service
public class StoryboardService extends ServiceImpl<StoryboardMapper, Storyboard> {

    /**
     * 查询项目下全部分镜（按 seq 排序）。
     */
    public List<Storyboard> listByProject(Long projectId) {
        return baseMapper.selectList(new LambdaQueryWrapper<Storyboard>()
                .eq(Storyboard::getProjectId, projectId)
                .orderByAsc(Storyboard::getSeq));
    }

    /**
     * 获取分镜，校验存在。
     */
    public Storyboard getByIdOwned(Long storyboardId) {
        Storyboard sb = baseMapper.selectById(storyboardId);
        if (sb == null) {
            throw new BusinessException(404, "分镜不存在");
        }
        return sb;
    }

    /**
     * 更新单个分镜（Prompt/台词/时长/关联角色）。
     */
    public Storyboard update(Long storyboardId, String prompt, String dialogue,
                             String narration, java.math.BigDecimal durationSec,
                             String characterIds) {
        Storyboard sb = getByIdOwned(storyboardId);
        if (prompt != null) sb.setPrompt(prompt);
        if (dialogue != null) sb.setDialogue(dialogue);
        if (narration != null) sb.setNarration(narration);
        if (durationSec != null) sb.setDurationSec(durationSec);
        if (characterIds != null) sb.setCharacterIds(characterIds);
        baseMapper.updateById(sb);
        return sb;
    }

    /**
     * 替换项目的全部分镜（生成分镜后调用）。
     */
    public void replaceProjectStoryboards(Long projectId, List<Storyboard> storyboards) {
        // 先删除旧的
        baseMapper.delete(new LambdaQueryWrapper<Storyboard>()
                .eq(Storyboard::getProjectId, projectId));
        // 再插入新的
        int seq = 1;
        for (Storyboard sb : storyboards) {
            sb.setId(null);
            sb.setProjectId(projectId);
            if (sb.getSeq() == null) {
                sb.setSeq(seq);
            }
            seq++;
            baseMapper.insert(sb);
        }
    }
}
