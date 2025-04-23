package se.vandmo.dependencylock.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
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
  private static final int BUFFER_SIZE = 8192;

  static {
  }

  public static String calculateFor(File file) {
    try {
      byte[] hashed;
      byte[] buffer = new byte[BUFFER_SIZE];
      try (FileInputStream fis = new FileInputStream(file)) {
        MessageDigest messageDigest = digest.get();
        int bytesRead = fis.read(buffer);
        while (bytesRead >= 0) {
          messageDigest.update(buffer, 0, bytesRead);
          bytesRead = fis.read(buffer);
        }
        hashed = messageDigest.digest();
      }
      return encodeHashed(hashed);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static String encodeHashed(byte[] hashed) {
    return ALGORITHM_HEADER + Base64.getEncoder().encodeToString(hashed);
  }

  static String calculateFor(byte[] bytes) {
    byte[] hashed = digest.get().digest(bytes);
    return encodeHashed(hashed);
  }
}
