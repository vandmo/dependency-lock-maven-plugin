package se.vandmo.dependencylock.maven;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.maven.plugin.logging.Log;

public final class LockedPlugins extends LockedEntities<Plugin> {

  private LockedPlugins(Plugins lockedPlugins, Log log) {
    super(lockedPlugins, log);
  }

  public static LockedPlugins from(Plugins plugins, Log log) {
    return new LockedPlugins(requireNonNull(plugins), log);
  }

  public DiffReport compareWith(Plugins plugins, Filters filters) {
    return super.compareWith(plugins, filters);
  }

  @Override
  List<String> findDiffs(
      AtomicReference<Plugin> lockedPluginRef, Plugin actualPlugin, Filters filters) {
    final List<String> wrongs = super.findDiffs(lockedPluginRef, actualPlugin, filters);
    wrongs.addAll(diffVersion(lockedPluginRef, actualPlugin, Plugin::withVersion, filters));
    LockedArtifacts lockedArtifacts = LockedArtifacts.from(lockedPluginRef.get().dependencies, log);
    DiffReport diff = lockedArtifacts.compareWith(actualPlugin.dependencies, filters);
    if (!diff.equals()) {
      diff.report("dependencies for " + lockedPluginRef.get().toString_withoutIntegrity())
          .forEach(log::error);
      wrongs.add("dependencies");
    }
    return wrongs;
  }
}
