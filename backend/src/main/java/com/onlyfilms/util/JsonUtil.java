package com.onlyfilms.util;

import com.google.gson.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class JsonUtil {
    private static final Gson gson = new GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
        .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
        .create();
    
    // TypeAdapter for LocalDate
    private static class LocalDateAdapter implements JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
        private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        
        @Override
        public JsonElement serialize(LocalDate date, Type type, JsonSerializationContext context) {
            return date == null ? JsonNull.INSTANCE : new JsonPrimitive(date.format(formatter));
        }
        
        @Override
        public LocalDate deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonNull() || json.getAsString().isEmpty()) {
                return null;
            }
            return LocalDate.parse(json.getAsString(), formatter);
        }
    }
    
    // TypeAdapter for LocalDateTime
    private static class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        
        @Override
        public JsonElement serialize(LocalDateTime dateTime, Type type, JsonSerializationContext context) {
            return dateTime == null ? JsonNull.INSTANCE : new JsonPrimitive(dateTime.format(formatter));
        }
        
        @Override
        public LocalDateTime deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonNull() || json.getAsString().isEmpty()) {
                return null;
            }
            return LocalDateTime.parse(json.getAsString(), formatter);
        }
    }
    
    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }
    
    public static <T> T fromJson(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }
    
    public static <T> T fromRequest(HttpServletRequest req, Class<T> clazz) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return fromJson(sb.toString(), clazz);
    }
    
    public static void sendJson(HttpServletResponse resp, Object obj) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();
        out.print(toJson(obj));
        out.flush();
    }
    
    public static void sendJson(HttpServletResponse resp, int statusCode, Object obj) throws IOException {
        resp.setStatus(statusCode);
        sendJson(resp, obj);
    }
    
    public static void sendError(HttpServletResponse resp, int statusCode, String message) throws IOException {
        resp.setStatus(statusCode);
        sendJson(resp, new ApiResponse(false, message, null));
    }
    
    public static void sendSuccess(HttpServletResponse resp, String message, Object data) throws IOException {
        sendJson(resp, new ApiResponse(true, message, data));
    }
    
    public static void sendSuccess(HttpServletResponse resp, Object data) throws IOException {
        sendJson(resp, new ApiResponse(true, "Success", data));
    }
    
    // Alias methods used by existing servlets
    public static void writeSuccess(HttpServletResponse resp, Object data) throws IOException {
        sendJson(resp, 200, new ApiResponse(true, "Success", data));
    }
    
    public static void writeSuccess(HttpServletResponse resp, String message, Object data) throws IOException {
        sendJson(resp, 200, new ApiResponse(true, message, data));
    }
    
    public static void writeCreated(HttpServletResponse resp, Object data) throws IOException {
        sendJson(resp, 201, new ApiResponse(true, "Created", data));
    }
    
    public static void writeBadRequest(HttpServletResponse resp, String message) throws IOException {
        sendJson(resp, 400, new ApiResponse(false, message, null));
    }
    
    public static void writeUnauthorized(HttpServletResponse resp, String message) throws IOException {
        sendJson(resp, 401, new ApiResponse(false, message, null));
    }
    
    public static void writeForbidden(HttpServletResponse resp, String message) throws IOException {
        sendJson(resp, 403, new ApiResponse(false, message, null));
    }
    
    public static void writeNotFound(HttpServletResponse resp, String message) throws IOException {
        sendJson(resp, 404, new ApiResponse(false, message, null));
    }
    
    public static void writeServerError(HttpServletResponse resp, String message) throws IOException {
        sendJson(resp, 500, new ApiResponse(false, message, null));
    }
}
