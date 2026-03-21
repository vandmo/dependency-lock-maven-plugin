package se.vandmo.dependencylock.maven.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.ModelProblemCollectorRequest;
import org.apache.maven.model.profile.DefaultProfileActivationContext;
import org.apache.maven.model.profile.ProfileActivationContext;
import org.apache.maven.model.profile.ProfileSelector;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import se.vandmo.dependencylock.maven.ArtifactIdentifier;
import se.vandmo.dependencylock.maven.Dependencies;
import se.vandmo.dependencylock.maven.ProfileUtils;
import se.vandmo.dependencylock.maven.mojos.model.Activation;
import se.vandmo.dependencylock.maven.mojos.model.IActivation;
import se.vandmo.dependencylock.maven.mojos.model.IActivationOS;
import se.vandmo.dependencylock.maven.mojos.model.IActivationProperty;
import se.vandmo.dependencylock.maven.mojos.model.Profile;

@Named
@Singleton
public class ProfileHandlerImpl extends AbstractLogEnabled implements ProfileHandler {

  private final ProfileSelector profileSelector;

  private final RuntimeInformation runtimeInformation;

  private final ProjectBuilder projectBuilder;

  private final ArtifactHandlerManager artifactHandlerManager;

  @Inject
  public ProfileHandlerImpl(
      ProfileSelector profileSelector,
      RuntimeInformation runtimeInformation,
      ProjectBuilder projectBuilder,
      ArtifactHandlerManager artifactHandlerManager) {
    this.profileSelector = profileSelector;
    this.runtimeInformation = runtimeInformation;
    this.projectBuilder = projectBuilder;
    this.artifactHandlerManager = artifactHandlerManager;
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

  private void onSkippedProfile(
      String artifactKey, String reason, org.apache.maven.model.Profile profile) {
    getLogger()
        .info(
            "["
                + artifactKey
                + "] Skipping profile "
                + reason
                + ": "
                + profile.getId()
                + " ("
                + profile.getLocation("")
                + ")");
  }

  @Override
  public Stream<Profile> lookupAvailableProfiles(
      MavenSession mavenSession, Dependencies dependencies) {
    getLogger().debug("Collecting project artifacts...");
    final Collection<? extends Artifact> projectArtifacts = gatherProjectArtifacts(dependencies);
    Map<ActivationKey, Profile> resultProfiles = new HashMap<>();
    for (Artifact artifact : projectArtifacts) {
      final String projectArtifactKey = ArtifactUtils.key(artifact);
      getLogger().debug("[" + projectArtifactKey + "] Collecting profiles...");
      try {
        ProjectBuildingRequest projectBuildingRequest =
            new DefaultProjectBuildingRequest(mavenSession.getProjectBuildingRequest());
        ProjectBuildingResult result = projectBuilder.build(artifact, true, projectBuildingRequest);
        MavenProject project = result.getProject();
        while (project != null) {
          final List<org.apache.maven.model.Profile> profiles = project.getModel().getProfiles();
          if (profiles != null && !profiles.isEmpty()) {
            for (org.apache.maven.model.Profile profile : profiles) {
              final org.apache.maven.model.Activation profileActivation = profile.getActivation();
              if (profileActivation != null) {
                if (profileActivation.getJdk() != null) {
                  onSkippedProfile(projectArtifactKey, "JDK dependent (unsupported)", profile);
                  continue;
                }
                if (profileActivation.getFile() != null) {
                  onSkippedProfile(projectArtifactKey, "file dependent (unsupported)", profile);
                  continue;
                }
                ActivationKey activationKey = new ActivationKey(profileActivation);
                if (activationKey.isEmpty()) {
                  onSkippedProfile(projectArtifactKey, "with empty activation", profile);
                  continue;
                }
                if (resultProfiles.containsKey(activationKey)) { // already known
                  continue;
                }
                Profile profileEntry = new Profile();
                profileEntry.setId(profile.getId());
                profileEntry.setActivation(new Activation(activationKey));
                resultProfiles.put(activationKey, profileEntry);
              }
            }
          }
          project = project.getParent();
        }
      } catch (ProjectBuildingException e) {
        getLogger().error("[" + projectArtifactKey + "] Failed to build project. Ignoring.", e);
      }
    }
    return resultProfiles.values().stream();
  }

  private Collection<? extends Artifact> gatherProjectArtifacts(Dependencies dependencies) {
    final ArtifactHandler pomArtifacthandler = artifactHandlerManager.getArtifactHandler("pom");
    return dependencies.stream()
        .map(
            d -> {
              final ArtifactIdentifier artifactIdentifier = d.getArtifactIdentifier();
              return new DefaultArtifact(
                  artifactIdentifier.groupId,
                  artifactIdentifier.artifactId,
                  d.getVersion(),
                  null,
                  "pom",
                  null,
                  pomArtifacthandler);
            })
        // there may be duplicate project entries so we must use the overload which allows to select
        // which to keep
        .collect(Collectors.toMap(ArtifactUtils::key, a -> a, (a, b) -> a))
        .values();
  }

  @Override
  public Collection<Profile> computeEnabledProfiles(
      MavenSession mavenSession,
      Collection<Profile> profiles,
      Map<Profile, Function<RepositorySystemSession, RepositorySystemSession>> profilingSessions)
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
      profilingSessions.put(disabledProfile, buildProfileEmulator(disabledProfile));
    }
    return actuallyEnabledProfiles;
  }

  private Function<Map<String, String>, Map<String, String>> buildSystemPropertiesEmulation(
      Profile profile) throws MojoExecutionException {
    final IActivation activation = profile.getActivation();
    Function<Map<String, String>, Map<String, String>> result = UnaryOperator.identity();
    result = result.andThen(buildActivationOsTransformer(activation.getOs()));
    result = result.andThen(buildActivationPropertyTransformer(activation.getProperty()));
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
    final IActivation activation = profile.getActivation();
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

  private UnaryOperator<Map<String, String>> buildActivationOsTransformer(IActivationOS os)
      throws MojoExecutionException {
    if (os == null) {
      return UnaryOperator.identity();
    }
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

  private UnaryOperator<Map<String, String>> buildActivationPropertyTransformer(
      IActivationProperty property) throws MojoExecutionException {
    if (property == null) {
      return UnaryOperator.identity();
    }
    Map<String, String> emulatedValues = ProfileUtils.emulateSystemProperties(property);
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
