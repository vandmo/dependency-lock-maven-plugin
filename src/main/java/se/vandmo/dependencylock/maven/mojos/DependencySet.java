package se.vandmo.dependencylock.maven.mojos;

public final class DependencySet {
  public String[] includes = new String[0];
  public String[] excludes = new String[0];
  public String version = "check";
  public Integrity integrity;
  public Boolean allowMissing = null;
  public Boolean allowExtraneous = null;

  public enum Integrity {
    check,
    ignore
  }
}
