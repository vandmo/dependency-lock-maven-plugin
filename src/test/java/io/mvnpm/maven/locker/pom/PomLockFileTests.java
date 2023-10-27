package io.mvnpm.maven.locker.pom;

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
import io.mvnpm.maven.locker.Artifact;
import io.mvnpm.maven.locker.ArtifactIdentifier;

public final class PomLockFileTests {

  @Test
  public void element_when_text() {
    InvalidPomLockFileException exception = assertInvalid("element-when-text");
    assertEquals("Expected characters on line 6", exception.getMessage());
  }

  @Test
  public void text_then_element() {
    InvalidPomLockFileException exception = assertInvalid("text-then-element");
    assertEquals("Expected end of text element on line 7", exception.getMessage());
  }

  @Test
  public void wrong_top() {
    InvalidPomLockFileException exception = assertInvalid("wrong-top");
    assertEquals("Expected 'project'-element on line 2", exception.getMessage());
  }

  @Test
  public void wrong_end() {
    InvalidPomLockFileException exception = assertInvalid("wrong-end");
    assertThat(exception.getCause(), instanceOf(XMLStreamException.class));
  }

  @Test
  public void wrong_optional() {
    InvalidPomLockFileException exception = assertInvalid("wrong-optional");
    assertEquals(
        "Invalid optional value 'muahaha' for dependency on line 6", exception.getMessage());
  }

  @Test
  public void no_groupId() {
    InvalidPomLockFileException exception = assertInvalid("no-groupId");
    assertEquals("Missing groupId on line 15", exception.getMessage());
  }

  @Test
  public void no_artifactId() {
    InvalidPomLockFileException exception = assertInvalid("no-artifactId");
    assertEquals("Missing artifactId on line 15", exception.getMessage());
  }

  @Test
  public void no_version() {
    InvalidPomLockFileException exception = assertInvalid("no-version");
    assertEquals("Missing version on line 15", exception.getMessage());
  }

  @Test
  public void no_type() {
    assertValid("no-type");
  }

  @Test
  public void no_scope() {
    assertValid("no-scope");
  }

  @Test
  public void no_optional() {
    assertValid("no-optional");
  }

  @Test
  public void no_integrity() {
    InvalidPomLockFileException exception = assertInvalid("no-integrity");
    assertEquals("Missing integrity property for: io.netty--netty-common", exception.getMessage());
  }

  @Test
  public void no_dependencies() {
    InvalidPomLockFileException exception = assertInvalid("no-dependencies");
    assertEquals("Missing 'dependencyManagement' element", exception.getMessage());
  }

  @Test
  public void valid() {
    assertValid("valid");
  }

  private static void assertValid(String name) {
    assertEquals(
        Arrays.asList(
            Artifact.builder()
                .artifactIdentifier(
                    ArtifactIdentifier.builder()
                        .groupId("io.netty")
                        .artifactId("netty-buffer")
                        .build())
                .version("4.1.65.Final")
                .scope("compile")
                .integrity("sha512:something")
                .build()),
        read(name));
  }

  private static InvalidPomLockFileException assertInvalid(String name) {
    return assertThrows(InvalidPomLockFileException.class, () -> read(name));
  }

  private static List<Artifact> read(String name) {
    return PomLockFile.read(
        new File(
            format(ROOT, "src/test/resources/io/mvnpm/maven/locker/poms/%s.xml", name)));
  }
}
