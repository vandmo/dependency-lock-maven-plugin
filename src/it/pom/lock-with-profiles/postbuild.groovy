import static java.nio.charset.StandardCharsets.UTF_8
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.core.CombinableMatcher.both
import static org.hamcrest.core.IsIterableContaining.hasItem
import static org.hamcrest.core.StringEndsWith.endsWith
import static org.hamcrest.core.StringStartsWith.startsWith
import static org.junit.Assert.assertTrue

import org.apache.commons.io.FileUtils

lockFile = new File(basedir, ".dependency-lock/pom.xml")
expectedLockFile = new File(basedir, "expected/pom.xml")

assertTrue(lockFile.isFile())
assertTrue(FileUtils.contentEquals(expectedLockFile, lockFile))

buildLog = FileUtils.readLines(new File(basedir, "build.log"), UTF_8)
assertThat(buildLog, hasItem(both(startsWith("[INFO] Creating ")).and(endsWith("/dependency-lock-maven-plugin/target/its/pom/lock-with-profiles/.dependency-lock/pom.xml"))))
