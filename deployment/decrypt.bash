set -euxo pipefail

openssl aes-256-cbc \
  -k "${PASSPHRASE}" \
  -in deployment/signingkey.asc.enc \
  -out deployment/signingkey.asc -d
