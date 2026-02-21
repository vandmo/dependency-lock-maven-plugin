package se.vandmo.dependencylock.maven.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.ModelProblemCollectorRequest;
import org.apache.maven.model.profile.DefaultProfileActivationContext;
import org.apache.maven.model.profile.ProfileActivationContext;
import org.apache.maven.model.profile.ProfileSelector;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import se.vandmo.dependencylock.maven.ProfileUtils;
import se.vandmo.dependencylock.maven.mojos.model.Activation;
import se.vandmo.dependencylock.maven.mojos.model.ActivationOS;
import se.vandmo.dependencylock.maven.mojos.model.Profile;

@Named
@Singleton
public class ProfileHandlerImpl extends AbstractLogEnabled implements ProfileHandler {

  private final ProfileSelector profileSelector;

  private final RuntimeInformation runtimeInformation;

  @Inject
  public ProfileHandlerImpl(
      ProfileSelector profileSelector, RuntimeInformation runtimeInformation) {
    this.profileSelector = profileSelector;
    this.runtimeInformation = runtimeInformation;
  }

  private ProfileActivationContext buildProfileAcivationContext(
      MavenExecutionRequest mavenExecutionRequest, MavenProject mavenProject) {
    DefaultProfileActivationContext result = new DefaultProfileActivationContext();
    result.setSystemProperties(mavenExecutionRequest.getSystemProperties());
    Properties userProperties = mavenExecutionRequest.getUserProperties();
    userProperties.computeIfAbsent(
        ProfileActivationContext.PROPERTY_NAME_PACKAGING, (p) -> mavenProject.getPackaging());
    result.setUserProperties(userProperties);
    result.setProjectDirectory(mavenProject.getParentFile());
    return result;
  }

  @Override
  public Stream<Profile> filterActiveProfiles(
      Collection<Profile> profiles,
      MavenProject mavenProject,
      MavenExecutionRequest executionRequest)
      throws MojoExecutionException {
    if (null == profiles) {
      return Stream.empty();
    }
    Map<String, Profile> profileMap = new HashMap<>(profiles.size());
    Collection<org.apache.maven.model.Profile> emulatedProfiles = new ArrayList<>(profiles.size());
    for (Profile profile : profiles) {
      profileMap.put(profile.getId(), profile);
      emulatedProfiles.add(generateProfile(profile));
    }
    final Logger logger = getLogger();
    Collection<org.apache.maven.model.Profile> actuallyEnabledProfiles =
        this.profileSelector.getActiveProfiles(
            emulatedProfiles,
            buildProfileAcivationContext(executionRequest, mavenProject),
            new WarnLoggerProblemCollector(logger));
    return actuallyEnabledProfiles.stream()
        .map(
            p -> {
              final Profile result = profileMap.get(p.getId());
              if (result == null) {
                logger.warn("Unexpected enabled profile found (id: " + p.getId() + ")");
              }
              return result;
            })
        .filter(Objects::nonNull);
  }

  @Override
  public Collection<Profile> computeEnabledProfiles(
      MavenSession mavenSession,
      Collection<Profile> profiles,
      Map<String, Function<RepositorySystemSession, RepositorySystemSession>> profilingSessions)
      throws MojoExecutionException {
    if (profiles == null || profiles.isEmpty()) {
      return Collections.emptyList();
    }
    if (!runtimeInformation.isMavenVersion("[3.9.7,)")) {
      throw new MojoExecutionException("Maven 3.9.7 or newer is required.");
    }
    final Collection<Profile> actuallyEnabledProfiles = new ArrayList<>(profiles.size());
    final Collection<Profile> disabledProfiles = new ArrayList<>(profiles);
    final Map<String, Profile> profilesById = new HashMap<>();
    final Collection<org.apache.maven.model.Profile> emulatedProfiles =
        new ArrayList<>(profiles.size());
    for (Profile profile : profiles) {
      final org.apache.maven.model.Profile emulatedProfile = generateProfile(profile);
      emulatedProfiles.add(emulatedProfile);
      profilesById.put(emulatedProfile.getId(), profile);
    }
    final DefaultProfileActivationContext profileActivationContext =
        new DefaultProfileActivationContext();
    profileActivationContext.setSystemProperties(mavenSession.getSystemProperties());
    profileActivationContext.setUserProperties(mavenSession.getUserProperties());
    for (org.apache.maven.model.Profile actuallyEnabledProfile :
        this.profileSelector.getActiveProfiles(
            emulatedProfiles, profileActivationContext, new ModelProblemLogger(getLogger()))) {
      final Profile enabledProfile = profilesById.get(actuallyEnabledProfile.getId());
      disabledProfiles.remove(enabledProfile);
      actuallyEnabledProfiles.add(enabledProfile);
    }
    if (actuallyEnabledProfiles.size() > 1) {
      getLogger().warn("More than one profile is currently enabled.");
    }
    for (Profile disabledProfile : disabledProfiles) {
      profilingSessions.put(disabledProfile.getId(), buildProfileEmulator(disabledProfile));
    }
    return actuallyEnabledProfiles;
  }

