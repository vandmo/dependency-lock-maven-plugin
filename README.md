# dependency-lock-maven-plugin

[![Build Status](https://travis-ci.com/vandmo/dependency-lock-maven-plugin.svg?branch=master)](https://travis-ci.com/vandmo/dependency-lock-maven-plugin)
![usefuleness 100%](https://img.shields.io/badge/usefulness-100%25-success.svg)

Maven plugin that makes sure that Maven dependency are not accidentaly changed.

Locking
-------
`mvn se.vandmo:dependency-lock-maven-plugin:lock`
will create a _dependencies-lock.json_ file.

You should then commit that file to you source control of choice.

If some dependencies are part of the same multi-module project you might want those dependencies to be the same version as the artifact where the dependencies are locked.
You can achieve this by editing the _dependencies-lock.json_ and change `"version" : "1.2.3"` to `"version": { "use-mine": true }`.
That change will be retained the next time you run the lock goal.

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
      <configuration>
        <filename>optional-lock-filename.json</filename>
      </configuration>
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

Goals
-----
### check
Checks that actual dependencies matches the lock file. Fails the build if there
is no match.
Expects a lock file to exist.

### format
Formats the lock file.
This can be useful after manually editing the lock file to make sure future
changes does not reformat the edits.

### lock
Creates a lock file from the actual dependencies.
Considers some values in the existing lock file.
