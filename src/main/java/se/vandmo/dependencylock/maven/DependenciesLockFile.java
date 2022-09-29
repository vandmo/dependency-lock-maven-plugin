package se.vandmo.dependencylock.maven;

public interface DependenciesLockFile {

  void write(Artifacts projectDependencies);

  LockedDependencies read(boolean enableIntegrityChecking);
}
