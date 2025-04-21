package se.vandmo.dependencylock.maven;

import java.util.List;
import java.util.stream.Stream;

public final class DiffReport {
  private final List<String> missing;
  private final List<String> different;
  private final List<String> extraneous;

  DiffReport(List<String> different, List<String> missing, List<String> extraneous) {
    super();
    this.different = different;
    this.missing = missing;
    this.extraneous = extraneous;
  }

  public boolean equals() {
    return missing.isEmpty() && different.isEmpty() && extraneous.isEmpty();
  }

  public Stream<String> report(String category) {
    Stream<String> result = Stream.empty();
    if (!missing.isEmpty()) {
      result =
          Stream.concat(
              result,
              Stream.concat(
                  Stream.of("Missing " + category + ":"),
                  missing.stream().map(line -> "  " + line)));
    }
    if (!extraneous.isEmpty()) {
      result =
          Stream.concat(
              result,
              Stream.concat(
                  Stream.of("Extraneous " + category + ":"),
                  extraneous.stream().map(line -> "  " + line)));
    }
    if (!different.isEmpty()) {
      result =
          Stream.concat(
              result,
              Stream.concat(
                  Stream.of("The following " + category + " differ:"),
                  different.stream().map(line -> "  " + line)));
    }
    return result;
  }
}
