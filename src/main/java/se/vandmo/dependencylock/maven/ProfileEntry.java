package se.vandmo.dependencylock.maven;

import java.util.Objects;
import se.vandmo.dependencylock.maven.mojos.model.Profile;

/** */
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
