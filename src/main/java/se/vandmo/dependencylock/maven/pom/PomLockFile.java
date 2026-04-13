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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.codehaus.stax2.XMLEventReader2;
import se.vandmo.dependencylock.maven.Artifact;
import se.vandmo.dependencylock.maven.ArtifactIdentifier;
import se.vandmo.dependencylock.maven.Artifacts;
import se.vandmo.dependencylock.maven.Dependencies;
import se.vandmo.dependencylock.maven.Dependency;
import se.vandmo.dependencylock.maven.Extension;
import se.vandmo.dependencylock.maven.Plugin;
import se.vandmo.dependencylock.maven.ProfileEntry;
import se.vandmo.dependencylock.maven.mojos.model.Activation;
import se.vandmo.dependencylock.maven.mojos.model.ActivationOS;
import se.vandmo.dependencylock.maven.mojos.model.ActivationProperty;
import se.vandmo.dependencylock.maven.mojos.model.Profile;
import se.vandmo.dependencylock.maven.versions.VersionConstraint;
import se.vandmo.dependencylock.maven.versions.VersionConstraints;

public final class PomLockFile {

  private static final String V2 = "2";
  private static final String V3 = "3";

  public static final class Contents {
    public final Optional<List<Dependency>> dependencies;
    public final Optional<Collection<ProfileEntry>> profiles;
    public final Optional<List<Plugin>> plugins;
    public final Optional<List<Extension>> extensions;

    public Contents(List<Dependency> dependencies, Collection<ProfileEntry> profiles) {
      this(Optional.of(dependencies), Optional.empty(), Optional.empty(), Optional.of(profiles));
    }

    public Contents(
        Optional<List<Dependency>> dependencies,
        Optional<List<Plugin>> plugins,
        Optional<List<Extension>> extensions,
        Optional<Collection<ProfileEntry>> profiles) {
      this.dependencies = dependencies.map(d -> unmodifiableList(new ArrayList<>(d)));
      this.plugins = plugins;
      this.extensions = extensions;
      this.profiles = profiles;
    }
  }

  private static final String POM_NS = "http://maven.apache.org/POM/4.0.0";
  private static final String DEPENDENCY_LOCK_NS = "urn:se.vandmo.dependencylock";
  private static final QName LOCKFILE_VERSION = new QName(DEPENDENCY_LOCK_NS, "version");
  private static final QName PROJECT = new QName(POM_NS, "project");
  private static final QName DEPENDENCIES = new QName(POM_NS, "dependencies");
  private static final QName ACTIVATION = new QName(POM_NS, "activation");
  private static final QName OS = new QName(POM_NS, "os");
  private static final QName ARCH = new QName(POM_NS, "arch");
  private static final QName FAMILY = new QName(POM_NS, "family");
  private static final QName PROPERTY = new QName(POM_NS, "property");
  private static final QName NAME = new QName(POM_NS, "name");
  private static final QName VALUE = new QName(POM_NS, "value");
  private static final QName PROFILES = new QName(POM_NS, "profiles");
  private static final QName PROFILE = new QName(POM_NS, "profile");
  private static final QName ID = new QName(POM_NS, "id");
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

