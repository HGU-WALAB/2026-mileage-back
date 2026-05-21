package com.csee.swplus.mileage.portfolio.service;

import com.csee.swplus.mileage.portfolio.dto.*;
import com.csee.swplus.mileage.portfolio.prompt.PortfolioPromptLoader;
import com.csee.swplus.mileage.profile.entity.Profile;
import com.csee.swplus.mileage.profile.repository.ProfileRepository;
import com.csee.swplus.mileage.user.entity.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Exports portfolio data as a single-file HTML page (recruiter-optimized, print-friendly).
 * **blueStyle** layout: sidebar (name, role, school, meta-line, summary-chip, tech pills, contact) +
 * main (About, Projects, Mileage & extracurricular timeline, optional Achievements, Activities, footer).
 *
 * <p>LLM prompts load fragments from {@code classpath:prompts/} via {@link PortfolioPromptLoader}.
 * Verbatim default blue CSS is embedded **only** when STEP 2 has no effective {@code [design_preferences]}.
 */
@Service
@RequiredArgsConstructor
public class PortfolioHtmlExportService {

    /** Mileage / activity text that likely denotes an award (for ACHIEVEMENTS section, blueStyle). */

    private final PortfolioService portfolioService;
    private final ProfileRepository profileRepository;
    private final PortfolioPromptLoader promptLoader;

    /**
     * Must match {@code server.servlet.context-path} (e.g. {@code /milestone25}) so export {@code img} URLs
     * resolve like the live app ({@code /milestone25/api/...}), not {@code /api/...} at domain root.
     */
    @Value("${server.servlet.context-path:}")
    private String servletContextPath;

    @Value("${file.portfolio-profile-upload-dir:${file.profile-upload-dir:./uploads/profile}}")
    private String profileUploadDir;

    /**
     * Generates HTML portfolio for the given user. Uses visible repos only.
     */
    public String generateHtml(Users user) {
        UserInfoResponse userInfo = portfolioService.getUserInfo(user);
        TechStackResponse techStack = portfolioService.getTechStack(user);
        RepositoriesResponse repos = portfolioService.getRepositories(user, 1, 100, null, true);
        MileageListResponse mileage = portfolioService.getMileageList(user);
        ActivitiesResponse activities = portfolioService.getActivities(user, null);

        String githubUrl = null;
        Profile profile = profileRepository.findBySnum(user.getUniqueId()).orElse(null);
        if (profile != null && profile.getGithubLink() != null && !profile.getGithubLink().isEmpty()) {
            githubUrl = profile.getGithubLink();
        } else if (profile != null && profile.getGithubUsername() != null) {
            githubUrl = "https://github.com/" + profile.getGithubUsername();
        }

        return buildHtml(userInfo, techStack, repos.getRepositories(),
                mileage.getMileage(), activities.getActivities(), githubUrl, user.getEmail());
    }

    /**
     * Builds portfolio data in the STEP 2 INPUT DATA format for LLM prompt / full test.
     * Returns plain text that can be pasted into the prompt template.
     */
    public String buildPromptInputData(Users user) {
        UserInfoResponse userInfo = portfolioService.getUserInfo(user);
        TechStackResponse techStack = portfolioService.getTechStack(user);
        RepositoriesResponse repos = portfolioService.getRepositories(user, 1, 100, null, true);
        MileageListResponse mileage = portfolioService.getMileageList(user);
        ActivitiesResponse activities = portfolioService.getActivities(user, null);

        String githubUrl = null;
        Profile profile = profileRepository.findBySnum(user.getUniqueId()).orElse(null);
        if (profile != null && profile.getGithubLink() != null && !profile.getGithubLink().isEmpty()) {
            githubUrl = profile.getGithubLink();
        } else if (profile != null && profile.getGithubUsername() != null) {
            githubUrl = "https://github.com/" + profile.getGithubUsername();
        }
        String email = user.getEmail();

        StringBuilder sb = new StringBuilder();
        sb.append("[bio]\n");
        sb.append("- 이름: ").append(nullToEmpty(userInfo.getName())).append("\n");
        sb.append("- 학교/학과: ").append(schoolDept(userInfo)).append("\n");
        sb.append("- 학년/학기: (").append(nullToEmpty(userInfo.getGrade())).append("학년 ")
          .append(nullToEmpty(userInfo.getSemester())).append("학기)\n");
        sb.append("- 한줄 자기소개: ").append(nullToEmpty(userInfo.getBio())).append("\n");
        appendProfileLinksLines(sb, userInfo);
        sb.append("\n");

        appendTechStackPlainText(sb, techStack);

        sb.append("[github_repos]\n");
        if (repos.getRepositories() != null) {
            for (RepoEntryResponse r : repos.getRepositories()) {
                appendGithubRepoPromptLines(sb, r);
            }
        }
        sb.append("\n");

        sb.append("[mileage_list]\n");
        if (mileage.getMileage() != null) {
            for (MileageEntryResponse m : mileage.getMileage()) {
                String sem = nullToEmpty(m.getSemester());
                String cat = nullToEmpty(m.getCategoryName());
                String sub = nullToEmpty(m.getSubitemName());
                String add = m.getAdditional_info() != null && !m.getAdditional_info().isEmpty()
                        ? m.getAdditional_info() : nullToEmpty(m.getDescription1());
                sb.append("- ").append(sem).append(" ").append(cat).append(" - ").append(sub)
                  .append(" · ").append(add).append("\n");
            }
        }
        sb.append("\n");

        sb.append("[activities]\n");
        if (activities.getActivities() != null) {
            for (ActivityResponse a : activities.getActivities()) {
                appendActivityPromptLine(sb, a);
            }
        }
        sb.append("\n");

        sb.append("[contact]\n");
        sb.append("- GitHub URL: ").append(githubUrl != null ? githubUrl : "").append("\n");
        sb.append("- Email: ").append(email != null ? email : "").append("\n");

        return sb.toString();
    }

    /**
     * Builds the full LLM prompt (ROLE, TASK, STEP 1-5) with user's portfolio data filled into STEP 2.
     * Ready to paste into an LLM for portfolio HTML generation.
     */
    public String buildFullPrompt(Users user) {
        String inputData = buildPromptInputData(user);
        return cvPromptHead() + inputData + "\n```\n\n" + buildCvPromptAfterStep2(true, null);
    }

    /**
     * Builds CV-specific LLM prompt with job info and selected portfolio items only.
     * Bio and tech stack are always included. Repos, mileage, activities are filtered by selected IDs.
     */
    public String buildCvPrompt(Users user, CvBuildPromptRequest request) {
        return buildCvHtmlGenerationPrompt(user, request, false, null);
    }

    /**
     * CV HTML prompt with optional STEP 0 chain-of-thought block (single-phase mode) and/or attached planner JSON (two-phase render).
     */
    public String buildCvHtmlGenerationPrompt(Users user, CvBuildPromptRequest request,
            boolean prependStep0CoT, String portfolioPlanJsonOrNull) {
        CompiledPortfolioPromptInput in = compileCvPortfolioInput(user, request);
        return assembleCvHtmlPrompt(in, prependStep0CoT, portfolioPlanJsonOrNull);
    }

    /**
     * Call 1 — planner only: STEP 2 body + instructions to emit a JSON section plan (no HTML).
     */
    public String buildCvPortfolioPlanPrompt(Users user, CvBuildPromptRequest request) {
        CompiledPortfolioPromptInput in = compileCvPortfolioInput(user, request);
        return promptLoader.load("shared/portfolio-plan-head-cv.txt")
                + in.step2Body
                + promptLoader.load("shared/portfolio-plan-tail.txt");
    }

    /**
     * Reflective “archive” prompt: neutral tone, one-shot HTML, chronological/category ordering in STEP 2.
     * Same data sources and selection IDs as {@link #buildCvPrompt}; uses archive prompt fragments only.
     */
    public String buildArchivePrompt(Users user, CvBuildPromptRequest request) {
        return buildArchiveHtmlGenerationPrompt(user, request, false, null);
    }

    /**
     * Archive HTML prompt with optional STEP 0 CoT and/or planner JSON attachment.
     */
    public String buildArchiveHtmlGenerationPrompt(Users user, CvBuildPromptRequest request,
            boolean prependStep0CoT, String portfolioPlanJsonOrNull) {
        CompiledPortfolioPromptInput in = compileArchivePortfolioInput(user, request);
        return assembleArchiveHtmlPrompt(in, prependStep0CoT, portfolioPlanJsonOrNull);
    }

