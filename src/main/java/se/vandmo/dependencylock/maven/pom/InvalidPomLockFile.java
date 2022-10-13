package se.vandmo.dependencylock.maven.pom;

import static java.lang.String.format;
import static java.util.Locale.ROOT;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

public final class InvalidPomLockFile extends RuntimeException {

  InvalidPomLockFile(String message) {
    super(message);
  }

  InvalidPomLockFile(String message, Location location) {
    super(format(ROOT, "%s on line %d", message, location.getLineNumber()));
  }

  InvalidPomLockFile(XMLStreamException cause) {
    super(cause);
  }

}
