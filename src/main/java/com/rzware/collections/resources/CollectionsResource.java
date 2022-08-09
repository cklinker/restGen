package com.rzware.collections.resources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.networknt.schema.JsonSchema;
import com.rzware.collections.SchemaDAO;
import com.rzware.collections.dto.ErrorResponse;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Path("/{tenant}")
@Produces(MediaType.APPLICATION_JSON)
public class CollectionsResource {
    @Inject
    SecurityIdentity identity;


    private String SCHEMA_COLLECTION_NAME = "schema";
    @Inject
    MongoClient mongoClient;
    @ConfigProperty(name = "collections.allow-adhoc")
    Boolean allowAdHoc;
    @Inject
    SchemaDAO schemaDAO;
    @Inject
    ObjectMapper objectMapper;

    @GET
    @Path("/{collection}")
    @Authenticated
    public Response getCollection(@PathParam("tenant") String tenant, @PathParam("collection") String collectionName,
                                @DefaultValue("0") @QueryParam("page") Integer page, @DefaultValue("20") @QueryParam("pageSize") Integer pageSize,
                                @QueryParam("filter") String filter) {
        if(!allowAdHoc && schemaDAO.getSchema(tenant, collectionName) == null){
            ErrorResponse er = new ErrorResponse(104, collectionName + " does not have a defined schema and adhoc collections are not allowed");
            return Response.status(404).entity(er).build();
        }
        MongoCollection<Document> collection = mongoClient.getDatabase(tenant).getCollection(collectionName);
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
        sb.delete(sb.length() - 1, sb.length());
        sb.append("]");
        return Response.ok(sb.toString()).build();
    }

