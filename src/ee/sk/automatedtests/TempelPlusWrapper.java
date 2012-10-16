package ee.sk.automatedtests;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.MimetypesFileTypeMap;

import ee.sk.digidoc.factory.DigiDocFactory;
import ee.sk.digidoc.DataFile;
import ee.sk.digidoc.DigiDocException;
import ee.sk.digidoc.SignedDoc;
import ee.sk.tempelPlus.util.Util;
import ee.sk.utils.ConfigManager;

/**
 * JDigiDoc methods
 * 
 * @author J�rgen Hannus, Erik Kaju
 */

public class TempelPlusWrapper {

	private static SignedDoc sdoc = null;
	private static BufferedReader stdout, stderr;
	static Vector<String> outputLines;

	String config_file, slash, tempelPlusPath, testDataPath, recipient, cert1, cert2, cert3, bits;

	boolean printChecksumData, windows;

	// Synchronize exceptions between main and sub-threads
	private volatile Exception exc = null;

	//private String tpVersion;

	public void main(String[] args) throws IOException {
	}

	/** Wrapper constructor - loads configuration from properties file **/
	public TempelPlusWrapper() {
		slash = System.getProperty("file.separator");
		
		if(slash.equalsIgnoreCase("\\")){
			windows = true;
		}else{
			windows = false;
		}
		testDataPath = new java.io.File("").getAbsolutePath() + slash + "TestData" + slash;
		String configFile = testDataPath +"config" + slash + "tempelplus-wrapper.properties";
		Log.write("Loading TempelPlusWrapper configuration from: " + configFile);
		Properties tpwConfigFile = new Properties();
		String tpwConfigPath = configFile;

		// Function to load variable values from config file
		try {
			tpwConfigFile.load(new FileInputStream(tpwConfigPath));
		} catch (Exception e) {
			Log.write("Configuration file: " + configFile + " not found or is unreachable.", "ERROR");
			throw new RuntimeException("Configuration file: " + configFile + " not found or is unreachable.");
		}

		config_file = tpwConfigFile.getProperty("JDDOC_CONFIG");
		tempelPlusPath = tpwConfigFile.getProperty("TEMPELPLUS_PATH");
		testDataPath = tpwConfigFile.getProperty("TESTDATA_PATH");
		recipient = Util.convertPropString("RECIPIENT", tpwConfigFile.getProperty("RECIPIENT"));
		//PIN = tpwConfigFile.getProperty("PIN");
		// delay = dbConfigFile.getProperty("HOST");
		cert1 = testDataPath + tpwConfigFile.getProperty("CERT1");
		cert2 = testDataPath + tpwConfigFile.getProperty("CERT2");
		cert3 = testDataPath + tpwConfigFile.getProperty("CERT3");
		
		bits = tpwConfigFile.getProperty("TEMPELPLUS_LINUX_BIT");

		printChecksumData = Boolean.valueOf(tpwConfigFile.getProperty("PRINT_CHECKSUMDATA"));
	}
	
	private boolean tempelPlusLines = false;

	/** Executes tempelplus with given parameters
	 * @param parameter - tempelplus parameters string
	 * @throws Exception
	 */
	public void runTempelPlus(String parameter[]) throws Exception {
		tempelplusExecution(parameter, null);
	}
	
	/** Executes tempelplus with given parameters (meant for executing tempelplus with -follow param)
	 * @param parameter - tempelplus parameters string
	 * @param dataFolder - testfolder 
	 * @throws Exception
	 */
	public void runTempelPlusListener(String parameter[], String dataFolder) throws Exception {
		tempelplusExecution(parameter, dataFolder);
	}
	
	