    /**
     * Call 1 — archive planner prompt.
     */
    public String buildArchivePortfolioPlanPrompt(Users user, CvBuildPromptRequest request) {
        CompiledPortfolioPromptInput in = compileArchivePortfolioInput(user, request);
        return promptLoader.load("shared/portfolio-plan-head-archive.txt")
                + in.step2Body
                + promptLoader.load("shared/portfolio-plan-tail.txt");
    }

    /**
     * STEP 2 fenced body plus whether legacy blue CSS should be embedded in STEP 5-A.
     */
    private static final class CompiledPortfolioPromptInput {
        private final String step2Body;
        private final boolean embedLegacyCss;
        private final DesignPreferencesDto designPreferences;

        private CompiledPortfolioPromptInput(String step2Body, boolean embedLegacyCss,
                DesignPreferencesDto designPreferences) {
            this.step2Body = step2Body;
            this.embedLegacyCss = embedLegacyCss;
            this.designPreferences = designPreferences;
        }
    }

    private CompiledPortfolioPromptInput compileCvPortfolioInput(Users user, CvBuildPromptRequest request) {
        java.util.Set<Long> repoIds = request.getSelected_repo_ids() != null
                ? new java.util.HashSet<>(request.getSelected_repo_ids()) : java.util.Collections.emptySet();
        java.util.Set<Long> mileageIds = request.getSelected_mileage_ids() != null
                ? new java.util.HashSet<>(request.getSelected_mileage_ids()) : java.util.Collections.emptySet();
        java.util.Set<Long> activityIds = request.getSelected_activity_ids() != null
                ? new java.util.HashSet<>(request.getSelected_activity_ids()) : java.util.Collections.emptySet();

        UserInfoResponse userInfo = portfolioService.getUserInfo(user);
        TechStackResponse techStack = portfolioService.getTechStack(user);
        RepositoriesResponse repos = portfolioService.getRepositories(user, 1, 100, true, false);
        MileageListResponse mileage = portfolioService.getMileageList(user);
        ActivitiesResponse activities = portfolioService.getActivities(user, null);

        String githubUrl = null;
        Profile profile = profileRepository.findBySnum(user.getUniqueId()).orElse(null);
        if (profile != null && profile.getGithubLink() != null && !profile.getGithubLink().isEmpty()) {
            githubUrl = profile.getGithubLink();
        } else if (profile != null && profile.getGithubUsername() != null) {
            githubUrl = "https://github.com/" + profile.getGithubUsername();
        }
        String email = user.getEmail();

        StringBuilder sb = new StringBuilder();

        sb.append("[job_info]\n");
        sb.append("- 공고정보: ").append(nullToEmpty(request.getJob_posting())).append("\n");
        sb.append("- 지원 직무: ").append(nullToEmpty(request.getTarget_position())).append("\n");
        sb.append("- 추가 요청사항: ").append(nullToEmpty(request.getAdditional_notes())).append("\n\n");

        sb.append("[bio]\n");
        sb.append("- 이름: ").append(nullToEmpty(userInfo.getName())).append("\n");
        sb.append("- 학교/학과: ").append(schoolDept(userInfo)).append("\n");
        sb.append("- 학년/학기: (").append(nullToEmpty(userInfo.getGrade())).append("학년 ")
          .append(nullToEmpty(userInfo.getSemester())).append("학기)\n");
        sb.append("- 한줄 자기소개: ").append(nullToEmpty(userInfo.getBio())).append("\n");
        appendProfileLinksLines(sb, userInfo);
        sb.append("\n");

        appendTechStackPlainText(sb, techStack);

        sb.append("[github_repos]\n");
        if (repos.getRepositories() != null) {
            for (RepoEntryResponse r : repos.getRepositories()) {
                if (r.getId() == null || !repoIds.contains(r.getId())) {
                    continue;
                }
                appendGithubRepoPromptLines(sb, r);
            }
        }
        sb.append("\n");

        sb.append("[mileage_list]\n");
        if (mileage.getMileage() != null) {
            for (MileageEntryResponse m : mileage.getMileage()) {
                if (m.getId() == null || !mileageIds.contains(m.getId())) {
                    continue;
                }
                String sem = nullToEmpty(m.getSemester());
                String cat = nullToEmpty(m.getCategoryName());
                String sub = nullToEmpty(m.getSubitemName());
                String add = m.getAdditional_info() != null && !m.getAdditional_info().isEmpty()
                        ? m.getAdditional_info() : nullToEmpty(m.getDescription1());
                sb.append("- ").append(sem).append(" ").append(cat).append(" - ").append(sub)
                  .append(" · ").append(add).append("\n");
            }
        }
        sb.append("\n");

        sb.append("[activities]\n");
        if (activities.getActivities() != null) {
            for (ActivityResponse a : activities.getActivities()) {
                if (a.getId() == null || !activityIds.contains(a.getId())) {
                    continue;
                }
                appendActivityPromptLine(sb, a);
            }
        }
        sb.append("\n");

        sb.append("[contact]\n");
        sb.append("- GitHub URL: ").append(githubUrl != null ? githubUrl : "").append("\n");
        sb.append("- Email: ").append(email != null ? email : "").append("\n");

        appendDesignPreferencesBlock(sb, request.getDesign_preferences());

        boolean embedLegacyCss = !hasEffectiveDesignPreferences(request.getDesign_preferences());
        return new CompiledPortfolioPromptInput(sb.toString(), embedLegacyCss, request.getDesign_preferences());
    }

    private CompiledPortfolioPromptInput compileArchivePortfolioInput(Users user, CvBuildPromptRequest request) {
        Set<Long> repoIds = request.getSelected_repo_ids() != null
                ? new HashSet<>(request.getSelected_repo_ids()) : Collections.emptySet();
        Set<Long> mileageIds = request.getSelected_mileage_ids() != null
                ? new HashSet<>(request.getSelected_mileage_ids()) : Collections.emptySet();
        Set<Long> activityIds = request.getSelected_activity_ids() != null
                ? new HashSet<>(request.getSelected_activity_ids()) : Collections.emptySet();

        UserInfoResponse userInfo = portfolioService.getUserInfo(user);
        TechStackResponse techStack = portfolioService.getTechStack(user);
        RepositoriesResponse repos = portfolioService.getRepositories(user, 1, 100, true, false);
        MileageListResponse mileage = portfolioService.getMileageList(user);
        ActivitiesResponse activities = portfolioService.getActivities(user, null);

        String githubUrl = null;
        Profile profile = profileRepository.findBySnum(user.getUniqueId()).orElse(null);
        if (profile != null && profile.getGithubLink() != null && !profile.getGithubLink().isEmpty()) {
            githubUrl = profile.getGithubLink();
        } else if (profile != null && profile.getGithubUsername() != null) {
            githubUrl = "https://github.com/" + profile.getGithubUsername();
        }
        String email = user.getEmail();

        List<MileageEntryResponse> mileageList = new ArrayList<>();
        if (mileage.getMileage() != null) {
            for (MileageEntryResponse m : mileage.getMileage()) {
                if (m.getId() != null && mileageIds.contains(m.getId())) {
                    mileageList.add(m);
                }
            }
        }
        mileageList.sort(Comparator
                .comparing((MileageEntryResponse m) -> m.getSemester() != null ? m.getSemester() : "\uFFFF")
                .thenComparing(m -> nullToEmpty(m.getCategoryName()))
                .thenComparing(m -> nullToEmpty(m.getSubitemName())));

        List<ActivityResponse> activityList = new ArrayList<>();
        if (activities.getActivities() != null) {
            for (ActivityResponse a : activities.getActivities()) {
                if (a.getId() != null && activityIds.contains(a.getId())) {
                    activityList.add(a);
                }
            }
        }
        activityList.sort(Comparator.comparing(ActivityResponse::getStart_date, Comparator.nullsLast(Comparator.naturalOrder())));

        List<RepoEntryResponse> repoList = new ArrayList<>();
        if (repos.getRepositories() != null) {
            for (RepoEntryResponse r : repos.getRepositories()) {
                if (r.getId() != null && repoIds.contains(r.getId())) {
                    repoList.add(r);
                }
            }
        }
        repoList.sort(Comparator.comparing(
                (RepoEntryResponse r) -> r.getUpdated_at() != null ? r.getUpdated_at() : "",
                Comparator.reverseOrder()));

        StringBuilder sb = new StringBuilder();

        sb.append("[self_assessment_context]\n");
        sb.append("- 관심 영역 (현재 관심·탐색 방향; 요청 필드 job_posting): ").append(nullToEmpty(request.getJob_posting())).append("\n");
        sb.append("- 초점·방향: ").append(nullToEmpty(request.getTarget_position())).append("\n");
        sb.append("- 추가 메모: ").append(nullToEmpty(request.getAdditional_notes())).append("\n\n");

        sb.append("[bio]\n");
        sb.append("- 이름: ").append(nullToEmpty(userInfo.getName())).append("\n");
        sb.append("- 학교/학과: ").append(schoolDept(userInfo)).append("\n");
        sb.append("- 학년/학기: (").append(nullToEmpty(userInfo.getGrade())).append("학년 ")
          .append(nullToEmpty(userInfo.getSemester())).append("학기)\n");
        sb.append("- 한줄 자기소개: ").append(nullToEmpty(userInfo.getBio())).append("\n");
        appendProfileLinksLines(sb, userInfo);
        sb.append("\n");

        appendTechStackPlainText(sb, techStack);

        sb.append("[mileage_list]\n");
        for (MileageEntryResponse m : mileageList) {
            String sem = nullToEmpty(m.getSemester());
            String cat = nullToEmpty(m.getCategoryName());
            String sub = nullToEmpty(m.getSubitemName());
            String add = m.getAdditional_info() != null && !m.getAdditional_info().isEmpty()
                    ? m.getAdditional_info() : nullToEmpty(m.getDescription1());
            sb.append("- ").append(sem).append(" ").append(cat).append(" - ").append(sub)
              .append(" · ").append(add).append("\n");
        }
        sb.append("\n");

        sb.append("[activities]\n");
        for (ActivityResponse a : activityList) {
            appendActivityPromptLine(sb, a);
        }
        sb.append("\n");

        sb.append("[github_repos]\n");
        for (RepoEntryResponse r : repoList) {
            appendGithubRepoPromptLines(sb, r);
        }
        sb.append("\n");

        sb.append("[contact]\n");
        sb.append("- GitHub URL: ").append(githubUrl != null ? githubUrl : "").append("\n");
        sb.append("- Email: ").append(email != null ? email : "").append("\n");

        appendDesignPreferencesBlock(sb, request.getDesign_preferences());

        boolean embedLegacyCss = !hasEffectiveDesignPreferences(request.getDesign_preferences());
        return new CompiledPortfolioPromptInput(sb.toString(), embedLegacyCss, request.getDesign_preferences());
    }

