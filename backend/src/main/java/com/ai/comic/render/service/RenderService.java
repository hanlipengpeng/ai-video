package com.ai.comic.render.service;

import com.ai.comic.common.BusinessException;
import com.ai.comic.common.ProjectStatus;
import com.ai.comic.common.StorageService;
import com.ai.comic.common.TaskConstants;
import com.ai.comic.config.StorageProperties;
import com.ai.comic.creation.entity.Storyboard;
import com.ai.comic.creation.service.StoryboardService;
import com.ai.comic.media.entity.VideoClip;
import com.ai.comic.media.service.VideoService;
import com.ai.comic.project.entity.Project;
import com.ai.comic.project.service.ProjectService;
import com.ai.comic.task.entity.TaskLog;
import com.ai.comic.task.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 渲染合成服务（文档 3.2.7、4.2）。
 * <p>
 * 用 FFmpeg 拼接所有 video_clip + 字幕（SRT），输出 1080p 竖屏 mp4。
 * <p>
 * 流程：
 * <ol>
 *   <li>校验所有视频片段就绪；</li>
 *   <li>从 storyboard.dialogue 生成 SRT 字幕文件；</li>
 *   <li>用 concat 协议拼接视频片段；</li>
 *   <li>烧录字幕（subtitles filter）；</li>
 *   <li>输出 1080x1920 mp4 到本地存储。</li>
 * </ol>
 * <p>
 * 注意：异步方法通过 {@code self} 引用调用，避免 Spring AOP 自调用导致 @Async 失效。
 */
@Slf4j
@Service
public class RenderService {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private StoryboardService storyboardService;

    @Autowired
    private VideoService videoService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private StorageService storageService;

    @Autowired
    private StorageProperties storageProperties;

    /** 自引用，确保 @Async 方法经由 Spring 代理调用 */
    @Autowired
    @Lazy
    private RenderService self;

    /**
     * 提交合成任务。
     */
    public TaskLog submitComposeTask(Long userId, Long projectId) {
        Project p = projectService.getOwned(projectId, userId);
        List<Storyboard> storyboards = storyboardService.listByProject(projectId);
        if (storyboards.isEmpty()) {
            throw new BusinessException(400, "请先生成分镜");
        }
        // 校验所有视频片段就绪
        List<VideoClip> readyClips = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        double totalDuration = 0;
        for (Storyboard sb : storyboards) {
            VideoClip clip = videoService.getLatestByStoryboard(sb.getId());
            if (clip == null || !"SUCCESS".equals(clip.getStatus()) || clip.getVideoUrl() == null) {
                missing.add("第" + sb.getSeq() + "镜");
            } else {
                readyClips.add(clip);
                if (clip.getDurationSec() != null) {
                    totalDuration += clip.getDurationSec().doubleValue();
                } else {
                    totalDuration += 5.0;
                }
            }
        }
        if (readyClips.isEmpty()) {
            throw new BusinessException(400, "没有可用的视频片段，请先生成");
        }
        if (!missing.isEmpty()) {
            log.warn("部分分镜缺少视频片段，将跳过: {}", missing);
        }

        TaskLog task = taskService.createTask(userId, projectId, TaskConstants.BIZ_COMPOSE,
                projectId, "ffmpeg",
                "{\"clipCount\":" + readyClips.size() + ",\"skipped\":" + missing.size() + "}");
        projectService.transitStatus(projectId, ProjectStatus.COMPOSING);
        final List<VideoClip> finalClips = readyClips;
        final double finalDuration = totalDuration;
        self.doComposeAsync(task.getId(), projectId, storyboards, finalClips, finalDuration);
        return task;
    }

