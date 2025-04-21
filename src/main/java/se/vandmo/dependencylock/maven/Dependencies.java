package se.vandmo.dependencylock.maven;

import static java.util.Collections.sort;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public final class Dependencies implements Iterable<Dependency> {

  public final List<Dependency> dependencies;

  private Dependencies(Collection<Dependency> dependencies) {
    ArrayList<Dependency> copy = new ArrayList<>(dependencies);
    sort(copy);
    this.dependencies = unmodifiableList(copy);
  }

  public static Dependencies fromMavenArtifacts(
      Collection<org.apache.maven.artifact.Artifact> artifacts) {
    return new Dependencies(artifacts.stream().map(a -> Dependency.from(a)).collect(toList()));
  }

  public static Dependencies fromDependencies(Collection<Dependency> dependencies) {
    return new Dependencies(dependencies);
  }

  public Optional<Dependency> by(ArtifactIdentifier identifier) {
    for (Dependency dependency : dependencies) {
      if (identifier.equals(dependency.artifact.identifier)) {
        return Optional.of(dependency);
      }
    }
    return Optional.empty();
  }

  @Override
  public Iterator<Dependency> iterator() {
    return dependencies.iterator();
  }
}