    private String assembleCvHtmlPrompt(CompiledPortfolioPromptInput in,
            boolean prependStep0CoT, String portfolioPlanJsonOrNull) {
        StringBuilder out = new StringBuilder(cvPromptHead());
        if (prependStep0CoT) {
            out.append(promptLoader.load("cv/step0-cot.txt"));
        }
        out.append(in.step2Body);
        out.append("\n```\n\n");
        if (portfolioPlanJsonOrNull != null && !portfolioPlanJsonOrNull.trim().isEmpty()) {
            out.append("# PORTFOLIO_PLAN_JSON (authoritative outline — obey structure; every fact MUST exist in STEP 2)\n");
            out.append("```json\n");
            out.append(portfolioPlanJsonOrNull.trim());
            out.append("\n```\n\n");
        }
        out.append(buildCvPromptAfterStep2(in.embedLegacyCss, in.designPreferences));
        return out.toString();
    }

    private String assembleArchiveHtmlPrompt(CompiledPortfolioPromptInput in,
            boolean prependStep0CoT, String portfolioPlanJsonOrNull) {
        StringBuilder out = new StringBuilder(archivePromptHead());
        if (prependStep0CoT) {
            out.append(promptLoader.load("archive/step0-cot.txt"));
        }
        out.append(in.step2Body);
        out.append("\n```\n\n");
        if (portfolioPlanJsonOrNull != null && !portfolioPlanJsonOrNull.trim().isEmpty()) {
            out.append("# PORTFOLIO_PLAN_JSON (authoritative outline — obey structure; every fact MUST exist in STEP 2)\n");
            out.append("```json\n");
            out.append(portfolioPlanJsonOrNull.trim());
            out.append("\n```\n\n");
        }
        out.append(buildArchivePromptAfterStep2(in.embedLegacyCss, in.designPreferences));
        return out.toString();
    }

    private String repoDisplayDescription(RepoEntryResponse r) {
        if (r == null || r.getDescription() == null) {
            return "";
        }
        return r.getDescription().trim();
    }

    private String cvPromptHead() {
        return promptLoader.load("cv/head.txt");
    }

    private String archivePromptHead() {
        return promptLoader.load("archive/head.txt");
    }

    /** STEP 3–7 fragments after the STEP 2 closing fence (call {@link #assembleCvHtmlPrompt} for full document). */
    private String buildCvPromptAfterStep2(boolean embedLegacyCss, DesignPreferencesDto designPreferences) {
        String css = promptLoader.defaultBlueCss();
        StringBuilder tail = new StringBuilder();
        tail.append(promptLoader.load("cv/step3-rules.txt"));
        tail.append(promptLoader.load("cv/step4-design.txt"));
        tail.append(promptLoader.load("cv/step5-intro.txt"));
        if (embedLegacyCss) {
            tail.append(promptLoader.load("shared/step5a-legacy-cv.txt")
                    .replace("{{CSS}}", css)
                    .replace("{{HTML_TITLE}}", "[name] · Portfolio"));
        } else {
            tail.append(buildCustomDesignPromptBlock(designPreferences));
        }
        tail.append(promptLoader.load("cv/step5b-through-step7.txt"));
        return tail.toString();
    }

    private String buildArchivePromptAfterStep2(boolean embedLegacyCss, DesignPreferencesDto designPreferences) {
        String css = promptLoader.defaultBlueCss();
        StringBuilder tail = new StringBuilder();
        tail.append(promptLoader.load("archive/step3-rules.txt"));
        tail.append(promptLoader.load("archive/step4-design.txt"));
        tail.append(promptLoader.load("archive/step5-intro.txt"));
        if (embedLegacyCss) {
            tail.append(promptLoader.load("shared/step5a-legacy-archive.txt")
                    .replace("{{CSS}}", css)
                    .replace("{{HTML_TITLE}}", "[name] · Reflection Profile"));
        } else {
            tail.append(buildCustomDesignPromptBlock(designPreferences));
        }
        tail.append(promptLoader.load("archive/step5b-through-step7.txt"));
        return tail.toString();
    }

    private String buildCustomDesignPromptBlock(DesignPreferencesDto prefs) {
        String headLinks = promptLoader.load("shared/head-font-links-mandatory.txt");
        String minimum = promptLoader.load("shared/step5-css-minimum-snippet.txt");
        String layoutKey = prefs != null ? prefs.getLayout() : null;
        String colorKey = prefs != null ? prefs.getColor_theme() : null;
        String layout = resolveLayoutSnippet(layoutKey);
        String theme = resolveThemeTokenSnippet(layoutKey, colorKey);
        String density = resolveDensitySnippet(prefs != null ? prefs.getDensity() : null);
        return promptLoader.load("shared/step5a-skip-custom-design.txt")
                .replace("{{MANDATORY_HEAD_FONT_LINKS}}", headLinks)
                .replace("{{CSS_THEME_TOKENS}}", theme)
                .replace("{{CSS_MINIMUM_SNIPPET}}", minimum)
                .replace("{{CSS_LAYOUT_SNIPPET}}", layout)
                .replace("{{CSS_DENSITY_SNIPPET}}", density);
    }