	/**
	 * <b> Execute TempelPlus </b>
	 * 
	 * @param parameter - String tempelplus parameters
	 * @param dataFolder - only for working in follow mode 
	 * @throws Exception
	 */
	private void tempelplusExecution(String parameter[], String dataFolder) throws Exception {
		
		// Variables used when tempelplus is executed in follow mode (testcases 38, 39)
		String listeningFolder = null,addedFilesFolder = null;
		File addedFile;
		String[] addedFiles = null;
		
		if(dataFolder != null){
			listeningFolder = testDataPath + dataFolder + slash;
			addedFilesFolder = testDataPath + dataFolder + "_2" + slash;
			addedFile = new File(addedFilesFolder);
			addedFiles = addedFile.list();
		}
		
		try {
			tempelPlusLines = false;
			final Runtime r;
			r = Runtime.getRuntime();
			Log.write("Executing arguments...");

			Process process;
			
			String[] executableStringArray;
			if(windows){
				executableStringArray = createExecutableStringArray(new String[]{"cmd.exe", "/c", "tempelplus"}, parameter);
			}else{
				executableStringArray = createExecutableStringArray(new String[]{"sh", "TempelPlus" + bits + ".sh"}, parameter);
			}
			
			
			String executableString = "";
			for(int i = 0; i < executableStringArray.length; i++){
				executableString = executableString + " " + executableStringArray[i];
			}
			Log.write(executableString, "Executing command");
			
			process = r.exec(executableStringArray, null, new File(tempelPlusPath));
			//process = r.exec(new String[]{"sh", "TempelPlus"+bits+".sh", "remove", "SK Arenduste test", "/home/erikaj/eclipse_workspaces/TempelPlus/JDigiDoc/TestData/TP-19/", "-output_folder", "/home/erikaj/eclipse_workspaces/TempelPlus/JDigiDoc/TestData/TP-19/output_folder/"}, null, new File(tempelPlusPath));
			//process = r.exec(executableString, null, new File(tempelPlusPath));

			Log.write("Arguments executed, about to read stdout...");
			InputStream is = process.getInputStream();

			stdout = new BufferedReader(new InputStreamReader(is));
			is = process.getErrorStream();
			stderr = new BufferedReader(new InputStreamReader(is));
			outputLines = new Vector<String>();

			// InputStream in = process.getInputStream();
			OutputStream out = process.getOutputStream();

			new BufferedWriter(new OutputStreamWriter(out));

			Thread readingOutput = executeReadTPLinesThread();
			
			if (dataFolder != null) {
				Thread.sleep(2500);
				for (int i = 0; i < addedFiles.length; i++) {
					copy(addedFilesFolder + addedFiles[i], listeningFolder + addedFiles[i]);
					Thread.sleep(1500);
					System.out.println("File copy has been performed: " + addedFilesFolder + addedFiles[i] + " >> " + listeningFolder + addedFiles[i]);
				}

				checkThreadException(readingOutput, addedFiles.length * 10000);
				process.destroy();
			}else{
				checkThreadException(readingOutput, null);
			}
			dataFolder = null;
			readingOutput.interrupt();
			
			executeReadErrorsThread();
			

			OutputStream os = process.getOutputStream();
			PrintStream ps2 = new PrintStream(os);
			Log.write("Sent input...");
			ps2.close();
			Log.write("Waiting for process...");
			Log.nextLine();
			if(dataFolder == null){
			process.waitFor();
			}
			process.destroy();
			Log.write("Done.");
		} catch (Exception e) {
			Log.write("Exception: " + e, true);
		}
	}

	private String[] createExecutableStringArray(String[] exec, String[] params) {
		
		String[] executableStringArray = new String[(exec.length + params.length)];
		
		for(int i = 0; i < exec.length; i++){
			executableStringArray[i] = exec[i];
		}
		
		for(int i = (executableStringArray.length-params.length); i < executableStringArray.length; i++){
			executableStringArray[i] = params[i-exec.length];
		}
		
		return executableStringArray;
	}

	private void executeReadErrorsThread() {
		// Error messages
		Thread stderrThread = new Thread() {
			public void run() {
				try {
					int l;
					String line;

					for (l = 0; (line = stderr.readLine()) != null;) {
						if (line.length() > 0)
							l++;
						// System.out.print(",");
						System.out.println(line);
					}

					Log.write("Read " + l + " lines of errors.");
					stderr.close();
				} catch (IOException ie) {
					Log.write("IO exception on stderr: " + ie);
				}
			}
		}; // End of error messages
		stderrThread.start();
	}

	private void checkThreadException(Thread thread, Integer secondsToKillThread) throws Exception {
		
		if(secondsToKillThread == null){
			thread.join();
		}else{
			Thread.sleep(secondsToKillThread);
			thread.interrupt();
		}
		
		if (exc != null) {
			throw new Exception(exc);
		}
		Log.write("No exceptions in read tempelplus output thread");
	}

	private Thread executeReadTPLinesThread() throws Exception {
		// Command line output
		Thread stdoutThread = new Thread("Read TempelPlus Lines Thread") {
			public void run() {
				try {
					int l;
					String line;
					for (l = 0; (line = stdout.readLine()) != null;) {

						if (line.length() > 0) {
							l++;
							// Populate outputLines
							outputLines.addElement(line);

						}

						try {
							verifyTempelPlusOutput(line);
						} catch (Exception e) {
							exc = e;
						}
					}
					Log.nextLine();
					Log.write("Read " + l + " lines of output.");
					stdout.close();
				} catch (IOException ie) {
					Log.write("IO exception on stdout: " + ie);
				}
			}
		}; // End of command line output
		stdoutThread.start();
		return stdoutThread;

	}

	private void verifyTempelPlusOutput(String line) throws Exception {
		if (line.contains("config file =")) {
			tempelPlusLines = true;
		}

		if (tempelPlusLines && line.length() > 1) {

			if (line.contains("File already exists:")) {
				Log.write("Tempelplus operation interfered by some unexpected file! \"File already exists\" message discovered.", true);
			}

			if (line.contains("NullPointerException")) {
				tempelPlusLines = false;
				Log.write("TempelPlus Crashed! There was a NullPointerException in output!", true);
			}
			
			if (line.contains("ERROR")) {
				tempelPlusLines = false;
				Log.write("TempelPlus execution Mistake! A string \"ERROR\" was discovered in in output: " + line, true);
			}

			if (line.contains("CertificateExpiredException")) {
				tempelPlusLines = false;
				Log.write("TempelPlus Crashed! There was a CertificateExpiredException in output, make sure your eToken certificates are valid!", true);
			}

			Log.write(line, "TempelPlus Execution");

			if (line.contains("TempelPlus v1.0.0 stopping.")) {
				tempelPlusLines = false;
			}
		}
	}

