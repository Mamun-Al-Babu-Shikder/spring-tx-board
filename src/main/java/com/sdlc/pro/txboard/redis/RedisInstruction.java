package com.sdlc.pro.txboard.redis;

public enum RedisInstruction {
    FT_LIST("FT._LIST"),
    FT_INFO("FT.INFO"),
    JSON_SET("JSON.SET"),
    JSON_GET("JSON.GET"),
    FT_SEARCH("FT.SEARCH"),
    FT_CREATE("FT.CREATE"),
    FT_AGGREGATE("FT.AGGREGATE");

    public final String instruction;

    RedisInstruction(String instruction) {
        this.instruction = instruction;
    }

    public String getInstruction() {
        return instruction;
    }
}
