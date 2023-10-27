package io.mvnpm.maven.locker;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public final class Checksum {
  private static final MessageDigest SHA512_DIGEST;

  /**
   * The current algorithm that is being used. If this doesn't match an error will be thrown and
   * dependency files need to be regenerated.
   */
  public static final String ALGORITHM_HEADER = "sha512:";

  static {
    try {
      SHA512_DIGEST = MessageDigest.getInstance("SHA-512");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  public static String forFile(File file) {
    try {
      return forBytes(Files.readAllBytes(file.toPath()));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static String forBytes(byte[] bytes) {
    byte[] hashed = SHA512_DIGEST.digest(bytes);
    return ALGORITHM_HEADER + Base64.getEncoder().encodeToString(hashed);
  }
}
