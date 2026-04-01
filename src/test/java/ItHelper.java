import java.io.File;
import java.io.IOException;
import java.util.function.Function;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;

/**
 * Helper class for integration tests.
 *
 * @author Antoine Malliarakis
 */
public final class ItHelper {

  private ItHelper() {}

  public static Function<String, File> referenceFileProvider(
      File rootDirectory, String mavenVersion) {
    File mavenSpecializedDirectory = new File(rootDirectory, mavenVersion);
    Function<String, File> simpleFileProvider =
        simpleFileProvider(new File(rootDirectory, "expected"));
    if (mavenSpecializedDirectory.exists()) {
      System.out.println("Using maven specific reference directory: " + mavenSpecializedDirectory);
      Function<String, File> mavenExpectedFileProvider =
          simpleFileProvider(new File(mavenSpecializedDirectory, "expected"));
      return (fileName) -> {
        File file = mavenExpectedFileProvider.apply(fileName);
        if (file.exists()) {
          return file;
        }
        return simpleFileProvider.apply(fileName);
      };
    }
    return simpleFileProvider;
  }

  private static Function<String, File> simpleFileProvider(File rootDirectory) {
    return (fileName) -> new File(rootDirectory, fileName);
  }

  public static Function<String, File> actualFileProvider(File rootActualDirectory) {
    return simpleFileProvider(rootActualDirectory);
  }

  public static void validateContents(
      String fileName,
      Function<String, File> actualFileProvider,
      Function<String, File> referenceFileProvider)
      throws IOException {
    File actualFile = actualFileProvider.apply(fileName);
    File referenceFile = referenceFileProvider.apply(fileName);
    Assert.assertTrue("Expected " + actualFile + " to exist", actualFile.exists());
    Assert.assertTrue("Expected " + referenceFile + " to exist", referenceFile.exists());
    Assert.assertTrue(
        "Unexpected contents found for "
            + fileName
            + " (actual: "
            + actualFile
            + ", reference: "
            + referenceFile
            + ")",
        FileUtils.contentEquals(actualFile, referenceFile));
  }
}
