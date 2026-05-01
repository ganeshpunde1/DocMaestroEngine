package com.maestro.po.ms.inference.model.annotation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TextractBlockCoordinate
{
    Float top;
    
    Float left;
    
    String text;
}
