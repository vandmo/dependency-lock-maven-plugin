package se.vandmo.dependencylock.maven;

import static java.util.Objects.requireNonNull;

import java.util.stream.Stream;
import org.apache.maven.plugin.logging.Log;

public final class LockedBuild {
  public final Plugins plugins;
  public final Extensions extensions;
  private final Log log;

  private LockedBuild(Extensions extensions, Plugins plugins, Log log) {
    this.extensions = extensions;
    this.plugins = plugins;
    this.log = log;
  }

  public static LockedBuild from(Build build, Log log) {
    return from(build.plugins, build.extensions, log);
  }

  public static LockedBuild from(Plugins plugins, Extensions extensions, Log log) {
    return new LockedBuild(requireNonNull(extensions), requireNonNull(plugins), log);
  }

  public Diff compareWith(Build build, Filters filters) {
    DiffReport pluginsDiff = LockedPlugins.from(plugins, log).compareWith(build.plugins, filters);
    DiffReport extensionsDiff =
        LockedExtensions.from(extensions, log).compareWith(build.extensions, filters);
    return new Diff(pluginsDiff, extensionsDiff);
  }

  public static final class Diff {
    private final DiffReport pluginsDiff;
    private final DiffReport extensionsDiff;

    Diff(DiffReport pluginsDiff, DiffReport extensionsDiff) {
      this.pluginsDiff = pluginsDiff;
      this.extensionsDiff = extensionsDiff;
    }

    public boolean equals() {
      return pluginsDiff.equals() && extensionsDiff.equals();
    }

    public Stream<String> report() {
      return Stream.concat(pluginsDiff.report("plugins"), extensionsDiff.report("extensions"));
    }
  }
}
