package se.vandmo.dependencylock.maven;

public final class MojoExecutionRuntimeException extends RuntimeException {
  public MojoExecutionRuntimeException(String message, Throwable cause) {
    super(message, cause);
  }
}
