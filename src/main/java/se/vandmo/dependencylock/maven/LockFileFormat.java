package se.vandmo.dependencylock.maven;

import java.io.File;
import org.apache.maven.plugin.logging.Log;

public enum LockFileFormat {

  json {
    @Override
    public String defaultFilename() {
      return "dependencies-lock.json";
    }

    @Override
    DependenciesLockFile dependenciesLockFile_from(
        DependenciesLockFileAccessor dependenciesLockFileAccessor,
        PomMinimums pomMinimums,
        Log log) {
      return DependenciesLockFileJson.from(dependenciesLockFileAccessor, log);
    }
  },

  pom {
    @Override
    public String defaultFilename() {
      return ".dependency-lock/pom.xml";
    }

    @Override
    DependenciesLockFile dependenciesLockFile_from(
        DependenciesLockFileAccessor dependenciesLockFileAccessor,
        PomMinimums pomMinimums,
        Log log) {
      return DependenciesLockFilePom.from(dependenciesLockFileAccessor, pomMinimums, log);
    }
  };

  abstract String defaultFilename();

  private String getLockFilename(String filename) {
    if (filename != null) {
      return filename;
    }
    return defaultFilename();
  }

  public DependenciesLockFileAccessor dependenciesLockFileAccessor_fromBasedirAndFilename(File basedir, String filename) {
    return DependenciesLockFileAccessor.fromBasedir(basedir, getLockFilename(filename));
  }

  abstract DependenciesLockFile dependenciesLockFile_from(
      DependenciesLockFileAccessor dependenciesLockFileAccessor,
      PomMinimums pomMinimums,
      Log log);

}
