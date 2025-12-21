package com.sdlc.pro.txboard.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Instant;

@Configuration(proxyBeanMethods = false)
public class GsonConfig {

    @Bean
    public Gson gson() {
        GsonBuilder builder = new GsonBuilder();

        TypeAdapter<Instant> instantAdapter = new TypeAdapter<>() {
            @Override
            public void write(JsonWriter out, Instant value) throws IOException {
                if (value == null) {
                    out.nullValue();
                    return;
                }
                out.value(value.toString());
            }

            @Override
            public Instant read(JsonReader in) throws IOException {
                if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                    in.nextNull();
                    return null;
                }
                String s = in.nextString();
                try {
                    return Instant.parse(s);
                } catch (Exception ex) {
                    throw new JsonParseException(ex);
                }
            }
        };

        builder.registerTypeAdapter(Instant.class, instantAdapter);
        return builder.create();
    }
}