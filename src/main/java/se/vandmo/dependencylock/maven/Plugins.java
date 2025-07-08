package se.vandmo.dependencylock.maven;

import static java.util.Collections.emptyList;

import java.util.Collection;
import java.util.stream.Stream;

public final class Plugins extends LockableEntitiesWithArtifact<Plugin> {

  private Plugins(Collection<Plugin> plugins) {
    super(plugins, true);
  }

  public static Plugins empty() {
    return from(emptyList());
  }

  public static Plugins from(Collection<Plugin> plugins) {
    return new Plugins(plugins);
  }

  @Override
  public Stream<Artifact> artifacts() {
    return stream().flatMap(p -> Stream.concat(Stream.of(p.artifact), p.dependencies.stream()));
  }
}
