package ee.sk.tempelPlus.util;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.security.cert.X509Certificate;

import javax.activation.MimetypesFileTypeMap;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import ee.sk.digidoc.DataFile;
import ee.sk.digidoc.DigiDocException;
import ee.sk.digidoc.Signature;
import ee.sk.digidoc.SignatureProductionPlace;
import ee.sk.digidoc.SignedDoc;
import ee.sk.digidoc.factory.DigiDocFactory;
import ee.sk.digidoc.factory.SignatureFactory;
import ee.sk.tempelPlus.TempelPlus;
import ee.sk.utils.ConfigManager;

public class GraphicSign extends JFrame{

   static String pin =null;
   boolean correctPin = false;
   X509Certificate cert =null;
   MimetypesFileTypeMap m = new MimetypesFileTypeMap();
   DigiDocFactory digFac = null;
   SignatureFactory sigFac = null;

   public GraphicSign(){
      setSize(400,400);
      setTitle("Graphic TempelPlus signing tool");
      setDefaultCloseOperation(EXIT_ON_CLOSE);

      Toolkit toolkit = getToolkit();
      Dimension size = toolkit.getScreenSize();
      setLocation(size.width/2 - getWidth()/2, size.height/2 - getHeight()/2);

      JPanel panel = new JPanel();
      getContentPane().add(panel);

      JMenuBar menubar = new JMenuBar();
      JMenu filemenu = new JMenu("File");
      JMenuItem close = new JMenuItem("Close");
      close.addActionListener(new ActionListener() {

         public void actionPerformed(ActionEvent e) {
            dispose();
            System.exit(0);
         }
      });
      filemenu.add(close);
      menubar.add(filemenu);
      setJMenuBar(menubar);
      //file list
      final DefaultListModel model = new DefaultListModel();
      final JList list = new JList(model);
      list.setBounds(150, 30, 220, 150);
      JScrollPane listPane = new JScrollPane(list);
      JLabel addFilesText = new JLabel("Files to be signed");

      //file list buttons
      final JFileChooser chooser = new JFileChooser();
      chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
      chooser.setMultiSelectionEnabled(true);
      JButton addButton = new JButton("Add file");
      addButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            int retVal = chooser.showDialog(GraphicSign.this, "Choose");
            if(retVal==JFileChooser.APPROVE_OPTION){
               File[] files = chooser.getSelectedFiles();
               for(File f:files){
                  model.addElement(f);
               }
//               GraphicSign.this.repaint();
            }else{
            }
         }
      });
      JButton remButton = new JButton("Remove file");
      remButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            for(int r:list.getSelectedIndices()){
               model.remove(r);
            }
         }
      });

      //delete checkBox
      final JCheckBox deleteBox = new JCheckBox("Delete original files after sign");

      //exit folder text&buttons
      JLabel exitFolderText = new JLabel("Output folder:");
      final JFileChooser exitChooser = new JFileChooser();
      JButton exitButton = new JButton("Choose");
      final JTextField exitFolder = new JTextField();
      exitFolder.setPreferredSize(new Dimension(200,20));
      exitChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

      exitButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            int retVal = exitChooser.showDialog(GraphicSign.this, "Choose");
            if(retVal==JFileChooser.APPROVE_OPTION){
               File file = exitChooser.getSelectedFile();
               exitFolder.setText(file.getAbsolutePath());
            }
         }
      });


      //sign button
      JButton sign = new JButton("Sign");
      sign.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            if(!model.isEmpty()&&exitFolder.getText()!=null&&exitFolder.getText().length()>0)
               doSigning(model,exitFolder.getText(),deleteBox.isSelected());
         }
      });

      //setting all up
      GridBagConstraints c = new GridBagConstraints();
      panel.setLayout(new GridBagLayout());
      c.gridwidth =GridBagConstraints.REMAINDER;
      panel.add(addFilesText,c);
