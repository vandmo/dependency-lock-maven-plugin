package io.mvnpm.maven.locker.pom;

import static java.lang.String.format;
import static java.util.Locale.ROOT;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

public final class InvalidPomLockFileException extends RuntimeException {

  public InvalidPomLockFileException(String message) {
    super(message);
  }

  public InvalidPomLockFileException(String message, Location location) {
    super(format(ROOT, "%s on line %d", message, location.getLineNumber()));
  }

  public InvalidPomLockFileException(XMLStreamException cause) {
    super(cause);
  }
}
