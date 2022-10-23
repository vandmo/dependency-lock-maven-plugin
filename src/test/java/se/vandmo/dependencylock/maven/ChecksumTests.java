package se.vandmo.dependencylock.maven;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

import java.io.File;
import org.junit.Test;

public final class ChecksumTests {

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
}
