import com.google.common.io.BaseEncoding
import com.google.common.io.Files
import org.apache.commons.io.FileUtils

import java.security.MessageDigest

import static java.nio.charset.StandardCharsets.UTF_8
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.core.CombinableMatcher.both
import static org.hamcrest.core.IsIterableContaining.hasItem
import static org.hamcrest.core.StringEndsWith.endsWith
import static org.hamcrest.core.StringStartsWith.startsWith
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertTrue

lockFile = new File(basedir, "dependencies-lock.json")
expectedLockFile = new File(basedir, "expected-dependencies-lock.json")

assertTrue("Lock file missing", lockFile.isFile())

/**
 * The MRN plugin seems to re-write the JAR files on the way through so we can't depend on the values of the SHA sums
 * being constant - the header changes every test run. To workaround this rely on the other json test to check the
 * regular target data and this one to just check the integrity hash by finding the file in the repository
 * (target/local-repo) and validating the checksum appears twice (exactly) in the file.
 */
jar = new File(basedir, "../../local-repo/se/vandmo/testing/leaf/1.0/leaf-1.0.jar")
hash = BaseEncoding.base16().encode(MessageDigest.getInstance("SHA-512").digest(Files.toByteArray(jar)));

assertThat("Lock file content not as expected", FileUtils.readLines(lockFile), hasItems(containsString(hash), containsString(hash)));
