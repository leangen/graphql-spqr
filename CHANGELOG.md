# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [0.12.1] - 2022-12-18
### Changed
- [Breaking] Upgraded to graphql-java v20.0 [#437](https://github.com/leangen/graphql-spqr/issues/437)

## [0.12.0] - 2022-11-28
### Changed
- [Breaking] Upgraded to graphql-java v19.2 [#433](https://github.com/leangen/graphql-spqr/issues/433)
- [Breaking] Replace batching strategy with `DataLoader` [#432](https://github.com/leangen/graphql-spqr/issues/432)
- [Breaking] Remove custom `TypeResolutionEnvironment` (replaced with constructor-injected `GlobalEnvironment`)
- [Breaking] Remove implicit mapping directives [#435](https://github.com/leangen/graphql-spqr/issues/435)
- ### Fixed
- Applied directives are now correctly defined in the schema (and will be present in the SDL)

## [0.11.2] - 2021-03-21
### Added
- Make it easy to customize how `Executable`s are created [#383](https://github.com/leangen/graphql-spqr/issues/383)
### Changed
- Improve `GraphQLSchemaGenerator` API and JavaDoc [#384](https://github.com/leangen/graphql-spqr/issues/384)
### Fixed
- Support for `@JsonTypeInfo` [#353](https://github.com/leangen/graphql-spqr/issues/353)
- [Breaking] Bring `MapToListTypeAdapter` in line with the other adapters [#382](https://github.com/leangen/graphql-spqr/issues/382)

## [0.11.1] - 2021-02-09
### Changed
- Upgraded to graphql-java v16.2
### Fixed
- Complexity analysis broken with multiple named fragments [#379](https://github.com/leangen/graphql-spqr/issues/379)

## [0.11.0] - 2021-01-31
### Added
- Add `GraphQLError`s to the response via `ResolutionEnvironment` [#346](https://github.com/leangen/graphql-spqr/issues/346)
### Changed
- [Breaking] Upgraded to graphql-java v16.1 [#373](https://github.com/leangen/graphql-spqr/issues/373)
- [Breaking] Distinguish between no default value and null default value [#374](https://github.com/leangen/graphql-spqr/issues/374)

## [0.10.1] - 2019-12-30
### Added
- Easy hierarchical names for nested types: `DefaultTypeInfoGenerator#withHierarchicalNames` [#310](https://github.com/leangen/graphql-spqr/issues/310)
- Reintroduce `fieldOrder` and add `inputFieldOrder` [#279](https://github.com/leangen/graphql-spqr/issues/279)
- Make it easier to apply a `ResolverBuilder` to specific types only [#277](https://github.com/leangen/graphql-spqr/issues/277)
- Make it easy to filter interfaces in `InterfaceMappingStrategy` [#299](https://github.com/leangen/graphql-spqr/issues/299)
- Support `DataFetcherResult` even when not declared [#318](https://github.com/leangen/graphql-spqr/issues/318)
- Add `reason` field to `@GraphQLIgnore` annotation

### Changed
- Relax the mapping rules for abstract inputs (makes it easy to use e.g. Immutables and AutoValue) [#293](https://github.com/leangen/graphql-spqr/issues/293) [#245](https://github.com/leangen/graphql-spqr/issues/245)
- Don't scan for implementations of concrete input types by default [#332](https://github.com/leangen/graphql-spqr/issues/332)
- Collect transitive interfaces for object types [#282](https://github.com/leangen/graphql-spqr/issues/282)
- Relax name collision detection: only consider `io.leangen` annotations [#232](https://github.com/leangen/graphql-spqr/issues/232)
- Upgraded to graphql-java v13
  
### Fixed
- [Breaking] Filtering of fields/methods/parameters now happens _before_ type resolution [#298](https://github.com/leangen/graphql-spqr/issues/298)
- Complexity calculation for multi-root queries [#313](https://github.com/leangen/graphql-spqr/issues/313)
- `ResolverInterceptor`s must see real underlying exceptions [#314](https://github.com/leangen/graphql-spqr/issues/314)
- `@GraphQLIgnore` should affect auto discovery only if applied directly [#288](https://github.com/leangen/graphql-spqr/issues/288)

## [0.10.0] - 2019-05-24
### Added
- Take annotations on private fields into account (Lombok compatibility) [#160](https://github.com/leangen/graphql-spqr/issues/160)
- Optimize converter performance [#250](https://github.com/leangen/graphql-spqr/issues/250)
- Use the `_mappedInputField` directive to attach meta data to input fields [#216](https://github.com/leangen/graphql-spqr/issues/216)
- Generator should support accepting a bean supplier (makes prototype/dependent scoped beans easier to use) [#254](https://github.com/leangen/graphql-spqr/issues/254)
- Ability to register additional Java types (used as additional interface implementations or possible union types) [#208](https://github.com/leangen/graphql-spqr/issues/208)
- Support for `OffsetTime` [#260](https://github.com/leangen/graphql-spqr/issues/260)

### Changed
- [Breaking] Upgraded to graphql-java v12 [#252](https://github.com/leangen/graphql-spqr/issues/252)
- [Breaking] Field sorting no longer possible (due to graphql-java v12 upgrade)
- Jackson and ClassGraph versions upgraded [#226](https://github.com/leangen/graphql-spqr/issues/226) [#227](https://github.com/leangen/graphql-spqr/issues/227)
- Small improvements [a663162](https://github.com/leangen/graphql-spqr/commit/a663162430add139fbb706f2973cbea8e9d0df78)
- `JavaDeprecationMappingConfig` accessible from `GeneratorConfiguration` [4aa8c5f](https://github.com/leangen/graphql-spqr/commit/4aa8c5fc539ef3b2837349df00ea318271e8f6cb) [7a34178](https://github.com/leangen/graphql-spqr/commit/7a341788cc2501232c77336bdb969f4c3fc551ef)
- [Breaking] `java.sql.Timestamp` scalar (de)serializes to/from UTC timezone and not local time [#251](https://github.com/leangen/graphql-spqr/issues/251)

### Fixed
- Generate valid names for array types [#217](https://github.com/leangen/graphql-spqr/issues/217)
- Generate GraphQL type names for generic Java types correctly [#255](https://github.com/leangen/graphql-spqr/issues/255)
- Resolve type references discovered in subscriptions and mutations [#238](https://github.com/leangen/graphql-spqr/issues/238)
- Duplicate possible types in unions cause `TypeMappingException` and not `IndexOutOfBoundsException` [#256](https://github.com/leangen/graphql-spqr/issues/256)
- Adding custom directives can cause `NullPointerException` or `IllegalStateException` [#258](https://github.com/leangen/graphql-spqr/issues/258)

## [0.9.9] - 2018-12-13
### Added
- Introduced full support for both schema and client directives [#200](https://github.com/leangen/graphql-spqr/issues/200)
- Introduced [`SchemaTransformer`](https://github.com/leangen/graphql-spqr/blob/master/src/main/java/io/leangen/graphql/generator/mapping/SchemaTransformer.java) to enable modifying field and argument definitions
- Introduced [`ResolverInterceptor`](https://github.com/leangen/graphql-spqr/blob/master/src/main/java/io/leangen/graphql/execution/ResolverInterceptor.java) that can perform arbitrary logic around the invocation of the underlying method/field [#180](https://github.com/leangen/graphql-spqr/issues/180) [#92](https://github.com/leangen/graphql-spqr/issues/92)
- Added a way to get all deserialized arguments `ResolutionEnvironment` [#174](https://github.com/leangen/graphql-spqr/issues/174)
- `ArgumentInjectorParams#isPresent` to distinguish between the explicitly provided null input value vs no value provided [#197](https://github.com/leangen/graphql-spqr/issues/197)

### Changed
- Upgraded to [graphql-java 11.0](https://github.com/graphql-java/graphql-java/releases/tag/v11.0)
- Significantly improved the performance of converter selection [#194](https://github.com/leangen/graphql-spqr/issues/194)
- `Optional` arguments will be injected with `Optional.empty` if `null` is explicitly provided, and `null` is no value was given [#197](https://github.com/leangen/graphql-spqr/issues/197)
- All exceptions thrown during field resolution now bubble up unchanged (no longer wrapped in `RuntimeException`)
- Try loading implementation classes using the parent class' loader first [#177](https://github.com/leangen/graphql-spqr/issues/177)
- Renamed `withTypeAliasGroup` to `withTypeSynonymGroup`

### Fixed
- Fixed interface type resolution logic [#168](https://github.com/leangen/graphql-spqr/issues/168)
- Arguments with default values will no longer be mapped as non-null [#163](https://github.com/leangen/graphql-spqr/issues/163)
- `javax.annotation.Nonnull` works again [#165](https://github.com/leangen/graphql-spqr/issues/165)
- Fixed parsing of object/json scalar literals that contain variables

## [0.9.8] - 2018-08-19
### Added
- Underlying `AnnotatedType` now accessible from the produced `GraphQLType` [#139](https://github.com/leangen/graphql-spqr/issues/139)
- Underlying `Operation` now accessible from the produced `GraphQLFieldDefinition` [#139](https://github.com/leangen/graphql-spqr/issues/139)
- String interpolation support (i18n / l10n) [#162](https://github.com/leangen/graphql-spqr/issues/162)
- Jackson & Gson types (e.g. `ObjectNode`, `JsonObject` etc) are now fully
  supported out-of-the-box. [#122](https://github.com/leangen/graphql-spqr/issues/122)
- Ability to use `Connection`s (`Page`) in non spec-compliant queries [#152](https://github.com/leangen/graphql-spqr/issues/152)
- `DefaultValueProvider` can now declare a constructor accepting `GlobalEnvironment`
- It is now possible to explicitly treat chosen types as equal for the purposes
  of name collision detection [#124](https://github.com/leangen/graphql-spqr/issues/124) [#157](https://github.com/leangen/graphql-spqr/issues/157)
- Ability to register multiple `InputFieldBuilder`s [#147](https://github.com/leangen/graphql-spqr/issues/147)
- Now using ClassGraph for implementation type auto discovery, bringing huge improvements in stability and performance [#126](https://github.com/leangen/graphql-spqr/issues/126) [#161](https://github.com/leangen/graphql-spqr/issues/161)
- Input object fields can now have default values [#131](https://github.com/leangen/graphql-spqr/issues/131)
- Existing Gson instance can now be used for configuration [#132](https://github.com/leangen/graphql-spqr/issues/132)
- Ability to customize deprecation reason without annotations [#133](https://github.com/leangen/graphql-spqr/issues/133)
- Ability to provide custom logic for implementation type auto discovery [#135](https://github.com/leangen/graphql-spqr/issues/135)
- Support for `Iterable`
- Support for `java.sql.Date/Time/Timestamp` [#136](https://github.com/leangen/graphql-spqr/issues/136)

### Changed
- [Breaking] `TypeMapper` interface changed to better support recursive use-cases [#155](https://github.com/leangen/graphql-spqr/issues/155)
- Java primitives are now non-nullable by default [#156](https://github.com/leangen/graphql-spqr/issues/156)
- [Breaking] `InputFieldDiscoveryStrategy` renamed to `InputFieldBuilder` [#147](https://github.com/leangen/graphql-spqr/issues/147)
- [Breaking] `InputFieldBuilder#getInputFields` now receives `InputFieldBuilderParams`
- [Breaking] `ResolverBuilder` interface changed :
  `ResolverBuilder#buildQueryResolvers/buildMutationResolvers/buildSubscriptionResolvers` now receive `ResolverBuilderParams`
- [Breaking] `ResolverArgumentBuilder` interface changed:
  `ResolverArgumentBuilder#buildResolverArguments` now receives `ArgumentBuilderParams`
- [Breaking] `ArgumentInjector` interface changed :
  `ArgumentInjector#getArgumentValue` now receives `ArgumentInjectorParams` [#158](https://github.com/leangen/graphql-spqr/issues/158)
- [Breaking] `ArgumentInjector#supports` now receives the related `Parameter` in addition to its `AnnotatedType`
- [Breaking] `OperationNameGenerator` interface changed :
  `OperationNameGenerator#generateQueryName/generateMutationName/generateSubscriptionName` now receive `OperationNameGeneratorParams`
- [Breaking] All provided `OperationNameGenerator`s redesigned for reusability:
  the new `DefaultOperationNameGenerator` should be applicable to most usages
- `MapToListTypeAdapter` now produces non-nullable entries [#145](https://github.com/leangen/graphql-spqr/issues/145)
- [Breaking] `GsonValueMapperFactory.Configurer` interface changed:
  `GsonValueMapperFactory.Configurer#configure` now receives `ConfigurerParams`
- [Breaking] `JacksonValueMapperFactory.Configurer` interface changed:
  `JacksonValueMapperFactory.Configurer#configure` now receives `ConfigurerParams`
- [Breaking] Abstract input types with a single concrete implementation no longer have a type discriminator field
- All dependencies upgraded, most notably graphql-java in now on 9.2
- Generator methods (`withXXX`) have more intuitive behavior [#123](https://github.com/leangen/graphql-spqr/issues/123)
- Default deprecation reason changed to "Deprecated" for better GraphiQL compatibility

### Deprecated
- `ClassUtils#findImplementations` is superseded by `ClassFinder` [#135](https://github.com/leangen/graphql-spqr/issues/135)

### Removed
- `BeanOperationNameGenerator` removed. Superseded by `PropertyOperationNameGenerator`.
- `MethodOperationNameGenerator` removed. Superseded by `MemberOperationNameGenerator`.
- `ReturnTypeOperationNameGenerator` removed, as it served no purpose.
- `DelegatingOperationNameGenerator` removed. Superseded by `DefaultOperationNameGenerator`.

### Fixed
- Fixed `ScalarMap` deserialization error. `ObjectScalarAdapter` is no longer an `OutputConverter`. [#106](https://github.com/leangen/graphql-spqr/issues/106) [#112](https://github.com/leangen/graphql-spqr/issues/112)
- Fixed type name collision going unnoticed when an input and an output type share a name [#151](https://github.com/leangen/graphql-spqr/issues/151)
- Fixed generic type discovery for static inner classes
- Fixed double-quoting of IDs [#134](https://github.com/leangen/graphql-spqr/issues/134)
- Fixed numerous problems in implementation auto discovery with fat JARs, new class format etc [#111](https://github.com/leangen/graphql-spqr/issues/111) [#121](https://github.com/leangen/graphql-spqr/issues/121) [#150](https://github.com/leangen/graphql-spqr/issues/150)
