package se.vandmo.dependencylock.maven.mojos;

import static java.util.Locale.ROOT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Profile;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.PluginContainerException;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import se.vandmo.dependencylock.maven.Artifact;
import se.vandmo.dependencylock.maven.Artifacts;
import se.vandmo.dependencylock.maven.Plugins;

final class LockProjectHelper {

  private final Log log;
  private final MavenPluginManager mavenPluginManager;
  private final MavenSession mavenSession;

  LockProjectHelper(Log log, MavenPluginManager mavenPluginManager, MavenSession mavenSession) {
    this.log = log;
    this.mavenPluginManager = mavenPluginManager;
    this.mavenSession = mavenSession;
  }

  private se.vandmo.dependencylock.maven.Plugin loadPlugin(
      MavenProject project, Plugin plugin, Map<String, Artifact> artifactCache)
      throws MojoExecutionException {
    try {
      final PluginDescriptor descriptor =
          mavenPluginManager.getPluginDescriptor(
              plugin, project.getRemotePluginRepositories(), mavenSession.getRepositorySession());
      mavenPluginManager.setupPluginRealm(descriptor, mavenSession, null, null, null);
      final org.apache.maven.artifact.Artifact pluginArtifact = descriptor.getPluginArtifact();
      return se.vandmo.dependencylock.maven.Plugin.forArtifact(Artifact.from(pluginArtifact))
          .artifacts(
              Artifacts.fromArtifacts(
                  descriptor.getArtifacts().stream()
                      .filter(a -> !a.equals(pluginArtifact))
                      .map(
                          mavenArtifact -> {
                            final String artifactId = mavenArtifact.getId();
                            Artifact cachedValue = artifactCache.get(artifactId);
                            if (null == cachedValue) {
                              cachedValue = Artifact.from(mavenArtifact);
                              artifactCache.put(artifactId, cachedValue);
                            }
                            return cachedValue;
                          })
                      .collect(Collectors.toList())))
          .build();
    } catch (PluginResolutionException e) {
      throw new MojoExecutionException("Failed resolving plugin " + plugin, e);
    } catch (PluginDescriptorParsingException e) {
      throw new MojoExecutionException("Failed parsing plugin descriptor of plugin " + plugin, e);
    } catch (InvalidPluginDescriptorException e) {
      throw new MojoExecutionException("Invalid plugin descriptor found for plugin " + plugin, e);
    } catch (PluginContainerException e) {
      throw new MojoExecutionException("Failed loading container for plugin " + plugin, e);
    }
  }

  Plugins loadPlugins(MavenProject project) throws MojoExecutionException {
    final List<Plugin> buildPlugins = project.getBuildPlugins();
    final Map<String, Artifact> artifactCache = new HashMap<>();
    final List<se.vandmo.dependencylock.maven.Plugin> result = new ArrayList<>(buildPlugins.size());
    Map<String, Plugin> declaredPlugins = new HashMap<>();
    for (Plugin plugin : buildPlugins) {
      declaredPlugins.put(plugin.getId(), plugin);
      result.add(loadPlugin(project, plugin, artifactCache));
    }
    for (Plugin profilePlugin : lookupAdditionalProfilePlugins(project, declaredPlugins)) {
      result.add(loadPlugin(project, profilePlugin, artifactCache));
    }
    return Plugins.from(result);
  }

  private Collection<Plugin> lookupAdditionalProfilePlugins(
      MavenProject project, Map<String, Plugin> declaredPlugins) {
    final List<Profile> profiles = project.getModel().getProfiles();
    if (profiles.isEmpty()) {
      return Collections.emptyList();
    }
    final Map<String, String> baseProfilePluginVersions =
        loadCurrentProjectBuildPluginVersions(project);
    final Map<String, Plugin> missingPlugins = new HashMap<>();
    for (Profile profile : profiles) {
      final Map<String, String> potentialProfilePluginVersions =
          new HashMap<>(baseProfilePluginVersions);
      final PluginManagement profilePluginManagement = profile.getBuild().getPluginManagement();
      if (profilePluginManagement != null) {
        profilePluginManagement
            .getPluginsAsMap()
            .forEach(
                (key, value) -> {
                  final String profilePluginVersion = value.getVersion();
                  if (profilePluginVersion != null) {
                    log.warn(
                        String.format(
                            ROOT,
                            "Detected version managed at profile level for plugin %s. This is not"
                                + " recommended as it may yield inconsistent behaviours.",
                            key));
                    potentialProfilePluginVersions.put(key, profilePluginVersion);
                  }
                });
      }
      for (Plugin plugin : profile.getBuild().getPlugins()) {
        if (null != plugin.getVersion()) {
          final String pluginId = plugin.getId();
          if (!declaredPlugins.containsKey(pluginId)) {
            missingPlugins.put(pluginId, plugin);
          }
        } else {
          final String pluginKey = plugin.getKey();
          final String potentialVersion = potentialProfilePluginVersions.get(pluginKey);
          if (null != potentialVersion) {
            final Plugin forkedPlugin = plugin.clone();
            forkedPlugin.setVersion(potentialVersion);
            final String pluginId = forkedPlugin.getId();
            if (!declaredPlugins.containsKey(pluginId)) {
              missingPlugins.put(pluginId, forkedPlugin);
            }
          }
        }
      }
    }
    return missingPlugins.values();
  }

  private Map<String, String> loadCurrentProjectBuildPluginVersions(MavenProject project) {
    Map<String, String> baseProfilePluginVersions = new HashMap<>();
    project
        .getBuild()
        .getPluginsAsMap()
        .forEach((key, value) -> baseProfilePluginVersions.put(key, value.getVersion()));
    final PluginManagement projectPluginManagement = project.getBuild().getPluginManagement();
    if (projectPluginManagement != null) {
      projectPluginManagement
          .getPluginsAsMap()
          .forEach(
              (key, value) -> {
                final String managedPluginVersion = value.getVersion();
                if (managedPluginVersion != null) {
                  baseProfilePluginVersions.putIfAbsent(key, managedPluginVersion);
                }
              });
    }
    return Collections.unmodifiableMap(baseProfilePluginVersions);
  }
}
