package se.vandmo.dependencylock.maven;

import static java.lang.String.format;
import static java.util.Locale.ROOT;
import static java.util.Objects.requireNonNull;
import static se.vandmo.dependencylock.maven.lang.Strings.joinNouns;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.maven.plugin.logging.Log;

public final class LockedPlugins {

  public final Plugins lockedPlugins;
  private final DiffHelper diffHelper;
  private final Log log;

  private LockedPlugins(Plugins lockedPlugins, Log log) {
    this.lockedPlugins = lockedPlugins;
    this.diffHelper = new DiffHelper(log);
    this.log = log;
  }

  public static LockedPlugins from(Plugins plugins, Log log) {
    return new LockedPlugins(requireNonNull(plugins), log);
  }

  public DiffReport compareWith(Plugins plugins, Filters filters) {
    LockFileExpectationsDiff expectationsDiff = new LockFileExpectationsDiff(plugins, filters);
    List<String> extraneous = findExtraneous(plugins, filters);
    return new DiffReport(expectationsDiff.different, expectationsDiff.missing, extraneous);
  }

  private final class LockFileExpectationsDiff {
    private List<String> missing = new ArrayList<>();
    private List<String> different = new ArrayList<>();

    private LockFileExpectationsDiff(Plugins plugins, Filters filters) {
      for (Plugin lockedPlugin : lockedPlugins) {
        final ArtifactIdentifier identifier = lockedPlugin.getArtifactIdentifier();
        Optional<Plugin> possiblyOtherArtifact = plugins.by(identifier);
        if (!possiblyOtherArtifact.isPresent()) {
          if (filters.allowMissing(lockedPlugin)) {
            log.info(format(ROOT, "Ignoring missing %s", identifier));
          } else {
            missing.add(identifier.toString());
          }
        } else {
          Plugin actualPlugin = possiblyOtherArtifact.get();
          AtomicReference<Plugin> lockedPluginRef = new AtomicReference<>(lockedPlugin);
          List<String> wrongs = findDiffs(lockedPluginRef, actualPlugin, filters);
          if (!wrongs.isEmpty()) {
            different.add(
                format(
                    ROOT,
                    "Expected %s but found %s, wrong %s",
                    lockedPluginRef.get(),
                    actualPlugin,
                    joinNouns(wrongs)));
          }
        }
      }
    }

    private List<String> findDiffs(
        AtomicReference<Plugin> lockedDependencyRef, Plugin actualDependency, Filters filters) {
      List<String> wrongs = new ArrayList<>();
      wrongs.addAll(diffHelper.diffIntegrity(lockedDependencyRef.get(), actualDependency, filters));
      wrongs.addAll(
          diffHelper.diffVersion(
              lockedDependencyRef, actualDependency, Plugin::withVersion, filters));
      LockedArtifacts lockedArtifacts =
          LockedArtifacts.from(lockedDependencyRef.get().dependencies, log);
      DiffReport diff = lockedArtifacts.compareWith(actualDependency.dependencies, filters);
      if (!diff.equals()) {
        diff.report("dependencies for " + lockedDependencyRef.get().toString_withoutIntegrity())
            .forEach(log::error);
        wrongs.add("dependencies");
      }
      return wrongs;
    }
  }

  private List<String> findExtraneous(Plugins plugins, Filters filters) {
    return diffHelper.findExtraneous(plugins, lockedPlugins, filters);
  }
}
