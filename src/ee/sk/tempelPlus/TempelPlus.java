package ee.sk.tempelPlus;

//import java.io.Console;
//import java.io.File;
//import java.io.IOException;
//import java.util.List;
//import java.util.regex.Pattern;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import ee.sk.digidoc.DigiDocException;
import ee.sk.tempelPlus.util.Config;
import ee.sk.tempelPlus.util.PinReader;
import ee.sk.tempelPlus.util.TempelPlusException;
import ee.sk.tempelPlus.util.console.TextDevice;
import ee.sk.utils.ConfigManager;


/** Main program
 * Tempel+ starts, then signing, verifying, decrypting, extracting etc process is executed, program ends.
 * @author Erik Kaju
 *
 */

public abstract class TempelPlus {
	// Käsud
	private static final String SIGN = "sign";
	private static final String VERIFY = "verify";
	private static final String REMOVE = "remove";
	private static final String EXCTRACT = "extract";
	private static final String PACK = "container";
	private static final String CRYPT = "encrypt";
	private static final String DECRYPT = "decrypt";

	public boolean remInput = false;
	public boolean follow = false;
	public File outputFolder;
	private static String configFile = null;
	public static final String version = "v1.1.0";
	static long start = 0;

	private static Logger log = null;
	// Üldine parameeter
	public final String OUTPUT_F = "-output_folder";
	public final String REM_INPUT = "-remove_input";
	public final String FOLLOW = "-follow";
	public final String PARAMPIN = "-pin";
	
	public boolean usingOutPutFolder = false;
	boolean firstRun = true;
	
	String pin;
	public String commandParameterPin = null;

	public static void main(String[] args) throws DigiDocException
	{
		for (int i = 0; i < args.length; i++) {
			args[i] = args[i].replace((char) 8211, (char) 45);// asendame valed
																// kriipsud
																// õigetega
			args[i] = args[i].replace((char) 8212, (char) 45);
		}
//		for (String arg: args)
//		{
//		   System.out.println(arg);
//		}

		boolean errors = readConfig(args);
		if (errors) {
			printHelpAndExit(1);
		}
		log.info("TempelPlus " + version + " starting");
		log.info("Using configfile:" + configFile);
		log.info("User:" + System.getProperty("user.name"));
		start = System.currentTimeMillis();
		int command = parseCommand(args);
		if (command == 0) {
			printHelpAndExit(0);
		} else if (command == 1) {
			Sign sign = new Sign();
			sign.run(args);
		} else if (command == 2) {
			Verify verify = new Verify();
			verify.run(args);
		} else if (command == 3) {
			Remove remove = new Remove();
			remove.run(args);
		} else if (command == 4) {
			Extract extract = new Extract();
			extract.run(args);
		} else if (command == 5) {
			Pack pack = new Pack();
			pack.run(args);
		} else if (command == 6) {
			Crypt crypt = new Crypt();
			crypt.run(args);
		} else if (command == 7) {
			Decrypt decrypt = new Decrypt();
			decrypt.run(args);
		}
		if (errors || command == -1) {
			printHelpAndExit(1);
		}
		// long end = System.currentTimeMillis()-start;
		// log.info("TempelPlus "+version+" stopping. Time used:"+end/1000+"s");
		exit(0);
	}

	private static void printHelpAndExit(int level) {
		System.out.println("usage:");
		System.out.println("Help : TempelPlus -?");
		System.out.println("Help : TempelPlus -help");
		System.out
				.println("Options: sign <parameters>- makes(if needed) and signs documents");
		System.out
				.println("        verify <parameters>- verifies signatures of signed document");
		System.out
				.println("        remove <parameters>- removes signature from signed document (digidoc container)");
		System.out
				.println("        extract <parameters> - extracts datafile from signed document");
		System.out
				.println("        container <parameters> - makes a new container");
		System.out
				.println("        encrypt <parameters> - encrypts a file");
		System.out
				.println("        decrypt <parameters> - decrypts a file");
		System.out.println("Option help : TempelPlus <option> <-?> or <-help>");
		exit(1);
	}

