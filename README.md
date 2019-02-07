# dependency-lock-maven-plugin

[![Build Status](https://travis-ci.com/vandmo/dependency-lock-maven-plugin.svg?branch=master)](https://travis-ci.com/vandmo/dependency-lock-maven-plugin)
![usefuleness 100%](https://img.shields.io/badge/usefulness-100%25-success.svg)

Maven plugin that makes sure that Maven dependency are not accidentaly changed.

Locking
-------
`mvn se.vandmo:dependency-lock-maven-plugin:lock`
will create a _dependencies-lock.json_ file.

You should then commit that file to you source control of choice.

Validating
----------
The following snippet in your _pom.xml_ file will make sure that the actual
dependencies are the same as in the _dependencies-lock.json_ file.

```xml
<build>
  <plugins>
    <plugin>
      <groupId>se.vandmo</groupId>
      <artifactId>dependency-check-maven-plugin</artifactId>
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
