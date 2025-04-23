package se.vandmo.dependencylock.maven;

import static java.util.Collections.emptyList;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.maven.plugin.descriptor.PluginDescriptor;

public final class Plugins extends LockableEntitiesWithArtifact<Plugin> {

  private Plugins(Collection<Plugin> plugins) {
    super(plugins);
  }

  public static Plugins empty() {
    return from(emptyList());
  }

  public static Plugins from(Collection<Plugin> plugins) {
    return new Plugins(plugins);
  }

  public static Plugins fromMavenPlugins(Collection<PluginDescriptor> pluginDescriptors) {
    return new Plugins(
        pluginDescriptors.stream()
            .map(plugin -> Plugin.fromPluginDescriptor(plugin))
            .collect(Collectors.toList()));
  }

  @Override
  public Stream<Artifact> artifacts() {
    return stream().flatMap(p -> Stream.concat(Stream.of(p.artifact), p.dependencies.stream()));
  }
}
