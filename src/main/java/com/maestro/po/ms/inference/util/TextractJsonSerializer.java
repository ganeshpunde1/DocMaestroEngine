package com.maestro.po.ms.inference.util;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.textract.model.Block;
import software.amazon.awssdk.services.textract.model.Geometry;
import software.amazon.awssdk.services.textract.model.Point;
import software.amazon.awssdk.services.textract.model.Relationship;
import software.amazon.awssdk.services.textract.model.BoundingBox;

/**
 * Serializes AWS Textract {@link Block} objects to JSON using Jackson mixins
 * to control the output field names. Also supports generating structured JSON
 * from a flat key-value map using slash-delimited path keys.
 *
 * @author Ganesh Punde
 */
@Slf4j
@Component
public class TextractJsonSerializer
{
    private static ObjectMapper obj;

    public TextractJsonSerializer() {}

    @PostConstruct
    private void init()
    {
        log.debug("Initializing ObjectMapper in TextractJsonSerializer.java");
        obj = new ObjectMapper();
        obj.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        obj.configure(SerializationFeature.INDENT_OUTPUT, true);
        obj.addMixIn(Block.class, BaseBlockMixin.class);
        obj.addMixIn(Geometry.class, GeoBlockMixin.class);
        obj.addMixIn(Relationship.class, RelationshipBlockMixin.class);
        obj.addMixIn(BoundingBox.class, BoundingBoxMixin.class);
        obj.addMixIn(Point.class, PointBlockMixin.class);
        log.debug("ObjectMapper successfully initialized");
    }

    /**
     * Generates a nested JSON string from a flat map of slash-delimited path keys to values.
     * For example, key {@code "patient/name"} produces {@code {"patient": {"name": "value"}}}.
     *
     * @param params the map of path keys to string values
     * @return a JSON string representation, or {@code "{ }"} if the map is null or empty
     */
    public static String generateJsonFromMap(Map<String, String> params)
    {
        if (params == null || params.isEmpty())
        {
           return "{ }"; 
        }
        
        ObjectNode rootNode = obj.createObjectNode();

        for (Map.Entry<String, String> entry : params.entrySet())
        {
            String path = entry.getKey();
            String value = entry.getValue();

            String[] pathSegments = path.split("/");

            ObjectNode currentNode = rootNode;
            for (int i = 0; i < pathSegments.length; i++)
            {
                String segment = pathSegments[i];

                if (i == pathSegments.length - 1)
                { // Last segment is the key for the value
                    currentNode.put(segment, value);
                }
                else if ("".equalsIgnoreCase(segment))
                {
                    //Blank refers to the whole document, which is already created
                    continue;
                }
                else
                { // Intermediate segment, create a new object if it doesn't exist
                    if (!currentNode.has(segment))
                    {
                        currentNode.set(segment, obj.createObjectNode());
                    }
                    currentNode = (ObjectNode) currentNode.get(segment);
                }
            }
        }

        try
        {
            String jsonString = obj.writeValueAsString(rootNode);
            return jsonString;
        }
        catch (JsonProcessingException e)
        {
            log.error("Failed to serialize Textract map to JSON: {}", e.getMessage());
        }
        
        return "{ }";

        /*
        Removed dead commented-out code block
        */
    }

    /**
     * Serializes a list of Textract {@link Block} objects to a JSON string.
     *
     * @param blocks the list of Textract blocks to serialize
     * @return a JSON string of the blocks, or empty string on serialization failure
     */
    public static String convertTextractOutputToJson(List<Block> blocks)
    {
        if (CollectionUtils.isEmpty(blocks))
        {
            return "{}";

        }

        try
        {
            return obj.writeValueAsString(blocks);
        }
        catch (Exception e)
        {
            log.error("Deserialization of textract output failed for reason: " + e);
        }
        return "";
    }

    private abstract class BaseBlockMixin
    {
        @JsonProperty("block_type")
        abstract String blockTypeAsString();

        @JsonProperty("confidence")
        abstract Float confidence();

        @JsonProperty("text")
        abstract String text();

        @JsonProperty("text_type")
        abstract String textTypeAsString();

        @JsonProperty("row_index")
        abstract Integer rowIndex();

        @JsonProperty("column_index")
        abstract Integer columnIndex();

        @JsonProperty("row_span")
        abstract Integer rowSpan();

        @JsonProperty("column_span")
        abstract Integer columnSpan();

        @JsonProperty("geometry")
        abstract GeoBlockMixin geometry();

        @JsonProperty("id")
        abstract String id();

        @JsonProperty("entity_types")
        abstract List<String> entityTypesAsStrings();

        @JsonProperty("relationships")
        abstract List<RelationshipBlockMixin> relationships();

        @JsonProperty("selection_status")
        abstract String selectionStatusAsString();

        @JsonProperty("page")
        abstract Integer page();

    }

    private abstract class GeoBlockMixin
    {
        @JsonProperty("polygon")
        abstract List<PointBlockMixin> polygon();

        @JsonProperty("bounding_box")
        abstract BoundingBoxMixin boundingBox();
    }

    private abstract class PointBlockMixin
    {
        @JsonProperty("x")
        abstract Float x();

        @JsonProperty("y")
        abstract Float y();
    }

    private abstract class RelationshipBlockMixin
    {
        @JsonProperty("type")
        abstract String typeAsString();

        @JsonProperty("ids")
        abstract List<String> ids();
    }

    private abstract class BoundingBoxMixin
    {
        @JsonProperty("width")
        abstract Float width();

        @JsonProperty("height")
        abstract Float height();

        @JsonProperty("left")
        abstract Float left();

        @JsonProperty("top")
        abstract Float top();
    }

}
