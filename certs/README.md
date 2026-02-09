# SSL Certificate Setup

This directory contains SSL certificates for the SCIM server.

## Files

- `keystore.pfx` - PKCS12 keystore containing the SSL certificate and private key
- `keystore.pin` - Plain text file containing the keystore password

## Generating a Self-Signed Certificate for Testing

To generate a self-signed certificate for testing purposes, run:

```bash
keytool -genkeypair -alias scim-server \
  -keyalg RSA -keysize 2048 \
  -storetype PKCS12 \
  -keystore certs/keystore.pfx \
  -validity 3650 \
  -storepass $(cat certs/keystore.pin) \
  -dname "CN=localhost, OU=SCIM, O=Organization, L=City, ST=State, C=US"
```

Or using OpenSSL:

```bash
# Generate private key and certificate
openssl req -x509 -newkey rsa:2048 -keyout certs/key.pem -out certs/cert.pem -days 3650 -nodes \
  -subj "/C=US/ST=State/L=City/O=Organization/OU=SCIM/CN=localhost"

# Convert to PKCS12 format
openssl pkcs12 -export -in certs/cert.pem -inkey certs/key.pem \
  -out certs/keystore.pfx -name scim-server \
  -password pass:$(cat certs/keystore.pin)
```

## Using Your Own Certificate

1. Place your PKCS12 keystore file as `certs/keystore.pfx`
2. Update the password in `certs/keystore.pin`
3. Restart the server

## Security Note

⚠️ **Important**: The `keystore.pin` file should NOT be committed to version control in production. 
Add it to `.gitignore` and manage it securely through your deployment process.
