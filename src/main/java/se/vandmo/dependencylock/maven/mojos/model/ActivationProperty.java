package se.vandmo.dependencylock.maven.mojos.model;

import java.util.Objects;

/** */
public class ActivationProperty implements IActivationProperty {
  private String name;
  private String value;

  public ActivationProperty() {
    super();
  }

  public ActivationProperty(IActivationProperty src) {
    super();
    this.name = src.getName();
    this.value = src.getValue();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ActivationProperty)) return false;
    ActivationProperty that = (ActivationProperty) o;
    return Objects.equals(name, that.name) && Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, value);
  }

  @Override
  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
