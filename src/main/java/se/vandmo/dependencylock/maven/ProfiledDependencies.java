package se.vandmo.dependencylock.maven;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Instances of this class shall represent dependencies which are profiled based on a given platform
 * activation logic.
 */
public class ProfiledDependencies {
  private final Dependencies sharedDependencies;
  private final Collection<ProfileEntry> profileEntries;

  public ProfiledDependencies(Dependencies sharedDependencies) {
    this(sharedDependencies, Collections.emptyList());
  }

  public ProfiledDependencies(
      Dependencies sharedDependencies, Collection<ProfileEntry> profileEntries) {
    this.sharedDependencies =
        Objects.requireNonNull(sharedDependencies, "defaultArtifacts == null");
    this.profileEntries =
        new ArrayList<>(Objects.requireNonNull(profileEntries, "profileEntries == null"));
  }

  public Dependencies getSharedDependencies() {
    return this.sharedDependencies;
  }

  public Stream<ProfileEntry> profileEntries() {
    return this.profileEntries.stream();
  }

  public Stream<Artifact> artifacts() {
    return Stream.concat(
            sharedDependencies.artifacts(),
            profileEntries.stream()
                .map(ProfileEntry::getDependencies)
                .flatMap(LockableEntitiesWithArtifact::artifacts))
        .distinct();
  }

  /**
   * Returns a stream containing all the entities which should be used for the given platform.
   *
   * @param profileIds identifiers of the profiles which have been enabled
   * @return a stream of all entities set which should be managed
   */
  public Stream<Dependencies> forProfiles(String[] profileIds) {
    Stream<Dependencies> result = Stream.of(this.sharedDependencies);
    if (profileIds.length != 0) {
      Collection<String> profileIdsSet = Stream.of(profileIds).collect(Collectors.toSet());
      result =
          Stream.concat(
              result,
              this.profileEntries.stream()
                  .filter(entry -> profileIdsSet.contains(entry.getProfile().getId()))
                  .map(ProfileEntry::getDependencies));
    }
    return result;
  }
}
