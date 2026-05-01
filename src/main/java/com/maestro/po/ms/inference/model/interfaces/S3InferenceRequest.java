package com.maestro.po.ms.inference.model.interfaces;

import java.util.HashMap;
import java.util.Map;

public interface S3InferenceRequest
{
    default Map<String, String> tags()
    {
        return new HashMap<String, String>();   
    }
    
    default Map<String, String> metadata()
    {
        return new HashMap<String, String>();
    }
    
    String s3Content();
}
