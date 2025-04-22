package se.vandmo.dependencylock.maven;

public final class Project {
  public final Plugins plugins;
  public final Dependencies dependencies;
  public final Extensions extensions;

  private Project(Plugins plugins, Dependencies dependencies, Extensions extensions) {
    this.plugins = plugins;
    this.dependencies = dependencies;
    this.extensions = extensions;
  }

  public static Project from(Plugins plugins, Dependencies dependencies, Extensions extensions) {
    return new Project(plugins, dependencies, extensions);
  }
}
