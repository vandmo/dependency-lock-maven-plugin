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
import se.vandmo.dependencylock.maven.ArtifactIdentifier;
import se.vandmo.dependencylock.maven.Dependency;
import se.vandmo.dependencylock.maven.LockFileAccessor;
import se.vandmo.dependencylock.maven.LockedDependencies;

public final class DependenciesLockFileJson {

  private final LockFileAccessor dependenciesLockFile;
  private final VersionConstraintJsonSerializer versionConstraintJsonSerializer;

  private DependenciesLockFileJson(LockFileAccessor dependenciesLockFile) {
    this.dependenciesLockFile = dependenciesLockFile;
    this.versionConstraintJsonSerializer = new VersionConstraintJsonSerializer();
  }

  public static DependenciesLockFileJson from(LockFileAccessor dependenciesLockFile) {
    return new DependenciesLockFileJson(requireNonNull(dependenciesLockFile));
  }

  public void write(LockedDependencies lockedDependencies) {
    JsonNode json = toJson(lockedDependencies, JsonNodeFactory.instance);
    try (Writer writer = dependenciesLockFile.writer()) {
      writeJson(writer, json);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private JsonNode toJson(LockedDependencies lockedDependencies, JsonNodeFactory nodeFactory) {
    ObjectNode json = nodeFactory.objectNode();
    json.set("dependencies", asJson(lockedDependencies, nodeFactory));
    return json;
  }

  private JsonNode asJson(LockedDependencies lockedDependencies, JsonNodeFactory nodeFactory) {
    ArrayNode json = nodeFactory.arrayNode();
    for (Dependency lockedDependency : lockedDependencies.lockedEntities) {
      json.add(asJson(lockedDependency, nodeFactory));
    }
    return json;
  }

  private JsonNode asJson(Dependency lockedDependency, JsonNodeFactory nodeFactory) {
    ObjectNode json = nodeFactory.objectNode();
    final ArtifactIdentifier artifactIdentifier = lockedDependency.getArtifactIdentifier();
    json.put("groupId", artifactIdentifier.groupId);
    json.put("artifactId", artifactIdentifier.artifactId);
    json.set(
        "version",
        lockedDependency.getVersion().accept(versionConstraintJsonSerializer, nodeFactory));
    json.put("scope", lockedDependency.scope);
    json.put("type", artifactIdentifier.type);
    json.put("optional", lockedDependency.optional);
    json.put("integrity", lockedDependency.getIntegrityForLockFile());
    artifactIdentifier.classifier.ifPresent(
        actualClassifier -> json.put("classifier", actualClassifier));
    return json;
  }
}
