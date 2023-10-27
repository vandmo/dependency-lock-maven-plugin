package io.mvnpm.maven.locker;

import static java.lang.String.format;
import static java.util.Locale.ROOT;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import io.mvnpm.maven.locker.DependenciesLockFileAccessor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class DependenciesLockFileAccessorTests {

  private static Random random = new Random();

  @Rule public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void parentFoldersShouldBeCreated() throws IOException {
    File basedir = folder.newFolder();
    String folderName1 = randomEnoughString();
    String folderName2 = randomEnoughString();
    String filename = randomEnoughString();
    File lockFile = new File(new File(new File(basedir, folderName1), folderName2), filename);
    assertFalse(lockFile.isFile());
    DependenciesLockFileAccessor.fromBasedir(
            basedir, format(ROOT, "%s/%s/%s", folderName1, folderName2, filename))
        .writer()
        .close();
    assertTrue(lockFile.isFile());
  }

  private static String randomEnoughString() {
    long l1 = random.nextInt(Integer.MAX_VALUE);
    long l2 = random.nextInt(Integer.MAX_VALUE);
    return (Long.toString(l1, 36) + Long.toString(l2, 36)).toUpperCase(ROOT);
  }
}
