package se.vandmo.dependencylock.maven;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class ChecksumTests {

  @Rule public final TemporaryFolder folder = new TemporaryFolder();

  @Test(expected = NullPointerException.class)
  public void calculateFor_byteArrayNull() {
    Checksum.calculateFor((byte[]) null);
  }

  @Test(expected = NullPointerException.class)
  public void calculateFor_fileNull() {
    Checksum.calculateFor((File) null);
  }

  @Test
  public void calculateFor() {
    assertEquals(
        "sha512:23I/NBoELY3hqoE+/V4C/BdFzL4llIYldRSATi7EvOuypG8eStRCFUlD+el+G8R8OuDt2rfeDAGpxR8VNCpbGQ==",
        Checksum.calculateFor("abcdefghijklmno".getBytes(UTF_8)));
  }

  @Test
  public void calculateForFile() throws IOException {
    final File file = folder.newFile("abcdefghijklmno.txt");
    Files.write(file.toPath(), "abcdefghijklmno".getBytes(UTF_8));
    assertEquals(
        "sha512:23I/NBoELY3hqoE+/V4C/BdFzL4llIYldRSATi7EvOuypG8eStRCFUlD+el+G8R8OuDt2rfeDAGpxR8VNCpbGQ==",
        Checksum.calculateFor(file));
  }
}
