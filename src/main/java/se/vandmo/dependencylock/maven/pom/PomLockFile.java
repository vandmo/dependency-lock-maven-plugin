package se.vandmo.dependencylock.maven.pom;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
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
import se.vandmo.dependencylock.maven.Artifacts;
import se.vandmo.dependencylock.maven.Dependency;
import se.vandmo.dependencylock.maven.Extension;
import se.vandmo.dependencylock.maven.Plugin;

public final class PomLockFile {
  public static final class Build {
    public final List<Plugin> plugins;
    public final List<Extension> extensions;

    public Build(List<Plugin> plugins, List<Extension> extensions) {
      this.plugins = unmodifiableList(new ArrayList<>(plugins));
      this.extensions = unmodifiableList(new ArrayList<>(extensions));
    }
  }

  public static final class Contents {
    public final List<Dependency> dependencies;
    public final Optional<Build> build;

    public Contents(List<Dependency> dependencies) {
      this.dependencies = unmodifiableList(new ArrayList<>(dependencies));
      this.build = Optional.empty();
    }

    public Contents(
        List<Dependency> dependencies, List<Plugin> plugins, List<Extension> extensions) {
      this.dependencies = unmodifiableList(new ArrayList<>(dependencies));
      this.build = Optional.of(new Build(plugins, extensions));
    }
  }

  private static final String POM_NS = "http://maven.apache.org/POM/4.0.0";
  private static final String DEPENDENCY_LOCK_NS = "urn:se.vandmo.dependencylock";
  private static final QName PROJECT = new QName(POM_NS, "project");
  private static final QName DEPENDENCIES = new QName(POM_NS, "dependencies");
  private static final QName DEPENDENCY = new QName(POM_NS, "dependency");
  private static final QName EXTENSIONS = new QName(POM_NS, "extensions");
  private static final QName BUILD = new QName(POM_NS, "build");
  private static final QName EXTENSION = new QName(POM_NS, "extension");
  private static final QName PLUGINS = new QName(POM_NS, "plugins");
  private static final QName PLUGIN = new QName(POM_NS, "plugin");
  private static final QName GROUP_ID = new QName(POM_NS, "groupId");
  private static final QName ARTIFACT_ID = new QName(POM_NS, "artifactId");
  private static final QName VERSION = new QName(POM_NS, "version");
  private static final QName TYPE = new QName(POM_NS, "type");
  private static final QName SCOPE = new QName(POM_NS, "scope");
  private static final QName CLASSIFIER = new QName(POM_NS, "classifier");
  private static final QName OPTIONAL = new QName(POM_NS, "optional");
  private static final QName INTEGRITY = new QName(DEPENDENCY_LOCK_NS, "integrity");

