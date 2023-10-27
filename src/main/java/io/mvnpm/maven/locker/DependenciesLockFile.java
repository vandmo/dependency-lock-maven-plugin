package io.mvnpm.maven.locker;

public interface DependenciesLockFile {

  void write(Artifacts projectDependencies);

  LockedDependencies read();
}
