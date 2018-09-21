package pl.superhero;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import io.vertx.core.Vertx;
import java.io.IOException;
import static java.lang.String.format;
import static org.apache.commons.lang3.Validate.notNull;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.bson.Document;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import org.junit.*;
import static org.junit.Assert.assertThat;
import static pl.superhero.TestUtils.jsonEntity;
import static pl.superhero.TestUtils.readObject;

public class CreateHeroTest {

  private static final String HERO_MONGO_URI_TEST = "HERO_MONGO_URI_TEST";
  private static final ObjectNode heroes = readObject(CreateHeroTest.class.getResource("heroes.json"));
  private static final String API_HOST = "localhost";
  private static final int API_PORT = 0; // pick random available

  private static MainVerticle mainVerticle;
  private static MongoDatabase mongo;
  private static Vertx vertx;

  @BeforeClass
  public static void beforeClass() throws Exception {
        String mongoUri = notNull(
            System.getenv(HERO_MONGO_URI_TEST),
            "System environment %s cannot be null",
            HERO_MONGO_URI_TEST);

    MongoClientURI uri = new MongoClientURI(mongoUri);
    mongo = new MongoClient(uri).getDatabase(uri.getDatabase());

    vertx = Vertx.vertx();
    mainVerticle = new MainVerticle(API_HOST, API_PORT);
    TestUtils.<String>waitForCompletion(r -> vertx.deployVerticle(mainVerticle, r));
  }

  @AfterClass
  public static void afterClass() throws Exception {
    TestUtils.<Void>waitForCompletion(vertx::close);
  }

  @Before
  public void before() {
    mongo.getCollection("heroes").drop();
  }

  @Test
  public void should_create_hero() throws IOException {

    JsonNode batman = heroes.get("Batman");
    HttpResponse response = Request.Post(format("http://localhost:%d/heroes", mainVerticle.getApiPort()))
        .body(jsonEntity(batman))
        .execute()
        .returnResponse();
    System.out.println(org.apache.commons.io.IOUtils.toString(response.getEntity().getContent()));
    assertThat(response.getStatusLine().getStatusCode(), is(201));

    Document document = mongo.getCollection("heroes").find().first();
    assertThat(readObject(document.toJson()).without("_id"), equalTo(batman));
  }

}
