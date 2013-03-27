package ee.sk.automatedtests;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;
/**
 * 
 * TempelPlus tests
 * 
 * 
 * @author Jï¿½rgen Hannus, Erik Kaju
 */

public class TempelPlusWrapperTest {
	
	TempelPlusWrapper tempelPlusWrapper = new TempelPlusWrapper();

	String slash = tempelPlusWrapper.slash;
	String testDataPath = tempelPlusWrapper.testDataPath;
	String recipient = tempelPlusWrapper.recipient;
	String cn = tempelPlusWrapper.cn;
	String cert1 = tempelPlusWrapper.cert1;
	String cert2 = tempelPlusWrapper.cert2;
	String cert3 = tempelPlusWrapper.cert3;
	
	public boolean TP1_1, TP1_2, TP9, TP10_14_13, TP11, TP12, TP15, TP16 , TP17, 
	TP18, TP19, TP20, TP21, TP22, TP23, TP24, TP25, TP26_28, TP27_29, TP30, TP36, 
	TP37, TP38, TP39, TP40, TP41, TP42, TP43, TP44, TP45, TP97, TP98, TP99 = false;
	
	@Before
	public void before(){
		System.out.println("Starting test suite");
		Log.write("Starting..", "New test");
		System.gc();
		System.out.println("Java free memory: " + Runtime.getRuntime().freeMemory());
	}
	
	/*
	 * TP-1_1: Check whether TempelPlus -help command is giving expected output.
	 */
	@Test
	public final void testTP1_1() throws Exception {
		tempelPlusWrapper.runTempelPlus(new String[]{"-help"});
		boolean isHelp = (tempelPlusWrapper.tempelPlusHelp());
		assertTrue(isHelp);
	}

	/*
	 * TP-1_2: Check whether TempelPlus sign -help command is giving expected
	 * output.
	 */
	@Test
	public final void testTP1_2()  throws Exception {
		tempelPlusWrapper.runTempelPlus(new String[]{"sign", "-help"});
		boolean isSignHelp = (tempelPlusWrapper.tempelPlusSignHelp());
		assertTrue(isSignHelp);
	}
	
//	/*
//	 * TP-30: Encrypt file via TempelPlus, decrypt files via TempelPlus (with
//	 * invalid recipient).
//	 * 
//	 * Return true if output matches "Decryption of the files failed!"
//	 */
//	@Test
//	public final void testTP30() throws Exception  {
//		String folder = testDataPath + "TP-30" + slash;
//		
//		tempelPlusWrapper.cleanFolder(folder, new String[]{".cdoc"}, 1);
//		
//		String fileIn = tempelPlusWrapper.getInputFile(folder);
//		String container = tempelPlusWrapper.getEncryptedContainer(fileIn);
//		tempelPlusWrapper.runTempelPlus(new String[]{"encrypt", fileIn, "-cert", cert3});
//		
//		tempelPlusWrapper.runTempelPlus(new String[]{"decrypt", container, "-recipient", recipient});
//		TP30 = tempelPlusWrapper.invalidDecrypt();
//		assertTrue(TP30);
//		tempelPlusWrapper.deleteFile(container);
//	}

	/*
	 * TP-9: Calculate checksum of input file, sign input file via TempelPlus,
	 * validate container via JDigiDoc, extract file from container via
	 * JDigiDoc, calculate checksum of output file, compare checksums.
	 * 
	 * Return true if container is valid and checksums match.
	 */
	@Test
	public void testTP9() throws Exception {
		
		String folder = testDataPath + "TP-9" + slash;
		
		tempelPlusWrapper.cleanFolder(folder, new String[]{"_out", ".ddoc"}, 1);
		
		String fileIn = tempelPlusWrapper.getInputFile(folder);
		String fileOut = tempelPlusWrapper.getOutputFile(fileIn);
		String container = tempelPlusWrapper.getContainer(fileIn);
		
		tempelPlusWrapper.runTempelPlus(new String[]{"sign", fileIn});
		
		tempelPlusWrapper.containerRemoveFile(container, fileOut);
	
		TP9 = tempelPlusWrapper.verifyChecksums(fileIn, fileOut, container);
		
		assertTrue(TP9);
		
		tempelPlusWrapper.deleteFile(container);
		tempelPlusWrapper.deleteFile(fileOut);
	}

	/*
	 * TP-10 && TP-14 && TP-13: Calculate checksum of input files (different
	 * file types and file sizes), sign input files via TempelPlus, validate
	 * containers via JDigiDoc, extract input files from containers via
	 * JDigiDoc, calculate checksums of output files, compare checksums.
	 * 
	 * Return true if containers are valid and checksums match.
	 */
	@Test
	public final void testTP10_14_13()  throws Exception {
		
		String folder = testDataPath + "TP-10_14_13" + slash;
		
		tempelPlusWrapper.cleanFolder(folder, new String[]{"_out", ".ddoc"}, 2);
		
		tempelPlusWrapper.runTempelPlus(new String[]{"sign", folder});
		tempelPlusWrapper.containerRemoveFiles(folder);
		TP10_14_13 = tempelPlusWrapper.verifyFolder(folder);
		
		assertTrue(TP10_14_13);
		
		tempelPlusWrapper.cleanFolder(folder, new String[]{"_out", ".ddoc"}, 2);
	}

	/*
	 * TP-11: Calculate checksum of input file, sign input file via TempelPlus
	 * with additional parameters, validate container via JDigiDoc, extract file
	 * from container via JDigiDoc, calculate checksum of output file, compare
	 * checksums.
	 * 
	 * Return true if container is valid and checksums match.
	 */
	@Test
	public final void testTP11() throws Exception {
		String folder = testDataPath + "TP-11" + slash;
		
		tempelPlusWrapper.cleanFolder(folder, new String[]{"_out", ".ddoc"}, 1);
		
		String fileIn = tempelPlusWrapper.getInputFile(folder);
		String fileOut = tempelPlusWrapper.getOutputFile(fileIn);
		String container = tempelPlusWrapper.getContainer(fileIn);
		
		
		tempelPlusWrapper.runTempelPlus(new String[]{"sign", fileIn, "-role", "knowittest", "-country", "Eesti", "-state", "Harjumaa", "-city", "Tallinn", "-postcode", "12345"});
		tempelPlusWrapper.containerRemoveFile(container, fileOut);

		TP11 = tempelPlusWrapper.verifyChecksums(fileIn, fileOut, container);
		assertTrue(TP11);
		tempelPlusWrapper.deleteFile(fileOut);
		tempelPlusWrapper.deleteFile(container);
	}

