package se.vandmo.dependencylock.maven;

import java.util.HashMap;
import java.util.Map;
import org.apache.maven.model.Activation;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.shared.utils.Os;
import se.vandmo.dependencylock.maven.mojos.model.ActivationOS;

/** */
public final class ProfileUtils {
  private ProfileUtils() {
    super();
  }

  public static Map<String, String> emulateSystemProperties(ActivationOS os)
      throws MojoExecutionException {
    Map<String, String> emulatedValues = new HashMap<>();
    final String arch = os.getArch();
    if (arch != null) {
      if (arch.startsWith("!")) {
        emulatedValues.put("os.arch", "not-" + arch.substring(1));
      } else {
        emulatedValues.put("os.arch", arch);
      }
    }
    final String targetOsFamily = os.getFamily();
    if (targetOsFamily != null) {
      if (targetOsFamily.startsWith("!")) {
        throw new MojoExecutionException("Negated os family activation clauses are not supported");
      }
      switch (targetOsFamily) {
        case "windows":
          emulatedValues.put("os.name", "windows");
          break;
        case "win9x":
          emulatedValues.put("os.name", "windows-95");
          break;
        case "winnt":
          emulatedValues.put("os.name", "windows-nt");
          break;
        case "os/2":
          emulatedValues.put("os.name", "os/2");
          break;
        case "netware":
          emulatedValues.put("os.name", "netware");
          break;
        case "dos":
          if (Os.PATH_SEP.equals(";")) {
            emulatedValues.put("os.name", "dos");
            break;
          }
          throw new MojoExecutionException(
              "dos family emulation is not supported on non windows platforms");
        case "mac":
          emulatedValues.put("os.name", "darwin");
          break;
        case "tandem":
          emulatedValues.put("os.name", "tandem-nonstop_kernel");
          break;
        case "unix":
          if (Os.PATH_SEP.equals(":")) {
            emulatedValues.put("os.name", "unix");
            break;
          }
          throw new MojoExecutionException(
              "unix family emulation is not supported on non unix platforms");
        case "z/os":
          emulatedValues.put("os.name", "z/os");
          break;
        case "os/400":
          emulatedValues.put("os.name", "os/400");
          break;
        case "openvms":
          emulatedValues.put("os.name", "openvms");
          break;
        default:
          throw new MojoExecutionException("Unsupported os family: " + targetOsFamily);
      }
    }
    String targetOsName = os.getName();
    if (targetOsName != null) {
      final String alreadyEmulatedOsName = emulatedValues.get("os.name");
      if (targetOsName.startsWith("!")) {
        throw new MojoExecutionException("Negated os name activation clauses are not supported");
      }
      if (alreadyEmulatedOsName == null) {
        emulatedValues.put("os.name", "not-" + targetOsName);
      } else if (!targetOsName.equals(alreadyEmulatedOsName)) {
        throw new MojoExecutionException(
            "Found conflicting os name clause ("
                + targetOsName
                + ") which is not compatible with existing family ("
                + alreadyEmulatedOsName
                + ")");
      }
    }
    return emulatedValues;
  }

  public static Activation toMavenActivation(
      se.vandmo.dependencylock.maven.mojos.model.Activation src) {
    final org.apache.maven.model.Activation emulatedActivation =
        new org.apache.maven.model.Activation();
    final ActivationOS srcOs = src.getOs();
    if (null != srcOs) {
      final org.apache.maven.model.ActivationOS emulatedActivationOS =
          new org.apache.maven.model.ActivationOS();
      emulatedActivationOS.setArch(srcOs.getArch());
      emulatedActivationOS.setName(srcOs.getName());
      emulatedActivationOS.setFamily(srcOs.getFamily());
      emulatedActivationOS.setVersion(srcOs.getVersion());
      emulatedActivation.setOs(emulatedActivationOS);
    }
    return emulatedActivation;
  }
}
