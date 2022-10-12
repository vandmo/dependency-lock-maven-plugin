package se.vandmo.dependencylock.maven;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.io.BaseEncoding;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.lang3.StringUtils;

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

  public static String calculateFor(File file) {
    try {
      return calculateFor(Files.toByteArray(file));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  static String calculateFor(byte[] bytes) {
    return ALGORITHM_HEADER
        + BaseEncoding.base64()
        .encode(SHA512_DIGEST.digest(bytes));
  }

  public static void checkAlgorithmHeader(String value, String errorMessageTemplate) {
    checkArgument(StringUtils.startsWith(value, ALGORITHM_HEADER),
        errorMessageTemplate,
        ALGORITHM_HEADER,
        value);
  }
}
