package se.vandmo.dependencylock.maven;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

import java.util.Objects;
import java.util.Optional;

public final class ArtifactIdentifier implements Comparable<ArtifactIdentifier> {

  public final String groupId;
  public final String artifactId;
  public final Optional<String> classifier;

  public static ArtifactIdentifier from(org.apache.maven.artifact.Artifact artifact) {
    return new ArtifactIdentifier(
        artifact.getGroupId(),
        artifact.getArtifactId(),
        ofNullable(artifact.getClassifier()));
  }

  ArtifactIdentifier(
      String groupId,
      String artifactId,
      Optional<String> classifier) {
    this.groupId = requireNonNull(groupId);
    this.artifactId = requireNonNull(artifactId);
    this.classifier = requireNonNull(classifier);
  }

  @Override
  public int compareTo(ArtifactIdentifier other) {
    return toString().compareTo(other.toString());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb
        .append(groupId)
        .append(':').append(artifactId);
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
    final ArtifactIdentifier other = (ArtifactIdentifier) obj;
    if (!Objects.equals(this.groupId, other.groupId)) {
      return false;
    }
    if (!Objects.equals(this.artifactId, other.artifactId)) {
      return false;
    }
    if (!Objects.equals(this.classifier, other.classifier)) {
      return false;
    }
    return true;
  }

}
