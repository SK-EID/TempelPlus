package ee.sk.tempelPlus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import ee.sk.digidoc.DigiDocException;
import ee.sk.digidoc.Signature;
import ee.sk.digidoc.SignedDoc;
import ee.sk.digidoc.factory.DigiDocFactory;
import ee.sk.tempelPlus.util.Config;
import ee.sk.tempelPlus.util.Util;
import ee.sk.utils.ConfigManager;

public class Remove extends TempelPlus
{
   public Logger log = Logger.getLogger(Remove.class);

   public boolean run(String[] args) throws DigiDocException
   {
      try
      {
         if (args.length > 1 && (args[1].trim().equalsIgnoreCase("-?") || args[1].trim().equalsIgnoreCase("-help")))
         {
            printHelp();
            exit(0);
         }

         if (args.length < 3)
         {
            if (args.length == 2 && (args[1].trim() == "-?" || args[1].trim() == "-help"))
            {

            }
            printHelp();
            System.exit(1);
         }

         List<File> workFiles = getFilesWithExt(args[2], new ArrayList<File>());
         List<String> workFileOutputPaths = getRelativeOutputPaths(workFiles, args[2]);
         
         if(workFiles.size()==0)
         {
            log.error("No files with extension "+Config.getProp(Config.FORMAT)+" specified!");
            printHelp();
            System.exit(1);
         }
         
         parseParams(args);

         boolean OutputNull = outputFolder == null;

         setOutPut(args[2]);

         //Kontrollime failide olemasolu
//         for(File file:workFiles)
//         {
//            check(outputFolder + File.separator + file.getName(), Config.getProps().getProperty(Config.FORMAT), !OutputNull);
//         }
         
         askQuestion("Are you sure you want to remove signatures from "+workFiles.size()+" files? Y\\N");
         DigiDocFactory digFac = ConfigManager.instance().getDigiDocFactory();
         int i=1;
         int countSignatures=0;
         for(File file:workFiles)
         {
            log.info("Removing signatures from file "+i+" of "+workFiles.size()+". Currently processing '"+file.getName()+"'");
            SignedDoc sdoc = digFac.readSignedDoc(file.getAbsolutePath());
            log.info("Found "+sdoc.countSignatures()+" signatures:");
            for (int j=sdoc.countSignatures()-1;j>=0;j--)
            {
               Signature s = sdoc.getSignature(j);
               log.info(Util.getCNField(s));
               if(args[1].equals("ALL")||Util.getCNField(s).contains(args[1]))
               {
                  sdoc.removeSignature(j);
                  countSignatures++;
               }
            }
            
            log.info("Done");
            sdoc.writeToFile(new File(makeName((outputFolder==null?"":outputFolder)+File.separator + workFileOutputPaths.get(i-1) + File.separator + file.getName(),Config.getProps().getProperty(Config.FORMAT))));
            i++;
         }
         log.info(workFiles.size()+" documents were handled successfully. "+countSignatures+" signatures removed");
      }
      catch (Exception e)
      {
         verifyError(e, "Removal of signatures failed!", true);
    	  //log.error("Removal of signatures failed!",e);
         return true;
      }
      return false;
   }

   protected void printHelp(){
      log.info("Removes signature(s) from all given files");
      log.info("usage:");
      log.info("TempelPlus remove <signature desc> <folder or file> <additional params>");
      log.info("<signature desc>              1) signer certificate subject CN value to remove specific signature or ");
      log.info("                              2) ALL to remove all signatures of the document(s)");
      log.info("");
      log.info("Additional (optional) params:");
      log.info("-output_folder <folder>       folder where files are written");
   }

   public void parseParams(String[] args)
   {
      if (args.length > 3)
      {// lisaargumendid
         for (int i = 3; i < args.length; i++)
         {
            if (args[i].equalsIgnoreCase(OUTPUT_F))
            {
               if (i + 1 < args.length)
               {
                  outputFolder = new File(args[i + 1]);
               }
               i++;
            }
         }
      }
   }

}
