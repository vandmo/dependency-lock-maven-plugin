package se.vandmo.dependencylock.maven;

import java.util.Optional;

public final class Project {
  public final Optional<Parents> parents;
  public final Optional<Plugins> plugins;
  public final Optional<Extensions> extensions;
  public final Dependencies dependencies;

  private Project(Optional<Parents> parents, Optional<Plugins> plugins, Optional<Extensions> extensions, Dependencies dependencies) {
    this.parents = parents;
    this.plugins = plugins;
    this.extensions = extensions;
    this.dependencies = dependencies;
  }

  public static Project from(Dependencies dependencies, Optional<Plugins> plugins, Optional<Extensions> extensions) {
    return new Project(Optional.empty(), Optional.of(build), dependencies);
  }

  public static Project from(Dependencies dependencies, Optional<Parents> parents, Optional<Plugins> plugins, Optional<Extensions> extensions) {
    return new Project(parents, Optional.of(build), dependencies);
  }

  public static Project from(Dependencies dependencies) {
    return new Project(Optional.empty(), Optional.empty(), Optional.empty(), dependencies);
  }
}
