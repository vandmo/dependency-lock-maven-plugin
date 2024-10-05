package se.vandmo.dependencylock.maven.lang;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

  @Test
  public void isBlank() {
    assertTrue(Strings.isBlank(null));
    assertTrue(Strings.isBlank(""));
    assertTrue(Strings.isBlank(" "));
    assertTrue(Strings.isBlank(" \n\t "));
    assertFalse(Strings.isBlank("a"));
  }

  @Test
  public void startsWith() {
    assertTrue(Strings.startsWith(null, null));
    assertFalse(Strings.startsWith(null, "abc"));
    assertFalse(Strings.startsWith("abc", null));
    assertTrue(Strings.startsWith("abcdef", "abc"));
    assertFalse(Strings.startsWith("defabc", "abc"));
  }
}