  public static Contents read(File file) {
    try {
      return doRead(file);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (XMLStreamException e) {
      throw new InvalidPomLockFile(e);
    }
  }

  private static Contents doRead(File file) throws IOException, XMLStreamException {
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

  private static Contents fromProject(XMLEventReader2 reader) throws XMLStreamException {
    List<Dependency> dependencies = null;
    List<Plugin> plugins = null;
    List<Extension> extensions = null;
    boolean inBuild = false;
    boolean buildFound = false;
    while (reader.hasNextEvent()) {
      XMLEvent event = reader.nextEvent();
      if (event.isStartElement()) {
        QName name = event.asStartElement().getName();
        if (name.equals(DEPENDENCIES)) {
          if (null != dependencies) {
            throw new InvalidPomLockFile("Duplicate 'dependencies' element");
          }
          dependencies = fromDependencies(reader);
        } else if (name.equals(BUILD)) {
          if (buildFound) {
            throw new InvalidPomLockFile("Duplicate 'build' element");
          }
          buildFound = true;
          inBuild = true;
        } else if (inBuild && name.equals(EXTENSIONS)) {
          if (null != extensions) {
            throw new InvalidPomLockFile("Duplicate 'extensions' element");
          }
          extensions = fromExtensions(reader);
        } else if (inBuild && name.equals(PLUGINS)) {
          if (null != plugins) {
            throw new InvalidPomLockFile("Duplicate 'plugins' element");
          }
          plugins = fromPlugins(reader);
        } else {
          skipElement(reader);
        }
      } else if (event.isEndElement()) {
        if (event.asEndElement().getName().equals(BUILD)) {
          inBuild = false;
        }
      }
    }
    if (null == dependencies) {
      throw new InvalidPomLockFile("Missing 'dependencies' element");
    }
    if (buildFound) {
      if (extensions == null) {
        throw new InvalidPomLockFile("Missing 'extensions' element");
      }
      if (plugins == null) {
        throw new InvalidPomLockFile("Missing 'plugins' element");
      }
      return new Contents(dependencies, plugins, extensions);
    }
    return new Contents(dependencies);
  }

  private static List<Dependency> fromDependencies(XMLEventReader2 rdr) throws XMLStreamException {
    List<Dependency> result = new ArrayList<>();
    while (rdr.hasNextEvent()) {
      XMLEvent evt = rdr.nextEvent();
      if (evt.isStartElement()) {
        QName name = evt.asStartElement().getName();
        if (!name.equals(DEPENDENCY)) {
          skipElement(rdr);
        } else {
          result.add(fromDependency(rdr));
        }
      } else if (evt.isEndElement()) {
        QName name = evt.asEndElement().getName();
        if (name.equals(DEPENDENCIES)) {
          return result;
        }
      }
    }
    throw new InvalidPomLockFile("Ended prematurely");
  }

  private static List<Artifact> fromPluginDependencies(XMLEventReader2 rdr)
      throws XMLStreamException {
    List<Artifact> result = new ArrayList<>();
    while (rdr.hasNextEvent()) {
      XMLEvent evt = rdr.nextEvent();
      if (evt.isStartElement()) {
        QName name = evt.asStartElement().getName();
        if (!name.equals(DEPENDENCY)) {
          skipElement(rdr);
        } else {
          result.add(fromPluginDependency(rdr));
        }
      } else if (evt.isEndElement()) {
        QName name = evt.asEndElement().getName();
        if (name.equals(DEPENDENCIES)) {
          return result;
        }
      }
    }
    throw new InvalidPomLockFile("Ended prematurely");
  }

  private static List<Extension> fromExtensions(XMLEventReader2 rdr) throws XMLStreamException {
    List<Extension> result = new ArrayList<>();
    while (rdr.hasNextEvent()) {
      XMLEvent evt = rdr.nextEvent();
      if (evt.isStartElement()) {
        QName name = evt.asStartElement().getName();
        if (!name.equals(EXTENSION)) {
          skipElement(rdr);
        } else {
          result.add(fromExtension(rdr));
        }
      } else if (evt.isEndElement()) {
        QName name = evt.asEndElement().getName();
        if (name.equals(EXTENSIONS)) {
          return result;
        }
      }
    }
    throw new InvalidPomLockFile("Ended prematurely");
  }

  private static List<Plugin> fromPlugins(XMLEventReader2 rdr) throws XMLStreamException {
    List<Plugin> result = new ArrayList<>();
    while (rdr.hasNextEvent()) {
      XMLEvent evt = rdr.nextEvent();
      if (evt.isStartElement()) {
        QName name = evt.asStartElement().getName();
        if (!name.equals(PLUGIN)) {
          skipElement(rdr);
        } else {
          result.add(fromPlugin(rdr));
        }
      } else if (evt.isEndElement()) {
        QName name = evt.asEndElement().getName();
        if (name.equals(PLUGINS)) {
          return result;
        }
      }
    }
    throw new InvalidPomLockFile("Ended prematurely");
  }

  private static Dependency fromDependency(XMLEventReader2 rdr) throws XMLStreamException {
    String groupId = null;
    String artifactId = null;
    String version = null;
    String type = null;
    String scope = null;
    String classifier = null;
    Boolean optional = null;
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
              optional = false;
              break;
            case "true":
              optional = true;
              break;
            default:
              throw new InvalidPomLockFile(
                  format(ROOT, "Invalid optional value '%s' for dependency", optionalStr),
                  startElementLocation);
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
        if (optional == null) {
          throw new InvalidPomLockFile("Missing optional", event.getLocation());
        }
        if (integrity == null) {
          throw new InvalidPomLockFile("Missing integrity", event.getLocation());
        }
        return Dependency.builder()
            .artifactIdentifier(
                ArtifactIdentifier.builder()
                    .groupId(groupId)
                    .artifactId(artifactId)
                    .classifier(Optional.ofNullable(classifier))
                    .type(type)
                    .build())
            .version(version)
            .integrity(integrity)
            .scope(scope)
            .optional(optional)
            .build();
      }
    }
    throw new InvalidPomLockFile("Ended prematurely");
  }

