package se.vandmo.dependencylock.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public final class Checksum {

  public interface ChecksumCalculator {
    String calculateFor(ArtifactReference artifact, File file);

    String getAlgorithmHeader();
  }

  public static class DefaultChecksumCalculator implements ChecksumCalculator {
    private static final int BUFFER_SIZE = 8192;
    private final String algorithm;
    private final String algorithmHeader;

    public DefaultChecksumCalculator(final String algorithm) {
      this.algorithm = algorithm;
      this.algorithmHeader = algorithm.toLowerCase().replace("-", "") + ":";
    }

    @Override
    public String calculateFor(ArtifactReference artifact, File file) {
      try {
        byte[] hashed;
        byte[] buffer = new byte[BUFFER_SIZE];
        try (FileInputStream fis = new FileInputStream(file)) {
          MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
          int bytesRead = fis.read(buffer);
          while (bytesRead >= 0) {
            messageDigest.update(buffer, 0, bytesRead);
            bytesRead = fis.read(buffer);
          }
          hashed = messageDigest.digest();
        }
        return encodeHashed(hashed);
      } catch (NoSuchAlgorithmException e) {
        throw new IllegalStateException("Unknown checksum algorithm", e);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    private String encodeHashed(byte[] hashed) {
      return algorithmHeader + Base64.getEncoder().encodeToString(hashed);
    }

    @Override
    public String getAlgorithmHeader() {
      return algorithmHeader;
    }
  }

  private static final ChecksumCalculator SHA512 = new DefaultChecksumCalculator("SHA-512");

  private static ChecksumCalculator calculator = SHA512;

  static ChecksumCalculator getCalculator() {
    return Checksum.calculator;
  }

  static void setCalculator(ChecksumCalculator calculator) {
    Checksum.calculator = calculator;
  }

  public static String calculateFor(ArtifactReference artifact, File file) {
    return calculator.calculateFor(artifact, file);
  }
}
