package se.vandmo.dependencylock.maven.json;

import static java.util.Objects.requireNonNull;
import static se.vandmo.dependencylock.maven.json.JsonUtils.getBooleanOrDefault;
import static se.vandmo.dependencylock.maven.json.JsonUtils.getNonBlankStringValue;
import static se.vandmo.dependencylock.maven.json.JsonUtils.possiblyGetStringValue;
import static se.vandmo.dependencylock.maven.json.JsonUtils.readJson;
import static se.vandmo.dependencylock.maven.json.JsonUtils.writeJson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.plugin.logging.Log;
import se.vandmo.dependencylock.maven.ArtifactIdentifier;
import se.vandmo.dependencylock.maven.Dependencies;
import se.vandmo.dependencylock.maven.DependenciesLockFile;
import se.vandmo.dependencylock.maven.Dependency;
import se.vandmo.dependencylock.maven.LockFileAccessor;
import se.vandmo.dependencylock.maven.LockedDependencies;

public final class DependenciesLockFileJson implements DependenciesLockFile {

  private final LockFileAccessor dependenciesLockFile;
  private final Log log;

  private DependenciesLockFileJson(LockFileAccessor dependenciesLockFile, Log log) {
    this.dependenciesLockFile = dependenciesLockFile;
    this.log = log;
  }

  public static DependenciesLockFileJson from(LockFileAccessor dependenciesLockFile, Log log) {
    return new DependenciesLockFileJson(requireNonNull(dependenciesLockFile), requireNonNull(log));
  }

  public LockedDependencies read() {
    JsonNode json = readJsonNode();
    if (!json.isObject()) {
      throw new IllegalStateException("Expected top level type to be an object");
    }
    JsonNode dependencies = json.get("dependencies");
    if (dependencies == null || !dependencies.isArray()) {
      throw new IllegalStateException("Expected a property named 'dependencies' of type array");
    }
    return fromJson(dependencies, log);
  }

  private static LockedDependencies fromJson(JsonNode json, Log log) {
    if (!json.isArray()) {
      throw new IllegalStateException("Needs to be an array");
    }
    List<Dependency> lockedDependencies = new ArrayList<>();
    for (JsonNode entry : json) {
      lockedDependencies.add(lockedDependencyFromJson(entry));
    }
    return LockedDependencies.from(Dependencies.fromDependencies(lockedDependencies), log);
  }

  private static Dependency lockedDependencyFromJson(JsonNode json) {
    return Dependency.builder()
        .artifactIdentifier(
            ArtifactIdentifier.builder()
                .groupId(getNonBlankStringValue(json, "groupId"))
                .artifactId(getNonBlankStringValue(json, "artifactId"))
                .classifier(possiblyGetStringValue(json, "classifier"))
                .type(possiblyGetStringValue(json, "type"))
                .build())
        .version(getNonBlankStringValue(json, "version"))
        .integrity(getNonBlankStringValue(json, "integrity"))
        .scope(getNonBlankStringValue(json, "scope"))
        .optional(getBooleanOrDefault(json, "optional", false))
        .build();
  }

  private JsonNode readJsonNode() {
    try (Reader reader = dependenciesLockFile.reader()) {
      return readJson(reader);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public void write(Dependencies projectDependencies) {
    write(LockedDependencies.from(projectDependencies, log));
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