    /**
     * STEP 4-B color theme tokens; falls back to layout-recommended palette when color_theme is blank.
     */
    private String resolveThemeTokenSnippet(String layout, String colorTheme) {
        if (colorTheme != null && !colorTheme.trim().isEmpty()) {
            String c = colorTheme.trim().toLowerCase();
            if (c.contains("인디고") || c.contains("indigo")) {
                return promptLoader.load("shared/theme-tokens-indigo.txt");
            }
            if (c.contains("시안") || c.contains("cyan")) {
                return promptLoader.load("shared/theme-tokens-cyan.txt");
            }
            if (c.contains("에메랄드") || c.contains("emerald")) {
                return promptLoader.load("shared/theme-tokens-emerald.txt");
            }
            if (c.contains("앰버") || c.contains("amber")) {
                return promptLoader.load("shared/theme-tokens-amber.txt");
            }
            if (c.contains("슬레이트") || c.contains("slate")) {
                return "/* slate theme — map STEP 4 slate tokens to :root */\n"
                        + ":root { --primary: #334155; --secondary: #64748b; --primary-soft: #cbd5e1; --track: #e2e8f0; }\n";
            }
            if (c.contains("로즈") || c.contains("rose")) {
                return ":root { --primary: #f43f5e; --secondary: #fb7185; --primary-soft: #ffe4e6; --track: #fecdd3; }\n";
            }
        }
        if (layout != null) {
            String l = layout.trim();
            if (l.contains("랜딩")) {
                return promptLoader.load("shared/theme-tokens-cyan.txt");
            }
            if (l.contains("사이드바")) {
                return promptLoader.load("shared/theme-tokens-emerald.txt");
            }
            if (l.contains("카드") && l.contains("그리드")) {
                return promptLoader.load("shared/theme-tokens-amber.txt");
            }
        }
        return promptLoader.load("shared/theme-tokens-indigo.txt");
    }

    /**
     * Maps Korean layout labels from the FE to a paste-ready CSS/HTML skeleton fragment.
     */
    private String resolveLayoutSnippet(String layout) {
        if (layout == null || layout.trim().isEmpty()) {
            return promptLoader.load("shared/layout-snippet-single-column.txt");
        }
        String l = layout.trim();
        if (l.contains("랜딩")) {
            return promptLoader.load("shared/layout-snippet-landing.txt");
        }
        if (l.contains("사이드바")) {
            return promptLoader.load("shared/layout-snippet-sidebar.txt");
        }
        if (l.contains("카드") && l.contains("그리드")) {
            return promptLoader.load("shared/layout-snippet-card-grid.txt");
        }
        if (l.contains("단일") || l.contains("칼럼") || l.contains("컬럼")) {
            return promptLoader.load("shared/layout-snippet-single-column.txt");
        }
        return promptLoader.load("shared/layout-snippet-single-column.txt");
    }

    private String resolveDensitySnippet(String density) {
        if (density == null || density.trim().isEmpty()) {
            return "(No 1-page density snippet — STEP 2 밀도 is not \"1페이지 내\".)";
        }
        String d = density.trim();
        if (d.contains("1페이지") || d.contains("1 페이지")) {
            return promptLoader.load("shared/density-snippet-one-page.txt");
        }
        return "(Density is not 1페이지 내 — optional spacing; do not hide .secondary-* items.)";
    }

    private static boolean hasEffectiveDesignPreferences(DesignPreferencesDto p) {
        if (p == null) {
            return false;
        }
        return nonBlank(p.getLayout()) || nonBlank(p.getColor_theme())
                || nonBlank(p.getDensity()) || nonBlank(p.getAdditional_notes());
    }

    private static boolean nonBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private void appendGithubRepoPromptLines(StringBuilder sb, RepoEntryResponse r) {
        String title = r.getCustom_title() != null && !r.getCustom_title().isEmpty()
                ? r.getCustom_title()
                : r.getGithub_title();
        if (title == null) {
            title = "Repository";
        }
        sb.append("- ").append(title).append("\n");
        String period = formatRepoPeriodForPrompt(r.getDuration());
        if (!period.isEmpty()) {
            sb.append("  · 기간: ").append(period).append("\n");
        }
        String desc = repoDisplayDescription(r);
        if (!desc.isEmpty()) {
            sb.append("  · 설명: ").append(desc).append("\n");
        }
        String langs = formatRepoLanguages(r);
        if (!langs.isEmpty()) {
            sb.append("  · 기술: ").append(langs).append("\n");
        }
        String team = formatTeamCompositionForPrompt(r.getTeam_composition());
        if (!team.isEmpty()) {
            sb.append("  · 팀 구성: ").append(team).append("\n");
        }
        String myRole = formatMyRoleForPrompt(r.getMy_role());
        if (!myRole.isEmpty()) {
            sb.append("  · 내 역할: ").append(myRole).append("\n");
        }
        if (r.getKey_contributions() != null && !r.getKey_contributions().trim().isEmpty()) {
            sb.append("  · 주요 기여:\n");
            for (String line : r.getKey_contributions().trim().split("\\r?\\n")) {
                String t = line.trim();
                if (!t.isEmpty()) {
                    sb.append("    - ").append(t).append("\n");
                }
            }
        }
        StringBuilder metaBits = new StringBuilder();
        if (r.getCommit_count() != null) {
            metaBits.append(r.getCommit_count()).append(" commits ");
        }
        if (r.getStargazers_count() != null) {
            metaBits.append(r.getStargazers_count()).append(" stars ");
        }
        if (r.getForks_count() != null) {
            metaBits.append(r.getForks_count()).append(" forks ");
        }
        String meta = metaBits.toString().trim();
        if (!meta.isEmpty()) {
            sb.append("  · ").append(meta).append("\n");
        }
        if (r.getHtml_url() != null && !r.getHtml_url().trim().isEmpty()) {
            sb.append("  · URL: ").append(r.getHtml_url().trim()).append("\n");
        }
    }

