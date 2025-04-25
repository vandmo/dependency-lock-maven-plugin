package se.vandmo.dependencylock.maven;

import static java.util.Objects.requireNonNull;

import java.util.stream.Stream;

public final class Build {
  public final Plugins plugins;
  public final Extensions extensions;

  private Build(Plugins plugins, Extensions extensions) {
    this.plugins = requireNonNull(plugins);
    this.extensions = requireNonNull(extensions);
  }

  public static Build empty() {
    return from(Plugins.empty(), Extensions.empty());
  }

  public static Build from(Plugins plugins, Extensions extensions) {
    return new Build(plugins, extensions);
  }

  public Stream<Artifact> artifacts() {
    return Stream.concat(extensions.artifacts(), plugins.artifacts());
  }
}
