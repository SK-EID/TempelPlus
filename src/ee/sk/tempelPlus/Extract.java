package ee.sk.tempelPlus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import ee.sk.digidoc.DataFile;
import ee.sk.digidoc.DigiDocException;
import ee.sk.digidoc.SignedDoc;
import ee.sk.digidoc.factory.DigiDocFactory;
import ee.sk.tempelPlus.util.Config;
import ee.sk.utils.ConfigManager;

public class Extract extends TempelPlus {

	public Logger log = Logger.getLogger(Extract.class);
	private boolean verifyEnabled = false;
	Verify verify;
	String verificationCN = null;

	public boolean run(String[] args) throws DigiDocException {
		try {
			parseParams(args);
			
			if(verifyEnabled){
				verify = new Verify();
				verify.setPrintFileCount(false);
			}
			
			if (args.length > 1 && (args[1].trim().equalsIgnoreCase("-?") || args[1].trim().equalsIgnoreCase("-help"))) {
				printHelp();
				exit(0);
			}

			List<File> workFiles = getFilesWithExt(args[1], new ArrayList<File>());
			List<String> workFileOutputPaths = getRelativeOutputPaths(workFiles, args[1]);
			
			if (!follow && workFiles.size() == 0) {
				log.info("\nSpecified directory has no files to work with: '" + args[1] + "'! \n");
				printHelp();
				System.exit(1);
			}
			
			setOutPut(args[1]);
			
			// Checks whether the decrypted files should be written to separate folders or directly to output folder
			String ext = Config.getProp(Config.CMN_EXT_DIR);
			if(ext != null && ext.trim().equalsIgnoreCase("true")){
				cmnExtDir = true;
			} else {
				cmnExtDir = false;
			}
			
			askQuestion("Are you sure you want to extract data files from " + workFiles.size() + " files? Y\\N ");
			DigiDocFactory digFac = ConfigManager.instance().getDigiDocFactory();
			
			
			int countFiles = 0;
			
			while (true) {

				int i = 1;

				for (File file : workFiles) {
					currentFile = file;
					if (verifyEnabled) {

						log.info("\nVerifying container before extraction.");
						String fileToExctract = file.getAbsolutePath();
						String[] verificationArgs; // = new String[]{"verify",
													// fileToExctract};

						if (verificationCN != null) {
							verificationArgs = new String[4];
							verificationArgs[2] = "-cn";
							verificationArgs[3] = verificationCN;
						} else {
							verificationArgs = new String[2];
						}

						verificationArgs[0] = "verify";
						verificationArgs[1] = fileToExctract;

						if (!verify.run(verificationArgs)) {

							if (follow) {
								String fault = "DigiDoc file's verification failed.";
								if (verificationCN != null) {
									fault += "Verification parameter CN: '" + verificationCN + "'";
								}
								throw new Exception(fault);
							} else {
								log.info("Skipping container: " + fileToExctract);
								i++;
								continue;
							}
						}

					}
					System.out.println();

					log.info("Extracting from file " + i + " of " + workFiles.size() + ". Currently extracting: '" + file.getName() + "'");
					SignedDoc sdoc = digFac.readSignedDoc(file.getAbsolutePath());

					File dir = null;
					if (!cmnExtDir) {
						// File dir = new File(outputFolder + File.separator +
						// file.getName());
						dir = new File(makeNameWithDot(outputFolder + File.separator + workFileOutputPaths.get(i-1) + File.separator + file.getName(), ""));
						if (dir.mkdirs())
							log.info("Made directory: " + dir);
						else {
							log.error("Creation of directory '" + dir + "' failed.");
							exit(1);
						}
					}

					File dataf = null;
					log.info("Found " + sdoc.countDataFiles() + " files. Extracting..");

					for (int j = 0; j < sdoc.countDataFiles(); j++) {
						DataFile f = sdoc.getDataFile(j);

						if (!cmnExtDir) {
							outPutFileName = makeName(dir + File.separator + f.getFileName(), f.getFileName().substring(f.getFileName().lastIndexOf(".") + 1));
						} else {
							outPutFileName = makeName(outputFolder + File.separator + f.getFileName(), f.getFileName().substring(f.getFileName().lastIndexOf(".") + 1));
						}

						FileOutputStream fos = new FileOutputStream(outPutFileName);
						InputStream is = f.getBodyAsStream();
						byte[] data = new byte[4096];
                		int n = 0;
                		while((n = is.read(data)) > 0) {
                			fos.write(data, 0, n);
                		}
                		fos.close();
                		is.close();
						countFiles++;
					}

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
					workFileOutputPaths = getRelativeOutputPaths(workFiles, args[1]);
					i = 1;
				} else {
					break;
				}

			}
			log.info("\n" + workFiles.size() + " documents were handled successfully. " + countFiles + " files extracted");
			
		} catch (Exception e) {
			
			//// BEGIN COPYPASTED CODE FROM Sign.java 
			//(in future this could be refactored, if such functionality will be implemented in other places) For example extract the functionality as protected method into TempelPlus class 
			if (follow) {
				//e.printStackTrace();
				log.info("Working in follow mode");
				String errorFolderPath;

				errorFolderPath = outputFolder.getAbsolutePath() + File.separator + "error";
				boolean createdErrorFolder = false;
				if (new File(errorFolderPath).exists()) {
					// log.info("Error folder already exists.");
					createdErrorFolder = true;
				} else {
					log.info("Creating an error folder into specified output directory");

					int creationCounter = 1;

					while (createdErrorFolder == false) {

						createdErrorFolder = (new File(errorFolderPath)).mkdirs();
						if (creationCounter >= 5) {
							wait(1);
							log.fatal("FATAL error. Couldn't create an error directory after 5 attempts.");
							return true;
						}
						creationCounter++;
					}
				}

				log.info("Trying to move a problematic file into error directory.");

				if (currentFile.exists()) {

					File errorFile = new File(makeName(errorFolderPath + File.separator + currentFile.getName()));
					moveFile(currentFile, new File(errorFolderPath + File.separator + errorFile.getName()));
					System.gc();

				} else {
					log.fatal("File: " + currentFile.getName() + " got missing from input directory during extracting process!");

					if (outPutFileName != null && new File(outPutFileName).exists()) {
						moveFile(new File(outPutFileName), new File(makeName(errorFolderPath + File.separator + new File(outPutFileName).getName())));
					}
				}
				log.info("Restarting extracting process.");
				log.info("");
				this.run(args);

			} else {
				return true;
			}
			//// END COPYPASTED CODE FROM Sign.java
			
			verifyError(e, "Extraction of files failed!", true);
			//log.error("Extraction of files failed!", e);
			return true;
		}
		return false;
	}