	private static int parseCommand(String[] args) {
		int action = -1;
		if (args != null && args.length >= 2) {
			if (args[0].equals(SIGN)) {
				action = 1;
			} else if (args[0].equals(VERIFY)) {
				action = 2;
			} else if (args[0].equals(REMOVE)) {
				action = 3;
			} else if (args[0].equals(EXCTRACT)) {
				action = 4;
			} else if (args[0].equals(PACK)) {
				action = 5;
			} else if (args[0].equals(CRYPT)) {
				action = 6;
			} else if (args[0].equals(DECRYPT)) {
				action = 7;
			}
		}
		return action;
	}

	public boolean isDigiDoc(File file) {
		if (file.getName().endsWith("ddoc") || file.getName().endsWith("bdoc")) {
			return true;
		}
		return false;
	}

	/**
	 * Gets all the files from subdirectories
	 *
	 * @param dir
	 * @param files
	 * @return
	 */
	public static List<File> getFiles(String dir, List<File> files) {
		if (dir.contains("*") || dir.contains("?")) {
			return getFilesWithMetas(dir, files);
		}
		File dirFile = new File(dir);
		if (dirFile.isDirectory()) {
			for (String single : dirFile.list())
				getFiles(dirFile.getAbsolutePath() + File.separator + single,
						files);
		} else if (dirFile.exists()) {
			files.add(dirFile);
		}
		return files;
	}

	/**
	 * Gets all the files from subdirectories
	 *
	 * @param dir
	 * @param files
	 * @return
	 */
	public List<File> getFiles(List<String> dirs, List<File> files) {
		for (String dir : dirs) {
			if (dir.contains("*") || dir.contains("?")) {
				return getFilesWithMetas(dir, files);
			}
			File dirFile = new File(dir);
			if (dirFile.isDirectory()) {
				for (String single : dirFile.list())
					getFiles(dirFile.getAbsolutePath() + File.separator
							+ single, files);
			} else if (dirFile.exists()) {
				files.add(dirFile);
			}
		}
		return files;
	}

	public static List<File> getFilesWithMetas(String dir, List<File> files) {
		String path = dir.substring(0, dir.lastIndexOf(File.separator));
		String[] found = new File(path).list();
		String fileName = dir.substring(dir.lastIndexOf(File.separator) + 1);
		fileName = fileName.replace(".", "\\.");
		if (fileName.contains("*"))
			fileName = fileName.replace("*", ".*");
		if (fileName.contains("?"))
			fileName = fileName.replace("?", ".{1}");
		System.out.println(fileName);
		for (String f : found) {
			System.out.println(f);
			if (Pattern.matches(fileName, f))
				files.add(new File(path + "\\" + f));
		}
		System.out.println(files.size());
		return files;
	}

	/**
	 * Gets files from subdirectories that match extension from config file
	 *
	 * @param dir
	 * @param files
	 * @return
	 */
	public List<File> getFilesWithExt(String dir, List<File> files) {
		return getFilesWithExt(dir, files, Config.getProp(Config.FORMAT));
	}

	/**
	 * Gets files from subdirectories that match extension
	 *
	 * @param dir
	 * @param files
	 * @return
	 */
	public List<File> getFilesWithExt(String dir, List<File> files,
			String extension) {
		File dirFile = new File(dir);
		if (dirFile.isDirectory())
		{
		   String [] dirList = dirFile.list();
		   if (dirList != null)
		   {
		      for (String single : dirList)
		      {
		         //System.out.println(single);
//		         if (single.equals("System Volume Information"))
//		         {
//		            System.out.println(single);
//		         }
		         getFilesWithExt(dirFile.getAbsolutePath() + File.separator
						+ single, files, extension);
		      }
		   }
		}
		else
		{
			if (dirFile.getPath().toLowerCase()
					.endsWith(extension.toLowerCase()))
			{
				files.add(dirFile);
			}
		}
		return files;
	}

	/**
	 * returns directory
	 *
	 * @param dir
	 * @return
	 */
	public File getDirectory(String dir) {
		File dirFile = new File(dir);
		if (dirFile.isDirectory()) {
			return dirFile;
		}
		else
		{
		   String parentDir = dirFile.getParent();
		   if (parentDir != null)
		   {
		      return new File(dirFile.getParent());
		   }
		   else
		   {
		      return new File(getCurrentDirectory());
		   }
		}
	}