	/** Create a container from a single file **/
	void createNewContainer(String file, String containerPath) throws DigiDocException {
		ConfigManager.init(config_file);
		sdoc = new SignedDoc(SignedDoc.FORMAT_DIGIDOC_XML, SignedDoc.VERSION_1_3);
		File f = new File(file);
		File container = new File(containerPath);
		String mime = new MimetypesFileTypeMap().getContentType(f);
		// add file to container
		sdoc.addDataFile(f, mime, DataFile.CONTENT_EMBEDDED_BASE64);
		sdoc.writeToFile(container);
		// sdoc = digFac.readSignedDoc(file);
	}

	/** Create containers from a folder **/
	public void createContainerFromFolder(String file1, String file2, String file3, String containerPath) throws DigiDocException, IOException {
		ConfigManager.init(config_file);
		sdoc = new SignedDoc(SignedDoc.FORMAT_DIGIDOC_XML, SignedDoc.VERSION_1_3);
		File f1 = new File(file1);
		File f2 = new File(file2);
		File f3 = new File(file3);
		File container = new File(containerPath);
		String mime1 = new MimetypesFileTypeMap().getContentType(f1);
		String mime2 = new MimetypesFileTypeMap().getContentType(f2);
		String mime3 = new MimetypesFileTypeMap().getContentType(f3);
		// add file to container
		sdoc.addDataFile(f1, mime1, DataFile.CONTENT_EMBEDDED_BASE64);
		sdoc.addDataFile(f2, mime2, DataFile.CONTENT_EMBEDDED_BASE64);
		sdoc.addDataFile(f3, mime3, DataFile.CONTENT_EMBEDDED_BASE64);
		sdoc.writeToFile(container);
	}

	/** Extract one file from container **/
	public boolean containerRemoveFile(String container, String fileOut) throws DigiDocException, IOException {
		try {
			ConfigManager.init(config_file);
			sdoc = new SignedDoc(SignedDoc.FORMAT_DIGIDOC_XML, SignedDoc.VERSION_1_3);
			DigiDocFactory digFac = ConfigManager.instance().getDigiDocFactory();
			sdoc = digFac.readSignedDoc(container);
			FileOutputStream fos = new FileOutputStream(fileOut);
			byte[] xml = sdoc.getDataFile(0).getBodyAsData();
			fos.write(xml);
			fos.flush();
			fos.close();
			System.out.println("File extracted from container!");
		} catch (DigiDocException dde) {
			System.out.println("DigiDoc error: " + dde);
			return false;
		} catch (IOException ioe) {
			System.out.println("IO Error: " + ioe);
			return false;
		}
		return true;
	}

	/** Extract all files from container **/
	boolean containerRemoveFiles(String folder) {
		try {
			ConfigManager.init(config_file);
			sdoc = new SignedDoc(SignedDoc.FORMAT_DIGIDOC_XML, SignedDoc.VERSION_1_3);
			DigiDocFactory digFac = ConfigManager.instance().getDigiDocFactory();
			File filesFolder = new File(folder);
			String[] files = filesFolder.list();
			for (int i = 0; i < files.length; i++) {
				if (files[i].endsWith(".ddoc")) {
					sdoc = digFac.readSignedDoc(folder + files[i]);
					String fileName = files[i];
					// get file name without extension
					String fileNameExtensionless = sdoc.getDataFile(0).getFileName().substring(0, fileName.lastIndexOf("."));
					// extension of the file inside container
					String containerFileName = sdoc.getDataFile(0).getFileName();
					String newExtension = containerFileName.substring(fileName.lastIndexOf("."));
					// get file from container and give new name
					FileOutputStream fos = new FileOutputStream(folder + fileNameExtensionless + "_out" + newExtension);
					byte[] xml = sdoc.getDataFile(0).getBodyAsData();
					// taking file from container
					fos.write(xml);
					fos.flush();
					fos.close();
				}
			}
			System.out.println("Files extracted from container!");
		} catch (DigiDocException dde) {
			System.out.println("DigiDoc error: " + dde);
			return false;
		} catch (IOException ioe) {
			System.out.println("IO Error: " + ioe);
			return false;
		} catch (Exception e) {
			System.out.println("General error: " + e);
			return false;
		}
		return true;
	}

