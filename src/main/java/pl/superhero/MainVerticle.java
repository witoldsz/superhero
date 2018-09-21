package pl.superhero;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import io.vertx.ext.web.Router;
import io.vertx.core.Future;
import static io.vertx.core.Future.future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.api.contract.RouterFactoryOptions;
import io.vertx.ext.web.handler.StaticHandler;
import java.util.List;
import pl.superhero.handlers.CreateHeroHandler;
import pl.superhero.handlers.ListHeroesHandler;
import pl.superhero.handlers.ShowHeroByIdHandler;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.Validate.notNull;

public class MainVerticle extends AbstractVerticle {

  private static final String HERO_MONGO_URI = "HERO_MONGO_URI";
  private static final String HERO_API_HOST = "HERO_API_HOST";
  private static final String HERO_API_PORT = "HERO_API_PORT";

  private final String apiHost;
  private final int apiPort;

  private HttpServer server;
  private MongoClient mongo;

  public MainVerticle(String apiHost, int apiPort) {
    this.apiHost = apiHost;
    this.apiPort = apiPort;
  }

  public MainVerticle() {
    this.apiPort = Integer.parseInt(System.getenv().getOrDefault(HERO_API_PORT, "8080"));
    this.apiHost = System.getenv().getOrDefault(HERO_API_HOST, "localhost");
  }

  public int getApiPort() {
    return server.actualPort();
  }

  @Override
  public void start(Future done) {
    System.setProperty("org.mongodb.async.type", "netty");
    String mongoUri = notNull(System.getenv(HERO_MONGO_URI), "System environment %s cannot be null", HERO_MONGO_URI);

    mongo = MongoClient.createShared(
        vertx,
        new JsonObject().put("connection_string", mongoUri)
    );

    // Just to make sure the connection works
    Future<List<String>> mongoF = future();
    mongo.getCollections(mongoF.completer());

    mongoF
      .compose(__ -> createRouterFactory())
      .compose(routerFactory -> {
        Router router = configureRouter(routerFactory);
        server = vertx.createHttpServer(new HttpServerOptions().setPort(apiPort).setHost(apiHost));
        server.requestHandler(router::accept).listen();
        done.complete();
      }, done);
  }

  @Override
  public void stop() {
    ofNullable(server).ifPresent(HttpServer::close);
    ofNullable(mongo).ifPresent(MongoClient::close);
  }

  private Future<OpenAPI3RouterFactory> createRouterFactory() {
    Future<OpenAPI3RouterFactory> routerFactoryF = future();
    OpenAPI3RouterFactory.create(this.vertx, "webroot/superheroes.yaml", routerFactoryF.completer());
    return routerFactoryF;
  }

  private Router configureRouter(OpenAPI3RouterFactory routerFactory) {
    routerFactory.setOptions(new RouterFactoryOptions()
      .setMountNotImplementedHandler(true)
      .setMountValidationFailureHandler(true));

    // Add routes handlers
    routerFactory.addHandlerByOperationId("listHeroes", new ListHeroesHandler(mongo));
    routerFactory.addHandlerByOperationId("createHero", new CreateHeroHandler(mongo));
    routerFactory.addHandlerByOperationId("showHeroById", new ShowHeroByIdHandler(mongo));

    Router router = routerFactory.getRouter();

    int oneMinute = 60;
    router.route("/*").handler(StaticHandler.create().setMaxAgeSeconds(oneMinute));
    return router;

  }

}
