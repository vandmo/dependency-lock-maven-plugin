# dependency-lock-maven-plugin

[![Build Status](https://img.shields.io/github/workflow/status/vandmo/dependency-lock-maven-plugin/Test%20and%20Release?label=Build)](https://github.com/vandmo/dependency-lock-maven-plugin/actions/workflows/test-and-release.yaml)
[![usefulness 100%](https://img.shields.io/badge/usefulness-100%25-success.svg?label=Usefulness)](https://www.google.com/search?q=pasta+machine)
[![Maven Central](https://img.shields.io/maven-central/v/se.vandmo/dependency-lock-maven-plugin.svg?label=Maven%20Central)](https://search.maven.org/artifact/se.vandmo/dependency-lock-maven-plugin)
[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/apache/maven.svg?label=License)](https://www.apache.org/licenses/LICENSE-2.0)

> **Warning**
> This is the development version for the upcoming 1.x release.
> For the current released version see [the 0.x branch](https://github.com/vandmo/dependency-lock-maven-plugin/tree/0.x)

Maven only requires you to specify versions of dependencies that you use directly.
Transitive dependencies aren't visible in the pom.xml and their version is chosen in a seemingly random way.

This Maven plugin enables you to:
* Review exactly which dependencies you have, including transitive ones
* Make sure dependencies are not accidentally changed
* Verify the integrity of your dependencies to avoid supply chain attacks, see [Dependency Confusion](https://fossa.com/blog/dependency-confusion-understanding-preventing-attacks/)
* Track changes to dependencies in your SCM
* Enable vulnerability scanning in all your dependencies, including transitive ones
* Enable Dependabot Security Alerts for transitive dependencies

It is a bit like `mvn dependency:list` but the output is intended to be tracked by you SCM
and the _check_ goal makes sure you don't forget to update the file.

Locking
-------
`mvn se.vandmo:dependency-lock-maven-plugin:lock`
will create a file named _dependencies-lock.json_ by default.

You should then commit that file to your source control of choice.

Choose between JSON format and POM XML format. The latter is more verbose but will be detected by Dependabot Security Alerts.

Validating
----------
The following snippet in your _pom.xml_ file will make sure that the actual
dependencies are the same as in the _dependencies-lock.json_ file.

```xml
<build>
  <plugins>
    <plugin>
      <groupId>se.vandmo</groupId>
      <artifactId>dependency-lock-maven-plugin</artifactId>
      <version>use latest version</version>
      <executions>
        <execution>
          <id>check</id>
          <phase>validate</phase>
          <goals>
            <goal>check</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

Tips
----
Adding the following in _~/.m2/settings.xml_ will allow you to write `mvn dependency-lock:lock`

```xml
<pluginGroups>
  <pluginGroup>se.vandmo</pluginGroup>
</pluginGroups>
```

Shared Configuration
--------------------
Configuration shared between all goals.

### filename
The filename of the lock file.

### format
Which lock file format to use, defaults to _json_.
* _json_, lock file in JSON format, default filename is dependency-lock.json
* _pom_, lock file in POM XML format, default filename is .dependeny-lock/pom.xml

Goals
-----
### check
Checks that actual dependencies matches the lock file. Fails the build if there
is no match.
Expects a lock file to exist.

If some dependencies are part of the same multi-module project you might want those dependencies
to be the same version as the artifact where the dependencies are locked.
You can achieve this by configuring the plugin like such:
```xml
<configuration>
  <dependencySets>
    <dependencySet>
      <includes>
        <include>org.myorg:myapplication-*</include>
      </includes>
      <version>use-project-version</version>
    </dependencySet>
  </dependencySets>
</configuration>
```
The filter syntax is `[groupId]:[artifactId]:[type]:[version]`
where each pattern segment is optional and supports full and partial `*` wildcards.
An empty pattern segment is treated as an implicit wildcard.
For example, `org.myorg.*` will match all artifacts whose group id starts with `org.myorg.`, and  `:::*-SNAPSHOT` will match all snapshot artifacts.

### lock
Creates a lock file from the actual dependencies.

Notes
-----
### Dependabot Updates won't work
Dependabot Updates currently creates a single PR for each change.
If you use pom format and merge all PRs from Dependabot then that combined build might work, but each single PR will fail.
There are feature requests for combined PRs for Dependabot which, if implemented, could make a combined PR work.
Another approach to automate the creation of PRs would be to have a GitHub workflow that creates a combined PR based on the Dependabot PRs.

### News in version 1.x
* Integrity checking is enabled by default
* Configuration is more verbose but also more flexible
