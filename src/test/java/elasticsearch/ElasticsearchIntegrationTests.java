package elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.Person;
import model.SearchResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@Tag("slow")
public class ElasticsearchIntegrationTests {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Renderer renderer = new Renderer(mapper);
    private static final Parser parser = new Parser(mapper);

    @Container
    private ElasticsearchContainer container = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:7.10.0");

    @Test
    public void testIndexAndSearch() throws Exception {
        final String endpoint = "http://" + container.getHttpHostAddress();
        ElasticsearchClient client = ElasticsearchClient.newBuilder(renderer, parser).withUri(endpoint).build();

        Person person = new Person("first", "last", "employer");
        client.index(person);

        refresh();

        SearchResponse response = client.search("search", "first");
        assertThat(response.hits()).hasSize(1);
        assertThat(response.hits().get(0).person()).isEqualTo(person);

        response = client.search("search", "non-existing");
        assertThat(response.hits()).isEmpty();
    }

    private void refresh() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        final String uri = "http://" + container.getHttpHostAddress() + "/persons/_refresh";

        final HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.noBody())
                .uri(URI.create(uri))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .build();

        client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }
}
