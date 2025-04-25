package se.vandmo.dependencylock.maven;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class VersionUtils {
  private VersionUtils() {
    super();
  }

  static boolean snapshotMatch(String version, String otherVersion) {
    if (version.equals(otherVersion)) {
      return true;
    }
    return stripSnapshot(version).equals(stripSnapshot(otherVersion));
  }

  private static final Pattern SNAPSHOT_TIMESTAMP =
      Pattern.compile("^((?<base>.*)-)?([0-9]{8}\\.[0-9]{6}-[0-9]+)$");

  static String stripSnapshot(String version) {
    if (version.endsWith("-SNAPSHOT")) {
      return version.substring(0, version.length() - 9);
    }
    Matcher matcher = SNAPSHOT_TIMESTAMP.matcher(version);
    if (matcher.matches()) {
      return matcher.group("base");
    } else {
      return version;
    }
  }
}
