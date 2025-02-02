package com.example.starter;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;


public class MainVerticle extends AbstractVerticle {

  private PgPool pool;


  public static void main(String[] args) {
    Vertx v = Vertx.vertx();
    v.deployVerticle(new MainVerticle());

  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    Router router = Router.router(vertx);

    PgConnectOptions connectOptions =
      new PgConnectOptions()
        .setPort(5432)
        .setHost("localhost")
        .setDatabase("mydb")
        .setUser("myuser")
        .setPassword("1234");

    PoolOptions poolOptions = new PoolOptions().setMaxSize(5);

    pool = PgPool.pool(vertx, connectOptions, poolOptions);

    router.route().handler(BodyHandler.create());
    router.get("/api/:deviceType/:id").handler(this::handleGetRequest);
    router.post("/api/:deviceType/:id").handler(this::handlePostRequest);
    router.put("/api/:deviceType/:id").handler(this::handlePutRequest);
    router.delete("/api/:deviceType/:id").handler(this::handleDeleteRequest);

    int port = config().getJsonObject("http", new JsonObject()).getInteger("port", 8080);

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(port)
      .onComplete(http -> {
        if (http.succeeded()) {
          startPromise.complete();
          System.out.println("HTTP server started on port " + port);
        } else {
          startPromise.fail(http.cause());
        }
      });
  }

   private void handlePostRequest(RoutingContext routingContext) {

    Promise<JsonObject> dbpromise = Promise.promise();
    JsonObject body = routingContext.body().asJsonObject();
    if (body == null) {
      routingContext.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", "Invalid or missing JSON body").encode());
      return;
    }

    String query = " INSERT INTO devices (deviceId, domain, state, city, location, deviceType) VALUES ($1, $2, $3, $4, $5::jsonb, $6) ";

    JsonObject location = body.getJsonObject("location");
    String encLocation = location.encode();

    pool.preparedQuery(query)
      .execute(Tuple.of(
        body.getString("deviceId"),
        body.getString("domain"),
        body.getString("state"),
        body.getString("city"),
        encLocation,
        body.getString("deviceType")
      ), ar -> {
        if (ar.succeeded()) {
          JsonObject response=new JsonObject().put("message", "Resource created successfully");
          dbpromise.complete(response);
        } else {
          System.out.println("Database Insert Error: " + ar.cause().getMessage());
          dbpromise.fail(new Exception("Failed to insert resource"));
        }
      });

    dbpromise.future().onComplete(ar->{
      if(ar.succeeded())
      {
        routingContext.response()
          .setStatusCode(201)
          .putHeader("content-type", "application/json")
          .end(ar.result().encode());
    } else {
      // Failure: send the error response
      routingContext.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", ar.cause().getMessage()).encode());
    }
    });

  }

  private void handleGetRequest(RoutingContext routingContext) {
    Promise<JsonObject>dbpromise = Promise.promise();

    String deviceId = routingContext.pathParam("id");

    String query = "SELECT deviceId, domain, state, city, location, deviceType FROM devices WHERE deviceId = $1";
    pool
      .preparedQuery(query)
      .execute(Tuple.of(deviceId), ar -> {
        if (ar.succeeded() && ar.result().size() > 0) {
          Row row = ar.result().iterator().next();

          JsonObject response = new JsonObject();
          for(int i=0;i<row.size();i++)
          {
            String columnName = row.getColumnName(i);
//            System.out.println("GET: columnName "+columnName);
            Object columnValue = row.getValue(i);
            response.put(columnName,columnValue);
          }

          response.put("message","Resource found in database");
          dbpromise.complete(response);
        } else {
          dbpromise.fail(new Exception("Resource not found!"));
        }
      });

    dbpromise.future().onComplete(ar ->
    {
      if(ar.succeeded())
      {
        routingContext.response()
          .setStatusCode(201)
          .putHeader("content-type", "application/json")
          .end(ar.result().encode());
      }
      else {
        routingContext.response()
          .setStatusCode(500)
          .putHeader("content-type", "application/json")
          .end(new JsonObject().put("error", ar.cause().getMessage()).encode());
      }
    });

  }

   private void handlePutRequest(RoutingContext routingContext) {
    String deviceId = routingContext.pathParam("id");
    JsonObject body = routingContext.body().asJsonObject();

    Promise<JsonObject>dbpromise = Promise.promise();

    if (body == null) {
      routingContext.response()
        .setStatusCode(404)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", "Resource not found").encode());
      return;
    }

    String query = "UPDATE devices SET domain=$1, state=$2, city=$3, location=$4::jsonb, deviceType=$5 WHERE deviceId=$6";

    JsonObject location = body.getJsonObject("location");
    String enclocation = location.encode();

    pool.preparedQuery(query)
      .execute(Tuple.of(
        body.getString("domain"),
        body.getString("state"),
        body.getString("city"),
        enclocation,
        body.getString("deviceType"),
        deviceId
        ),ar -> {
          if (ar.succeeded()) {
            if (ar.result().rowCount() > 0) {
              JsonObject response = new JsonObject();
              response.put("message", "Resource updated in database!");
              dbpromise.complete(response);
            } else {
              dbpromise.fail(new Exception("Resource not updated !"));
            }
          }
        });

    dbpromise.future().onComplete(ar->{
      if(ar.succeeded())
      {
        routingContext.response()
          .setStatusCode(200)
          .putHeader("content-type","application/json")
          .end(ar.result().encode());
      }
      else
      {
        routingContext.response()
          .setStatusCode(500)
          .putHeader("content-type", "application/json")
          .end(new JsonObject().put("error", ar.cause().getMessage()).encode());
      }
    });


  }

   private void handleDeleteRequest(RoutingContext routingContext) {
    String deviceId = routingContext.pathParam("id");

    String query = "DELETE FROM devices WHERE deviceId=$1";

    Promise<JsonObject> dbpromise = Promise.promise();

    pool.preparedQuery(query)
      .execute(Tuple.of(deviceId),ar-> {
        if(ar.succeeded()) {
          if (ar.result().rowCount() > 0) {
            JsonObject response = new JsonObject();
            response.put("message", "Resource deleted in database!");
            dbpromise.complete(response);
          }} else {
            dbpromise.fail(new Exception("Resource not found"));
          }
        });

    dbpromise.future().onComplete(ar->{
      if(ar.succeeded())
      {
          routingContext.response()
            .putHeader("content-type", "application/json")
            .end(ar.result().encode());
      } else
      {
            // Internal server error
            routingContext.response()
              .setStatusCode(500)
              .putHeader("content-type", "application/json")
              .end(new JsonObject().put("error", "Failed to delete resource").encode());
      }
    });

  }

}
