package se.vandmo.dependencylock.maven;

import org.apache.maven.artifact.resolver.filter.ArtifactFilter;

public final class DependencySetConfiguration {
  private final ArtifactFilter includes;
  private final ArtifactFilter excludes;
  public final Version version;
  public final Integrity integrity;
  public final Boolean allowMissing;
  public final Boolean allowSuperfluous;

  public DependencySetConfiguration(
      ArtifactFilter includes,
      ArtifactFilter excludes,
      Version version,
      Integrity integrity,
      Boolean allowMissing,
      Boolean allowSuperfluous) {
    this.includes = includes;
    this.excludes = excludes;
    this.version = version;
    this.integrity = integrity;
    this.allowMissing = allowMissing;
    this.allowSuperfluous = allowSuperfluous;
  }

  public boolean matches(Dependency dependency) {
    org.apache.maven.artifact.Artifact mavenArtifact = dependency.toMavenArtifact();
    return includes.include(mavenArtifact) && !excludes.include(mavenArtifact);
  }

  public enum Version {
    check,
    useProjectVersion,
    snapshot,
    ignore
  }

  public enum Integrity {
    check,
    ignore
  }
}