	/** Get all files from container **/
	boolean containerRemoveFiles(String folder, String addedFolder) {
		try {
			ConfigManager.init(config_file);
			sdoc = new SignedDoc(SignedDoc.FORMAT_DIGIDOC_XML, SignedDoc.VERSION_1_3);
			DigiDocFactory digFac = ConfigManager.instance().getDigiDocFactory();
			File filesFolder = new File(folder);
			String[] files = filesFolder.list();
			File addedFilesFolder = new File(addedFolder);
			String[] addedFiles = addedFilesFolder.list();
			for (int i = 0; i < files.length; i++) {
				if (files[i].endsWith(".ddoc")) {
					for (int j = 0; j <= addedFiles.length; j++) {
						sdoc = digFac.readSignedDoc(folder + files[i]);
						// container name
						// get file name without extension
						String fileName = sdoc.getDataFile(j).getFileName();
						String fileNameNoExt = fileName.substring(0, fileName.lastIndexOf("."));
						String oldExtension = fileName.substring(fileName.lastIndexOf("."), fileName.length());
						// get file from container and give new name
						FileOutputStream fos = new FileOutputStream(folder + fileNameNoExt + "_out" + oldExtension);
						byte[] xml = sdoc.getDataFile(j).getBodyAsData();
						// extract file from container
						fos.write(xml);
						fos.flush();
						fos.close();
					}
				}
			}
			System.out.println("Files extracted from container!");
		} catch (DigiDocException dde) {
			System.out.println("DigiDoc error: " + dde);
			return false;
		} catch (IOException ioe) {
			System.out.println("IO Error: " + ioe);
			return false;
		} catch (Exception e) {
			System.out.println("General error: " + e);
			return false;
		}
		return true;
	}

	/** Validate a single container via JDigiDoc **/
	public boolean validateContainer(String container) throws DigiDocException {
		ConfigManager.init(config_file);

		// initates output of getDigiDocFactory()
		DigiDocFactory digiFac = ConfigManager.instance().getDigiDocFactory();
		sdoc = digiFac.readSignedDoc(container);

		// if no errors occur via JDigiDoc, return true
		ArrayList<?> errs = sdoc.validate(true);
		if (errs.size() == 0)
			return true;
		else
			for (int j = 0; j < errs.size(); j++)
				System.out.println("\t\t" + (DigiDocException) errs.get(j));
		return false;
	}

	/** Validate containers in a folder via JDigiDoc **/
	boolean validateFolder(String folder) throws DigiDocException {
		ConfigManager.init(config_file);

		// initates output of getDigiDocFactory()
		DigiDocFactory digiFac = ConfigManager.instance().getDigiDocFactory();

		File filesFolder = new File(folder);
		String[] files = filesFolder.list();

		for (int i = 0; i <= files.length - 1; i++) {
			if (files[i].endsWith(".ddoc")) {
				sdoc = digiFac.readSignedDoc(folder + files[i]);
				// if no errors occur via JDigiDoc
				ArrayList<?> errs = sdoc.validate(true);
				if (errs.size() == 0)
					return true;
				else
					for (int j = 0; j < errs.size(); j++)
						System.out.println("\t\t" + (DigiDocException) errs.get(j));
				return false;
			}
		}
		return false;
	}

	public boolean isTempelPlusVerified = false;

	/** Initiate verify via TempelPlus **/
	public boolean tempelPlusVerify() {
		for (int i = 0; i < outputLines.size(); i++) {
			Pattern pattern = Pattern.compile("\\d documents verified successfully");
			Matcher matcher = pattern.matcher(outputLines.get(i));
			if (matcher.find() == true)
				isTempelPlusVerified = true;
		}
		return isTempelPlusVerified;
	}

	private boolean isTempelPlusUnsigned = false;

	/** Initiate verify via TempelPlus, match 0 valid signatures **/
	boolean tempelPlusUnsigned() {
		for (int i = 0; i < outputLines.size(); i++) {
			Pattern pattern = Pattern.compile("TempelPlus found 0 valid signatures and \\d invalid signatures");
			Matcher matcher = pattern.matcher(outputLines.get(i));
			if (matcher.find() == true)
				isTempelPlusUnsigned = true;
		}
		return isTempelPlusUnsigned;
	}

	public boolean isTempelPlusHelp = false;

	/** Initiate -help via TempelPlus **/
	boolean tempelPlusHelp() {
		for (int i = 0; i < outputLines.size(); i++) {
			Pattern pattern = Pattern.compile("sign|verify|encrypt");
			Matcher matcher = pattern.matcher(outputLines.get(i));
			if (matcher.find() == true)
				isTempelPlusHelp = true;
		}
		return isTempelPlusHelp;
	}

	private boolean isBrokenCertificate = false;

	/** Check whether the output of signing with a false certificate is true **/
	boolean brokenCertificateVerify() {
		for (int i = 0; i < outputLines.size(); i++) {
			Pattern pattern = Pattern.compile("Encryption of the files failed!");
			Matcher matcher = pattern.matcher(outputLines.get(i));
			if (matcher.find() == true) {
				isBrokenCertificate = true;
				Log.write("Encrypting with broken certificate failed as expected!", "SUCCESS");
			}
		}
		return isBrokenCertificate;
	}

	private boolean isNotValidDecrypt = false;

