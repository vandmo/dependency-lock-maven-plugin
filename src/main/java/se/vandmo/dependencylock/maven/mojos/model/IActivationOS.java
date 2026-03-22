package se.vandmo.dependencylock.maven.mojos.model;

/**
 * Readonly interface for OS based maven profile activation configuration.
 *
 * @since 1.4.0
 */
public interface IActivationOS {
  /**
   * Returns the criteria to use for the OS's architecture.
   *
   * @return <code>null</code> if no OS architecture criteria is configured
   */
  String getArch();

  /**
   * Returns the criteria to use for the OS's family.
   *
   * @return <code>null</code> if no OS family criteria is configured
   */
  String getFamily();

  /**
   * Returns the criteria to use for the OS's name.
   *
   * @return <code>null</code> if no OS name criteria is configured
   */
  String getName();

  /**
   * Returns the criteria to use for the OS's version.
   *
   * @return <code>null</code> if no OS version criteria is configured
   */
  String getVersion();
}
