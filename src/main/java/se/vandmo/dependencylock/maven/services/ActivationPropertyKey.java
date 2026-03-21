package se.vandmo.dependencylock.maven.services;

import org.apache.maven.model.ActivationProperty;
import se.vandmo.dependencylock.maven.mojos.model.IActivationProperty;

/**
 * Materialization of the activation property configuration which can be used as a key object in a map.
 */
final class ActivationPropertyKey implements IActivationProperty {
  private final String name;
  private final String value;

  private ActivationPropertyKey(String name, String value) {
    this.name = name;
    this.value = value;
  }

  ActivationPropertyKey(ActivationProperty src) {
    this(src.getName(), src.getValue());
  }

  @Override
  public String getValue() {
    return this.value;
  }

  @Override
  public String getName() {
    return this.name;
  }
}
