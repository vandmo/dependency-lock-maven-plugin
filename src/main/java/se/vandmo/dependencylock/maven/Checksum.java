package se.vandmo.dependencylock.maven;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public final class Checksum {

  /**
   * The current algorithm that is being used. If this doesn't match an error will be thrown and
   * dependency files need to be regenerated.
   */
  public static final String ALGORITHM_HEADER = "sha512:";

  private static final ThreadLocal<MessageDigest> digest =
      new ThreadLocal<MessageDigest>() {
        @Override
        protected MessageDigest initialValue() {
          try {
            return MessageDigest.getInstance("SHA-512");
          } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
          }
        }
      };

  static {
  }

  public static String calculateFor(File file) {
    try {
      return calculateFor(Files.readAllBytes(file.toPath()));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  static String calculateFor(byte[] bytes) {
    byte[] hashed = digest.get().digest(bytes);
    return ALGORITHM_HEADER + Base64.getEncoder().encodeToString(hashed);
  }
}
