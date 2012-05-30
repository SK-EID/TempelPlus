package ee.sk.tempelPlus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import ee.sk.digidoc.DataFile;
import ee.sk.digidoc.DigiDocException;
import ee.sk.digidoc.SignedDoc;
import ee.sk.digidoc.TokenKeyInfo;
import ee.sk.digidoc.factory.DigiDocFactory;
import ee.sk.digidoc.factory.SignatureFactory;
import ee.sk.tempelPlus.util.Config;
import ee.sk.utils.ConfigManager;
import ee.sk.xmlenc.EncryptedData;
import ee.sk.xmlenc.factory.EncryptedDataParser;
import ee.sk.xmlenc.factory.EncryptedStreamParser;

/** Decryption functionality
 * @author Erik Kaju
 *
 */
public class Decrypt extends TempelPlus {

   public Logger log = Logger.getLogger(Decrypt.class);

   private String recipient, decryptionTargetFolder;

   private static String RECIPIENT="-recipient";

   public boolean run(String[] args) throws DigiDocException {
      try
      {
         if (args.length > 1 && (args[1].trim().equalsIgnoreCase("-?") || args[1].trim().equalsIgnoreCase("-help")))
         {
            printHelp();
            exit(0);
         }
         
         parseParams(args);
         
         List<File> workFiles = getFilesWithExt(args[1], new ArrayList<File>(), "cdoc");
         if (workFiles.size() == 0) {
        	log.error("Encrypted file type should be \"cdoc\" ");
        	log.info("");
            printHelp();
            System.exit(1);
         }
         setOutPut(args[1]);
         //Kontrollime failide olemasolu
         EncryptedDataParser dencFac =  ConfigManager.instance().getEncryptedDataParser();
         
         String[] decryptionTargetDirectories = new String[workFiles.size()];
         //for(File file:workFiles){
         for(int i = 0; i < workFiles.size(); i++){
        	File file = workFiles.get(i);
        	
         	//EncryptedData m_cdoc = dencFac.readEncryptedData(file.getAbsolutePath());
            
            //File folder = new File(outputFolder+File.separator+m_cdoc.findPropertyByName(EncryptedData.ENCPROP_FILENAME).getContent());
         	
            decryptionTargetDirectories[i] = makeName(outputFolder + File.separator + file.getName().replace("." + Config.getProps().getProperty(Config.CRYPT), ""), "");
            
            
            
//            File folder = new File(decryptionTargetDirectories[i]);
//            
//            if(folder.exists() && folder.isDirectory()){
//               log.error("Folder already exists:"+folder.getAbsolutePath());
//               exit(1);
//            }
         }
         
         askQuestion("Are you sure you want to decrypt " + workFiles.size() + " files, encrypted to cdoc? Y\\N");
         pin = pinInsertion();
         int i = 1;
         int count = 0;
         SignatureFactory sigFac = ConfigManager.instance().getSignatureFactory();
//         X509Certificate cert =null;
//         try{
//            //Muudame leveli taset et keerata logi kinni kui vale PIN on
//            Logger localLogger = Logger.getLogger(DigiDocException.class);
//            Level curr = localLogger.getLevel();
//            localLogger.setLevel(Level.FATAL);
//            cert = sigFac.getCertificate(0, pin);
//            cert.checkValidity();
//            localLogger.setLevel(curr);
//         }catch (DigiDocException e){
//            if(e.getCode()==DigiDocException.ERR_TOKEN_LOGIN){
//               log.error("Incorrect PIN!");
//               exit(1);
//            }else{
//               log.error(e.getMessage(),e);
//               exit(1);
//            }
//         }
//         Util.checkCertificate(cert);// Kontrollime sertifikaati
         DigiDocFactory digFac = ConfigManager.instance().getDigiDocFactory();

         TokenKeyInfo[] tokens = sigFac.getTokensOfType(true);
         int signer_token = 0;
         int current_token = 0;
         //System.out.println("Cert: " + signer_cn);
         if (recipient != null && recipient.length() > 0)
         {
            for (TokenKeyInfo tki : tokens)
            {
               if (tki.getCertName().equals(recipient))
               {
                  signer_token = current_token;
                  //signer_token = (int)tki.getToken().getTokenID();
               }
               current_token++;
            }
         }

         while (true) {
            //for (File file : workFiles) {
        	for(int forC = 0; forC < workFiles.size(); forC++){
               File file = workFiles.get(forC);
               
               log.info("Decrypting file " + i + " of " + workFiles.size() + ". Currently processing '" + file.getName() + "'");
               FileInputStream fis = new FileInputStream(file);
               File f2 = File.createTempFile(file.getName().substring(0,file.getName().lastIndexOf('.'))+"___", Config.getProps().getProperty(Config.FORMAT));
               f2.deleteOnExit();
               FileOutputStream fos = new FileOutputStream(f2);
                 //System.out.println(recipient);
               EncryptedStreamParser streamParser = ConfigManager.instance().getEncryptedStreamParser();
               // dekrüpteerime
               streamParser.decryptStreamUsingRecipientName(fis, fos,
                     signer_token, // kiipkaardi Tokeni number. Eesti ID kaardil alati 0
                  pin,  // kiipkaardi PIN kood. Eest ID kaardil PIN1
                  recipient);
                  //Util.getCNField(cert)); // vastuvõtja EncryptedKey atribuudi Recipient väärtus
               fos.close();
               fis.close();

               //Remove unusual data after the </SignedDoc> element

               BufferedReader reader = new BufferedReader(new FileReader(f2));

               String line = "", oldtext = "";
               char [] inputBuf = new char[1024 * 1024];

               int readChars = 0;

               while((readChars = reader.read(inputBuf, 0, 1024 * 1024)) > 0)
               {
                  line = String.copyValueOf(inputBuf, 0, readChars);

                  if (line.indexOf("</SignedDoc>") > 0)
                  {
                     String lineNew = line.substring(0, line.indexOf("</SignedDoc>") + 12);
                     oldtext += lineNew;
                  }
                  else
                  {
                     oldtext += line;
                  }
               }
               reader.close();
               // replace a word in a file
               //String newtext = oldtext.replaceAll("drink", "Love");

               //To replace a line in a file
               String newtext = oldtext;

               FileWriter writer = new FileWriter(f2);
               writer.write(newtext);
               writer.close();


               //tekitame kaustanime
               EncryptedData m_cdoc = dencFac.readEncryptedData(file.getAbsolutePath());
               //File folder = new File(outputFolder+File.separator+m_cdoc.findPropertyByName(EncryptedData.ENCPROP_FILENAME).getContent());
               File folder = new File(decryptionTargetDirectories[forC]);
               if(!folder.mkdir()){
                  log.error("Creation of directory '"+folder.getAbsolutePath()+"' failed!");
                  exit(1);
               }
               //pakime lahti seesolnud ddoc'i
               SignedDoc sdoc = digFac.readSignedDoc(f2.getAbsolutePath());
               File dataf = null;
               for (int j = 0; j < sdoc.countDataFiles(); j++) {
                  DataFile f = sdoc.getDataFile(j);
                  dataf = new File(makeName(folder.getAbsolutePath()+ File.separator + f.getFileName(), f.getFileName().substring(
                        f.getFileName().lastIndexOf(".") + 1)));
                  new FileOutputStream(dataf).write(f.getBodyAsData());
                  //f.cleanupDfCache();//laseme temp failid maha
                  count++;
               }
               i++;
               f2.delete();
               log.info("Done: " + dataf.getAbsolutePath());
               if (follow || remInput) {
                  log.info("trying to delete file:" + file.getName());
                  file.delete();
               }
            }
            if (follow) {
               Thread.sleep(1000);
               workFiles = getFiles(args[1], new ArrayList<File>());
               i=1;
            } else {
               break;
            }
         }
         log.info(workFiles.size() + " files decrypted successfully! " + count + " files created.");
      } catch (Exception e) {
         //log.error("Decryption of the files failed!", e);
         verifyError(e, "Decryption of the files failed!", false);
         return true;
      }
      return false;
   }

