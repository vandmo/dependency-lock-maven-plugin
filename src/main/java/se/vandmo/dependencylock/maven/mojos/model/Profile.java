package se.vandmo.dependencylock.maven.mojos.model;

/** Information about a maven profile. */
public final class Profile {
  private String id;
  private Activation activation;

  public Activation getActivation() {
    return activation;
  }

  public void setActivation(Activation activation) {
    this.activation = activation;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }
}
