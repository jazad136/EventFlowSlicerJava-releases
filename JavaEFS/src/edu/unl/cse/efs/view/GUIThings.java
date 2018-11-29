package edu.unl.cse.efs.view;

import javax.swing.*;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

import static edu.unl.cse.efs.view.DecorationsRunner.*;

import java.awt.Color;
import java.awt.Dimension;
import java.util.Arrays;
import java.util.Vector;

public class GUIThings {
	static JFrame frameInUse;
	static Dimension currentFrameDimension;
	private static JPanel topPanel;
	public static final int FRAME_BASE_SIZE_X = 600, FRAME_BASE_SIZE_Y = 500;
	
	private static JButton b;
	private static JTextField t;
	private static JRadioButton rb;
	private static JCheckBox ch;
	private static JTabbedPane p;
	private static JTable tb;
	private static JLabel tableLabel;
	
	private static JComboBox<String> cb;
	private static JMenu m;
	private static JMenuItem mi;
	private static JRadioButtonMenuItem rmi;
	private static JCheckBoxMenuItem cmi;
	
	public GUIThings()
	{
		topPanel = new JPanel();
		b = new JButton("Button");
		t = new JTextField("Text Field");
		rb = new JRadioButton("Radio Button");
		ch = new JCheckBox("Check Box");
		
		p = new JTabbedPane();
		JPanel[] panePanels = new JPanel[]{new JPanel(), new JPanel()};
		panePanels[0].add(new JLabel("With Content ........."));
		p.addTab("Tabbed Pane", panePanels[0]);
		
		tableLabel = new JLabel("Table");
		tb = new JTable(2, 2);
		tb.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		
		tb.getModel().setValueAt("Table Cell", 1, 1);
		tableLabel.setLabelFor(tb);
		cb = new JComboBox<>(new Vector<>(Arrays.asList(new String[]{"Combo Box"})));
		m = new JMenu("Menu");
		mi = new JMenuItem("Menu Item");
		rmi = new JRadioButtonMenuItem("Radio Menu Item");
		rmi.setSelected(true);
		cmi = new JCheckBoxMenuItem("Check Menu Item");
		cmi.setSelected(true);
	}
	public static void layout()
	{
		initialize(topPanel, 1, 9);
		editingStartOfColumn(0);
		place(b); // 1 button 
		hardenEdits();
		
		place(t); // 2 text field
		place(rb); // 3 radio button
		place(ch); // 4 check box
		place(cb); // 5 combo box
		place(tableLabel); // table label
		place(tb); // table 
		place(p); //  tabbed panel
		
		frameInUse.add(topPanel);
		JMenuBar mb = new JMenuBar();	
		m.add(mi);
		m.add(rmi);
		m.add(cmi);
		mb.add(m);
		frameInUse.setJMenuBar(mb);
	}
	public static JFrame setupFrame(int width, int height)
	{
		// setup the frame
		JFrame frame = new JFrame("Fitting Tool");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setPreferredSize(new Dimension(width, height));
		return frame;
	}
	public static void main(String[] args)
	{
		try {UIManager.setLookAndFeel(new NimbusLookAndFeel());} 
		catch(UnsupportedLookAndFeelException e) {}
		frameInUse = setupFrame(FRAME_BASE_SIZE_X, FRAME_BASE_SIZE_Y);
		new GUIThings();
		layout();
		show(frameInUse);
	}
	public static void show(final JFrame frame)
	{
		frameInUse = frame;
		SwingUtilities.invokeLater(new Runnable(){public void run(){
			frame.pack();
			frame.setVisible(true);
			currentFrameDimension = frame.getSize();
		}});
	}
}
