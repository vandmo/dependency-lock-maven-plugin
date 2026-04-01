package se.vandmo.dependencylock.maven;

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.stream.Stream;

public final class Dependencies extends LockableEntitiesWithArtifact<Dependency> {

  private Dependencies(Collection<Dependency> dependencies) {
    super(dependencies, true);
  }

  public static Dependencies fromMavenArtifacts(
      Collection<org.apache.maven.artifact.Artifact> artifacts) {
    return new Dependencies(artifacts.stream().map(a -> Dependency.from(a)).collect(toList()));
  }

  public static Dependencies merge(Stream<Dependencies> dependencies) {
    return fromDependencies(dependencies.flatMap(LockableEntities::stream).collect(toList()));
  }

  public static Dependencies fromDependencies(Collection<Dependency> dependencies) {
    return new Dependencies(dependencies);
  }
}
