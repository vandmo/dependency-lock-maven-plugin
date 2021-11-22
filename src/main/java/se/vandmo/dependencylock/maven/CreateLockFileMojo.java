package se.vandmo.dependencylock.maven;

import static java.util.Locale.ROOT;
import static org.apache.maven.plugins.annotations.ResolutionScope.TEST;

import org.apache.maven.plugins.annotations.Mojo;


@Mojo(
  name = "create-lock-file",
  requiresDependencyResolution = TEST)
public final class CreateLockFileMojo extends AbstractDependencyLockMojo {

  @Override
  public void execute() {
    DependenciesLockFile lockFile = lockFile();
    getLog().info(String.format(ROOT, "Creating %s", lockFile.filename()));
    switch (format()) {
      case json:
        DependenciesLockFileJson lockFileJson = DependenciesLockFileJson.from(lockFile);
        LockedDependencies lockedDependencies = LockedDependencies.from(projectDependencies(), getLog());
        lockFileJson.write(lockedDependencies);
        break;
      case pom:
        PomIO.writePom(lockFile, pomMinimums(), projectDependencies());
        break;
      default:
        throw new RuntimeException("This should not happen!");
    }
  }

}
