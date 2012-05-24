package ee.sk.tempelPlus;

import java.io.File;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.activation.MimetypesFileTypeMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import ee.sk.digidoc.DataFile;
import ee.sk.digidoc.DigiDocException;
import ee.sk.digidoc.Signature;
import ee.sk.digidoc.SignatureProductionPlace;
import ee.sk.digidoc.SignedDoc;
import ee.sk.digidoc.TokenKeyInfo;
import ee.sk.digidoc.factory.DigiDocFactory;
import ee.sk.digidoc.factory.PKCS11SignatureFactory;
import ee.sk.digidoc.factory.SignatureFactory;
import ee.sk.tempelPlus.util.Config;
import ee.sk.tempelPlus.util.PinReader;
import ee.sk.tempelPlus.util.Util;
import ee.sk.utils.ConfigManager;

public class Sign extends TempelPlus {

   private static final String SIGNER_CN = "-signer_cn";
   private static final String ROLE = "-role";
   private static final String COUNTRY = "-country";
   private static final String STATE = "-state";
   private static final String CITY = "-city";
   private static final String POSTCODE = "-postcode";

   public Logger log = Logger.getLogger(Sign.class);

   public boolean run(String[] args) throws DigiDocException {
      parseParams(args);
      try
      {
         if (args.length > 1 && (args[1].trim().equalsIgnoreCase("-?") || args[1].trim().equalsIgnoreCase("-help")))
         {
            printHelp();
            exit(0);
         }
         if (args[1].trim().equalsIgnoreCase("?"))
         {
            log.error("No files specified!");
            printHelp();
            exit(1);
         }
         List<File> workFiles = getFiles(args[1], new ArrayList<File>());
         if (workFiles.size() == 0 && !follow) {
            log.error("No files specified!");
            printHelp();
            exit(0);
         }
         setOutPut(args[1]);
         // Kontrollime failide olemasolu
         for (File file : workFiles) {
            check(outputFolder + File.separator + file.getName(), Config.getProps().getProperty(Config.FORMAT));
         }
         String signer_cn = "";
         signer_cn = Config.getProp(Config.SIGNCN);

         SignatureFactory sigFac = ConfigManager.instance().getSignatureFactory();
         TokenKeyInfo[] tokens = sigFac.getTokensOfType(true);
         long signer_token = -1;
         int current_token = 0;
         //System.out.println("Cert: " + signer_cn);
         if (signer_cn != null && signer_cn.length() > 0)
         {
//            for (String sig_cn : sigFac.getAvailableTokenNames())
//            {
//               if (sig_cn.equals(signer_cn))
//               {
//
//                  signer_token = current_token;
//                  //break;
//               }
//
//               System.out.println(sig_cn);
//
//               current_token++;
//            }

            for (TokenKeyInfo tki : tokens)
            {
               //System.out.println("Existing cert: " + tki.getCertName());
               //log.info("Cert " + tki.getCertName());
               if (tki.getCertName().equals(signer_cn))
               {
                  //signer_token = tki.getToken().getTokenID();
                  signer_token = current_token;
               }
               current_token++;
            }

            if (signer_token == -1)
            {
               log.error("\nCommon Name of certificate used in signing process not found!");
               printHelp();
               exit(0);
            }
         }
         else
         {
            signer_token = 0;
         }

         if (workFiles.size() != 0)
            askQuestion("Are you sure you want to sign " + workFiles.size() + " files? Y\\N");
//         if (signer_token == 1)
//         {
//            signer_token = 0;
//         }
         //System.out.println(signer_token);
         MimetypesFileTypeMap m = new MimetypesFileTypeMap();

         String pin = new PinReader().askPin();

//         X509Certificate cert = null;
//         try {
//            // Muudame leveli taset et keerata logi kinni kui vale PIN on
//            Logger localLogger = Logger.getLogger(DigiDocException.class);
//            Level curr = localLogger.getLevel();
//            localLogger.setLevel(Level.ALL);
//            cert = sigFac.getCertificate((int)signer_token, pin);
//            cert.checkValidity();
//            localLogger.setLevel(curr);
//         } catch (DigiDocException e) {
//            if (e.getCode() == DigiDocException.ERR_TOKEN_LOGIN) {
//               log.error("Incorrect PIN!");
//               exit(1);
//            } else {
//               log.error(e.getMessage(), e);
//               exit(1);
//            }
//
//         }
//         Util.checkCertificate(cert);// Kontrollime sertifikaati
//         Util.initOSCPSerial(cert);
//         SignatureProductionPlace addr = new SignatureProductionPlace(Config.getProps().getProperty(Config.CITY),
//               Config.getProps().getProperty(Config.STATE), Config.getProps().getProperty(Config.COUNTRY), Config
//                     .getProps().getProperty(Config.POSTCODE));
//         String[] roles = null;
//         if (Config.getProps().getProperty(Config.ROLE) != null) {
//            roles = new String[] { Config.getProps().getProperty(Config.ROLE) };
//         }
//         int i = 1;
//         DigiDocFactory digFac = null;
//         SignedDoc sdoc;

         while (true)
         {
            int i = 1;
            for (File file : workFiles)
            {
               X509Certificate cert = null;
               try {
                  // Muudame leveli taset et keerata logi kinni kui vale PIN on
                  Logger localLogger = Logger.getLogger(DigiDocException.class);
                  Level curr = localLogger.getLevel();
                  localLogger.setLevel(Level.ALL);
                  cert = sigFac.getCertificate((int)signer_token, pin);
                  cert.checkValidity();

                  localLogger.setLevel(curr);
               } catch (DigiDocException e) {
                  if (e.getCode() == DigiDocException.ERR_TOKEN_LOGIN) {
                     log.error("Incorrect PIN!");
                     exit(1);
                  } else {
                     log.error(e.getMessage(), e);
                     exit(1);
                  }

               }
               Util.checkCertificate(cert);// Kontrollime sertifikaati
               Util.initOSCPSerial(cert);
               SignatureProductionPlace addr = new SignatureProductionPlace(Config.getProps().getProperty(Config.CITY),
                     Config.getProps().getProperty(Config.STATE), Config.getProps().getProperty(Config.COUNTRY), Config
                           .getProps().getProperty(Config.POSTCODE));
               String[] roles = null;
               if (Config.getProps().getProperty(Config.ROLE) != null) {
                  roles = new String[] { Config.getProps().getProperty(Config.ROLE) };
               }
               DigiDocFactory digFac = null;
               SignedDoc sdoc;


               log.info("Signing file " + i + " of " + workFiles.size() + ". Currently signing '" + file.getName()
                     + "'");
               if (!isDigiDoc(file)) {
                  sdoc = new SignedDoc(SignedDoc.FORMAT_DIGIDOC_XML, SignedDoc.VERSION_1_3);
                  log.info("File is not ddoc, converting..");
                  String mimeType = m.getContentType(file);
                  sdoc.addDataFile(file, mimeType, DataFile.CONTENT_EMBEDDED_BASE64);
               } else {
                  if (digFac == null)
                     digFac = ConfigManager.instance().getDigiDocFactory();
                  sdoc = digFac.readSignedDoc(file.getAbsolutePath());
               }
               Signature sig = sdoc.prepareSignature(cert, roles, addr);
               byte[] sidigest = sig.calculateSignedInfoDigest();
               byte[] sigval = null;
               try {
                  sigval = sigFac.sign(sidigest, 0, pin);
               } catch (DigiDocException e) {
                  if (e.getNestedException() != null && e.getNestedException().getMessage() != null
                        && e.getNestedException().getMessage().trim().equals("CKR_USER_NOT_LOGGED_IN")) {
                     log.warn("Not logged in message recieved, trying to restore sigfactory!");
                     if (sigFac instanceof PKCS11SignatureFactory) {
                        ((PKCS11SignatureFactory) sigFac).reset();
                        sigval = sigFac.sign(sidigest, 0, pin);
                     } else {
                        sigFac.reset();
                        sigval = sigFac.sign(sidigest, 0, pin);
                     }
                  } else {
                     log.error("Signing failed!", e);
                     return true;
                  }
               }
               sig.setSignatureValue(sigval);
               sig.getConfirmation();
               String fileName = outputFolder + File.separator + file.getName();
               if (!isDigiDoc(file))
                  fileName = makeName(fileName, Config.getProps().getProperty(Config.FORMAT));
               sdoc.writeToFile(new File(fileName));
               log.info("cleaning cache");
               //sdoc.cleanupDfCache();
               if (follow || remInput) {
                  log.info("trying to delete file:" + file.getName());
                  file.delete();
               }
               i++;
               log.info("Done");
            }
            if (follow) {
               Thread.sleep(1000);
               workFiles = getFiles(args[1], new ArrayList<File>());
               i = 1;
            } else {
               break;
            }
         }
         log.info(workFiles.size() + " documents signed successfully");
      } catch (Exception e) {
         log.error("Signing failed!", e);
         return true;
      }
      return false;
   }

//   public ArgsParams ConcatParams(String[] args, int Position)
//   {
//      String ReturnValue = "";
//      ArgsParams RetVal = new ArgsParams();
//      RetVal.i = Position;
//
//      if (args.length > Position + 1)
//      {// lisaargumendid
//         for (int i = Position; i < args.length; i++)
//         {
//            if (args[i].startsWith("-"))
//            {
//               break;
//            }
//            else
//            {
//               ReturnValue += (ReturnValue.length() > 0 ? " ": "") + args[i];
//               RetVal.i++;
//            }
//         }
//      }
//
//      RetVal.Params = ReturnValue;
//
//      return RetVal;
//   }

