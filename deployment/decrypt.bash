set -euxo pipefail

echo "${PASSPHRASE}" | gpg --passphrase-fd 0 deployment/signingkey.asc.gpg
