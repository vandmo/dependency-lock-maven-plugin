package se.vandmo.dependencylock.maven.mojos.model;

/**
 * Readonly interface for property-based maven profile activation configuration.
 *
 * @since 1.4.0
 */
public interface IActivationProperty {
  /**
   * Returns the value criteria to use for the property activation condition.
   *
   * @return if <code>null</code> the sole fact of the property existing is used as activation
   *     condition
   */
  String getValue();

  /**
   * Returns the name of the property used as criteria for activation condition.
   *
   * @return a never <code>null</code> value
   */
  String getName();
}
