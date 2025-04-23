package se.vandmo.dependencylock.maven.mojos;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.ExtensionRealmCache;
import org.apache.maven.plugin.InvalidPluginDescriptorException;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.PluginContainerException;
import org.apache.maven.plugin.PluginDescriptorParsingException;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugin.PluginResolutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.artifact.filter.StrictPatternIncludesArtifactFilter;
import se.vandmo.dependencylock.maven.Artifact;
import se.vandmo.dependencylock.maven.Artifacts;
import se.vandmo.dependencylock.maven.Dependencies;
import se.vandmo.dependencylock.maven.DependencySetConfiguration;
import se.vandmo.dependencylock.maven.Extensions;
import se.vandmo.dependencylock.maven.Filters;
import se.vandmo.dependencylock.maven.LockFileAccessor;
import se.vandmo.dependencylock.maven.LockFileFormat;
import se.vandmo.dependencylock.maven.Plugins;
import se.vandmo.dependencylock.maven.PomMinimums;

public abstract class AbstractDependencyLockMojo extends AbstractMojo {

  @Parameter(defaultValue = "${basedir}", required = true, readonly = true)
  private File basedir;

  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  private MavenProject project;

  @Parameter(defaultValue = "${session}", required = true, readonly = true)
  private MavenSession mavenSession;

  @Parameter(property = "dependencyLock.filename")
  private String filename;

  @Parameter(property = "dependencyLock.format")
  private LockFileFormat format = LockFileFormat.json;

  private @Parameter DependencySet[] dependencySets = new DependencySet[0];

  @Component private MavenPluginManager mavenPluginManager;

  LockFileAccessor lockFile() {
    return format.dependenciesLockFileAccessor_fromBasedirAndFilename(basedir, filename);
  }

  Dependencies projectDependencies() {
    return Dependencies.fromMavenArtifacts(project.getArtifacts());
  }

  Extensions projectExtensions() throws MojoExecutionException {
    Collection<ExtensionRealmCache.CacheRecord> extensionRealms =
        new ArrayList<>(project.getBuildExtensions().size());
    for (Extension extension : project.getBuildExtensions()) {
      final Plugin extensionAsAPlugin = new Plugin();
      extensionAsAPlugin.setGroupId(extension.getGroupId());
      extensionAsAPlugin.setArtifactId(extension.getArtifactId());
      extensionAsAPlugin.setVersion(extension.getVersion());
      try {
        extensionRealms.add(
            mavenPluginManager.setupExtensionsRealm(
                project, extensionAsAPlugin, mavenSession.getRepositorySession()));
      } catch (PluginManagerException e) {
        throw new MojoExecutionException(
            "Failed loading extension realm for plugin " + extensionAsAPlugin, e);
      }
    }
    return Extensions.fromMavenExtensionRealms(extensionRealms);
  }

  Plugins projectPlugins() throws MojoExecutionException {
    final List<Plugin> buildPlugins = project.getBuildPlugins();
    final Map<String, Artifact> artifactCache = new HashMap<>();
    final List<se.vandmo.dependencylock.maven.Plugin> result = new ArrayList<>(buildPlugins.size());
    for (Plugin plugin : buildPlugins) {
      result.add(loadPlugin(plugin, artifactCache));
    }
    return Plugins.from(result);
  }

  private se.vandmo.dependencylock.maven.Plugin loadPlugin(
      Plugin plugin, Map<String, Artifact> artifactCache) throws MojoExecutionException {
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

  PomMinimums pomMinimums() {
    return PomMinimums.from(project);
  }

  String projectVersion() {
    return project.getVersion();
  }

  LockFileFormat format() {
    return format;
  }

  Filters filters() {
    List<DependencySetConfiguration> dependencySetConfigurations =
        unmodifiableList(Arrays.stream(dependencySets).map(this::transform).collect(toList()));
    return new Filters(dependencySetConfigurations, projectVersion());
  }

  private DependencySetConfiguration transform(DependencySet dependencySet) {
    return new DependencySetConfiguration(
        new StrictPatternIncludesArtifactFilter(asList(dependencySet.includes)),
        new StrictPatternIncludesArtifactFilter(asList(dependencySet.excludes)),
        transformVersion(dependencySet.version),
        transformIntegrity(dependencySet.integrity),
        dependencySet.allowMissing,
        dependencySet.allowExtraneous);
  }

  private static DependencySetConfiguration.Integrity transformIntegrity(
      DependencySet.Integrity integrity) {
    if (integrity == null) {
      return null;
    }
    switch (integrity) {
      case check:
        return DependencySetConfiguration.Integrity.check;
      case ignore:
        return DependencySetConfiguration.Integrity.ignore;
      default:
        throw new RuntimeException("Invalid enum value encountered");
    }
  }

  private static DependencySetConfiguration.Version transformVersion(String version) {
    if (version == null) {
      return null;
    }
    switch (version) {
      case "check":
        return DependencySetConfiguration.Version.check;
      case "ignore":
        return DependencySetConfiguration.Version.ignore;
      case "use-project-version":
        return DependencySetConfiguration.Version.useProjectVersion;
      case "snapshot":
        return DependencySetConfiguration.Version.snapshot;
      default:
        throw new RuntimeException("Invalid value for version configuration");
    }
  }
}
