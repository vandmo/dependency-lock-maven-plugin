package io.mvnpm.maven.locker.pom;

import com.ctc.wstx.stax.WstxInputFactory;
import io.mvnpm.maven.locker.Artifact;
import io.mvnpm.maven.locker.ArtifactIdentifier;
import org.codehaus.stax2.XMLEventReader2;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static io.mvnpm.maven.locker.Artifact.DEFAULT_SCOPE;
import static io.mvnpm.maven.locker.ArtifactIdentifier.DEFAULT_TYPE;
import static java.lang.String.format;
import static java.util.Locale.ROOT;
import static javax.xml.stream.XMLInputFactory.IS_COALESCING;
import static javax.xml.stream.XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES;
import static javax.xml.stream.XMLInputFactory.IS_VALIDATING;
import static javax.xml.stream.XMLInputFactory.SUPPORT_DTD;

public final class PomLockFile {

    private static final String POM_NS = "http://maven.apache.org/POM/4.0.0";
    private static final String DEPENDENCY_LOCK_NS = "urn:se.vandmo.dependencylock";
    private static final QName PROJECT = new QName(POM_NS, "project");

    private static final QName DEPENDENCY_MANAGEMENT = new QName(POM_NS, "dependencyManagement");
    private static final QName DEPENDENCIES = new QName(POM_NS, "dependencies");
    private static final QName DEPENDENCY = new QName(POM_NS, "dependency");

    private static final QName PROPERTIES = new QName(POM_NS, "properties");
    private static final QName GROUP_ID = new QName(POM_NS, "groupId");
    private static final QName ARTIFACT_ID = new QName(POM_NS, "artifactId");
    private static final QName VERSION = new QName(POM_NS, "version");
    private static final QName TYPE = new QName(POM_NS, "type");
    private static final QName SCOPE = new QName(POM_NS, "scope");
    private static final QName CLASSIFIER = new QName(POM_NS, "classifier");
    private static final QName OPTIONAL = new QName(POM_NS, "optional");
    private static final QName INTEGRITY = new QName(DEPENDENCY_LOCK_NS, "integrity");
    private static final String LOCK_INTEGRITY_PROP_PREFIX = "lock-integrity-";

    public static List<Artifact> read(File file) {
        try {
            return doRead(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (XMLStreamException e) {
            throw new InvalidPomLockFileException(e);
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
                    throw new InvalidPomLockFileException("Expected 'project'-element", evt.getLocation());
                }
                return fromProject(reader);
            }
        }
        throw new InvalidPomLockFileException("Ended prematurely");
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
        AtomicReference<Map<String, String>> integrityMap = new AtomicReference<>();
        AtomicReference<List<Artifact.IntegrityBuilderStage>> deps = new AtomicReference<>();
        while (reader.hasNextEvent()) {
            XMLEvent event = reader.nextEvent();
            if (event.isStartElement()) {
                QName name = event.asStartElement().getName();
                if (name.equals(DEPENDENCY_MANAGEMENT)) {
                    fromElement(reader, DEPENDENCIES, r1 -> {
                        deps.set(fromDependencies(reader));
                    });
                } else if (name.equals(PROPERTIES)) {
                    integrityMap.set(extractIntegrityMap(reader));
                } else if (integrityMap.get() != null && deps.get() != null) {
                    break;
                } else {
                    skipElement(reader);
                }
            }
        }
        if (deps.get() == null) {
            throw new InvalidPomLockFileException("Missing 'dependencyManagement' element");
        }
        if (integrityMap.get() == null) {
            throw new InvalidPomLockFileException("Missing 'properties' element for integrity");
        }
        return deps.get().stream()
                .map(d -> buildArtifact(d, integrityMap.get()))
                .collect(Collectors.toList());
    }

    private static Artifact buildArtifact(Artifact.IntegrityBuilderStage d, Map<String, String> finalIntegrityMap) {
        if (!finalIntegrityMap.containsKey(d.artifactIdentifier.key())) {
            throw new InvalidPomLockFileException("Missing integrity property for: " + d.artifactIdentifier.key());
        }
        return d.integrity(finalIntegrityMap.get(d.artifactIdentifier.key())).build();
    }

    private static void fromElement(XMLEventReader2 rdr, QName name, Consumer<XMLEventReader2> onElement) {
        try {
            while (rdr.hasNextEvent()) {
                XMLEvent evt = null;
                evt = rdr.nextEvent();
                if (evt.isStartElement()) {
                    QName elName = evt.asStartElement().getName();
                    if (elName.equals(name)) {
                        onElement.accept(rdr);
                        break;
                    } else {
                        skipElement(rdr);
                    }
                }
            }
        } catch (XMLStreamException e) {
            throw new InvalidPomLockFileException(e);
        }
    }

