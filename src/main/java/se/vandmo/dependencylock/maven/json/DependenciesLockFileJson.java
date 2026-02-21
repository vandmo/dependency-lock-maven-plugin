package se.vandmo.dependencylock.maven.json;

import static java.util.Objects.requireNonNull;
import static se.vandmo.dependencylock.maven.json.JsonUtils.writeJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.stream.Stream;
import se.vandmo.dependencylock.maven.ArtifactIdentifier;
import se.vandmo.dependencylock.maven.Dependencies;
import se.vandmo.dependencylock.maven.Dependency;
import se.vandmo.dependencylock.maven.LockFileAccessor;
import se.vandmo.dependencylock.maven.Profiled;

public final class DependenciesLockFileJson {
  private static final String V3 = "3";

  private final LockFileAccessor dependenciesLockFile;

  private DependenciesLockFileJson(LockFileAccessor dependenciesLockFile) {
    this.dependenciesLockFile = dependenciesLockFile;
  }

  public static DependenciesLockFileJson from(LockFileAccessor dependenciesLockFile) {
    return new DependenciesLockFileJson(requireNonNull(dependenciesLockFile));
  }

  public void write(Dependencies projectDependencies) {
    ObjectNode json = JsonNodeFactory.instance.objectNode();
    json.set("dependencies", asJson(projectDependencies, JsonNodeFactory.instance));
    try (Writer writer = dependenciesLockFile.writer()) {
      writeJson(writer, json);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public void write(Profiled<Dependency, Dependencies> dependencies) {
    JsonNodeFactory jsonNodeFactory = JsonNodeFactory.instance;
    ObjectNode json = jsonNodeFactory.objectNode();
    json.put("version", V3);
    json.set(
        "artifacts",
        JsonUtils.buildArtifactsJson(
            Stream.concat(
                dependencies.getDefaultEntities().artifacts(),
                dependencies.profileEntries().flatMap(entry -> entry.getValue().artifacts())),
            jsonNodeFactory));
    json.set(
        "dependencies",
        JsonUtils.buildDependenciesJson(dependencies.getDefaultEntities(), jsonNodeFactory));
    json.set(
        "profiles", JsonUtils.buildProfilesJson(dependencies.profileEntries(), jsonNodeFactory));
    try (Writer writer = dependenciesLockFile.writer()) {
      writeJson(writer, json);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private JsonNode asJson(
      Iterable<Dependency> lockedDependencies, JsonNodeFactory jsonNodeFactory) {
    ArrayNode json = jsonNodeFactory.arrayNode();
    for (Dependency lockedDependency : lockedDependencies) {
      json.add(asJson(lockedDependency, jsonNodeFactory));
    }
    return json;
  }

  private JsonNode asJson(Dependency lockedDependency, JsonNodeFactory jsonNodeFactory) {
    ObjectNode json = jsonNodeFactory.objectNode();
    final ArtifactIdentifier artifactIdentifier = lockedDependency.getArtifactIdentifier();
    json.put("groupId", artifactIdentifier.groupId);
    json.put("artifactId", artifactIdentifier.artifactId);
    json.put("version", lockedDependency.getVersion());
    json.put("scope", lockedDependency.scope);
    json.put("type", artifactIdentifier.type);
    json.put("optional", lockedDependency.optional);
    json.put("integrity", lockedDependency.getIntegrityForLockFile());
    artifactIdentifier.classifier.ifPresent(
        actualClassifier -> json.put("classifier", actualClassifier));
    return json;
  }
}
