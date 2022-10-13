package se.vandmo.dependencylock.maven.pom;

import static java.lang.String.format;
import static java.util.Locale.ROOT;
import static javax.xml.stream.XMLInputFactory.IS_COALESCING;
import static javax.xml.stream.XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES;
import static javax.xml.stream.XMLInputFactory.IS_VALIDATING;
import static javax.xml.stream.XMLInputFactory.SUPPORT_DTD;

import com.ctc.wstx.stax.WstxInputFactory;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import org.codehaus.stax2.XMLEventReader2;
import se.vandmo.dependencylock.maven.Artifact;
import se.vandmo.dependencylock.maven.ArtifactIdentifier;

public final class PomLockFile {

  private static final String POM_NS = "http://maven.apache.org/POM/4.0.0";
  private static final String DEPENDENCY_LOCK_NS = "urn:se.vandmo.dependencylock";
  private static final QName PROJECT = new QName(POM_NS, "project");
  private static final QName DEPENDENCIES = new QName(POM_NS, "dependencies");
  private static final QName DEPENDENCY = new QName(POM_NS, "dependency");
  private static final QName GROUP_ID = new QName(POM_NS, "groupId");
  private static final QName ARTIFACT_ID = new QName(POM_NS, "artifactId");
  private static final QName VERSION = new QName(POM_NS, "version");
  private static final QName TYPE = new QName(POM_NS, "type");
  private static final QName SCOPE = new QName(POM_NS, "scope");
  private static final QName CLASSIFIER = new QName(POM_NS, "classifier");
  private static final QName OPTIONAL = new QName(POM_NS, "optional");
  private static final QName INTEGRITY = new QName(DEPENDENCY_LOCK_NS, "integrity");

