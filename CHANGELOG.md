# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.9.8] - 2018-08-19
### Added
- Underlying `AnnotatedType` now accessible from the produced `GraphQLType` #139
- Underlying `Operation` now accessible from the produced `GraphQLFieldDefinition` #139
- String interpolation support (i18n / l10n) #162
- Jackson & Gson types (e.g. `ObjectNode`, `JsonObject` etc) are now fully
  supported out-of-the-box. #122
- Ability to use `Connections` in non spec-compliant queries #152
- `DefaultValueProvider` can now declare a constructor accepting `GlobalEnvironment`
- It is now possible to explicitly treat chosen types as equal for the purposes
  of name collision detection #124 #157
- Ability to register multiple `InputFieldBuilder`s #147
- Now using ClassGraph for implementation type auto discovery, which fixes numerous
  problems with fat JARs, class format compatibility etc #111 #121 #126 #161
- Input object fields can now have default values #131
- Existing Gson instance can now be used for configuration #132
- Ability to customize deprecation reason without annotations #133
- Ability to provide custom logic for implementation type auto discovery #135
- Support for `Iterable`
- Support for `java.sql.Date/Time/Timestamp` #136

### Changed
- [Breaking] `TypeMapper` interface changed to better support recursive use-cases #155
- Java primitives are now non-nullable by default #156
- [Breaking] `InputFieldDiscoveryStrategy` renamed to `InputFieldBuilder` #147
- [Breaking] `InputFieldBuilder#getInputFields` now receives `InputFieldBuilderParams`
- [Breaking] `ResolverBuilder` interface changed :
  `ResolverBuilder#buildQueryResolvers/buildMutationResolvers/buildSubscriptionResolvers` now receive `ResolverBuilderParams`
- [Breaking] `ResolverArgumentBuilder` interface changed:
  `ResolverArgumentBuilder#buildResolverArguments` now receives `ArgumentBuilderParams`
- [Breaking] `ArgumentInjector` interface changed :
  `ArgumentInjector#getArgumentValue` now receives `ArgumentInjectorParams` #158
- [Breaking] `ArgumentInjector#supports` now receives the related `Parameter` in addition to its `AnnotatedType`
- [Breaking] `OperationNameGenerator` interface changed :
  `OperationNameGenerator#generateQueryName/generateMutationName/generateSubscriptionName` now receive `OperationNameGeneratorParams`
- [Breaking] All provided `OperationNameGenerator`s redesigned for reusability:
  the new `DefaultOperationNameGenerator` should be applicable to most usages
- `MapToListTypeAdapter` now produces non-nullable entries #145
- [Breaking] `GsonValueMapperFactory.Configurer` interface changed:
  `GsonValueMapperFactory.Configurer#configure` now receives `ConfigurerParams`
- [Breaking] `JacksonValueMapperFactory.Configurer` interface changed:
  `JacksonValueMapperFactory.Configurer#configure` now receives `ConfigurerParams`
- [Breaking] Abstract input types with a single concrete implementation no longer have a type discriminator field
- All dependencies upgraded, most notably graphql-java in now on 9.2
- Generator methods (`withXXX`) have more intuitive behavior #123
- Default deprecation reason changed to "Deprecated" for better GraphiQL compatibility

### Deprecated
- `ClassUtils#findImplementations` is superseded by `ClassFinder` #135

### Removed
- `BeanOperationNameGenerator` removed. Superseded by `PropertyOperationNameGenerator`.
- `MethodOperationNameGenerator` removed. Superseded by `MemberOperationNameGenerator`.
- `ReturnTypeOperationNameGenerator` removed, as it served no purpose.
- `DelegatingOperationNameGenerator` removed. Superseded by `DefaultOperationNameGenerator`.

### Fixed
- Fixed `ScalarMap` deserialization error. `ObjectScalarAdapter` is no longer an `OutputConverter`. #106 #112
- Fixed type name collision going unnoticed when an input and an output type share a name #151
- Fixed generic type discovery for static inner classes
- Fixed double-quoting of IDs #134