    @Async("aiTaskExecutor")
    public void doComposeAsync(Long taskId, Long projectId, List<Storyboard> storyboards,
                               List<VideoClip> clips, double totalDuration) {
        try {
            taskService.markRunning(taskId, null, null);

            // 工作目录
            String workDir = storageProperties.getStorage().getLocalPath() + "/compose/" + projectId + "/" + UUID.randomUUID();
            Files.createDirectories(Paths.get(workDir));

            // 1. 生成 concat 列表文件
            Path concatList = Paths.get(workDir, "concat.txt");
            try (BufferedWriter writer = Files.newBufferedWriter(concatList, StandardCharsets.UTF_8)) {
                for (VideoClip clip : clips) {
                    Path local = storageService.resolveLocalPath(clip.getVideoUrl());
                    writer.write("file '" + local.toString().replace("'", "\\'") + "'");
                    writer.newLine();
                }
            }

            // 2. 拼接后的中间视频
            Path mergedVideo = Paths.get(workDir, "merged.mp4");

            // 3. 生成 SRT 字幕文件
            Path srtFile = Paths.get(workDir, "subtitle.srt");
            generateSrt(srtFile, storyboards, clips);

            // 4. 最终输出文件
            String dateDir = java.time.LocalDate.now().toString();
            String fileName = UUID.randomUUID().toString().replace("-", "") + ".mp4";
            Path outputDir = Paths.get(storageProperties.getStorage().getLocalPath(), "compose_final", dateDir);
            Files.createDirectories(outputDir);
            Path finalVideo = outputDir.resolve(fileName);

            // 5. 调用 FFmpeg：先 concat 拼接，再烧录字幕 + 缩放到 1080x1920
            int step = 0;
            // Step 1: concat
            runFfmpeg("-f", "concat", "-safe", "0", "-i", concatList.toString(),
                    "-c", "copy", mergedVideo.toString());
            step++;
            taskService.updateProgress(taskId, 50);

            // Step 2: 烧录字幕 + 1080x1920 竖屏
            String vf = "scale=1080:1920:force_original_aspect_ratio=decrease,"
                    + "pad=1080:1920:(ow-iw)/2:(oh-ih)/2:black,"
                    + "subtitles=" + srtFile.toString().replace(":", "\\:").replace("'", "\\'");
            runFfmpeg("-i", mergedVideo.toString(),
                    "-vf", vf,
                    "-c:v", "libx264", "-preset", "medium", "-crf", "23",
                    "-c:a", "aac", "-b:a", "128k",
                    "-r", "30",
                    "-y", finalVideo.toString());
            taskService.updateProgress(taskId, 90);

            // 6. 更新项目最终视频 URL
            String relativeUrl = storageProperties.getStorage().getUrlPrefix()
                    + "/compose_final/" + dateDir + "/" + fileName;
            if (!relativeUrl.startsWith("/")) {
                relativeUrl = "/" + relativeUrl;
            }
            int durationSec = (int) Math.ceil(totalDuration);
            projectService.saveFinalVideo(projectId, relativeUrl, durationSec);

            // 清理工作目录
            try {
                deleteRecursive(Paths.get(workDir));
            } catch (Exception e) {
                log.warn("清理工作目录失败: {}", workDir);
            }

            taskService.markSuccess(taskId, "{\"videoUrl\":\"" + relativeUrl + "\",\"duration\":" + durationSec + "}");
            log.info("视频合成成功: projectId={}, url={}", projectId, relativeUrl);
        } catch (Exception e) {
            log.error("视频合成失败: projectId={}", projectId, e);
            taskService.markFailed(taskId, e.getMessage());
        }
    }

    /**
     * 生成 SRT 字幕文件（文档 3.2.7）。
     * <p>
     * 从 storyboard.dialogue 生成，按分镜时长累计时间轴。
     */
    private void generateSrt(Path srtFile, List<Storyboard> storyboards, List<VideoClip> clips) throws IOException {
        // 按 clip 顺序对应 storyboard（clips 已经过滤，需重新匹配）
        List<Storyboard> orderedStoryboards = new ArrayList<>();
        for (VideoClip clip : clips) {
            for (Storyboard sb : storyboards) {
                if (sb.getId().equals(clip.getStoryboardId())) {
                    orderedStoryboards.add(sb);
                    break;
                }
            }
        }

        try (BufferedWriter writer = Files.newBufferedWriter(srtFile, StandardCharsets.UTF_8)) {
            int index = 1;
            double currentTime = 0;
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss,SSS");
            for (int i = 0; i < orderedStoryboards.size(); i++) {
                Storyboard sb = orderedStoryboards.get(i);
                VideoClip clip = clips.get(i);
                double dur = clip.getDurationSec() != null ? clip.getDurationSec().doubleValue() : 5.0;
                // 合并台词与旁白
                String text = buildSubtitleText(sb);
                if (text == null || text.isBlank()) {
                    currentTime += dur;
                    continue;
                }
                double start = currentTime;
                double end = currentTime + dur;
                writer.write(String.valueOf(index++));
                writer.newLine();
                writer.write(toSrtTime(start, fmt) + " --> " + toSrtTime(end, fmt));
                writer.newLine();
                writer.write(text);
                writer.newLine();
                writer.newLine();
                currentTime = end;
            }
        }
    }

    private String buildSubtitleText(Storyboard sb) {
        StringBuilder sbText = new StringBuilder();
        if (sb.getDialogue() != null && !sb.getDialogue().isBlank()) {
            sbText.append(sb.getDialogue());
        }
        if (sb.getNarration() != null && !sb.getNarration().isBlank()) {
            if (sbText.length() > 0) sbText.append("\n");
            sbText.append("（旁白）").append(sb.getNarration());
        }
        return sbText.toString();
    }

    private String toSrtTime(double seconds, DateTimeFormatter fmt) {
        long totalMs = (long) (seconds * 1000);
        long hours = totalMs / 3_600_000;
        long minutes = (totalMs % 3_600_000) / 60_000;
        long secs = (totalMs % 60_000) / 1000;
        long ms = totalMs % 1000;
        return LocalTime.of((int) hours, (int) minutes, (int) secs).format(fmt)
                .replaceAll("\\d{3}$", String.format("%03d", ms));
    }

    /**
     * 调用 ffmpeg 命令。
     */
    private void runFfmpeg(String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(storageProperties.getFfmpeg().getPath());
        command.addAll(List.of(args));
        log.debug("执行 ffmpeg: {}", String.join(" ", command));
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        // 读取输出避免管道阻塞
        byte[] output = process.getInputStream().readAllBytes();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String err = new String(output, StandardCharsets.UTF_8);
            log.error("ffmpeg 失败 exitCode={}, output={}", exitCode,
                    err.length() > 2000 ? err.substring(0, 2000) : err);
            throw new BusinessException(500, "ffmpeg 执行失败 exitCode=" + exitCode);
        }
    }

    private void deleteRecursive(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (var stream = Files.list(path)) {
                for (Path child : stream.toList()) {
                    deleteRecursive(child);
                }
            }
        }
        Files.deleteIfExists(path);
    }
}
