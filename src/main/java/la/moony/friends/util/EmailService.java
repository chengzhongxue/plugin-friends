package la.moony.friends.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Properties;
import la.moony.friends.enums.NotificationType;
import la.moony.friends.extension.Friend;
import la.moony.friends.vo.FriendsConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ConfigMap;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.Secret;
import run.halo.app.infra.utils.JsonUtils;

@Component
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private JavaMailSenderImpl mailSender;
    private final ReactiveExtensionClient client;

    @Async
    public Mono<Void> sendMail(NotificationType notificationType, Friend friend) {

        return this.client.fetch(ConfigMap.class, "plugin-friends-configMap").filter((configMap) -> {
            return !configMap.getData().isEmpty() && configMap.getData().containsKey("base");
        }).flatMap((cm) -> {
            FriendsConfig config = this.getConfig(cm);
            if (config!=null) {
                String adminEmail = config.getEmail().getAdminEmail();
                boolean sendEmail = config.getEmail().isSendEmail();
                if (sendEmail) {
                    return this.sendEmail(adminEmail, notificationType.getSubject(), notificationType.getHtml(friend, config.getEmail().getDomain()));
                }
            }
            return Mono.empty();
        });
    }

    @Async
    public Mono<Void> sendMail(String email, NotificationType notificationType, Friend friend) {

        return this.client.fetch(ConfigMap.class, "plugin-friends-configMap").filter((configMap) -> {
            return !configMap.getData().isEmpty() && configMap.getData().containsKey("base");
        }).flatMap((cm) -> {
            FriendsConfig config = this.getConfig(cm);
            if (config!=null) {
                boolean sendEmail = config.getEmail().isSendEmail();
                if (sendEmail && StringUtils.isNotEmpty(email)) {
                    return this.sendEmail(email, notificationType.getSubject(), notificationType.getHtml(friend, config.getEmail().getDomain()));
                }
            }
            return Mono.empty();
        });
    }


    public Mono<Void> sendEmail(String to, String subject, String content) {
        return this.fetchConfig().flatMap((configNode) -> {
            JsonNode senderNode = configNode.get("sender");
            boolean enable = senderNode.get("enable").asBoolean();
            if (enable){
                String host = senderNode.get("host").asText();
                int port = senderNode.get("port").asInt();
                String username = senderNode.get("username").asText();
                String password = senderNode.get("password").asText();
                String formName = senderNode.get("displayName").asText();
                String encryption = senderNode.get("encryption").asText();
                String starttlsEnable = encryption.equals("TLS") ? "true" : "false";
                String sslEnable = encryption.equals("SSL") ? "true" : "false";
                this.mailSender = new JavaMailSenderImpl();
                this.mailSender.setHost(host);
                this.mailSender.setPort(port);
                this.mailSender.setUsername(username);
                this.mailSender.setPassword(password);
                this.mailSender.setProtocol("smtp");
                this.mailSender.setDefaultEncoding("UTF-8");
                Properties properties = new Properties();
                properties.setProperty("mail.form.name", formName);
                properties.setProperty("mail.smtp.auth", "true");
                properties.setProperty("mail.smtp.starttls.enable", starttlsEnable);
                properties.setProperty("mail.smtp.ssl.enable", sslEnable);
                this.mailSender.setJavaMailProperties(properties);
                MimeMessage mimeMessage = this.mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);

                try {
                    helper.setFrom(username, formName);
                    helper.setTo(to);
                    helper.setSubject(subject);
                    helper.setText(content, true);
                    helper.setSentDate(new Date());
                    this.mailSender.send(mimeMessage);
                    log.info("邮件发送成功！");
                } catch (Exception var18) {
                    var18.printStackTrace();
                    log.error("邮件发送失败！ {}", var18);
                }
            }
            return Mono.empty();
        });
    }

    public Mono<ObjectNode> fetchConfig() {
        return this.client.fetch(Secret.class, "notifier-setting-secret").mapNotNull(Secret::getStringData).mapNotNull((map) -> {
            return (String)map.get("default-email-notifier.json");
        }).filter(StringUtils::isNotBlank).map((value) -> {
            return (ObjectNode)JsonUtils.jsonToObject(value, ObjectNode.class);
        }).defaultIfEmpty(JsonNodeFactory.instance.objectNode());
    }

    public EmailService(ReactiveExtensionClient client) {
        this.client = client;
    }

    private FriendsConfig getConfig(ConfigMap cm) {
        String setRef = (String)cm.getData().get("base");
        return (FriendsConfig) JsonUtils.jsonToObject(setRef, FriendsConfig.class);
    }
}
