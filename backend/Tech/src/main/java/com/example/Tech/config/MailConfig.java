package com.example.Tech.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
@EnableConfigurationProperties(MailProperties.class)
public class MailConfig {

    /** Runs the mails: a small pool next to the request threads, or the calling thread when app.mail.async=false (tests). */
    @Bean(name = "mailExecutor")
    TaskExecutor mailExecutor(MailProperties properties) {
        if (!properties.async()) {
            return new SyncTaskExecutor();
        }
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("mail-");
        executor.initialize();
        return executor;
    }

    @Bean
    ApplicationRunner mailCheck(MailProperties properties, @Value("${spring.mail.username:}") String username) {
        return args -> {
            if (!properties.enabled()) {
                log.warn("MAIL_ENABLED=false: order emails are switched off");
            } else if (!StringUtils.hasText(username)) {
                log.warn("MAIL_USERNAME is not set: order emails are not sent (they are logged as SKIPPED in email_log). "
                        + "Set MAIL_USERNAME / MAIL_PASSWORD (a Gmail App Password) in .env");
            }
        };
    }
}