  public static List<Artifact> read(File file) {
    try {
      return doRead(file);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (XMLStreamException e) {
      throw new InvalidPomLockFile(e);
    }
  }

  private static List<Artifact> doRead(File file) throws IOException, XMLStreamException {
    WstxInputFactory inputFactory = createInputFactory();
    XMLEventReader2 reader = inputFactory.createXMLEventReader(file);
    while (reader.hasNextEvent()) {
      XMLEvent evt = reader.nextEvent();
      if (evt.isStartElement()) {
        QName name = evt.asStartElement().getName();
        if (!name.equals(PROJECT)) {
          throw new InvalidPomLockFile("Expected 'project'-element", evt.getLocation());
        }
        return fromProject(reader);
      }
    }
    throw new InvalidPomLockFile("Ended prematurely");
  }

  private static WstxInputFactory createInputFactory() {
    WstxInputFactory inputFactory = new WstxInputFactory();
    inputFactory.setProperty(IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    inputFactory.setProperty(SUPPORT_DTD, false);
    inputFactory.setProperty(IS_COALESCING, true);
    inputFactory.setProperty(IS_VALIDATING, false);
    return inputFactory;
  }

  private static List<Artifact> fromProject(XMLEventReader2 reader) throws XMLStreamException {
    while (reader.hasNextEvent()) {
      XMLEvent event = reader.nextEvent();
      if (event.isStartElement()) {
        QName name = event.asStartElement().getName();
        if (!name.equals(DEPENDENCIES)) {
          skipElement(reader);
        } else {
          return fromDependencies(reader);
        }
      }
    }
    throw new InvalidPomLockFile("Missing 'dependencies'-element");
  }

  private static List<Artifact> fromDependencies(XMLEventReader2 rdr) throws XMLStreamException {
    List<Artifact> result = new ArrayList<>();
    while (rdr.hasNextEvent()) {
      XMLEvent evt = rdr.nextEvent();
      if (evt.isStartElement()) {
        QName name = evt.asStartElement().getName();
        if (!name.equals(DEPENDENCY)) {
          skipElement(rdr);
        } else {
          result.add(fromDependency(rdr));
        }
      }
    }
    return result;
  }

  private static Artifact fromDependency(XMLEventReader2 rdr) throws XMLStreamException {
    String groupId = null;
    String artifactId = null;
    String version = null;
    String type = null;
    String scope = null;
    String classifier = null;
    boolean optional = false;
    String integrity = null;
    while (rdr.hasNextEvent()) {
      XMLEvent event = rdr.nextEvent();
      if (event.isStartElement()) {
        Location startElementLocation = event.getLocation();
        QName name = event.asStartElement().getName();
        if (name.equals(GROUP_ID)) {
          groupId = readSingleTextElement(rdr);
        } else if (name.equals(ARTIFACT_ID)) {
          artifactId = readSingleTextElement(rdr);
        } else if (name.equals(VERSION)) {
          version = readSingleTextElement(rdr);
        } else if (name.equals(TYPE)) {
          type = readSingleTextElement(rdr);
        } else if (name.equals(SCOPE)) {
          scope = readSingleTextElement(rdr);
        } else if (name.equals(CLASSIFIER)) {
          classifier = readSingleTextElement(rdr);
        } else if (name.equals(OPTIONAL)) {
          String optionalStr = readSingleTextElement(rdr);
          switch (optionalStr) {
            case "false":
              break;
            case "true":
              optional = true;
              break;
            default:
              throw new InvalidPomLockFile(format(ROOT, "Invalid optional value '%s' for dependency", optionalStr), startElementLocation);
          }
        } else if (name.equals(INTEGRITY)) {
          integrity = readSingleTextElement(rdr);
        } else {
          skipElement(rdr);
        }
      } else if (event.isEndElement()) {
        if (!event.asEndElement().getName().equals(DEPENDENCY)) {
          throw new InvalidPomLockFile("Expected '</dependency>'", event.getLocation());
        }
        if (groupId == null) {
          throw new InvalidPomLockFile("Missing groupId", event.getLocation());
        }
        if (artifactId == null) {
          throw new InvalidPomLockFile("Missing artifactId", event.getLocation());
        }
        if (version == null) {
          throw new InvalidPomLockFile("Missing version", event.getLocation());
        }
        if (type == null) {
          throw new InvalidPomLockFile("Missing type", event.getLocation());
        }
        if (scope == null) {
          throw new InvalidPomLockFile("Missing scope", event.getLocation());
        }
        if (integrity == null) {
          throw new InvalidPomLockFile("Missing integrity", event.getLocation());
        }
        return Artifact
            .builder()
            .artifactIdentifier(ArtifactIdentifier
                .builder()
                .groupId(groupId)
                .artifactId(artifactId)
                .classifier(Optional.ofNullable(classifier))
                .type(type)
                .build())
            .version(version)
            .scope(scope)
            .optional(optional)
            .integrity(Optional.of(integrity))
            .build();
      }
    }
    throw new InvalidPomLockFile("Ended prematurely");
  }

  private static String readSingleTextElement(XMLEventReader2 reader) throws XMLStreamException {
    XMLEvent characterEvent = expectEvent(reader);
    if (!characterEvent.isCharacters()) {
      throw new InvalidPomLockFile("Expected characters", characterEvent.getLocation());
    }
    String content = characterEvent.asCharacters().getData();
    XMLEvent endElementEvent = expectEvent(reader);
    if (!endElementEvent.isEndElement()) {
      throw new InvalidPomLockFile("Expected end of text element", endElementEvent.getLocation());
    }
    return content;
  }

  private static XMLEvent expectEvent(XMLEventReader2 reader) throws XMLStreamException {
    if (!reader.hasNextEvent()) {
      throw new InvalidPomLockFile("Ended prematurely");
    }
    return reader.nextEvent();
  }

  private static void skipElement(XMLEventReader2 reader) throws XMLStreamException {
    int level = 0;
    while (reader.hasNextEvent() && level >= 0) {
      XMLEvent event = reader.nextEvent();
      if (event.isStartElement()) {
        level += 1;
      } else if (event.isEndElement()) {
        level -= 1;
      }
    }
  }

  public static void main(String[] args) throws IOException, XMLStreamException {
    File f = new File("/home/mikaelv/expected-pom.xml");
    System.out.println(read(f));
  }
}
