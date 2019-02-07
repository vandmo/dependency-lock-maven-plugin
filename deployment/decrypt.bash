set -euxo pipefail

openssl aes-256-cbc -K $encrypted_9ef43f452f42_key -iv $encrypted_9ef43f452f42_iv -in deployment/signingkey.asc.enc -out deployment/signingkey.asc -d
