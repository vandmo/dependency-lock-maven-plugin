package se.vandmo.dependencylock.maven;

import java.io.File;
import org.apache.maven.plugin.logging.Log;
import se.vandmo.dependencylock.maven.json.DependenciesLockFileJson;
import se.vandmo.dependencylock.maven.pom.DependenciesLockFilePom;

public enum LockFileFormat {
  json {
    @Override
    public String defaultFilename() {
      return "dependencies-lock.json";
    }

    @Override
    public DependenciesLockFile dependenciesLockFile_from(
        LockFileAccessor lockFileAccessor, PomMinimums pomMinimums, Log log) {
      return DependenciesLockFileJson.from(lockFileAccessor, log);
    }
  },

  pom {
    @Override
    public String defaultFilename() {
      return ".dependency-lock/pom.xml";
    }

    @Override
    public DependenciesLockFile dependenciesLockFile_from(
        LockFileAccessor lockFileAccessor, PomMinimums pomMinimums, Log log) {
      return DependenciesLockFilePom.from(lockFileAccessor, pomMinimums, log);
    }
  };

  abstract String defaultFilename();

  private String getLockFilename(String filename) {
    if (filename != null) {
      return filename;
    }
    return defaultFilename();
  }

  public LockFileAccessor dependenciesLockFileAccessor_fromBasedirAndFilename(
      File basedir, String filename) {
    return LockFileAccessor.fromBasedir(basedir, getLockFilename(filename));
  }

  public abstract DependenciesLockFile dependenciesLockFile_from(
      LockFileAccessor lockFileAccessor, PomMinimums pomMinimums, Log log);
}