    private static String formatRepoPeriodForPrompt(DurationDto d) {
        if (d == null) {
            return "";
        }
        String start = firstNonBlank(d.getStarted_at(), d.getStarted_at_github());
        String end = firstNonBlank(d.getUpdated_at(), d.getUpdated_at_github());
        if (start.isEmpty() && end.isEmpty()) {
            return "";
        }
        if (start.isEmpty()) {
            return "~ " + end;
        }
        if (end.isEmpty()) {
            return start + " ~";
        }
        return start + " ~ " + end;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.trim().isEmpty()) {
            return a.trim();
        }
        if (b != null && !b.trim().isEmpty()) {
            return b.trim();
        }
        return "";
    }

    private static String formatTeamCompositionForPrompt(List<TeamRoleDto> team) {
        if (team == null || team.isEmpty()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (TeamRoleDto tr : team) {
            if (tr == null || tr.getRole() == null || tr.getRole().trim().isEmpty() || tr.getCount() == null) {
                continue;
            }
            parts.add(tr.getRole().trim() + "×" + tr.getCount());
        }
        return parts.isEmpty() ? "" : String.join(", ", parts);
    }

    private static String formatMyRoleForPrompt(MyRoleDto m) {
        if (m == null) {
            return "";
        }
        String role = m.getRole() != null ? m.getRole().trim() : "";
        Integer pct = m.getContribution_percent();
        if (!role.isEmpty() && pct != null) {
            return role + " (" + pct + "%)";
        }
        if (!role.isEmpty()) {
            return role;
        }
        if (pct != null) {
            return "기여도 " + pct + "%";
        }
        return "";
    }

    private String nullToEmpty(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    /** Formats one activity for LLM prompt {@code [activities]} blocks. */
    private static void appendActivityPromptLine(StringBuilder sb, ActivityResponse a) {
        String title = nullToEmptyStatic(a.getTitle());
        String start = a.getStart_date() != null ? a.getStart_date().toString() : "";
        String end = a.getEnd_date() != null ? a.getEnd_date().toString() : "";
        sb.append("- ").append(title).append(" (").append(start).append(" ~ ").append(end).append(")");
        appendActivityLabeledLine(sb, "주최", a.getHost());
        appendActivityLabeledLine(sb, "역할", a.getRole());
        appendActivityLabeledLine(sb, "성과", a.getAchievements());
        appendActivityLabeledLine(sb, "성과 설명", a.getAchievements_detail());
        String desc = nullToEmptyStatic(a.getDescription());
        if (!desc.isEmpty()) {
            appendActivityLabeledLine(sb, "설명", desc);
        }
        if (a.getUrl() != null && !a.getUrl().trim().isEmpty()) {
            appendActivityLabeledLine(sb, "URL", a.getUrl().trim());
        }
        if (a.getTags() != null && !a.getTags().isEmpty()) {
            appendActivityLabeledLine(sb, "tags", String.join(", ", a.getTags()));
        }
        sb.append("\n");
    }

    private static void appendActivityLabeledLine(StringBuilder sb, String label, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        sb.append("\n  · ").append(label).append(": ").append(value.trim());
    }

    /** Combines structured activity fields + description for server-rendered HTML timeline. */
    private static String buildActivityDisplayDescription(ActivityResponse a) {
        StringBuilder d = new StringBuilder();
        appendActivityTextLine(d, "주최", a.getHost());
        appendActivityTextLine(d, "역할", a.getRole());
        appendActivityTextLine(d, "성과", a.getAchievements());
        appendActivityTextLine(d, "성과 설명", a.getAchievements_detail());
        if (a.getDescription() != null && !a.getDescription().trim().isEmpty()) {
            if (d.length() > 0) {
                d.append("\n");
            }
            d.append(a.getDescription().trim());
        }
        return d.toString();
    }

    private static void appendActivityTextLine(StringBuilder d, String label, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        if (d.length() > 0) {
            d.append("\n");
        }
        d.append(label).append(": ").append(value.trim());
    }

    /**
     * Render the {@code [design_preferences]} block into STEP 2. Each line is omitted when its sub-field is blank.
     * The whole block (incl. header) is omitted when no sub-field is provided.
     */
    private void appendDesignPreferencesBlock(StringBuilder sb, DesignPreferencesDto prefs) {
        if (prefs == null) {
            return;
        }
        String layout = trimToNull(prefs.getLayout());
        String color = trimToNull(prefs.getColor_theme());
        String density = trimToNull(prefs.getDensity());
        String notes = prefs.getAdditional_notes() != null ? prefs.getAdditional_notes().trim() : null;
        if (notes != null && notes.isEmpty()) {
            notes = null;
        }
        if (layout == null && color == null && density == null && notes == null) {
            return;
        }
        sb.append("\n[design_preferences]\n");
        if (layout != null) {
            sb.append("- 레이아웃: ").append(layout).append("\n");
        }
        if (color != null) {
            sb.append("- 색상 테마: ").append(color).append("\n");
        }
        if (density != null) {
            sb.append("- 밀도: ").append(density).append("\n");
        }
        if (notes != null) {
            sb.append("- 추가 요청사항: ").append(notes).append("\n");
        }
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private void appendProfileLinksLines(StringBuilder sb, UserInfoResponse userInfo) {
        if (userInfo.getProfile_links() == null || userInfo.getProfile_links().isEmpty()) {
            return;
        }
        for (ProfileLinkDto p : userInfo.getProfile_links()) {
            if (p == null) {
                continue;
            }
            String url = p.getUrl() != null ? p.getUrl().trim() : "";
            if (url.isEmpty()) {
                continue;
            }
            String label = p.getLabel() != null && !p.getLabel().trim().isEmpty()
                    ? p.getLabel().trim() : url;
            sb.append("- 링크: ").append(label).append(" — ").append(url).append("\n");
        }
    }

    private List<RepoLanguageDto> getRepoLanguagesForDisplay(RepoEntryResponse r) {
        if (r.getLanguages() != null && !r.getLanguages().isEmpty()) {
            return r.getLanguages();
        }
        if (r.getLanguage() != null && !r.getLanguage().isEmpty()) {
            return Collections.singletonList(
                    RepoLanguageDto.builder().name(r.getLanguage()).percentage(null).build());
        }
        return Collections.emptyList();
    }

    private void appendTechStackPlainText(StringBuilder sb, TechStackResponse techStack) {
        sb.append("[tech_stack]\n");
        if (techStack.getDomains() != null) {
            for (TechStackDomainResponse d : techStack.getDomains()) {
                String dn = d.getName() != null ? d.getName() : "";
                if (d.getTech_stacks() != null) {
                    for (TechStackEntryResponse t : d.getTech_stacks()) {
                        String line = t.getName() != null ? t.getName() : "";
                        if (!dn.isEmpty()) line += " (" + dn + ")";
                        if (t.getLevel() != null) line += " " + t.getLevel() + "%";
                        sb.append("- ").append(line).append("\n");
                    }
                }
            }
        }
        sb.append("\n");
    }

    private String formatRepoLanguages(RepoEntryResponse r) {
        List<RepoLanguageDto> langList = getRepoLanguagesForDisplay(r);
        return langList.stream()
                .map(l -> l.getPercentage() != null
                        ? l.getName() + " (" + l.getPercentage() + "%)"
                        : l.getName())
                .collect(Collectors.joining(", "));
    }

    private String buildHtml(UserInfoResponse userInfo, TechStackResponse techStack,
            List<RepoEntryResponse> repos, List<MileageEntryResponse> mileageList,
            List<ActivityResponse> activities, String githubUrl, String email) {

        String name = escape(userInfo.getName());
        String schoolDeptEsc = escape(schoolDeptSansGrade(userInfo));
        List<String> bioParagraphs = splitBioParagraphs(userInfo.getBio());
        String[] roleSummary = deriveRoleAndSummaryChip(bioParagraphs);
        String roleLine = roleSummary[0];
        String summaryChip = roleSummary[1];
        List<String> aboutParas = buildBlueStyleAboutParagraphs(bioParagraphs);

        String metaLine = buildMetaLine(userInfo);
        String profileImgSrc = buildProfileImageSrc(userInfo);

        String title = name;
        if (roleLine != null && !roleLine.trim().isEmpty()) {
            title += " · " + escape(roleLine);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"ko\">\n<head>\n");
        sb.append("  <meta charset=\"UTF-8\" />\n");
        sb.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />\n");
        sb.append("  <title>").append(title).append("</title>\n");
        sb.append("  <link rel=\"preconnect\" href=\"https://fonts.googleapis.com\" />\n");
        sb.append("  <link rel=\"preconnect\" href=\"https://fonts.gstatic.com\" crossorigin />\n");
        sb.append("  <link href=\"https://fonts.googleapis.com/css2?family=Noto+Sans+KR:wght@400;500;600;700&family=Inter:wght@400;500;600;700&display=swap\" rel=\"stylesheet\" />\n");
        sb.append("  <style>\n");
        sb.append(promptLoader.defaultBlueCss());
        sb.append("\n  </style>\n</head>\n<body>\n");

        sb.append("  <div class=\"page\">\n");
        sb.append("    <div class=\"card\">\n");
        sb.append("      <div class=\"card-inner\">\n");

        sb.append("        <aside class=\"sidebar\">\n");
        sb.append("          <div class=\"profile\">\n");
        appendProfileImageTag(sb, profileImgSrc);
        if (name != null && !name.isEmpty()) {
            sb.append("            <div class=\"name\">").append(name).append("</div>\n");
        }
        if (roleLine != null && !roleLine.trim().isEmpty()) {
            sb.append("            <div class=\"role\">").append(escape(roleLine)).append("</div>\n");
        }
        if (schoolDeptEsc != null && !schoolDeptEsc.isEmpty()) {
            sb.append("            <div class=\"school\">").append(schoolDeptEsc).append("</div>\n");
        }
        if (metaLine != null && !metaLine.isEmpty()) {
            sb.append("            <div class=\"meta-line\">").append(escape(metaLine)).append("</div>\n");
        }
        if (summaryChip != null && !summaryChip.trim().isEmpty()) {
            sb.append("            <div class=\"summary-chip\"><span>").append(escape(summaryChip)).append("</span></div>\n");
        }

        if (hasTechStackEntries(techStack)) {
            sb.append("            <div class=\"section\" style=\"margin-bottom: 0;\">\n");
            sb.append("              <div class=\"section-header\" style=\"margin-bottom: 6px;\">\n");
            sb.append("                <div class=\"section-marker\"></div>\n");
            sb.append("                <div>\n");
            sb.append("                  <div class=\"section-title\" style=\"font-size: 13px;\">TECH STACK</div>\n");
            sb.append("                </div>\n");
            sb.append("              </div>\n");
            for (TechStackDomainResponse d : techStack.getDomains()) {
                if (d == null || d.getTech_stacks() == null) {
                    continue;
                }
                String dn = d.getName() != null ? d.getName().trim() : "";
                boolean hasAny = d.getTech_stacks().stream()
                        .anyMatch(t -> t != null && t.getName() != null && !t.getName().trim().isEmpty());
                if (!hasAny) {
                    continue;
                }
                if (!dn.isEmpty()) {
                    sb.append("              <div class=\"section-subtitle\" style=\"margin:8px 0 6px;\">")
                            .append(escape(dn))
                            .append("</div>\n");
                }
                sb.append("              <div class=\"pill-group\" style=\"margin-bottom:10px;\">\n");
                for (TechStackEntryResponse t : d.getTech_stacks()) {
                    if (t == null) {
                        continue;
                    }
                    String techName = t.getName() != null ? t.getName().trim() : "";
                    if (techName.isEmpty()) {
                        continue;
                    }
                    StringBuilder pillText = new StringBuilder(techName);
                    if (t.getLevel() != null) {
                        pillText.append(" ").append(t.getLevel()).append("%");
                    }
                    sb.append("                <div class=\"pill\">").append(escape(pillText.toString())).append("</div>\n");
                }
                sb.append("              </div>\n");
            }
            sb.append("            </div>\n");
        }

        boolean hasEmail = email != null && !email.trim().isEmpty();
        boolean hasGithub = githubUrl != null && !githubUrl.trim().isEmpty();
        boolean hasProfileLinks = userInfo.getProfile_links() != null && !userInfo.getProfile_links().isEmpty();

        if (hasEmail || hasGithub || hasProfileLinks) {
            sb.append("            <div class=\"contact-block\">\n");
            sb.append("              <div class=\"contact-label\">Contact</div>\n");
            if (hasEmail) {
                String emailEsc = escape(email.trim());
                sb.append("              <div class=\"contact-item\">\n");
                sb.append("                <span class=\"icon\">📧</span>\n");
                sb.append("                <a href=\"mailto:").append(emailEsc).append("\">").append(emailEsc).append("</a>\n");
                sb.append("              </div>\n");
            }
            if (hasGithub) {
                String gh = githubUrl.trim();
                sb.append("              <div class=\"contact-item\">\n");
                sb.append("                <span class=\"icon\">🐙</span>\n");
                sb.append("                <a href=\"").append(escape(gh)).append("\" class=\"link-chip\" target=\"_blank\" rel=\"noreferrer\">\n");
                sb.append("                  <span>GitHub</span>\n");
                sb.append("                  <span>").append(escape(toGithubDisplayText(gh))).append("</span>\n");
                sb.append("                </a>\n");
                sb.append("              </div>\n");
            }
            if (hasProfileLinks) {
                for (ProfileLinkDto p : userInfo.getProfile_links()) {
                    if (p == null) {
                        continue;
                    }
                    String rawUrl = p.getUrl() != null ? p.getUrl().trim() : "";
                    if (rawUrl.isEmpty()) {
                        continue;
                    }
                    String href = normalizeExternalHttpUrl(rawUrl);
                    if (href == null) {
                        continue;
                    }
                    String label = p.getLabel() != null && !p.getLabel().trim().isEmpty()
                            ? p.getLabel().trim()
                            : "Link";
                    sb.append("              <div class=\"contact-item\">\n");
                    sb.append("                <span class=\"icon\">🔗</span>\n");
                    sb.append("                <a href=\"").append(escape(href))
                            .append("\" class=\"link-chip\" target=\"_blank\" rel=\"noreferrer\">\n");
                    sb.append("                  <span>").append(escape(label)).append("</span>\n");
                    sb.append("                  <span>").append(escape(toUrlDisplayText(href))).append("</span>\n");
                    sb.append("                </a>\n");
                    sb.append("              </div>\n");
                }
            }
            sb.append("            </div>\n");
        }

        sb.append("          </div>\n");
        sb.append("        </aside>\n");

        sb.append("        <main class=\"main\">\n");

        if (!aboutParas.isEmpty()) {
            sb.append("          <section class=\"section\">\n");
            sb.append("            <div class=\"section-header\">\n");
            sb.append("              <div class=\"section-marker\"></div>\n");
            sb.append("              <div>\n");
            sb.append("                <div class=\"section-title\">ABOUT ME</div>\n");
            sb.append("                <div class=\"section-subtitle\">소개</div>\n");
            sb.append("              </div>\n");
            sb.append("            </div>\n");
            sb.append("            <div class=\"section-body about-text\">\n");
            for (String p : aboutParas) {
                if (p == null) {
                    continue;
                }
                String t = p.trim();
                if (t.isEmpty()) {
                    continue;
                }
                sb.append("              <p>").append(escape(t)).append("</p>\n");
            }
            sb.append("            </div>\n");
            sb.append("          </section>\n");
        }

        if (repos != null && !repos.isEmpty()) {
            sb.append("          <section class=\"section\">\n");
            sb.append("            <div class=\"section-header\">\n");
            sb.append("              <div class=\"section-marker\"></div>\n");
            sb.append("              <div>\n");
            sb.append("                <div class=\"section-title\">PROJECTS</div>\n");
            sb.append("                <div class=\"section-subtitle\">GitHub & 산학 프로젝트</div>\n");
            sb.append("              </div>\n");
            sb.append("            </div>\n");
            sb.append("            <div class=\"section-body\">\n");
            sb.append("              <div class=\"projects-grid\">\n");
            for (RepoEntryResponse r : repos) {
                if (r == null) {
                    continue;
                }
                String repoTitle = r.getCustom_title() != null && !r.getCustom_title().trim().isEmpty()
                        ? r.getCustom_title().trim()
                        : r.getGithub_title();
                if (repoTitle == null || repoTitle.trim().isEmpty()) {
                    repoTitle = "Repository";
                }
                String desc = repoDisplayDescription(r);
                String link = r.getHtml_url() != null ? r.getHtml_url().trim() : "#";
                String repoMeta = buildRepoDateRangeText(r.getCreated_at(), r.getUpdated_at());
                sb.append("                <article class=\"project-card\">\n");
                sb.append("                  <div class=\"project-header\">\n");
                sb.append("                    <div class=\"project-name\">").append(escape(repoTitle)).append("</div>\n");
                sb.append("                    <div class=\"project-meta\">").append(escape(repoMeta)).append("</div>\n");
                sb.append("                  </div>\n");
                sb.append("                  <div class=\"project-desc\">").append(escape(desc)).append("</div>\n");
                sb.append("                  <div class=\"project-footer\">\n");
                sb.append("                    <div class=\"stack-badges\">\n");
                for (RepoLanguageDto lang : getRepoLanguagesForDisplay(r)) {
                    if (lang == null || lang.getName() == null) {
                        continue;
                    }
                    String ln = lang.getName().trim();
                    if (ln.isEmpty()) {
                        continue;
                    }
                    String badgeText = ln;
                    if (lang.getPercentage() != null) {
                        badgeText = ln + " (" + lang.getPercentage().intValue() + "%)";
                    }
                    sb.append("                      <div class=\"stack-badge\">").append(escape(badgeText)).append("</div>\n");
                }
                if (r.getCommit_count() != null) {
                    sb.append("                      <div class=\"stack-badge\">").append(r.getCommit_count()).append(" commits</div>\n");
                }
                if (r.getStargazers_count() != null) {
                    sb.append("                      <div class=\"stack-badge\">★ ").append(r.getStargazers_count()).append("</div>\n");
                }
                if (r.getForks_count() != null) {
                    sb.append("                      <div class=\"stack-badge\">Forks ").append(r.getForks_count()).append("</div>\n");
                }
                sb.append("                    </div>\n");
                sb.append("                    <a href=\"").append(escape(link)).append("\" class=\"link-chip\" target=\"_blank\" rel=\"noreferrer\">\n");
                sb.append("                      <span>🔗</span>\n");
                sb.append("                      <span>GitHub 보기</span>\n");
                sb.append("                    </a>\n");
                sb.append("                  </div>\n");
                sb.append("                </article>\n");
            }
            sb.append("              </div>\n");
            sb.append("            </div>\n");
            sb.append("          </section>\n");
        }

        if (mileageList != null && !mileageList.isEmpty()) {
            sb.append("          <section class=\"section\">\n");
            sb.append("            <div class=\"section-header\">\n");
            sb.append("              <div class=\"section-marker\"></div>\n");
            sb.append("              <div>\n");
            sb.append("                <div class=\"section-title\">CURRICULAR & EXTRACURRICULAR</div>\n");
            sb.append("                <div class=\"section-subtitle\">전공 교과 · 비교과</div>\n");
            sb.append("              </div>\n");
            sb.append("            </div>\n");
            sb.append("            <div class=\"section-body\">\n");
            sb.append("              <div class=\"timeline\">\n");
            for (MileageEntryResponse m : mileageList) {
                if (m == null) {
                    continue;
                }
                String sem = m.getSemester() != null ? m.getSemester().trim() : "";
                String cat = m.getCategoryName() != null ? m.getCategoryName().trim() : "";
                String sub = m.getSubitemName() != null ? m.getSubitemName().trim() : "";
                String add = m.getAdditional_info() != null ? m.getAdditional_info().trim() : "";
                String d1 = m.getDescription1() != null ? m.getDescription1().trim() : "";
                String titleText = !sub.isEmpty() ? sub : cat;
                if (titleText == null) {
                    titleText = "";
                }
                String dateText = buildMileageDateText(sem, cat);
                String descText = buildMileageDescText(add, d1);
                if (titleText.trim().isEmpty() && dateText.trim().isEmpty() && descText.trim().isEmpty()) {
                    continue;
                }
                sb.append("                <div class=\"timeline-item\">\n");
                sb.append("                  <div class=\"timeline-dot\"></div>\n");
                if (!titleText.trim().isEmpty()) {
                    sb.append("                  <div class=\"timeline-title\">").append(escape(titleText)).append("</div>\n");
                }
                sb.append("                  <div class=\"timeline-date\">").append(escape(dateText)).append("</div>\n");
                if (!descText.trim().isEmpty()) {
                    sb.append("                  <div class=\"timeline-desc\">").append(escape(descText)).append("</div>\n");
                }
                sb.append("                </div>\n");
            }
            sb.append("              </div>\n");
            sb.append("            </div>\n");
            sb.append("          </section>\n");
        }

        List<MileageEntryResponse> awardMileages = new ArrayList<>();
        if (mileageList != null) {
            for (MileageEntryResponse m : mileageList) {
                if (m != null && mileageLooksLikeAward(m)) {
                    awardMileages.add(m);
                }
            }
        }
        if (!awardMileages.isEmpty()) {
            sb.append("          <section class=\"section\">\n");
            sb.append("            <div class=\"section-header\">\n");
            sb.append("              <div class=\"section-marker\"></div>\n");
            sb.append("              <div>\n");
            sb.append("                <div class=\"section-title\">ACHIEVEMENTS</div>\n");
            sb.append("                <div class=\"section-subtitle\">수상 · 대외 성과</div>\n");
            sb.append("              </div>\n");
            sb.append("            </div>\n");
            sb.append("            <div class=\"section-body\">\n");
            sb.append("              <div class=\"achievements-box\">\n");
            sb.append("                <div class=\"achievements-title\"><span>\uD83C\uDFC6</span> Highlights</div>\n");
            sb.append("                <ul class=\"achievements-list\">\n");
            for (MileageEntryResponse m : awardMileages) {
                String sem = m.getSemester() != null ? m.getSemester().trim() : "";
                String sub = m.getSubitemName() != null ? m.getSubitemName().trim() : "";
                String cat = m.getCategoryName() != null ? m.getCategoryName().trim() : "";
                String line = buildMileageDateText(sem, cat);
                if (!sub.isEmpty()) {
                    line = line.isEmpty() ? sub : sub + " — " + line;
                }
                String desc = buildMileageDescText(
                        m.getAdditional_info() != null ? m.getAdditional_info().trim() : "",
                        m.getDescription1() != null ? m.getDescription1().trim() : "");
                if (!desc.isEmpty()) {
                    line += ": " + desc;
                }
                if (!line.trim().isEmpty()) {
                    sb.append("                  <li>").append(escape(line.trim())).append("</li>\n");
                }
            }
            sb.append("                </ul>\n");
            sb.append("              </div>\n");
            sb.append("            </div>\n");
            sb.append("          </section>\n");
        }

        if (activities != null && !activities.isEmpty()) {
            sb.append("          <section class=\"section\">\n");
            sb.append("            <div class=\"section-header\">\n");
            sb.append("              <div class=\"section-marker\"></div>\n");
            sb.append("              <div>\n");
            sb.append("                <div class=\"section-title\">ACTIVITIES & EXPERIENCE</div>\n");
            sb.append("                <div class=\"section-subtitle\">활동 및 경험</div>\n");
            sb.append("              </div>\n");
            sb.append("            </div>\n");
            sb.append("            <div class=\"section-body\">\n");
            sb.append("              <div class=\"timeline\">\n");
            for (ActivityResponse a : activities) {
                if (a == null) {
                    continue;
                }
                String at = a.getTitle() != null ? a.getTitle().trim() : "";
                String desc = buildActivityDisplayDescription(a);
                String start = a.getStart_date() != null ? a.getStart_date().toString() : "";
                String end = a.getEnd_date() != null ? a.getEnd_date().toString() : "";
                String range = buildDateRangeText(start, end);
                if (at.isEmpty() && desc.isEmpty() && range.isEmpty()) {
                    continue;
                }
                sb.append("                <div class=\"timeline-item\">\n");
                sb.append("                  <div class=\"timeline-dot\"></div>\n");
                sb.append("                  <div class=\"timeline-title\">").append(escape(at)).append("</div>\n");
                sb.append("                  <div class=\"timeline-date\">").append(escape(range)).append("</div>\n");
                if (!desc.isEmpty()) {
                    sb.append("                  <div class=\"timeline-desc\">").append(escape(desc)).append("</div>\n");
                }
                sb.append("                </div>\n");
            }
            sb.append("              </div>\n");
            sb.append("            </div>\n");
            sb.append("          </section>\n");
        }

        sb.append("        <footer>\n");
        sb.append("          <span>이 포트폴리오는 실제 활동 및 GitHub 레포지토리 정보를 기반으로 자동 생성되었습니다.</span>\n");
        if (githubUrl != null && !githubUrl.trim().isEmpty()) {
            sb.append("          <a href=\"").append(escape(githubUrl.trim())).append("\" target=\"_blank\" rel=\"noreferrer\">GitHub</a>\n");
        }
        if (email != null && !email.trim().isEmpty()) {
            sb.append("          <a href=\"mailto:").append(escape(email.trim())).append("\">Email</a>\n");
        }
        sb.append("        </footer>\n");

        sb.append("        </main>\n");
        sb.append("      </div>\n");
        sb.append("    </div>\n");
        sb.append("  </div>\n");
        sb.append("</body>\n</html>");

        return sb.toString();
    }

    private boolean hasTechStackEntries(TechStackResponse techStack) {
        if (techStack == null || techStack.getDomains() == null) {
            return false;
        }
        for (TechStackDomainResponse d : techStack.getDomains()) {
            if (d != null && d.getTech_stacks() != null && !d.getTech_stacks().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void appendProfileImageTag(StringBuilder sb, String profileImgSrc) {
        if (profileImgSrc == null || profileImgSrc.trim().isEmpty()) {
            return;
        }
        if (profileImgSrc.startsWith("data:")) {
            sb.append("            <img src=\"").append(profileImgSrc).append("\" alt=\"Profile\" class=\"profile-img\" />\n");
        } else {
            sb.append("            <img src=\"").append(escape(profileImgSrc)).append("\" alt=\"Profile\" class=\"profile-img\" />\n");
        }
    }

    /** blueStyle: `.role` and `.summary-chip` from [bio] blocks (first / second segment or first line / rest). */
    private static String[] deriveRoleAndSummaryChip(List<String> paras) {
        if (paras == null || paras.isEmpty()) {
            return new String[] { "", "" };
        }
        if (paras.size() >= 2) {
            return new String[] { paras.get(0).trim(), paras.get(1).trim() };
        }
        String one = paras.get(0);
        int nl = one.indexOf('\n');
        if (nl > 0) {
            return new String[] { one.substring(0, nl).trim(), one.substring(nl + 1).trim() };
        }
        String t = one.trim();
        return new String[] { t, t };
    }

    /** ABOUT ME paragraphs: from second segment onward (duplicates summary line when3+ segments), blueStyle. */
    private static List<String> buildBlueStyleAboutParagraphs(List<String> paras) {
        List<String> about = new ArrayList<>();
        if (paras == null || paras.isEmpty()) {
            return about;
        }
        if (paras.size() >= 3) {
            for (int i = 1; i < paras.size(); i++) {
                about.add(paras.get(i).trim());
            }
        } else if (paras.size() == 2) {
            about.add(paras.get(1).trim());
        } else {
            String one = paras.get(0);
            int nl = one.indexOf('\n');
            if (nl > 0) {
                String after = one.substring(nl + 1).trim();
                for (String line : after.split("\\r?\\n")) {
                    String t = line.trim();
                    if (!t.isEmpty()) {
                        about.add(t);
                    }
                }
                if (about.isEmpty() && !after.isEmpty()) {
                    about.add(after);
                }
            } else {
                about.add(one.trim());
            }
        }
        return about;
    }

    private List<String> splitBioParagraphs(String bio) {
        if (bio == null) {
            return Collections.emptyList();
        }
        String t = bio.trim();
        if (t.isEmpty()) {
            return Collections.emptyList();
        }
        String[] parts = t.split("\\r?\\n+");
        List<String> out = new ArrayList<String>();
        for (String p : parts) {
            if (p == null) {
                continue;
            }
            String s = p.trim();
            if (!s.isEmpty()) {
                out.add(s);
            }
        }
        return out;
    }

    private String buildMetaLine(UserInfoResponse u) {
        if (u == null) {
            return "";
        }
        String g = u.getGrade() != null ? String.valueOf(u.getGrade()) : "";
        String s = u.getSemester() != null ? String.valueOf(u.getSemester()) : "";
        if (g.isEmpty() && s.isEmpty()) {
            return "";
        }
        if (!g.isEmpty() && !s.isEmpty()) {
            return g + "학년 " + s + "학기";
        }
        if (!g.isEmpty()) {
            return g + "학년";
        }
        return s + "학기";
    }

    private String buildDateRangeText(String start, String end) {
        String st = start != null ? start.trim() : "";
        String en = end != null ? end.trim() : "";
        if (st.isEmpty() && en.isEmpty()) {
            return "";
        }
        if (!st.isEmpty() && !en.isEmpty()) {
            return st + " ~ " + en;
        }
        return !st.isEmpty() ? st : en;
    }

    private String buildMileageDateText(String sem, String cat) {
        String s = sem != null ? sem.trim() : "";
        String c = cat != null ? cat.trim() : "";
        if (s.isEmpty() && c.isEmpty()) {
            return "";
        }
        if (!s.isEmpty() && !c.isEmpty()) {
            return s + " · " + c;
        }
        return !s.isEmpty() ? s : c;
    }

    private String buildMileageDescText(String add, String d1) {
        String a = add != null ? add.trim() : "";
        String d = d1 != null ? d1.trim() : "";
        if (a.isEmpty() && d.isEmpty()) {
            return "";
        }
        if (!a.isEmpty() && !d.isEmpty() && !a.equals(d)) {
            return a + " · " + d;
        }
        return !a.isEmpty() ? a : d;
    }

    private String buildRepoDateRangeText(String createdAt, String updatedAt) {
        String c = isoDatePrefixOrEmpty(createdAt);
        String u = isoDatePrefixOrEmpty(updatedAt);
        if (c.isEmpty() && u.isEmpty()) {
            return "GitHub 레포지토리";
        }
        if (!c.isEmpty() && u.isEmpty()) {
            return c;
        }
        if (c.isEmpty() && !u.isEmpty()) {
            return u;
        }
        if (c.equals(u)) {
            return c;
        }
        return c + " → " + u;
    }

    private String isoDatePrefixOrEmpty(String s) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return "";
        }
        return t.length() >= 10 ? t.substring(0, 10) : t;
    }

    private String toGithubDisplayText(String githubUrl) {
        if (githubUrl == null) {
            return "";
        }
        String trimmed = githubUrl.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        try {
            URL u = new URL(trimmed);
            String host = u.getHost() != null ? u.getHost() : "";
            String path = u.getPath() != null ? u.getPath() : "";
            if (!host.isEmpty() && !path.isEmpty()) {
                if ("www.github.com".equalsIgnoreCase(host)) {
                    host = "github.com";
                }
                return host + path;
            }
        } catch (MalformedURLException ignored) {
            /* fall through */
        }
        return trimmed;
    }

    private String toUrlDisplayText(String url) {
        if (url == null) {
            return "";
        }
        String trimmed = url.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        try {
            URL u = new URL(trimmed);
            String host = u.getHost() != null ? u.getHost() : "";
            String path = u.getPath() != null ? u.getPath() : "";
            if (!host.isEmpty()) {
                if (path == null || path.isEmpty() || "/".equals(path)) {
                    return host;
                }
                return host + path;
            }
        } catch (MalformedURLException ignored) {
            /* fall through */
        }
        return trimmed;
    }

    /**
     * Returns a safe http(s) URL for use in {@code href}, or null if not usable.
     * Accepts URLs without scheme and normalizes them to {@code https://...}.
     */
    private String normalizeExternalHttpUrl(String rawUrl) {
        if (rawUrl == null) {
            return null;
        }
        String s = rawUrl.trim();
        if (s.isEmpty()) {
            return null;
        }
        if (s.startsWith("//")) {
            s = "https:" + s;
        }
        if (!s.startsWith("http://") && !s.startsWith("https://")) {
            s = "https://" + s;
        }
        try {
            URL u = new URL(s);
            String protocol = u.getProtocol();
            if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
                return null;
            }
            if (u.getHost() == null || u.getHost().isEmpty()) {
                return null;
            }
            return u.toString();
        } catch (MalformedURLException ignored) {
            return null;
        }
    }

    private String schoolDept(UserInfoResponse u) {
        StringBuilder s = new StringBuilder();
        if (u.getDepartment() != null) s.append(u.getDepartment()).append(" ");
        if (u.getMajor1() != null) s.append(u.getMajor1());
        if (u.getMajor2() != null && !u.getMajor2().isEmpty()) s.append(" ").append(u.getMajor2());
        if (u.getGrade() != null || u.getSemester() != null) {
            s.append(" (").append(u.getGrade() != null ? u.getGrade() : "?").append("학년 ");
            s.append(u.getSemester() != null ? u.getSemester() : "?").append("학기)");
        }
        return s.toString().trim();
    }

    /** School / major line only (blueStyle sidebar `.school` — grade·semester go in `.meta-line`). */
    private String schoolDeptSansGrade(UserInfoResponse u) {
        StringBuilder s = new StringBuilder();
        if (u.getDepartment() != null) {
            s.append(u.getDepartment()).append(" ");
        }
        if (u.getMajor1() != null) {
            s.append(u.getMajor1());
        }
        if (u.getMajor2() != null && !u.getMajor2().isEmpty()) {
            s.append(" ").append(u.getMajor2());
        }
        return s.toString().trim();
    }

    private static boolean mileageLooksLikeAward(MileageEntryResponse m) {
        if (m == null) {
            return false;
        }
        String blob = String.join(" ",
                nullToEmptyStatic(m.getSubitemName()),
                nullToEmptyStatic(m.getCategoryName()),
                nullToEmptyStatic(m.getAdditional_info()),
                nullToEmptyStatic(m.getDescription1()));
        return blob.contains("\uC218\uC0C1")
                || blob.contains("\uC6B0\uC218")
                || blob.contains("\uAE08\uC0C1")
                || blob.contains("\uC740\uC0C1")
                || blob.contains("\uB3D9\uC0C1")
                || blob.contains("\uACBD\uC9C4")
                || blob.toLowerCase().contains("award");
    }

    private static String nullToEmptyStatic(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    /** Inline base64 when file exists; otherwise context-path-relative image URL (no hostname). */
    private String buildProfileImageSrc(UserInfoResponse userInfo) {
        return buildProfileImageSrcFromFilename(userInfo.getProfile_image_url());
    }

    private String buildProfileImageSrcFromFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return null;
        }
        try {
            Path filePath = Paths.get(profileUploadDir).resolve(filename).normalize();
            if (Files.exists(filePath) && Files.isReadable(filePath)) {
                byte[] bytes = Files.readAllBytes(filePath);
                String mime = Files.probeContentType(filePath);
                if (mime == null) {
                    if (filename.toLowerCase().endsWith(".png")) mime = "image/png";
                    else if (filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg")) mime = "image/jpeg";
                    else if (filename.toLowerCase().endsWith(".gif")) mime = "image/gif";
                    else if (filename.toLowerCase().endsWith(".webp")) mime = "image/webp";
                    else mime = "image/png";
                }
                String b64 = Base64.getEncoder().encodeToString(bytes);
                return "data:" + mime + ";base64," + b64;
            }
        } catch (IOException ignored) {
            /* fall through to relative URL */
        }
        String rel = buildProfileImageUploadRelativeUrl(filename);
        return escape(rel);
    }

    /**
     * {@code /milestone25/api/portfolio/user-info/image/...} when context-path is {@code /milestone25};
     * {@code /api/...} when context-path is empty (local default).
     */
    private String buildProfileImageUploadRelativeUrl(String filename) {
        String prefix = servletContextPath != null ? servletContextPath.trim() : "";
        if (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        return prefix + "/api/portfolio/user-info/image/"
                + UriUtils.encodePathSegment(filename, StandardCharsets.UTF_8);
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

}
