package ee.sk.tempelPlus;

import java.io.File;
import java.io.FileOutputStream;
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

	public boolean run(String[] args) throws DigiDocException {
		try {
			parseParams(args);
			if (args.length > 1 && (args[1].trim().equalsIgnoreCase("-?") || args[1].trim().equalsIgnoreCase("-help"))) {
				printHelp();
				exit(0);
			}

			List<File> workFiles = getFilesWithExt(args[1], new ArrayList<File>());
			if (workFiles.size() == 0) {
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
			int i = 1;
			int countFiles = 0;
			for (File file : workFiles) {
				log.info("Extracting from file " + i + " of " + workFiles.size() + ". Currently extracting: '" + file.getName() + "'");
				SignedDoc sdoc = digFac.readSignedDoc(file.getAbsolutePath());
				
				File dir = null;
				if (!cmnExtDir) {
//					File dir = new File(outputFolder + File.separator + file.getName());
					dir = new File(makeNameWithDot(outputFolder + File.separator + file.getName(), ""));
					if (dir.mkdir())
						log.info("Made directory " + outputFolder + File.separator + file.getName());
					else {
						log.error("Creation of directory '" + dir + "' failed.");
						exit(1);
					}
				}

				File dataf = null;
				log.info("Found " + sdoc.countDataFiles() + " files. Extracting..");
				for (int j = 0; j < sdoc.countDataFiles(); j++) {
					DataFile f = sdoc.getDataFile(j);
					if (!cmnExtDir)
						dataf = new File(makeName(dir + File.separator + f.getFileName(), f.getFileName().substring(f.getFileName().lastIndexOf(".") + 1)));
					else
						dataf = new File(makeName(outputFolder + File.separator + f.getFileName(), f.getFileName().substring(f.getFileName().lastIndexOf(".") + 1)));
					new FileOutputStream(dataf).write(f.getBodyAsData());
					countFiles++;
				}
				i++;
				log.info("Done");
			}
			log.info(workFiles.size() + " documents where handled successfully. " + countFiles + " files extracted");
		} catch (Exception e) {
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
	}

	public void parseParams(String[] args) {
		boolean FoundOutputParameter = false;
		if (args.length > 2) {// lisaargumendid
			for (int i = 2; i < args.length; i++) {
				if (args[i].equalsIgnoreCase(OUTPUT_F)) {
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
		} else {
			printHelp();
			exit(0);
		}
	}

}
