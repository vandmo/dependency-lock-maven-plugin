# dependency-lock-maven-plugin

Maven plugin that makes sure that Maven dependency are not accidentaly changed.

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
