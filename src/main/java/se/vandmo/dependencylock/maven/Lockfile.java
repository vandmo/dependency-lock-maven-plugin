package se.vandmo.dependencylock.maven;

import org.apache.maven.plugin.MojoExecutionException;

public interface Lockfile {

  void write(LockedProject contents);

  LockedProject read() throws MojoExecutionException;
}
