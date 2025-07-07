package se.vandmo.dependencylock.maven;

import static java.util.Objects.requireNonNull;

/** Instances of this class shall represent a lockable entity which is attached to an artifact. */
public abstract class LockableEntityWithArtifact<T extends LockableEntityWithArtifact<T>>
    extends LockableEntity<T> {
  final Artifact artifact;
  private org.apache.maven.artifact.Artifact mavenArtifact;

  LockableEntityWithArtifact(Artifact artifact) {
    super();
    this.artifact = requireNonNull(artifact);
  }

  @Override
  public abstract T withIntegrity(Integrity integrity);

  @Override
  public abstract T withVersion(String version);

  @Override
  public final Integrity getIntegrity() {
    return artifact.integrity;
  }

  @Override
  public final String getVersion() {
    return this.artifact.version;
  }

  @Override
  public final String getArtifactKey() {
    return artifact.getArtifactKey();
  }

  @Override
  public final ArtifactIdentifier getArtifactIdentifier() {
    return artifact.identifier;
  }

  @Override
  public String toString_withoutIntegrity() {
    return artifact.toString_withoutIntegrity();
  }

  @Override
  public final org.apache.maven.artifact.Artifact getMavenArtifact() {
    org.apache.maven.artifact.Artifact result = mavenArtifact;
    if (null == result) {
      result = toMavenArtifact();
      mavenArtifact = result;
    }
    return result;
  }

  org.apache.maven.artifact.Artifact toMavenArtifact() {
    return MavenArtifact.unscoped(artifact);
  }

  @Override
  public final String getIntegrityForLockFile() {
    return artifact.getIntegrityForLockFile();
  }

  protected StringBuilder toStringBuilder_withoutIntegrity() {
    return artifact.toStringBuilder_withoutIntegrity();
  }

  @Override
  public String toString() {
    return toStringBuilder_withoutIntegrity()
        .append('@')
        .append(
            artifact
                .integrity
                .<String>matching()
                .Calculated((calculated) -> calculated.checksum)
                .Folder((folder) -> "<Folder>")
                .Ignored((ignored) -> "<Ignored>")
                .get())
        .toString();
  }
}
