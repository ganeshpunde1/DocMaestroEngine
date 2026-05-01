package com.maestro.po.ms.inference.model.annotation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import software.amazon.awssdk.services.textract.model.Block;

@Data
@AllArgsConstructor
public class TextractJobContext
{
    private String jobId;

    private String requestId;

    private Map<String, List<Block>> sortedBlocks;

    public List<Block> allBlocks;
    
    public Map<String, String> textractOutput;
    
    public String resultJson;
    
    public Map<Integer, Map<String, List<TextractBlockCoordinate>>> intermediateRepresentation;
    
    public Map<Integer, Map<String, String>> keyValueByPage;
    
    public TextractJobContext()
    {
        this.sortedBlocks = new HashMap<String, List<Block>>();
        this.allBlocks = new ArrayList<Block>();
        this.textractOutput = new HashMap<String, String>();
        this.intermediateRepresentation = new HashMap<Integer, Map<String, List<TextractBlockCoordinate>>>();
        this.keyValueByPage = new HashMap<Integer, Map<String,String>>();
    }

}
