package com.maestro.po.ms.inference.util;

import java.io.ByteArrayInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Utility class for validating the format of Bedrock response payloads.
 * Supports JSON and XML format validation.
 *
 * @author Ganesh Punde
 */
public class ResponseFormatValidator
{
    private ResponseFormatValidator(){}

    /**
     * Validates whether the given string is well-formed XML.
     *
     * @param xml the string to validate
     * @return {@code true} if the string is valid XML, {@code false} otherwise
     */
    public static boolean isValidXML(String xml)
    {
        if (xml == null || xml.isBlank()) return false;
        try
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Disable XXE to prevent XML External Entity injection attacks
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.parse(new ByteArrayInputStream(xml.getBytes()));
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    /**
     * Validates whether the given string is well-formed JSON (object or array).
     *
     * @param json the string to validate
     * @return {@code true} if the string is valid JSON, {@code false} otherwise
     */
    public static boolean isValidJson(String json)
    {
        try
        {
            new JSONObject(json);
        }
        catch (Exception ex)
        {
            try
            {
                new JSONArray(json);
            }
            catch (Exception ex2)
            {
                return false;
            }
        }
        return true;
    }

}
