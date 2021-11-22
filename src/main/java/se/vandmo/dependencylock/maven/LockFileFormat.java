package se.vandmo.dependencylock.maven;

public enum LockFileFormat {

  json {
    @Override
    public String defaultFilename() {
      return "dependencies-lock.json";
    }
  },

  pom {
    @Override
    public String defaultFilename() {
      return ".dependency-lock/pom.xml";
    }
  };

  public abstract String defaultFilename();
}
