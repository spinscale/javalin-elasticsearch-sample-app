package app;

import com.fasterxml.jackson.databind.ObjectMapper;
import elasticsearch.ElasticsearchClient;
import elasticsearch.Parser;
import elasticsearch.Renderer;
import io.javalin.Javalin;
import model.Person;
import model.SearchResponse;

public class App {

    public static void main(String[] args) {
        final ObjectMapper mapper = new ObjectMapper();
        final Renderer renderer = new Renderer(mapper);
        final Parser parser = new Parser(mapper);
        final ElasticsearchClient client = ElasticsearchClient.newBuilder(renderer, parser).fromEnvironment().build();

        Javalin app = Javalin.create().start(7000);

        final String result = "{\"healthy\":\"ok\"}";
        // usually you check for the reachability of the Elasticsearch instance
        app.get("/", ctx -> ctx.contentType("application/json").result(result));

        app.get("/search", ctx -> {
            final SearchResponse searchResponse = client.search("search", ctx.queryParam("q"));
            ctx.contentType("application/json").status(200).result(renderer.searchResponse(searchResponse));
        });

        app.post("/person", ctx -> {
            final Person person = parser.toPerson(ctx.bodyAsBytes());
            client.index(person);
            ctx.status(200);
        });
    }
}