package se.vandmo.dependencylock.maven.mojos.model;

/** */
public class Activation implements IActivation {
  private IActivationOS os;
  private IActivationProperty property;

  public Activation() {
    super();
  }

  public Activation(IActivation src) {
    super();
    final IActivationOS srcOs = src.getOs();
    if (srcOs == null) {
      this.os = null;
    } else {
      this.os = new ActivationOS(srcOs);
    }
    final IActivationProperty srcProperty = src.getProperty();
    if (srcProperty == null) {
      this.property = null;
    } else {
      this.property = new ActivationProperty(srcProperty);
    }
  }

  @Override
  public IActivationProperty getProperty() {
    return property;
  }

  public void setProperty(ActivationProperty property) {
    this.property = property;
  }

  @Override
  public IActivationOS getOs() {
    return os;
  }

  public void setOs(ActivationOS os) {
    this.os = os;
  }
}
