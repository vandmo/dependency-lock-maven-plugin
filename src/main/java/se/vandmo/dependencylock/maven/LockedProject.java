package se.vandmo.dependencylock.maven;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.stream.Stream;
import org.apache.maven.plugin.logging.Log;

public final class LockedProject {
  public final Dependencies dependencies;
  public final Optional<Parents> parents;
  public final Optional<Plugins> plugins;
  public final Optional<Extensions> extensions;
  private final Log log;

  private LockedProject(Dependencies dependencies, Optional<Parents> parents, Optional<Plugins> plugins, Optional<Extensions> extensions, Log log) {
    this.parents = parents;
    this.dependencies = dependencies;
    this.plugins = plugins;
    this.extensions = extensions;
    this.log = log;
  }

  public static LockedProject from(Project project, Log log) {
    return new LockedProject(project.dependencies, project.parents, project.plugins, project.extensions, log);
  }

  public static LockedProject from(Dependencies dependencies, Optional<Parents> parents, Optional<Plugins> plugins, Optional<Extensions> extensions, Log log) {
    return new LockedProject(requireNonNull(dependencies), requireNonNull(parents), requireNonNull(plugins), requireNonNull(extensions), log);
  }

  public static LockedProject from(Dependencies dependencies, Log log) {
    return new LockedProject(requireNonNull(dependencies), Optional.empty(), Optional.empty(), Optional.empty(), log);
  }

  public Diff compareWith(Project project, Filters filters) {
    final DiffReport dependenciesDiff =
        LockedDependencies.from(dependencies, log)
            .compareWith(project.dependencies, filters)
            .getReport();
    return new Diff(
     dependenciesDiff,
     project.parents.map(parents -> LockedParents.from(parents, log).compareWith(this.parents.get(), filters)), // TODO avoid .get? how?
     project.plugins.map(plugins -> LockedPlugins.from(plugins, log).compareWith(this.plugins.get(), filters)),
     project.extensions.map(extensions -> LockedExtensions.from(extensions, log).compareWith(this.extensions.get(), filters)));
  }

  public static final class Diff {
    private final DiffReport dependenciesDiff;
    private final Optional<DiffReport> parentsDiff;
    private final Optional<DiffReport> pluginsDiff;
    private final Optional<DiffReport> extensionsDiff;

    Diff(DiffReport dependenciesDiff) {
      this(dependenciesDiff, Optional.empty(), Optional.empty(), Optional.empty());
    }

    Diff(
        DiffReport dependenciesDiff,
        Optional<DiffReport> parentsDiff,
        Optional<DiffReport> pluginsDiff,
        Optional<DiffReport> extensionsDiff) {
      this.dependenciesDiff = requireNonNull(dependenciesDiff);
      this.parentsDiff = requireNonNull(parentsDiff);
      this.pluginsDiff = requireNonNull(pluginsDiff);
      this.extensionsDiff = requireNonNull(extensionsDiff);
    }

    public boolean equals() {
      if (!dependenciesDiff.equals()) {
        return false;
      }
      if (parentsDiff.isPresent() && !parentsDiff.get().equals()) {
        return false;
      }
      if (pluginsDiff.isPresent() && !pluginsDiff.get().equals()) {
        return false;
      }
      if (extensionsDiff.isPresent() && !extensionsDiff.get().equals()) {
        return false;
      }
      return true;
    }

    public Stream<String> report() {
      return Stream.of(
              Optional.of(dependenciesDiff.report("dependencies")),
              parentsDiff.map(report -> report.report("parents")),
              pluginsDiff.map(report -> report.report("plugins")),
              extensionsDiff.map(report -> report.report("extensions"))).filter(Optional::isPresent).flatMap(Optional::get);
    }

    public void logTo(Log log) {
      report().forEach(log::error);
    }
  }
}
