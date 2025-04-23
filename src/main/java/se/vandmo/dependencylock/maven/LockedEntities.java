package se.vandmo.dependencylock.maven;

import static java.lang.String.format;
import static java.util.Locale.ROOT;
import static java.util.Objects.requireNonNull;
import static se.vandmo.dependencylock.maven.lang.Strings.joinNouns;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import org.apache.maven.plugin.logging.Log;

public class LockedEntities<EntityType extends LockableEntity> {
  public final LockableEntities<EntityType> lockedEntities;
  private final DiffHelper diffHelper;
  final Log log;

  LockedEntities(LockableEntities<EntityType> lockedEntities, Log log) {
    this.lockedEntities = requireNonNull(lockedEntities);
    this.diffHelper = new DiffHelper(log);
    this.log = log;
  }

  public DiffReport compareWith(LockableEntities<EntityType> actualEntities, Filters filters) {
    List<String> missing = new ArrayList<>();
    List<String> different = new ArrayList<>();
    collectDiff(actualEntities, filters, different, missing);
    List<String> extraneous = diffHelper.findExtraneous(actualEntities, lockedEntities, filters);
    return new DiffReport(different, missing, extraneous);
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

  final List<String> diffIntegrity(
      AtomicReference<EntityType> lockedEntityRef, EntityType actualEntity, Filters filters) {
    return diffHelper.diffIntegrity(lockedEntityRef.get(), actualEntity, filters);
  }

  final List<String> diffVersion(
      AtomicReference<EntityType> lockedEntityRef,
      EntityType actualEntity,
      BiFunction<EntityType, String, EntityType> versionUpdater,
      Filters filters) {
    return diffHelper.diffVersion(lockedEntityRef, actualEntity, versionUpdater, filters);
  }

  private void collectDiff(
      LockableEntities<EntityType> actualEntities,
      Filters filters,
      List<String> different,
      List<String> missing) {
    for (EntityType lockedEntity : lockedEntities) {
      final ArtifactIdentifier identifier = lockedEntity.getArtifactIdentifier();
      final Optional<EntityType> possiblyOtherEntity = actualEntities.by(identifier);
      if (!possiblyOtherEntity.isPresent()) {
        if (filters.allowMissing(lockedEntity)) {
          log.info(format(ROOT, "Ignoring missing %s", identifier));
        } else {
          missing.add(identifier.toString());
        }
      } else {
        EntityType actualEntity = possiblyOtherEntity.get();
        AtomicReference<EntityType> lockedEntityRef = new AtomicReference<>(lockedEntity);
        List<String> wrongs = findDiffs(lockedEntityRef, actualEntity, filters);
        if (!wrongs.isEmpty()) {
          different.add(
              format(
                  ROOT,
                  "Expected %s but found %s, wrong %s",
                  lockedEntityRef.get(),
                  actualEntity,
                  joinNouns(wrongs)));
        }
      }
    }
  }
}
