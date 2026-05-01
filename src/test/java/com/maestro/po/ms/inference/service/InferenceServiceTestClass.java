package com.maestro.po.ms.inference.service;

import static com.maestro.po.ms.inference.TestUtils.assertNotEmpty;
import static com.maestro.po.ms.inference.TestUtils.setMockField;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.maestro.po.ms.inference.exception.BadDataException;
import com.maestro.po.ms.inference.exception.InferenceException;
import com.maestro.po.ms.inference.model.annotation.InferenceAnswer;
import com.maestro.po.ms.inference.model.annotation.InferenceResponse;
import com.maestro.po.ms.inference.model.rest.InferenceQueueItem;
import com.maestro.po.ms.inference.model.rest.PdfInferenceRequest;
import com.maestro.po.ms.inference.repository.InferenceAnswerRepository;
import com.maestro.po.ms.inference.repository.InferenceResponseRepository;
import com.maestro.po.ms.inference.service.PdfInferenceRequestQueue;
import com.maestro.po.ms.inference.service.QueuedInferenceService;
import com.maestro.po.ms.inference.service.InferenceRetryService;
import com.maestro.po.ms.inference.service.InferenceServiceImpl;

import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseOutput;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.Message;

public class InferenceServiceTestClass
{

    @Test
    public void testQueue()
    {
        PdfInferenceRequestQueue inferenceQueue = new PdfInferenceRequestQueue();

        PdfInferenceRequest rq = new PdfInferenceRequest();
        InferenceResponse ir = new InferenceResponse();

        InferenceQueueItem nullItem1 = new InferenceQueueItem(null, null);
        InferenceQueueItem nullItem2 = new InferenceQueueItem(rq, null);
        InferenceQueueItem nullItem3 = new InferenceQueueItem(null, ir);
        InferenceQueueItem nonNullItem = new InferenceQueueItem(rq, ir);
        assertDoesNotThrow(new Executable()
        {

            @Override
            public void execute() throws Throwable
            {
                inferenceQueue.enqueue(null);
                inferenceQueue.enqueue(nullItem1);
                inferenceQueue.enqueue(nullItem2);
                inferenceQueue.enqueue(nullItem3);
                inferenceQueue.enqueue(nonNullItem);
            }

        });
    }

    @Test
    public void retryInferenceTest()
    {
        InferenceRetryService inferenceRetryService = new InferenceRetryService();

        BedrockRuntimeAsyncClient client = mock(BedrockRuntimeAsyncClient.class);

        inferenceRetryService.asyncBedrockRuntimeClient = client;
        ConverseRequest rq = ConverseRequest.builder().build();
        Supplier<ConverseResponse> sup = new Supplier<ConverseResponse>()
        {
            @Override
            public ConverseResponse get()
            {
                ContentBlock block = ContentBlock.fromText("{}");
                Message ms = Message.builder().content(block).build();
                ConverseOutput co = ConverseOutput.fromMessage(ms);
                return ConverseResponse.builder().output(co).build();
            }
        };
        CompletableFuture<ConverseResponse> resp = CompletableFuture.supplyAsync(sup);
        when(client.converse(rq)).thenReturn(resp);
        assertNotNull(rq);
        assertNotNull(inferenceRetryService.retryConverseRequest(rq, "AWQCEVRBTNNT", "text/plain"));
        assertNotNull(inferenceRetryService.retryConverseRequest(rq, "AWQCEVRBTNNT", "application/json"));
        assertNotNull(inferenceRetryService.retryConverseRequest(rq, "AWQCEVRBTNNT", null));

        assertThrows(InferenceException.class, new Executable()
        {

            @Override
            public void execute() throws Throwable
            {
                inferenceRetryService.retryConverseRequest(null, "AWQCEVRBTNNT", null);
            }

        });

        assertThrows(InferenceException.class, new Executable()
        {

            @Override
            public void execute() throws Throwable
            {
                inferenceRetryService.retryConverseRequest(rq, "AWQCEVRBTNNT", "application/xml");
            }

        });
    }

