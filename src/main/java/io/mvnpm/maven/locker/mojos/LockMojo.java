package io.mvnpm.maven.locker.mojos;

import io.mvnpm.maven.locker.DependenciesLockFileAccessor;
import io.mvnpm.maven.locker.pom.DependenciesLockFilePom;
import org.apache.maven.plugins.annotations.Mojo;

import static java.util.Locale.ROOT;
import static org.apache.maven.plugins.annotations.ResolutionScope.TEST;

@Mojo(name = "lock", requiresDependencyResolution = TEST)
public final class LockMojo extends AbstractDependencyLockMojo {

  @Override
  public void execute() {
    DependenciesLockFileAccessor lockFile = lockFile();
    getLog().info(String.format(ROOT, "Creating %s", lockFile.filename()));
    DependenciesLockFilePom lockFilePom =
            DependenciesLockFilePom.from(lockFile, pomMinimums(), getLog());
    lockFilePom.write(projectDependencies());
  }
}
