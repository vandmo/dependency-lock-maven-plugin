# Agent guide

## Repository shape

This is a single Maven `maven-plugin` project (`pom.xml`); it has no Maven
submodules. Production code is under
`src/main/java/se/vandmo/dependencylock/maven`, unit tests under `src/test/java`,
and Maven Invoker integration tests under `src/it`. The `mvn-support/` and
`.dependency-lock*/` trees contain checked-in lock files for the project and
the Maven versions used by its build. `src/main/resources` contains the
FreeMarker templates used for POM locks.

## Plugin entry points and flow

The annotated Mojos are:

* `mojos/LockMojo` (`lock`) creates a lock file. With `lockBuild`, it also
  locks parents, build plugins, and extensions.
* `mojos/CheckMojo` (`check`, default phase `validate`) reads a lock file and
  compares dependencies, and optionally parents, plugins, and extensions.
* `mojos/ListProfilesMojo` (`list-profiles`) reports dependency profiles that
  can be selected for locking.

`AbstractDependencyLockMojo` injects the current `MavenProject` and
`MavenSession`, selects the lock filename/format, and converts ordered
`dependencySets` into `Filters`. `LockMojo` resolves the current and emulated
profile dependency sets, separates shared dependencies from profile-specific
ones, builds the internal `Project`/`LockedProject` model, applies ignored
settings, and writes it. `CheckMojo` loads the lock, selects active profiles,
uses the current resolved artifacts, and delegates comparisons to the
`Locked*` classes and `DiffHelper`.

## Resolution and internal model

`services/DependenciesHelperImpl` uses Maven's `ProjectDependenciesResolver`
with the session's Aether `RepositorySystemSession` and maps Aether graph
dependencies to Maven artifacts, preserving scope and optionality. An
`IgnoreArtifactsWithIgnoredIntegrity` dependency filter avoids resolving
artifacts configured with `integrity=ignore`, while retaining an ignored
internal dependency entry.

`ArtifactIdentifier`, `Artifact`, and `Dependency` are the core lock model.
`Dependency` adds scope and optionality; `Parent`, `Plugin`, and `Extension`
reuse the same lockable artifact concepts. `MavenArtifact` adapts the
internal version constraints back to Maven's artifact/filter API. `Project`
contains profiled dependencies plus optional parents/plugins/extensions.

There are two different sources of “actual” dependencies:

* During `lock`, the plugin asks Maven/Aether to resolve the project dependency
  graph through `ProjectDependenciesResolver`.
* During `check`, it reads `mavenProject().getArtifacts()`, which is Maven's
  already-resolved project artifact set.

The plugin does not explicitly inspect `MavenSession` reactor projects or
replace reactor artifacts with project objects. Reactor behavior therefore
comes from Maven's normal resolution/session state, not a separate plugin
reactor-resolution path. `LockProjectHelper` separately resolves plugin
descriptors through `MavenPluginManager`; `Parents` walks the current
`MavenProject` parent chain.

## Filters and `use-project-version`

`DependencySet` configuration is converted to strict include/exclude artifact
filters. `Filters` reverses the configured list, so later dependency sets have
priority. It supplies version/integrity behavior and `allowMissing` /
`allowExtraneous` decisions.

`version=use-project-version` is implemented by `FilterUtils` replacing the
internal version with `VersionConstraints.useProjectVersion()`. JSON writes
this as `"${project.version}"`; POM parsing recognizes the same value.
`UseProjectVersionConstraint` compares against the current locking/checking
project version, and `DiffHelper` materializes that expected version while
checking. This is a version constraint, not reactor-project detection: it
does not determine whether an artifact is a current reactor module and does
not alter Aether resolution or artifact integrity handling. The multi-project
Invoker cases under `src/it/{json,pom}/lock-multi-project-use-project-version`
demonstrate the current behavior.

Other filter behaviors include exact version checking, ignored versions,
snapshot-relaxed matching, ignored integrity, and allowances for missing or
extraneous entries. `markIgnoredAsIgnored` applies those settings to the
generated model before writing.

## Integrity

`Artifact.from(org.apache.maven.artifact.Artifact)` calculates integrity from
the resolved file using `Checksum`, currently `sha512:` plus Base64-encoded
SHA-512. Directory artifacts are represented as `Integrity.Folder`; configured
or deliberately skipped checks use `Integrity.Ignored`. Lock comparisons
normally compare integrity and only suppress differences when the filter says
to ignore them.

## Lock formats

`LockFileFormat` selects JSON (default `dependencies-lock.json`) or POM
(`.dependency-lock/pom.xml`) through `LockFileAccessor`, which creates parent
directories when writing.

* `json/DependenciesLockFileJson` writes dependency-only locks; profiled
  locks use JSON versions 2/3 and store shared dependencies, profile entries,
  and an artifact table. `json/LockfileJson` handles full build locks.
* `pom/DependenciesLockFilePom` renders `pom.ftlx`. Full POM locks use
  `pom/LockFilePom` and additionally write `parents/pom.xml`,
  `plugins/pom.xml`, and `extensions/pom.xml` from their templates.
  `pom/PomLockFile` parses these files and validates lock versions/content.

## Tests and development

Unit tests cover model equality/conversion, checksums, filters/version
utilities, JSON and POM parsing, and plugin helper behavior. Invoker tests
cover JSON/POM locking and checking, profiles, transitivity, multi-project
builds, integrity/version/scope/optional failures, and configuration edge
cases; each scenario has an `invoker.properties` and usually a
`postbuild.groovy`.

Use the Maven wrapper for reproducible commands:

* `./mvnw test` runs unit tests.
* `./mvnw -Pits verify` runs the Invoker integration-test profile.
* `tasks/full-build` is the project’s build-pipeline wrapper.
* `tasks/format` formats code; generated sum-type sources are produced by
  `tasks/generate-sumtypes` and should not be edited manually.
* `tasks/lock-dependencies` updates the project lock files.

Keep generated/check-in lock files synchronized when changing dependency or
build behavior, and prefer the existing wrapper/tasks over ad-hoc Maven
versions.