   public void parseParams(String[] args) {
      boolean fine = false;
      try {
         fine = true;
         if (args.length > 2) {// lisaargumendid
            for (int i = 2; i < args.length; i++) {
               
            	// Entering pin via command parameter
            	if (args[i].equalsIgnoreCase(PARAMPIN))
                {
                   if (i + 1 < args.length)
                   {
                	   commandParameterPin = args[i+1];
                	   i++;
                   }
                }
            	else if (args[i].equalsIgnoreCase(OUTPUT_F))
               {
//                  if (i + 1 < args.length)
//                  {
//                     outputFolder = new File(args[i + 1]);
//                  }
//                  i++;

                  if (i + 1 < args.length)
                  {
                     //outputFolder = new File(args[i + 1]);

                     ArgsParams params = ConcatParams(args, i + 1);
                     outputFolder = new File(params.Params);
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
               else if (args[i].equalsIgnoreCase(REM_INPUT))
               {
                  remInput = true;
               }
               else if (args[i].equalsIgnoreCase(FOLLOW))
               {
                  follow = true;
               }
               else if (args[i].equalsIgnoreCase(RECIPIENT))
               {
//                  if (i + 1 < args.length)
//                  {
//                     recipient=args[i+1];
//                  }
//                  i++;

                  if (i + 1 < args.length)
                  {
                     //outputFolder = new File(args[i + 1]);

                     ArgsParams params = ConcatParams(args, i + 1);
                     recipient = params.Params;
                     i = params.i - 1;
                  }
                  //i++;
               }
               else {
                  fine = false;
               }
            }
            if(follow&&outputFolder==null){
               log.error("Must specify output folder when using follow!");
               exit(1);
            } else if (follow && !remInput) {
               log.error("Must specify remove input when using follow!");
               exit(1);
            }
         }
      } catch (Exception e) {
         //log.error("Parsing parameters failed", e);
         verifyError(e, "Parsing parameters failed", true);
    	 fine = false;
      }
      if (!fine) {
         printHelp();
         System.exit(1);
      }
   }

   protected void printHelp() {
      log.info("Descrypts selected files");
      log.info("usage:");
      log.info("TempelPlus decrypt <folder or file> -recipient <CN> <additional params>");
      log.info("-recipient <CN>               Common name of encryption recipient's certificate");
      log.info("");
      log.info("Additional (optional) params:");
      log.info("-pin <code>        			  etoken pin code");
      log.info("-output_folder <folder>       folder where files are written");
      log.info("-remove_input                 deletes files that are used");
      log.info("-follow                       program does not exit and watches input folder");
      log.info("                              must also use parameters -output_folder and -remove_input");
   }
}