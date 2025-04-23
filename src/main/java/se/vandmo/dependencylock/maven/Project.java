package se.vandmo.dependencylock.maven;

import java.util.Optional;

public final class Project {
  public final Optional<Build> build;
  public final Dependencies dependencies;

  private Project(Optional<Build> build, Dependencies dependencies) {
    this.build = build;
    this.dependencies = dependencies;
  }

  public static Project from(Dependencies dependencies, Build build) {
    return new Project(Optional.of(build), dependencies);
  }

  public static Project from(Dependencies dependencies) {
    return new Project(Optional.empty(), dependencies);
  }
}
