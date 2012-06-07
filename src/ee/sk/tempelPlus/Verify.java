package ee.sk.tempelPlus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import ee.sk.digidoc.DataFile;
import ee.sk.digidoc.DigiDocException;
import ee.sk.digidoc.Signature;
import ee.sk.digidoc.SignedDoc;
import ee.sk.digidoc.factory.DigiDocFactory;
import ee.sk.tempelPlus.util.Config;
import ee.sk.tempelPlus.util.Util;
import ee.sk.utils.ConfigManager;

public class Verify extends TempelPlus{

   public Logger log = Logger.getLogger(Verify.class);

   public boolean run(String[] args) throws DigiDocException {
      try
      {
//         for (int i = 0; i < args.length; i++)
//         {
//            System.out.println(args[i]);
//
//         }

         if (args.length > 1 && (args[1].trim().equalsIgnoreCase("-?") || args[1].trim().equalsIgnoreCase("-help")))
         {
            printHelp();
            exit(0);
         }


       List<File> workFiles = getFilesWithExt(args[1], new ArrayList<File>());
       if(workFiles.size()==0){
          log.error("No signed containers specified!");
          printHelp();
          System.exit(1);
       }
       askQuestion("Are you sure you want to get signature info for "+workFiles.size()+" files? Y\\N");

       int i=1;
       int validSignatures=0;
       int unValidSignatures=0;
       for(File file:workFiles)
       {
          log.info("Verifying file "+i+" of "+workFiles.size()+". Currently verifying '"+file.getName()+"'");
          DigiDocFactory digFac = ConfigManager.instance().getDigiDocFactory();
          SignedDoc sdoc = digFac.readSignedDoc(file.getAbsolutePath());
          sdoc.verify(true, true);
          log.info("Found "+sdoc.countDataFiles()+" datafiles:");
          for (int j=0;j<sdoc.countDataFiles();j++)
          {
             DataFile d = sdoc.getDataFile(j);
             log.info("Datafile "+d.getId()+": filename: "+d.getFileName()+", mime: "+d.getMimeType());
          }
          log.info("Found "+sdoc.countSignatures()+" signatures:");
          for (int j=0;j<sdoc.countSignatures();j++)
          {
             Signature s = sdoc.getSignature(j);//TODO who is signer??
             ArrayList<DigiDocException> errors = s.verify(sdoc, true, true);

//             System.out.println("Verification error are:");
//             for(DigiDocException e:errors)
//             {
//                System.out.println(e);
//             }

             ArrayList<DigiDocException> errors2 = new ArrayList<DigiDocException>();
             errors2.addAll(s.validate());

//             System.out.println("Validation error are:");
//             for(DigiDocException e:errors2)
//             {
//                System.out.println(e);
//             }

             errors2.addAll(errors);

             String status="OK";
             if(errors2!=null&&!errors2.isEmpty()){
                status = "ERROR";
             }
             log.info("Signature "+s.getId()+", Signer: "+Util.getCNField(s)+", SigningTime: "+Config.sdf.format(s.getSignedProperties().getSigningTime())+", "+status);
             if(errors2==null||errors2.isEmpty())
                validSignatures++;
             else{
                log.debug("Invalid signature. Errors are:");
                for(DigiDocException e:errors2){
                   log.debug(e);
                }
                unValidSignatures++;
             }
          }
          i++;
          log.info("Done");
       }
       log.info(workFiles.size()+" documents verified successfully");
       log.info("TempelPlus found "+validSignatures+" valid signatures and "+unValidSignatures+" invalid signatures");
      }catch (Exception e){
    	 verifyError(e, "Verification failed!", true);
         
         return true;
      }
      return false;
   }

   protected void printHelp(){
      log.info("Verifies signature(s) of all given files");
      log.info("usage:");
      log.info("TempelPlus verify <folder or file>");
   }

}
