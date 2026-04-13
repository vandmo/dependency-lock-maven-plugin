package se.vandmo.dependencylock.maven.versions;

/**
 * Implementations of this interface shall provide with information which can be used in version
 * constraint evaluation.
 */
public interface VersionConstraintContext {
  String getProjectVersion();
}