	/*
	 *
	 */
   String getCurrentDirectory()
   {
      String cwd = null;

      try
      {
         cwd = new java.io.File(".").getCanonicalPath();
      }
      catch (java.io.IOException e)
      {
      }

      //String userDir = System.getProperty( "user.dir" );

      return cwd;
   }

   
    /** Makes filename, adds (n+1) to the name if needed
     * @param nameAndExtension - filename including file extension
     * @return new filename with extension (AbsolutePath)
     */
    public String makeName(String nameAndExtension){
    	String name = nameAndExtension.substring(0, nameAndExtension.lastIndexOf("."));
    	String extension = nameAndExtension.substring(nameAndExtension.lastIndexOf(".")+1, (nameAndExtension.length()));
    	
    	File f = checkIfNameNumbersNeeded(name, extension);
		
		log.debug("Made file:" + f.getAbsolutePath());
		return f.getAbsolutePath();
    	
    }
	/** Makes filename, adds (n+1) to the name if needed
	 * @param name - file extension
	 * @param newExtension - (target) file extension
	 * @return new filename with extension (AbsolutePath)
	 * @throws TempelPlusException
	 */
	public String makeName(String name, String newExtension) throws TempelPlusException {
		
		if (name.contains("."))
			name = name.substring(0, name.lastIndexOf("."));
		
		File f = checkIfNameNumbersNeeded(name, newExtension);
		
		log.debug("Made file:" + f.getAbsolutePath());
		return f.getAbsolutePath();
	}

	/** Returns new File if filename was inappropriate adds (n+1) to the name
	 * @param name
	 * @param extension - may be used "" or null
	 * @return
	 */
	private File checkIfNameNumbersNeeded(String name, String extension) {
		
		if(extension == "" || extension == null){
			extension = "";
		}else{
			extension = "." + extension;
		}

		File f = new File(name + extension);
		
				
		
		if (f.exists()) {
			int counter = 1;
			
			f = new File(name + "(" + counter +")" + extension);
			
			while(new File(name + "(" + counter +")" + extension).exists()){
				counter++;
				f = new File(name + "(" + counter +")" + extension);
			}
			//log.error("File already exists:" + f.getAbsolutePath());
			//exit(1);
		}
		return f;
	}
	
	
	

//	public void check(String name, String ext) throws TempelPlusException {
//		if (name.contains("."))
//			name = name.substring(0, name.lastIndexOf("."));
//		File f = new File(name + "." + ext);
//		if (f.exists()) {
//			if (f.isDirectory()) {
//				log.error("File already exists:" + f.getAbsolutePath());
//				exit(1);
//			} else {
//				log.error("File already exists:" + f.getAbsolutePath());
//				exit(1);
//			}
//		}
//	}
	

	public void check(String name, String ext) throws TempelPlusException {
		
		if (name.contains("."))
			name = name.substring(0, name.lastIndexOf("."));
		File f = new File(name + "." + ext);
		if (f.exists()) {
			if (f.isDirectory()) {
				log.error("File already exists:" + f.getAbsolutePath());
				exit(1);
			} else {
				log.error("File already exists:" + f.getAbsolutePath());
				exit(1);
			}
		}
	}

   public void check(String name, String ext, boolean Exists) throws TempelPlusException {
      if (name.contains("."))
         name = name.substring(0, name.lastIndexOf("."));
      File f = new File(name + "." + ext);
      if (Exists)
      {
         if (f.exists())
         {
            if (f.isDirectory())
            {
               log.error("File already exists:" + f.getAbsolutePath());
               exit(1);
            }
            else
            {
               log.error("File already exists:" + f.getAbsolutePath());
               exit(1);
            }
         }
      }
      else
      {
         if (!f.exists())
         {
            log.error("File does not exist: " + name + "." + ext);
            exit(1);
         }
      }
   }

