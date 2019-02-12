package se.vandmo.dependencylock.maven;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

import java.util.Objects;

public final class Artifact implements Comparable<Artifact> {

  public final ArtifactIdentifier identifier;
  public final String version;
  public final String scope;
  public final String type;

  public static Artifact from(org.apache.maven.artifact.Artifact artifact) {
    return new Artifact(
        new ArtifactIdentifier(
            artifact.getGroupId(),
            artifact.getArtifactId(),
            ofNullable(artifact.getClassifier())),
        artifact.getVersion(),
        artifact.getScope(),
        artifact.getType());
  }

  Artifact(
      ArtifactIdentifier identifier,
      String version,
      String scope,
      String type) {
    this.identifier = requireNonNull(identifier);
    this.version = requireNonNull(version);
    this.scope = requireNonNull(scope);
    this.type = requireNonNull(type);
  }

  @Override
  public int compareTo(Artifact other) {
    return toString().compareTo(other.toString());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb
        .append(identifier.toString())
        .append(':').append(version)
        .append(':').append(scope)
        .append(':').append(type);
    return sb.toString();
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
    final Artifact other = (Artifact) obj;
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

}
