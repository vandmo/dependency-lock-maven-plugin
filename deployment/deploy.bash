set -euxo pipefail

mvn versions:set \
  --batch-mode \
  --define newVersion=${TRAVIS_TAG}

bash deployment/decrypt.bash
gpg2 --keyring=${TRAVIS_BUILD_DIR}/pubring.gpg --no-default-keyring --import deployment/signingkey.asc
gpg2 --allow-secret-key-import --keyring=${TRAVIS_BUILD_DIR}/secring.gpg --no-default-keyring --import deployment/signingkey.asc || true

mvn clean deploy \
  --batch-mode \
  --settings deployment/settings.xml \
  --activate-profiles gpg.sign
  --define gpg.executable=gpg2 \
  --define gpg.keyname=9368046AC6F2656D2D9FD382EDC4AF47B8A88C15 \
  --define gpg.passphrase=${PASSPHRASE} \
  --define gpg.publicKeyring=${TRAVIS_BUILD_DIR}/pubring.gpg \
  --define gpg.secretKeyring=${TRAVIS_BUILD_DIR}/secring.gpg \
  --define maven.skip.install=true \
  --define maven.test.skip=true
