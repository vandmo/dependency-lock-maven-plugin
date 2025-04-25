package se.vandmo.dependencylock.maven;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.maven.plugin.logging.Log;

public final class LockedArtifacts extends LockedEntities<Artifact> {

  private LockedArtifacts(Artifacts lockedArtifacts, Log log) {
    super(lockedArtifacts, log);
  }

  public static LockedArtifacts from(Artifacts artifacts, Log log) {
    return new LockedArtifacts(requireNonNull(artifacts), log);
  }

  public DiffReport compareWith(Artifacts artifacts, Filters filters) {
    return super.compareWith(artifacts, filters);
  }

  @Override
  List<String> findDiffs(
      AtomicReference<Artifact> lockedArtifactRef, Artifact actualArtifact, Filters filters) {
    List<String> wrongs = super.findDiffs(lockedArtifactRef, actualArtifact, filters);
    wrongs.addAll(diffVersion(lockedArtifactRef, actualArtifact, Artifact::withVersion, filters));
    return wrongs;
  }
}