	/** Check whether the output of decrypting file when no rights fails **/
	boolean invalidDecrypt() { // NO_UCD
		for (int i = 0; i < outputLines.size(); i++) {
			Pattern pattern = Pattern.compile("Decryption of the files failed!");
			Matcher matcher = pattern.matcher(outputLines.get(i));
			if (matcher.find() == true) {
				isNotValidDecrypt = true;
				Log.write("Decrypting with broken certificate failed as expected!", "SUCCESS");
			}
		}
		return isNotValidDecrypt;
	}

	private boolean isTempelPlusSignHelp = false;

	/** Initiate sign -help via TempelPlus **/
	boolean tempelPlusSignHelp() {
		for (int i = 0; i < outputLines.size(); i++) {
			Pattern pattern = Pattern.compile("role|countr|-output_folder");
			Matcher matcher = pattern.matcher(outputLines.get(i));
			if (matcher.find() == true)
				isTempelPlusSignHelp = true;
		}
		return isTempelPlusSignHelp;
	}

	// private boolean isFalsePin = false;
	//
	// /** Initiate TempelPlus with incorrect PIN **/
	// boolean falsePin() {
	// for (int i = 0; i < outputLines.size(); i++) {
	// Pattern pattern = Pattern.compile("Incorrect PIN!");
	// Matcher matcher = pattern.matcher(outputLines.get(i));
	// if (matcher.find() == true)
	// isFalsePin = true;
	// }
	// return isFalsePin;
	// }

	/** Compare checksum of both files **/
	public boolean checksumMatch(String fileIn, String fileOut) throws IOException, NoSuchAlgorithmException {
		String checksumBefore = generateChecksum(fileIn);
		String checksumAfter = generateChecksum(fileOut);
		if (checksumBefore.equals(checksumAfter))
			return true;
		else
			return false;
	}

	/** Generate file checksum **/
	private String generateChecksum(String fileIn) throws IOException, NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA1");
		FileInputStream fis = new FileInputStream(fileIn);
		byte[] dataBytes = new byte[1024];

		int nread = 0;
		while ((nread = fis.read(dataBytes)) != -1) {
			md.update(dataBytes, 0, nread);
		}
		;

		byte[] mdbytes = md.digest();