	/*
	 * TP-12: Calculate checksum of large input file, sign large input file via
	 * TempelPlus with additional parameters, validate container via JDigiDoc,
	 * extract file from container via JDigiDoc, calculate checksum of output
	 * file, compare checksums.
	 * 
	 * Return true if container is valid and checksums match.
	 */
	@Test
	public final void testTP12() throws Exception {
		String folder = testDataPath + "TP-12" + slash;
		tempelPlusWrapper.cleanFolder(folder, new String[]{"_out", ".ddoc"}, 1);
		
		String fileIn = tempelPlusWrapper.getInputFile(folder);
		String fileOut = tempelPlusWrapper.getOutputFile(fileIn);
		String container = tempelPlusWrapper.getContainer(fileIn);
		tempelPlusWrapper.runTempelPlus(new String[]{"sign", fileIn, "-role", "SKtest", "-country", "Eesti", "-state", "Harjumaa", "-city", "Tallinn", "-postcode", "54321"});
		tempelPlusWrapper.containerRemoveFile(container, fileOut);
		TP12 = tempelPlusWrapper.verifyChecksums(fileIn, fileOut, container);
		assertTrue(TP12);
		tempelPlusWrapper.deleteFile(container);
		tempelPlusWrapper.deleteFile(fileOut);
	}

	/*
	 * TP-15: Validate signed container via JDigiDoc, verify signed container
	 * via TempelPlus.
	 * 
	 * Return true if JDigiDoc validation doesn't return errors and TempelPlus
	 * returns "documents verified successfully".
	 */
	@Test
	public final void testTP15() throws Exception {
		String folder = testDataPath + "TP-15" + slash;
		String fileIn = tempelPlusWrapper.getInputFile(folder);
		tempelPlusWrapper.runTempelPlus(new String[]{"verify", fileIn});
		
		TP15 = tempelPlusWrapper.verifyValidity(fileIn);
		
		assertTrue(TP15);
	}

	/*
	 * TP-16: Validate signed containers via JDigiDoc, verify signed containers
	 * via TempelPlus.
	 * 
	 * Return true if JDigiDoc validation doesn't return errors and TempelPlus
	 * returns "documents verified successfully".
	 */
	@Test
	public final void testTP16() throws Exception {
		String folder = testDataPath + "TP-16" + slash;
		tempelPlusWrapper.runTempelPlus(new String[]{"verify", folder});
		boolean isDigiTempelValid = tempelPlusWrapper.tempelPlusVerify();
		System.out.println(isDigiTempelValid);
		boolean isJDigiDocValid = tempelPlusWrapper.validateFolder(folder);
		if (isDigiTempelValid == true && isJDigiDocValid == false)
			System.out.println("DigiTempel verify passed, "
					+ "JDigiDoc validation failed!");
		else if (isDigiTempelValid == false && isJDigiDocValid == true)
			System.out.println("DigiTempel verify failed, "
					+ "JDigiDoc validation passed!");
		else if (isDigiTempelValid == true && isJDigiDocValid == true) {
			System.out.println("DigiTempel verify passed, "
					+ "JDigiDoc validation passed!");
			TP16 = true;
		} else {
			System.out.println("DigiTempel and JDigiDoc verifing failed");
			TP16 = false;
		}
		assertTrue(TP16);
	}

	/*
	 * TP-17: Sign input file via TempelPlus, remove signature via TempelPlus,
	 * verify output file.
	 * 
	 * Return true if
	 * "TempelPlus found 0 valid signatures and x invalid signatures".
	 */
	@Test
	public final void testTP17()  throws Exception {
		String folder = testDataPath + "TP-17" + slash;
		String outputFolder = folder + "output_folder" + slash;
		tempelPlusWrapper.outputFolderExist(outputFolder);
		
		tempelPlusWrapper.cleanFolder(folder, new String[]{".ddoc"});
		tempelPlusWrapper.cleanFolder(outputFolder, new String[]{".ddoc"}, 0);
		
		String fileIn = tempelPlusWrapper.getInputFile(folder);
		tempelPlusWrapper.runTempelPlus(new String[]{"sign", fileIn});
		
		String container = tempelPlusWrapper.getContainer(fileIn);
		tempelPlusWrapper.runTempelPlus(new String[]{"remove", "ALL", container, "-output_folder", outputFolder});
		String fileOut = tempelPlusWrapper.getInputFile(outputFolder);
		tempelPlusWrapper.runTempelPlus(new String[]{"verify", fileOut});
		TP17 = tempelPlusWrapper.tempelPlusUnsigned();
		assertTrue(TP17);
		tempelPlusWrapper.deleteFile(container);
		tempelPlusWrapper.deleteFile(outputFolder);
	}

	/*
	 * TP-18: Sign input files via TempelPlus, remove signatures via TempelPlus,
	 * verify output files.
	 * 
	 * Return true if
	 * "TempelPlus found 0 valid signatures and x invalid signatures".
	 */
	@Test
	public final void testTP18()  throws Exception {
		String folder = testDataPath + "TP-18" + slash;
		String outputFolder = folder + "output_folder" + slash;
		tempelPlusWrapper.outputFolderExist(outputFolder);
		
		tempelPlusWrapper.cleanFolder(folder, new String[]{".ddoc"});
		tempelPlusWrapper.cleanFolder(outputFolder, new String[]{".ddoc"}, 0);
		
		tempelPlusWrapper.runTempelPlus(new String[]{"sign", folder});
		tempelPlusWrapper.runTempelPlus(new String[]{"remove", "ALL", folder, "-output_folder", outputFolder});
		tempelPlusWrapper.runTempelPlus(new String[]{"verify", outputFolder});
		TP18 = tempelPlusWrapper.tempelPlusUnsigned();
		assertTrue(TP18);
		// delete signed containers
		File filesFolder = new File(folder);
		String[] files = filesFolder.list();
		for (int i = 0; i < files.length; i++) {
			if (files[i].endsWith(".ddoc"))
				tempelPlusWrapper.deleteFile(folder + files[i]);
			else if (files[i].contains("out"))
				tempelPlusWrapper.deleteFile(folder + files[i]);
		}
		// delete output files
		tempelPlusWrapper.deleteFile(outputFolder);
	}