  private Function<Map<String, String>, Map<String, String>> buildSystemPropertiesEmulation(
      Profile profile) throws MojoExecutionException {
    Function<Map<String, String>, Map<String, String>> result = UnaryOperator.identity();
    result = result.andThen(buildActivationOsTransformer(profile.getActivation().getOs()));
    return result;
  }

  /**
   * Builds the maven equivalent of the given profile parameter.
   *
   * @param profile the profile to generate an equivalent of
   * @return the corresponding profile
   * @throws NullPointerException if the specified profile parameter is <code>null</code>
   * @throws MojoExecutionException if the specified profile parameter has no activation defined
   */
  private org.apache.maven.model.Profile generateProfile(Profile profile)
      throws MojoExecutionException {
    final Activation activation = profile.getActivation();
    if (null == activation) {
      getLogger().error("Ignoring unsupported profile with no activation: " + profile.getId());
      throw new MojoExecutionException(
          "Missing activation criteria for profile " + profile.getId());
    }
    final org.apache.maven.model.Profile emulatedProfile = new org.apache.maven.model.Profile();
    emulatedProfile.setId(profile.getId());
    emulatedProfile.setActivation(ProfileUtils.toMavenActivation(activation));
    return emulatedProfile;
  }

  private UnaryOperator<Map<String, String>> buildActivationOsTransformer(ActivationOS os)
      throws MojoExecutionException {
    Map<String, String> emulatedValues = ProfileUtils.emulateSystemProperties(os);
    if (emulatedValues.isEmpty()) {
      return UnaryOperator.identity();
    }
    return new UnaryOperator<Map<String, String>>() {
      @Override
      public Map<String, String> apply(Map<String, String> result) {
        result.putAll(emulatedValues);
        return result;
      }
    };
  }

  private Function<RepositorySystemSession, RepositorySystemSession> buildProfileEmulator(
      Profile profile) throws MojoExecutionException {
    final Function<Map<String, String>, Map<String, String>> systemPropertiesFunction =
        buildSystemPropertiesEmulation(profile);
    return new Function<RepositorySystemSession, RepositorySystemSession>() {
      @Override
      public RepositorySystemSession apply(RepositorySystemSession repositorySystemSession) {
        DefaultRepositorySystemSession result =
            new DefaultRepositorySystemSession(repositorySystemSession);
        result.setCache(null); // required otherwise previously resolved descriptors
        result.setSystemProperties(
            systemPropertiesFunction.apply(new HashMap<>(result.getSystemProperties())));
        return result;
      }
    };
  }

  private static class WarnLoggerProblemCollector implements ModelProblemCollector {
    private final Logger logger;

    public WarnLoggerProblemCollector(Logger logger) {
      this.logger = logger;
    }

    @Override
    public void add(ModelProblemCollectorRequest req) {
      logger.warn(
          "Issue encountered while trying to compute enabled profiles: " + req.getMessage(),
          req.getException());
    }
  }

  private static final class ModelProblemLogger implements ModelProblemCollector {
    private final Logger logger;

    ModelProblemLogger(Logger logger) {
      this.logger = logger;
    }

    @Override
    public void add(ModelProblemCollectorRequest req) {
      this.logger.warn(req.toString());
    }
  }
}
