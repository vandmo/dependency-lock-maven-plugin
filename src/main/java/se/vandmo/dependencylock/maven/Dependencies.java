package se.vandmo.dependencylock.maven;

import static java.util.stream.Collectors.toList;

import java.util.Collection;

public final class Dependencies extends LockableEntitiesWithArtifact<Dependency> {

  private Dependencies(Collection<Dependency> dependencies) {
    super(dependencies);
  }

  public static Dependencies fromMavenArtifacts(
      Collection<org.apache.maven.artifact.Artifact> artifacts) {
    return new Dependencies(artifacts.stream().map(a -> Dependency.from(a)).collect(toList()));
  }

  public static Dependencies fromDependencies(Collection<Dependency> dependencies) {
    return new Dependencies(dependencies);
  }
}
