package se.vandmo.dependencylock.maven;

import static java.util.Optional.ofNullable;

import org.apache.maven.artifact.Artifact;
import se.vandmo.dependencylock.maven.versions.VersionConstraint;
import se.vandmo.dependencylock.maven.versions.VersionConstraints;

/** A reference to an artifact. */
public class ArtifactReference {
  public final ArtifactIdentifier identifier;
  public final VersionConstraint version;

  protected ArtifactReference(ArtifactIdentifier identifier, VersionConstraint version) {
    this.identifier = identifier;
    this.version = version;
  }

  public static ArtifactReference from(Artifact artifact) {
    return new ArtifactReference(
        ArtifactIdentifier.builder()
            .groupId(artifact.getGroupId())
            .artifactId(artifact.getArtifactId())
            .classifier(ofNullable(artifact.getClassifier()))
            .type(ofNullable(artifact.getType()))
            .build(),
        VersionConstraints.version(artifact.getVersion()));
  }

  public VersionConstraint getVersion() {
    return version;
  }

  public String getArtifactKey() {
    return identifier.toString() + ':' + version;
  }

  public ArtifactIdentifier getArtifactIdentifier() {
    return identifier;
  }
}
