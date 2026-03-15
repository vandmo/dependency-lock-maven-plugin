package se.vandmo.dependencylock.maven.mojos.model;

public interface IActivationOS {
  String getArch();

  String getFamily();

  String getName();

  String getVersion();
}
