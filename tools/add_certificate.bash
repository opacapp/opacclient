#!/bin/bash

if [ -z $1 ]; then
  echo "Usage: add_certificate.sh <SERVER>"
  exit 1
fi

HOST=$1
CERT=/tmp/cert.pem
echo -n | openssl s_client -servername $HOST -connect $HOST:443 | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > $CERT

BCJAR=tools/bcprov-jdk15on-146.jar

TRUSTSTORE=opacclient/libopac/src/main/resources/ssl_trust_store.bks
STOREPASS=ro5eivoijeeGohsh0daequoo5Zeepaen

ALIAS=$HOST"-"`openssl x509 -inform PEM -subject_hash -noout -in $CERT`

echo "Adding certificate to $TRUSTSTORE..."
keytool -import -v -trustcacerts -alias $ALIAS \
      -file $CERT \
      -keystore $TRUSTSTORE -storetype BKS \
      -providerclass org.bouncycastle.jce.provider.BouncyCastleProvider \
      -providerpath $BCJAR \
      -storepass $STOREPASS
