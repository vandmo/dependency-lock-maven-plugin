package se.vandmo.dependencylock.maven;

import java.util.Objects;

public final class Parent extends LockableEntityWithArtifact<Parent> implements Comparable<Parent> {
  private Parent(Artifact artifact) {
    super(artifact);
  }

  public static ArtifactIdentifierBuilderStage builder() {
    return new ArtifactIdentifierBuilderStage();
  }

  @Override
  public int compareTo(Parent o) {
    return this.artifact.compareTo(o.artifact);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Parent)) return false;
    Parent parent1 = (Parent) o;
    return Objects.equals(artifact, parent1.artifact);
  }

  @Override
  public int hashCode() {
    return Objects.hash(artifact);
  }

  public Parent withVersion(String version) {
    return new Parent(this.artifact.withVersion(version));
  }

  public Parent withIntegrity(Integrity integrity) {
    return new Parent(this.artifact.withIntegrity(integrity));
  }

  public static final class ArtifactIdentifierBuilderStage {
    private ArtifactIdentifierBuilderStage() {}

    public VersionBuilderStage artifactIdentifier(ArtifactIdentifier artifactIdentifier) {
      return new VersionBuilderStage(Artifact.builder().artifactIdentifier(artifactIdentifier));
    }
  }

  public static final class VersionBuilderStage {
    private final Artifact.VersionBuilderStage artifactBuilderStage;

    private VersionBuilderStage(Artifact.VersionBuilderStage artifactBuilderStage) {
      this.artifactBuilderStage = artifactBuilderStage;
    }

    public IntegrityBuilderStage version(String version) {
      return new IntegrityBuilderStage(artifactBuilderStage.version(version));
    }
  }

  public static final class IntegrityBuilderStage {
    private final Artifact.IntegrityBuilderStage artifactBuilderStage;

    private IntegrityBuilderStage(Artifact.IntegrityBuilderStage artifactBuilderStage) {
      this.artifactBuilderStage = artifactBuilderStage;
    }

    public FinalBuilderStage integrity(String integrity) {
      return new FinalBuilderStage(artifactBuilderStage.integrity(integrity));
    }

    public FinalBuilderStage integrity(Integrity integrity) {
      return new FinalBuilderStage(artifactBuilderStage.integrity(integrity));
    }
  }

  public static final class FinalBuilderStage {
    private final Artifact.FinalBuilderStage artifactFinalBuilderStage;

    private FinalBuilderStage(Artifact.FinalBuilderStage artifactFinalBuilderStage) {
      this.artifactFinalBuilderStage = artifactFinalBuilderStage;
    }

    public Parent build() {
      return new Parent(artifactFinalBuilderStage.build());
    }
  }
}