	/*
	 * TP-19: Sign input files via TempelPlus, remove specific signatures via
	 * TempelPlus, verify output files.
	 * 
	 * Return true if
	 * "TempelPlus found 0 valid signatures and x invalid signatures".
	 */
	@Test
	public final void testTP19()  throws Exception {
		String folder = testDataPath + "TP-19" + slash;
		String outputFolder = folder + "output_folder" + slash;
		tempelPlusWrapper.outputFolderExist(outputFolder);
		
		tempelPlusWrapper.cleanFolder(folder, new String[]{".ddoc"});
		tempelPlusWrapper.cleanFolder(outputFolder, new String[]{".ddoc"}, 0);
		
		tempelPlusWrapper.runTempelPlus(new String[]{"sign", folder});
		tempelPlusWrapper.runTempelPlus(new String[]{"remove", recipient, folder, "-output_folder", outputFolder});
		
		File filesFolder = new File(folder);
		String[] files = filesFolder.list();
		for (int i = 0; i < TempelPlusWrapper.outputLines.size(); i++) {
			Pattern pattern = Pattern.compile("\\d documents were handled successfully. [1-999] "
					+ "signatures removed");
			Matcher matcher = pattern.matcher(TempelPlusWrapper.outputLines
					.get(i));
			if (matcher.find() == true)
				TP19 = true;
		}
		assertTrue(TP19);
		
		for (int l = 0; l < files.length; l++) {
			if (files[l].endsWith(".ddoc"))
				tempelPlusWrapper.deleteFile(folder + files[l]);
			else if (files[l].contains("_out"))
				tempelPlusWrapper.deleteFile(folder + files[l]);
		}
		tempelPlusWrapper.deleteFile(outputFolder);
	}

	/*
	 * TP-20: Calculate checksum of input file, create container from input file
	 * via JDigiDoc, validate container via JDigiDoc, extract input file from
	 * container via TempelPlus, calculate checksum of output file, compare
	 * checksums.
	 * 
	 * Return true if container is valid and checksums match.
	 */
	@Test
	public final void testTP20() throws Exception {
		String folder = testDataPath + "TP-20" + slash;
		String outputFolder = folder + "output_folder" + slash;
		
		tempelPlusWrapper.cleanFolder(folder, new String[]{".ddoc"});
		tempelPlusWrapper.deleteFile(outputFolder);
		
		tempelPlusWrapper.outputFolderExist(outputFolder);
		String fileIn = tempelPlusWrapper.getInputFile(folder);
		String container = tempelPlusWrapper.getContainer(fileIn);
		File fileName = new File(fileIn);
		String noExtFName = fileName.getName();
		String newFileName = noExtFName.substring(0, noExtFName.lastIndexOf("."));
		newFileName = newFileName.concat(".ddoc" + slash);
		String fileOut = outputFolder + newFileName + fileName.getName();
		tempelPlusWrapper.createNewContainer(fileIn, container);
		// if folder doesn't exist
		tempelPlusWrapper.outputFolderExist(outputFolder);
		tempelPlusWrapper.runTempelPlus(new String[]{"extract", container, "-output_folder", outputFolder});

		TP20 = tempelPlusWrapper.verifyChecksums(fileIn, fileOut, container);
		assertTrue(TP20);
		tempelPlusWrapper.deleteFile(container);
		tempelPlusWrapper.deleteFile(outputFolder);
	}

	/*
	 * TP-21: Calculate checksums of input files, create containers from input
	 * files via JDigiDoc, extract files from containers via TempelPlus,
	 * calculate checksums of output files, compare checksums.
	 * 
	 * Return true if containers are valid and checksums match.
	 */
	@Test
	public final void testTP21() throws Exception {
		
		String folder = testDataPath + "TP-21" + slash;
		tempelPlusWrapper.cleanFolder(folder, new String[]{".ddoc", "_out"});
		String outputFolder = folder + "output_folder" + slash;
		String[] inputFiles = tempelPlusWrapper.getInputFiles(folder);
		String[] outputFiles = tempelPlusWrapper.getOutputFiles(inputFiles);
		String[] containers = new String[inputFiles.length];
		tempelPlusWrapper.deleteFile(outputFolder);
		
		
		tempelPlusWrapper.outputFolderExist(outputFolder);
		
		for (int i = 0; i < inputFiles.length; i++) {
			String curValue = tempelPlusWrapper.getContainer(inputFiles[i]);
			containers[i] = curValue;
		}
		for (int i = 0; i < inputFiles.length; i++) {
			tempelPlusWrapper.createNewContainer(inputFiles[i], containers[i]);
		}
		tempelPlusWrapper.runTempelPlus(new String[]{"extract", folder, "-output_folder", outputFolder});
		
		for (int l = 0; l < inputFiles.length; l++) {
			File f = new File(inputFiles[l]);
			String noExtFName = f.getName();
			String newFileName = noExtFName.substring(0,
					noExtFName.lastIndexOf("."));
			newFileName = newFileName.concat(".ddoc" + slash);
			String result = outputFolder + newFileName + f.getName();
			outputFiles[l] = result;
		}
		
		if (tempelPlusWrapper.checksumFolder(inputFiles, outputFiles) == false)
			System.out.println("Checksums don't match!");
		else if (tempelPlusWrapper.checksumFolder(inputFiles, outputFiles) == true) {
			System.out.println("Checksums match!");
			TP21 = true;
		}
		assertTrue(TP21);
		
		for (int i = 0; i < containers.length; i++) {
			System.out.println("Deleting: " + containers[i]);
			tempelPlusWrapper.deleteFile(containers[i]);
		}
		tempelPlusWrapper.deleteFile(outputFolder);
		
	}

	/*
	 * TP-22: Calculate checksum of input file, create container from input file
	 * via TempelPlus, validate container via JDigiDoc, extract output file via
	 * JDigiDoc, calculate checksum of output file, compare checksums.
	 * 
	 * Return true if container is valid and checksums match.
	 */
	@Test
	public final void testTP22() throws Exception {
		String folder = testDataPath + "TP-22" + slash;
		
		tempelPlusWrapper.cleanFolder(folder, new String[]{".ddoc", "_out"});
		
		String fileIn = tempelPlusWrapper.getInputFile(folder);
		String fileOut = tempelPlusWrapper.getOutputFile(fileIn);
		String container = tempelPlusWrapper.getContainer(fileIn);
		tempelPlusWrapper.runTempelPlus(new String[]{"container", fileIn});
		tempelPlusWrapper.containerRemoveFile(container, fileOut);
		
		TP22 = tempelPlusWrapper.verifyChecksums(fileIn, fileOut, container);

		assertTrue(TP22);
		tempelPlusWrapper.deleteFile(container);
		tempelPlusWrapper.deleteFile(fileOut);
	}

