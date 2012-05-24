package ee.sk.tempelPlus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.activation.MimetypesFileTypeMap;

import org.apache.log4j.Logger;

import ee.sk.digidoc.DataFile;
import ee.sk.digidoc.DigiDocException;
import ee.sk.digidoc.SignedDoc;
import ee.sk.tempelPlus.util.Config;
import ee.sk.tempelPlus.util.Util;
import ee.sk.xmlenc.EncryptedData;
import ee.sk.xmlenc.EncryptedKey;

public class Crypt extends TempelPlus {

   public Logger log = Logger.getLogger(Crypt.class);

   private static final String CERT ="-cert";

   List<String> certPaths = new ArrayList<String>();

   public boolean run(String[] args) throws DigiDocException {
      try
      {
         if (args.length > 1 && (args[1].trim().equalsIgnoreCase("-?") || args[1].trim().equalsIgnoreCase("-help")))
         {
            printHelp();
            exit(0);
         }

         List<String> files = parseParams(args);

         List<File> workFiles = getFiles(files, new ArrayList<File>());
         if (workFiles.size() == 0) {
            printHelp();
            System.exit(1);
         }

         List<File> certs = getFiles(certPaths, new ArrayList<File>());
         setOutPut(args[1]);
         //Kontrollime failide olemasolu
         for(File file:workFiles){
            check(outputFolder + File.separator + file.getName(), "cdoc");
         }
         askQuestion("Are you sure you want to encrypt " + workFiles.size() + " files? Y\\N");

         MimetypesFileTypeMap m = new MimetypesFileTypeMap();
         int i = 1;
         ArrayList<EncryptedKey> keys = new ArrayList<EncryptedKey>();
         String recipient =null;
         int r=1;
         for (File cert : certs) {
            X509Certificate recvCert = SignedDoc.readCertificate(cert);

            boolean keyUsages[] = recvCert.getKeyUsage();

            if(keyUsages != null && keyUsages.length > 3 && keyUsages[3] == true)
            {
               //is crypto cert
            }
            else
            {
               log.info("The certificate " + cert.getAbsolutePath() +  " used in crypto process was not meant for crypting the data.");
               log.info("Please specify another certificate.");
               System.exit(1);
            }

            EncryptedKey ekey = new EncryptedKey("ID"+r, // optional Id atribute value
                  Util.getCNField(recvCert), // optional Recipient atribute value
                  EncryptedData.DENC_ENC_METHOD_RSA1_5, // fikseeritud krüptoalgoritm
                  null, // optional KeyName subelement value
                  null, // optional CarriedKeyName subelement value
                  recvCert); // recipients certificate. Required!
            keys.add(ekey);
            if ((recvCert != null) && (recipient == null))
               recipient = SignedDoc.getCommonName(recvCert.getSubjectDN().getName());
            r++;
         }
         for (File file : workFiles) {
            log.info("Encrypting file " + i + " of " + workFiles.size() + ". Currently processing '" + file.getName() + "'");
            String mimeType = m.getContentType(file);
//            SignedDoc sdoc = new SignedDoc(SignedDoc.FORMAT_DIGIDOC_XML, SignedDoc.VERSION_1_3);
//            DataFile df = sdoc.addDataFile(file, mimeType, DataFile.CONTENT_EMBEDDED_BASE64);
//            byte[] data = SignedDoc.readFile(file);
//            //df.setBase64Body(data);
//            df.setBody(data);
//            byte[] inData = sdoc.toXML().getBytes();
//            EncryptedData cdoc = new EncryptedData(null, null, null, EncryptedData.DENC_XMLNS_XMLENC, EncryptedData.DENC_ENC_METHOD_AES128);
//            for(EncryptedKey ekey:keys){
//               cdoc.addEncryptedKey(ekey);
//            }
//            cdoc.setData(inData);
//            cdoc.setDataStatus(1);
//            String name = file.getName().substring(0,file.getName().lastIndexOf('.'));
//
//            cdoc.addProperty("Filename", name + ".ddoc");
//            //cdoc.addProperty("OriginalMimeType", SignedDoc.xmlns_digidoc);
//            StringBuffer sb = new StringBuffer();
//            sb.append(file.getName());
//            sb.append("|");
//            sb.append(new Long(file.length()).toString() + " B|");
//            sb.append(mimeType+"|");
//            sb.append("/" + file.getName());
//            cdoc.addProperty("orig_file", sb.toString());
//            cdoc.encrypt(EncryptedData.DENC_COMPRESS_BEST_EFFORT);
//            //cdoc.encrypt(EncryptedData.DENC_COMPRESS_NEVER);
//            FileOutputStream fos = new FileOutputStream(new File(makeName(outputFolder + File.separator + file.getName(), "cdoc")));
//            fos.write(cdoc.toXML());
//            fos.close();

//            //teeme valmis ddoc konteineri
            SignedDoc sdoc = new SignedDoc(SignedDoc.FORMAT_DIGIDOC_XML, SignedDoc.VERSION_1_3);
            //String mimeType = m.getContentType(file);
            sdoc.addDataFile(file, mimeType, DataFile.CONTENT_EMBEDDED_BASE64);
            File f2 = File.createTempFile(file.getName().substring(0,file.getName().lastIndexOf('.'))+"___", Config.getProps().getProperty(Config.FORMAT));
            f2.deleteOnExit();
            sdoc.writeToFile(f2);

            //Remove unusual data after the </SignedDoc> element

            //File file1 = new File(outFileName);
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




            //teeme valmis krüpti konteineri
            EncryptedData cdoc = new EncryptedData(null, // optional Id atribute
                  // value
                  null, // optional Type atribute value
                  null, // optional Mime atribute value
                  EncryptedData.DENC_XMLNS_XMLENC, // fixed xml namespace
                  EncryptedData.DENC_ENC_METHOD_AES128); // fixed cryptographikalgorithm
            for(EncryptedKey ekey:keys){
               cdoc.addEncryptedKey(ekey);
            }

            //cdoc.addProperty(EncryptedData.ENCPROP_FILENAME, file.getName());

            String name = file.getName().substring(0,file.getName().lastIndexOf('.'));
            cdoc.addProperty(EncryptedData.ENCPROP_FILENAME, name + ".ddoc");

            //cdoc.addProperty(EncryptedData.ENCPROP_ORIG_MIME, m.getContentType(f2));
            cdoc.addProperty(EncryptedData.ENCPROP_ORIG_MIME, SignedDoc.xmlns_digidoc);

            StringBuffer sb = new StringBuffer();
            sb.append(file.getName());
            sb.append("|");
            sb.append(new Long(file.length()).toString() + " B|");
            sb.append(mimeType+"|");
            sb.append("/" + file.getName());
            cdoc.addProperty("orig_file", sb.toString());

            // Krüpteerime. Valikud on ainult EncryptedData.DENC_COMPRESS_ALLWAYS
            // ja EncryptedData.DENC_COMPRESS_NEVER
            String outFileName = makeName(outputFolder + File.separator + file.getName(), "cdoc");
            File outFile = new File(outFileName);
            cdoc.encryptStream(new FileInputStream(f2), new FileOutputStream(outFile), EncryptedData.DENC_COMPRESS_BEST_EFFORT);

            f2.delete();

            i++;
            log.info("Done");
         }
         log.info(workFiles.size() + " files encrypted successfully!.");
      } catch (Exception e) {
         log.error("Encryption of the files failed!", e);
         return true;
      }
      return false;
   }

