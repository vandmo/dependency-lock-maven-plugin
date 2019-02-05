package se.vandmo.dependencylock.maven;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

import java.util.Objects;
import java.util.Optional;

public final class Artifact implements Comparable<Artifact> {

  public final String groupId;
  public final String artifactId;
  public final String version;
  public final String scope;
  public final String type;
  public final Optional<String> classifier;

  public static Artifact from(org.apache.maven.artifact.Artifact artifact) {
    return new Artifact(
        artifact.getGroupId(),
        artifact.getArtifactId(),
        artifact.getVersion(),
        artifact.getScope(),
        artifact.getType(),
        ofNullable(artifact.getClassifier()));
  }

  Artifact(
      String groupId,
      String artifactId,
      String version,
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

  @Override
  public int compareTo(Artifact other) {
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
    final Artifact other = (Artifact) obj;
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
