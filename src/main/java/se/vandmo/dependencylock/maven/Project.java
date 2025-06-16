package se.vandmo.dependencylock.maven;

import java.util.Optional;

public final class Project {
  public final Optional<Parent> parent;
  public final Optional<Build> build;
  public final Dependencies dependencies;

  private Project(Optional<Parent> parent, Optional<Build> build, Dependencies dependencies) {
    this.parent = parent;
    this.build = build;
    this.dependencies = dependencies;
  }

  public static Project from(Dependencies dependencies, Build build) {
    return new Project(Optional.empty(), Optional.of(build), dependencies);
  }

  public static Project from(Dependencies dependencies, Parent parent, Build build) {
    return new Project(Optional.ofNullable(parent), Optional.of(build), dependencies);
  }

  public static Project from(Dependencies dependencies) {
    return new Project(Optional.empty(), Optional.empty(), dependencies);
  }
}
