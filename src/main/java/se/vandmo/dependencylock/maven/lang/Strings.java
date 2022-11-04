package se.vandmo.dependencylock.maven.lang;

import java.util.List;

public final class Strings {
  private Strings() {}

  public static String joinNouns(List<String> nouns) {
    switch (nouns.size()) {
      case 0:
        return "";
      case 1:
        return nouns.get(0);
      default:
        int lastIdx = nouns.size() - 1;
        return String.join(
            " and ", String.join(", ", nouns.subList(0, lastIdx)), nouns.get(lastIdx));
    }
  }

}
