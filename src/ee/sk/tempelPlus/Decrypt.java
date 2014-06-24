package ee.sk.tempelPlus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import ee.sk.digidoc.DataFile;
import ee.sk.digidoc.DigiDocException;
import ee.sk.digidoc.SignedDoc;
import ee.sk.digidoc.TokenKeyInfo;
import ee.sk.digidoc.factory.DigiDocFactory;
import ee.sk.digidoc.factory.SignatureFactory;
import ee.sk.tempelPlus.util.Config;
import ee.sk.tempelPlus.util.TempelPlusException;
import ee.sk.utils.ConfigManager;
import ee.sk.xmlenc.EncryptedData;
import ee.sk.xmlenc.factory.EncryptedDataParser;
import ee.sk.xmlenc.factory.EncryptedStreamParser;

/**
 * Decryption functionality
 * 
 * @author Erik Kaju
 * 
 */

public class Decrypt extends TempelPlus {

	public Logger log = Logger.getLogger(Decrypt.class);
	private String recipient;
	private static String RECIPIENT = "-recipient";
	private static final String FAIL = "Decryption of the files failed!";
	private static String[] decryptionTargetDirectories;
	private static List<File> workFiles;
	private List<String> workFileOutputPaths;
	SignatureFactory sigFac = null;

	public boolean run(String[] args) throws DigiDocException {
		try {
			if (args.length > 1 && (args[1].trim().equalsIgnoreCase("-?") || args[1].trim().equalsIgnoreCase("-help"))) {
				printHelp();
				exit(0);
			}

			parseParams(args);

			updateWorkFiles(args);

			if (workFiles.size() == 0 && !follow) {
				log.error("Encrypted file type should be \"cdoc\" ");
				log.info("");
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
			
			updateDecryptTargetDirs(workFiles, workFileOutputPaths);

			askQuestion("Are you sure you want to decrypt " + workFiles.size() + " files, encrypted to cdoc? Y\\N");
			pin = pinInsertion();
			int i = 1, count = 0;
			int decrypt_token_index = -1; // used in case of etoken
			TokenKeyInfo decrypt_token = null; // used in case of HSM

			sigFac = ConfigManager.instance().getSignatureFactory(); // getSignatureFactoryOfType() shouldn't be used as it doesn't use singleton pattern

			//find decryption token according to user's pre-defined parameters
			if (usingSlotAndLabel) { //HSM and slot&label
				decrypt_token = findTokenBySlotAndLabel();
				if (decrypt_token == null) {
					log.error("\nDecryption token with label '" + commandParameterLabel + "' was not found from slot with ID '" + commandParameterSlot + "'!");
					log.info("");
					log.error(FAIL);
					exit(1);
				}
			} else if (recipient != null && recipient.length() > 0) { //etoken and CN
				decrypt_token_index = findTokenIndexByCn();
				if (decrypt_token_index < 0) {
					log.error("No decryption certificates with specified CN (" + recipient + ") found on eToken device");
					printAvailableDeviceCNs(getDeviceRecipients());
					log.info("");
					log.error(FAIL);
					exit(1);
				}
			}

//         X509Certificate cert =null;
//         try{
//            //Muudame leveli taset et keerata logi kinni kui vale PIN on
//            Logger localLogger = Logger.getLogger(DigiDocException.class);
//            Level curr = localLogger.getLevel();
//            localLogger.setLevel(Level.FATAL);
//            cert = sigFac.getCertificate(0, pin);
//            cert.checkValidity();
//            localLogger.setLevel(curr);
//         }catch (DigiDocException e){
//            if(e.getCode()==DigiDocException.ERR_TOKEN_LOGIN){
//               log.error("Incorrect PIN!");
//               exit(1);
//            }else{
//               log.error(e.getMessage(),e);
//               exit(1);
//            }
//         }
//         Util.checkCertificate(cert);// Kontrollime sertifikaati
			DigiDocFactory digFac = ConfigManager.instance().getDigiDocFactory();
			int decryptedCount = 0;
			while (true) {

				// in follow mode workfiles arraylist may be out of sync
				if (follow) {
					updateWorkFiles(args);
					updateDecryptTargetDirs(workFiles, workFileOutputPaths);
				}
				//for (File file : workFiles) {
				for (int forC = 0; forC < workFiles.size(); forC++) {
					File file = workFiles.get(forC);

					// If the token has not been found -> will get list of cdoc recipients and compare them to device tokens' recipients
					if (!usingSlotAndLabel && decrypt_token_index < 0) {
						ArrayList<String> deviceTokenRecipients = getDeviceRecipients();
						ArrayList<String> cdocTokenRecipients = getCdocRecipients(file);
						log.info("No recipient specified by user");
						log.info("Found " + deviceTokenRecipients.size() + " recipients on device");
						log.info("Found " + cdocTokenRecipients.size() + " recipients in cdoc");
						decrypt_token_index = findRecipientsPair(deviceTokenRecipients, cdocTokenRecipients);

						if (decrypt_token_index < 0) {
							log.error("No matching CNs were found among device and cdoc tokens");
							log.info("");
							printAvailableDeviceCNs(deviceTokenRecipients);
							log.info("");
							printAvailableCdocCNs(cdocTokenRecipients);
							log.info("");
							exit(1);
						}
						log.info("Will use token with index: " + decrypt_token_index + ". Recipient: " + deviceTokenRecipients.get(decrypt_token_index));
						recipient = deviceTokenRecipients.get(decrypt_token_index);
					}
									
// Kontrollida, kas krüpteeritud fail oli DDOC konteineris?
//					EncryptedDataParser dencFac =  ConfigManager.instance().getEncryptedDataParser();
//					EncryptedData cdoc = dencFac.readEncryptedData(file.getAbsolutePath());
//					if(cdoc.findPropertyByName("orig_file") == null) {
//						log.info("Skipping file " + i + " of " + workFiles.size() + ". File '" + file.getName() + "' is not in DDOC container and decrypting is currently not supported!");
//						i++;
//						log.info("Done");
//						continue;
//					}			
					
					log.info("Decrypting file " + i + " of " + workFiles.size() + ". Currently processing '" + file.getName() + "'");
					FileInputStream fis = new FileInputStream(file);
					File f2 = File.createTempFile(file.getName().substring(0, file.getName().lastIndexOf('.')) + "___", "ddoc"); // currently only supported decrypting files that are in DDOC container
					f2.deleteOnExit();
					FileOutputStream fos = new FileOutputStream(f2);
					EncryptedStreamParser streamParser = ConfigManager.instance().getEncryptedStreamParser();

					//decryption
					if (usingSlotAndLabel) { //HSM
						int token_slot = (int) decrypt_token.getSlot();
						String token_label = decrypt_token.getLabel();
						streamParser.decryptStreamUsingRecipientSlotIdAndTokenLabel(fis, fos, token_slot, token_label, pin); //token is determined with slot and label
					} else { // etoken 
			            streamParser.decryptStreamUsingRecipientName(fis, fos, decrypt_token_index, pin, null); //token is determined by its index in the list of all decryption tokens found from the device
					}

					fos.close();
					fis.close();

					//Remove unusual data after the </SignedDoc> element
					BufferedReader reader = new BufferedReader(new FileReader(f2));

					String line = "", oldtext = "";
					char[] inputBuf = new char[1024 * 1024];

					int readChars = 0;

					while ((readChars = reader.read(inputBuf, 0, 1024 * 1024)) > 0) {
						line = String.copyValueOf(inputBuf, 0, readChars);

						if (line.indexOf("</SignedDoc>") > 0) {
							String lineNew = line.substring(0, line.indexOf("</SignedDoc>") + 12);
							oldtext += lineNew;
						} else {
							oldtext += line;
						}
					}
					reader.close();

					//To replace a line in a file
					String newtext = oldtext;

					FileWriter writer = new FileWriter(f2);
					writer.write(newtext);
					writer.close();

					//tekitame kaustanime
					File folder = null;
					
					if(!cmnExtDir){
						folder = new File(makeNameWithDot(decryptionTargetDirectories[forC], "")); //kaustanimi lähtefaili nimega (.cdoc lõpuga)
					}else{
						folder = new File(decryptionTargetDirectories[forC], ""); //kaustanimi lähtefaili nimega (.cdoc lõpuga)	
					}
					
					
					
					if (!folder.exists() && !folder.mkdirs()) {
						log.error("Creation of directory '" + folder.getAbsolutePath() + "' failed!");
						exit(1);
					}

					//pakime lahti seesolnud ddoc'i
					SignedDoc sdoc = digFac.readSignedDoc(f2.getAbsolutePath());
					File dataf = null;
					for (int j = 0; j < sdoc.countDataFiles(); j++) {
						DataFile f = sdoc.getDataFile(j);
						dataf = new File(makeName(folder.getAbsolutePath() + File.separator + f.getFileName(), f.getFileName().substring(f.getFileName().lastIndexOf(".") + 1)));
						
						new FileOutputStream(dataf).write(f.getBodyAsData());
						//f.cleanupDfCache();//laseme temp failid maha
						count++;
					}
					i++;
					f2.delete();
					log.info("Done: " + dataf.getAbsolutePath());
					decryptedCount++;
					if (follow || remInput) {
						log.info("trying to delete file:" + file.getName());
						file.delete();
					}
				}
				if (follow) {
					Thread.sleep(1000);
					workFiles = getFilesWithExt(args[1], new ArrayList<File>(), Config.getProps().getProperty(Config.CRYPT));
					i = 1;
				} else {
					break;
				}
			}
			log.info(decryptedCount + " files decrypted successfully! " + count + " files created.");
		} catch (Exception e) {
			//log.error("Decryption of the files failed!", e);
			verifyError(e, "Decryption of the files failed!", false);
			return true;
		}
		return false;
	}

	/**
	 * Finds token for decryption from device. Token is found according to slot
	 * ID and token label parameters defined by the user.
	 * 
	 * @return TokenKeyInfo object which contains data of the selected token
	 */
	private TokenKeyInfo findTokenBySlotAndLabel() {

		TokenKeyInfo[] tokens = sigFac.getTokensOfType(false); //võtab kõik dekrüpteerimise tokenid         
		if (tokens.length == 0) {
			log.error("\nNo decryption tokens were found from device!");
			exit(1);
		}
		if (commandParameterSlot >= 0 && commandParameterLabel != null) { //slot ja label määratud
			for (TokenKeyInfo tki : tokens) {
				if (commandParameterSlot == tki.getSlot() && commandParameterLabel.equals(tki.getLabel())) {
					return tki;
				}
			}
		}
		return null;
	}

	/**
	 * Finds index of the token for decryption from the list of decryption
	 * tokens on the device. Token is found according to certificate's CN value
	 * defined by the user.
	 * 
	 * @return index of the token in the list of all decryption tokens on the
	 *         device
	 */
	private int findTokenIndexByCn() {
		TokenKeyInfo[] tokens = sigFac.getTokensOfType(false); //võtab kõik dekrüpteerimise tokenid         

		if (tokens.length == 0) {
			log.error("\nNo decryption tokens were found from device!");
			exit(1);
		}
		for (int i = 0; i < tokens.length; i++) {
			
			
//			if (tokens[i].getCertName().equals(recipient)) {
//				return i;
//			} else if (recipient.contains(",")) { //If user added organization to recipient, like AS Sertifitseerimiskeskus or ID-CARD
//				if (tokens[i].getCertName().equals(recipient.substring(0, recipient.lastIndexOf(",")))) {
//					return i;
//				}
//			}
			if(matchCNs(tokens[i].getCertName(), recipient)){
				return i;
			}
		}
		return -1;
	}

	private void updateWorkFiles(String[] args) {
		workFiles = getFilesWithExt(args[1], new ArrayList<File>(), Config.getProps().getProperty(Config.CRYPT));
		workFileOutputPaths = getRelativeOutputPaths(workFiles, args[1]);
	}

	private void updateDecryptTargetDirs(List<File> workFiles, List<String> relativeOutputPaths) throws TempelPlusException {
		decryptionTargetDirectories = new String[workFiles.size()];
		//for(File file:workFiles){
		for (int i = 0; i < workFiles.size(); i++) {
			File file = workFiles.get(i);

			//EncryptedData m_cdoc = dencFac.readEncryptedData(file.getAbsolutePath());

			//File folder = new File(outputFolder+File.separator+m_cdoc.findPropertyByName(EncryptedData.ENCPROP_FILENAME).getContent());
			
			if(!cmnExtDir){
				decryptionTargetDirectories[i] = makeNameWithDot(outputFolder + relativeOutputPaths.get(i) + File.separator + file.getName()/*.replace("." + Config.getProps().getProperty(Config.CRYPT), "")*/, "");
			}else{
				decryptionTargetDirectories[i] = outputFolder + relativeOutputPaths.get(i);
			}
			
			

//            File folder = new File(decryptionTargetDirectories[i]);
//            
//            if(folder.exists() && folder.isDirectory()){
//               log.error("Folder already exists:"+folder.getAbsolutePath());
//               exit(1);
//            }
		}
	}

	/**
	 *  Prints available CNs for decryption
	 * @param deviceTokenRecipients
	 */
	private void printAvailableDeviceCNs(ArrayList<String> deviceTokenRecipients) {
		log.info("List of available CNs on device:");
		for (int i = 0; i < deviceTokenRecipients.size(); i++) {
			log.info(deviceTokenRecipients.get(i));
		}

	}

	/**
	 *  Prints available CNs for decryption
	 * @param cdocTokenRecipients
	 */
	private void printAvailableCdocCNs(ArrayList<String> cdocTokenRecipients) {
		log.info("List of available CNs in cdoc:");
		for (int i = 0; i < cdocTokenRecipients.size(); i++) {
			log.info(cdocTokenRecipients.get(i));
		}

	}

	/**
	 * Finds a matching pair of CN values in cdoc and certificates on device.
	 * 
	 * @param deviceTokenRecipients
	 *            CN values of decryption certificates on device
	 * @param cdocTokenRecipients
	 *            CN values of cdoc file's recipients
	 * @return index of the decryption token in a list of available decryption
	 *         tokens on device which has a matching CN name with cdoc file's
	 *         recipient list
	 */
	private int findRecipientsPair(ArrayList<String> deviceTokenRecipients, ArrayList<String> cdocTokenRecipients) {
		for (int i = 0; i < deviceTokenRecipients.size(); i++) {
			for (int j = 0; j < cdocTokenRecipients.size(); j++) {
				if (deviceTokenRecipients.get(i).equals(cdocTokenRecipients.get(j)))
					return i;
				else if (cdocTokenRecipients.get(j).contains(","))
					if (deviceTokenRecipients.get(i).equals(cdocTokenRecipients.get(j).substring(0, cdocTokenRecipients.get(j).lastIndexOf(","))))
						return i;
			}
		}
		return -1;
	}

	/**
	 * TempelPlus function that finds all encrypted keys in cdoc, gets
	 * recipients from them, returns them in arraylist
	 * 
	 * @param cdocFile
	 *            - cdoc file
	 * @return - all recipients found in cdoc
	 * @throws DigiDocException
	 */
	private ArrayList<String> getCdocRecipients(File cdocFile) throws DigiDocException {

		ArrayList<String> recipients = new ArrayList<String>();

		EncryptedDataParser dencFac = ConfigManager.instance().getEncryptedDataParser();
		EncryptedData m_cdoc = dencFac.readEncryptedData(cdocFile.getAbsolutePath());
		int numberOfRecipientsOfCdoc = m_cdoc.getNumKeys();

		for (int i = 0; i < numberOfRecipientsOfCdoc; i++)
			recipients.add(m_cdoc.getEncryptedKey(i).getRecipient());

		if (recipients.size() < 1) {
			log.error("CDOC might be corrupted, no recipient information found: " + cdocFile);
			exit(1);
		}
		return recipients;
	}

	/**
	 * Finds list of decryption certificates on the device and gets their CN names.
	 * @return list of CN names of decryption certificates found from the device
	 */
	private ArrayList<String> getDeviceRecipients() {
		TokenKeyInfo[] tokens = sigFac.getTokensOfType(false); //võtab kõik dekrüpteerimise tokenid         
		if (tokens.length == 0) {
			log.error("\nNo decryption tokens were found from device!");
			exit(1);
		}
		ArrayList<String> certificateCNs = new ArrayList<String>();
		for (TokenKeyInfo tki : tokens)
			certificateCNs.add(tki.getCertName());
		return certificateCNs;
	}

	public void parseParams(String[] args) {
		boolean fine = false;
		try {
			fine = true;
			if (args.length > 2) {// lisaargumendid
				for (int i = 2; i < args.length; i++) {

					// Entering pin via command parameter
					if (args[i].equalsIgnoreCase(PARAMPIN)) {
						if (i + 1 < args.length) {
							commandParameterPin = args[i + 1];
							i++;
						}
					} else if (args[i].equalsIgnoreCase(OUTPUT_F)) {
//                  if (i + 1 < args.length)
//                  {
//                     outputFolder = new File(args[i + 1]);
//                  }
//                  i++;

						if (i + 1 < args.length) {
							//outputFolder = new File(args[i + 1]);

							ArgsParams params = ConcatParams(args, i + 1);
							outputFolder = new File(params.Params);
							i = params.i - 1;
						}
						//i++;

					} else if (args[i].equalsIgnoreCase("-config")) {
						if (i + 1 < args.length) {
							ArgsParams params = ConcatParams(args, i + 1);
							i = params.i - 1;
						}
						//i++;
					} else if (args[i].equalsIgnoreCase(REM_INPUT)) {
						remInput = true;
					} else if (args[i].equalsIgnoreCase(FOLLOW)) {
						follow = true;
					} else if (args[i].equalsIgnoreCase(RECIPIENT)) {
//                  if (i + 1 < args.length)
//                  {
//                     recipient=args[i+1];
//                  }
//                  i++;

						if (i + 1 < args.length) {
							//outputFolder = new File(args[i + 1]);

							ArgsParams params = ConcatParams(args, i + 1);
							recipient = params.Params;
							i = params.i - 1;
						}
						//i++;

					} else if (args[i].equalsIgnoreCase(SLOT)) {
						if (i + 1 < args.length) {
							commandParameterSlot = Long.parseLong(args[i + 1]);
							i++;
						}
					} else if (args[i].equalsIgnoreCase(LABEL)) {
						if (i + 1 < args.length) {
							commandParameterLabel = args[i + 1];
							i++;
						}
					} else if (args[i].equalsIgnoreCase(CMN_EXT_DIR)) {
						Config.getProps().put(Config.CMN_EXT_DIR, "true");
					} else {
						fine = false;
					}
				}

				if (follow && outputFolder == null) {
					log.error("Must specify output folder when using follow!");
					exit(1);
				} else if (follow && !remInput) {
					log.error("Must specify remove input when using follow!");
					exit(1);
				}

				if (commandParameterSlot >= 0 && commandParameterLabel != null) {
					usingSlotAndLabel = true;
				} else if ((commandParameterSlot < 0 && commandParameterLabel != null) || (commandParameterSlot >= 0 && commandParameterLabel == null)) {
					log.error("Slot or label parameter not specified! Slot and label have to be used concurrently!");
					exit(1);
				}
			}

		} catch (Exception e) {
			//log.error("Parsing parameters failed", e);
			verifyError(e, "Parsing parameters failed", true);
			fine = false;
		}
		if (!fine) {
			printHelp();
			System.exit(1);
		}
	}

	protected void printHelp() {
		log.info("Decrypts selected files");
		log.info("usage:");
		log.info("TempelPlus decrypt <folder or file> <additional params>");
		log.info("");
		log.info("Additional (optional) params:");
		log.info("-recipient <CN>               Common name of encryption recipient's certificate");
		log.info("-pin <code>        			  etoken pin code");
		log.info("-output_folder <folder>       folder where files are written");
		log.info("-remove_input                 deletes files that are used");
		log.info("-follow                       program does not exit and watches input folder");
		log.info("                              must also use parameters -output_folder and -remove_input");
		log.info("-slot <slot_id>          		slot's ID where the token is taken from. Must be used along with label parameter. ");
		log.info("-label <label>          		label name of the token to be used. Must be used along with slot parameter");
		log.info("-cmn_ext_dir          		decrypted files are written to a common output directory (instead of subfolders)");
	}
}