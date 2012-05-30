package ee.sk.tempelPlus.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.log4j.Logger;

import sun.security.util.ObjectIdentifier;
import sun.security.x509.CertificatePoliciesExtension;
import sun.security.x509.PolicyInformation;
import sun.security.x509.X509CertImpl;
import ee.sk.digidoc.Signature;
import ee.sk.tempelPlus.TempelPlus;
import ee.sk.utils.ConfigManager;

public class Util {

   public static Logger log = Logger.getLogger(Util.class);

   public static void checkCertificate(X509Certificate certk) {
	      try {
	         X509CertImpl cert = (X509CertImpl) certk;
	         CertificatePoliciesExtension c = (CertificatePoliciesExtension) cert.getExtension(new ObjectIdentifier(
	               "2.5.29.32"));
	         Object o = c.get("policies");
	         if (o instanceof ArrayList) {
	            ArrayList a = (ArrayList) o;
	            for (Object x : a) {
	               if (x instanceof PolicyInformation) {
	                  PolicyInformation p = (PolicyInformation) x;
	                  //Fix @ 10.04.2012 - Test Corporate certificates are allowed too.(Identifier starting with "1.3.6.1.4.1.10015.3.7") 
	                  if (p.getPolicyIdentifier().getIdentifier().toString().startsWith("1.3.6.1.4.1.10015.7")){
	                	  log.info("Executing operation with Corporate certificate");
	                	  return;  
	                  }else if (p.getPolicyIdentifier().getIdentifier().toString().startsWith("1.3.6.1.4.1.10015.3.7")){
	                	  log.info("Executing operation with TEST Corporate certificate");
	                	  return;
	                  }
	               }
	            }
	         }
	         log.error("Operation is allowed only with Corporate certificates (DigiTempel)");
	         TempelPlus.exit(1);
	      } catch (Exception e) {
	         log.error("Error checking policy!", e);
	      }
	   }


   public static void initOSCPSerial(X509Certificate cert5) throws NoSuchAlgorithmException, CertificateException,
         IOException, InstantiationException, IllegalAccessException, ClassNotFoundException, KeyStoreException,
         NoSuchProviderException {
      String sigFlag = ConfigManager.instance().getProperty("SIGN_OCSP_REQUESTS");
      if (sigFlag == null || !sigFlag.equals("true"))
         return;
      Provider prv = (Provider) Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider").newInstance();
      Security.addProvider(prv);
      FileInputStream fi = new FileInputStream(Config.getProp("DIGIDOC_PKCS12_CONTAINER"));
      KeyStore store = KeyStore.getInstance("PKCS12", "BC");
      store.load(fi, Config.getProp("DIGIDOC_PKCS12_PASSWD").toCharArray());
      java.util.Enumeration en = store.aliases();
      // find the key alias
      String pName = null;
      while (en.hasMoreElements()) {
         String n = (String) en.nextElement();
         if (store.isKeyEntry(n)) {
            pName = n;
         }
      }
      java.security.cert.Certificate[] certs = store.getCertificateChain(pName);
      ArrayList<BigInteger> serials = new ArrayList<BigInteger>();
      for (int i = 0; (certs != null) && (i < certs.length); i++) {
         java.security.cert.X509Certificate cert = (java.security.cert.X509Certificate) certs[i];
         if (cert.getSerialNumber() != null && cert.getSerialNumber().compareTo(BigInteger.ZERO) > 0) {
            checkSerial(cert);
            serials.add(cert.getSerialNumber());
         }
      }
      if (serials.size() > 1) {
         log.warn("More than one serial found for OCSP certificate! Trying:" + serials.get(0));
      }
      Properties props = Config.getProps();
      props.put(Config.OCSPSCS, serials.get(0).toString());
      ConfigManager.init(props);
   }

   /**
    *LigipÃ¤Ã¤s SK OCSP teenusele on vaid IP-pÃµhise ligipÃ¤Ã¤su alusel vÃµi
    * asutusele vÃ¤ljastatud ligipÃ¤Ã¤sutÃµendiga, ligipÃ¤Ã¤su ei ole personaalse
    * juurdepÃ¤Ã¤sutÃµendiga. Selleks peab kÃ¤ivitamisel kontrollima
    * juuredepÃ¤Ã¤sutÃµendi tÃ¼Ã¼pi! Isikliku juurdepÃ¤Ã¤sutÃµendi tunneb Ã¤ra selle
    * jÃ¤rgi, et selles on sertifikaadi Subjecti (DN-i) vÃ¤li SERIALNUMBER
    * pikkusega 11 numbrit. Kui vÃ¤li ei ole 11 numbrit vÃµib eeldada et tegu on
    * asutusele vÃ¤ljastatud juurdepÃ¤Ã¤sutÃµendiga (Urmo: kommentaar â€“ jÃµle loll
    * tuvastus, no aga praegu ma midagi paremat vÃ¤lja ei mÃµelnud ï�Š)
    *
    * @param cert5
    */
   private static void checkSerial(X509Certificate cert5) {
      String rawSerial = cert5.getSubjectX500Principal().toString();
      log.debug("Checking DN:" + rawSerial);
      if (rawSerial.contains("SERIALNUMBER"))
         rawSerial = rawSerial.substring(rawSerial.indexOf("SERIALNUMBER=") + 13);
      else {
         return;
      }
      if (rawSerial.contains(","))
         rawSerial = rawSerial.substring(0, rawSerial.indexOf(","));
      rawSerial.trim();
      if (rawSerial.length() == 11) {
         log.error("Can't use OCSP with personal certificate!");
         TempelPlus.exit(1);
      }
      return;
   }

   public static String getCNField(Signature s) {;
      return getCNField(s.getCertValueOfType(1).getCert());
   }

   public static String getCNField(X509Certificate cert) {
      String name = cert.getSubjectDN().getName();
      name = name.substring(name.indexOf("CN=") + 3);
      if (name.startsWith("\"")) {
         name = name.substring(1);
         name = name.substring(0, name.indexOf("\""));
      } else {
         if (name.indexOf(",") != -1)
            name = name.substring(0, name.indexOf(","));
      }
      return name;
   }

}
