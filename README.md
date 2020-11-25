# Using the built-in Java HTTP client to connect to Elasticsearch

This repository features a sample Javalin web application, that does not use
the High Level Rest Client, but rather the built-in Java HTTP client, that is
GA since Java 11.

You can build the application running

```
./gradlew clean check shadowJar
```

Then use java (version 15 and above) to run the uber jar like this:

```
java --enable-preview -jar build/libs/javalin-elasticsearch-client-0.1.0-SNAPSHOT-all.jar
```

In order to properly configure the connection to your Elasticsearch instance,
take a look at the corresponding blog post at

https://spinscale.de/posts/2020-11-25-using-the-built-in-java-http-client-to-query-elasticsearch.html

