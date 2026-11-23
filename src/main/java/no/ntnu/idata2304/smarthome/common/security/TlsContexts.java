package no.ntnu.idata2304.smarthome.common.security;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Map;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/** Loads the server identity or the certificate that clients use to trust it. */
public final class TlsContexts {
  public static final String PROTOCOL = "TLSv1.3";

  private TlsContexts() {}

  /** Loads the server's private key and certificate from its PKCS#12 keystore. */
  public static SSLContext forServer(Map<String, String> configuration) throws IOException {
    Path path = Path.of(
        configuration.getOrDefault("SMART_HOME_TLS_KEYSTORE", "config/tls/server.p12"));
    String configuredPassword = configuration.get("SMART_HOME_TLS_KEYSTORE_PASSWORD");
    if (configuredPassword == null || configuredPassword.isEmpty()) {
      throw new IOException("Set SMART_HOME_TLS_KEYSTORE_PASSWORD. See docs/running.md.");
    }
    char[] password = configuredPassword.toCharArray();
    try (var file = Files.newInputStream(path)) {
      var keys = KeyStore.getInstance("PKCS12");
      keys.load(file, password);
      var managers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      managers.init(keys, password);
      var context = SSLContext.getInstance(PROTOCOL);
      context.init(managers.getKeyManagers(), null, null);
      return context;
    } catch (IOException | GeneralSecurityException exception) {
      throw new IOException("Cannot load TLS server keystore " + path + ". See docs/running.md.",
          exception);
    } finally {
      Arrays.fill(password, '\0');
    }
  }

  /** Loads the public certificate installed on sensor nodes and control panels. */
  public static SSLContext forClient(Map<String, String> configuration) throws IOException {
    Path path = Path.of(
        configuration.getOrDefault("SMART_HOME_TLS_CERTIFICATE", "config/tls/server.crt"));
    try (var file = Files.newInputStream(path)) {
      var certificates = CertificateFactory.getInstance("X.509");
      var certificate = (X509Certificate) certificates.generateCertificate(file);
      certificate.checkValidity();
      var trusted = KeyStore.getInstance("PKCS12");
      trusted.load(null, null);
      trusted.setCertificateEntry("smart-home-server", certificate);
      var managers = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      managers.init(trusted);
      var context = SSLContext.getInstance(PROTOCOL);
      context.init(null, managers.getTrustManagers(), null);
      return context;
    } catch (IOException | GeneralSecurityException exception) {
      throw new IOException("Cannot load trusted TLS certificate " + path + ". See docs/running.md.",
          exception);
    }
  }
}
