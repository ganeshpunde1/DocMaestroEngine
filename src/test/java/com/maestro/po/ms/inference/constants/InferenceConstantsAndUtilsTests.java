package com.maestro.po.ms.inference.constants;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.maestro.po.ms.inference.util.ResponseFormatValidator;

public class InferenceConstantsAndUtilsTests
{
    @Test
    public void validationTest() {
        String validJson = "{}";
        String validJsonArray = "[{\"infer\" : true}, {\"infer\" : true}]";
        String validXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root></root>";
        
        String invalidJson = "{{}";
        String invalidXml = "<?>";
        
        assertTrue(ResponseFormatValidator.isValidJson(validJson));
        assertTrue(ResponseFormatValidator.isValidJson(validJsonArray));
        assertTrue(ResponseFormatValidator.isValidXML(validXml));
        
        assertFalse(ResponseFormatValidator.isValidJson(invalidJson));
        assertFalse(ResponseFormatValidator.isValidXML(invalidXml));
    }
}
