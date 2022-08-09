package com.rzware.collections;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.rzware.collections.dto.ErrorResponse;
import com.rzware.collections.dto.Schema;
import org.bson.Document;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.util.Set;

@ApplicationScoped
public class SchemaDAO {
    private String SCHEMA_COLLECTION_NAME = "schema";
    @Inject
    MongoClient mongoClient;
    @Inject
    ObjectMapper objectMapper;

    public JsonSchema getSchema(String tenant, String collectionName){
        MongoCollection<Document> collection = mongoClient.getDatabase(tenant).getCollection(SCHEMA_COLLECTION_NAME);
        BasicDBObject query = new BasicDBObject();
        query.put("_id", collectionName);

        Document doc = collection.find(query).first();
        if(doc != null){
            JsonSchema jsonSchema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(doc.getString("schema"));
            return jsonSchema;
        }
        return null;
    }

    public Response validateSchema(JsonSchema schema, String jsonNode){
        try {
            JsonNode jn = objectMapper.readTree(jsonNode);
            Set<ValidationMessage> validations = schema.validate(jn);
            if(validations != null && validations.size() > 0){
                String message = "";
                for(ValidationMessage vm : validations){
                    message += vm.getMessage() + "  ";
                }
                ErrorResponse er = new ErrorResponse(106, message);
                return Response.status(404).entity(er).build();
            }
        } catch (JsonProcessingException e) {
            ErrorResponse er = new ErrorResponse(105, "Invalid data");
            return Response.status(404).entity(er).build();
        }
        return null;
    }
}
