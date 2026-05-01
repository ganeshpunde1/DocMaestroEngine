package com.maestro.po.ms.inference.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.maestro.po.ms.inference.repository.InferenceAnswerRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Throttles outgoing Bedrock API calls based on the current tokens-per-minute (TPM)
 * usage. Blocks the calling thread until TPM drops below the configured limit.
 *
 * @author Ganesh Punde
 */
@Slf4j
@Component
public class TokenThrottleService
{
    @Autowired
    InferenceAnswerRepository inferenceAnswerRepository;

    @Value("${maestro.inference.config.tokenLimit}")
    private long tokenLimit;

    private static final Object blocker = new Object();

    /**
     * Blocks the current thread until the rolling one-minute token usage
     * falls below the configured token limit.
     */
    public void throttle()
    {
        synchronized (blocker)
        {
            Long currentTPM = inferenceAnswerRepository.calculateCurrentTPM();
            if (currentTPM == null || currentTPM < 0) currentTPM = 0L;

            while (currentTPM > tokenLimit)
            {
                sleep(2000);
                currentTPM = inferenceAnswerRepository.calculateCurrentTPM();
                if (currentTPM == null || currentTPM < 0) currentTPM = 0L;
            }
        }
    }

    private void sleep(int length)
    {
        try
        {
            log.debug("Sleeping for {} ms.", length);
            Thread.sleep(length);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            log.warn("TokenThrottleService sleep interrupted");
        }
    }
}