	/*
	 * TP-23: Calculate checksums of input files, create containers via
	 * TempelPlus, validate containers via JDigiDoc, extract input files via
	 * JDigiDoc, calculate checksums of output files, compare checksums.
	 * 
	 * Return true if containers are valid and checksums match.
	 */
	@Test
	public final void testTP23() throws Exception {
		String folder = testDataPath + "TP-23" + slash;
		tempelPlusWrapper.cleanFolder(folder, new String[]{".ddoc", "_out"});
		tempelPlusWrapper.runTempelPlus(new String[]{"container", folder});
		tempelPlusWrapper.containerRemoveFiles(folder);

		TP23 = tempelPlusWrapper.verifyFolder(folder);
		assertTrue(TP23);
		tempelPlusWrapper.cleanFolder(folder, new String[]{".ddoc", "_out"});
	}

	/*
	 * TP-24: Calculate checksums of input files (also added files), create
	 * containers of input files via TempelPlus, validate containers via
	 * JDigiDoc, extract files via JDigiDoc, calculate checksums of output
	 * files, compare checksums.
	 * 
	 * 
	 * Return true if containers are valid and checksums match.
	 */
	@Test
	public final void testTP24() throws Exception {
		String folder = testDataPath + "TP-24" + slash;
		String outputFolder = testDataPath + "TP-24_3" + slash;
		tempelPlusWrapper.outputFolderExist(outputFolder);
		
		tempelPlusWrapper.cleanFolder(outputFolder, new String[]{".ddoc", "_out"}, 0);
		
		String addedFilesFolder = testDataPath + "TP-24_2" + slash;
		String[] inputFiles = tempelPlusWrapper.getInputFiles(folder);
		String[] addedFiles = tempelPlusWrapper.getInputFiles(addedFilesFolder);
		
		// Containering files from TP24 folder and adding TP24_2 to them?!!?
		tempelPlusWrapper.runTempelPlus(new String[]{ "container", folder, "-output_folder", outputFolder, "-add_file", addedFiles[0], addedFiles[1]});
		tempelPlusWrapper.containerRemoveFiles(outputFolder, addedFilesFolder);
		// don't count the folders
		int inputFilesCount = inputFiles.length;
		int addedFilesCount = addedFiles.length;
		String[] outputFiles = new String[inputFilesCount + addedFilesCount];
		for (int i = 0; i < inputFiles.length; i++) {
			String curVal = inputFiles[i];
			File f = new File(curVal);
			String fileName = f.getName();
			String noExt = fileName.substring(0, fileName.lastIndexOf("."));
			String ext = fileName.substring(fileName.lastIndexOf("."),
					fileName.length());
			String newFileName = (outputFolder + noExt + "_out" + ext);
			outputFiles[i] = newFileName;
		}
		for (int i = (outputFiles.length - 1); i <= outputFiles.length; i++) {
			String curVal = addedFiles[i - (outputFiles.length - 1)];
			File f = new File(curVal);
			String fileName = f.getName();
			String noExt = fileName.substring(0, fileName.lastIndexOf("."));
			String ext = fileName.substring(fileName.lastIndexOf("."),
					fileName.length());
			String newFileName = (outputFolder + noExt + "_out" + ext);
			outputFiles[i - 1] = newFileName;
		}
		String[] inputFilesWAdded = new String[inputFilesCount
				+ addedFilesCount];
		System.out.println("inputFiles.length: " + inputFiles.length);
		System.out.println("inputFilesWAdded.length: "
				+ inputFilesWAdded.length);
		for (int i = 0; i < (inputFilesWAdded.length); i++) {
			if (i < inputFilesCount)
				inputFilesWAdded[i] = inputFiles[i];
			else {
				String curVal = addedFiles[i - (inputFiles.length)];
				inputFilesWAdded[i] = curVal;
			}
		}
		
		for (int i = 0; i < outputFiles.length; i++){
			System.out.println("outpuFiles[i]: " + outputFiles[i]);
		}
		for (int i = 0; i < inputFilesWAdded.length; i++){
			System.out.println("inputFilesWAdded[i]: " + inputFilesWAdded[i]);
		}
		
		TP24 = tempelPlusWrapper.verifyChecksumsAndValidate(outputFolder, outputFiles, inputFilesWAdded);
		
		assertTrue(TP24);
		
		File filesFolder = new File(outputFolder);
		String[] files = filesFolder.list();
		Pattern pattern = Pattern.compile("(out)*(ddoc)*");
		for (int i = 0; i < files.length; i++) {
			Matcher matcher = pattern.matcher(files[i]);
			if (matcher.find() == true) {
				tempelPlusWrapper.deleteFile(outputFolder + files[i]);
			}
		}
	}

	/*
	 * TP-25: Calculate checksums of input files and added files folder, create
	 * containers from input files via TempelPlus, validate containers via
	 * JDigiDoc, extract input files via JDigiDoc, calculate checksums of output
	 * files, compare checksums.
	 * 
	 * Return true if checksums match and containers are valid.
	 */
	@Test
	public final void testTP25() throws Exception {
		String folder = testDataPath + "TP-25" + slash;
		String outputFolder = testDataPath + "TP-25_3" + slash;
		tempelPlusWrapper.outputFolderExist(outputFolder);
		tempelPlusWrapper.cleanFolder(outputFolder, new String[]{".ddoc", "_out"}, 0);
		
		String addedFilesFolder = testDataPath + "TP-25_2" + slash;
		tempelPlusWrapper.runTempelPlus(new String[]{"container", folder, "-output_folder", outputFolder, "-add_file", addedFilesFolder});
		tempelPlusWrapper.containerRemoveFiles(outputFolder, addedFilesFolder);
		String[] inputFiles = tempelPlusWrapper.getInputFiles(folder);
		String[] addedFiles = tempelPlusWrapper.getInputFiles(addedFilesFolder);
		// don't count the folders
		int inputFilesCount = inputFiles.length;
		int addedFilesCount = addedFiles.length;
		String[] outputFiles = new String[inputFilesCount + addedFilesCount];
		for (int i = 0; i < inputFiles.length; i++) {
			String curVal = inputFiles[i];
			File f = new File(curVal);
			String fileName = f.getName();
			String noExt = fileName.substring(0, fileName.lastIndexOf("."));
			String ext = fileName.substring(fileName.lastIndexOf("."),
					fileName.length());
			String newFileName = (outputFolder + noExt + "_out" + ext);
			outputFiles[i] = newFileName;
		}
		for (int i = (outputFiles.length - 1); i <= outputFiles.length; i++) {
			String curVal = addedFiles[i - (outputFiles.length - 1)];
			File f = new File(curVal);
			String fileName = f.getName();
			String noExt = fileName.substring(0, fileName.lastIndexOf("."));
			String ext = fileName.substring(fileName.lastIndexOf("."),
					fileName.length());
			String newFileName = (outputFolder + noExt + "_out" + ext);
			outputFiles[i - 1] = newFileName;
		}
		String[] inputFilesWAdded = new String[inputFilesCount
				+ addedFilesCount];
		System.out.println("inputFiles.length: " + inputFiles.length);
		System.out.println("inputFilesWAdded.length: "
				+ inputFilesWAdded.length);
		for (int i = 0; i < (inputFilesWAdded.length); i++) {
			if (i < inputFilesCount)
				inputFilesWAdded[i] = inputFiles[i];
			else {
				String curVal = addedFiles[i - (inputFiles.length)];
				inputFilesWAdded[i] = curVal;
			}
		}
		for (int i = 0; i < outputFiles.length; i++)
			Log.write("outpuFiles[i]: " + outputFiles[i]);
		for (int i = 0; i < inputFilesWAdded.length; i++)
			Log.write("inputFilesWAdded[i]: " + inputFilesWAdded[i]);
		
		TP25 = tempelPlusWrapper.verifyChecksumsAndValidate(outputFolder, outputFiles, inputFilesWAdded);
		assertTrue(TP25);
		
		File filesFolder = new File(outputFolder);
		String[] files = filesFolder.list();
		Pattern pattern = Pattern.compile("(out)*(ddoc)*");
		for (int i = 0; i < files.length; i++) {
			Matcher matcher = pattern.matcher(files[i]);
			if (matcher.find() == true) {
				tempelPlusWrapper.deleteFile(outputFolder + files[i]);
			}
		}
	}