//      c.gridwidth =GridBagConstraints.REMAINDER;
      panel.add(listPane,c);
      JPanel buttons = new JPanel();
      buttons.setLayout(new FlowLayout());
      buttons.add(addButton);
      buttons.add(remButton);
      panel.add(buttons,c);
      panel.add(deleteBox,c);
      JPanel low = new JPanel();
      low.setLayout(new FlowLayout());
      low.add(exitFolderText);
      low.add(exitFolder);
      low.add(exitButton);
      panel.add(low,c);
      panel.add(sign,c);
   }


   public static void main (String[] args){
      GraphicSign sign = new GraphicSign();
      sign.setVisible(true);
   }

   private static void showAlert(String s){
      JOptionPane.showMessageDialog(null, s, "Error", JOptionPane.ERROR_MESSAGE);
   }

   private void doSigning(DefaultListModel model, String exitFolder, boolean delete){
      try{
         TempelPlus.readConfig(new String[0]);
         if(sigFac==null)
            sigFac = ConfigManager.instance().getSignatureFactory();

         JPasswordField pwd = new JPasswordField(10);
         int action =-1;
         for(int i=0;i<model.size();i++){
            File file=(File) model.get(i);
            if (!check(exitFolder + "\\" + file.getName(), Config.getProps().getProperty(Config.FORMAT)))
               return;
         }
         if(!correctPin){
            while(action<0){
               action = JOptionPane.showConfirmDialog(null, pwd,"Enter pin",JOptionPane.OK_CANCEL_OPTION);
               pin= new String(pwd.getPassword());
            }
         }
         if(cert==null){
            try{
               //Muudame leveli taset et keerata logi kinni kui vale PIN on
               Logger localLogger = Logger.getLogger(DigiDocException.class);
               Level curr = localLogger.getLevel();
               localLogger.setLevel(Level.FATAL);
               cert = sigFac.getCertificate(0, pin);
               cert.checkValidity();
               localLogger.setLevel(curr);
            }catch (DigiDocException e){
               if(e.getCode()==DigiDocException.ERR_TOKEN_LOGIN){
                  showAlert("Incorrect PIN!");
                  return;
               }else{
                  showAlert(e.getMessage());
                  return;
               }
            }
            correctPin=true;
         }
         String[] roles = null;
         if(Config.getProps().getProperty(Config.ROLE)!=null){
            roles = new String[] { Config.getProps().getProperty(Config.ROLE) };
         }
         SignatureProductionPlace addr = new SignatureProductionPlace(Config.getProps().getProperty(Config.CITY),
               Config.getProps().getProperty(Config.STATE), Config.getProps().getProperty(Config.COUNTRY), Config
                     .getProps().getProperty(Config.POSTCODE));

         Util.checkCertificate(cert);// Kontrollime sertifikaati
         Util.initOSCPSerial(cert);
         for(int i=0;i<model.size();i++){
            File file=(File) model.get(i);
            SignedDoc sdoc;
            if(!(file.getName().endsWith("ddoc")||file.getName().endsWith("bdoc"))){
               sdoc = new SignedDoc(SignedDoc.FORMAT_DIGIDOC_XML, SignedDoc.VERSION_1_3);
               String mimeType = m.getContentType(file);
               sdoc.addDataFile(file, mimeType, DataFile.CONTENT_EMBEDDED_BASE64);
            } else {
               if (digFac == null)
                  digFac = ConfigManager.instance().getDigiDocFactory();
               sdoc = digFac.readSignedDoc(file.getAbsolutePath());
            }
            Signature sig = sdoc.prepareSignature(cert, roles, addr);
            byte[] sidigest = sig.calculateSignedInfoDigest();
            byte[] sigval = sigFac.sign(sidigest, 0, pin);
            sig.setSignatureValue(sigval);
            sig.getConfirmation();
            String fileName = exitFolder + "\\" + file.getName();
            if(!(file.getName().endsWith("ddoc")||file.getName().endsWith("bdoc"))){
               fileName = makeName(fileName, Config.getProps().getProperty(Config.FORMAT));
            }
            sdoc.writeToFile(new File(fileName));
            if (delete) {
               file.delete();
            }

         }
         JOptionPane.showMessageDialog(null, "All files signed!", "Info", JOptionPane.PLAIN_MESSAGE);
         model.clear();
      }catch (Exception e){
         showAlert(e.getMessage());
         e.printStackTrace();
         return;
      }
   }

   private static String makeName(String name,String ext) throws TempelPlusException{
      if(name.contains("."))
         name = name.substring(0,name.lastIndexOf("."));
      File f = new File(name+"."+ext);
      if(f.exists()){

      }
      return f.getAbsolutePath();
   }

   private static boolean check(String name,String ext) throws TempelPlusException{
      if(name.contains("."))
         name = name.substring(0,name.lastIndexOf("."));
      File f = new File(name+"."+ext);
      if(f.exists()){
         if(f.isDirectory()){
            showAlert("File already exists:"+f.getAbsolutePath());
            return false;
         }else{
            showAlert("File already exists:"+f.getAbsolutePath());
            return false;
         }
      }
      return true;
   }

}