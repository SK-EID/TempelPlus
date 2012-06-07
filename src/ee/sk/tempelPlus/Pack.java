package ee.sk.tempelPlus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.activation.MimetypesFileTypeMap;

import org.apache.log4j.Logger;

import ee.sk.digidoc.DataFile;
import ee.sk.digidoc.DigiDocException;
import ee.sk.digidoc.SignedDoc;
import ee.sk.tempelPlus.util.Config;

public class Pack extends TempelPlus {

   private static final String ADD_F = "-add_file";

   private static ArrayList<String> addFilesDir=null;
   private static List<File> addFiles = null;

   public Logger log = Logger.getLogger(Pack.class);

   public boolean run(String[] args) throws DigiDocException {
      try {
         parseParams(args);
         if (args.length > 1 && (args[1].trim().equalsIgnoreCase("-?") || args[1].trim().equalsIgnoreCase("-help")))
         {
            printHelp();
            exit(0);
         }
         if (args[1].trim().equalsIgnoreCase("?"))
         {
            printHelp();
            exit(1);
         }
         List<File> workFiles = getFiles(args[1], new ArrayList<File>());
         if(addFilesDir!=null&&addFilesDir.size()!=0){
            addFiles=new ArrayList<File>();
            for(String folder:addFilesDir)
               addFiles = getFiles(folder, addFiles);
         }
         if (workFiles.size() == 0) {
            printHelp();
            System.exit(1);
         }
         setOutPut(args[1]);
         //Kontrollime failide olemasolu
         for(File file:workFiles){
            if(!isDigiDoc(file)){
               check(outputFolder + File.separator + file.getName(), Config.getProps().getProperty(Config.FORMAT));
            }else{
               check(outputFolder + File.separator + file.getName().substring(0,file.getName().lastIndexOf('.')), Config.getProps().getProperty(Config.FORMAT));
            }
         }
         askQuestion("Are you sure you want to create " + workFiles.size() + " new files? Y\\N ");
         MimetypesFileTypeMap m = new MimetypesFileTypeMap();
         int i = 1;
         for (File file : workFiles) {
            log.info("Creating file " + i + " of " + workFiles.size() + ". Currently processing '"
                  + file.getName() + "'");
            SignedDoc sdoc = new SignedDoc(SignedDoc.FORMAT_DIGIDOC_XML, SignedDoc.VERSION_1_3);
            String mimeType = m.getContentType(file);
            sdoc.addDataFile(file, mimeType, DataFile.CONTENT_EMBEDDED_BASE64);
            if(addFiles!=null){
               for(File addF:addFiles){
                  log.info("Adding additional file "+addF.getName());
                  mimeType = m.getContentType(file);
                  sdoc.addDataFile(addF, mimeType, DataFile.CONTENT_EMBEDDED_BASE64);
               }
            }
            i++;
//            log.info(file.getAbsolutePath());
            if(!isDigiDoc(file)){
               sdoc.writeToFile(new File(makeName(outputFolder+File.separator+file.getName(), Config.getProps().getProperty(Config.FORMAT))));
            }else{
               sdoc.writeToFile(new File(makeName(outputFolder+File.separator+file.getName().substring(0,file.getName().lastIndexOf('.')), Config.getProps().getProperty(Config.FORMAT))));
            }
            log.info("Done");
         }
         log.info(workFiles.size() + " documents created successfully.");
      } catch (Exception e) {
    	 verifyError(e, "Creation of the files failed!", true);
         //log.error("Creation of the files failed!",e);
         return true;
      }
      return false;
   }

   protected void printHelp() {
      log.info("Makes a ddoc container from files");
      log.info("usage:");
      log.info("TempelPlus container <folder or file> <additional params>");
      log.info("");
      log.info("Additional (optional) params:");
      log.info("-output_folder <folder>            folder where files are written");
      log.info("-add_file <folder or file list>    files to be added to every created container");
   }

   public void parseParams(String[] args) {
      if (args.length > 2) {// lisaargumendid
         for (int i = 2; i < args.length; i++) {
            if (args[i].equalsIgnoreCase(OUTPUT_F))
            {
//               if (i + 1 < args.length)
//               {
//                  outputFolder = new File(args[i + 1]);
//               }
//               i++;
               if (i + 1 < args.length)
               {
                  //outputFolder = new File(args[i + 1]);

                  ArgsParams params = ConcatParams(args, i + 1);
                  outputFolder = new File(params.Params);
                  i = params.i - 1;
               }

            } else if (args[i].equalsIgnoreCase(ADD_F)) {
               addFilesDir = new ArrayList<String>();
               for(int j=i+1;j<args.length;j++){
                  if(!args[j].startsWith("-")){
                     addFilesDir.add(args[j]);
                  }
               }
               i+=addFilesDir.size();
            }
         }
      }
   }

}
