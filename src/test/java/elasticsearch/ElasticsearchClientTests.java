package elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import model.Person;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.base.Charsets;

import java.net.Inet4Address;
import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ElasticsearchClientTests {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Renderer renderer = new Renderer(mapper);
    private static final Parser parser = new Parser(mapper);

    @Test
    public void testIndex() throws Exception {
        Person person = new Person("first", "last", "employer");
        String endpoint = "/persons/_doc/";
        HttpServer httpServer = createWebserver();
        try {
            httpServer.createContext(endpoint, exchange -> {
                final byte[] response = ("""
                        {
                          "_index" : "persons",
                          "_type" : "_doc",
                          "_id" : "V5Pz9HUBDDGl8mU3hTv7",
                          "_version" : 1,
                          "result" : "created",
                          "_shards" : {
                            "total" : 2,
                            "successful" : 1,
                            "failed" : 0
                          },
                          "_seq_no" : 9,
                          "_primary_term" : 4
                        }
                        """).getBytes(Charsets.UTF_8);
                exchange.sendResponseHeaders(201, response.length);
                exchange.getResponseBody().write(response);
            });

            // all good no, exceptions
            createClient(httpServer).index(person);
        } finally {
            httpServer.stop(0);
        }
    }

    @Test
    public void testIndexReturningError() throws Exception {
        final String endpoint = "/persons/_doc/";

        HttpServer httpServer = createWebserver();
        try {
            httpServer.createContext(endpoint, exchange -> {
                final byte[] response = ("{ \"error\" : { }, \"status\" : 400 }").getBytes(Charsets.UTF_8);
                exchange.sendResponseHeaders(400, response.length);
                exchange.getResponseBody().write(response);
            });

            ElasticsearchClient client = createClient(httpServer);
            Person person = new Person("first", "last", "employer");
            assertThatThrownBy(() -> client.index(person)).hasMessage("Error indexing new person: 400");
        } finally {
            httpServer.stop(0);
        }
    }

    private HttpServer createWebserver() throws Exception {
        HttpServer httpServer = HttpServer.create();
        // bind to random port to prevent conflicts
        httpServer.bind(new InetSocketAddress(Inet4Address.getLocalHost(), 0), 0);
        httpServer.start();
        return httpServer;
    }

    private ElasticsearchClient createClient(HttpServer httpServer) {
        final InetSocketAddress address = httpServer.getAddress();
        return ElasticsearchClient.newBuilder(renderer, parser).withUri("http://" + address.getHostName() + ":" + address.getPort()).build();
    }
}
