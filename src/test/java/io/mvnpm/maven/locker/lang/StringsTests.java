package io.mvnpm.maven.locker.lang;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class StringsTests {

  @Test(expected = NullPointerException.class)
  public void joinNouns_null() {
    Strings.joinNouns(null);
  }

  @Test
  public void joinNouns() {
    assertEquals("", Strings.joinNouns(emptyList()));
    assertEquals("a", Strings.joinNouns(asList("a")));
    assertEquals("a and b", Strings.joinNouns(asList("a", "b")));
    assertEquals("a, b and c", Strings.joinNouns(asList("a", "b", "c")));
    assertEquals("a, b, c and d", Strings.joinNouns(asList("a", "b", "c", "d")));
  }
}
