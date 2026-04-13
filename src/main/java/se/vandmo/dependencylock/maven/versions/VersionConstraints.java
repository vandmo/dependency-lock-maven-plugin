package se.vandmo.dependencylock.maven.versions;

import java.util.Objects;

/**
 * @author Antoine Malliarakis
 */
public final class VersionConstraints {
  private VersionConstraints() {}

  private static final VersionConstraint USE_PROJECT_VERSION = new UseProjectVersionConstraint();
  private static final VersionConstraint IGNORE_VERSION = new IgnoreVersionConstraint();

  public static VersionConstraint useProjectVersion() {
    return USE_PROJECT_VERSION;
  }

  public static VersionConstraint ignoreVersion() {
    return IGNORE_VERSION;
  }

  public static VersionConstraint version(String version) {
    return new VersionConstraintImpl(Objects.requireNonNull(version));
  }
}
