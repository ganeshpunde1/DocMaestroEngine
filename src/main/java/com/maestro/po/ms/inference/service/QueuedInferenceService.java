package com.maestro.po.ms.inference.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.maestro.po.ms.inference.model.annotation.InferenceResponse;
import com.maestro.po.ms.inference.model.rest.InferenceQueueItem;
import com.maestro.po.ms.inference.model.rest.PdfInferenceRequest;

import lombok.extern.slf4j.Slf4j;

/**
 * Queued variant of the inference service that enqueues incoming requests
 * into an in-memory queue and processes them on a fixed schedule.
 * Extends {@link InferenceServiceImpl} to reuse the core pipeline logic.
 *
 * @author Ganesh Punde
 */
@Slf4j
@Component
@Configuration
public class QueuedInferenceService extends InferenceServiceImpl
{

    @Autowired
    private PdfInferenceRequestQueue inferenceQueue;
    
    /**
     * Enqueues the inference request instead of processing it immediately.
     * Validates inputs before adding to the queue.
     *
     * @param pr        the PDF inference request
     * @param requestId the inference tracking record
     */
    @Override
    @Async
    public void executeInferencePipeline(PdfInferenceRequest pr, InferenceResponse requestId)
    {
        if (pr == null || requestId == null || requestId.getRequestId() == null || requestId.getRequestId().isBlank())
        {
            log.debug("The request involves a null parameter. Stopping inference.");
            return;
        }
        
        inferenceQueue.enqueue(new InferenceQueueItem(pr, requestId));

    }

    /**
     * Scheduled task that dequeues the next pending inference request and
     * processes it through the core pipeline. Runs at the configured poll interval.
     */
    @Scheduled(fixedRateString = "${maestro.system.poll.interval:30000}")
    public void processNextQueuedRequest()
    {
        Optional<InferenceQueueItem> oItem = inferenceQueue.dequeue();
        if (oItem.isEmpty())
        {
            //log.debug("No items in the queue returning");
            return;
        }

        InferenceQueueItem item = oItem.get();

        log.debug("Initiating the inference for requestId: " + item.getInferenceResponse().getRequestId());
        processInferencePipeline(item.getInferenceRequest(), item.getInferenceResponse());
    }
}