		// convert the byte to hex format
		StringBuffer buffer = new StringBuffer("");
		for (int i = 0; i < mdbytes.length; i++) {
			buffer.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
		}
		return buffer.toString();
	}

	boolean checksumFolder(String folder) throws IOException, NoSuchAlgorithmException {
		ArrayList<String> checksumBefore = new ArrayList<String>();
		ArrayList<String> checksumAfter = new ArrayList<String>();
		File filesFolder = new File(folder);
		String[] files = filesFolder.list();

		for (int i = 0; i < files.length; i++) {
			Pattern pattern = Pattern.compile("_out");
			Matcher matcher = pattern.matcher(files[i]);
			Pattern pattern2 = Pattern.compile("ddoc");
			Matcher matcher2 = pattern2.matcher(files[i]);
			// if outputfile
			if (matcher.find()) {
				MessageDigest md = MessageDigest.getInstance("SHA1");
				FileInputStream fis = new FileInputStream(folder + files[i]);
				byte[] dataBytes = new byte[1024];
				int nread = 0;
				while ((nread = fis.read(dataBytes)) != -1) {
					md.update(dataBytes, 0, nread);
				}
				;
				byte[] mdbytes = md.digest();
				// convert the byte to hex format
				StringBuffer buffer = new StringBuffer("");
				for (int j = 0; j < mdbytes.length; j++) {
					buffer.append(Integer.toString((mdbytes[j] & 0xff) + 0x100, 16).substring(1));
				}
				// add checksum to checksumAfter arraylist
				checksumAfter.add(buffer.toString());
				continue;
				// anything else besides ddoc
			} else if (matcher2.find())
				continue;
			// if input file
			else {
				MessageDigest md = MessageDigest.getInstance("SHA1");
				FileInputStream fis = new FileInputStream(folder + files[i]);
				byte[] dataBytes = new byte[1024];
				int nread = 0;
				while ((nread = fis.read(dataBytes)) != -1) {
					md.update(dataBytes, 0, nread);
				}
				;
				byte[] mdbytes = md.digest();
				// convert the byte to hex format
				StringBuffer buffer = new StringBuffer("");
				for (int k = 0; k < mdbytes.length; k++) {
					buffer.append(Integer.toString((mdbytes[k] & 0xff) + 0x100, 16).substring(1));
				}
				// add checksum to checksumBefore arraylist
				checksumBefore.add(buffer.toString());
			}
		}
		boolean isMatch = true;

		/*
		 * Iterator<String> cBitr = checksumBefore.iterator(); Iterator<String>
		 * cAitr = checksumAfter.iterator(); while (cBitr.hasNext() &&
		 * cAitr.hasNext()) { Object in = cBitr.next(); Object out =
		 * cAitr.next(); if (in.equals(out)) {
		 * System.out.println("Checksum before: " + in + ", checksum after: " +
		 * out + " => MATCH"); } else { System.out.println("Checksum before: " +
		 * in + " , checksum after: " + out + " => ERROR"); isMatch = false; } }
		 */
		return isMatch;
	}

	boolean checksumFolder(String[] inputFiles, String[] outputFiles) throws IOException, NoSuchAlgorithmException {
		ArrayList<String> checksumBefore = new ArrayList<String>();
		ArrayList<String> checksumAfter = new ArrayList<String>();
		for (int i = 0; i < inputFiles.length; i++) {
			MessageDigest md = MessageDigest.getInstance("SHA1");
			FileInputStream fis = new FileInputStream(inputFiles[i]);
			byte[] dataBytes = new byte[1024];
			int nread = 0;
			while ((nread = fis.read(dataBytes)) != -1) {
				md.update(dataBytes, 0, nread);
			}
			;
			byte[] mdbytes = md.digest();
			// convert the byte to hex format
			StringBuffer buffer = new StringBuffer("");
			for (int k = 0; k < mdbytes.length; k++) {
				buffer.append(Integer.toString((mdbytes[k] & 0xff) + 0x100, 16).substring(1));
			}
			// add checksum to checksumBefore arraylist
			checksumBefore.add(buffer.toString());
		}
		for (int i = 0; i < outputFiles.length; i++) {
			MessageDigest md = MessageDigest.getInstance("SHA1");
			FileInputStream fis = new FileInputStream(outputFiles[i]);
			byte[] dataBytes = new byte[1024];
			int nread = 0;
			while ((nread = fis.read(dataBytes)) != -1) {
				md.update(dataBytes, 0, nread);
			}
			;
			byte[] mdbytes = md.digest();
			// convert the byte to hex format
			StringBuffer buffer = new StringBuffer("");
			for (int j = 0; j < mdbytes.length; j++) {
				buffer.append(Integer.toString((mdbytes[j] & 0xff) + 0x100, 16).substring(1));
			}
			// add checksum to checksumAfter arraylist
			checksumAfter.add(buffer.toString());
		}
		boolean isMatch = false;
		if (checksumBefore.equals(checksumAfter))
			isMatch = true;

		/*
		 * Iterator<String> cBitr = checksumBefore.iterator(); Iterator<String>
		 * cAitr = checksumAfter.iterator(); while (cBitr.hasNext() &&
		 * cAitr.hasNext()) { Object in = cBitr.next(); Object out =
		 * cAitr.next(); if (in.equals(out)) {
		 * System.out.println("Checksum before: " + in + ", checksum after: " +
		 * out + " => MATCH"); } else { System.out.println("Checksum before: " +
		 * in + " , checksum after: " + out + " => ERROR"); isMatch = false; } }
		 */

		return isMatch;
	}

	/** Delete file 
	 * @throws IOException **/
	public boolean deleteFile(String sFilePath) throws IOException {
		File oFile = new File(sFilePath);

		if (oFile.isDirectory()) {
			File[] aFiles = oFile.listFiles();
			for (File oFileCur : aFiles) {
				deleteFile(oFileCur.getAbsolutePath());
			}
		}
		Log.write("Trying to delete: " + oFile, "Debug");
		
//		Writer out = new OutputStreamWriter(new FileOutputStream(sFilePath));
//		
//		out.flush();
//        out.close();
//        out = null;
        System.gc();
        
		return oFile.delete();
	}

	/** Get file from folder **/
	public String getInputFile(String folder) {
		File filesFolder = new File(folder);
		String[] files = filesFolder.list();
		String fileIn = null;
		for (int i = 0; i < files.length; i++) {
			if (new File(folder + files[i]).isDirectory() && !new File(folder + files[i]).getName().contains(".cdoc"))
				continue;
			else
				fileIn = folder + files[i];
		}
		return fileIn;
	}

	/** Store file names in an array - cdoc files not included 
	 * @throws Exception **/
	String[] getInputFiles(String folder) throws Exception {
		return getFiles(folder, false);
	}

	/** Store file names in an array 
	 * @throws Exception **/
	String[] getAllFiles(String folder) throws Exception {
		return getFiles(folder, true);
	}

	/** Store file names in an array 
	 * @throws Exception **/
	private String[] getFiles(String folder, boolean includeCdoc) throws Exception {
		File filesFolder = new File(folder);
		String[] files = filesFolder.list();
		ArrayList<String> aL = new ArrayList<String>();
		try {
			for (int i = 0; i < files.length; i++) {
				if (!new File(folder + files[i]).isDirectory()) {
					// Ugly fast solution
					if (includeCdoc) {
						String curVal = files[i];
						String newVal = folder + curVal;
						aL.add(newVal);
					} else {
						if (!new File(folder + files[i]).getName().contains("cdoc")) {
							String curVal = files[i];
							String newVal = folder + curVal;
							aL.add(newVal);
						}
					}
				} else
					continue;
			}
		} catch (Exception e) {
			//e.printStackTrace();
			Log.write("Could not read testdata. Problematic destination: " + folder, true);
		}
		String[] returnFiles = new String[aL.size()];
		for (int i = 0; i < aL.size(); i++) {
			returnFiles[i] = aL.get(i);
		}
		return returnFiles;
	}

	/** Store encrypted file names in an array **/
	ArrayList<String> getEncryptedFiles(String folder) {
		File filesFolder = new File(folder);
		String[] files = filesFolder.list();
		String curVal = null;
		ArrayList<String> arFiles = new ArrayList<String>();
		for (int i = 0; i < files.length; i++) {
			curVal = files[i];
			if (curVal.matches(".cdoc")) {
				arFiles.add(folder + files[i]);
			}
		}
		return arFiles;
	}

	/** Get outputFile name from inputFile **/
	public String getOutputFile(String inputFile) {

		// Get extension of the file
		String ext = inputFile.substring(inputFile.lastIndexOf("."), inputFile.length());
		// Get filename
		String fileInNoExt = inputFile.substring(0, inputFile.lastIndexOf("."));

		// filename + "_out" + extension
		String fileOut = fileInNoExt + "_out" + ext;

		return fileOut;
	}

	/** Get outputFile names from inputFiles **/
	String[] getOutputFiles(String[] inputFiles) {
		String[] outputFiles = new String[inputFiles.length];
		for (int i = 0; i < inputFiles.length; i++) {
			String curValue = inputFiles[i];
			System.out.println("curValue: " + curValue);
			String ext = curValue.substring(curValue.lastIndexOf("."), curValue.length());
			String fileInNoExt = curValue.substring(0, curValue.lastIndexOf("."));
			String fileOut = fileInNoExt + "_out" + ext;
			outputFiles[i] = fileOut;
		}
		return outputFiles;
	}

	/** Get container name from inputFile **/
	public String getContainer(String inputFile) {
		String container = getNameOnly(inputFile) + ".ddoc";
		return container;
	}
	
	/** Get filename only **/
	public String getNameOnly(String inputFile){
		String fileInNoExt = inputFile.substring(0, inputFile.lastIndexOf("."));
		return fileInNoExt;
	}

	/** Get encrypted container name from inputFile **/
	String getEncryptedContainer(String inputFile) {
		String fileInNoExt = inputFile.substring(0, inputFile.lastIndexOf("."));
		String container = fileInNoExt + ".cdoc";

		return container;
	}

	/** Check whether output_folder exist, if not then create **/
	void outputFolderExist(String outputFolder) {
		boolean exists = (new File(outputFolder)).exists();
		if (!exists)
			new File(outputFolder).mkdir();
	}

	void copy(String fromFileName, String toFileName) throws IOException {
		File fromFile = new File(fromFileName);
		File toFile = new File(toFileName);
		if (!fromFile.exists())
			throw new IOException("FileCopy: " + "no such source file: " + fromFileName);
		if (!fromFile.isFile())
			throw new IOException("FileCopy: " + "can't copy directory: " + fromFileName);
		if (!fromFile.canRead())
			throw new IOException("FileCopy: " + "source file is unreadable: " + fromFileName);
		if (toFile.isDirectory())
			toFile = new File(toFile, fromFile.getName());
		if (toFile.exists()) {
			if (!toFile.canWrite())
				throw new IOException("FileCopy: " + "destination file is unwriteable: " + toFileName);
			System.out.print("Overwrite existing file " + toFile.getName() + "? (Y/N): ");
			System.out.flush();
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			String response = in.readLine();
			if (!response.equals("Y") && !response.equals("y"))
				throw new IOException("FileCopy: " + "existing file was not overwritten.");
		} else {
			String parent = toFile.getParent();
			if (parent == null)
				parent = System.getProperty("user.dir");
			File dir = new File(parent);
			if (!dir.exists())
				throw new IOException("FileCopy: " + "destination directory doesn't exist: " + parent);
			if (dir.isFile())
				throw new IOException("FileCopy: " + "destination is not a directory: " + parent);
			if (!dir.canWrite())
				throw new IOException("FileCopy: " + "destination directory is unwriteable: " + parent);
		}
		FileInputStream from = null;
		FileOutputStream to = null;
		try {
			from = new FileInputStream(fromFile);
			to = new FileOutputStream(toFile);
			byte[] buffer = new byte[4096];
			int bytesRead;

			while ((bytesRead = from.read(buffer)) != -1)
				to.write(buffer, 0, bytesRead); // write
		} finally {
			if (from != null)
				try {
					from.close();
				} catch (IOException e) {
					System.out.println("Error: " + e);
				}
			if (to != null)
				try {
					to.close();
				} catch (IOException e) {
					System.out.println("Error: " + e);
				}
		}
	}

	/**
	 * Returns true if <b>fileIn</b> and <b>fileOut</b> checksums match and
	 * <b>container</b> is valid
	 **/
	public boolean verifyChecksums(String fileIn, String fileOut, String container) throws IOException, NoSuchAlgorithmException, DigiDocException {
		boolean result = false;

		if (checksumMatch(fileIn, fileOut) == true && validateContainer(container) == true) {
			result = true;
			Log.write("Checksums match, created container is valid!", "Success");
		} else if (checksumMatch(fileIn, fileOut) == true && validateContainer(container) == false) {
			Log.write("Checksums match, created container is not valid!", "ERROR");
		} else if (checksumMatch(fileIn, fileOut) == false && validateContainer(container) == true) {
			Log.write("Checksums don't match, created container is valid!", "ERROR");
		} else {
			Log.write("Checksums don't match, created container is not valid!", "ERROR");
		}

		return result;
	}

	/** Returns true if <b>fileIn</b> and <b>fileOut</b> checksums match **/
	public boolean verifyInOutFilesChecksum(String fileIn, String outputFile) throws IOException, NoSuchAlgorithmException {

		if (checksumMatch(fileIn, outputFile) == true) {
			System.out.println("Checksums match!");
			return true;
		} else {
			return false;
		}
	}

	/** Returns true if digitempel is valid and jDigidoc is valid **/
	public boolean verifyValidity(String fileIn) throws DigiDocException {

		boolean isDigiTempelValid = tempelPlusVerify();
		boolean isJDigiDocValid = validateContainer(fileIn);
		boolean result = false;

		if (isDigiTempelValid == true && isJDigiDocValid == true) {
			Log.write("DigiTempel verify passed, JDigiDoc validation passed!", "SUCCESS");
			result = true;
		} else if (isDigiTempelValid == true && isJDigiDocValid == false) {
			Log.write("DigiTempel verify passed, JDigiDoc validation failed!", "ERROR");
		} else if (isDigiTempelValid == false && isJDigiDocValid == true) {
			Log.write("DigiTempel verify failed, JDigiDoc validation passed!", "ERROR");
		} else {
			Log.write("Both DigiTempel and JDigiDoc verifing failed", "ERROR");
		}
		return result;
	}

	/** Returns true if folder validation and folder checksumming succeeds **/
	public boolean verifyFolder(String folder) throws NoSuchAlgorithmException, DigiDocException, IOException {
		boolean result = false;

		if (validateFolder(folder) == true && checksumFolder(folder) == true) {
			Log.write("Created containers are valid, checksums match!", "SUCCESS");
			result = true;
		} else if (validateFolder(folder) == true && checksumFolder(folder) == false) {
			Log.write("Created containers are valid, checksums don't match!", "ERROR");
		} else if (validateFolder(folder) == false && checksumFolder(folder) == true) {
			Log.write("Created containers are not valid, checksums match!", "ERROR");
		} else {
			Log.write("Created containers are not valid, also checksums do not match!", "ERROR");
		}
		return result;
	}

	public boolean verifyChecksumsAndValidate(String outputFolder, String[] outputFiles, String[] inputFilesWAdded) throws IOException, NoSuchAlgorithmException, DigiDocException {
		boolean result = false;

		if (validateFolder(outputFolder) == true && checksumFolder(inputFilesWAdded, outputFiles) == true) {
			result = true;
			Log.write("Checksums match, containers are valid", "SUCCESS");
		} else if (checksumFolder(inputFilesWAdded, outputFiles) == true && validateFolder(outputFolder) == false)
			Log.write("Checksums match, containers are valid!", "ERROR");
		else if (checksumFolder(inputFilesWAdded, outputFiles) == false && validateFolder(outputFolder) == true)
			Log.write("Checksums do not match, containers are valid!", "ERROR");
		else {
			Log.write("Checksums do not match, containers are valid!", "ERROR");
		}
		return result;
	}

	public boolean checkSumFolderInputOutput(String[] inputFiles, String[] outputFiles) throws IOException, NoSuchAlgorithmException {
		if (checksumFolder(inputFiles, outputFiles)) {
			Log.write("Checksums match!", "SUCCESS");
			return true;
		} else {
			Log.write("Checksums don't match!", "ERROR");
			return false;
		}
	}

	public void cleanFolder(String folder, String[] filesContaining, int numberFilesToRemain) throws Exception {
		cleanFolder(folder, filesContaining);

		if (getAllFiles(folder).length != numberFilesToRemain) {
			Log.write("Unexpected number of files left in TestData folder (" + folder + ") after clearing! Expected: " + numberFilesToRemain + ", remained: " + getInputFiles(folder).length, true);
		}
	}

	public void cleanFolder(String folder, String[] filesContaining) throws Exception {

		String[] allFolderFiles = getAllFiles(folder);

		Log.write("Cleaning testData folder before/after execution: " + folder);

		int cleared = 0;
		for (int i = 0; i < allFolderFiles.length; i++) {

			allFolderFiles[i] = allFolderFiles[i].replace(folder, "");

			for (int j = 0; j < filesContaining.length; j++) {

				if (allFolderFiles[i].contains(filesContaining[j])) {

					if (!deleteFile(folder + allFolderFiles[i])) {
						Log.write("Could not clean TestData folder: " + folder + ". Problematic file: " + allFolderFiles[i] + " Make sure this file is accessible and is not in use by any other program.", true);
					} else {
						cleared++;
					}
				}
			}
		}

		if (cleared > 0) {
			Log.write("Cleared " + cleared + " files");
		} else {
			Log.write("Nothing to delete");
		}
	}	
}