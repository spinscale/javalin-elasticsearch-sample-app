package elasticsearch;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.Person;
import model.SearchHit;
import model.SearchResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A class to parse HTTP response byte arrays to POJOs
 * using Jacksons ObjectMapper
 */
public class Parser {

    private final ObjectMapper mapper;

    public Parser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    private static final JsonPointer hitsArray = JsonPointer.compile("/hits/hits");
    private static final JsonPointer hitsTotalValue = JsonPointer.compile("/hits/total/value");
    private static final JsonPointer hitSource = JsonPointer.compile("/_source");

    SearchResponse toSearchResponse(byte[] data) throws IOException {
        final JsonNode node = mapper.readTree(data);
        boolean hasHits = node.at(hitsTotalValue).longValue() > 0;
        if (hasHits) {
            final JsonNode hits = node.at(hitsArray);
            List<SearchHit> searchHits = new ArrayList<>(hits.size());
            hits.forEach(hit -> {
                Person person = parsePerson(hit.at(hitSource));
                searchHits.add(new SearchHit(hit.get("_index").asText(), hit.get("_id").asText(), hit.get("_score").floatValue(), person));
            });
            return new SearchResponse(searchHits);
        }

        return new SearchResponse(Collections.emptyList());
    }

    public Person toPerson(byte[] data) throws IOException {
        final JsonNode node = mapper.readTree(data);
        return parsePerson(node);
    }

    private static final JsonPointer pointerFirstName = JsonPointer.compile("/name/first");
    private static final JsonPointer pointerLastName = JsonPointer.compile("/name/last");
    private static final JsonPointer pointerEmployer = JsonPointer.compile("/employer");

    private Person parsePerson(JsonNode node) {
        final String firstName = node.at(pointerFirstName).asText();
        final String lastName = node.at(pointerLastName).asText();
        final String employer = node.at(pointerEmployer).asText();
        return new Person(firstName, lastName, employer);
    }
}
