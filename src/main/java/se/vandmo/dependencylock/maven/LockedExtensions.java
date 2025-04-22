package se.vandmo.dependencylock.maven;

import static java.lang.String.format;
import static java.util.Locale.ROOT;
import static java.util.Objects.requireNonNull;
import static se.vandmo.dependencylock.maven.lang.Strings.joinNouns;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.maven.plugin.logging.Log;

public final class LockedExtensions {

  public final Extensions lockedExtensions;
  private final DiffHelper diffHelper;
  private final Log log;

  private LockedExtensions(Extensions lockedExtensions, Log log) {
    this.lockedExtensions = lockedExtensions;
    this.diffHelper = new DiffHelper(log);
    this.log = log;
  }

  public static LockedExtensions from(Extensions extensions, Log log) {
    return new LockedExtensions(requireNonNull(extensions), log);
  }

  public DiffReport compareWith(Extensions extensions, Filters filters) {
    LockFileExpectationsDiff expectationsDiff = new LockFileExpectationsDiff(extensions, filters);
    List<String> extraneous = diffHelper.findExtraneous(extensions, lockedExtensions, filters);
    return new DiffReport(expectationsDiff.different, expectationsDiff.missing, extraneous);
  }

  private final class LockFileExpectationsDiff {
    private List<String> missing = new ArrayList<>();
    private List<String> different = new ArrayList<>();

    private LockFileExpectationsDiff(Extensions extensions, Filters filters) {
      for (Extension lockedExtension : lockedExtensions) {
        final ArtifactIdentifier identifier = lockedExtension.getArtifactIdentifier();
        Optional<Extension> possiblyOtherArtifact = extensions.by(identifier);
        if (!possiblyOtherArtifact.isPresent()) {
          if (filters.allowMissing(lockedExtension)) {
            log.info(format(ROOT, "Ignoring missing %s", identifier));
          } else {
            missing.add(identifier.toString());
          }
        } else {
          Extension actualExtension = possiblyOtherArtifact.get();
          AtomicReference<Extension> lockedExtensionRef = new AtomicReference<>(lockedExtension);
          List<String> wrongs = findDiffs(lockedExtensionRef, actualExtension, filters);
          if (!wrongs.isEmpty()) {
            different.add(
                format(
                    ROOT,
                    "Expected %s but found %s, wrong %s",
                    lockedExtensionRef.get(),
                    actualExtension,
                    joinNouns(wrongs)));
          }
        }
      }
    }

    private List<String> findDiffs(
        AtomicReference<Extension> lockedExtensionRef, Extension actualExtension, Filters filters) {
      List<String> wrongs = new ArrayList<>();
      wrongs.addAll(diffHelper.diffIntegrity(lockedExtensionRef.get(), actualExtension, filters));
      wrongs.addAll(
          diffHelper.diffVersion(
              lockedExtensionRef, actualExtension, Extension::withVersion, filters));
      return wrongs;
    }
  }
}
