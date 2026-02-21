package se.vandmo.dependencylock.maven.mojos;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.ExtensionRealmCache;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.PluginManagerException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.artifact.filter.StrictPatternIncludesArtifactFilter;
import se.vandmo.dependencylock.maven.DependencySetConfiguration;
import se.vandmo.dependencylock.maven.Extensions;
import se.vandmo.dependencylock.maven.Filters;
import se.vandmo.dependencylock.maven.LockFileAccessor;
import se.vandmo.dependencylock.maven.LockFileFormat;
import se.vandmo.dependencylock.maven.MojoExecutionRuntimeException;
import se.vandmo.dependencylock.maven.Plugins;
import se.vandmo.dependencylock.maven.PomMinimums;
import se.vandmo.dependencylock.maven.mojos.model.Profile;

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

  @Parameter private Profile[] profiles;

  LockFileAccessor lockFile() {
    return format.dependenciesLockFileAccessor_fromBasedirAndFilename(basedir, filename);
  }

  Collection<Profile> getDependenciesProfiles() throws MojoExecutionException {
    final Profile[] configuredProfiles = this.profiles;
    if (null == configuredProfiles) {
      return null;
    }
    final List<Profile> result = unmodifiableList(asList(configuredProfiles));
    if (result.stream().map(Profile::getId).distinct().count() != result.size()) {
      throw new MojoExecutionException(
          "Duplicate profile IDs found: "
              + result.stream().map(Profile::getId).collect(Collectors.joining(", ")));
    }
    return result;
  }

  final MavenSession mavenSession() {
    return mavenSession;
  }

  final MavenProject mavenProject() {
    return this.project;
  }

  Extensions projectExtensions() {
    Collection<ExtensionRealmCache.CacheRecord> extensionRealms =
        new ArrayList<>(mavenProject().getBuildExtensions().size());
    for (Extension extension : mavenProject().getBuildExtensions()) {
      final Plugin extensionAsAPlugin = new Plugin();
      extensionAsAPlugin.setGroupId(extension.getGroupId());
      extensionAsAPlugin.setArtifactId(extension.getArtifactId());
      extensionAsAPlugin.setVersion(extension.getVersion());
      try {
        extensionRealms.add(
            mavenPluginManager.setupExtensionsRealm(
                mavenProject(), extensionAsAPlugin, mavenSession.getRepositorySession()));
      } catch (PluginManagerException e) {
        throw new MojoExecutionRuntimeException(
            "Failed loading extension realm for plugin " + extensionAsAPlugin, e);
      }
    }
    return Extensions.fromMavenExtensionRealms(extensionRealms);
  }

  Plugins projectPlugins() {
    return new LockProjectHelper(getLog(), mavenPluginManager, mavenSession)
        .loadPlugins(mavenProject());
  }

  PomMinimums pomMinimums() {
    return PomMinimums.from(mavenProject());
  }

  String projectVersion() {
    return mavenProject().getVersion();
  }

  LockFileFormat format() {
    return format;
  }

  private Filters filters;

  Filters filters() {
    Filters result = filters;
    if (result == null) {
      List<DependencySetConfiguration> dependencySetConfigurations =
          unmodifiableList(Arrays.stream(dependencySets).map(this::transform).collect(toList()));
      result = new Filters(dependencySetConfigurations, projectVersion());
      filters = result;
    }
    return result;
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
