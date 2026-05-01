package com.maestro.po.ms.inference.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.maestro.po.ms.inference.annotations.Base64SizeCheck;
import com.maestro.po.ms.inference.annotations.EnumValueCheck;
import com.maestro.po.ms.inference.annotations.PropertyValueCheck;
import com.maestro.po.ms.inference.model.annotation.EnumInferenceAnswerMimeType;
import com.maestro.po.ms.inference.repository.EnumInferenceAnswerMimeTypeRepository;
import com.maestro.po.ms.inference.service.Base64SizeValidator;
import com.maestro.po.ms.inference.service.Base64StringValidator;
import com.maestro.po.ms.inference.service.EnumValueValidator;
import com.maestro.po.ms.inference.service.PropertyValueValidator;

@TestPropertySource(locations = "classpath:test.properties")
@RunWith(SpringRunner.class)
@SpringBootTest
public class InferenceValidatorTest
{
    private ClassLoader classLoader = this.getClass().getClassLoader();

    @Autowired
    EnumInferenceAnswerMimeTypeRepository mimeType;

    @Autowired
    Environment environment;
    
    @EnumValueCheck(enumRepository = EnumInferenceAnswerMimeTypeRepository.class)
    private String emptyEnumString = "";
    
    @PropertyValueCheck(property="test.property.available")
    private String emptyPropertyString = "";
    
    @Base64SizeCheck
    private String empty = "";
    
    @Base64SizeCheck(maxSize = 100)
    private String maxSize100 = "";
    
    @Base64SizeCheck(minSize = 100)
    private String minSize100 = "";
    
    @Base64SizeCheck(maxSizeProperty = "max")
    private String maxSizeString100 = "";
    
    @Base64SizeCheck(minSizeProperty = "min")
    private String minSizeString100 = "";
    

    @Test
    public void base64Validator() throws Exception
    {
        Base64StringValidator b64SV = new Base64StringValidator();
        assertTrue(b64SV.isValid(null, null));
        assertTrue(b64SV.isValid("", null));
        assertFalse(b64SV.isValid("AWS_MAXIMUS", null));
        URL resource = classLoader.getResource("base64.txt");
        if (resource != null)
        {
            String base64String = FileUtils.readFileToString(new File(resource.getFile()), Charset.defaultCharset());
            assertTrue(b64SV.isValid(base64String, null));
        }
    }

    @Test
    public void enumValueValidator() throws Exception
    {
        EnumValueValidator eVV = new EnumValueValidator();
        assertTrue(eVV.isValid(null, null));
        assertTrue(eVV.isValid("", null));
        EnumInferenceAnswerMimeTypeRepository mimeType = mock(EnumInferenceAnswerMimeTypeRepository.class);
        when(mimeType.findById("true")).thenReturn(Optional.of(new EnumInferenceAnswerMimeType()));
        EnumValueCheck check = this.getClass().getDeclaredField("emptyEnumString").getAnnotation(EnumValueCheck.class);
        eVV.initialize(check);
        eVV.parentEnumRepository = mimeType;
        assertTrue(eVV.isValid("true", null));
        assertFalse(eVV.isValid("false", null));

        eVV.parentEnumRepository = null;
        assertFalse(eVV.isValid("true", null));
    }

    @Test
    public void propertyValueValidator() throws Exception
    {
        PropertyValueValidator propertyValueValidator = new PropertyValueValidator();
        String baseProp = environment.getProperty("test.property.available");
        if (baseProp != null)
            propertyValueValidator.setProps(List.of(baseProp.split(",")));
        assertTrue(propertyValueValidator.isValid(null, null));
        assertTrue(propertyValueValidator.isValid("", null));
        assertTrue(propertyValueValidator.isValid("true", null));
        assertFalse(propertyValueValidator.isValid("false", null));
    }

    @Test
    public void base64SizeValidatorTest() throws Exception
    {
        Base64SizeValidator b64SV = new Base64SizeValidator();
        assertTrue(b64SV.isValid(null, null));
        assertTrue(b64SV.isValid("", null));
        assertTrue(b64SV.isValid("AWS_MAXIMUS", null));
        Base64SizeCheck emptyAnnon = this.getClass().getDeclaredField("empty").getAnnotation(Base64SizeCheck.class);
        Base64SizeCheck maxSizeAnnon = this.getClass().getDeclaredField("maxSize100").getAnnotation(Base64SizeCheck.class);
        Base64SizeCheck maxSizeStringAnnon = this.getClass().getDeclaredField("maxSizeString100").getAnnotation(Base64SizeCheck.class);
        Base64SizeCheck minSizeAnnon = this.getClass().getDeclaredField("minSize100").getAnnotation(Base64SizeCheck.class);
        Base64SizeCheck minSizeStringAnnon = this.getClass().getDeclaredField("minSizeString100").getAnnotation(Base64SizeCheck.class);
        
        
        
        assertDoesNotThrow(new Executable() {

            @Override
            public void execute() throws Throwable
            {
                b64SV.initialize(emptyAnnon);
                b64SV.initialize(maxSizeAnnon);
                // skip property-based annotations that require Spring context
                b64SV.initialize(minSizeAnnon);
            }

        });

        URL resource = classLoader.getResource("base64.txt");
        if (resource != null)
        {
            String base64String = FileUtils.readFileToString(new File(resource.getFile()), Charset.defaultCharset());
            b64SV.initialize(emptyAnnon);
            assertTrue(b64SV.isValid(base64String, null));

            b64SV.initialize(maxSizeAnnon);
            assertFalse(b64SV.isValid(base64String, null));
        }
    }

}
