package com.example.demo.config;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;
import org.locationtech.jts.geom.Point;

public class PointSerializer extends StdSerializer<Point> {

    public PointSerializer() {
        super(Point.class);
    }

    @Override
    public void serialize(Point value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        gen.writeStartObject();
        gen.writeStringProperty("type", "Point");
        gen.writeArrayPropertyStart("coordinates");
        gen.writeNumber(value.getX()); // longitude (x)
        gen.writeNumber(value.getY()); // latitude (y)
        gen.writeEndArray();
        gen.writeEndObject();
    }
}
