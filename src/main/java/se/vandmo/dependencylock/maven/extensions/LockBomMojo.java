package se.vandmo.dependencylock.maven.extensions;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import se.vandmo.dependencylock.maven.Artifact;
import se.vandmo.dependencylock.maven.DependenciesLockFileAccessor;
import se.vandmo.dependencylock.maven.LockFileFormat;
import se.vandmo.dependencylock.maven.LockedDependencies;
import se.vandmo.dependencylock.maven.PomMinimums;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;

@Named("lock-bom")
@Singleton
public final class LockBomMojo extends AbstractMavenLifecycleParticipant {

  private SystemStreamLog log;

  @Override
  public void afterProjectsRead(MavenSession session)
          throws MavenExecutionException
  {
    addBom(session);
  }
  private void addBom(MavenSession session) throws MavenExecutionException {
    final File basedir = session.getCurrentProject().getBasedir();
    final LockFileFormat format = LockFileFormat.pom;
    DependenciesLockFileAccessor lockFile = format.dependenciesLockFileAccessor_fromBasedirAndFilename(basedir, null);
    if (!lockFile.exists()) {
      throw new MavenExecutionException(
              "No lock file found, create one by running 'mvn"
                      + " se.vandmo:dependency-lock-maven-plugin:lock'", session.getRequest().getPom());
    }
    final PomMinimums pomMinimums = PomMinimums.from(session.getCurrentProject());
    LockedDependencies lockedDependencies =
            format.dependenciesLockFile_from(lockFile, pomMinimums, getLog()).read();
    getLog().info("Locking dependencies through bom: " + lockFile.filename());
    for (Artifact artifact : lockedDependencies.lockedDependencies.artifacts) {
      Dependency dependency = new Dependency();
      dependency.setGroupId(artifact.identifier.groupId);
      dependency.setArtifactId(artifact.identifier.artifactId);
      dependency.setVersion(artifact.version);
      dependency.setScope(artifact.scope);
      dependency.setOptional(artifact.optional);
      artifact.identifier.classifier.ifPresent(dependency::setClassifier);
      dependency.setType(artifact.identifier.type);
      session.getCurrentProject().getDependencyManagement().addDependency(dependency);
    }
  }

  public Log getLog() {
    if (this.log == null) {
      this.log = new SystemStreamLog();
    }

    return this.log;
  }
}
