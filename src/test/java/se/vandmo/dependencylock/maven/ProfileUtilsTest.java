package se.vandmo.dependencylock.maven;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import se.vandmo.dependencylock.maven.mojos.model.IActivationOS;

public class ProfileUtilsTest {

  @Test
  public void emulatedProperties_IActivationOS_appropriatelySupportsOsName()
      throws MojoExecutionException {
    IActivationOS mockActivationOS = Mockito.mock(IActivationOS.class);
    Mockito.doReturn("linux").when(mockActivationOS).getName();
    Mockito.doReturn("aarch-x64").when(mockActivationOS).getArch();

    Map<String, String> expected = new HashMap<>();
    expected.put("os.name", "linux");
    expected.put("os.arch", "aarch-x64");

    Map<String, String> emulatedProperties = ProfileUtils.emulateSystemProperties(mockActivationOS);

    assertEquals("Unexpected emulated properties generated", expected, emulatedProperties);
  }

  @Test
  public void emulatedProperties_IActivationOS_appropriatelyFailsOnConflict() {
    IActivationOS mockActivationOS = Mockito.mock(IActivationOS.class);
    Mockito.doReturn("linux").when(mockActivationOS).getName();
    Mockito.doReturn("aarch-x64").when(mockActivationOS).getArch();
    Mockito.doReturn("mac").when(mockActivationOS).getFamily();

    Throwable caughtException = null;
    try {
      ProfileUtils.emulateSystemProperties(mockActivationOS);
    } catch (MojoExecutionException e) {
      caughtException = e;
    }
    Assert.assertNotNull(
        "An error should have been raised since os family was conflicting with os name.",
        caughtException);
    Assert.assertEquals(
        "Unexpected exception message",
        "Found conflicting os name clause (linux) which is not compatible with existing family"
            + " (darwin)",
        caughtException.getMessage());
  }
}
