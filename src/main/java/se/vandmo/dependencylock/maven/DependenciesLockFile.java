package se.vandmo.dependencylock.maven;

public interface DependenciesLockFile {

  void write(Dependencies projectDependencies);

  LockedDependencies read();
}
