package se.vandmo.dependencylock.maven.versions;

/**
 * @author Antoine Malliarakis
 */
public interface VersionConstraintVisitor<T, C> {
  T onVersion(String version, C context);

  T onProjectVersion(C context);

  T onIgnoreVersion(C context);
}
