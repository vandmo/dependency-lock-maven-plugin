package se.vandmo.dependencylock.maven.pom;

import static java.util.Objects.requireNonNull;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.Writer;
import se.vandmo.dependencylock.maven.LockFileAccessor;
import se.vandmo.dependencylock.maven.PomMinimums;
import se.vandmo.dependencylock.maven.ProfiledDependencies;

public final class DependenciesLockFilePom extends WithLockFilePom {

  private final LockFileAccessor dependenciesLockFile;
  private final PomMinimums pomMinimums;

  private DependenciesLockFilePom(LockFileAccessor dependenciesLockFile, PomMinimums pomMinimums) {
    this.dependenciesLockFile = dependenciesLockFile;
    this.pomMinimums = pomMinimums;
  }

  public static DependenciesLockFilePom from(
      LockFileAccessor dependenciesLockFile, PomMinimums pomMinimums) {
    return new DependenciesLockFilePom(
        requireNonNull(dependenciesLockFile), requireNonNull(pomMinimums));
  }

  public void write(ProfiledDependencies projectDependencies) {
    Configuration cfg = createConfiguration();
    try {
      Template template = cfg.getTemplate("pom.ftlx");
      try (Writer writer = dependenciesLockFile.writer()) {
        template.process(makeDataModel(pomMinimums, projectDependencies), writer);
      }
    } catch (IOException | TemplateException e) {
      throw new RuntimeException(e);
    }
  }
}
