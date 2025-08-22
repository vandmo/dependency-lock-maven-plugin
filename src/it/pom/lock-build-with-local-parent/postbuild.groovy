import static java.nio.charset.StandardCharsets.UTF_8
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.core.CombinableMatcher.both
import static org.hamcrest.core.IsIterableContaining.hasItem
import static org.hamcrest.core.StringEndsWith.endsWith
import static org.hamcrest.core.StringStartsWith.startsWith
import static org.junit.Assert.assertTrue

import org.apache.commons.io.FileUtils

def assertLockFile(lockFilename, expectedFilename) {
    lockFile = new File(basedir, lockFilename)
    expectedLockFile = new File(basedir, expectedFilename)
    assertTrue(lockFile.isFile())
    assertTrue(expectedLockFile.isFile())
    assertTrue(FileUtils.contentEquals(expectedLockFile, lockFile))
}

assertLockFile(".dependency-lock/pom.xml", "expected/pom.xml")
assertLockFile(".dependency-lock/parents/pom.xml", "expected/parents/pom.xml")
assertLockFile(".dependency-lock/plugins/pom.xml", "expected/plugins/pom.xml")
assertLockFile(".dependency-lock/extensions/pom.xml", "expected/extensions/pom.xml")

buildLog = FileUtils.readLines(new File(basedir, "build.log"), UTF_8)
assertThat(buildLog, hasItem(both(startsWith("[INFO] Creating ")).and(endsWith("/dependency-lock-maven-plugin/target/its/pom/lock-build-with-local-parent/.dependency-lock/pom.xml"))))