   public void parseParams(String[] args) {
      boolean fine = false;
      try
      {
         fine = true;
         if (args.length > 2)
         {// lisaargumendid
            for (int i = 2; i < args.length; i++)
            {
               if (args[i].equalsIgnoreCase(OUTPUT_F))
               {
                  if (i + 1 < args.length)
                  {
                     //outputFolder = new File(args[i + 1]);

                     ArgsParams params = ConcatParams(args, i + 1);
                     outputFolder = new File(params.Params);
                     i = params.i - 1;
                  }
                  //i++;
               }
               else if (args[i].equalsIgnoreCase(REM_INPUT))
               {
                  remInput = true;
               }
               else if (args[i].equalsIgnoreCase(FOLLOW))
               {
                  follow = true;
               }
               else if (args[i].equalsIgnoreCase(SIGNER_CN))
               {
                  if (i + 1 < args.length)
                  {
                     ArgsParams params = ConcatParams(args, i + 1);
                     Config.getProps().put(Config.SIGNCN, params.Params);
                     i = params.i - 1;
                  }
                  //i++;
               }
               else if (args[i].equalsIgnoreCase(ROLE))
               {
//                  if (i + 1 < args.length)
//                  {
//                     Config.getProps().put(Config.ROLE, args[i + 1]);
//                  }
//                  i++;

                  if (i + 1 < args.length)
                  {
                     ArgsParams params = ConcatParams(args, i + 1);
                     Config.getProps().put(Config.ROLE, params.Params);
                     i = params.i - 1;
                  }
                  //i++;
               }
               else if (args[i].equalsIgnoreCase(COUNTRY))
               {
//                  if (i + 1 < args.length)
//                  {
//                     Config.getProps().put(Config.COUNTRY, args[i + 1]);
//                  }
//                  i++;

                  if (i + 1 < args.length)
                  {
                     ArgsParams params = ConcatParams(args, i + 1);
                     Config.getProps().put(Config.COUNTRY, params.Params);
                     i = params.i - 1;
                  }
                  //i++;
               }
               else if (args[i].equalsIgnoreCase(STATE))
               {
//                  if (i + 1 < args.length)
//                  {
//                     Config.getProps().put(Config.STATE, args[i + 1]);
//                  }
//                  i++;

                  if (i + 1 < args.length)
                  {
                     ArgsParams params = ConcatParams(args, i + 1);
                     Config.getProps().put(Config.STATE, params.Params);
                     i = params.i - 1;
                  }
                  //i++;
               }
               else if (args[i].equalsIgnoreCase(CITY))
               {
//                  if (i + 1 < args.length)
//                  {
//                     Config.getProps().put(Config.CITY, args[i + 1]);
//                  }
//                  i++;

                  if (i + 1 < args.length)
                  {
                     ArgsParams params = ConcatParams(args, i + 1);
                     Config.getProps().put(Config.CITY, params.Params);
                     i = params.i - 1;
                  }
                  //i++;
               }
               else if (args[i].equalsIgnoreCase(POSTCODE))
               {
//                  if (i + 1 < args.length)
//                  {
//                     Config.getProps().put(Config.POSTCODE, args[i + 1]);
//                  }
//                  i++;

                  if (i + 1 < args.length)
                  {
                     ArgsParams params = ConcatParams(args, i + 1);
                     Config.getProps().put(Config.POSTCODE, params.Params);
                     i = params.i - 1;
                  }
                  //i++;
               }
               else if (args[i].equalsIgnoreCase("-config"))
               {
                  if (i + 1 < args.length)
                  {
                     ArgsParams params = ConcatParams(args, i + 1);
                     i = params.i - 1;
                  }
                  //i++;
               }
               else
               {
                  System.out.println(args[i]);
                  fine = false;
               }
            }
            if (follow)
            {
               if (outputFolder == null && remInput)
               {
                  log.error("Must specify output folder when using follow!");
                  exit(1);
               } else if (outputFolder == null) {
                  log.error("Must specify output folder and remove input when using follow!");
                  exit(1);
               } else if (!remInput) {
                  log.error("Must specify remove input when using follow!");
                  exit(1);
               }
            }
         }
      } catch (Exception e) {
         log.error("Parsing parameters failed", e);
         fine = false;
      }
      if (!fine) {
         printHelp();
         System.exit(1);
      }
   }

   protected void printHelp() {
      // log.info("##############################################################");
      log.info("Signs all given files");
      log.info("usage:");
      log.info("TempelPlus sign <folder or file> <additional params>");
      log.info("Additional (optional) params:");
      log.info("-output_folder <folder>       folder where files are written");
      log.info("-remove_input                 deletes files that are used");
      log.info("-follow                       program does not exit and watches input folder");
      log.info("                              must also use parameters -output_folder and -remove_input");
      log.info("-signer_cn <cert_name>        Common Name of certificate to be used for signing");
      log.info("-role <role> 	              signer role used in signing");
      log.info("-country <country>            signer country used in signing");
      log.info("-state <state>                signer state used in signing");
      log.info("-city <city>                  signer city used in signing");
      log.info("-postcode <postcode>          signer postcode used in signing");
      // log.info("##############################################################");
   }
}

