package se.vandmo.dependencylock.maven;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class Project {
  public final Dependencies dependencies;
  public final Optional<Parents> parents;
  public final Optional<Plugins> plugins;
  public final Optional<Extensions> extensions;

  private Project(Dependencies dependencies, Optional<Parents> parents, Optional<Plugins> plugins, Optional<Extensions> extensions) {
    this.dependencies = requireNonNull(dependencies);
    this.parents = requireNonNull(parents);
    this.plugins = requireNonNull(plugins);
    this.extensions = requireNonNull(extensions);
  }

  public static Project from(Dependencies dependencies, Optional<Parents> parents, Optional<Plugins> plugins, Optional<Extensions> extensions) {
    return new Project(dependencies, parents, plugins, extensions);
  }

  public static Project from(Dependencies dependencies) {
    return new Project(dependencies, Optional.empty(), Optional.empty(), Optional.empty());
  }
}
