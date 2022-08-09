package com.rzware.collections.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class ErrorResponse {
    public Integer code;
    public String message;

    public ErrorResponse(){}
    public ErrorResponse(Integer code, String message){
        this.code = code;
        this.message = message;
    }
}
