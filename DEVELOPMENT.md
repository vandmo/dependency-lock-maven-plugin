There are a bunch of scripts in the `tasks/` folder that can be useful:
* `tasks/format` formats all code
* `tasks/full-build` run everything that is run in the build pipe
* `tasks/generate-sumtypes` generates some code that shouldn't be edited manually
* `tasks/lock-dependencies` updates the lock file
* `tasks/lock-non-lts-maven-dependencies` updates the lock file for the maven version targeted by the maven wrapper

How to configure the maven wrapper to use maven 3.6.3: `./mvnw --batch-mode --show-version org.apache.maven.plugins:maven-wrapper-plugin:wrapper -Dmaven="3.6.3" --no-transfer-progress`
How to configure the maven wrapper to use maven 4.0.0-rc5: `./mvnw --batch-mode --show-version org.apache.maven.plugins:maven-wrapper-plugin:wrapper -Dmaven="4.0.0-rc-5" --no-transfer-progress`

