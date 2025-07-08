package se.vandmo.dependencylock.maven;

import static java.util.stream.Collectors.toList;

import java.util.Collection;

public final class Artifacts extends LockableEntities<Artifact> {

  private Artifacts(Collection<Artifact> artifacts) {
    super(artifacts, true);
  }

  public static Artifacts fromMavenArtifacts(
      Collection<org.apache.maven.artifact.Artifact> artifacts) {
    return new Artifacts(artifacts.stream().map(a -> Artifact.from(a)).collect(toList()));
  }

  public static Artifacts fromArtifacts(Collection<Artifact> artifacts) {
    return new Artifacts(artifacts);
  }
}
