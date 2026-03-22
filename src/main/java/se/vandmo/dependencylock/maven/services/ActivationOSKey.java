package se.vandmo.dependencylock.maven.services;

import java.util.Objects;
import org.apache.maven.model.ActivationOS;
import se.vandmo.dependencylock.maven.lang.Strings;
import se.vandmo.dependencylock.maven.mojos.model.IActivationOS;

/**
 * Materialization of the activation os configuration which can be used as a key object in a map.
 */
final class ActivationOSKey implements IActivationOS {
  private final String family;
  private final String arch;
  private final String version;
  private final String name;

  ActivationOSKey(ActivationOS src) {
    this(src.getFamily(), src.getArch(), src.getVersion(), src.getName());
  }

  private ActivationOSKey(String family, String arch, String version, String name) {
    this.family = family;
    this.arch = arch;
    this.version = version;
    this.name = name;
  }

  boolean isEmpty() {
    return Strings.isBlank(family)
        && Strings.isBlank(arch)
        && Strings.isBlank(version)
        && Strings.isBlank(name);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ActivationOSKey)) return false;
    ActivationOSKey that = (ActivationOSKey) o;
    return Objects.equals(family, that.family)
        && Objects.equals(arch, that.arch)
        && Objects.equals(version, that.version)
        && Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(family, arch, version, name);
  }

  @Override
  public String getArch() {
    return this.arch;
  }

  @Override
  public String getFamily() {
    return this.family;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public String getVersion() {
    return this.version;
  }
}
