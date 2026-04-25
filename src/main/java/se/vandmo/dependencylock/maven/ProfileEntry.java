package se.vandmo.dependencylock.maven;

import java.util.Objects;
import se.vandmo.dependencylock.maven.mojos.model.Profile;

/**
 * Instances of this class shall represent the declaration of a profile combined with the associated
 * specific dependencies.
 */
public final class ProfileEntry {
  private final Profile profile;
  private final Dependencies dependencies;

  public ProfileEntry(Profile profile, Dependencies dependencies) {
    super();
    this.profile = new Profile(Objects.requireNonNull(profile, "profile == null"));
    this.dependencies = dependencies;
  }

  public Dependencies getDependencies() {
    return dependencies;
  }

  public Profile getProfile() {
    return profile;
  }
}
