package se.vandmo.dependencylock.maven;

import static java.util.Collections.emptyList;

import java.util.Collection;
import java.util.stream.Collectors;
import org.apache.maven.plugin.ExtensionRealmCache;

public final class Extensions extends LockableEntitiesWithArtifact<Extension> {

  private Extensions(Collection<Extension> extensions) {
    super(extensions);
  }

  public static Extensions from(Collection<Extension> extensions) {
    return new Extensions(extensions);
  }

  public static Extensions fromMavenExtensionRealms(
      Collection<ExtensionRealmCache.CacheRecord> extensionRealms) {
    return new Extensions(
        extensionRealms.stream()
            .map(cacheRecord -> Extension.fromMavenExtensionRealm(cacheRecord))
            .collect(Collectors.toList()));
  }

  public static Extensions empty() {
    return from(emptyList());
  }
}