    @Test
    public void inferenceQueuingServiceTest()
    {
        QueuedInferenceService inferenceQueuingService = new QueuedInferenceService();
        PdfInferenceRequestQueue queue = mock(PdfInferenceRequestQueue.class);
        setMockField(inferenceQueuingService, queue, "inferenceQueue");

        PdfInferenceRequest rq = new PdfInferenceRequest();
        InferenceResponse ir = new InferenceResponse();

        Optional<InferenceQueueItem> oItem = Optional.of(new InferenceQueueItem(rq, ir));
        when(queue.dequeue()).thenReturn(oItem);

        assertDoesNotThrow(new Executable()
        {

            @Override
            public void execute() throws Throwable
            {
                inferenceQueuingService.executeInferencePipeline(null, null);
                inferenceQueuingService.executeInferencePipeline(rq, null);
                inferenceQueuingService.executeInferencePipeline(null, ir);
                inferenceQueuingService.executeInferencePipeline(rq, ir);

                ir.setRequestId("");
                inferenceQueuingService.executeInferencePipeline(rq, ir);
            }

        });

        ir.setRequestId("EWTRH%H^%H^%");
        rq.setQueries(new ArrayList<>());

        assertDoesNotThrow(new Executable()
        {

            @Override
            public void execute() throws Throwable
            {
                inferenceQueuingService.executeInferencePipeline(rq, ir);
                inferenceQueuingService.processNextQueuedRequest();
            }

        });

    }

    @Test
    public void testGetInference()
    {
        InferenceServiceImpl inferenceServiceImpl = new InferenceServiceImpl();
        InferenceResponseRepository nullRepo = mock(InferenceResponseRepository.class);
        when(nullRepo.findByRequestId(any())).thenReturn(null);
        setMockField(inferenceServiceImpl, nullRepo, "inferenceResponseRepository");

        assertThrows(BadDataException.class, new Executable()
        {
            @Override
            public void execute() throws Throwable
            {
                inferenceServiceImpl.getInferenceResult(null);
            }
        });

        assertThrows(BadDataException.class, new Executable()
        {
            @Override
            public void execute() throws Throwable
            {
                inferenceServiceImpl.getInferenceResult("");
            }
        });
        assertEquals("UNKNOWN", inferenceServiceImpl.getInferenceResult("sassafras").getStatus());
        InferenceResponseRepository nonNullRepo = mock(InferenceResponseRepository.class);

        InferenceResponse dummyResp = new InferenceResponse();
        dummyResp.setStatusCd("ALTERED");
        when(nonNullRepo.findByRequestId(any())).thenReturn(dummyResp);

        InferenceAnswerRepository nullAnswerRepo = mock(InferenceAnswerRepository.class);
        when(nullAnswerRepo.findAllByInferenceResponseId(any())).thenReturn(null);
        setMockField(inferenceServiceImpl, nonNullRepo, "inferenceResponseRepository");
        setMockField(inferenceServiceImpl, nullAnswerRepo, "answerRepository");

        assertEquals("ALTERED", inferenceServiceImpl.getInferenceResult("sassafras").getStatus());

        InferenceAnswerRepository nonNullAnswerRepo = mock(InferenceAnswerRepository.class);
        List<InferenceAnswer> ans = new ArrayList<>();
        for (int i = 0; i < 4; ++i)
            ans.add(new InferenceAnswer());
        when(nonNullAnswerRepo.findAllByInferenceResponseId(any())).thenReturn(ans);
        setMockField(inferenceServiceImpl, nonNullAnswerRepo, "answerRepository");
        
        assertNotNull(inferenceServiceImpl.getInferenceResult("sassafras").getStatus());
        assertNotEmpty(inferenceServiceImpl.getInferenceResult("sassafras").getInferenceAnswers());

    }

}
