package se.vandmo.dependencylock.maven.services;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;
import se.vandmo.dependencylock.maven.mojos.model.Profile;

/** Implementations of this interface shall be able to handle all profile-related logics. */
public interface ProfileHandler {

  /**
   * Filters outs from the specified profiles collection all the profiles which are actually not
   * enabled according to the provided context.
   *
   * @param profiles the profiles which need to be appropriately filtered, if <code>null</code> this
   *     method should do nothing
   * @param mavenProject the project in which the profiles are being evaluated
   * @param executionRequest the current maven execution request context
   * @return a stream containing only profiles which ought to be active
   */
  Stream<Profile> filterActiveProfiles(
      Collection<Profile> profiles,
      MavenProject mavenProject,
      MavenExecutionRequest executionRequest)
      throws MojoExecutionException;

  /**
   * Computes and returns a collection of profiles which are enabled according to the specified
   * maven session and appends to the provided profilingSessions the functions which can be used to
   * emulate non-enabled profiles.
   *
   * @param mavenSession the current maven session
   * @param profiles the collection of profiles to challenge
   * @param profilingSessions where to store functions to emulate non-enabled profiles
   * @return a never <code>null</code> collection of all the actually enabled profiles
   * @throws NullPointerException if any of the provided parameters is <code>null</code>
   * @throws MojoExecutionException if there was any unexpected error while trying to compute the
   *     enabled profiles
   */
  Collection<Profile> computeEnabledProfiles(
      MavenSession mavenSession,
      Collection<Profile> profiles,
      Map<String, Function<RepositorySystemSession, RepositorySystemSession>> profilingSessions)
      throws MojoExecutionException;
}