   public static ArgsParams ConcatParams(String[] args, int Position)
   {
      String ReturnValue = "";
      ArgsParams RetVal = new ArgsParams();
      RetVal.i = Position;

      if (args.length > Position)
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

	public static boolean readConfig(String[] args) {
		if (args == null) {
			return true;
		}
		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-config"))
			{
			   if (i + 1 < args.length)
			   {
			      configFile = args[i + 1];

			      //System.out.println("try: " + configFile);

               ArgsParams params = ConcatParams(args, i + 1);
               configFile = params.Params;

               //System.out.println("try2: " + configFile);
               //i = params.i;
			   }
			}
		}
		if (configFile == null)
		{
		   Properties p = null;
         try
         {
            p = getEnvVars();
         }
         catch (Throwable e)
         {
            // TODO Auto-generated catch block
            //e.printStackTrace();
         }
		   System.out.println("the current value of WORKING_DIRECTORY is : " + p.getProperty("WORKING_DIRECTORY"));

			configFile = "TempelPlus.conf";
		}

		System.out.println("config file = " + configFile);

		try {
			Config.init(configFile); // loen sisse oma konfiguratsiooni
			Config.init(Config.getProps().getProperty(Config.JDOC_LOC)
					+ File.separator + "jdigidoc.cfg"); // //loen jdigidoci
														// konfiguratsiooni
			Config.init(configFile); // vajalik kuna mõni jdigidoci väärtus
										// võib olla üle kirjutanud minu konfi
			ConfigManager.init(Config.getProps().getProperty(Config.JDOC_LOC)
					+ File.separator + "jdigidoc.cfg");
			ConfigManager.init(configFile);// kirjutame üle
			// Sätime paika logimise
			// Appender app = new FileAppender(new
			// PatternLayout("%d{yyyy-MM-dd HH:mm:ss} [%c{1},%p] %M; %m%n"),
			// Config.getProp(Config.LOG_FILE));
			// Logger log = Logger.getLogger("*");
			// log.addAppender(app);
			PropertyConfigurator.configure(Config
					.getProp("DIGIDOC_LOG4J_CONFIG"));
			log = Logger.getLogger(TempelPlus.class);
		} catch (IOException e) {
			System.out.println("Reading configuration failed:");
			e.printStackTrace();
			return true;
		}
		return false;
	}

	public void askQuestion(String question) {
		if (Config.getProp(Config.CQUEST).equals("yes")) {
			log.info(question);
			//Console c = System.console();
			TextDevice c = Config.getTextDevice();//TextDevices.defaultTextDevice();
			String input = c.readLine();
			if (input == null || input.equalsIgnoreCase("N")) {
				exit(0);
			} else if (input.equalsIgnoreCase("Y")) {
				return;
			} else
				askQuestion(question);
		}
	}

	public static void exit(int level) {
		long end = System.currentTimeMillis() - start;
		long seconds = end / 1000;
		long minutes = 0;
		long hours = 0;
		if (seconds > 60) {
			minutes = seconds / 60;
			seconds = seconds % 60;
		}
		if (minutes > 60) {
			hours = minutes / 60;
			minutes = minutes % 60;
		}
		String time = "";
		if (hours != 0)
			time += hours + " hours ";
		if (minutes != 0)
			time += minutes + " minutes ";
		time += seconds + " seconds";
		if (log != null)
		   log.info("TempelPlus " + version + " stopping. Time used: " + time);
		System.exit(level);
	}

	public void setOutPut(String arg) {
		System.out.println(arg);
		if (outputFolder != null && !outputFolder.isDirectory()) {
			log.error("Output folder is not a directory!");
			printHelp();
			System.exit(1);
		} else if (outputFolder == null) {
			outputFolder = getDirectory(arg);
		}
	}

	protected abstract void printHelp();

