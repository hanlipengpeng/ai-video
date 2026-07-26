package com.ai.comic.aigc;

import com.ai.comic.aigc.config.AgnesProperties;
import com.ai.comic.aigc.entity.ApiKey;
import com.ai.comic.aigc.mapper.ApiKeyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Agnes AI Key 池（文档第 15 章）。
 * <p>
 * 职责：
 * <ul>
 *   <li>启动时从 api_key 表加载 Key（若表为空，则从 application.yml 注入）；</li>
 *   <li>内存维护每个 Key 各模型类型的最近 1 分钟调用次数（滑动窗口）；</li>
 *   <li>按模型类型选 Key：过滤 DISABLED/COOLED/RPM 用尽的 Key，轮询选取；</li>
 *   <li>429 时标记 COOLED 60s，401 时标记 DISABLED；</li>
 *   <li>所有 Key 都不可用时抛 {@link NoAvailableKeyException}，调用方应让任务排队。</li>
 * </ul>
 */
@Slf4j
@Component
public class KeyPool {

    @Autowired
    private ApiKeyMapper apiKeyMapper;

    @Autowired
    private AgnesProperties agnesProperties;

    /** 内存中的 Key 列表 */
    private final CopyOnWriteArrayList<ApiKey> keys = new CopyOnWriteArrayList<>();

    /** 每个 Key 各模型类型最近 1 分钟的调用时间戳窗口（用于 RPM 计数） */
    private final Map<Long, Map<ModelType, List<Long>>> rpmWindows = new ConcurrentHashMap<>();

