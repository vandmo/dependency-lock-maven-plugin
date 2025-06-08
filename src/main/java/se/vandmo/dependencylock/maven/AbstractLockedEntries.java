package se.vandmo.dependencylock.maven;

import static java.lang.String.format;
import static java.util.Locale.ROOT;
import static se.vandmo.dependencylock.maven.lang.Strings.joinNouns;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import org.apache.maven.plugin.logging.Log;

/**
 * Base class for entires in charge of performing diff checks.
 *
 * @param <EntityType> the type of entities supported by this implementation
 */
public class AbstractLockedEntries<EntityType extends LockableEntity<EntityType>> {
  final DiffHelper diffHelper;
  final Log log;

  AbstractLockedEntries(Log log) {
    this.diffHelper = new DiffHelper(log);
    this.log = log;
  }

  final List<String> diffVersion(
      AtomicReference<EntityType> lockedEntityRef,
      EntityType actualEntity,
      BiFunction<EntityType, String, EntityType> versionUpdater,
      Filters filters) {
    return diffHelper.diffVersion(lockedEntityRef, actualEntity, versionUpdater, filters);
  }

  /**
   * Computes the difference between the specified parameters and returns a list of nouns describing
   * the encountered differences.
   *
   * <p>Any detail should be printed out to the log.
   *
   * @param lockedEntityRef the reference entity being used, may be updated depending on settings
   * @param actualEntity the actual entity met
   * @param filters the filters to apply
   * @return a never <code>null</code> at worse empty list of nouns describing the encountered
   *     differences
   */
  List<String> findDiffs(
      AtomicReference<EntityType> lockedEntityRef, EntityType actualEntity, Filters filters) {
    final List<String> wrongs = new ArrayList<>();
    wrongs.addAll(diffIntegrity(lockedEntityRef, actualEntity, filters));
    return wrongs;
  }

  final String formatDifference(EntityType expected, EntityType actual, List<String> wrongs) {
    return format(ROOT, "Expected %s but found %s, wrong %s", expected, actual, joinNouns(wrongs));
  }

  final List<String> diffIntegrity(
      AtomicReference<EntityType> lockedEntityRef, EntityType actualEntity, Filters filters) {
    return diffHelper.diffIntegrity(lockedEntityRef.get(), actualEntity, filters);
  }
}