  public static Contents read(File file, boolean requireScope) throws InvalidPomLockFile {
    try {
      return doRead(file, requireScope);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (XMLStreamException e) {
      throw new InvalidPomLockFile(e);
    }
  }

  private static Contents doRead(File file, boolean requireScope)
      throws IOException, XMLStreamException {
    WstxInputFactory inputFactory = createInputFactory();
    XMLEventReader2 reader = inputFactory.createXMLEventReader(file);
    while (reader.hasNextEvent()) {
      XMLEvent evt = reader.nextEvent();
      if (evt.isStartElement()) {
        final StartElement element = evt.asStartElement();
        QName name = element.getName();
        if (!name.equals(PROJECT)) {
          throw new InvalidPomLockFile("Expected 'project'-element", evt.getLocation());
        }
        final Attribute attribute = element.getAttributeByName(LOCKFILE_VERSION);
        if (attribute == null) {
          return fromProject(reader, requireScope);
        }
        final String version = attribute.getValue();
        if (version.equals(V2)) {
          return fromProjectV2(reader, requireScope);
        }
        throw new InvalidPomLockFile(
            format(
                ROOT,
                "Unexpected lock file version \"%s\". The file might have been generated by a newer"
                    + " version of this plugin than is used for this check.",
                version));
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

  private static Contents fromProject(XMLEventReader2 reader, boolean requireScope)
      throws XMLStreamException {
    List<Dependency> dependencies = null;
    while (reader.hasNextEvent()) {
      XMLEvent event = reader.nextEvent();
      if (event.isStartElement()) {
        QName name = event.asStartElement().getName();
        if (name.equals(DEPENDENCIES)) {
          if (null != dependencies) {
            throw new InvalidPomLockFile("Duplicate 'dependencies' element");
          }
          dependencies = fromDependencies(reader, requireScope);
        } else {
          skipElement(reader);
        }
      }
    }
    if (null == dependencies) {
      throw new InvalidPomLockFile("Missing 'dependencies' element");
    }
    return new Contents(dependencies, Collections.emptyList());
  }

  private static Contents fromProjectV2(XMLEventReader2 reader, boolean requireScope)
      throws XMLStreamException {
    List<Dependency> dependencies = null;
    List<Plugin> plugins = null;
    List<Extension> extensions = null;
    Collection<ProfileEntry> profiles = new ArrayList<>();
    boolean inBuild = false;
    boolean inProfiles = false;
    boolean buildFound = false;
    while (reader.hasNextEvent()) {
      XMLEvent event = reader.nextEvent();
      if (event.isStartElement()) {
        QName name = event.asStartElement().getName();
        if (name.equals(DEPENDENCIES)) {
          if (null != dependencies) {
            throw new InvalidPomLockFile(
                "Unexpected 'dependencies' element found", event.getLocation());
          }
          dependencies = fromDependencies(reader, requireScope);
        } else if (name.equals(BUILD)) {
          if (inProfiles || buildFound) {
            throw new InvalidPomLockFile("Unexpected 'build' element found.");
          }
          buildFound = true;
          inBuild = true;
        } else if (name.equals(EXTENSIONS)) {
          if (!inBuild || null != extensions) {
            throw new InvalidPomLockFile("Unexpected 'extensions' element", event.getLocation());
          }
          extensions = fromExtensions(reader);
        } else if (name.equals(PLUGINS)) {
          if (!inBuild || null != plugins) {
            throw new InvalidPomLockFile("Unexpected 'plugins' element", event.getLocation());
          }
          plugins = fromPlugins(reader);
        } else if (name.equals(PROFILE)) {
          if (!inProfiles) {
            throw new InvalidPomLockFile("Unexpected 'profile' element", event.getLocation());
          }
          profiles.add(fromProfile(reader, requireScope));
        } else if (name.equals(PROFILES)) {
          inProfiles = true;
        } else {
          skipElement(reader);
        }
      } else if (event.isEndElement()) {
        if (event.asEndElement().getName().equals(BUILD)) {
          inBuild = false;
        } else if (event.asEndElement().getName().equals(PROFILES)) {
          inProfiles = false;
        }
      }
    }
    if (buildFound) {
      return new Contents(
          Optional.ofNullable(dependencies),
          Optional.ofNullable(plugins),
          Optional.ofNullable(extensions),
          Optional.of(profiles));
    }
    return new Contents(dependencies, profiles);
  }

  private static ActivationOS fromActivationOs(XMLEventReader2 reader) throws XMLStreamException {
    String arch = null;
    String family = null;
    String name = null;
    String version = null;

    while (reader.hasNextEvent()) {
      XMLEvent event = reader.nextEvent();
      if (event.isStartElement()) {
        QName nodeName = event.asStartElement().getName();
        if (nodeName.equals(ARCH)) {
          if (arch != null) {
            throw new InvalidPomLockFile("Duplicate arch node found");
          }
          arch = readSingleTextElement(reader);
        } else if (nodeName.equals(FAMILY)) {
          if (family != null) {
            throw new InvalidPomLockFile("Duplicate family node found");
          }
          family = readSingleTextElement(reader);
        } else if (nodeName.equals(NAME)) {
          if (name != null) {
            throw new InvalidPomLockFile("Duplicate name node found");
          }
          name = readSingleTextElement(reader);
        } else if (nodeName.equals(VERSION)) {
          if (version != null) {
            throw new InvalidPomLockFile("Duplicate version node found");
          }
          version = readSingleTextElement(reader);
        } else {
          skipElement(reader);
        }
      } else if (event.isEndElement()) {
        if (event.asEndElement().getName().equals(OS)) {
          ActivationOS activationOs = new ActivationOS();
          activationOs.setArch(arch);
          activationOs.setFamily(family);
          activationOs.setName(name);
          activationOs.setVersion(version);
          return activationOs;
        }
      }
    }
    throw new InvalidPomLockFile("Ended prematurely");
  }

  private static ActivationProperty fromActivationProperty(XMLEventReader2 reader)
      throws XMLStreamException {
    String name = null;
    String value = null;
    while (reader.hasNextEvent()) {
      XMLEvent event = reader.nextEvent();
      if (event.isStartElement()) {
        QName nodeName = event.asStartElement().getName();
        if (nodeName.equals(NAME)) {
          if (name != null) {
            throw new InvalidPomLockFile("Duplicate name node found");
          }
          name = readSingleTextElement(reader);
        } else if (nodeName.equals(VALUE)) {
          if (value != null) {
            throw new InvalidPomLockFile("Duplicate value node found");
          }
          value = readSingleTextElement(reader);
        } else {
          skipElement(reader);
        }
      } else if (event.isEndElement()) {
        if (event.asEndElement().getName().equals(PROPERTY)) {
          if (name == null) {
            throw new InvalidPomLockFile("Missing name node found", event.getLocation());
          }
          ActivationProperty result = new ActivationProperty();
          result.setName(name);
          result.setValue(value);
          return result;
        }
      }
    }
    throw new InvalidPomLockFile("Ended prematurely");
  }

  private static Activation fromActivation(XMLEventReader2 reader) throws XMLStreamException {
    ActivationOS os = null;
    ActivationProperty property = null;
    while (reader.hasNextEvent()) {
      XMLEvent event = reader.nextEvent();
      if (event.isStartElement()) {
        QName nodeName = event.asStartElement().getName();
        if (nodeName.equals(OS)) {
          if (os != null) {
            throw new InvalidPomLockFile("Duplicate os node found");
          }
          os = fromActivationOs(reader);
        } else if (nodeName.equals(PROPERTY)) {
          if (property != null) {
            throw new InvalidPomLockFile("Duplicate property node found");
          }
          property = fromActivationProperty(reader);
        } else {
          skipElement(reader);
        }
      } else if (event.isEndElement()) {
        if (event.asEndElement().getName().equals(ACTIVATION)) {
          Activation result = new Activation();
          result.setOs(os);
          result.setProperty(property);
          return result;
        }
      }
    }
    throw new InvalidPomLockFile("Ended prematurely");
  }

  private static ProfileEntry fromProfile(XMLEventReader2 reader, boolean requireScope)
      throws XMLStreamException {
    List<Dependency> dependencies = null;
    String id = null;
    Activation activation = null;

    while (reader.hasNextEvent()) {
      XMLEvent event = reader.nextEvent();
      if (event.isStartElement()) {
        QName name = event.asStartElement().getName();
        if (name.equals(DEPENDENCIES)) {
          if (null != dependencies) {
            throw new InvalidPomLockFile(
                "Unexpected 'dependencies' element found", event.getLocation());
          }
          dependencies = fromDependencies(reader, requireScope);
        } else if (name.equals(ACTIVATION)) {
          if (null != activation) {
            throw new InvalidPomLockFile(
                "Unexpected 'activation' element found", event.getLocation());
          }
          activation = fromActivation(reader);
        } else if (name.equals(ID)) {
          if (id != null) {
            throw new InvalidPomLockFile("Unexpected profile id tag found.", event.getLocation());
          }
          id = readSingleTextElement(reader);
        } else {
          skipElement(reader);
        }
      } else if (event.isEndElement()) {
        if (event.asEndElement().getName().equals(PROFILE)) {
          if (null == activation) {
            throw new InvalidPomLockFile("Missing activation element", event.getLocation());
          }
          if (null == id) {
            throw new InvalidPomLockFile("Missing id element", event.getLocation());
          }
          if (null == dependencies) {
            throw new InvalidPomLockFile("Missing dependencies", event.getLocation());
          }
          Profile profile = new Profile();
          profile.setId(id);
          profile.setActivation(activation);
          return new ProfileEntry(profile, Dependencies.fromDependencies(dependencies));
        }
      }
    }
    throw new InvalidPomLockFile("Ended prematurely");
  }

  private static List<Dependency> fromDependencies(XMLEventReader2 rdr, boolean requireScope)
      throws XMLStreamException {
    List<Dependency> result = new ArrayList<>();
    while (rdr.hasNextEvent()) {
      XMLEvent evt = rdr.nextEvent();
      if (evt.isStartElement()) {
        QName name = evt.asStartElement().getName();
        if (!name.equals(DEPENDENCY)) {
          skipElement(rdr);
        } else {
          result.add(fromDependency(rdr, requireScope));
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

  private static Dependency fromDependency(XMLEventReader2 rdr, boolean requireScope)
      throws XMLStreamException {
    String groupId = null;
    String artifactId = null;
    VersionConstraint version = null;
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
          version = readVersionConstraint(rdr);
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
        validateArtifact(groupId, artifactId, version, event);
        if (type == null) {
          throw new InvalidPomLockFile("Missing type", event.getLocation());
        }
        if (scope == null) {
          if (requireScope) {
            throw new InvalidPomLockFile("Missing scope", event.getLocation());
          } else {
            scope = "";
          }
        }
        if (optional == null) {
          if (requireScope) {
            throw new InvalidPomLockFile("Missing optional", event.getLocation());
          } else {
            optional = false;
          }
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
    VersionConstraint version = null;
    String type = null;
    String classifier = null;
    String integrity = null;
    while (rdr.hasNextEvent()) {
      XMLEvent event = rdr.nextEvent();
      if (event.isStartElement()) {
        QName name = event.asStartElement().getName();
        if (name.equals(GROUP_ID)) {
          groupId = readSingleTextElement(rdr);
        } else if (name.equals(ARTIFACT_ID)) {
          artifactId = readSingleTextElement(rdr);
        } else if (name.equals(VERSION)) {
          version = readVersionConstraint(rdr);
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
        validateArtifact(groupId, artifactId, version, event);
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
    VersionConstraint version = null;
    String integrity = null;
    while (rdr.hasNextEvent()) {
      XMLEvent event = rdr.nextEvent();
      if (event.isStartElement()) {
        QName name = event.asStartElement().getName();
        if (name.equals(GROUP_ID)) {
          groupId = readSingleTextElement(rdr);
        } else if (name.equals(ARTIFACT_ID)) {
          artifactId = readSingleTextElement(rdr);
        } else if (name.equals(VERSION)) {
          version = readVersionConstraint(rdr);
        } else if (name.equals(INTEGRITY)) {
          integrity = readSingleTextElement(rdr);
        } else {
          skipElement(rdr);
        }
      } else if (event.isEndElement()) {
        if (!event.asEndElement().getName().equals(EXTENSION)) {
          throw new InvalidPomLockFile("Expected '</extension>'", event.getLocation());
        }
        validateArtifactWithIntegrity(groupId, artifactId, version, integrity, event);
        return Extension.builder()
            .artifactIdentifier(
                ArtifactIdentifier.builder()
                    .groupId(groupId)
                    .artifactId(artifactId)
                    .type("maven-plugin")
                    .build())
            .version(version)
            .integrity(integrity)
            .build();
      }
    }
    throw new InvalidPomLockFile("Ended prematurely");
  }

  private static void validateArtifactWithIntegrity(
      String groupId,
      String artifactId,
      VersionConstraint version,
      String integrity,
      XMLEvent event) {
    validateArtifact(groupId, artifactId, version, event);
    if (integrity == null) {
      throw new InvalidPomLockFile("Missing integrity", event.getLocation());
    }
  }

  private static void validateArtifact(
      String groupId, String artifactId, VersionConstraint version, XMLEvent event) {
    if (groupId == null) {
      throw new InvalidPomLockFile("Missing groupId", event.getLocation());
    }
    if (artifactId == null) {
      throw new InvalidPomLockFile("Missing artifactId", event.getLocation());
    }
    if (version == null) {
      throw new InvalidPomLockFile("Missing version", event.getLocation());
    }
  }

  private static Plugin fromPlugin(XMLEventReader2 rdr) throws XMLStreamException {
    String groupId = null;
    String artifactId = null;
    VersionConstraint version = null;
    String integrity = null;
    List<Artifact> artifacts = emptyList();
    while (rdr.hasNextEvent()) {
      XMLEvent event = rdr.nextEvent();
      if (event.isStartElement()) {
        QName name = event.asStartElement().getName();
        if (name.equals(GROUP_ID)) {
          groupId = readSingleTextElement(rdr);
        } else if (name.equals(ARTIFACT_ID)) {
          artifactId = readSingleTextElement(rdr);
        } else if (name.equals(VERSION)) {
          version = readVersionConstraint(rdr);
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
        validateArtifactWithIntegrity(groupId, artifactId, version, integrity, event);
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

  private static VersionConstraint readVersionConstraint(XMLEventReader2 reader)
      throws XMLStreamException {
    String versionConstraint = readSingleTextElement(reader);
    if ("${project.version}".equals(versionConstraint)) {
      return VersionConstraints.useProjectVersion();
    } else if ("ignored".equals(versionConstraint)) {
      return VersionConstraints.ignoreVersion();
    } else {
      return VersionConstraints.version(versionConstraint);
    }
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