    /** 轮询游标（按模型类型分别轮询） */
    private final Map<ModelType, AtomicInteger> cursors = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 先从数据库加载
        List<ApiKey> dbKeys = apiKeyMapper.selectList(new LambdaQueryWrapper<>());
        if (dbKeys != null && !dbKeys.isEmpty()) {
            keys.addAll(dbKeys);
            log.info("从 api_key 表加载 {} 个 Key", dbKeys.size());
        } else {
            // 数据库为空时，从 yml 配置注入
            for (AgnesProperties.KeyConfig kc : agnesProperties.getKeys()) {
                if (kc.getApiKey() == null || kc.getApiKey().isBlank()) {
                    log.warn("跳过空 Key 配置: name={}", kc.getName());
                    continue;
                }
                ApiKey k = new ApiKey();
                k.setName(kc.getName());
                k.setApiKey(kc.getApiKey());
                k.setPlan(kc.getPlan());
                k.setStatus("ACTIVE");
                k.setTotalCalls(0L);
                k.setCreatedAt(LocalDateTime.now());
                k.setUpdatedAt(LocalDateTime.now());
                apiKeyMapper.insert(k);
                keys.add(k);
                log.info("从配置注入 Key 到 api_key 表: name={}, plan={}", kc.getName(), kc.getPlan());
            }
        }
        log.info("KeyPool 初始化完成，共 {} 个 Key", keys.size());
    }

    /**
     * 按模型类型选取一个可用 Key。
     * <p>
     * 过滤 DISABLED / COOLED / 当前模型 RPM 用尽的 Key，剩余轮询选取。
     *
     * @param modelType 模型类型
     * @return 可用 Key
     * @throws NoAvailableKeyException 所有 Key 不可用时抛出
     */
    public synchronized ApiKey select(ModelType modelType) {
        cleanupExpiredCooldowns();
        List<ApiKey> available = new ArrayList<>();
        for (ApiKey k : keys) {
            if (!"ACTIVE".equals(k.getStatus())) {
                continue;
            }
            if (isRpmExhausted(k, modelType)) {
                continue;
            }
            available.add(k);
        }
        if (available.isEmpty()) {
            throw new NoAvailableKeyException("无可用 Agnes Key（全部冷却/禁用/达 RPM 上限）");
        }
        // 轮询
        AtomicInteger cursor = cursors.computeIfAbsent(modelType, mt -> new AtomicInteger(0));
        int idx = Math.abs(cursor.getAndIncrement()) % available.size();
        ApiKey selected = available.get(idx);
        recordCall(selected, modelType);
        // 更新数据库 last_used_at / total_calls
        selected.setLastUsedAt(LocalDateTime.now());
        selected.setTotalCalls((selected.getTotalCalls() == null ? 0 : selected.getTotalCalls()) + 1);
        try {
            apiKeyMapper.updateById(selected);
        } catch (Exception e) {
            log.warn("更新 api_key 使用统计失败: id={}, err={}", selected.getId(), e.getMessage());
        }
        log.debug("选取 Key: id={}, name={}, model={}", selected.getId(), selected.getName(), modelType);
        return selected;
    }

    /**
     * 标记 Key 限流冷却（429）。
     */
    public void markCooled(Long keyId) {
        for (ApiKey k : keys) {
            if (k.getId().equals(keyId)) {
                int coolSec = agnesProperties.getRateLimit().getCoolDownSec();
                k.setStatus("COOLED");
                k.setCooledUntil(LocalDateTime.now().plusSeconds(coolSec));
                k.setUpdatedAt(LocalDateTime.now());
                apiKeyMapper.updateById(k);
                log.warn("Key 限流冷却: id={}, name={}, 冷却 {}s", k.getId(), k.getName(), coolSec);
                return;
            }
        }
    }

    /**
     * 标记 Key 失效（401 鉴权失败）。
     */
    public void markDisabled(Long keyId) {
        for (ApiKey k : keys) {
            if (k.getId().equals(keyId)) {
                k.setStatus("DISABLED");
                k.setUpdatedAt(LocalDateTime.now());
                apiKeyMapper.updateById(k);
                log.error("Key 鉴权失败已禁用: id={}, name={}", k.getId(), k.getName());
                return;
            }
        }
    }

    /**
     * 清理已过期的冷却 Key，恢复为 ACTIVE（文档 15.3 状态机）。
     */
    private void cleanupExpiredCooldowns() {
        LocalDateTime now = LocalDateTime.now();
        for (ApiKey k : keys) {
            if ("COOLED".equals(k.getStatus()) && k.getCooledUntil() != null
                    && k.getCooledUntil().isBefore(now)) {
                k.setStatus("ACTIVE");
                k.setCooledUntil(null);
                k.setUpdatedAt(now);
                apiKeyMapper.updateById(k);
                log.info("Key 冷却到期恢复: id={}, name={}", k.getId(), k.getName());
            }
        }
    }

    /**
     * 判断指定 Key 在指定模型类型上是否已达 RPM 上限。
     * <p>
     * 滑动窗口：统计最近 60 秒内的调用次数。
     */
    private boolean isRpmExhausted(ApiKey key, ModelType modelType) {
        AgnesProperties.PlanRpm planRpm = agnesProperties.getRpm().ofPlan(key.getPlan());
        int limit = modelType.rpmOf(planRpm);
        if (limit <= 0) {
            return false;
        }
        Map<ModelType, List<Long>> perModel = rpmWindows.get(key.getId());
        if (perModel == null) {
            return false;
        }
        List<Long> timestamps = perModel.get(modelType);
        if (timestamps == null || timestamps.isEmpty()) {
            return false;
        }
        long now = System.currentTimeMillis();
        long windowStart = now - 60_000L;
        // 清理过期时间戳并统计
        timestamps.removeIf(ts -> ts < windowStart);
        return timestamps.size() >= limit;
    }

    /**
     * 记录一次调用到 RPM 滑动窗口。
     */
    private void recordCall(ApiKey key, ModelType modelType) {
        Map<ModelType, List<Long>> perModel = rpmWindows.computeIfAbsent(key.getId(), k -> new ConcurrentHashMap<>());
        List<Long> timestamps = perModel.computeIfAbsent(modelType, mt -> new CopyOnWriteArrayList<>());
        long now = System.currentTimeMillis();
        timestamps.add(now);
        // 顺便清理过期
        long windowStart = now - 60_000L;
        timestamps.removeIf(ts -> ts < windowStart);
    }

    /**
     * 查询所有 Key 状态（运维查看，文档 15.10）。
     */
    public List<ApiKey> snapshot() {
        cleanupExpiredCooldowns();
        return new ArrayList<>(keys);
    }
}