   protected void printHelp() {
      log.info("Encrypts selected files using given certificates");
      log.info("usage:");
      log.info("TempelPlus encrypt <folder or file> -cert <list of certificate files to be used> <additional params>");
      log.info("Additional (optional) params:");
      log.info("-output_folder <folder>       folder where files are written");
   }

   public List<String> parseParams(String[] args) {
      boolean fine = false;
      ArrayList<String> files = new ArrayList<String>();

      try {
         fine = true;
         if (args.length > 3) {// lisaargumendid
            for (int i = 1; i < args.length; i++)
            {
               if (args[i].equalsIgnoreCase(OUTPUT_F))
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
               else if (args[i].equalsIgnoreCase(CERT))
               {
                  while(i+1<args.length&&!args[i+1].startsWith("-")){
                     i++;
                     certPaths.add(args[i]);
                  }
               }
               else
               {
                  ArgsParams params = ConcatParams(args, i);
                     //i = params.i - 1;
                  if (!params.Params.isEmpty())
                  {
                     files.add(params.Params);
                  }

                  //i = params.i - 1;

                  if(outputFolder!=null||!certPaths.isEmpty())
                  {
                     fine = false;
                  }
               }
            }
            if(certPaths==null||certPaths.isEmpty()){
               log.error("Must specify cert!");
               printHelp();
               exit(1);
            }
         }else if(args.length<3)
            fine=false;
      } catch (Exception e) {
         log.error("Parsing parameters failed", e);
         fine = false;
      }

      if (!fine) {
         printHelp();
         System.exit(1);
      }
      return files;
   }

}
