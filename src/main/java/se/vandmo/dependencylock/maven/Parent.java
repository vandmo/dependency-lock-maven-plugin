package se.vandmo.dependencylock.maven;

import java.util.Objects;
import org.apache.maven.project.MavenProject;

public final class Parent extends LockableEntityWithArtifact<Parent> implements Comparable<Parent> {
  private Parent(Artifact artifact) {
    super(artifact);
  }

  public static ArtifactIdentifierBuilderStage builder() {
    return new ArtifactIdentifierBuilderStage();
  }

  public static Parent from(MavenProject project) {
    final MavenProject parentProject = project.getParent();
    if (parentProject == null) {
      return null;
    }

    final org.apache.maven.artifact.Artifact parentArtifact = project.getParentArtifact();
    String integrity = Checksum.calculateFor(parentArtifact.getFile());
    return new Parent(
        Artifact.builder()
            .artifactIdentifier(ArtifactIdentifier.from(parentArtifact))
            .version(parentArtifact.getVersion())
            .integrity(integrity)
            .build(),
        from(parentProject));
  }

  @Override
  public int compareTo(Parent o) {
    return this.artifact.compareTo(o.artifact);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Parent)) return false;
    Parent parent1 = (Parent) o;
    return Objects.equals(artifact, parent1.artifact) && Objects.equals(parent, parent1.parent);
  }

  @Override
  public int hashCode() {
    return Objects.hash(artifact, parent);
  }

  public Parent withVersion(String version) {
    return new Parent(this.artifact.withVersion(version), this.parent);
  }

  public Parent withIntegrity(Integrity integrity) {
    return new Parent(this.artifact.withIntegrity(integrity), this.parent);
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

    public ParentBuilderStage version(String version) {
      return new ParentBuilderStage(artifactBuilderStage.version(version));
    }
  }

  public static final class ParentBuilderStage {
    private final Artifact.IntegrityBuilderStage artifactBuilderStage;

    private ParentBuilderStage(Artifact.IntegrityBuilderStage artifactBuilderStage) {
      this.artifactBuilderStage = artifactBuilderStage;
    }

    public IntegrityBuilderStage parent(Parent parent) {
      return new IntegrityBuilderStage(this.artifactBuilderStage, parent);
    }
  }

  public static final class IntegrityBuilderStage {
    private final Artifact.IntegrityBuilderStage artifactBuilderStage;
    private final Parent parent;

    private IntegrityBuilderStage(
        Artifact.IntegrityBuilderStage artifactBuilderStage, Parent parent) {
      this.artifactBuilderStage = artifactBuilderStage;
      this.parent = parent;
    }

    public FinalBuilderStage integrity(String integrity) {
      return new FinalBuilderStage(artifactBuilderStage.integrity(integrity), this.parent);
    }
  }

  public static final class FinalBuilderStage {
    private final Artifact.FinalBuilderStage artifactFinalBuilderStage;
    private final Parent parent;

    private FinalBuilderStage(Artifact.FinalBuilderStage artifactFinalBuilderStage, Parent parent) {
      this.artifactFinalBuilderStage = artifactFinalBuilderStage;
      this.parent = parent;
    }

    public Parent build() {
      return new Parent(artifactFinalBuilderStage.build(), parent);
    }
  }
}
