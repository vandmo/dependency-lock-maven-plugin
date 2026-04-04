package se.vandmo.dependencylock.maven;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.artifact.filter.StrictPatternIncludesArtifactFilter;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Unit test class dedicated to the validation of the {@link LockedParents} class.
 */
public class LockedParentsTest {

    @Test
    public void validate_checksumIgnoringAndVersionIgnoringAreSupported() {
        final Parents expectedParents = new Parents(Collections.singletonList(
                Parent.builder()
                        .artifactIdentifier(
                                ArtifactIdentifier.builder()
                                        .groupId("com.github.some-groupId")
                                        .artifactId("whatever-artifactId")
                                        .type("pom")
                                        .build()
                        ).version("1.2.0-SNAPSHOT")
                        .integrity("sha512:h5VNQl9OruqXka54GCXYJ41fXj/PTarH4nf3YJYMTJjIrupolMoVQtVuAWeOUMHDvIiWVY91x1Gdw4+pJWhrlw==")
                        .build()
        ));
        Parents actualParents = new Parents(Collections.singletonList(
                Parent.builder()
                        .artifactIdentifier(
                                ArtifactIdentifier.builder()
                                        .groupId("com.github.some-groupId")
                                        .artifactId("whatever-artifactId")
                                        .type("pom")
                                        .build()
                        ).version("1.2.0")
                        .integrity("sha512:ypo0tF7QE7splJrj5g+xlA07xOeEGafQaNPgWTicnItmvcFIkdGvDEfl9Ipr0n9XvASXDDAFby6jiSmSEaEuKg==")
                        .build()
        ));
        DiffReport result = LockedParents.from(expectedParents, Mockito.mock(Log.class)).compareWith(actualParents, new Filters(
                Collections.singletonList(
                        new DependencySetConfiguration(
                                new StrictPatternIncludesArtifactFilter(
                                        Collections.singletonList("com.github.some-groupId")
                                ),
                                new StrictPatternIncludesArtifactFilter(Collections.emptyList()),
                                DependencySetConfiguration.Version.useProjectVersion,
                                DependencySetConfiguration.Integrity.ignore,
                                null,
                                null
                        )
                ),
                "1.2.0"
        ));
        Assert.assertEquals(
                Collections.emptyList(),
                result.report("parents").collect(Collectors.toList())
        );
    }
}