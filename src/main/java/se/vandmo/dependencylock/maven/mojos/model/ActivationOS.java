package se.vandmo.dependencylock.maven.mojos.model;

/** */
public class ActivationOS implements IActivationOS {
  private String family;
  private String arch;
  private String version;
  private String name;

  public ActivationOS() {
    super();
  }

  public ActivationOS(IActivationOS src) {
    super();
    this.family = src.getFamily();
    this.arch = src.getArch();
    this.version = src.getVersion();
    this.name = src.getName();
  }

  @Override
  public String getArch() {
    return arch;
  }

  public void setArch(String arch) {
    this.arch = arch;
  }

  @Override
  public String getFamily() {
    return family;
  }

  public void setFamily(String family) {
    this.family = family;
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }
}