  private static Artifact fromPluginDependency(XMLEventReader2 rdr) throws XMLStreamException {
    String groupId = null;
    String artifactId = null;
    String version = null;
    String type = null;
    String classifier = null;
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
        } else if (name.equals(CLASSIFIER)) {
          classifier = readSingleTextElement(rdr);
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
        if (integrity == null) {
          throw new InvalidPomLockFile("Missing integrity", event.getLocation());
        }
        return Artifact.builder()
            .artifactIdentifier(
                ArtifactIdentifier.builder()
                    .groupId(groupId)
                    .artifactId(artifactId)
                    .classifier(Optional.ofNullable(classifier))
                    .type(type)
                    .build())
            .version(version)
            .integrity(integrity)
            .build();
      }
    }
    throw new InvalidPomLockFile("Ended prematurely");
  }

  private static Extension fromExtension(XMLEventReader2 rdr) throws XMLStreamException {
    String groupId = null;
    String artifactId = null;
    String version = null;
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
        } else if (name.equals(INTEGRITY)) {
          integrity = readSingleTextElement(rdr);
        } else {
          skipElement(rdr);
        }
      } else if (event.isEndElement()) {
        if (!event.asEndElement().getName().equals(EXTENSION)) {
          throw new InvalidPomLockFile("Expected '</extension>'", event.getLocation());
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
        if (integrity == null) {
          throw new InvalidPomLockFile("Missing integrity", event.getLocation());
        }
        return Extension.builder()
            .artifactIdentifier(
                ArtifactIdentifier.builder().groupId(groupId).artifactId(artifactId).build())
            .version(version)
            .integrity(integrity)
            .build();
      }
    }
    throw new InvalidPomLockFile("Ended prematurely");
  }

  private static Plugin fromPlugin(XMLEventReader2 rdr) throws XMLStreamException {
    String groupId = null;
    String artifactId = null;
    String version = null;
    String integrity = null;
    List<Artifact> artifacts = emptyList();
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
        } else if (name.equals(INTEGRITY)) {
          integrity = readSingleTextElement(rdr);
        } else if (name.equals(DEPENDENCIES)) {
          artifacts = fromPluginDependencies(rdr);
        } else {
          skipElement(rdr);
        }
      } else if (event.isEndElement()) {
        if (!event.asEndElement().getName().equals(PLUGIN)) {
          throw new InvalidPomLockFile("Expected '</plugin>'", event.getLocation());
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
        if (integrity == null) {
          throw new InvalidPomLockFile("Missing integrity", event.getLocation());
        }
        return Plugin.builder()
            .artifactIdentifier(
                ArtifactIdentifier.builder()
                    .groupId(groupId)
                    .artifactId(artifactId)
                    .type("maven-plugin")
                    .build())
            .version(version)
            .integrity(integrity)
            .artifacts(Artifacts.fromArtifacts(artifacts))
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
}
