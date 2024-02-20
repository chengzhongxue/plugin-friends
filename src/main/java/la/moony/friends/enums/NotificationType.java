//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package la.moony.friends.enums;

import la.moony.friends.extension.Friend;

public enum NotificationType {
    AUDIT("朋友圈，有新的博客申请提交，请审核！"),
    AUDITED("朋友圈，您的博客申请已通过，请查看！"),
    REJECTED("朋友圈，您的博客申请被驳回，请查看！");

    private String subject;
    private String html;

    private NotificationType(String s) {
        this.subject = s;
    }

    public String getSubject() {
        return this.subject;
    }

    public String getHtml(Friend friend, String jumpUrl) {
        switch (this) {
            case AUDIT:
                return this.getAuditHtml(friend, jumpUrl);
            case AUDITED:
                return this.getAuditedHtml(friend, jumpUrl);
            case REJECTED:
                return this.getRejectedHtml(friend, jumpUrl);
            default:
                return null;
        }
    }

    public String getAuditHtml(Friend friend, String jumpUrl) {
        String title = this.getSubject();
        String displayName = friend.getSpec().getDisplayName();
        String rss = friend.getSpec().getRssUrl();
        String description = friend.getSpec().getDescription();
        String email = friend.getSpec().getAdminEmail();
        String html = "    <div class=\"container\" style=\"background-color: #ffffff; max-width: 600px; margin: 20px auto; padding: 20px; border-radius: 8px; box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);\">\n        <h2 style=\"color: #333333;\">${title}</h2>\n        <h3 style=\"color: #333333;\">博客信息：</h3>\n        <ul style=\"list-style-type: none; padding: 0;\">\n            <li style=\"margin-bottom: 10px;\"><strong>博客名称：</strong>${displayName}</li>\n            <li style=\"margin-bottom: 10px;\"><strong>RSS地址：</strong><a href=\"${rss}\" target=\"_blank\" style=\"color: #007BFF; text-decoration: underline;\">${rss}</a></li>\n            <li style=\"margin-bottom: 10px;\"><strong>描述：</strong>${description}</li>\n            <li style=\"margin-bottom: 10px;\"><strong>Email：</strong><a href=\"mailto:${email}\" style=\"color: #007BFF; text-decoration: underline;\">${email}</a></li>\n        </ul>\n        <p style=\"line-height: 1.6;\"><a href=\"${jumpUrl}\" target=\"_blank\" style=\"color: #007BFF; text-decoration: underline;\">前往审核</a></p>\n    </div>\n";
        html = html.replace("${title}", title);
        html = html.replace("${displayName}", displayName);
        html = html.replace("${rss}", rss);
        html = html.replace("${email}", email);
        html = html.replace("${description}", description);
        html = html.replace("${jumpUrl}", "https://" + jumpUrl + "/console/friends?tab=friend");
        return html;
    }

    public String getAuditedHtml(Friend friend, String jumpUrl) {
        String title = this.getSubject();
        String displayName = friend.getSpec().getDisplayName();
        String html = "    <div class=\"container\" style=\"background-color: #ffffff; max-width: 600px; margin: 20px auto; padding: 20px; border-radius: 8px; box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);\">\n        <h2 style=\"color: #333333;\">${title}</h2>\n        <p style=\"color: #666666;\">亲爱的 ${displayName}，</p>\n        <p style=\"color: #333333;\">您的博客已通过审核。</p>\n        <p style=\"line-height: 1.6;\"><a href=\"${jumpUrl}\" target=\"_blank\" style=\"color: #007BFF; text-decoration: underline;\">前往查看</a></p>\n    </div>\n";
        html = html.replace("${title}", title);
        html = html.replace("${displayName}", displayName);
        html = html.replace("${jumpUrl}", "https://" + jumpUrl + "/blogs?sort=collect_time");
        return html;
    }

    public String getRejectedHtml(Friend friend, String jumpUrl) {
        String title = this.getSubject();
        String displayName = friend.getSpec().getDisplayName();
        String reason = friend.getSpec().getReason();
        String html =
            "    <div class=\"container\" style=\"background-color: #ffffff; max-width: 600px; margin: 20px auto; padding: 20px; border-radius: 8px; box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);\">\n        <h2 style=\"color: #333333;\">${title}</h2>\n        <p style=\"color: #666666;\">亲爱的 ${displayName}，</p>\n        <p style=\"color: #333333;\">您的博客被驳回。</p>\n        <p style=\"color: #333333;\">说明：${description}。</p>\n        <p style=\"line-height: 1.6;\"><a href=\"${jumpUrl}\" target=\"_blank\" style=\"color: #007BFF; text-decoration: underline;\">前往查看</a></p>\n    </div>\n";
        html = html.replace("${title}", title);
        html = html.replace("${displayName}", displayName);
        html = html.replace("${description}", reason);
        html = html.replace("${jumpUrl}", "https://" + jumpUrl + "/blog-requests");
        return html;
    }
}
