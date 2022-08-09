package com.rzware.collections.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.bson.codecs.pojo.annotations.BsonProperty;

import java.util.Objects;

@RegisterForReflection
public class Schema {
    private String id;
    private  String schema;

    public Schema() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Schema schema = (Schema) o;
        return id.equals(schema.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
