import re
import sys
import os
import subprocess
import ssl
import shlex

if len(sys.argv) == 1:
    print("Usage: add_certificate.py <SERVER>")
    sys.exit(1)

host = sys.argv[1]
cert_file = "cert.pem"
cert = ssl.get_server_certificate((host, 443))
f = open(cert_file, 'w')
f.write(cert)
f.close()

bcjar = "tools/bcprov-jdk15on-146.jar"

truststore = "opacclient/libopac/src/main/resources/ssl_trust_store.bks"
storepass = "ro5eivoijeeGohsh0daequoo5Zeepaen"

alias = host + "-" + subprocess.check_output("openssl x509 -inform PEM -subject_hash -noout -in " + cert_file, shell=True).decode('utf-8')

print("Adding certificate to " + truststore + "...")

subprocess.call("keytool -import -v -trustcacerts -alias " + alias +
            " -file " + cert_file +
            " -keystore " + truststore + " -storetype BKS" +
            " -providerclass org.bouncycastle.jce.provider.BouncyCastleProvider" +
            " -providerpath " + bcjar +
            " -storepass " + storepass, shell=True)
            
os.remove(cert_file)
