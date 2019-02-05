package se.vandmo.dependencylock.maven;

import static java.util.Collections.singletonMap;
import static java.util.Optional.ofNullable;

import com.fasterxml.jackson.databind.DeserializationFeature;
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

  public Artifacts read() {
    List<Artifact> artifacts = new ArrayList<>();
    Json json = readJson();
    json.dependencies.forEach(dependency -> {
      artifacts.add(new Artifact(
          dependency.groupId,
          dependency.artifactId,
          dependency.version,
          dependency.scope,
          dependency.type,
          ofNullable(dependency.classifier)));
    });
    return new Artifacts(artifacts);
  }

  private Json readJson() throws UncheckedIOException {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      return objectMapper.readValue(file, Json.class);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
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

  public static final class Json {
    public List<Dependency> dependencies;
    public static class Dependency {
      public String groupId;
      public String artifactId;
      public String version;
      public String scope;
      public String type;
      public String classifier;
    }
  }

}
