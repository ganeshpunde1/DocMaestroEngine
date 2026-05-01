package com.maestro.po.ms.inference.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Rate-limits AWS Textract API calls to stay within service quotas.
 * Maintains separate per-second counters for StartDocumentTextDetection
 * and GetDocumentTextDetection calls, sleeping when limits are reached.
 *
 * @author Ganesh Punde
 */
@Slf4j
@Component
public class TextractThrottleService
{
    private static final long ONE_SECOND = 1000;

    @Value(value = "${maestro.inference.config.textract.throttle.startDocTextLimit:5}")
    private Integer startDocTextThrottleLimit;

    @Value(value = "${maestro.inference.config.textract.throttle.getDocTextLimit:5}")
    private Integer getDocTextThrottleLimit;

    @Value(value = "${maestro.inference.config.textract.throttle.sleepInterval:20000}")
    private Integer sleepInterval;

    private static volatile Object startDocTextDetectLock;

    private static volatile long lastStartDocTextDetectTime;

    private static volatile int startDocTextDetectAPICallCount;

    private static volatile Object getDocTextDetectLock;

    private static volatile long lastGetDocTextDectectTime;

    private static volatile int getGetDocTextDectectAPICallCount;

    @PostConstruct
    private void initialize()
    {
        startDocTextDetectLock = new Object();
        lastStartDocTextDetectTime = System.currentTimeMillis();
        startDocTextDetectAPICallCount = 0;

        getDocTextDetectLock = new Object();
        lastGetDocTextDectectTime = System.currentTimeMillis();
        getGetDocTextDectectAPICallCount = 0;
    }

    /**
     * Throttles calls to the Textract StartDocumentTextDetection API
     * to stay within the configured per-second request limit.
     */
    public void throttleStartDocumentTextDetection()
    {
        synchronized (startDocTextDetectLock)
        {
            long time = System.currentTimeMillis() - lastStartDocTextDetectTime;
            if (time > ONE_SECOND)
            {
                startDocTextDetectAPICallCount = 0;
                lastStartDocTextDetectTime = System.currentTimeMillis();
            }
            else
            {
                startDocTextDetectAPICallCount++;
                if (startDocTextDetectAPICallCount >= startDocTextThrottleLimit)
                {
                    startDocTextDetectAPICallCount = 0;
                    if (time < ONE_SECOND)
                    {
                        log.debug("INFORMATIONAL: SLEEPING FOR MILLS: " + (ONE_SECOND - time));
                        safeSleep(ONE_SECOND - time);
                    }
                    lastStartDocTextDetectTime = System.currentTimeMillis();
                }
            }
        }
    }

    /**
     * Throttles calls to the Textract GetDocumentTextDetection API
     * to stay within the configured per-second request limit.
     */
    public void throttleGetDocumentTextDetection()
    {
        synchronized (getDocTextDetectLock)
        {
            long time = System.currentTimeMillis() - lastGetDocTextDectectTime;
            if (time > ONE_SECOND)
            {
                getGetDocTextDectectAPICallCount = 0;
                lastGetDocTextDectectTime = System.currentTimeMillis();
            }
            else
            {
                getGetDocTextDectectAPICallCount++;
                if (getGetDocTextDectectAPICallCount >= getDocTextThrottleLimit)
                {
                    getGetDocTextDectectAPICallCount = 0;
                    if (time < ONE_SECOND)
                    {
                        log.debug("INFORMATIONAL: SLEEPING FOR MILLS: " + (ONE_SECOND - time));
                        safeSleep(ONE_SECOND - time);
                    }
                    lastGetDocTextDectectTime = System.currentTimeMillis();
                }
            }
        }
    }

    /**
     * Sleeps for the specified number of milliseconds, suppressing interruptions.
     *
     * @param i sleep duration in milliseconds
     */
    public void safeSleep(long i)
    {
        try
        {
            Thread.sleep(i);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            log.warn("TextractThrottleService safeSleep interrupted");
        }
    }

}
