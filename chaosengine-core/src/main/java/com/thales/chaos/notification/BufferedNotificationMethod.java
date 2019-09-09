package com.thales.chaos.notification;

import com.thales.chaos.exception.ChaosException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.backoff.BackOffExecution;
import org.springframework.util.backoff.ExponentialBackOff;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.thales.chaos.constants.MathUtils.PHI;
import static com.thales.chaos.exception.enums.ChaosErrorCode.NOTIFICATION_BUFFER_ERROR;
import static com.thales.chaos.exception.enums.ChaosErrorCode.NOTIFICATION_BUFFER_RETRY_EXCEEDED;

public abstract class BufferedNotificationMethod implements NotificationMethods {
    private static final Integer FORCED_FLUSH_SIZE = 50;
    protected final Logger log = LoggerFactory.getLogger(getClass());
    private ConcurrentLinkedQueue<ChaosNotification> notificationBuffer = new ConcurrentLinkedQueue<>();
    private ExponentialBackOff exponentialBackOff = new ExponentialBackOff();
    private BackOffExecution backOffExecution;

    protected BufferedNotificationMethod () {
        exponentialBackOff.setMultiplier(PHI);
        exponentialBackOff.setInitialInterval(1000);
        exponentialBackOff.setMaxElapsedTime(30000);
    }

    @Override
    public void logNotification (ChaosNotification notification) {
        getQueue().add(notification);
        if (getQueue().size() > FORCED_FLUSH_SIZE) {
            flushBuffer();
        }
    }

    private Queue<ChaosNotification> getQueue () {
        return notificationBuffer;
    }

    protected void flushBuffer () {
        while (!getQueue().isEmpty()) {
            backOffExecution = exponentialBackOff.start();
            ChaosNotification chaosNotification = getQueue().poll();
            // Make sure to Null Check chaosExperimentEvent, in case two instances of Flush Buffer are running simultaneously
            if (chaosNotification == null) continue;
            try {
                flushBufferInternal(chaosNotification);
            } catch (Exception e) {
                throw new ChaosException(NOTIFICATION_BUFFER_ERROR, e);
            }
        }
    }

    private synchronized void flushBufferInternal (ChaosNotification chaosNotification) throws InterruptedException {
        long waitTime;
        while (true) {
            try {
                sendNotification(chaosNotification);
                break;
            } catch (IOException e) {
                log.error("Caught IO Exception when sending notification.");
                waitTime = backOffExecution.nextBackOff();
                if (waitTime == BackOffExecution.STOP) {
                    throw new ChaosException(NOTIFICATION_BUFFER_RETRY_EXCEEDED, e);
                }
            }
            wait(waitTime);
        }
    }

    protected abstract void sendNotification (ChaosNotification chaosNotification) throws IOException;
}
