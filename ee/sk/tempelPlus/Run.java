package ee.sk.tempelPlus;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

import ee.sk.tempelPlus.util.Config;
/**
 * Starts tempelPlus from commandline
 * @author Kahro
 *
 */
public class Run {

   public static void main(String[] args) throws Exception {
      boolean errors = readConfig(args);
      if (errors) {
         System.exit(1);
      }
      String jDocLoc = Config.getProp(Config.JDOC_LOC);
      File libDir = new File(jDocLoc+File.separator+"lib"+File.separator);
      ArrayList<URL> urls = new ArrayList<URL>();
      urls.add(new File(jDocLoc+File.separator).toURI().toURL());
      urls.add(new File(jDocLoc+File.separator+"JDigiDoc.jar").toURI().toURL());
      urls.add(new File(jDocLoc+File.separator+"jdcerts.jar").toURI().toURL());
      urls.add(new File(jDocLoc+File.separator+"tinyxmlcanonicalizer-0.9.0.jar").toURI().toURL());
      urls.add(new File("TempelPlus.jar").toURI().toURL());
      String bcProv = Config.getProp(Config.BC_PROV);
      if(libDir.isDirectory()){
         for(String file:libDir.list()){
            if(!file.toLowerCase().contains("bcprov")||file.equals(bcProv))
               urls.add(new File(libDir.getAbsolutePath()+File.separator+file).toURI().toURL());
         }
      }else{
         System.out.println("JDoc location is invalid!");
         System.exit(1);
      }

//      for (URL url: urls)
//      {
//         System.out.println(url);
//      }

      ClassLoader cl = new URLClassLoader(urls.toArray(new URL[urls.size()]));
      Class c;
      if(args.length>=1&&args[0].equals("-g")){
         c = Class.forName("ee.sk.tempelPlus.util.GraphicSign", false, cl);
      }else{
         c = Class.forName("ee.sk.tempelPlus.TempelPlus", false, cl);
      }
      Method m = c.getMethod("main", String[].class);
      m.invoke(null, new Object[] { args });

   }

   public static ArgsParams ConcatParams(String[] args, int Position)
   {
      String ReturnValue = "";
      ArgsParams RetVal = new ArgsParams();
      RetVal.i = Position;

      if (args.length > Position + 1)
      {// lisaargumendid
         for (int i = Position; i < args.length; i++)
         {
            if (args[i].startsWith("-"))
            {
               break;
            }
            else
            {
               ReturnValue += (ReturnValue.length() > 0 ? " ": "") + args[i];
               RetVal.i++;
            }
         }
      }

      RetVal.Params = ReturnValue;

      return RetVal;
   }

  private static boolean readConfig(String[] args) {
      String configFile = null;
      if (args != null)
      {
         for (int i = 0; i < args.length; i++)
         {
            if (args[i].equalsIgnoreCase("-config") && i + 1 < args.length)
            {
               configFile = args[i + 1];

               ArgsParams params = ConcatParams(args, i + 1);
               configFile = params.Params;
            }
         }
      }
      if (configFile == null || configFile.trim().length() == 0)
         configFile = "TempelPlus.conf";
      System.out.println(configFile);
      try {
         Config.init(configFile);
      } catch (IOException e) {
         System.out.println("Reading configuration failed:");
         e.printStackTrace();
         return true;
      }
      return false;
   }
}