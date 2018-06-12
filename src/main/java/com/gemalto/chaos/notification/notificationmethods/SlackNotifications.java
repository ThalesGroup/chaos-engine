package com.gemalto.chaos.notification.notificationmethods;

import com.gemalto.chaos.notification.ChaosEvent;
import com.gemalto.chaos.notification.NotificationMethods;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty({"slack.webhookuri"})
public class SlackNotifications implements NotificationMethods {

    @Value("${slack.webhookuri}")
    private static String webhookUri;

    @Override
    public void logEvent(ChaosEvent event) {
        // TODO: Implement Slack logging.

    }
}
