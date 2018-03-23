# GraphQL SPQR

> GraphQL SPQR (GraphQL Schema Publisher & Query Resolver, pronounced like _speaker_) is a simple-to-use library for rapid development of GraphQL APIs in Java.

[![Join the chat at https://gitter.im/leangen/graphql-spqr](https://badges.gitter.im/leangen/graphql-spqr.svg)](https://gitter.im/leangen/graphql-spqr?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.leangen.graphql/spqr/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.leangen.graphql/spqr)
[![Javadoc](http://javadoc-badge.appspot.com/io.leangen.graphql/spqr.svg?label=javadoc)](http://www.javadoc.io/doc/io.leangen.graphql/spqr)
[![Build Status](https://travis-ci.org/leangen/graphql-spqr.svg?branch=master)](https://travis-ci.org/leangen/graphql-spqr)
[![Hex.pm](https://img.shields.io/hexpm/l/plug.svg?maxAge=2592000)](https://raw.githubusercontent.com/leangen/graphql-spqr/master/LICENSE)
[![Semver](http://img.shields.io/SemVer/2.0.0.png)](http://semver.org/spec/v2.0.0.html)

## Intro

GraphQL SPQR aims to make it dead simple to add a GraphQL API to _any_ Java project. It works by dynamically generating a GraphQL schema from Java code.

* Requires minimal setup (~3 lines of code)
* Deeply configurable and extensible (not opinionated)
* Allows rapid prototyping and iteration (no boilerplate)
* Easily used in legacy projects with no changes to the existing code base
* Has very few dependencies

## Known issues with Kotlin

Due to a 2 year old [bug](https://youtrack.jetbrains.com/oauth?state=%2Fissue%2FKT-13228), Kotlin properties produce incorrect `AnnotatedType`s on which most of SPQR is based. The most obvious implication is that `@GraphQlNonNull` annotation won't work when used on properties.
There's nothing that can be done about this from our side so, for the time being, **Kotlin support is a non-goal of this project** but we will try to be compaitble where possible.

## Code-first approach

When developing GraphQL-enabled applications it is common to define the schema first and hook up the business logic later. This is known as the schema-first style. While it has its advantages, in strongly and statically typed languages, like Java, it leads to a lot of duplication.

For example, a schema definition of a simple GraphQL type could like this:

```graphql
type Link {
    id: ID!
    url: String!
    description: String
}
```

and, commonly, a corresponding Java type would exist in the system, similar to the following:

```java
public class Link {
    
    private final String id;
    private final String url;
    private final String description;
    
    //constructors, getters and setters
    //...
}
```

Both of these blocks contain the exact same information. Worse yet, changing one requires an immediate change to the other. This makes refactoring risky and cumbersome. On the other hand, if you’re trying to introduce a GraphQL API into an existing project, writing the schema practically means re-describing the entire existing model. This is both expensive and error-prone, and still suffers from duplication.

Instead, GraphQL SPQR takes the code-first approach, by generating the schema from the existing model. This keeps the schema and the model in sync, easing refactoring. It also works well in projects where GraphQL is introduced on top of an existing codebase.

## Installation

GraphQL SPQR is deployed to [Maven Central](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22io.leangen.graphql%22%20AND%20a%3A%22spqr%22).

Maven

```xml
<dependency>
    <groupId>io.leangen.graphql</groupId>
    <artifactId>spqr</artifactId>
    <version>0.9.6</version>
</dependency>
```

Gradle

```groovy
compile 'io.leangen.graphql:spqr:0.9.6'
```

## Hello world

The example will use annotations provided by GraphQL SPQR itself, but these are optional and the mapping is completely configurable, enabling existing services to be exposed through GraphQL without modification.

Service class:

```java
class UserService {

    @GraphQLQuery(name = "user")
    public User getById(@GraphQLArgument(name = "id") Integer id) {
      ...
    }
}
```

Domain class:

```java
public class User {

    private String name;
    private Integer id;
    private Date registrationDate;

    @GraphQLQuery(name = "name", description = "A person's name")
    public String getName() {
        return name;
    }

    @GraphQLQuery(name = "id", description = "A person's id")
    public Integer getId() {
        return id;
    }

    @GraphQLQuery(name = "regDate", description = "Date of registration")
    public Date getRegistrationDate() {
        return registrationDate;
    }
}
```

Exposing the service with graphql-spqr:

```java
UserService userService = new UserService(); //instantiate the service (or inject by Spring or another framework)
GraphQLSchemaGenerator schema = new GraphQLSchemaGenerator()
    .withOperationsFromSingleton(userService) //register the service
    .generate(); //done ;)
GraphQL graphQL = new GraphQL(schema);

//keep the reference to GraphQL instance and execute queries against it.
//this operation selects a user by ID and requests name and regDate fields only
ExecutionResult result = graphQL.execute(   
    "{ user (id: 123) {
        name,
        regDate
    }}");
```

## Full Tutorial

_Work in progress_

## Full example

See more complete examples using Spring Boot at https://github.com/leangen/graphql-spqr-samples