	protected void printHelp() {
		log.info("Extracts all files from given digidoc files");
		log.info("usage:");
		log.info("TempelPlus extract <folder or file>");
		log.info("");
		log.info("Additional (optional) params:");
		log.info("-output_folder <folder>       folder where files are written");
		log.info("-cmn_ext_dir          		extracted files are written to a common output directory (instead of subfolders)");
	    log.info("-verify <common name>			verify containers before extracting. Also certificate owner's common name may be used");
		log.info("-remove_input                 deletes files that are used");
		log.info("-follow                       program does not exit and watches input folder");
	}

	public void parseParams(String[] args) {
		boolean FoundOutputParameter = false;
		int commonVars = 2;
		
		if (args.length > commonVars) {// lisaargumendid
			for (int i = commonVars; i < args.length; i++) {
				
				if(args[i].equalsIgnoreCase(EXTRACTPARAM_VERIFY)){
					verifyEnabled = true;
					
					if (i + 1 < args.length) {
						ArgsParams params = ConcatParams(args, i + 1);
						verificationCN = params.Params;
						i = params.i - 1;
					}else{
						i++;
					}
					
				}else if (args[i].equalsIgnoreCase(FOLLOW)) {
					follow = true;
				}else if (args[i].equalsIgnoreCase(REM_INPUT)) {
					remInput = true;
				}else if (args[i].equalsIgnoreCase(OUTPUT_F)) {
//               if (i + 1 < args.length)
//               {
//                  outputFolder = new File(args[i + 1]);
//
//                  FoundOutputParameter = true;
//               }
//               i++;

					if (i + 1 < args.length) {
						//outputFolder = new File(args[i + 1]);

						ArgsParams params = ConcatParams(args, i + 1);
						outputFolder = new File(params.Params);
						FoundOutputParameter = true;
						i = params.i - 1;
					}
				} else if (args[i].equalsIgnoreCase(CMN_EXT_DIR)) {
					Config.getProps().put(Config.CMN_EXT_DIR, "true");
				}
			}
			
			if (!FoundOutputParameter) {
				printHelp();
				exit(0);
			}
			
			checkFollowArgs(follow, outputFolder, remInput);
			
		} else {
			printHelp();
			exit(0);
		}
	}

}
