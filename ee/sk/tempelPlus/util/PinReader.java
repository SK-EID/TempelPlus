package ee.sk.tempelPlus.util;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import org.apache.log4j.Logger;

import ee.sk.tempelPlus.TempelPlus;
import ee.sk.tempelPlus.util.console.TextDevice;

/** eToken pin code obtaining
 * @author Erik Kaju
 *
 */
public class PinReader {

	public Logger log = Logger.getLogger(PinReader.class);

	private final StringBuilder pin = new StringBuilder();

	public String obtainPin() throws Exception {
		String login = null;
		
		
		if (Config.getProps().getProperty(Config.PIN_E).equals(Config.PINC)) {
			TextDevice c = Config.getTextDevice();
			// Console c = System.console();
			if (c == null) {
				log.error("No console");
				throw new Exception();
			}
			c.printf("Enter pin: ");
			login = new String(c.readPassword());
		} else if (Config.getProps().getProperty(Config.PIN_E).equals(Config.PING)) {
			return getGraphicPin();
		} else if (Config.getProps().getProperty(Config.PIN_E).equals(Config.PINCFG)) {
			return getConfigPin();
		}
		
		return login;
	}

	private String getConfigPin() {
		log.info("Reading pin from TempelPlus configuration file");
		
		if(Config.getProps().getProperty(Config.PINFROMCONFIG).length() > 0){
			return Config.getProps().getProperty(Config.PINFROMCONFIG);
		}else{
			log.info("Pin configuration is set to: config, but it seems that \"pin\" parameter or its value is missing.");
			ee.sk.tempelPlus.TempelPlus.exit(1);
			System.exit(1);
		}
		return null;
	}

	private String getGraphicPin() throws Exception {
		final Frame frame = new Frame("Enter pin");

		// IF "x" is pressed
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				log.info("Operation cancelled by user");
				frame.dispose();
				TempelPlus.exit(0);
			}
		});
		
		Panel toolbarPanel = new Panel();
		toolbarPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		Label label = new Label("Enter pin:");
		Button b = new Button("OK");
		final TextField t = new TextField();
		final ActionListener buttonAction = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (t.getText().length() > 0) {
						pin.replace(0, pin.length(), t.getText());
						frame.dispose();
						synchronized (pin) {
							pin.notifyAll();
						}
				} else {
					log.warn("Pin textbox should not be empty! Enter pin and then click \"OK\" or press \"Enter\"");
				}
			}
		};
		t.setEchoChar('*');
		t.setMinimumSize(new Dimension(50, 20));
		t.setPreferredSize(new Dimension(50, 20));
		t.addKeyListener(new KeyListener() {
			public void keyTyped(KeyEvent e) {
				if (e.getKeyChar() == KeyEvent.VK_ENTER){
					buttonAction.actionPerformed(new ActionEvent(t, 1, null));
				}
				if (e.getKeyChar() == KeyEvent.VK_ESCAPE){
					System.out.println("Operation cancelled by user");
					TempelPlus.exit(0);
				}
			}

			public void keyReleased(KeyEvent e) {
			}

			public void keyPressed(KeyEvent e) {
			}
		});
		
		b.addActionListener(buttonAction);
		toolbarPanel.add(label);
		toolbarPanel.add(t);
		toolbarPanel.add(b);
		frame.add(toolbarPanel, BorderLayout.NORTH);
		frame.pack();
		frame.setVisible(true);
		
		synchronized (pin) {
			pin.wait();
		}
		
		return pin.toString();
	}
}
