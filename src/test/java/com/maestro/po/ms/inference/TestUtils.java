package com.maestro.po.ms.inference;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collection;

import org.junit.jupiter.api.AssertionFailureBuilder;
import org.springframework.test.util.ReflectionTestUtils;

public final class TestUtils
{
    private TestUtils() {}
    
    public static void assertNotEmpty(Collection<? extends Object> c) {
        assertNotNull(c);
        if (c.isEmpty())
        {
            AssertionFailureBuilder.assertionFailure()
                .message("Expected the collection to be non-empty")
                .reason("The given collection was empty")
                .actual(c)
                .buildAndThrow();
        }
    }

    public static void setMockField(Object source, Object mockedField, String name)
    {
        if (source == null)
            return;

        ReflectionTestUtils.setField(source, name, mockedField);

    }
}
