package elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import model.Person;
import model.SearchResponse;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.base.Charsets;

import static org.assertj.core.api.Assertions.assertThat;

public class ParserTests {

    private static final Parser parser = new Parser(new ObjectMapper());

    @Test
    public void testSearchResponseParsing() throws Exception {
        final byte[] data = sampleSearchResponse();
        final SearchResponse response = parser.toSearchResponse(data);
        assertThat(response.hits()).hasSize(2);

        assertThat(response.hits().get(0).id()).isEqualTo("first");
        assertThat(response.hits().get(0).index()).isEqualTo("foo");
        Person firstPerson = new Person("first", "last", "Elastic");
        assertThat(response.hits().get(0).person()).isEqualTo(firstPerson);

        assertThat(response.hits().get(1).id()).isEqualTo("second");
        assertThat(response.hits().get(1).index()).isEqualTo("bar");
        Person secondPerson = new Person("2nd", "2nd last", "2nd Elastic");
        assertThat(response.hits().get(1).person()).isEqualTo(secondPerson);
    }

    private static byte[] sampleSearchResponse() {
        return """
                {
                  "took" : 754,
                  "timed_out" : false,
                  "_shards" : {
                    "total" : 1,
                    "successful" : 1,
                    "skipped" : 0,
                    "failed" : 0
                  },
                  "hits" : {
                    "total" : {
                      "value" : 2,
                      "relation" : "eq"
                    },
                    "max_score" : 1.0,
                    "hits" : [
                      {
                        "_index" : "foo",
                        "_type" : "_doc",
                        "_id" : "first",
                        "_score" : 1.0,
                        "_source" : {
                          "name" : { "first": "first", "last":"last" },
                          "employer": "Elastic"\s
                        }
                      },
                      {
                        "_index" : "bar",
                        "_type" : "_doc",
                        "_id" : "second",
                        "_score" : 1.0,
                        "_source" : {
                          "name" : { "first": "2nd", "last":"2nd last" },
                          "employer": "2nd Elastic"\s
                        }
                      }
                    ]
                  }
                }""".getBytes(Charsets.UTF_8);
    }
}