	 public static Properties getEnvVars() throws Throwable
	 {
	    Process p = null;
	    Properties envVars = new Properties();
	    Runtime r = Runtime.getRuntime();
	    String OS = System.getProperty("os.name").toLowerCase();


	    if (OS.indexOf("windows 9") > -1)
	    {
	      p = r.exec( "command.com /c set" );
	      }
	    else if ( (OS.indexOf("nt") > -1)
	           || (OS.indexOf("windows 2000") > -1 )
	           || (OS.indexOf("windows xp") > -1)
	           || (OS.indexOf("windows 7") > -1) )
	    {

	      p = r.exec( "cmd.exe /c set" );
	    }
	    else
	    {
	      // our last hope, we assume Unix (thanks to H. Ware for the fix)
	      p = r.exec( "env" );
	    }
	    BufferedReader br = new BufferedReader
	       ( new InputStreamReader( p.getInputStream() ) );
	    String line;
	    while( (line = br.readLine()) != null )
	    {
	       int idx = line.indexOf( '=' );
	       String key = line.substring( 0, idx );
	       String value = line.substring( idx+1 );
	       envVars.setProperty( key, value );
	     // System.out.println( key + " = " + value );
	    }
	    return envVars;
	 }
	 
	
	 
	 
	 /**Function used during tempelplus actions, which require entering eToken pin
	  * 
	  * Pin insertion moment. If there is a tempelplus pin parameter inserted,
	  * pin from parameter will be used, else pin entering method prescribed in configuration file
	  * will be used
	 * @return pin code as String
	 * @throws Exception
	 */
	public String pinInsertion() throws Exception {
			String pin;
			log.info("");
			 if(commandParameterPin != null){
				 log.info("Using pin that was entered as TempelPlus commandline parameter");
				 pin = commandParameterPin;
			 }else{
				 pin = new PinReader().obtainPin();
			 }
			return pin;
	}
	
	
	/** Copies file
	 * @param inPut - source file location
	 * @param outPut - target file location
	 * @return true if success, false if not
	 */
	public boolean copyFile(File inPut, File outPut){
		try {
			
			copying(inPut, outPut);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	/** Moves file (copies the file to target location and deletes the original)
	 * @param inPut - source file location
	 * @param outPut - target file location
	 * @return
	 */
	public boolean moveFile(File inPut, File outPut){
		try {
			
			copying(inPut, outPut);
			System.gc();
			
			try {
				tryDeleting(inPut, 5, "", "");
			} catch (Exception e) {
				log.warn("Could not delete: " + inPut.getName());
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	/** Copying process used by copying or moving functions
	 * @param inPut - source file location
	 * @param outPut - target file location
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void copying(File inPut, File outPut) throws FileNotFoundException, IOException {
		InputStream in = new FileInputStream(inPut);
		OutputStream out = new FileOutputStream(outPut);
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
		   out.write(buf, 0, len);
		}
		in.close();
		out.close();
	}
	
	/** Tries to delete file, specified number of tries
	 * @param currentFile - file to delete
	 * @param attempts - number of retries
	 * @param successMessage - what to print if success
	 * @param failMessage - what to print if failed
	 * @return - true if deleted successfully, false is not
	 */
	public boolean tryDeleting(File currentFile, int attempts, String successMessage, String failMessage) {
		int deletionCounter = 1;

		while (currentFile.exists()) {
			currentFile.delete();

			if (deletionCounter >= attempts) {
				wait(1);
				log.fatal(failMessage);
				return false;
			}

			deletionCounter++;
		}
		log.info(successMessage);
		return true;
	}
	
	public void wait(int seconds){
		try {
			Thread.sleep(seconds*1000);
			log.info("...");
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	
	/** Verifies if this is a digidoc exception
	 * 
	 * @param e - exception
	 * @param ignoreFaults - if true, only error message is displayed, if false exit(1) is also executed
	 */
	public void verifyError(Exception e, String messageString, boolean ignoreFaults) {
		
		if(messageString != null){
		log.error(messageString);
		}
		
		if (e.getClass() == DigiDocException.class) {
			
			printDdocErrorInformation(e);
		}else {
			log.error("Unexpected error (non digidocexception).");
			log.error("Fault location: " + e.getStackTrace()[0]);
			log.error("Fault type:" + e.getClass().toString().replace("class", ""));
			if(e.getMessage() != null){
				log.error("Fault message: " + e.getMessage());
			}
		}
		
		if(!ignoreFaults){
			exit(1);
		}
		
	}

	/** In some cases TempelPlus informs user what had possibly caused a faulty situation
	 * @param e - Exception (DigiDocException)
	 */
	private void printDdocErrorInformation(Exception e) {
		int exceptionCode = ((DigiDocException) e).getCode();
		log.error("A Digidoc exception occured! Error code: " + exceptionCode);
		
		if (exceptionCode == DigiDocException.ERR_TOKEN_LOGIN) {
			log.error("Incorrect PIN or etoken ejected!");
		} else if(exceptionCode == DigiDocException.ERR_CRYPTO_DRIVER || 
				  exceptionCode == DigiDocException.ERR_PKCS11_INIT || 
				  exceptionCode == DigiDocException.ERR_SIGN){
			log.error("Communicating with etoken failed!");
			exit(1);
		}else if(exceptionCode == DigiDocException.ERR_OCSP_REQ_CREATE || 
				  exceptionCode == DigiDocException.ERR_OCSP_REQ_SEND ){
			log.error("Creating or sending OCSP request failed!");
		}
	}
}
