package com.rzware.collections.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.rzware.collections.dto.ErrorResponse;
import com.rzware.collections.dto.Schema;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import org.bson.BsonDocument;
import org.bson.Document;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/{tenant}/schema")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class SchemaResource {
    @Inject
    SecurityIdentity identity;
    private String SCHEMA_COLLECTION_NAME = "schema";
    @Inject
    MongoClient mongoClient;
    @Inject
    ObjectMapper objectMapper;
    @GET
    public String getCollection(@PathParam("tenant") String tenant, @DefaultValue("0") @QueryParam("page") Integer page,
                                @DefaultValue("20") @QueryParam("pageSize") Integer pageSize,
                                @QueryParam("filter") String filter) {
        MongoCollection<Document> collection = mongoClient.getDatabase(tenant).getCollection(SCHEMA_COLLECTION_NAME);
        FindIterable<Document> docs = null;
        if (filter != null) {
            docs = collection.find(BsonDocument.parse(filter));
        } else {
            docs = collection.find();
        }
        StringBuilder sb = new StringBuilder().append("[");
        for (Document doc : docs.limit(pageSize).skip(page * pageSize)) {
            sb.append(doc.toJson()).append(",");
        }
        ;
        sb.append("]");
        return sb.toString();
    }

    @GET
    @Path("/{id}")
    public Response getItem(@PathParam("tenant") String tenant, @PathParam("id") String id) {
        MongoCollection<Document> collection = mongoClient.getDatabase(tenant).getCollection(SCHEMA_COLLECTION_NAME);
        BasicDBObject query = new BasicDBObject();
        query.put("_id", id);

        Document doc = collection.find(query).first();
        if(doc != null) return Response.status(200).entity(doc.toJson()).build();
        return Response.status(404).build();
    }
    @DELETE
    @Path("/{id}")
    public Response deleteItem(@PathParam("tenant") String tenant, @PathParam("id") String id) {
        MongoCollection<Document> collection = mongoClient.getDatabase(tenant).getCollection(SCHEMA_COLLECTION_NAME);
        BasicDBObject query = new BasicDBObject();
        query.put("_id", id);

        Document doc = collection.find(query).first();
        if(doc != null) {
            return Response.status(200).entity(collection.deleteOne(doc).getDeletedCount() == 1l).build();
        }
        return Response.status(404).build();
    }
    @POST
    @Path("/{id}")
    public Response createItem(@PathParam("tenant") String tenant, @PathParam("id") String schemaName, Schema schema) {
        MongoCollection<Document> collection = mongoClient.getDatabase(tenant).getCollection(SCHEMA_COLLECTION_NAME);
        BasicDBObject query = new BasicDBObject();
        query.put("_id", schemaName);

        Document doc = collection.find(query).first();
        if(doc != null) {
            ErrorResponse er = new ErrorResponse(101, "Schema already exists");
            return Response.ok(er).build();
        }
        JsonSchema jsonSchema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(schema.getSchema());
        try {
            String obj = objectMapper.writeValueAsString(schema);
            doc = Document.parse(obj);
            doc.remove("_id");
            doc.put("_id", schemaName);
            collection.insertOne(doc);
            return Response.ok(doc.toJson()).build();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return Response.serverError().entity(e).build();
        }
    }
    @PUT
    @Path("/{id}")
    public Response updateItem(@PathParam("tenant") String tenant, @PathParam("id") String id, Schema schema) {
        MongoCollection<Document> collection = mongoClient.getDatabase(tenant).getCollection(SCHEMA_COLLECTION_NAME);
        BasicDBObject query = new BasicDBObject();
        query.put("_id", id);

        Document doc = collection.find(query).first();
        if(doc != null){
            JsonSchema jsonSchema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(schema.getSchema());
            try {
                String obj = objectMapper.writeValueAsString(schema);
                Document updateDoc = Document.parse(obj);
                updateDoc.remove("_id");
                updateDoc.put("_id", id);
                collection.findOneAndReplace(query, updateDoc);
                return Response.ok(updateDoc.toJson()).build();
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        ErrorResponse er = new ErrorResponse(103, "Schema not found");
        return Response.status(404).entity(er).build();
    }

}
