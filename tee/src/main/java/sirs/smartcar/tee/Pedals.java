package sirs.smartcar.tee;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.security.InvalidKeyException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

public class Pedals {
	
	
	private TeeClient tee;

	
	public Pedals(TeeClient tee) {
		
		this.tee = tee;
	}

	public static void main(String[] args) throws Exception {
		TeeClient tee = new TeeClient();
		Pedals pedals = new Pedals(tee);
		pedals.usePedals();
	}
	
	
	public void usePedals() {
		
		buildInterface();
	}

	
	public void buildInterface() {
		
		JFrame frame = new JFrame("PEDALS");
		frame.setLayout(new BorderLayout());
		
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(1,2,0,0));
		
		JPanel interior = new JPanel();
		interior.setLayout(new FlowLayout(FlowLayout.LEADING, 30, 80));

		JButton speed_button = new JButton("SPEED");
		JButton brake_button = new JButton("BRAKE");
		speed_button.setPreferredSize(new Dimension(150,250));
		brake_button.setPreferredSize(new Dimension(150,250));

		interior.add(brake_button);
		interior.add(speed_button);
		panel.add(interior);
		frame.add(panel, BorderLayout.CENTER);

		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setSize(400, 450);
		frame.setResizable(false);
		frame.setVisible(true);
		
		callActionListeners(speed_button, brake_button);
		
	}
	
	public void callActionListeners(JButton speed_button, JButton brake_button) {
		speed_button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					tee.sendMessage("speed");
				} catch (InvalidKeyException e1) {
					e1.printStackTrace();
				}
				
			}
		});
		
		brake_button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					tee.sendMessage("brake");
				} catch (InvalidKeyException e1) {
					e1.printStackTrace();
				}
			
			}
		});
	}
	
	
	public void callCloseAL(JFrame frame) {
		frame.addWindowListener(new java.awt.event.WindowAdapter() {
		    @Override
		    public void windowClosing(java.awt.event.WindowEvent e) {
		    	tee.closeConnection();
		        e.getWindow().dispose();
		    }
		});
	}

}
