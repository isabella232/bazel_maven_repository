# Bazel Rules for Maven Repositories

A bazel ruleset creating a more idiomatic bazel representation of a maven repo using a
pinned list of artifacts.

Release: `1.0-rc6`

| Link | Sha |
| ---- | --- |
| [Zip File](https://github.com/square/bazel_maven_repository/archive/1.0-rc6.zip) | `e29e57beef25e771acbeddd17b9b9c53d40f81a438018197110f5c2896682e3b` |
| [Tarball](https://github.com/square/bazel_maven_repository/archive/1.0-rc6.tar.gz) | `6558f4e00b8fd956bd29c6c52039c8df286734399cde0f33018b7b40fe061919` |

**[Table of Contents](http://tableofcontent.eu)**
<!-- Table of contents generated generated by http://tableofcontent.eu -->
- [Bazel Rules for Maven Respositories](#bazel-rules-for-maven-respositories)
  - [Overview](#overview)
  - [Supported Types](#supported-types)
  - [Repository URLs](#repository-urls)
  - [Inter-artifact dependencies](#inter-artifact-dependencies)
  - [Coordinate Translation](#coordinate-translation)
    - [Mangling](#mangling)
  - [Artifact Configuration](#artifact-configuration)
    - [Sha verification](#sha-verification)
    - [Substitution of build targets](#substitution-of-build-targets)
    - [Packaging](#packaging)
    - [Classifiers](#classifiers)
  - [API](#api-reference)
    - [maven_repository_specification](#maven_repository_specification)
    - [maven_jvm_artifact](#maven_jvm_artifact)
  - [Limitations](#limitations)
  - [Other Usage Notes](#other-usage-notes)
    - [Caches](#caches)
    - [Clogged WORKSPACE files](#clogged-workspace-files)
    - [Kotlin](#kotlin)
      - [ijar (abi-jar) and inline functions](#ijar-abi-jar-and-inline-functions)
      - [rules_kotlin and maven integration paths](#rules_kotlin-and-maven-integration-paths)
  - [License](#license)


## Overview

**Bazel Rules for Maven Repositories** allow the specification of a list of artifacts which
constitute maven repository's universe of deps, and exposes these deps into a bazel *repository*
namespace.  The name of the repository specification rule becomes the repository name in Bazel.
For instance the following specification:
 
```python
MAVEN_REPOSITORY_RULES_VERSION = "1.0-rc5"
MAVEN_REPOSITORY_RULES_SHA = "294daa44084ec9a097e2308a9d1b2e73fda5788af232e4e142955fdbe4e60cb5"
http_archive(
    name = "maven_repository_rules",
    urls = ["https://github.com/square/bazel_maven_repository/archive/%s.zip" % MAVEN_REPOSITORY_RULES_VERSION],
    type = "zip",
    strip_prefix = "bazel_maven_repository-%s" % MAVEN_REPOSITORY_RULES_VERSION,
    sha256 = MAVEN_REPOSITORY_RULES_SHA,
)
load("@maven_repository_rules//maven:maven.bzl", "maven_repository_specification")
maven_repository_specification(
    name = "maven",
    artifacts = {
        "com.google.guava:guava:25.0-jre": { "sha256": "3fd4341776428c7e0e5c18a7c10de129475b69ab9d30aeafbb5c277bb6074fa9" },
    },
)
```
 
... results in deps of the format `@maven//com/google/guava:guava` (which can be abbreviated to 
`@maven//com/google/guava`)

Dependency versions are resolved in the single artifact list.  Only one version is permitted within
a repository.

> Note: bazel_maven_repository has no workspace dependencies, so adding it to your project will not
> result in any additional bazel repositories to be fetched.

## Repository URLs

By default, `maven_repository_specification` pulls artifacts from `https://repo1.maven.org/maven2`.  However,
for artifacts hosted on different (public or private) maven repositories, using the `repository_urls` allows
the downloader to target any number of `Maven 2`-style repositories.  e.g.:

```
maven_repository_specification(
    name = "maven",
    artifacts = {
        "android.arch.lifecycle:common:1.1.1": { "sha256": "abcdef" },
        "android.arch.lifecycle:livedata-core:1.1.1:aar": { "sha256": "fedcba" },
    },
    repository_urls = [
        "https://repo1.maven.org/maven2",
        "https://dl.google.com/dl/android/maven2/",
    ]
)
```

> Note: Failing to specify the proper set of repositories is a common cause of seeing `404` errors

## Supported Types

Currently `.aar` and `.jar` artifacts are supported.  OSGI bundles are supported by assuming they are
normal `.jar` artifacts (which they are, just have a packaging property of `bundle` and some extra
metadata in `META-INF` of the `.jar` file).

`.aar` artifacts should be specified as `"some.group:some-artifact:1.0:aar"` (just append `:aar`
onto the artifact spec string). 

For any other types, please file a feature request, or supply a pull request.  So long as there
exists a proper bazel import or library rule to bring the artifact's file into bazel's dependency
graph, it should be possible to support it.

> Note: Failing to specify whether an artifact is an `aar` is a common cause of seeing `404` errors

## Inter-artifact dependencies

This rule will, in the generated repository, infer inter-artifact dependencies from pom.xml files
of those artifacts (pulling in only `compile` and `runtime` dependencies, and avoiding any `systemPath`
dependencies).  This avoids the bazel user having to over-specify the full set of dependency jars.

All artifacts, even transitively depended-on ones, need to be specified with pinned versions in the
`artifacts` property, and any artifacts discovered in the inferred dependency search, which are not
present in the main rule's artifact list will be flagged and the build will fail with an error listing
them.

## Coordinate Translation

Translation from maven group/artifact coordinates to bazel package/target coordinates is naive but
orderly.  The logic mirrors the layout of a maven repository, with group_id elements (separated by
`.`) turning into a package hierarchy, and the artifact_id turning into a bazel target. 

### Mangling

Bazel tends not to like package and target names using anything other than `[A-Za-z9-0_]` (though it
can support dashes in some cases).  These rules do a straight mangling of other characters into `_`
in artifact_ids (though not in group_ids because: reasons).

While this typically turns into what you'd expect, there are a few times where it doesn't. 

For instance:

```python
maven_repository_specification(
    name = "maven",
    insecure_artifacts = [
        "org.mockito:mockito-core:1.9.5",
        "joda-time:joda-time:1.1",
    ],
)
```
 
would be referenced in a rule like so:

```python
java_library(
    name = "foo",
    srcs = glob(["*.java"]),
    deps = [
        "@maven//org/mockito:mockito_core",
        "@maven//joda-time:joda_time",
    ],
)
```

> Note: The package/workspace layout generated by the `maven_repository_specification` rule can be
> found at `<workspace>/bazel-<workspace_name>/external/<maven_repo_name>` (all bazel generated
> workspaces are available in `bazel-yourworkspace/external`).  The package structure can be
> inspected if it is confusing.

## Artifact Configuration
### Sha verification

Artifacts with SHA256 checksums can be added to `artifacts`, in the form:
```
    artifacts = {
        "com.google.guava:guava:25.0-jre": { "sha256": "3fd4341776428c7e0e5c18a7c10de129475b69ab9d30aeafbb5c277bb6074fa9" },
    }
```
Artifacts without SHA headers should configured as insecure, like so:
```
    artifacts = {
        "com.google.guava:guava:25.0-jre": { "insecure": True },
    }
```

The rules will reject artifacts without SHAs are not marked as "insecure". 

> Note: These rules cannot validate that the checksum is the right one, only that the one supplied
> in configuration matches the checksum of the file downloaded.  It is the responsibility of the
> maintainer to use proper security practices and obtain the expected checksum from a trusted source.

### Substitution of build targets

One can provide a `BUILD.bazel` target snippet that will be substituted for the auto-generated target
implied by a maven artifact.  This is very useful for providing an annotation-processor-exporting
alternative target.  The substitution is naive, so the string needs to be appropriate and any rules
need to be correct, contain the right dependencies, etc.  To aid that it's also possible to (on a
per-package basis) substitute dependencies on a given fully-qualified bazel target for another. 

A simple use-case would be to substitute a target name (e.g. "mockito-core" -> "mockito") for
cleaner/easier use in bazel:

```python
MOCKITO_SNIPPET = """
alias(name = "mockito", actual=":mockito_core")
maven_jvm_artifact(name = "mockito_core", artifact = "org.mockito:mockito-core:{v}")
"""
maven_repository_specification(
    name = "maven",
    artifacts = {
        "org.mockito:mockito-core:2.20.1": { "sha256": "blahblahblah", "build_snippet": MOCKITO_SNIPPET.format(v="2.20.1") },
        # ... all the other deps.
    },
)
```

This would allow the following use in a `BUILD.bazel` file.

```python
java_test(
  name = "MyTest",
  srcs = "MyTest.java",
  deps = [
    # ... other deps
    "@maven//org/mockito" # instead of "@maven//org/mockito:mockito-core"
  ],
)
```

More complex use-cases are possible, such as adding substitute targets with annotation processing `java_plugin`
targets and exports.  An example with Dagger would look like this (with the basic rule imports assumed):

```python
DAGGER_PROCESSOR_SNIPPET = """
# use this target
java_library(name = "dagger", exports = [":dagger_api"], exported_plugins = [":dagger_plugin"])

# alternatively-named import of the raw dagger library.
maven_jvm_artifact(name = "dagger_api", artifact = "com.google.dagger:dagger:2.20")

java_plugin(
    name = "dagger_plugin",
    processor_class = "dagger.internal.codegen.ComponentProcessor",
    generates_api = True,
    deps = [":dagger_compiler"],
)
"""
```

The above is given as a substitution in the `maven_repository_specification()` rule.  However, since the inferred
dependencies of `:dagger-compiler` would create a dependency cycle because it includes `:dagger` as a dep, the
specification rule also should include a `dependency_target_substitution`, to ensures that the inferred rules in
the generated `com/google/dagger/BUILD` file consume `:dagger_api` instead of the wrapper replacement target.

```python
maven_repository_specification(
    name = "maven",
    artifacts = {
        "com.google.dagger:dagger:2.20": {
            "sha256": "blahblahblah",
            "build_snippet": DAGGER_PROCESSOR_SNIPPET,
        },
        "com.google.dagger:dagger-compiler:2.20": { "sha256": "blahblahblah" },
        "com.google.dagger:dagger-producers:2.20": { "sha256": "blahblahblah" },
        "com.google.dagger:dagger-spi:2.20": { "sha256": "blahblahblah" },
        "com.google.code.findbugs:jsr305:3.0.2": { "sha256": "blahblahblah" },
        # ... all the other deps.
    },
    dependency_target_substitutes = {
        "com.google.dagger": {"@maven//com/google/dagger:dagger": "@maven//com/google/dagger:dagger_api"},
    }
)
```

Thereafter, any target with a dependency on (in this example) `@maven//com/google/dagger` will invoke annotation
processing and generate any dagger-generated code.  The same pattern could be used for
[Dagger](http://github.com/google/dagger), [AutoFactory and AutoValue](http://github.com/google/auto), etc.

Such snippet constants can be extracted into .bzl files and imported to keep the WORKSPACE file tidy. In the
future some standard templates may be offered by this project, but not until deps validation is available, as
it would be too easy to have templates' deps lists go out of date as versions bumped, if no other validation
prevented it or notified about it.

### Packaging

Optionally, an artifact may specify a packaging. Valid artifact coordinates are listable this way:
`"group_id:artifact_id:version[:packaging]"`

At present, only `jar` (default) and `aar` packaging are supported.

### Classifiers

Classifiers have only limited support. An artifact can specify a classifier, but only that or
the unclassified artifact can be used, but not both.

Classifiers are tacked on the end, e.g. `"foo.bar:blah:1.0:jar:some-classifier"`

## Cacheing

Bazel can cache artifacts if you provide sha256 hashes.  These will make the artifacts candidates
for the "content addressable" cache, which is machine-wide, and survives even `bazel clean --expunge`.
The caveat for this is that if you put the wrong hash, and if that hash is to a file you already have
downloaded, bazel's internal download mechanism will serve that file instead of the remote file. This
can cause you to accidentally supply a wrong version, wrong artifact, or wrong kind of file if you're
not careful.  So take caution when recording the sha256 hashes, both for security and performance
reasons.  The hash will be preferred over the artifact as a way to identify the artifact, under the
hood.

## API Reference

### maven_repository_specification

This rule assembles a bazel workspace that represents the artifacts supplied in a bazely form, with
groupIds split on `.` and representing package paths, and artifactIds used as target names (replacing
`.` and `-` with `_` by default).

The rule supports per-artifact configuration as well as some limited group-level configuration.



```
maven_repository_specification(
        # The name of the repository
        name,

        # The dictionary of artifact -> properties which allows us to specify artifacts with more details.
        # These properties don't include the group, artifact name, version, classifier, or type, which are
        # all specified by the artifact key itself.
        #
        # The currently supported properties are:
        #    sha256 -> the hash of the artifact file to be downloaded. (Incompatible with "insecure")
        #    insecure -> if True, don't fail on a missing sha256 hash. (Incompatible with "sha256")
        #    build_snippet -> replaces the generated target snippet with the supplied text
        artifacts = {},

        # The dictionary of per-group target substitutions.  These must be in the format:
        # "@myreponame//path/to/package:target": "@myrepotarget//path/to/package:alternate"
        # These are not public aliases, but only apply to intra-package references. These can be 
        # used to address build cycles introduced by one or more build_snippets that wrap targets.
        # See the dagger example in the test/test_workspace sample repository.  
        dependency_target_substitutes = {},

        # Optional list of repositories which the build rule will attempt to fetch maven artifacts and
        # metadata.
        repository_urls = ["https://repo1.maven.org/maven2"]):
```

### maven_jvm_artifact

This rule is mostly used by the generated code, but can be used in build_snippets. It undrestands
the structure of the individual fetch workspaces built for each artifact, and so provides the link
between the man maven workspace and the workhorse workspaces responsibility for fetching each .jar, etc.

```
maven_jvm_artifact(
  artifact, # The maven-style artifact coordinates (groupId:artifactId:version[[:type]:classifier])
  name = None, # The bazel target name (implicitly artifactId with "." and "-" converted to "_")
  deps = [], # Any dependencies needed at compile-time for consumers of this target.
  runtime_deps = [], # Any dependencies needed only at runtime (built into _test and _binary deploy jars
  exports = [], # Any targets listed here are treated by the consuming rule as if it had declared them.
  visibility = ["//visibility:public"],
  **kwargs) # Extra parameters passed through to the underlying import rules
```

> Note: `deps`, `runtime_deps`, and `exports` behave exactly as a java_library would treat them.  Technically
> `runtime_deps` and `exports` are part of the `**kwargs` and are just passed through naively to the underlying
> import rules.

> Note: The rules_kotlin (as of March, 2019) contain a bug which fails to propagate compile deps.
> For some cases, adding a build_snippet that exports otherwise unused dependencies as exports can
> mitigate this for a few common cases (e.g. rxjava2->reactive-streams).

## Limitations

  * Doesn't support -SNAPSHOT dependencies (#5)
  * Doesn't support multiple versions of a dependency (by design).
  * Doesn't support multiple calls to `maven_repository_specification()` due to collisions in
    the implicit fetching rules it creates. This limitation will be lifted in a version. (#6)
  * Doesn't support -source.jar downloading and attachment. (#44)
  * .pom files are not cached across `bazel clean --expunge` despite being immutable.  Jar files
    can be if sha256 hashes are supplied.

## Other Usage Notes

### Caches

Because of the nature of bazel repository/workspace operation, updating the list of artifacts may
invalidate build caches, and force a re-run of workspace operations (and possibly reduce
incrementality of the next build).  This is unavoidable.

### Clogged WORKSPACE files

It may make sense, if one's maven universe gets big, to extract the list of artifacts into a 
constant in a separate file (e.g. `maven_artifacts.bzl`) and import it.

### Kotlin

#### ijar (abi-jar) and inline functions

Bazel java rules tend to process .jar artifacts through a tool called `ijar` for performance reasons
and to reduce the compile-time dependency graph (since purely implementation dependencies are not
required and do not contribute to the compilation job's hash). These are useful, but less valuable
with pre-built jars.

Further, when ijar strips function bodies, it currently does not honor any kotlin semantics with
respect to inline functions.  Given that, and other possible jvm language conflicts, and given that
the benefit of ijar is more for built-from-source artifacts, bazel_maven_repository simply imports
the maven-hosted jars with ijar disabled, so the raw jar is used in compilation.  This is a
near-term workaround for bazelbuild/bazel#4549, and better approaches may be used once ijar is more
aware of other jvm languages' needs.

#### rules_kotlin and maven integration paths

[rules_kotlin] currently break when running in full sandbox mode (without the kotlin compilation
worker).  Specifically, it misinterprets paths in the sandbox.  Therefore, if using [rules_kotlin]
it is crucial to include `--strategy=KotlinCompile=worker` either on the command-line, or in the
project's .bazelrc or your personal .bazelrc.  Otherwise, the annotation processor will fail to
find the jar contents for annotation processors such as `Dagger 2` or `AutoValue` or `AutoFactory`.

## License

License
Copyright 2018 Square, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
