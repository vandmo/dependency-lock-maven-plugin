package io.mvnpm.maven.locker;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

import java.util.Objects;
import java.util.Optional;

public final class ArtifactIdentifier implements Comparable<ArtifactIdentifier> {

  public static final String DEFAULT_TYPE = "jar";

  public final String groupId;
  public final String artifactId;
  public final Optional<String> classifier;
  public final String type;

  public static ArtifactIdentifier from(org.apache.maven.artifact.Artifact artifact) {
    return new ArtifactIdentifier(
        artifact.getGroupId(),
        artifact.getArtifactId(),
        ofNullable(artifact.getClassifier()),
        ofNullable(artifact.getType()).orElse(DEFAULT_TYPE));
  }

  private ArtifactIdentifier(
      String groupId, String artifactId, Optional<String> classifier, String type) {
    this.groupId = requireNonNull(groupId);
    this.artifactId = requireNonNull(artifactId);
    this.classifier = requireNonNull(classifier);
    this.type = requireNonNull(type);
  }

  public String key() {
    return groupId + "--" + artifactId;
  }

  public static GroupIdBuilderStage builder() {
    return new GroupIdBuilderStage();
  }

  public static final class GroupIdBuilderStage {
    private GroupIdBuilderStage() {}

    public ArtifactIdBuilderStage groupId(String groupId) {
      return new ArtifactIdBuilderStage(requireNonNull(groupId));
    }
  }

  public static final class ArtifactIdBuilderStage {
    private final String groupId;

    private ArtifactIdBuilderStage(String groupId) {
      this.groupId = groupId;
    }

    public ClassifierBuilderStage artifactId(String artifactId) {
      return new ClassifierBuilderStage(groupId, requireNonNull(artifactId));
    }
  }

  public static final class ClassifierBuilderStage {
    private final String groupId;
    private final String artifactId;

    private ClassifierBuilderStage(String groupId, String artifactId) {
      this.groupId = groupId;
      this.artifactId = artifactId;
    }

    public TypeBuilderStage classifier(String classifier) {
      return new TypeBuilderStage(groupId, artifactId, Optional.of(classifier));
    }

    public TypeBuilderStage classifier(Optional<String> possiblyClassifier) {
      return new TypeBuilderStage(groupId, artifactId, requireNonNull(possiblyClassifier));
    }

    public FinalBuilderStage type(String type) {
      return new TypeBuilderStage(groupId, artifactId, Optional.empty()).type(type);
    }

    public FinalBuilderStage type(Optional<String> possiblyType) {
      return new TypeBuilderStage(groupId, artifactId, Optional.empty()).type(possiblyType);
    }

    public ArtifactIdentifier build() {
      return new ArtifactIdentifier(groupId, artifactId, Optional.empty(), DEFAULT_TYPE);
    }
  }

  public static final class TypeBuilderStage {
    private final String groupId;
    private final String artifactId;
    private final Optional<String> classifier;

    private TypeBuilderStage(String groupId, String artifactId, Optional<String> classifier) {
      this.groupId = groupId;
      this.artifactId = artifactId;
      this.classifier = classifier;
    }

    public FinalBuilderStage type(String type) {
      return new FinalBuilderStage(
          groupId, artifactId, classifier, Optional.of(type).orElse(DEFAULT_TYPE));
    }

    public FinalBuilderStage type(Optional<String> possiblyType) {
      return new FinalBuilderStage(
          groupId, artifactId, classifier, requireNonNull(possiblyType).orElse(DEFAULT_TYPE));
    }

    public ArtifactIdentifier build() {
      return new ArtifactIdentifier(groupId, artifactId, classifier, DEFAULT_TYPE);
    }
  }

  public static final class FinalBuilderStage {
    private final String groupId;
    private final String artifactId;
    private final Optional<String> classifier;
    private final String type;

    private FinalBuilderStage(
        String groupId, String artifactId, Optional<String> classifier, String type) {
      this.groupId = groupId;
      this.artifactId = artifactId;
      this.classifier = classifier;
      this.type = type;
    }

    public ArtifactIdentifier build() {
      return new ArtifactIdentifier(groupId, artifactId, classifier, type);
    }
  }

  @Override
  public int compareTo(ArtifactIdentifier other) {
    return toString().compareTo(other.toString());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(groupId).append(':').append(artifactId);
    classifier.ifPresent(
        actualClassifier -> {
          sb.append(':').append(actualClassifier);
        });
    sb.append(':').append(type);
    return sb.toString();
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 17 * hash + Objects.hashCode(this.groupId);
    hash = 17 * hash + Objects.hashCode(this.artifactId);
    hash = 17 * hash + Objects.hashCode(this.classifier);
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
    if (!Objects.equals(this.type, other.type)) {
      return false;
    }
    return true;
  }
}
