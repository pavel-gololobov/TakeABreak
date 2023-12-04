package ru.levap.takeabreak;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.ini4j.InvalidFileFormatException;
import org.ini4j.Wini;

public class MainWindow {

	private JFrame frmTakeABreak;
	private Thread workThread;
	private boolean terminated = false;
	private DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm");
	private JLabel lblNewLabel;
	private JPanel panel;
	int pX, pY;
	private JLabel lblX;
	
	static private HourMinute startOfWork;
	static private HourMinute endOfWork;
	static private HourMinute startOfLunch;
	static private HourMinute endOfLunch;
	static private int breakAtMinute;
	static private int shortWorkAtMinute;
	
	static private double locationX;
	static private double locationY;
	
	enum PartOfHour {
		UNKNOWN,
		LONG_WORK,
		SHORT_WORK,
		BREAK
	}
	
	/**
	 * Class for Hour and Minute pair
	 * 
	 * @author levap
	 *
	 */
	public class HourMinute {
		public int hour;
		public int minute;
		
		public HourMinute(String hour, String minute) {
			this.hour = Integer.parseInt(hour);
			this.minute = Integer.parseInt(minute);
		}
	}
	
	/**
	 * Convert text strint to HourMinute
	 * 
	 * @param hourMinuteString
	 * @return
	 */
	private HourMinute convertStringToHourMinute(String hourMinuteString) {
		if((hourMinuteString != null) && (!hourMinuteString.isEmpty())) {
			String[] timeSlices = hourMinuteString.trim().split(":");
			if(timeSlices.length == 2) {
				return new HourMinute(timeSlices[0], timeSlices[1]);
			}
		}
		return null;
	}

	/**
	 * Work thread
	 * 
	 * @author levap
	 *
	 */
	class WorkThread implements Runnable {
		private PartOfHour currentPartOfHour = PartOfHour.UNKNOWN;
		
		@Override
		public void run() {
	    	while(!terminated) {
	    		LocalDateTime timeNow = LocalDateTime.now().withNano(0);
	    		
	    		LocalDateTime startOfWorkTime = timeNow.withHour(startOfWork.hour)
	    				.withMinute(startOfWork.minute).withSecond(0);
	    		LocalDateTime endOfWorkTime = timeNow.withHour(endOfWork.hour)
	    				.withMinute(endOfWork.minute).withSecond(0);
	    		
	    		LocalDateTime startOfLunchTime = timeNow.withHour(startOfLunch.hour)
	    				.withMinute(startOfLunch.minute).withSecond(0);
	    		LocalDateTime endOfLunchTime = timeNow.withHour(endOfLunch.hour)
	    				.withMinute(endOfLunch.minute).withSecond(0);

	    		PartOfHour nowPartOfHour = PartOfHour.UNKNOWN;
	    		
	    		if((timeNow.isAfter(startOfWorkTime) && timeNow.isBefore(startOfLunchTime)) ||
	    				(timeNow.isBefore(endOfWorkTime) && timeNow.isAfter(endOfLunchTime))) {
	    			
	    			if(timeNow.getMinute() >= shortWorkAtMinute && timeNow.getMinute() < breakAtMinute) {
		    			nowPartOfHour = PartOfHour.SHORT_WORK;
		    		} else if (timeNow.getMinute() >= breakAtMinute && timeNow.getMinute() <= 59) {
		    			nowPartOfHour = PartOfHour.BREAK;
		    		} else {
		    			nowPartOfHour = PartOfHour.LONG_WORK;
		    		}
	    		} else {
	    			nowPartOfHour = PartOfHour.BREAK;
	    		}

	    		if(nowPartOfHour != currentPartOfHour) {
	    			currentPartOfHour = nowPartOfHour;
	    		}

	    		SwingUtilities.invokeLater(new Runnable() {
		    		public void run() {
		    			String timeString = timeNow.format(timeFormat);
		    			lblNewLabel.setText(timeString);
		    			
		    			Color color = Color.GREEN;
		    			if(currentPartOfHour == PartOfHour.SHORT_WORK) {
			    			color = Color.YELLOW;
			    		} else if (currentPartOfHour == PartOfHour.BREAK) {
			    			color = Color.RED;
			    		}
		    			panel.setBackground(color);
		    		}
	    		});
	    		
	    		try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
	    	}
		}
	}
	
	/**
	 * Launch the application.
	 * 
	 * @throws IOException 
	 * @throws InvalidFileFormatException 
	 */
	static public void main(String[] args) throws InvalidFileFormatException, IOException {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainWindow window = new MainWindow();
					window.frmTakeABreak.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}


	/**
	 * Create the application.
	 * 
	 * @throws IOException 
	 * @throws InvalidFileFormatException 
	 */
	public MainWindow() throws InvalidFileFormatException, IOException {
		Wini ini = new Wini(new File("settings.ini"));

		startOfWork = convertStringToHourMinute(ini.get("Scheduler", "startOfWork"));
		endOfWork = convertStringToHourMinute(ini.get("Scheduler", "endOfWork"));
		startOfLunch = convertStringToHourMinute(ini.get("Scheduler", "startOfLunch"));
		endOfLunch = convertStringToHourMinute(ini.get("Scheduler", "endOfLunch"));

        breakAtMinute = ini.get("Scheduler", "breakAtMinute", int.class);
        shortWorkAtMinute = ini.get("Scheduler", "shortWorkAtMinute", int.class);
        
        locationX = ini.get("Location", "locationX", double.class);
        locationY = ini.get("Location", "locationY", double.class);
        
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmTakeABreak = new JFrame();
		frmTakeABreak.setTitle("Take a Break");
		frmTakeABreak.setBounds(100, 100, 194, 66);
		frmTakeABreak.setLocation((int) locationX, (int) locationY);
		frmTakeABreak.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmTakeABreak.getContentPane().setLayout(new GridLayout(1, 0, 0, 0));
		frmTakeABreak.setAlwaysOnTop(true);
		frmTakeABreak.setUndecorated(true);
		frmTakeABreak.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		frmTakeABreak.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				terminated = true;
				try {
					workThread.join();
				} catch (InterruptedException e1) {
				}
			}
		});
		
		panel = new JPanel();
		panel.setBackground(Color.WHITE);
		frmTakeABreak.getContentPane().add(panel);
		panel.setLayout(null);
		
		lblNewLabel = new JLabel("----");
		lblNewLabel.setBackground(Color.WHITE);
		lblNewLabel.setFont(new Font("Consolas", Font.BOLD, 36));
		lblNewLabel.setBounds(12, 12, 100, 42);
		lblNewLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				pX = e.getX();
                pY = e.getY();
			}
		});
		lblNewLabel.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
            public void mouseDragged(MouseEvent me) {
            	frmTakeABreak.setLocation(frmTakeABreak.getLocation().x + me.getX() - pX, 
            			frmTakeABreak.getLocation().y + me.getY() - pY);
            }
        });
		panel.add(lblNewLabel);

		lblX = new JLabel("x");
		lblX.setBounds(165, 15, 29, 38);
		lblX.setFont(new Font("Consolas", Font.BOLD, 30));
		lblX.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					Wini ini = new Wini(new File("settings.ini"));
					Point location = frmTakeABreak.getLocation();
					ini.put("Location", "locationX", location.getX());
					ini.put("Location", "locationY", location.getY());
					ini.store();
				} catch (InvalidFileFormatException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				System.exit(0);
			}
		});
		panel.add(lblX);
		
		workThread = new Thread(new WorkThread(), "WorkThread");
		workThread.start();
	}
}
