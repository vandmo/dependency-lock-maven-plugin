package se.vandmo.dependencylock.maven;

import static org.apache.maven.plugins.annotations.ResolutionScope.TEST;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;


@Mojo(
  name = "lock",
  requiresDependencyResolution = TEST)
public final class LockMojo extends AbstractDependencyLockMojo {

  @Override
  public void execute() throws MojoFailureException {
    getLog().warn("The 'lock' goal is deprecated, use 'create-lock-file' instead.");
    switch (format()) {
      case json:
        DependenciesLockFileJson lockFileJson = DependenciesLockFileJson
            .from(lockFile(), getLog());
        LockedDependencies existingLockedDependencies = getExistingLockedDependencies(lockFileJson);
        LockedDependencies lockedDependencies = existingLockedDependencies.updateWith(projectDependencies());
        lockFileJson.write(lockedDependencies);
        break;
      default:
        throw new MojoFailureException("Only supported for json lock files, use create-lock-file instead");
    }
  }

  private LockedDependencies getExistingLockedDependencies(DependenciesLockFileJson lockFile) {
    if (lockFile.exists()) {
      return lockFile.read();
    } else {
      return LockedDependencies.empty(getLog());
    }
  }

}
