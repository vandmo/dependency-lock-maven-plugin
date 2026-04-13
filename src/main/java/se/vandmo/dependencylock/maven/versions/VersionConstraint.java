package se.vandmo.dependencylock.maven.versions;

/**
 * Instances of this class shall be able to represent the constraint on a given artifact's version.
 */
public abstract class VersionConstraint {

  VersionConstraint() {
    super();
  }

  /**
   * Have the specified visitor parameter visit this instance with the given context parameter.
   *
   * @param visitor what should be processing this instance
   * @param context the context for this instance's processing
   * @return the result of the given visitor's visting of this instance
   * @param <T> the type of data returned by the specified visitor parameter
   * @param <C> the type of context supported by the specified visitor parameter
   * @throws NullPointerException if the specified visitor parameter is <code>null</code>
   */
  public abstract <T, C> T accept(VersionConstraintVisitor<T, C> visitor, C context);

  /**
   * Returns <code>true</code> if this constraint is compliant with the given constraint.
   *
   * @param other the constraint against which this constraint is to be compared.
   * @param context the context in which the comparison is to be performed.
   * @return true if, and only if, the given constraint could be satisfied by this constraint.
   */
  public abstract boolean compliantWith(VersionConstraint other, VersionConstraintContext context);

  public abstract String toString();
}
