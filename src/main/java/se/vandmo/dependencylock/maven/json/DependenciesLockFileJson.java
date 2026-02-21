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

  private DependenciesLockFileJson(LockFileAccessor dependenciesLockFile) {
    this.dependenciesLockFile = dependenciesLockFile;
  }

  public static DependenciesLockFileJson from(LockFileAccessor dependenciesLockFile) {
    return new DependenciesLockFileJson(requireNonNull(dependenciesLockFile));
  }

  public void write(LockedDependencies lockedDependencies) {
    ObjectNode json = JsonNodeFactory.instance.objectNode();
    json.set("dependencies", asJson(lockedDependencies));
    try (Writer writer = dependenciesLockFile.writer()) {
      writeJson(writer, json);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private JsonNode asJson(LockedDependencies lockedDependencies) {
    ArrayNode json = JsonNodeFactory.instance.arrayNode();
    for (Dependency lockedDependency : lockedDependencies.lockedEntities) {
      json.add(asJson(lockedDependency));
    }
    return json;
  }

  private JsonNode asJson(Dependency lockedDependency) {
    ObjectNode json = JsonNodeFactory.instance.objectNode();
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
