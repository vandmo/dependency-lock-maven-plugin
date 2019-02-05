set -euxo pipefail

openssl aes-256-cbc \
  -K ${encrypted_408b1194d341_key} \
  -iv ${encrypted_408b1194d341_iv} \
  -in deployment/signingkey.asc.enc \
  -out deployment/signingkey.asc -d
