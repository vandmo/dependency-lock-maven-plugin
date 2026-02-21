import org.apache.commons.io.FileUtils

import static java.nio.charset.StandardCharsets.UTF_8
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.core.IsIterableContaining.hasItem 

buildLog = FileUtils.readLines(new File(basedir, "build.log"), UTF_8)
assertThat(
        buildLog,
        hasItem(
                "[ERROR] Failed to execute goal se.vandmo:dependency-lock-maven-plugin:0-SNAPSHOT:lock (default-cli) on project json-lock-with-profiles-outdated-maven-version: Maven 3.9.7 or newer is required. -> [Help 1]"
        )
)
