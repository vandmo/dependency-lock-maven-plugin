package se.vandmo.dependencylock.maven;

/**
 * Instances of this class shall represent a versioned entity which can be checked for integrity.
 */
public abstract class LockableEntity<T extends LockableEntity<T>> {

  LockableEntity() {
    super();
  }

  public abstract T withVersion(String version);

  public abstract T withIntegrity(Integrity integrity);

  public abstract Integrity getIntegrity();

  public abstract String getVersion();

  public abstract String getArtifactKey();

  public abstract ArtifactIdentifier getArtifactIdentifier();

  public abstract String toString_withoutIntegrity();

  public abstract org.apache.maven.artifact.Artifact getMavenArtifact();

  public abstract String getIntegrityForLockFile();
}
