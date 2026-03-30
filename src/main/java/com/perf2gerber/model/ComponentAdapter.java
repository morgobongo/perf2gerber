package com.perf2gerber.model;

import com.google.gson.*;

import java.lang.reflect.Type;

public class ComponentAdapter implements JsonSerializer<Component>, JsonDeserializer<Component> {
    @Override
    public JsonElement serialize(Component src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject result = context.serialize(src).getAsJsonObject();
        result.addProperty("_type", src.getClass().getSimpleName());
        return result;
    }

    @Override
    public Component deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        String type = jsonObject.get("_type").getAsString();

        if ("FixedComponent".equals(type)) {
            return context.deserialize(jsonObject, FixedComponent.class);
        } else if ("StretchComponent".equals(type)) {
            return context.deserialize(jsonObject, StretchComponent.class);
        }

        throw new JsonParseException("Unknown Component Type: " + type);
    }
}
