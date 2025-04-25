package se.vandmo.dependencylock.maven;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.maven.plugin.logging.Log;

public final class LockedExtensions extends LockedEntities<Extension> {

  private LockedExtensions(Extensions lockedExtensions, Log log) {
    super(lockedExtensions, log);
  }

  public static LockedExtensions from(Extensions extensions, Log log) {
    return new LockedExtensions(requireNonNull(extensions), log);
  }

  public DiffReport compareWith(Extensions extensions, Filters filters) {
    return super.compareWith(extensions, filters);
  }

  @Override
  List<String> findDiffs(
      AtomicReference<Extension> lockedExtensionRef, Extension actualExtension, Filters filters) {
    List<String> result = super.findDiffs(lockedExtensionRef, actualExtension, filters);
    result.addAll(
        diffVersion(lockedExtensionRef, actualExtension, Extension::withVersion, filters));
    return result;
  }
}
