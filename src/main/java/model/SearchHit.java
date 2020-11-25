package model;

public record SearchHit(String index, String id, float score, Person person) {
}
