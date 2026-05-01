package com.maestro.po.ms.inference.model.rest;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.maestro.po.ms.inference.annotations.RegexCheck;
import com.maestro.po.ms.inference.model.annotation.EitherRegexOrAnswerCheck;

import lombok.Data;

@Data
@EitherRegexOrAnswerCheck(message="Both answers and regex cannot be given for the same field")
public class FieldMatchingRule
{
    @JsonProperty("answers")
    private List<String> answers;
    
    @JsonProperty("regex")
    @RegexCheck
    private String regex;

}
