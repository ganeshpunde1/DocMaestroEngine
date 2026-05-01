package com.maestro.po.ms.inference.service;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Component;

import com.maestro.po.ms.inference.model.rest.InferenceQueueItem;

/**
 * Thread-safe in-memory queue for holding pending PDF inference requests.
 * Uses a {@link ConcurrentLinkedQueue} to support concurrent enqueue and dequeue operations.
 *
 * @author Ganesh Punde
 */
@Component
public class PdfInferenceRequestQueue
{
    private Queue<InferenceQueueItem> items = new ConcurrentLinkedQueue<InferenceQueueItem>();

    /**
     * Adds an inference request item to the queue.
     * Items with null request or response fields are silently ignored.
     *
     * @param item the queue item containing the request and tracking record
     */
    public void enqueue(InferenceQueueItem item)
    {
        if (item != null && item.getInferenceRequest() != null && item.getInferenceResponse() != null)
        {
            items.add(item);
        }

    }

    /**
     * Removes and returns the next item from the queue.
     *
     * @return an {@link Optional} containing the next item, or empty if the queue is empty
     */
    public Optional<InferenceQueueItem> dequeue()
    {
        return Optional.ofNullable(items.poll());
    }

}
