import static java.nio.charset.StandardCharsets.UTF_8
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.core.CombinableMatcher.both
import static org.hamcrest.core.IsIterableContaining.hasItem
import static org.hamcrest.core.StringEndsWith.endsWith
import static org.hamcrest.core.StringStartsWith.startsWith
import static org.junit.Assert.assertTrue

import org.apache.commons.io.FileUtils

def expectedFileProvider = ItHelper.referenceFileProvider(basedir, mavenVersion)
def actualFileProvider = ItHelper.actualFileProvider(basedir)

ItHelper.validateContents("dependencies-lock.json", actualFileProvider, expectedFileProvider);

buildLog = FileUtils.readLines(new File(basedir, "build.log"), UTF_8)
assertThat(buildLog, hasItem(both(startsWith("[INFO] Creating ")).and(endsWith("/dependency-lock-maven-plugin/target/its/json/lock-build-with-extension/dependencies-lock.json"))))
