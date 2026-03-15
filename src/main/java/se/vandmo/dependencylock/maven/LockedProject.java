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

  private LockedProject(
      Dependencies dependencies,
      Optional<Parents> parents,
      Optional<Plugins> plugins,
      Optional<Extensions> extensions) {
    this.parents = parents;
    this.dependencies = dependencies;
    this.plugins = plugins;
    this.extensions = extensions;
  }

  public static LockedProject from(Project project) {
    return new LockedProject(
        project.dependencies, project.parents, project.plugins, project.extensions);
  }

  public static LockedProject from(
      Dependencies dependencies,
      Optional<Parents> parents,
      Optional<Plugins> plugins,
      Optional<Extensions> extensions) {
    return new LockedProject(
        requireNonNull(dependencies),
        requireNonNull(parents),
        requireNonNull(plugins),
        requireNonNull(extensions));
  }

  public static LockedProject from(Dependencies dependencies) {
    return new LockedProject(
        requireNonNull(dependencies), Optional.empty(), Optional.empty(), Optional.empty());
  }

  public static final class Diff {
    private final DiffReport dependenciesDiff;
    private final Optional<DiffReport> parentsDiff;
    private final Optional<DiffReport> pluginsDiff;
    private final Optional<DiffReport> extensionsDiff;

    Diff(DiffReport dependenciesDiff) {
      this(dependenciesDiff, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public Diff(
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
              extensionsDiff.map(report -> report.report("extensions")))
          .filter(Optional::isPresent)
          .flatMap(Optional::get);
    }

    public void logTo(Log log) {
      report().forEach(log::error);
    }
  }
}
