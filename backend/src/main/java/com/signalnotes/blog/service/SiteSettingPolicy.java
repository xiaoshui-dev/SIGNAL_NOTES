package com.signalnotes.blog.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Central policy for settings that may be edited or returned by the site APIs. */
public final class SiteSettingPolicy {
    // Keeps the full admin form bounded while allowing the explicitly controlled settings contract to grow.
    public static final int MAX_KEYS = 200;
    public static final int MAX_VALUE_LENGTH = 20_000;
    public static final int MAX_TOTAL_LENGTH = 300_000;
    public static final Set<String> ADMIN_ONLY_KEYS = Set.of();
    public static final Set<String> PUBLIC_KEYS = Set.of(
        "siteName", "siteShortName", "siteTagline", "heroEyebrow", "heroTitle", "heroSummary", "blogTitle", "blogIntro",
        "landingTopicsTitle", "landingTopicsIntro", "landingTopics", "featuredTitle", "landingStatusTopics", "landingRecentLabel",
        "landingTopicsLabel", "landingExploreLabel", "landingAboutLink", "searchTitle", "searchIntro", "categoriesTitle", "categoriesIntro",
        "tagsTitle", "tagsIntro", "categoryRouteIntro", "tagRouteIntro", "archiveTitle", "archiveIntro", "aboutTitle", "aboutLead", "aboutBody",
        "aboutPrinciple1Title", "aboutPrinciple1Body", "aboutPrinciple2Title", "aboutPrinciple2Body", "aboutPrinciple3Title", "aboutPrinciple3Body",
        "contactTitle", "contactIntro", "publicEmail", "replyPromise", "contactFormMessagePlaceholder", "contactConsentLabel", "contactLicenseHeading",
        "contactLicenseNote", "privacyTitle", "privacyUpdatedAt", "privacyIntroHeading", "privacyContent", "privacyCommentsHeading", "privacyCommentsPolicy",
        "privacyRightsHeading", "privacyRights", "termsHeading", "termsContent", "noResultsTitle", "noResultsDescription", "searchSuggestionsTitle",
        "noPublicPosts", "noPublicPostsDescription", "noPublicTags", "noTaggedPostsTitle", "noTaggedPostsDescription", "notFoundTitle", "notFoundDescription",
        "status403Label", "status403Title", "status403Description", "status404Label", "status404Title", "status404Description", "status500Label",
        "status500Title", "status500Description", "status503Label", "status503Title", "status503Description", "statusDefaultLabel", "statusDefaultTitle",
        "statusDefaultDescription", "authorName", "authorRole", "authorBio", "footerDescription", "copyrightText", "licenseText", "subscribeTitle",
        "subscribeDescription", "shareTemplate", "landingLoadingLabel", "shareArticleLabel", "sharePosterTitle", "sharePosterLoadingLabel", "shareCopyLinkLabel", "shareDownloadLabel", "shareSystemLabel", "shareCopiedLabel", "shareSavedLabel", "shareScanLabel", "shareQrDescription", "shareLandscapeLabel", "sharePortraitLabel", "landingNavPosts", "landingNavTopics", "landingNavAbout", "heroEnterBlog", "heroViewFeatured",
        "landingSelectedLabel", "landingTopicsSectionLabel", "landingAboutSectionLabel", "landingAllPostsLabel", "landingFooterEnterLabel", "landingFooterAboutLabel", "landingFooterContactLabel",
        "landingFooterPrivacyLabel", "blogFilterAllLabel", "blogReadMoreLabel", "blogNavPostsLabel", "blogNavCategoriesLabel", "blogNavTagsLabel", "blogNavArchivesLabel", "blogNavAboutLabel", "blogNavSearchPlaceholder", "blogNavHomeLabel", "blogNavRssLabel", "noConnectionLabel", "noConnectionTitle", "noConnectionDescription",
        "reconnectLabel", "reconnectingLabel", "listEndLabel", "listEndDescription", "subscribeEyebrow", "subscribeEmailPlaceholder",
        "subscribeButtonLabel", "searchInputPlaceholder", "searchButtonLabel", "searchResultSummary", "searchPaginationPrevious", "searchPaginationNext",
        "searchResultLink", "categoriesSectionLabel", "categoryPostCountLabel", "tagPostCountLabel", "authorSectionLabel", "authorPostCountLabel",
        "contactNameLabel", "contactEmailLabel", "contactSubjectLabel", "contactMessageLabel", "contactSubmitLabel", "noNotesLabel",
        "articleNotFoundTitle", "articleNotFoundBackLabel", "articleRelatedTitle", "articlePreviousLabel", "articleNextLabel",
        "commentsSectionLabel", "commentsTitle", "commentsNameLabel", "commentsNamePlaceholder", "commentsBodyLabel", "commentsPlaceholder",
        "commentsReplyPlaceholder", "commentsSubmitLabel", "commentsReplySubmitLabel", "commentsCancelReplyLabel", "commentsReplyActionLabel",
        "commentsReportActionLabel", "commentsReportedLabel", "commentsEmptyLabel", "commentsReplySuffix", "commentsValidationError",
        "commentsSubmittedMessage", "commentsSubmitError", "commentsReportError", "articleCopyLinkLabel", "articleCopySuccessLabel", "articleCopyFailureLabel",
        "privacyUpdatedPrefix", "notFoundBackLabel", "statusRetryLabel", "statusBackLabel", "statusSearchLabel"
    );
    public static final Set<String> MAIL_KEYS = Set.of("mail.enabled", "mail.host", "mail.port", "mail.username", "mail.password", "mail.from", "mail.notificationTo", "mail.starttls", "mail.auth");

    private SiteSettingPolicy() {}
    public static boolean isAllowed(String key) { return PUBLIC_KEYS.contains(key) || MAIL_KEYS.contains(key) || ADMIN_ONLY_KEYS.contains(key); }

    public static Map<String, String> validateAndNormalize(Map<String, String> input) {
        if (input == null) throw new IllegalArgumentException("设置请求不能为空");
        if (input.size() > MAX_KEYS) throw new IllegalArgumentException("设置项数量超过限制");
        Map<String, String> normalized = new LinkedHashMap<>();
        int totalLength = 0;
        for (Map.Entry<String, String> entry : input.entrySet()) {
            String value = entry.getValue() == null ? "" : entry.getValue();
            if (value.length() > MAX_VALUE_LENGTH) throw new IllegalArgumentException("设置值长度超过限制: " + entry.getKey());
            totalLength += value.length();
            if (totalLength > MAX_TOTAL_LENGTH) throw new IllegalArgumentException("设置内容总长度超过限制");
            if ("mail.passwordConfigured".equals(entry.getKey())) continue;
            if (!isAllowed(entry.getKey())) throw new IllegalArgumentException("不支持的设置项: " + entry.getKey());
            normalized.put(entry.getKey(), value);
        }
        return normalized;
    }
}
