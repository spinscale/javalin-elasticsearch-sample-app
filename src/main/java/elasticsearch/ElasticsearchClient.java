package elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import kotlin.text.Charsets;
import model.Person;
import model.SearchResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ElasticsearchClient {

    private static final String INDEX = "persons";

    private final HttpClient client;
    private final String endpoint;
    private final Map<String, String> headers;
    private final Renderer renderer;
    private final Parser parser;

    private ElasticsearchClient(Renderer renderer, Parser parser, String endpoint, Map<String, String> headers) {
        this.renderer = renderer;
        this.parser = parser;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.endpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length()-1) : endpoint;
        // map might be immutable, so create a new one
        this.headers = new HashMap<>(headers);
        this.headers.putIfAbsent("Content-Type", "application/json");
    }

    public SearchResponse search(String templateName, String query) throws IOException, InterruptedException {
        final String body = renderer.render(templateName, Map.of("query", query));
        final HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .uri(URI.create(endpoint + "/" + INDEX + "/_search"))
                .timeout(Duration.ofSeconds(10));
        headers.forEach((key, value) -> requestBuilder.setHeader(key, value));
        final HttpResponse<byte[]> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofByteArray());
        return parser.toSearchResponse(response.body());
    }

    public void index(Person person) throws IOException, InterruptedException {
        final HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(toJson(person)))
                .uri(URI.create(endpoint + "/" + INDEX + "/_doc/"))
                .timeout(Duration.ofSeconds(10));
        headers.forEach((key, value) -> requestBuilder.setHeader(key, value));

        final HttpResponse<byte[]> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofByteArray());
        // This is really bad error handling, you need to bubble the Elasticsearch client side exception up as well!
        if (response.statusCode() != 201) {
            throw new RuntimeException("Error indexing new person: " + response.statusCode());
        }
    }

    private String toJson(Person person) throws IOException {
        // unfortunately we cannot use records in JTE yet, because it is a preview feature
        // so we have serialize each getter into its own field
        return renderer.render("person", Map.of("firstName", person.firstName(), "lastName", person.lastName(), "employer", person.employer()));
        // if the above is fixed, or records are not a preview feature anymore, we can go with this instead and fix the template
        //return renderer.render("person", Map.of("person", person));
    }

    public static Builder newBuilder(Renderer renderer, Parser parser) {
        return new Builder(renderer, parser);
    }

    public static class Builder {

        private String authorizationHeader;
        private String uri;
        private final Renderer renderer;
        private final Parser parser;

        public Builder(Renderer renderer, Parser parser) {
            this.renderer = renderer;
            this.parser = parser;
        }

        // pretty much copied from
        // https://github.com/elastic/elasticsearch/blob/master/client/rest/src/main/java/org/elasticsearch/client/RestClient.java#L143-L177
        public Builder withCloudId(String cloudId) {
            // there is an optional first portion of the cloudId that is a human readable string, but it is not used.
            if (cloudId.contains(":")) {
                if (cloudId.indexOf(":") == cloudId.length() - 1) {
                    throw new IllegalStateException("cloudId " + cloudId + " must begin with a human readable identifier followed by a colon");
                }
                cloudId = cloudId.substring(cloudId.indexOf(":") + 1);
            }

            String decoded = new String(Base64.getDecoder().decode(cloudId), Charsets.UTF_8);
            // once decoded the parts are separated by a $ character.
            // they are respectively domain name and optional port, elasticsearch id, kibana id
            String[] decodedParts = decoded.split("\\$");
            if (decodedParts.length != 3) {
                throw new IllegalStateException("cloudId " + cloudId + " did not decode to a cluster identifier correctly");
            }

            // domain name and optional port
            String[] domainAndMaybePort = decodedParts[0].split(":", 2);
            String domain = domainAndMaybePort[0];
            int port;

            if (domainAndMaybePort.length == 2) {
                try {
                    port = Integer.parseInt(domainAndMaybePort[1]);
                } catch (NumberFormatException nfe) {
                    throw new IllegalStateException("cloudId " + cloudId + " does not contain a valid port number");
                }
            } else {
                port = 443;
            }

            String url = decodedParts[1]  + "." + domain;
            this.uri = "https://" + url + ":" + port;
            return this;
        }

        public Builder withUri(String uri) {
            this.uri = uri;
            return this;
        }

        public Builder withAuth(String username, String password) {
            String input = username + ":" + password;
            String value = Base64.getEncoder().encodeToString(input.getBytes(Charsets.UTF_8));
            this.authorizationHeader = "Basic " + value;
            return this;
        }

        public Builder withApiKey(String apiKey) {
            String value = Base64.getEncoder().encodeToString(apiKey.getBytes(Charsets.UTF_8));
            this.authorizationHeader = "ApiKey " + value;
            return this;
        }

        // build the client from the existing env vars with different priorities
        public Builder fromEnvironment() {
            final String cloudId = System.getenv("ELASTICSEARCH_CLOUD_ID");
            final String endpoint = System.getenv("ELASTICSEARCH_URL");
            if (cloudId != null) {
                withCloudId(cloudId);
            } else if (endpoint != null) {
                withUri(endpoint);
            } else {
                throw new RuntimeException("Missing Elasticsearch endpoint. Either configure ELASTICSEARCH_CLOUD_ID or ELASTICSEARCH_URL");
            }
            final String apiKey = System.getenv("ELASTICSEARCH_API_KEY");
            if (apiKey != null) {
                withApiKey(apiKey);
            } else {
                final String username = System.getenv("ELASTICSEARCH_USERNAME");
                final String password = System.getenv("ELASTICSEARCH_PASSWORD");
                if (username != null && password != null) {
                    withAuth(username, password);
                }
            }

            return this;
        }

        public ElasticsearchClient build() {
            Map<String, String> headers = authorizationHeader != null ? Map.of("Authorization", authorizationHeader) : Collections.emptyMap();
            return new ElasticsearchClient(renderer, parser, uri, headers);
        }
    }
}
