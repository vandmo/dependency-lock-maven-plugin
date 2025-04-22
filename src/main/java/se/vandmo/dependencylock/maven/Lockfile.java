package se.vandmo.dependencylock.maven;

public interface Lockfile {

  void write(LockedProject contents);

  LockedProject read();
}
