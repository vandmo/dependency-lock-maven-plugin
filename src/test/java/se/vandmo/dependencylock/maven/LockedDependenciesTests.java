package se.vandmo.dependencylock.maven;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class LockedDependenciesTests {

  @Test(expected = NullPointerException.class)
  public void joinNouns_null() {
    LockedDependencies.joinNouns(null);
  }

  @Test
  public void joinNouns() {
    assertEquals("", LockedDependencies.joinNouns(emptyList()));
    assertEquals("a", LockedDependencies.joinNouns(asList("a")));
    assertEquals("a and b", LockedDependencies.joinNouns(asList("a", "b")));
    assertEquals("a, b and c", LockedDependencies.joinNouns(asList("a", "b", "c")));
    assertEquals("a, b, c and d", LockedDependencies.joinNouns(asList("a", "b", "c", "d")));
  }
}
