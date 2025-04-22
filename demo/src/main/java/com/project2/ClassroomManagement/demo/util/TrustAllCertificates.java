package com.project2.ClassroomManagement.demo.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

public class TrustAllCertificates {
    
    private static final Logger logger = LoggerFactory.getLogger(TrustAllCertificates.class);
    
    private static final TrustManager[] TRUST_ALL_CERTIFICATES = new TrustManager[] {
        new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
            
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
                // Trust all client certificates
            }
            
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
                // Trust all server certificates
            }
        }
    };
    
    private static final HostnameVerifier TRUST_ALL_HOSTNAMES = (hostname, session) -> true;
    
    public static void install() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, TRUST_ALL_CERTIFICATES, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(TRUST_ALL_HOSTNAMES);
            
            // Also set system properties for JavaMail
            System.setProperty("mail.smtp.ssl.trust", "*");
            System.setProperty("mail.smtp.ssl.checkserveridentity", "false");
            
            logger.info("Installed trust for all certificates");
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            logger.error("Failed to install trust for all certificates", e);
        }
    }
}
