package se.vandmo.dependencylock.maven;

import static java.util.Objects.requireNonNull;

import java.util.stream.Stream;
import org.apache.maven.plugin.logging.Log;

public final class LockedProject {
  public final Plugins plugins;
  public final Dependencies dependencies;
  public final Extensions extensions;
  private final Log log;

  private LockedProject(
      Plugins plugins, Dependencies dependencies, Extensions extensions, Log log) {
    this.dependencies = dependencies;
    this.extensions = extensions;
    this.plugins = plugins;
    this.log = log;
  }

  public static LockedProject from(Project project, Log log) {
    return from(project.plugins, project.dependencies, project.extensions, log);
  }

  public static LockedProject from(
      Plugins plugins, Dependencies dependencies, Extensions extensions, Log log) {
    return new LockedProject(
        requireNonNull(plugins), requireNonNull(dependencies), requireNonNull(extensions), log);
  }

  public Diff compareWith(Project project, Filters filters) {
    DiffReport pluginsDiff = LockedPlugins.from(plugins, log).compareWith(project.plugins, filters);
    DiffReport dependenciesDiff =
        LockedDependencies.from(dependencies, log)
            .compareWith(project.dependencies, filters)
            .getReport();
    DiffReport extensionsDiff =
        LockedExtensions.from(extensions, log).compareWith(project.extensions, filters);
    return new Diff(pluginsDiff, dependenciesDiff, extensionsDiff);
  }

  public static final class Diff {
    private final DiffReport pluginsDiff;
    private final DiffReport dependenciesDiff;
    private final DiffReport extensionsDiff;

    Diff(DiffReport pluginsDiff, DiffReport dependenciesDiff, DiffReport extensionsDiff) {
      this.pluginsDiff = pluginsDiff;
      this.dependenciesDiff = dependenciesDiff;
      this.extensionsDiff = extensionsDiff;
    }

    public boolean equals() {
      return pluginsDiff.equals() && dependenciesDiff.equals() && extensionsDiff.equals();
    }

    public Stream<String> report() {
      return Stream.concat(
          dependenciesDiff.report("dependencies"),
          Stream.concat(pluginsDiff.report("plugins"), extensionsDiff.report("extensions")));
    }

    public void logTo(Log log) {
      report().forEach(log::error);
    }
  }
}
