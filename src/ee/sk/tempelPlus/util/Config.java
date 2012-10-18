package ee.sk.tempelPlus.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Properties;

import ee.sk.tempelPlus.util.console.TextDevice;
import ee.sk.tempelPlus.util.console.TextDevices;

public class Config {

   private static Properties props;
   public static final String JDOC_LOC = "jddoc_location";
   public static final String FORMAT = "format";
   public static final String CRYPT = "crypt";
   public static final String WORK_DIR = "work_directory";
   public static final String DATE_F = "date_format";
   public static final String PIN_E = "pin_enter";
   public static final String PINC = "console";
   public static final String PING = "graphic";
   public static final String PINCFG = "config";
   public static final String PINFROMCONFIG = "pin";
   public static final String CQUEST = "control_question";
   public static final String LOG_FILE = "log_file";
   public static final String BC_PROV = "bc_prov";
   public static final String CMN_EXT_DIR = "cmn_ext_dir";

   public static final String ROLE = "role";
   public static final String STATE = "state";
   public static final String CITY = "city";
   public static final String POSTCODE = "postcode";
   public static final String SIGNCN = "signerCN";
   public static final String COUNTRY = "country";
   public static final String OCSPSCS = "DIGIDOC_OCSP_SIGN_CERT_SERIAL";
   public static final String OCSPSCS_ENABLED = "SIGN_OCSP_REQUESTS";

   public static SimpleDateFormat sdf = null;

   public static Properties getProps() {
      return props;
   }

   public static void setProps(Properties props) {
      Config.props = props;
   }

   public static void init(String fileName) throws IOException {
	  InputStreamReader confReader = new InputStreamReader(new FileInputStream(fileName));
	  if (props == null)
		  props = new Properties();
	  props.load(confReader);
	  confReader.close();
      sdf = new SimpleDateFormat(Config.getProp(Config.DATE_F));
   }

   public static String getProp(String prop) {
      return props.getProperty(prop);
   }

   public static TextDevice getTextDevice() {
      return TextDevices.defaultTextDevice();
   }
}