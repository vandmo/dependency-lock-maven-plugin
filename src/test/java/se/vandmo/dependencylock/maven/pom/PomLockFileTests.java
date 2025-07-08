package se.vandmo.dependencylock.maven.pom;

import static java.lang.String.format;
import static java.util.Locale.ROOT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import org.junit.Test;
import se.vandmo.dependencylock.maven.ArtifactIdentifier;
import se.vandmo.dependencylock.maven.Dependency;

public final class PomLockFileTests {

  @Test
  public void element_when_text() {
    InvalidPomLockFile exception = assertInvalid("element-when-text");
    assertEquals("Expected characters on line 5", exception.getMessage());
  }

  @Test
  public void text_then_element() {
    InvalidPomLockFile exception = assertInvalid("text-then-element");
    assertEquals("Expected end of text element on line 5", exception.getMessage());
  }

  @Test
  public void wrong_top() {
    InvalidPomLockFile exception = assertInvalid("wrong-top");
    assertEquals("Expected 'project'-element on line 2", exception.getMessage());
  }

  @Test
  public void wrong_end() {
    InvalidPomLockFile exception = assertInvalid("wrong-end");
    assertThat(exception.getCause(), instanceOf(XMLStreamException.class));
  }

  @Test
  public void wrong_optional() {
    InvalidPomLockFile exception = assertInvalid("wrong-optional");
    assertEquals(
        "Invalid optional value 'muahaha' for dependency on line 5", exception.getMessage());
  }

  @Test
  public void no_groupId() {
    InvalidPomLockFile exception = assertInvalid("no-groupId");
    assertEquals("Missing groupId on line 15", exception.getMessage());
  }

  @Test
  public void no_artifactId() {
    InvalidPomLockFile exception = assertInvalid("no-artifactId");
    assertEquals("Missing artifactId on line 15", exception.getMessage());
  }

  @Test
  public void no_version() {
    InvalidPomLockFile exception = assertInvalid("no-version");
    assertEquals("Missing version on line 15", exception.getMessage());
  }

  @Test
  public void no_type() {
    InvalidPomLockFile exception = assertInvalid("no-type");
    assertEquals("Missing type on line 15", exception.getMessage());
  }

  @Test
  public void no_scope() {
    InvalidPomLockFile exception = assertInvalid("no-scope");
    assertEquals("Missing scope on line 15", exception.getMessage());
  }

  @Test
  public void no_optional() {
    InvalidPomLockFile exception = assertInvalid("no-optional");
    assertEquals("Missing optional on line 15", exception.getMessage());
  }

  @Test
  public void no_integrity() {
    InvalidPomLockFile exception = assertInvalid("no-integrity");
    assertEquals("Missing integrity on line 15", exception.getMessage());
  }

  @Test
  public void no_dependencies() {
    InvalidPomLockFile exception = assertInvalid("no-dependencies");
    assertEquals("Missing 'dependencies' element", exception.getMessage());
  }

  @Test
  public void valid() {
    assertEquals(
        Arrays.asList(
            Dependency.builder()
                .artifactIdentifier(
                    ArtifactIdentifier.builder()
                        .groupId("io.netty")
                        .artifactId("netty-buffer")
                        .build())
                .version("4.1.65.Final")
                .integrity("sha512:something")
                .scope("compile")
                .build()),
        read("valid"));
  }

  private static InvalidPomLockFile assertInvalid(String name) {
    return assertThrows(InvalidPomLockFile.class, () -> read(name));
  }

  private static List<Dependency> read(String name) {
    return PomLockFile.read(
            new File(
                format(
                    ROOT, "src/test/resources/se/vandmo/dependencylock/maven/poms/%s.xml", name)))
        .dependencies
        .get();
  }
}
