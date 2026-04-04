package se.vandmo.dependencylock.maven;

import java.util.Objects;
import se.vandmo.dependencylock.maven.versions.VersionConstraint;
import se.vandmo.dependencylock.maven.versions.VersionConstraints;

/**
 * Instances of this class shall represent a versioned entity which can be checked for integrity.
 */
public abstract class LockableEntity<T extends LockableEntity<T>> {

  LockableEntity() {
    super();
  }

  public T withVersion(String version) {
    return withVersion(
        VersionConstraints.version(Objects.requireNonNull(version, "version == null")));
  }

  public abstract T withVersion(VersionConstraint version);

  public abstract T withIntegrity(Integrity integrity);

  public abstract Integrity getIntegrity();

  public abstract VersionConstraint getVersion();

  public abstract String getArtifactKey();

  public abstract ArtifactIdentifier getArtifactIdentifier();

  public abstract String toString_withoutIntegrity();

  public abstract org.apache.maven.artifact.Artifact getMavenArtifact();

  public abstract String getIntegrityForLockFile();
}
