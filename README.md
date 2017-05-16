# GraphQL SPQR

GraphQL SPQR (GraphQL Schema Publisher & Query Resolver) is a simple to use library for rapid development of GraphQL APIs in Java.

[![Join the chat at https://gitter.im/leangen/Lobby](https://badges.gitter.im/leangen/Lobby.svg)](https://gitter.im/leangen/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.leangen.graphql/spqr/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.leangen.geantyref/geantyref)
[![Javadoc](https://javadoc-emblem.rhcloud.com/doc/io.leangen.graphql/spqr/badge.svg)](http://www.javadoc.io/doc/io.leangen.geantyref/geantyref)
[![Build Status](https://travis-ci.org/leangen/graphql-spqr.svg?branch=master)](https://travis-ci.org/leangen/graphql-spqr)
[![Hex.pm](https://img.shields.io/hexpm/l/plug.svg?maxAge=2592000)](https://raw.githubusercontent.com/leangen/graphql-spqr/master/LICENSE)
[![Semver](http://img.shields.io/SemVer/2.0.0.png)](http://semver.org/spec/v2.0.0.html)

## Example

The example will use annotations provided by GraphQL SPQR itself, but these are optional and the mapping is completely configurable,
enabling existing services to be exposed through GraphQL without modification.

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
public class User<T> implements Person {

    @GraphQLQuery(name = "name", description = "A person's name")
    public String name;

    @GraphQLQuery(name = "id", description = "A person's id")
    public Integer id;

    @GraphQLQuery(name = "regDate", description = "Date of registration")
    public Date registrationDate;

    @Override
    public String getName() {
        return name;
    }

    public Integer getId() {
        return id;
    }

    public Date getRegistrationDate() {
        return registrationDate;
    }
}
``` 

Exposing the service:

```java
UserService userService = new UserService(); //could also be injected by Spring or another framework
GraphQLSchemaGenerator schema = new GraphQLSchemaGenerator()
    .withOperationsFromSingleton(userService) //more services can be added the same way
    .generate();
GraphQL graphQL = new GraphQL(schema);

//keep the reference to GraphQL instance and execute queries against it.
//this operation selects a user by ID and requests name and regDate fields only
ExecutionResult result = graphQL.execute(   
    "{ user (id: 123) {
        name,
        regDate
    }}");
```
##Full example

See more complete examples using Spring MVC on [https://github.com/leangen/graphql-spqr-samples](https://github.com/leangen/graphql-spqr-samples)