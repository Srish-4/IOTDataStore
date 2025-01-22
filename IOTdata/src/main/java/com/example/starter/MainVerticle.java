package com.example.starter;

import io.vertx.core.AbstractVerticle;   // Vert.x AbstractVerticle class
import io.vertx.core.Promise;             // Promise class for handling start/fail logic
import io.vertx.core.Vertx;               // Vertx class for creating and managing verticles
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;                  // SLF4J Logger interface
import org.slf4j.LoggerFactory;           // SLF4J LoggerFactory for creating loggers

import java.util.HashMap;
import java.util.Map;


public class MainVerticle extends AbstractVerticle {

  private static final String SERVICE_ADDRESS = "crud.service";

  private final Map<String,JsonObject> dataStore= new HashMap<>();

  public static void main(String[] args) {
    var v = Vertx.vertx();
    v.deployVerticle(new MainVerticle());
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    Router router= Router.router(vertx);

    router.route().handler(BodyHandler.create());

   router.get("/api/:deviceType/:id").handler(this::handleGetRequest);
   router.post("/api/:deviceType/:id").handler(this::handlePostRequest);
   router.put("/api/:deviceType/:id").handler(this::handlePutRequest);
   router.delete("/api/:deviceType/:id").handler(this::handleDeleteRequest);


    vertx.createHttpServer()
      .requestHandler(router)
      .listen(8080)
      .onComplete(http -> {
        if (http.succeeded()) {
          startPromise.complete();
          System.out.println("HTTP server started on port 8080");
        } else {
          startPromise.fail(http.cause());
        }
      });

  }

  private void handlePostRequest(RoutingContext routingContext) {
    System.out.println("Headers: " + routingContext.request().headers());
    System.out.println("Raw Body: " + routingContext.body().asString());
    String deviceType = routingContext.pathParam("deviceType");
    String deviceId = routingContext.pathParam("id");

  JsonObject body = routingContext.body().asJsonObject();

  if (body == null)
  {
    routingContext.response()
      .setStatusCode(400)
      .putHeader("content-type", "application/json")
      .end(new JsonObject().put("error", "Invalid or missing JSON body").encode());
    return;
  }

    body.put("deviceType",deviceType);
    body.put("deviceId",deviceId);

    dataStore.put(deviceId,body);

    // Debugging information
    System.out.println("Data stored in dataStore: " + body.encodePrettily());


//    System.out.println("Received POST data: " + body.encodePrettily());

  routingContext.response()
    .putHeader("content-type","application/json")
    .setStatusCode(201)
    .end(new JsonObject().put("message","Resource created successfully").encode());

  }

  private void handleGetRequest(RoutingContext routingContext) {
    String deviceId = routingContext.pathParam("id");

     System.out.println(deviceId);
     ;

     if(!dataStore.containsKey(deviceId))
     {
       routingContext.response()
         .setStatusCode(404)
         .putHeader("content-type","application/json")
         .end(new JsonObject().put("error","Resource not found").encode());
       return;
     }

     JsonObject resource = dataStore.get(deviceId);
      routingContext.response()
        .putHeader("content-type", "application/json")
        .end(resource.encode());
  }

  private void handlePutRequest(RoutingContext routingContext) {
    String deviceId = routingContext.pathParam("id");
    String deviceType = routingContext.pathParam("deviceType");

    JsonObject body = routingContext.body().asJsonObject();

    if(body==null)
    {
      routingContext.response()
        .setStatusCode(400)
        .putHeader("content-type","application/json")
        .end(new JsonObject().put("error","Invalid or missing json body").encode());
      return;
    }

    if(!dataStore.containsKey(deviceId))
    {
      routingContext.response()
        .setStatusCode(404)
        .putHeader("content-type","application.json")
        .end(new JsonObject().put("error","Resource not found").encode());
      return;
    }

    JsonObject existingResource = dataStore.get(deviceId);

    existingResource.mergeIn(body);

    existingResource.put("deviceType",deviceType);
    existingResource.put("deviceId",deviceId);

    dataStore.put(deviceId,existingResource);

    // Debugging information
    System.out.println("Updated data in dataStore: " + existingResource.encodePrettily());

    routingContext.response()
      .putHeader("content-type", "application/json")
      .setStatusCode(200)
      .end(new JsonObject().put("message","Resource updated successfully").put("updatedData",existingResource).encode());

  }

  private void handleDeleteRequest(RoutingContext routingContext) {
    String deviceId = routingContext.pathParam("id");

    if (!dataStore.containsKey(deviceId)) {
      routingContext.response()
        .setStatusCode(404)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", "Resource not found").encode());
      return;
    }

    dataStore.remove(deviceId);

    JsonObject response = new JsonObject()
      .put("message", "Resource deleted successfully")
      .put("id", deviceId);

    routingContext.response()
      .putHeader("content-type", "application/json")
      .end(response.encode());
  }
}


