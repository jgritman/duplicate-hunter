## Duplicate Hunter

This is an example of a REST-ful API Service which allows CRUD operations on customer objects. It then flags potential duplicates in the database using the [Duke deduplication engine](https://github.com/larsga/Duke).

### Running

An embedded Tomcat server running the application can be launched using the included Gradle wrapper script:

```
./gradlew run
```

This only requires Java 6 or higher to be installed the machine. Gradle will download itself and the projects dependencies automatically. The server uses an in memory H2 Database and Lucene engine, so any customer records disappear on application shutdown.

The server will be running at [http://localhost:8080](http://localhost:8080) after invoking the run script.

### Endpoints

There are `GET`, `POST`, `PUT`, and `DELETE` operations available from the `/customers` endpoint. The fields available on the customer model are:

```
{
  "name": "",
  "email": "",
  "address1": "",
  "address2": "",
  "city": "",
  "state": "",
  "zip": ""
}
```

Additionally there's a `GET /potentialDuplicates` endpoint returning pairs of customers that exceeded the threshold for potential match.

A [Swagger specification](https://github.com/wordnik/swagger-spec) for the API is available from [http://localhost:8080/api-docs](http://localhost:8080/api-docs).

### Design

The server itself is built using [Spring Boot](http://projects.spring.io/spring-boot/), which offers a way to bring up a Spring MVC/JPA app with minimal coding. This allowed me to start testing the deduplication logic quickly, though this could have just as easily been built with Play 2, Grails, or raw Servlets.

As for the deduplication engine, my idea from the start was to compute the similars after each operation and store the result in the database, rather than calculate it on the fly for each call to `/potentialDuplicates`. However, even one time calculation might get rather expensive quickly, so the problem demanded some kind of index based solution that could handle non-exact matches.

I spent a lot of time working on how to configure Lucene to do the the right kind of fuzzy searches on my own. It's very difficult to find help for this problem through Google, due to the numerous terms that describe the problem. Also, much of the work done in this area is behind academic paywalls.

However, in the course my exploration, I came across Duke. It already used Lucene, and had a very sophisticated configuration based approach. You can change the method used to match each piece of data (exact matching, Levenshtein, etc.) and assign a weight to each field. Based on the documentation, the authors of the framework put a lot of thought into performance. While I haven't put the framework to the test against a large production dataset, I'm fairly confident the extensibility is there if bottlenecks are found.

### Production Considerations

Besides using a real database and disk backed Lucene index, I would recommend moving to some type of asynchronous invocation of the deduplication process. This could be done by placing customer modification notifications on a message queue, so another server could pick those check for duplicates. Alternately, the deduplication process could run on a schedule, and pick up any records with an updated/deleted date after the last run.

### Testing

There's a [Spock](https://code.google.com/p/spock/) specification that tests out the basics of duplication detection. This integration test goes through the controller and loads up the entire Spring context.

The tests can be invoked using Gradle:

```
./gradlew test
```
