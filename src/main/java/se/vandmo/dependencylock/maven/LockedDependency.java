package se.vandmo.dependencylock.maven;

import static java.util.Objects.requireNonNull;
import static se.vandmo.dependencylock.maven.JsonUtils.getStringValue;
import static se.vandmo.dependencylock.maven.JsonUtils.possiblyGetStringValue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;
import java.util.function.Predicate;

public final class LockedDependency implements Comparable<LockedDependency>, Predicate<Artifact> {

  public final ArtifactIdentifier identifier;
  public final String version;
  public final String scope;
  public final String type;

  private LockedDependency(
      ArtifactIdentifier identifier,
      String version,
      String scope,
      String type) {
    this.identifier = requireNonNull(identifier);
    this.version = requireNonNull(version);
    this.scope = requireNonNull(scope);
    this.type = requireNonNull(type);
  }

  public static LockedDependency fromJson(JsonNode json) {
    return new LockedDependency(
        new ArtifactIdentifier(
            getStringValue(json, "groupId"),
            getStringValue(json, "artifactId"),
            possiblyGetStringValue(json, "classifier"),
            possiblyGetStringValue(json, "type")),
        getStringValue(json, "version"),
        getStringValue(json, "scope"),
        getStringValue(json, "type"));
  }

  public static LockedDependency from(Artifact artifact) {
    return new LockedDependency(
        artifact.identifier,
        artifact.version,
        artifact.scope,
        artifact.type
    );
  }

  public JsonNode asJson() {
    ObjectNode json = JsonNodeFactory.instance.objectNode();
    json.put("groupId", identifier.groupId);
    json.put("artifactId", identifier.artifactId);
    json.put("version", version);
    json.put("scope", scope);
    json.put("type", type);
    identifier.classifier.ifPresent(actualClassifier -> json.put("classifier", actualClassifier));
    return json;
  }

  public Artifact toArtifact() {
    return Artifact
        .builder()
        .artifactIdentifier(identifier)
        .version(version)
        .scope(scope)
        .type(type)
        .build();
  }

  @Override
  public boolean test(Artifact artifact) {
    return
        identifier.equals(artifact.identifier) &&
        version.matches(artifact.version) &&
        scope.equals(artifact.scope) &&
        type.equals(artifact.type);
  }

  @Override
  public int compareTo(LockedDependency other) {
    return toString().compareTo(other.toString());
  }

  @Override
  public String toString() {
    return new StringBuilder()
        .append(identifier)
        .append(':').append(version)
        .append(':').append(scope)
        .append(':').append(type)
        .toString();
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 17 * hash + Objects.hashCode(this.identifier);
    hash = 17 * hash + Objects.hashCode(this.version);
    hash = 17 * hash + Objects.hashCode(this.scope);
    hash = 17 * hash + Objects.hashCode(this.type);
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final LockedDependency other = (LockedDependency) obj;
    if (!Objects.equals(this.identifier, other.identifier)) {
      return false;
    }
    if (!Objects.equals(this.version, other.version)) {
      return false;
    }
    if (!Objects.equals(this.scope, other.scope)) {
      return false;
    }
    if (!Objects.equals(this.type, other.type)) {
      return false;
    }
    return true;
  }

  public WithMyVersion withMyVersion(String myVersion) {
    return new WithMyVersion(myVersion);
  }

  public final class WithMyVersion implements Predicate<Artifact>{

    private final String myVersion;

    private WithMyVersion(String myVersion) {
      this.myVersion = myVersion;
    }

    public String toString() {
      return new StringBuilder()
          .append(identifier)
          .append(':').append(myVersion)
          .append(':').append(scope)
          .append(':').append(type)
          .toString();
    }

    @Override
    public boolean test(Artifact artifact) {
      return
          identifier.equals(artifact.identifier) &&
          version.matches(myVersion) &&
          scope.equals(artifact.scope) &&
          type.equals(artifact.type);
    }
  }
}
