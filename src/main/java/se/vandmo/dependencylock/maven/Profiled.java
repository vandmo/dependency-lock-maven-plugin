package se.vandmo.dependencylock.maven;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Instances of this class shall represent entries which are profiled based on a given platform
 * identifier.
 */
public class Profiled<
    EntityType extends LockableEntity<EntityType>, SetType extends LockableEntities<EntityType>> {
  private final SetType defaultEntities;
  private final Map<String, SetType> byProfile;

  public Profiled(SetType defaultEntities) {
    this(defaultEntities, Collections.emptyMap());
  }

  public Profiled(SetType defaultEntities, Map<String, SetType> byProfileId) {
    this.defaultEntities = Objects.requireNonNull(defaultEntities, "defaultArtifacts == null");
    this.byProfile = new HashMap<>(Objects.requireNonNull(byProfileId, "byProfile == null"));
  }

  public SetType getDefaultEntities() {
    return this.defaultEntities;
  }

  public Stream<Map.Entry<String, SetType>> profileEntries() {
    return this.byProfile.entrySet().stream();
  }

  public Stream<Artifact> artifacts() {
    return Stream.concat(
            defaultEntities.artifacts(), byProfile.values().stream().flatMap(SetType::artifacts))
        .distinct();
  }

  /**
   * Returns a stream containing all the entities which should be used for the given platform.
   *
   * @param profileIds identifiers of the profiles which have been enabled
   * @return a stream of all entities set which should be managed
   */
  public Stream<SetType> forProfiles(String[] profileIds) {
    Stream<SetType> result = Stream.of(this.defaultEntities);
    if (profileIds.length != 0) {
      result =
          Stream.concat(
              result,
              Stream.of(profileIds)
                  .flatMap(
                      profileId ->
                          Optional.ofNullable(this.byProfile.get(profileId))
                              .map(Stream::of)
                              .orElse(Stream.empty())));
    }
    return result;
  }
}
