package se.vandmo.dependencylock.maven.mojos.model;

/**
 * Readonly interface for maven profile activation configuration.
 *
 * @since 1.4.0
 */
public interface IActivation {
  /**
   * Returns property-based activation configuration, if any.
   *
   * @return <code>null</code> if no property based activation is configured
   */
  IActivationProperty getProperty();

  /**
   * Returns OS-based activation configuration, if any.
   *
   * @return <code>null</code> if no OS based activation is configured
   */
  IActivationOS getOs();
}
