package se.vandmo.dependencylock.maven.services;

import java.util.Objects;
import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationOS;
import org.apache.maven.model.ActivationProperty;
import se.vandmo.dependencylock.maven.mojos.model.IActivation;
import se.vandmo.dependencylock.maven.mojos.model.IActivationOS;
import se.vandmo.dependencylock.maven.mojos.model.IActivationProperty;

/** Materialization of the activation configuration which can be used as a key object in a map. */
final class ActivationKey implements IActivation {
  private final ActivationOSKey os;
  private final ActivationPropertyKey property;

  ActivationKey(Activation activation) {
    this(activation.getOs(), activation.getProperty());
  }

  boolean isEmpty() {
    return (os == null || os.isEmpty()) && property == null;
  }

  private ActivationKey(ActivationOS os, ActivationProperty property) {
    this(
        os == null ? null : new ActivationOSKey(os),
        property == null ? null : new ActivationPropertyKey(property));
  }

  private ActivationKey(ActivationOSKey os, ActivationPropertyKey property) {
    this.os = os;
    this.property = property;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ActivationKey)) return false;
    ActivationKey that = (ActivationKey) o;
    return Objects.equals(os, that.os) && Objects.equals(property, that.property);
  }

  @Override
  public int hashCode() {
    return Objects.hash(os, property);
  }

  @Override
  public IActivationProperty getProperty() {
    return this.property;
  }

  @Override
  public IActivationOS getOs() {
    return this.os;
  }
}
