package model;

import java.util.List;

public record SearchResponse(List<SearchHit> hits) {
}
