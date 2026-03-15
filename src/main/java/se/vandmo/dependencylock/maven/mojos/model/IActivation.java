package se.vandmo.dependencylock.maven.mojos.model;

public interface IActivation {
  IActivationProperty getProperty();

  IActivationOS getOs();
}
