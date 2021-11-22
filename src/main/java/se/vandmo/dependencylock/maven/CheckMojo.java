package se.vandmo.dependencylock.maven;

import static java.util.Arrays.asList;
import static org.apache.maven.plugins.annotations.LifecyclePhase.VALIDATE;
import static org.apache.maven.plugins.annotations.ResolutionScope.TEST;

import java.io.File;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.artifact.filter.StrictPatternIncludesArtifactFilter;


@Mojo(
  name = "check",
  defaultPhase = VALIDATE,
  requiresDependencyResolution = TEST)
public final class CheckMojo extends AbstractMojo {

  @Parameter(
    defaultValue = "${basedir}",
    required = true,
    readonly = true)
  private File basedir;

  @Parameter(
    defaultValue="${project}",
    required = true,
    readonly = true)
  private MavenProject project;

  @Parameter(defaultValue = DependenciesLockFile.DEFAULT_FILENAME)
  private String filename;

  @Parameter
  private String[] useMyVersionFor = new String[0];

  @Override
  public void execute() throws MojoExecutionException {
    DependenciesLockFile lockFile = DependenciesLockFile.fromBasedir(basedir, filename);
    if (!lockFile.exists()) {
      throw new MojoExecutionException(
          "No lock file found, create one by running 'mvn se.vandmo:dependency-lock-maven-plugin:lock'");
    }
    ArtifactFilter useMyVersionForFilter = new StrictPatternIncludesArtifactFilter(asList(useMyVersionFor));
    LockedDependencies lockedDependencies = lockFile.read(getLog());
    Artifacts actualDependencies = Artifacts.from(project.getArtifacts());
    LockedDependencies.Diff diff = lockedDependencies.compareWith(actualDependencies, project.getVersion(), useMyVersionForFilter);
    if (diff.equals()) {
      getLog().info("Actual dependencies matches locked dependencies");
    } else {
      diff.logTo(getLog());
      throw new MojoExecutionException("Dependencies differ");
    }
  }

}
