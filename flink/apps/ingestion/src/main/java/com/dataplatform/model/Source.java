package com.dataplatform.model;

import java.util.Map;

public class Source {
    private String type;
    private String format;
    private String packaging;
    private Map<String, Object> auth;
    private FlatteningInstructions flatteningInstructions;
    private Map<String, String> parserOptions;
    private Api api;
    private S3 s3;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getPackaging() {
        return packaging;
    }

    public void setPackaging(String packaging) {
        this.packaging = packaging;
    }

    public Map<String, Object> getAuth() {
        return auth;
    }

    public void setAuth(Map<String, Object> auth) {
        this.auth = auth;
    }

    public Map<String, String> getParserOptions() {
        return parserOptions;
    }

    public void setParserOptions(Map<String, String> parserOptions) {
        this.parserOptions = parserOptions;
    }

    public FlatteningInstructions getFlatteningInstructions() {
        return flatteningInstructions;
    }

    public void setFlatteningInstructions(FlatteningInstructions flatteningInstructions) {
        this.flatteningInstructions = flatteningInstructions;
    }

    public Api getApi() {
        return api;
    }

    public void setApi(Api api) {
        this.api = api;
    }

    public S3 getS3() {
        return s3;
    }

    public void setS3(S3 s3) {
        this.s3 = s3;
    }
}
