package se.vandmo.dependencylock.maven;

import static java.util.Objects.requireNonNull;
import static se.vandmo.dependencylock.maven.JsonUtils.getStringValue;
import static se.vandmo.dependencylock.maven.JsonUtils.possiblyGetStringValue;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;
import java.util.Optional;

public final class LockedDependency implements Comparable<LockedDependency> {

  public final String groupId;
  public final String artifactId;
  public final LockedVersion version;
  public final String scope;
  public final String type;
  public final Optional<String> classifier;

  private LockedDependency(
      String groupId,
      String artifactId,
      LockedVersion version,
      String scope,
      String type,
      Optional<String> classifier) {
    this.groupId = requireNonNull(groupId);
    this.artifactId = requireNonNull(artifactId);
    this.version = requireNonNull(version);
    this.scope = requireNonNull(scope);
    this.type = requireNonNull(type);
    this.classifier = requireNonNull(classifier);
  }

  public static LockedDependency fromJson(JsonNode json) {
    return new LockedDependency(
        getStringValue(json, "groupId"),
        getStringValue(json, "artifactId"),
        LockedVersion.fromJson(json.get("version")),
        getStringValue(json, "scope"),
        getStringValue(json, "type"),
        possiblyGetStringValue(json, "classifier"));
  }

  public static LockedDependency from(Artifact artifact) {
    return new LockedDependency(
        artifact.groupId,
        artifact.artifactId,
        LockedVersion.fromVersion(artifact.version),
        artifact.scope,
        artifact.type,
        artifact.classifier
    );
  }

  public boolean matches(Artifact artifact, String projectVersion) {
    return
        groupId.equals(artifact.groupId) &&
        artifactId.equals(artifact.artifactId) &&
        version.matches(artifact.version, projectVersion) &&
        scope.equals(artifact.scope) &&
        type.equals(artifact.type) &&
        classifier.equals(artifact.classifier);
  }

  @Override
  public int compareTo(LockedDependency other) {
    return toString().compareTo(other.toString());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb
        .append(groupId)
        .append(':').append(artifactId)
        .append(':').append(version)
        .append(':').append(scope)
        .append(':').append(type);
    classifier.ifPresent(actualClassifier -> {
      sb.append(':').append(actualClassifier);
    });
    return sb.toString();
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 17 * hash + Objects.hashCode(this.groupId);
    hash = 17 * hash + Objects.hashCode(this.artifactId);
    hash = 17 * hash + Objects.hashCode(this.version);
    hash = 17 * hash + Objects.hashCode(this.scope);
    hash = 17 * hash + Objects.hashCode(this.type);
    hash = 17 * hash + Objects.hashCode(this.classifier);
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
    if (!Objects.equals(this.groupId, other.groupId)) {
      return false;
    }
    if (!Objects.equals(this.artifactId, other.artifactId)) {
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
    if (!Objects.equals(this.classifier, other.classifier)) {
      return false;
    }
    return true;
  }

}