    private static List<Artifact.IntegrityBuilderStage> fromDependencies(XMLEventReader2 rdr)  {
        try {
            List<Artifact.IntegrityBuilderStage> result = new ArrayList<>();
            while (rdr.hasNextEvent()) {
                XMLEvent evt = rdr.nextEvent();
                if (evt.isStartElement()) {
                    QName name = evt.asStartElement().getName();
                    if (name.equals(DEPENDENCY)) {
                        result.add(fromDependency(rdr));
                    } else {
                        skipElement(rdr);
                    }
                } else if (evt.isEndElement()) {
                    if (!evt.asEndElement().getName().equals(DEPENDENCIES)) {
                        throw new InvalidPomLockFileException("Expected '</dependencies>'", evt.getLocation());
                    }
                    break;
                }
            }
            return result;
        } catch (XMLStreamException e) {
            throw new InvalidPomLockFileException(e);
        }
    }


    private static Map<String, String> extractIntegrityMap(XMLEventReader2 reader) throws XMLStreamException {
        Map<String, String> integrityMap = new HashMap<>();
        XMLEvent event;
        while (reader.hasNext()) {
            event = reader.nextEvent();
            if (event.isStartElement() && event.asStartElement().getName().getLocalPart()
                    .startsWith(LOCK_INTEGRITY_PROP_PREFIX)) {
                String name = event.asStartElement().getName().getLocalPart();
                String value = readSingleTextElement(reader);
                final String key = name.substring(LOCK_INTEGRITY_PROP_PREFIX.length());
                integrityMap.put(key, value);
            } else if (event.isEndElement() && event.asEndElement().getName().equals(PROPERTIES)) {
                break; // Finished processing properties
            }
        }
        return integrityMap;
    }

    private static Artifact.IntegrityBuilderStage fromDependency(XMLEventReader2 rdr) {
        String groupId = null;
        String artifactId = null;
        String version = null;
        String type = null;
        String scope = null;
        String classifier = null;
        Boolean optional = null;
        try {
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
                                throw new InvalidPomLockFileException(
                                        format(ROOT, "Invalid optional value '%s' for dependency", optionalStr),
                                        startElementLocation);
                        }
                    } else {
                        skipElement(rdr);
                    }
                } else if (event.isEndElement()) {
                    if (!event.asEndElement().getName().equals(DEPENDENCY)) {
                        throw new InvalidPomLockFileException("Expected '</dependency>'", event.getLocation());
                    }
                    if (groupId == null) {
                        throw new InvalidPomLockFileException("Missing groupId", event.getLocation());
                    }
                    if (artifactId == null) {
                        throw new InvalidPomLockFileException("Missing artifactId", event.getLocation());
                    }
                    if (version == null) {
                        throw new InvalidPomLockFileException("Missing version", event.getLocation());
                    }
                    if (type == null) {
                        type = DEFAULT_TYPE;
                    }
                    if (scope == null) {
                        scope = DEFAULT_SCOPE;
                    }
                    if (optional == null) {
                        optional = false;
                    }
                    //if (integrity == null) {
                    //  throw new InvalidPomLockFile("Missing integrity", event.getLocation());
                    //}
                    return Artifact.builder()
                            .artifactIdentifier(
                                    ArtifactIdentifier.builder()
                                            .groupId(groupId)
                                            .artifactId(artifactId)
                                            .classifier(Optional.ofNullable(classifier))
                                            .type(type)
                                            .build())
                            .version(version)
                            .scope(scope)
                            .optional(optional);
                }
            }
        } catch (XMLStreamException e) {
            throw new InvalidPomLockFileException(e);
        }
        throw new InvalidPomLockFileException("Ended prematurely");
    }

    private static String readSingleTextElement(XMLEventReader2 reader) throws XMLStreamException {
        XMLEvent characterEvent = expectEvent(reader);
        if (!characterEvent.isCharacters()) {
            throw new InvalidPomLockFileException("Expected characters", characterEvent.getLocation());
        }
        String content = characterEvent.asCharacters().getData();
        XMLEvent endElementEvent = expectEvent(reader);
        if (!endElementEvent.isEndElement()) {
            throw new InvalidPomLockFileException("Expected end of text element", endElementEvent.getLocation());
        }
        return content;
    }

    private static XMLEvent expectEvent(XMLEventReader2 reader) throws XMLStreamException {
        if (!reader.hasNextEvent()) {
            throw new InvalidPomLockFileException("Ended prematurely");
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
