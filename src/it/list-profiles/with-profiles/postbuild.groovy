import org.apache.commons.io.FileUtils

import static java.nio.charset.StandardCharsets.UTF_8
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.containsInRelativeOrder

buildLog = FileUtils.readLines(new File(basedir, "build.log"), UTF_8)
assertThat(buildLog, containsInRelativeOrder(
        "[INFO] 2 profiles found:",
        "[INFO] <profile>",
        "[INFO]   <id>mac</id>",
        "[INFO]   <activation>",
        "[INFO]     <os>",
        "[INFO]       <family>mac</family>",
        "[INFO]     </os>",
        "[INFO]   </activation>",
        "[INFO] </profile>",
        "[INFO] <profile>",
        "[INFO]   <id>windows</id>",
        "[INFO]   <activation>",
        "[INFO]     <os>",
        "[INFO]       <family>windows</family>",
        "[INFO]     </os>",
        "[INFO]   </activation>",
        "[INFO] </profile>"))