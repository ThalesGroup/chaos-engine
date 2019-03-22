package com.gemalto.chaos.notification;

import com.gemalto.chaos.exception.ChaosException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.backoff.BackOffExecution;
import org.springframework.util.backoff.ExponentialBackOff;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.gemalto.chaos.constants.MathUtils.PHI;

public abstract class BufferedNotificationMethod implements NotificationMethods {
    private static final Integer FORCED_FLUSH_SIZE = 50;
    protected final Logger log = LoggerFactory.getLogger(getClass());
    private ConcurrentLinkedQueue<ChaosEvent> notificationBuffer = new ConcurrentLinkedQueue<>();
    private ExponentialBackOff exponentialBackOff = new ExponentialBackOff();
    private BackOffExecution backOffExecution;

    protected BufferedNotificationMethod () {
        exponentialBackOff.setMultiplier(PHI);
        exponentialBackOff.setInitialInterval(1000);
        exponentialBackOff.setMaxElapsedTime(30000);
    }

    @Override
    public void logEvent (ChaosEvent event) {
        getQueue().add(event);
        if (getQueue().size() > FORCED_FLUSH_SIZE) {
            flushBuffer();
        }
    }

    private Queue<ChaosEvent> getQueue () {
        return notificationBuffer;
    }

    protected void flushBuffer () {
        while (!getQueue().isEmpty()) {
            backOffExecution = exponentialBackOff.start();
            ChaosEvent chaosEvent = getQueue().poll();
            // Make sure to Null Check chaosEvent, in case two instances of Flush Buffer are running simultaneously
            if (chaosEvent == null) continue;
            try {
                flushBufferInternal(chaosEvent);
            } catch (Exception e) {
                throw new ChaosException(e);
            }
        }
    }

    private synchronized void flushBufferInternal (ChaosEvent chaosEvent) throws InterruptedException {
        long waitTime;
        while (true) {
            try {
                sendNotification(chaosEvent);
                break;
            } catch (IOException e) {
                log.error("Caught IO Exception when sending notification.");
                waitTime = backOffExecution.nextBackOff();
                if (waitTime == BackOffExecution.STOP) {
                    throw new ChaosException("Repeatedly failed to send notification", e);
                }
            }
            try {
                wait(waitTime);
            } catch (InterruptedException e1) {
                log.error("Interrupted while sleeping", e1);
                throw e1;
            }
        }
    }

    protected abstract void sendNotification (ChaosEvent chaosEvent) throws IOException;
}
