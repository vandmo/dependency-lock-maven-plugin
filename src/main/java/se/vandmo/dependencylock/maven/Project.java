package se.vandmo.dependencylock.maven;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

public final class Project {
  public final ProfiledDependencies dependencies;
  public final Optional<Parents> parents;
  public final Optional<Plugins> plugins;
  public final Optional<Extensions> extensions;

  private Project(
      ProfiledDependencies dependencies,
      Optional<Parents> parents,
      Optional<Plugins> plugins,
      Optional<Extensions> extensions) {
    this.dependencies = requireNonNull(dependencies);
    this.parents = requireNonNull(parents);
    this.plugins = requireNonNull(plugins);
    this.extensions = requireNonNull(extensions);
  }

  public static Project from(
      Dependencies dependencies,
      Optional<Parents> parents,
      Optional<Plugins> plugins,
      Optional<Extensions> extensions) {
    return from(new ProfiledDependencies(dependencies), parents, plugins, extensions);
  }

  public static Project from(
      ProfiledDependencies dependencies,
      Optional<Parents> parents,
      Optional<Plugins> plugins,
      Optional<Extensions> extensions) {
    return new Project(dependencies, parents, plugins, extensions);
  }

  public static Project from(
      Dependencies dependencies, Parents parents, Plugins plugins, Extensions extensions) {
    return from(new ProfiledDependencies(dependencies), parents, plugins, extensions);
  }

  public static Project from(
      ProfiledDependencies dependencies, Parents parents, Plugins plugins, Extensions extensions) {
    return from(dependencies, Optional.of(parents), Optional.of(plugins), Optional.of(extensions));
  }

  public static Project from(Dependencies dependencies) {
    return from(new ProfiledDependencies(dependencies));
  }

  public static Project from(ProfiledDependencies dependencies) {
    return new Project(dependencies, Optional.empty(), Optional.empty(), Optional.empty());
  }
}
