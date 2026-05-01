package com.maestro.po.ms.inference;

import static com.maestro.po.ms.inference.TestUtils.setMockField;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.maestro.po.ms.inference.controller.PdfInferenceController;
import com.maestro.po.ms.inference.exception.BadDataException;
import com.maestro.po.ms.inference.handler.CustomGlobalExceptionHandler;
import com.maestro.po.ms.inference.model.annotation.InferenceResponse;
import com.maestro.po.ms.inference.model.rest.InferenceRestResponse;
import com.maestro.po.ms.inference.model.rest.PdfInferenceRequest;
import com.maestro.po.ms.inference.service.InferenceService;

@RunWith(SpringRunner.class)
@SpringBootTest()
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@TestPropertySource(locations = "classpath:test.properties")
class InferenceApplicationTests
{
    
    @Mock
    PdfInferenceController inferenceController;

    @Mock
    InferenceService inferenceService;

    @Test
    void contextLoads()
    {
        return;
    }

    @Test
    public void customGlobalErrorHandler()
    {
        CustomGlobalExceptionHandler errorHandler = new CustomGlobalExceptionHandler();

    }

    @Test
    public void inferenceController()
    {
        InferenceResponse rsp = new InferenceResponse();
        InferenceRestResponse irr = new InferenceRestResponse();
        when(inferenceService.createInference()).thenReturn(rsp);
        when(inferenceService.getInferenceResult(any())).thenReturn(irr);
        PdfInferenceController inf = new PdfInferenceController();
        setMockField(inf, inferenceService, "inferenceService");
        inf.submitPdfSummarizationRequest(new PdfInferenceRequest());
        assertDoesNotThrow(new Executable()
        {

            @Override
            public void execute() throws Throwable
            {
                inferenceController.submitPdfSummarizationRequest(null);
            }

        });

        assertThrows(BadDataException.class, new Executable()
        {

            @Override
            public void execute() throws Throwable
            {
                new PdfInferenceController().retrieveSummarizationResult(null);
            }

        });
        assertDoesNotThrow(new Executable()
        {

            @Override
            public void execute() throws Throwable
            {
                inf.retrieveSummarizationResult("ASXVERB");
            }

        });       
        
    }
    
    @Test
    public void testAWSAPIConfig()
    {
        // AwsClientConfiguration is a @Configuration class and cannot be autowired as a bean directly in tests
        assertDoesNotThrow(new Executable()
        {
            @Override
            public void execute() throws Throwable
            {
                new com.maestro.po.ms.inference.provider.AwsClientConfiguration();
            }
        });
    }

}
