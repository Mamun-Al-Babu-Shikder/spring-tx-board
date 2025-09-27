package com.sdlc.pro.txboard.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.time.Instant;

/**
 * Small fallback module that provides a serializer for java.time.Instant when
 * jackson-datatype-jsr310 is not present on the classpath.
 */
public class TxBoardFallbackJavaTimeModule extends SimpleModule {
    private static final long serialVersionUID = 1L;

    public TxBoardFallbackJavaTimeModule() {
        super("TxBoardFallbackJavaTimeModule");

        addSerializer(Instant.class, new JsonSerializer<Instant>() {
            @Override
            public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                if (value == null) {
                    gen.writeNull();
                } else {
                    // Use ISO-8601 format (Instant.toString()) which is a safe, readable representation
                    gen.writeString(value.toString());
                }
            }
        });
    }
}

