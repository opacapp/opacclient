#!/bin/bash
BCJAR=tools/bcprov-jdk15on-146.jar
TRUSTSTORE=opacclient/opacapp/src/main/res/raw/ssl_trust_store.bks
STOREPASS=ro5eivoijeeGohsh0daequoo5Zeepaen

DOMAINS=$(keytool -list \
      -keystore $TRUSTSTORE -storetype BKS \
      -providerclass org.bouncycastle.jce.provider.BouncyCastleProvider \
      -providerpath $BCJAR \
	  -storepass $STOREPASS | grep trustedCertEntr  | cut -d',' -f1 | rev | cut -d'-' -f2- | rev | grep "\." | tr '\n' ' ')

CERT=/tmp/cert.pem

for HOST in $DOMAINS; do
	echo -n | openssl s_client -servername $HOST -connect $HOST:443 | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > $CERT
	ALIAS=$HOST"-"`openssl x509 -inform PEM -subject_hash -noout -in $CERT`

	echo "Adding certificate to $TRUSTSTORE..."
	keytool -import -v -trustcacerts -alias $ALIAS \
		  -file $CERT \
		  -keystore $TRUSTSTORE -storetype BKS \
		  -providerclass org.bouncycastle.jce.provider.BouncyCastleProvider \
		  -providerpath $BCJAR \
		  -storepass $STOREPASS;
done
