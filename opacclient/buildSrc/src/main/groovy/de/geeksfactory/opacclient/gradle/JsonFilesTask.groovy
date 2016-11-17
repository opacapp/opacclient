package de.geeksfactory.opacclient.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.util.GFileUtils
import org.json.JSONArray
import org.json.JSONObject

import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import java.security.KeyStore

class JsonFilesTask extends DefaultTask {
    private static final String API_URL = "https://info.opacapp.net/androidconfigs/?format=json"

    @TaskAction
    def downloadFiles() {
        String bibsDir = "${project.projectDir}/../opacapp/src/main/assets/bibs"
        String assetsDir = "${project.projectDir}/../opacapp/src/main/assets"
    
        HttpsURLConnection conn = (HttpsURLConnection) API_URL.toURL().openConnection()
        conn.setSSLSocketFactory(createSSLSocketFactory())
        String response = conn.inputStream.getText("UTF-8")
        JSONArray data = new JSONArray(response)
        GFileUtils.mkdirs(new File(bibsDir))
        GFileUtils.cleanDirectory(new File(bibsDir))
        for (int i = 0; i < data.length(); i++) {
            JSONObject library = data.get(i);
            String id = library.getString("_id")
            library.remove("_id")
            File file = new File(bibsDir, id + ".json")
            file.write(library.toString(), "UTF-8")
        }

        String lastUpdate = conn.headerFields.get("X-Page-Generated").get(0)
        File file = new File(assetsDir, "last_library_config_update.txt")
        file.write(lastUpdate, "UTF-8")
    }

    private SSLSocketFactory createSSLSocketFactory() {
        // Let's Encrypt certificates are not (yet?) trusted by Java JDK :(
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType())
        keystore.load(getClass().getClassLoader().getResourceAsStream("letsencrypt_x3.jks"),
                "FVVMHEdautxVcb9h4WdeZdMmvUwUHzgh".toCharArray())
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509")
        tmf.init(keystore)
        SSLContext context = SSLContext.getInstance("TLS")
        context.init(null, tmf.getTrustManagers(), null)
        SSLSocketFactory factory = context.getSocketFactory()
        return factory
    }
}