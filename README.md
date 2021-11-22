# dependency-lock-maven-plugin

[![Build Status](https://travis-ci.com/vandmo/dependency-lock-maven-plugin.svg?branch=master)](https://travis-ci.com/vandmo/dependency-lock-maven-plugin)
![usefulness 100%](https://img.shields.io/badge/usefulness-100%25-success.svg)

Maven only requires you to specify versions of dependencies that you use directly.
Transitive dependencies aren't visible in the pom.xml and their version is chosen in a seemingly random way.

This Maven plugin  enables you to:
* Review exactly which dependencies you have, including transitive ones
* Make sure dependencies are not accidentally changed
* Track changes to dependencies in your SCM
* Enable vulnerability scanning in all your dependencies, including transitive ones
* Enable Dependabot Security Alerts for transitive dependencies

It is a bit like `mvn dependency:list` but the output is intended to be tracked by you SCM
and the _check_ goal makes sure you don't forget to update the file.

Locking
-------
`mvn se.vandmo:dependency-lock-maven-plugin:create-lock-file`
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
Adding the following in _~/.m2/settings.xml_ will allow you to write `mvn dependency-lock:create-lock-file`

```xml
<pluginGroups>
  <pluginGroup>se.vandmo</pluginGroup>
</pluginGroups>
```

Configuration
-------------
### filename
The filename of the lock file.

### useMyVersionFor
Used to specify which dependencies should have the same version as the main module.

Example:
```xml
<useMyVersionFor>
  <dependency>org.myorg:myapplication-controllers-*</dependency>
  <dependency>org.myorg:myapplication-helpers</dependency>
</useMyVersionFor>
```

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
  <useMyVersionFor>
    <dependency>org.myorg:myapplication-*</dependency>
  </useMyVersionFor>
</configuration>
```
The filter syntax is `[groupId]:[artifactId]:[type]:[version]`
where each pattern segment is optional and supports full and partial `*` wildcards.
An empty pattern segment is treated as an implicit wildcard.
For example, `org.myorg.*` will match all artifacts whose group id starts with `org.myorg.`, and  `:::*-SNAPSHOT` will match all snapshot artifacts.

### format
__Deprecated:__ Use `create-lock-file` instead and avoid editing the lock file

~~Formats the lock file.
This can be useful after manually editing the lock file to make sure future
changes does not reformat the edits. Note that editing the lock file isn't needed any longer.~~

### lock
__Deprecated:__ Use `create-lock-file` instead

~~Creates a lock file from the actual dependencies.
Considers some values in the existing lock file.~~


### create-lock-file
Creates a lock file from the actual dependencies.
