package se.vandmo.dependencylock.maven;

import static java.util.Collections.singletonMap;
import static se.vandmo.dependencylock.maven.JsonUtils.readJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public final class DependenciesLockFile {

  private final File file;

  private DependenciesLockFile(File file) {
    this.file = file;
  }

  public static DependenciesLockFile fromBasedir(File basedir) {
    return new DependenciesLockFile(new File(basedir, "dependencies-lock.json"));
  }

  public void write(Artifacts artifacts) {
    try {
      new ObjectMapper()
          .writerWithDefaultPrettyPrinter()
          .writeValue(file, asJson(artifacts));
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  public LockedDependencies read() {
    JsonNode json = readJson(file);
    if (!json.isObject()) {
      throw new IllegalStateException("Expected top level type to be an object");
    }
    JsonNode dependencies = json.get("dependencies");
    if (dependencies == null || !dependencies.isArray()) {
      throw new IllegalStateException("Expected a property named 'dependencies' of type array");
    }
    return LockedDependencies.fromJson(dependencies);
  }

  private static Map<String, Object> asJson(Artifacts artifacts) {
    List<Map<String, String>> list = new ArrayList<>();
    artifacts.artifacts.stream().sorted().forEach(artifact -> list.add(asJson(artifact)));
    return singletonMap("dependencies", list);
  }

  private static Map<String, String> asJson(Artifact artifact) {
    Map<String, String> m = new LinkedHashMap<>();
    m.put("groupId", artifact.groupId);
    m.put("artifactId", artifact.artifactId);
    m.put("version", artifact.version);
    m.put("scope", artifact.scope);
    m.put("type", artifact.type);
    artifact.classifier.ifPresent(actualClassifier -> m.put("classifier", actualClassifier));
    return m;
  }

  public boolean exists() {
    return file.exists();
  }

}
