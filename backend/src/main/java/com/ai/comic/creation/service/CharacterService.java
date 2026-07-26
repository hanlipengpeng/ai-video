package com.ai.comic.creation.service;

import com.ai.comic.common.BusinessException;
import com.ai.comic.creation.entity.Character;
import com.ai.comic.creation.mapper.CharacterMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 角色服务。
 */
@Service
public class CharacterService extends ServiceImpl<CharacterMapper, Character> {

    /**
     * 查询项目下所有角色。
     */
    public List<Character> listByProject(Long projectId) {
        return baseMapper.selectList(new LambdaQueryWrapper<Character>()
                .eq(Character::getProjectId, projectId)
                .orderByAsc(Character::getId));
    }

    /**
     * 按 ID 批量查询。
     */
    public List<Character> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return baseMapper.selectBatchIds(ids);
    }

    /**
     * 获取角色，校验归属项目。
     */
    public Character getByIdOwned(Long characterId) {
        Character c = baseMapper.selectById(characterId);
        if (c == null) {
            throw new BusinessException(404, "角色不存在");
        }
        return c;
    }

    /**
     * 更新角色（外貌描述等）。
     */
    public Character update(Long characterId, String appearance, String personality, String aliases, String name) {
        Character c = getByIdOwned(characterId);
        if (name != null) c.setName(name);
        if (appearance != null) c.setAppearance(appearance);
        if (personality != null) c.setPersonality(personality);
        if (aliases != null) c.setAliases(aliases);
        baseMapper.updateById(c);
        return c;
    }

    /**
     * 保存角色参考图 URL。
     */
    public void saveReferenceImage(Long characterId, String url) {
        Character c = getByIdOwned(characterId);
        c.setReferenceImageUrl(url);
        c.setStatus("READY");
        baseMapper.updateById(c);
    }
}
