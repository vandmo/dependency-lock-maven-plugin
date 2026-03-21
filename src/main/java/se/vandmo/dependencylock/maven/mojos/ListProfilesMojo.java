package se.vandmo.dependencylock.maven.mojos;

import static org.apache.maven.plugins.annotations.ResolutionScope.TEST;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import se.vandmo.dependencylock.maven.Dependencies;
import se.vandmo.dependencylock.maven.mojos.model.IActivation;
import se.vandmo.dependencylock.maven.mojos.model.IActivationOS;
import se.vandmo.dependencylock.maven.mojos.model.IActivationProperty;
import se.vandmo.dependencylock.maven.mojos.model.Profile;
import se.vandmo.dependencylock.maven.services.ProfileHandler;

@Mojo(name = "list-profiles", requiresDependencyResolution = TEST, threadSafe = true)
public class ListProfilesMojo extends AbstractMojo {
  private final ProfileHandler profileHandler;

  @Parameter(defaultValue = "${session}", required = true, readonly = true)
  private MavenSession mavenSession;

  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  private MavenProject project;

  @Inject
  public ListProfilesMojo(ProfileHandler profileHandler) {
    this.profileHandler = profileHandler;
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    List<Profile> profiles =
        this.profileHandler
            .lookupAvailableProfiles(
                this.mavenSession, Dependencies.fromMavenArtifacts(this.project.getArtifacts()))
            .collect(Collectors.toList());
    if (profiles.isEmpty()) {
      getLog().info("No profiles found");
    } else {
      getLog().info(profiles.size() + " profiles found:");
      for (Profile p : profiles) {
        dumpProfile(p);
      }
    }
  }

  private void dumpProfile(Profile profile) {
    getLog().info("<profile>");
    getLog().info("  <id>" + profile.getId() + "</id>");
    getLog().info("  <activation>");
    if (profile.getActivation() != null) {
      IActivation activation = profile.getActivation();
      final IActivationOS activationOs = activation.getOs();
      if (activationOs != null) {
        getLog().info("    <os>");
        final String activationOsFamily = activationOs.getFamily();
        if (activationOsFamily != null) {
          getLog().info("      <family>" + activationOsFamily + "</family>");
        }
        final String activationOsName = activationOs.getName();
        if (activationOsName != null) {
          getLog().info("      <name>" + activationOsName + "</name>");
        }
        final String activationOsArch = activationOs.getArch();
        if (activationOsArch != null) {
          getLog().info("      <arch>" + activationOsArch + "</arch>");
        }
        final String activationOsVersion = activationOs.getVersion();
        if (activationOsVersion != null) {
          getLog().info("      <version>" + activationOsVersion + "</version>");
        }
        getLog().info("    </os>");
      }
      final IActivationProperty activationProperty = activation.getProperty();
      if (activationProperty != null) {
        getLog().info("    <property>");
        getLog().info("      <name>" + activationProperty.getName() + "</name>");
        final String activationPropertyValue = activationProperty.getValue();
        if (activationPropertyValue != null) {
          getLog().info("      <value>" + activationPropertyValue + "</value>");
        }
        getLog().info("    </property>");
      }
    }
    getLog().info("  </activation>");
    getLog().info("</profile>");
  }
}