    @GET
    @Path("/{collection}/{id}")
    @Authenticated
    public Response getItem(@PathParam("tenant") String tenant, @PathParam("collection") String collectionName,
                                @PathParam("id") String id) {
        if(!allowAdHoc && schemaDAO.getSchema(tenant, collectionName) == null){
            ErrorResponse er = new ErrorResponse(104, collectionName + " does not have a defined schema and adhoc collections are not allowed");
            return Response.status(404).entity(er).build();
        }
        MongoCollection<Document> collection = mongoClient.getDatabase(tenant).getCollection(collectionName);
        BasicDBObject query = new BasicDBObject();
        query.put("_id", new ObjectId(id));

        Document doc = collection.find(query).first();
        if(doc != null) return Response.ok(doc.toJson()).build();
        return null;
    }
    @DELETE
    @Path("/{collection}/{id}")
    @Authenticated
    public Response deleteItem(@PathParam("tenant") String tenant, @PathParam("collection") String collectionName,
                                @PathParam("id") String id) {
        if(!allowAdHoc && schemaDAO.getSchema(tenant, collectionName) == null){
            ErrorResponse er = new ErrorResponse(104, collectionName + " does not have a defined schema and adhoc collections are not allowed");
            return Response.status(404).entity(er).build();
        }
        MongoCollection<Document> collection = mongoClient.getDatabase(tenant).getCollection(collectionName);
        BasicDBObject query = new BasicDBObject();
        query.put("_id", new ObjectId(id));

        Document doc = collection.find(query).first();
        if(doc != null) {
            return Response.ok(collection.deleteOne(doc).getDeletedCount() == 1l).build();
        }
        return Response.ok(false).build();
    }
    @POST
    @Path("/{collection}")
    @Authenticated
    public Response createItem(@PathParam("tenant") String tenant, @PathParam("collection") String collectionName, String jsonNode) {
        JsonSchema schema = schemaDAO.getSchema(tenant, collectionName);
        if(!allowAdHoc && schema == null){
            ErrorResponse er = new ErrorResponse(104, collectionName + " does not have a defined schema and adhoc collections are not allowed");
            return Response.status(404).entity(er).build();
        }
        if(schema != null){
            Response r = schemaDAO.validateSchema(schema, jsonNode);
            if(r != null) return r;
        }
        MongoCollection<Document> collection = mongoClient.getDatabase(tenant).getCollection(collectionName);
        Document doc = Document.parse(jsonNode);
        collection.insertOne(doc);
        return Response.ok(doc.toJson()).build();
    }
    @PUT
    @Path("/{collection}/{id}")
    @Authenticated
    public Response updateItem(@PathParam("tenant") String tenant, @PathParam("collection") String collectionName,
                             @PathParam("id") String id, String jsonNode) {
        JsonSchema schema = schemaDAO.getSchema(tenant, collectionName);
        if(!allowAdHoc && schema == null){
            ErrorResponse er = new ErrorResponse(104, collectionName + " does not have a defined schema and adhoc collections are not allowed");
            return Response.status(404).entity(er).build();
        }
        if(schema != null){
            Response r = schemaDAO.validateSchema(schema, jsonNode);
            if(r != null) return r;
        }

        MongoCollection<Document> collection = mongoClient.getDatabase(tenant).getCollection(collectionName);
        BasicDBObject query = new BasicDBObject();
        query.put("_id", new ObjectId(id));

        Document doc = collection.find(query).first();
        if(doc != null){
            Document updateDoc = Document.parse(jsonNode);
            updateDoc.remove("_id");
            updateDoc.put("_id", doc.get("_id"));
            collection.findOneAndReplace(query, updateDoc);
            return Response.ok(updateDoc.toJson()).build();
        }
        ErrorResponse er = new ErrorResponse(103, "ID (" + id + ") not found in " + collectionName);
        return Response.status(404).entity(er).build();
    }
    @GET
    @Path("/swagger.json")
    public Response getSwagger(@PathParam("tenant") String tenant){
        MongoCollection<Document> collection = mongoClient.getDatabase(tenant).getCollection(SCHEMA_COLLECTION_NAME);
        FindIterable<Document> docs = collection.find();

        OpenAPI openAPI = new OpenAPI(SpecVersion.V31);
        Info info = new Info();
        info.setDescription("Swagger definition for " + tenant + " API");
        info.setTitle(tenant + " API");
        info.setVersion("1.0.0");
        openAPI.setInfo(info);
        Paths paths = new Paths();
        Components components = new Components();
        for(Document doc : docs) {
            PathItem pathItem = new PathItem();
            Operation getOperation = new Operation();
            getOperation.addParametersItem(new Parameter().in("query").name("filter").schema(new Schema().type("string")));
            getOperation.addParametersItem(new Parameter().in("query").name("page").schema(new Schema().type("integer").format("int32")._default("0")));
            getOperation.addParametersItem(new Parameter().in("query").name("pageSize").schema(new Schema().type("integer").format("int32")._default("20")));

            ApiResponses apiResponses = new ApiResponses();
            ApiResponse apiResponse = new ApiResponse().description("OK").content(new Content().addMediaType("application/json", new io.swagger.v3.oas.models.media.MediaType().schema(new Schema<>().type("array").items(new Schema<>().$ref("#/components/schemas/" + doc.getString("_id"))))));
            apiResponses.addApiResponse("200", apiResponse);

            try {
                io.swagger.v3.oas.models.media.Schema s = new io.swagger.v3.oas.models.media.Schema();
                s = objectMapper.readValue(doc.getString("schema"), s.getClass());
                s.addProperty("_id", new Schema().type("object").addProperty("$oid", new Schema().type("string")));
                components.addSchemas(doc.getString("_id"), s);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            getOperation.setResponses(apiResponses);
            pathItem.setGet(getOperation);
            Operation postOperation = new Operation();
            postOperation.requestBody(new RequestBody().content(new Content().addMediaType("application/json", new io.swagger.v3.oas.models.media.MediaType().schema(new Schema().$ref("#/components/schemas/" + doc.getString("_id"))))));
            ApiResponses postResponses = new ApiResponses();
            ApiResponse postResponse = new ApiResponse().description("OK").content(new Content().addMediaType("application/json", new io.swagger.v3.oas.models.media.MediaType().schema(new Schema<>().$ref("#/components/schemas/" + doc.getString("_id")))));
            postResponses.addApiResponse("200", postResponse);
            postOperation.setResponses(postResponses);
            pathItem.setPost(postOperation);
            paths.addPathItem("/" + tenant + "/" + doc.getString("_id"), pathItem);

            //Add /{id} paths
            PathItem idPathItem = new PathItem();
            //GET
            Operation idGetOperation = new Operation();
            idGetOperation.addParametersItem(new Parameter().in("path").name("id").required(true).schema(new Schema().type("string")));
            idGetOperation.setResponses(new ApiResponses().addApiResponse("200", new ApiResponse().description("OK").content(new Content().addMediaType("application/json", new io.swagger.v3.oas.models.media.MediaType().schema(new Schema<>().$ref("#/components/schemas/" + doc.getString("_id")))))));
            idPathItem.setGet(idGetOperation);

            //PUT
            Operation idPutOperation = new Operation();
            idPutOperation.addParametersItem(new Parameter().in("path").name("id").required(true).schema(new Schema().type("string")));
            idPutOperation.setResponses(new ApiResponses().addApiResponse("200", new ApiResponse().description("OK").content(new Content().addMediaType("application/json", new io.swagger.v3.oas.models.media.MediaType().schema(new Schema<>().$ref("#/components/schemas/" + doc.getString("_id")))))));
            idPutOperation.requestBody(new RequestBody().content(new Content().addMediaType("application/json", new io.swagger.v3.oas.models.media.MediaType().schema(new Schema<>().$ref("#/components/schemas/" + doc.getString("_id"))))));
            idPathItem.setPut(idPutOperation);

            //DELETE
            Operation idDeleteOperation = new Operation();
            idDeleteOperation.addParametersItem(new Parameter().in("path").name("id").required(true).schema(new Schema().type("string")));
            idDeleteOperation.setResponses(new ApiResponses().addApiResponse("200", new ApiResponse().description("OK").content(new Content().addMediaType("application/json", new io.swagger.v3.oas.models.media.MediaType().schema(new Schema<>().type("boolean"))))));
            idPathItem.setDelete(idDeleteOperation);

            paths.addPathItem("/" + tenant + "/" + doc.getString("_id") + "/{id}", idPathItem);

        }
//        OAuthFlows oAuthFlows = new OAuthFlows().implicit(new OAuthFlow().authorizationUrl("https://keycloak.rzware.com/realms/quarkus/protocol/openid-connect/auth").refreshUrl("https://keycloak.rzware.com/realms/quarkus/protocol/openid-connect/token").tokenUrl("https://keycloak.rzware.com/realms/quarkus/protocol/openid-connect/token/introspect"));
        components.addSecuritySchemes("openid", new SecurityScheme().openIdConnectUrl("https://keycloak.rzware.com/realms/quarkus/.well-known/openid-configuration").type(SecurityScheme.Type.OPENIDCONNECT));
//        components.addSecuritySchemes("oauth2", new SecurityScheme().description("Authentication").type(SecurityScheme.Type.OAUTH2).flows(oAuthFlows));
        openAPI.setComponents(components);
        List<SecurityRequirement> securityRequirements = new ArrayList<>();
        SecurityRequirement sr = new SecurityRequirement();
        List<String> scopes = new ArrayList<>();
        scopes.add("query");
        sr.addList("openid", scopes);
        securityRequirements.add(sr);
        openAPI.security(securityRequirements);
        openAPI.setPaths(paths);
        return Response.ok(Json.pretty(openAPI)).build();
    }
}