	/*
	 * TP-26_28: Calculate checksum of input file, encrypt / decrypt file via
	 * TempelPlus, encrypt / decrypt file via TempelPlus, calculate output file
	 * checksum, compare checksums.
	 * 
	 * Return true if checksums match.
	 */
	@Test
	public final void testTP26_28() throws Exception {
		String folder = testDataPath + "TP-26_28" + slash;
		
		tempelPlusWrapper.cleanFolder(folder, new String[]{".cdoc"}, 1);
		
		String fileIn = tempelPlusWrapper.getInputFile(folder);
		
		tempelPlusWrapper.deleteFile(fileIn.split("[.]")[0]+".ddoc");
		
		String container = tempelPlusWrapper.getEncryptedContainer(fileIn);
		String contFolder = fileIn.split("[.]")[0] + ".cdoc(1)"; // expected output folder is with .cdoc ending
		
		tempelPlusWrapper.deleteFile(contFolder);
		
		File fileName = new File(fileIn);
		String outputFile = contFolder + "" + slash + fileName.getName();
		System.out.println("outputFile" + outputFile);
		tempelPlusWrapper.runTempelPlus(new String[]{"encrypt", fileIn, "-cert", cert1});
		tempelPlusWrapper.runTempelPlus(new String[]{"decrypt", container, "-recipient", recipient});
		
		TP26_28 = tempelPlusWrapper.verifyInOutFilesChecksum(fileIn, outputFile);
		
		
		assertTrue(TP26_28);
		
		tempelPlusWrapper.deleteFile(fileIn.split("[.]")[0]+".ddoc");
		tempelPlusWrapper.deleteFile(contFolder);
		tempelPlusWrapper.cleanFolder(folder, new String[]{".cdoc"}, 1);
	}

	/*
	 * TP-27_29: Calculate checksum of input files, encrypt files via
	 * TempelPlus, decrypt files via TempelPlus by extracting the decrypted files directly to output folder, 
	 * calculate output files checksums, compare checksums.
	 * 
	 * Return true if checksums match.
	 */
	@Test
	public final void testTP27_29() throws Exception {
		String folder = testDataPath + "TP-27_29" + slash;
		String outputFolder = folder + "output_folder" + slash;
		
		tempelPlusWrapper.cleanFolder(folder, new String[]{".cdoc"});
		tempelPlusWrapper.deleteFile(outputFolder);
		
		// input files
		String[] inputFiles = tempelPlusWrapper.getInputFiles(folder);
		int filesCount = inputFiles.length;
		// output files
		String[] outputFiles = new String[filesCount];
		
		for (int l = 0; l < inputFiles.length; l++) {
			File f = new File(inputFiles[l]);
			String noExtFName = f.getName();
			String newFileName = noExtFName.substring(0,
					noExtFName.lastIndexOf("."));
			newFileName = newFileName.concat(slash);
			String result = outputFolder + /*newFileName +*/ f.getName();
			outputFiles[l] = result;
		}
		tempelPlusWrapper.runTempelPlus(new String[]{"encrypt", folder, "-cert", cert1, cert3});
		tempelPlusWrapper.outputFolderExist(outputFolder);
		tempelPlusWrapper.runTempelPlus(new String[]{"decrypt", folder, "-output_folder", outputFolder, "-recipient", recipient, "-cmn_ext_dir"}); // files are extracted directly to output folder
		
		TP27_29 = tempelPlusWrapper.checkSumFolderInputOutput(inputFiles, outputFiles);
		
		
		assertTrue(TP27_29);
		
		tempelPlusWrapper.cleanFolder(folder, new String[]{".cdoc"});
		tempelPlusWrapper.deleteFile(outputFolder);
	}


	/*
	 * TP-36: Sign file via TempelPlus, insert false PIN.
	 * 
	 * Return true if output matches "Incorrect PIN!".
	 */
//	@Test
//	public final void testTP36() {
//		String folder = testDataPath + "TP-36" + slash;
//		String fileIn = tempelPlusWrapper.getInputFile(folder);
//		tempelPlusWrapper.TempelPlusFalsePin("sign " + fileIn);
//		TP36 = tempelPlusWrapper.falsePin();
//		assertTrue(TP36);
//	}

	/*
	 * TP-38: Calculate checksum on input files, copy files from
	 * addedFilesFolder to listeningFolder, wait for TempelPlus to sign
	 * addedFiles, validate containers via JDigiDoc, extract addedFiles from
	 * containers, calculate checksums.
	 * 
	 * Return true if checksums match and containers are valid.
	 */
	@Test
	public final void testTP38() throws Exception {
		
		final String listeningFolder = testDataPath + "TP-38" + slash;
		String addedFilesFolder = testDataPath + "TP-38_2" + slash;
		final String outputFolder = testDataPath + "TP-38_3" + slash;
		File addedFile = new File(addedFilesFolder);
		String[] addedFiles = addedFile.list();
		Arrays.sort(addedFiles);
		
		tempelPlusWrapper.cleanFolder(listeningFolder, addedFiles, 0);
		tempelPlusWrapper.cleanFolder(outputFolder, new String[]{".ddoc", "_out"}, 0);
		
		
		for (int i = 0; i < addedFiles.length; i++) {
			String newVal = addedFilesFolder + addedFiles[i];
			addedFiles[i] = newVal;
		}
		
		Thread listener = new Thread() {
			public void run() {
				try {
					tempelPlusWrapper.runTempelPlusListener(new String[]{"sign", listeningFolder, "-follow" ,"-remove_input", "-output_folder", outputFolder}, "TP-38");
					//tempelPlusWrapper.runTempelPlus("sign " + listeningFolder + " -follow -remove_input -output_folder " + outputFolder);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		listener.run();
		Thread.sleep(3500);
		//listener.interrupt();
		tempelPlusWrapper.containerRemoveFiles(outputFolder);
		
		Thread.sleep(3000);
		Pattern pattern = Pattern.compile("(out)++");
		File outputFilesFolder = new File(outputFolder);
		String[] outputFiles = outputFilesFolder.list();
		ArrayList<String> aL = new ArrayList<String>();
		
		for (int i = 0; i < outputFiles.length; i++) {
			Matcher matcher = pattern.matcher(outputFiles[i]);
			if (matcher.find() == true) {
				aL.add(outputFolder + outputFiles[i]);
			}
		}
		String[] outFiles = new String[addedFiles.length];
		for (int i = 0; i < aL.size(); i++) {
			outFiles[i] = aL.get(i);
			System.out.println("outFiles[" + i + "]: " + outFiles[i]);
		}
		for (int i = 0; i < addedFiles.length; i++)
			System.out.println("addedFiles[" + i + "]:" + addedFiles[i]);
		
			if (tempelPlusWrapper.checksumFolder(addedFiles, outFiles) == true 	&& tempelPlusWrapper.validateFolder(outputFolder) == false){
				System.out.println("Checksums match, containers are not valid!");
			}else if (tempelPlusWrapper.checksumFolder(addedFiles, outFiles) == false && tempelPlusWrapper.validateFolder(outputFolder) == true){
				System.out.println("Checksums don't match, containers are valid!");
			}else if (tempelPlusWrapper.validateFolder(outputFolder) == true && tempelPlusWrapper.checksumFolder(addedFiles, outFiles) == true){
				System.out.println("Checksums match, containers are valid!");
				TP38 = true;
			}
		System.out.println(TP38);
		assertTrue(TP38);
		
		tempelPlusWrapper.cleanFolder(listeningFolder, addedFiles, 0);
		tempelPlusWrapper.cleanFolder(outputFolder, new String[]{".ddoc", "_out"}, 0);		
	}

	/*
	 * TP-39: Calculate checksum on input files, copy files from
	 * addedFilesFolder to listeningFolder, wait for TempelPlus to encrypt
	 * addedFiles, extract addedFiles from containers, calculate checksums.
	 * 
	 * Return true if checksums match and containers are valid.
	 */
	/*@Test
	public final void testTP39() throws Exception {
		String listeningFolder = testDataPath + "TP-39" + slash;
		String addedFilesFolder = testDataPath + "TP-39_2" + slash;
		String outputFolder = testDataPath + "TP-39_3" + slash;
		File addedFile = new File(addedFilesFolder);
		String[] addedFiles = addedFile.list();
		Arrays.sort(addedFiles);
		
		tempelPlusWrapper.cleanFolder(listeningFolder, addedFiles);
		tempelPlusWrapper.cleanFolder(listeningFolder, new String[]{".cdoc"}, 0);

		tempelPlusWrapper.deleteFile(outputFolder);
		new File(outputFolder).mkdir();
		
		System.out.println("ListeningFolder: " + listeningFolder);
		System.out.println("added files folder: " + addedFilesFolder);
		System.out.println("outPutfolder: " + outputFolder);
		System.out.println("Added file: " + addedFile);
		System.out.println("addedFiles: " + addedFiles);
		
		for (int i = 0; i < addedFiles.length; i++) {
			String newVal = addedFilesFolder + addedFiles[i];
			addedFiles[i] = newVal;
		}
		tempelPlusWrapper.runTempelPlusListener(new String[]{"encrypt", addedFilesFolder, "-cert", cert1, cert3, "-output_folder", listeningFolder}, "TP-39");
		Thread.sleep(1500);
		tempelPlusWrapper.runTempelPlus(new String[]{"decrypt", listeningFolder, "-recipient", recipient,"-follow", "-remove_input", "-output_folder", outputFolder});
		Thread.sleep(3000);
		Pattern.compile("(out)++");
		File outputFilesFolder = new File(outputFolder);
		String[] outputFiles = outputFilesFolder.list();
		// ArrayList<String> al = new ArrayList<String>();
		for (int l = 0; l < outputFiles.length; l++) {
			File f = new File(addedFilesFolder + addedFiles[l]);
			String noExtFName = f.getName();
			String newFileName = noExtFName.substring(0,
					noExtFName.lastIndexOf("."));
			
			String result = outputFolder + newFileName + slash + f.getName();
			outputFiles[l] = result;
		}
		if (tempelPlusWrapper.checksumFolder(addedFiles, outputFiles) == false)
			System.out.println("Checksums don't match!");
		else if (tempelPlusWrapper.checksumFolder(addedFiles, outputFiles) == true) {
			System.out.println("Checksums match!");
			TP39 = true;
		}
		assertTrue(TP39);
		// clean up
		for (int i = 0; i < outputFiles.length; i++) {
			System.out.println("Deleting file: " + outputFiles[i]);
			tempelPlusWrapper.deleteFile(outputFiles[i]);
		}
		File listeningFile = new File(listeningFolder);
		String[] listenedFiles = listeningFile.list();
		for (int i = 0; i < listenedFiles.length; i++) {
			System.out.println("Deleting file: " + listeningFolder
					+ listenedFiles[i]);
			tempelPlusWrapper.deleteFile(listeningFolder + listenedFiles[i]);
		}
		
		tempelPlusWrapper.deleteFile(outputFolder);
		new File(outputFolder).mkdir();
		
	}*/
	
	
	// NEW TESTS for TP v 1.2.0+
	/*
	 * TP-40: Verification with extra -cn argument, correct CN
	 * 
	 * Pass if verification uses -cn argument and validation works
	 */
	@Test
	public final void testTP40() throws Exception  {
		String folder = testDataPath + "TP-40_41" + slash;
		String fileIn = tempelPlusWrapper.getInputFile(folder);
		tempelPlusWrapper.runTempelPlus(new String[]{"verify", fileIn, "-cn", cn});
		
		if(tempelPlusWrapper.verifyValidity(fileIn)
		   && tempelPlusWrapper.verifyOutPut("Verifying file \\d of \\d")
		   && tempelPlusWrapper.verifyOutPut("OK")
		   && tempelPlusWrapper.verifyOutPutIsNot(", but was:")
		   ){
			TP40 = true;
		}
		assertTrue(TP40);
	}
	
	/*
	 * TP-41: Verification with extra -cn argument, incorrect CN
	 * 
	 * Pass if verification uses -cn argument with wrong value and validation does not succeed
	 */
	@Test
	public final void testTP41() throws Exception  {
		String folder = testDataPath + "TP-40_41" + slash;
		String fileIn = tempelPlusWrapper.getInputFile(folder);
		tempelPlusWrapper.runTempelPlus(new String[]{"verify", fileIn, "-cn", "\"WRONG RECIPIENT\""});
		
		if(tempelPlusWrapper.verifyOutPut("Verifying file \\d of \\d")
		   && tempelPlusWrapper.verifyOutPut("Verification unsuccessful.")
		   ){
			TP41 = true;
		}
		assertTrue(TP41);
	}
	
	/*
	 * TP-42: Verification with extra -verify argument, correct CN
	 * 
	 * Pass if extraction uses -verify argument with wrong value and validation succeeds
	 */
	@Test
	public final void testTP42() throws Exception  {
		String folder = testDataPath + "TP-42_43_44" + slash;
		String outputFolder = folder + slash + "output";
		tempelPlusWrapper.deleteFile(outputFolder);
		
		if(!new File(outputFolder).mkdir()){
			throw new Exception("Could not create test output directory: " + outputFolder);
		}
		
		String fileIn = tempelPlusWrapper.getInputFile(folder);
		tempelPlusWrapper.runTempelPlus(new String[]{"extract", fileIn, "-verify", cn, "-output_folder", outputFolder});
		
		if(tempelPlusWrapper.verifyOutPut("Verifying container before extraction.")
		   &&tempelPlusWrapper.verifyOutPut("\\d documents handled successfully")
		   && tempelPlusWrapper.verifyOutPutIsNot("Verification unsuccessful.")
		   && tempelPlusWrapper.verifyOutPut(", OK")
		   ){
			TP42 = true;
		}
		
		tempelPlusWrapper.deleteFile(outputFolder);
		assertTrue(TP42);
	}
	
	/*
	 * TP-43: Verification with extra -verify argument, incorrect CN
	 * Pass if extraction uses -verify argument with wrong value and validation does not succeed
	 */
	@Test
	public final void testTP43() throws Exception  {
		String folder = testDataPath + "TP-42_43_44" + slash;
		String outputFolder = folder + slash + "output";
		tempelPlusWrapper.deleteFile(outputFolder);
		
		if(!new File(outputFolder).mkdir()){
			throw new Exception("Could not create test output directory: " + outputFolder);
		}
		
		String fileIn = tempelPlusWrapper.getInputFile(folder);
		tempelPlusWrapper.runTempelPlus(new String[]{"extract", fileIn, "-verify", "\"WRONG RECIPIENT\"", "-output_folder", outputFolder});
		
		if(tempelPlusWrapper.verifyOutPut("Verifying container before extraction.")
		   &&tempelPlusWrapper.verifyOutPut("\\d documents handled successfully")
		   && tempelPlusWrapper.verifyOutPut("Verification unsuccessful.")
		   ){
			TP43 = true;
		}
		
		tempelPlusWrapper.deleteFile(outputFolder);
		assertTrue(TP43);
	}
	
	/*
	 * TP-42: Verification with extra -verify argument without specified CN value
	 * 
	 * Pass if extraction uses -verify argument and verification works
	 */
	@Test
	public final void testTP44() throws Exception  {
		String folder = testDataPath + "TP-42_43_44" + slash;
		String outputFolder = folder + slash + "output";
		tempelPlusWrapper.deleteFile(outputFolder);
		
		if(!new File(outputFolder).mkdir()){
			throw new Exception("Could not create test output directory: " + outputFolder);
		}
		
		String fileIn = tempelPlusWrapper.getInputFile(folder);
		tempelPlusWrapper.runTempelPlus(new String[]{"extract", fileIn, "-verify", "-output_folder", outputFolder});
		
		if(tempelPlusWrapper.verifyOutPut("Verifying container before extraction.")
		   &&tempelPlusWrapper.verifyOutPut("\\d documents handled successfully")
		   && tempelPlusWrapper.verifyOutPutIsNot("Verification unsuccessful.")
		   && tempelPlusWrapper.verifyOutPut(", OK")
		   ){
			TP44 = true;
		}
		
		tempelPlusWrapper.deleteFile(outputFolder);
		assertTrue(TP44);
	}
	
	
	/*
	 * TP-45: Sign a directory that has subdirectories, make sure that output has the same subdirectory structure
	 */
	@Test
	public final void testTP45() throws Exception  {
		String folder = testDataPath + "TP-45_1" + slash;
		String outputFolder = testDataPath + "TP-45_2" + slash;
		File outputFolderObj = new File(outputFolder);
		String subDir1 = slash + "1";
		String subDir2 = slash + "1" + slash + "2";
		
		int expectedNumberOfFiles = 3;
		
		tempelPlusWrapper.deleteFile(outputFolder);
		
		if(!outputFolderObj.mkdir()){
			throw new Exception("Could not create test output directory: " + outputFolder);
		}
		
		tempelPlusWrapper.runTempelPlus(new String[]{"sign", folder, "-output_folder", outputFolder});
		
		if(outputFolderObj.listFiles().length > 0 && 
		   new File(outputFolder + subDir1).listFiles().length > 0 &&
		   new File(outputFolder + subDir2).listFiles().length > 0
			){
			TP44 = true;
		}
		
		if(TP44 && tempelPlusWrapper.verifyOutPut(expectedNumberOfFiles + " documents signed successfully")){
			TP44 = true;
		}else{
			TP44 = false;
		}
		
		tempelPlusWrapper.deleteFile(outputFolder);
		outputFolderObj.mkdir();
		assertTrue(TP44);
	}
	
	/*
	 * TP-45: Decrypt a directory that has subdirectories, make sure that output has the same subdirectory structure
	 */
	@Test
	public final void testTP46() throws Exception  {
		String folder = testDataPath + "TP-46_1" + slash;
		String outputFolder = testDataPath + "TP-46_2" + slash;
		File outputFolderObj = new File(outputFolder);
		String subDir1 = slash + "1";
		String subDir2 = slash + "1" + slash + "2";
		
		int expectedNumberOfFiles = 3;
		
		tempelPlusWrapper.deleteFile(outputFolder);
		
		if(!outputFolderObj.mkdir()){
			throw new Exception("Could not create test output directory: " + outputFolder);
		}
		
		tempelPlusWrapper.runTempelPlus(new String[]{"decrypt", folder, "-output_folder", outputFolder, "-recipient", recipient});
		
		if(outputFolderObj.listFiles().length > 0 && 
		   new File(outputFolder + subDir1).listFiles().length > 0 &&
		   new File(outputFolder + subDir2).listFiles().length > 0
			){
			TP45 = true;
		}
		
		if(TP45 && tempelPlusWrapper.verifyOutPut(expectedNumberOfFiles + " files decrypted successfully! " + expectedNumberOfFiles + " files created.")){
			TP45 = true;
		}else{
			TP45 = false;
		}
		
		tempelPlusWrapper.deleteFile(outputFolder);
		outputFolderObj.mkdir();
		assertTrue(TP45);
	}
	
	/*
	 * TP-37: Encrypt file via TempelPlus with a broken certificate
	 * 
	 * Return true if output matches "Encryption of the files failed!".
	 */
	@Test
	public final void testTP37() throws Exception  {
		String folder = testDataPath + "TP-37" + slash;
		tempelPlusWrapper.cleanFolder(folder, new String[]{".cdoc"}, 1);
		
		String fileIn = tempelPlusWrapper.getInputFile(folder);
		tempelPlusWrapper.runTempelPlus(new String[]{"encrypt", fileIn, "-cert", cert3, cert2});
		TP37 = tempelPlusWrapper.brokenCertificateVerify();
		assertTrue(TP37);
	}
	
	/*
	 * TP-97: Calculate checksum of input file, create container from input file
	 * via JDigiDoc, validate container via JDigiDoc, extract input file from
	 * container via TempelPlus, calculate checksum of output file, compare
	 * checksums.
	 * 
	 * Return true if container is valid and checksums match.
	 */
	@Test
	public final void testTP97() throws Exception {
		String folder = testDataPath + "TP-97" + slash;
		String outputFolder = folder + "output_folder" + slash;
		
		tempelPlusWrapper.cleanFolder(folder, new String[]{".ddoc"});
		tempelPlusWrapper.deleteFile(outputFolder);
		
		tempelPlusWrapper.outputFolderExist(outputFolder);
		String fileIn = tempelPlusWrapper.getInputFile(folder);
		String container = tempelPlusWrapper.getContainer(fileIn);
		File fileName = new File(fileIn);
		String noExtFName = fileName.getName();
		String newFileName = noExtFName.substring(0, noExtFName.lastIndexOf("."));
		newFileName = newFileName.concat(".ddoc" + slash);
		String fileOut = outputFolder + newFileName + fileName.getName();
		tempelPlusWrapper.runTempelPlus(new String[]{"sign", fileIn});
		// if folder doesn't exist
		tempelPlusWrapper.outputFolderExist(outputFolder);
		tempelPlusWrapper.runTempelPlus(new String[]{"extract", container, "-verify", "-output_folder", outputFolder});

		TP97 = tempelPlusWrapper.verifyChecksums(fileIn, fileOut, container);
		assertTrue(TP97);
		tempelPlusWrapper.deleteFile(container);
		tempelPlusWrapper.deleteFile(outputFolder);
	}
	
	/*
	 * TP-98: #1053 - tempelplus v1.2.0 extract -verify laseb läbi ilma allkirjadeta konteineri
	 * sh TempelPlus32.sh extract /home/skisotest/io/tryb/in/ -output_folder /home/skisotest/io/tryb/in -follow -remove_input -verify "TESTNUMBER,SEITSMES,14212128025"
	 * Pass test if datafile is not extracted
	 */
	@Test
	public final void testTP98() throws Exception  {
		
		String folder = testDataPath + "TP-98_99" + slash;
		String outputFolder = folder + "output" + slash;
		tempelPlusWrapper.deleteFile(outputFolder);

		if(!new File(outputFolder).mkdir()){
			throw new Exception("Could not create test output directory: " + outputFolder);
		}
		
		tempelPlusWrapper.runTempelPlus(new String[]{"extract", folder, "-output_folder", outputFolder, "-verify", "TESTNUMBER,SEITSMES,14212128025"});
		
		if(tempelPlusWrapper.verifyOutPut("Verifying container before extraction.")
				   && tempelPlusWrapper.verifyOutPut("\\d documents handled successfully")
				   && tempelPlusWrapper.verifyOutPutIsNot("Verification successful.")
				   && tempelPlusWrapper.verifyOutPut("Found no signatures from container")
				   && tempelPlusWrapper.verifyOutPutIsNot(", OK")
				   && tempelPlusWrapper.verifyOutPut("0 files extracted")
				   ){
					TP98 = true;
				}
				
		tempelPlusWrapper.deleteFile(outputFolder);
		assertTrue(TP98);
	}
	
	/*
	 * TP-99: #1053 - tempelplus v1.2.0 extract -verify laseb läbi ilma allkirjadeta konteineri
	 * sh TempelPlus32.sh extract /home/skisotest/io/tryb/temp_sign/ -output_folder /home/skisotest/io/tryb/in -follow -remove_input -verify
	 * Pass test if datafile is not extracted  
	 */
	@Test
	public final void testTP99() throws Exception  {
		
		String folder = testDataPath + "TP-98_99" + slash;
		String outputFolder = folder + "output" + slash;
		tempelPlusWrapper.deleteFile(outputFolder);

		if(!new File(outputFolder).mkdir()){
			throw new Exception("Could not create test output directory: " + outputFolder);
		}
		
		tempelPlusWrapper.runTempelPlus(new String[]{"extract", folder, "-output_folder", outputFolder, "-verify"});
		
		if(tempelPlusWrapper.verifyOutPut("Verifying container before extraction.")
				   && tempelPlusWrapper.verifyOutPut("\\d documents handled successfully")
				   && tempelPlusWrapper.verifyOutPutIsNot("Verification successful.")
				   && tempelPlusWrapper.verifyOutPut("Found no signatures from container")
				   && tempelPlusWrapper.verifyOutPutIsNot(", OK")
				   && tempelPlusWrapper.verifyOutPut("0 files extracted")
				   ){
					TP99 = true;
				}
				
		tempelPlusWrapper.deleteFile(outputFolder);
		assertTrue(TP99);
	}
	
	
}