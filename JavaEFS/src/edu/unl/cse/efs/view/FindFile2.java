package edu.unl.cse.efs.view;
import java.awt.AWTKeyStroke;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FocusTraversalPolicy;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.awt.Dialog.ModalityType;

import javax.swing.*;

import edu.unl.cse.efs.view.EventFlowSlicerView.FileAction;
import edu.unl.cse.efs.view.EventFlowSlicerView.PrefsOption;

public class FindFile2 {
	public static JDialog displayerDialog;
	public static Font titleLblFont;
	public static Font otherLblFont;
	public static int CHOOSER_WIDTH = 550;
	public static int CHOOSER_HEIGHT = 200;
	public static boolean accepted;
	public static Dimension currentFrameDimension;

	public static void main(String[] args)
	{
		PrefsOption chosen = PrefsOption.LOAD;
		if(args.length > 0) {
			switch(args[0]) {
			case "load" 	: chosen = PrefsOption.LOAD; break;
			case "saveto" 	: chosen = PrefsOption.SAVE_TO; break;
			}
		}
		testShow(chosen);
	}
	private FindFile2(PrefsOption prefsType)
	{
		JLabel sampleLabel = new JLabel();
		titleLblFont = sampleLabel.getFont().deriveFont(14f);
		otherLblFont = sampleLabel.getFont().deriveFont(12f);
		if(prefsType == PrefsOption.LOAD) {
			new LoadContent();
			setupLoadContent();
		}
		if(prefsType == PrefsOption.SAVE_TO) {
			new SaveToContent();
			setupSaveContent();
		}
	}


	public void setupSaveContent()
	{
		SaveToContent.sizing();
		SaveToContent.layout();
		displayerDialog.add(SaveToContent.base);
	}
	public void setupLoadContent()
	{
		LoadContent.sizing();
		LoadContent.layout();
		LoadContent.traversal();
		displayerDialog.add(LoadContent.base);
	}

	public static void dialogSetupSteps(JFrame parentFrame)
	{
		parentFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		parentFrame.setPreferredSize(new Dimension(CHOOSER_WIDTH, CHOOSER_WIDTH));
		parentFrame.setMinimumSize(new Dimension(CHOOSER_WIDTH, CHOOSER_WIDTH));
		displayerDialog = new JDialog(parentFrame);
		displayerDialog.setPreferredSize(new Dimension(CHOOSER_WIDTH, CHOOSER_HEIGHT));
		displayerDialog.setModalityType(ModalityType.APPLICATION_MODAL);
		parentFrame.setLocation(0, 0);
		parentFrame.setVisible(true);
	}
	public static void showDialog(final JFrame parentFrame, PrefsOption prefsType)
	{
		accepted = false;
		dialogSetupSteps(parentFrame);
		new FindFile2(prefsType);
		displayerDialog.pack();
		displayerDialog.setLocationRelativeTo(parentFrame);
		currentFrameDimension = displayerDialog.getSize(); // at the latest possible state, get the size.
		displayerDialog.setVisible(true);
	}
	public static File getLoadedFile()
	{
		return null;
	}

	public static String constructedLoadFileString()
	{
		return LoadContent.chooserAction.getReturnedString();
	}

	public static void testShow(PrefsOption prefsType)
	{
		final JFrame parentFrame = new JFrame();

		parentFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		parentFrame.setPreferredSize(new Dimension(CHOOSER_WIDTH, CHOOSER_WIDTH));
		parentFrame.setMinimumSize(new Dimension(CHOOSER_WIDTH, CHOOSER_WIDTH));
		displayerDialog = new JDialog(parentFrame);
		displayerDialog.setPreferredSize(new Dimension(CHOOSER_WIDTH, CHOOSER_HEIGHT));
		displayerDialog.setModalityType(ModalityType.APPLICATION_MODAL);
		parentFrame.setLocation(0, 0);
		parentFrame.setVisible(true);
		new FindFile2(prefsType);
		SwingUtilities.invokeLater(new Runnable(){public void run() {
			displayerDialog.pack();
			displayerDialog.setLocationRelativeTo(parentFrame);
			currentFrameDimension = displayerDialog.getSize(); // at the latest possible state, get the size.
			displayerDialog.setVisible(true);
			System.exit(0);
		}});
	}


	public static class SaveToContent
	{
		public static String title;
		public static Box base;
		public static JLabel titleLbl, statementLbl;
		public static JButton wDirectoryB;
		public static JTextField wDirectoryT;
		public static JLabel wFileLbl;
		public static JTextField wFileT;
		public static JButton cancelB;
		public static JButton okB;
		public static JComponent[] allComps;
		public static Box highComps, lowComps, fcComps, bottomComps;

		public SaveToContent()
		{
			title = "Save Preferences File";
			statementLbl = new JLabel("Enter a file system location and click OK to save.");
			statementLbl.setFont(otherLblFont);
			wDirectoryB = new JButton("Select Directory");
			wFileLbl = new JLabel("Provide a filename.");
			wDirectoryT = new JTextField();
			wFileT = new JTextField();
			cancelB = new JButton("Cancel Save");
			okB = new JButton("OK");
			highComps = new Box(BoxLayout.PAGE_AXIS);
			lowComps = new Box(BoxLayout.PAGE_AXIS);
			fcComps = new Box(BoxLayout.LINE_AXIS);
			bottomComps = new Box(BoxLayout.LINE_AXIS);
			allComps = new JComponent[]{statementLbl, wDirectoryB, wDirectoryT, wFileLbl, wFileT, cancelB, okB,
					highComps, lowComps, fcComps, bottomComps};
			base = new Box(BoxLayout.PAGE_AXIS);
		}

		public static void sizing()
		{
			wDirectoryT.setPreferredSize(new Dimension(CHOOSER_WIDTH / 2 - 20, wDirectoryT.getPreferredSize().height));
			wDirectoryT.setMinimumSize(new Dimension(CHOOSER_WIDTH / 2 - 20, wDirectoryT.getMinimumSize().height));
			wDirectoryT.setMaximumSize(new Dimension(CHOOSER_WIDTH / 2 - 20, wDirectoryT.getMinimumSize().height));
			wFileT.setPreferredSize(new Dimension(CHOOSER_WIDTH / 2 - 20, wFileT.getPreferredSize().height));
			wFileT.setMinimumSize(new Dimension(CHOOSER_WIDTH / 2 - 20, wFileT.getMinimumSize().height));
			wFileT.setMaximumSize(new Dimension(CHOOSER_WIDTH / 2 - 20, wFileT.getMinimumSize().height));
		}

		private static void alignments()
		{
			for(JComponent e : allComps) {
				e.setAlignmentX(JComponent.CENTER_ALIGNMENT);
				e.setAlignmentY(JComponent.TOP_ALIGNMENT);
			}
		}

		public static void layout()
		{
			alignments();
			statementLbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
			base.add(statementLbl);

			highComps.add(wDirectoryB);
			highComps.add(wFileLbl);

			lowComps.add(wDirectoryT);
			lowComps.add(wFileT);

			fcComps.add(highComps);
			fcComps.add(lowComps);

			base.add(fcComps);

			bottomComps.add(cancelB);
			bottomComps.add(okB);
			base.setBorder(BorderFactory.createEmptyBorder(40, 20, 20, 20));
			base.add(bottomComps);
		}
	}
	public static class LoadContent
	{
		public static Box base;
		public static JLabel titleLbl;
		public static JLabel statementLbl;
		public static JLabel choiceLbl;
		public static JLabel fileLbl;
		public static JLabel wFileLbl;
		public static JLabel wDirectoryLbl;
		public static JButton loadB;
		public static JButton cancelB;
		public static JTextArea wDirectoryT;
		public static JTextArea wFileT;
		public static JComponent[] allComps;
		public static JTextArea[] areas;
		public static FileAction chooserAction;
		public static Box titleComps, highComps, lowComps, fcComps, confirmComps;
		public static FocusTraversalPolicy ftp;
		public LoadContent()
		{
			titleLbl = new JLabel("Information:");
			titleLbl.setFont(titleLblFont);
			statementLbl = new JLabel("epreferences.xml was not found in the working directory.");
			statementLbl.setFont(otherLblFont);
			choiceLbl = new JLabel("Choose whether to load preferences from a different location or cancel.");
			choiceLbl.setFont(otherLblFont);
			loadB = new JButton("Load New");
			cancelB = new JButton("Cancel");
			wDirectoryLbl = new JLabel("Selected Directory");
			wDirectoryT = new JTextArea(1, 15);
			wDirectoryT.getAccessibleContext().setAccessibleName("loadWorkingDir");
			wFileLbl = new JLabel("Selected Filename");
			wFileT = new JTextArea(1, 15);
			wFileT.getAccessibleContext().setAccessibleName("loadWorkingFile");
			titleComps = new Box(BoxLayout.PAGE_AXIS);
			highComps = new Box(BoxLayout.PAGE_AXIS);
			lowComps = new Box(BoxLayout.PAGE_AXIS);
			fcComps = new Box(BoxLayout.LINE_AXIS);
			confirmComps = new Box(BoxLayout.LINE_AXIS);
			base = new Box(BoxLayout.PAGE_AXIS);
			allComps = new JComponent[]{titleLbl, statementLbl, choiceLbl, loadB, cancelB};
			areas = new JTextArea[]{wFileT, wDirectoryT};
			chooserAction = new EventFlowSlicerView.FileAction(wFileT, true, true);
		}
		public static void traversal()
		{
			for(int i = 0; i < areas.length; i++) {
				Set<AWTKeyStroke> fset = new HashSet<AWTKeyStroke>( areas[i].getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS ) );
		        fset.add( KeyStroke.getKeyStroke( "TAB" ));
		        fset.add(KeyStroke.getKeyStroke( "ENTER" ));
		        areas[i].setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, fset );

		        Set<AWTKeyStroke> bset = new HashSet<AWTKeyStroke>( areas[i].getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS ) );
		        bset.add( KeyStroke.getKeyStroke( "shift TAB" ));
		        bset.add( KeyStroke.getKeyStroke( "shift ENTER" ));
		        areas[i].setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, bset );
			}
			ftp = new FocusTraversalPolicy(){
				final ArrayList<Component> order =
						new ArrayList<Component>(Arrays.asList(areas));
				@Override
				public Component getDefaultComponent(Container aContainer) {return areas[0];}
				@Override
				public Component getComponentBefore(Container aContainer, Component aComponent) {
					int idx = order.indexOf(aComponent)-1;
					if(idx < 0)
						idx = areas.length-1;
					return order.get(idx);
				}
				@Override
				public Component getComponentAfter(Container focusCycleRoot, Component aComponent)
				{
					int idx = (order.indexOf(aComponent)+1)%order.size();
					return order.get(idx);
				}
				@Override
				public Component getFirstComponent(Container aContainer) {return areas[0];}
				@Override
				public Component getLastComponent(Container aContainer) {return areas[areas.length-1];}
			};
		}
		public static void sizing()
		{
			wDirectoryT.setPreferredSize(new Dimension(CHOOSER_WIDTH / 2 - 20, wDirectoryT.getPreferredSize().height));
			wDirectoryT.setMinimumSize(new Dimension(CHOOSER_WIDTH / 2 - 20, wDirectoryT.getMinimumSize().height));
			wDirectoryT.setMaximumSize(new Dimension(CHOOSER_WIDTH / 2 - 20, wDirectoryT.getMinimumSize().height));
			wFileT.setPreferredSize(new Dimension(CHOOSER_WIDTH / 2 - 20, wFileT.getPreferredSize().height));
			wFileT.setMinimumSize(new Dimension(CHOOSER_WIDTH / 2 - 20, wFileT.getMinimumSize().height));
			wFileT.setMaximumSize(new Dimension(CHOOSER_WIDTH / 2 - 20, wFileT.getMinimumSize().height));
			titleComps.setMaximumSize(new Dimension(Integer.MAX_VALUE, titleComps.getMaximumSize().height));
			titleComps.setPreferredSize(new Dimension(Integer.MAX_VALUE, titleComps.getMaximumSize().height));
		}
		private static void alignments()
		{
			for(JComponent e : allComps) {
				e.setAlignmentX(JComponent.CENTER_ALIGNMENT);
				e.setAlignmentY(JComponent.TOP_ALIGNMENT);
			}
			loadB.setAlignmentY(JComponent.RIGHT_ALIGNMENT);
			cancelB.setAlignmentY(JComponent.RIGHT_ALIGNMENT);
			loadB.setAlignmentY(JComponent.TOP_ALIGNMENT);
			cancelB.setAlignmentY(JComponent.TOP_ALIGNMENT);

		}
		// when I say "meh alignment" I mean the alignments were assigned without much care, and are not required.
		public static void layout()
		{
			alignments();
			titleLbl.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
			titleComps.add(titleLbl);
			statementLbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

			base.add(titleComps);
			base.add(statementLbl);
			base.add(choiceLbl);

			// load button, cancel button.
			confirmComps.add(loadB);
			confirmComps.add(cancelB);

			// the middle menu
			highComps.add(wDirectoryLbl);
			highComps.add(wFileLbl);

			lowComps.add(wDirectoryT);
			lowComps.add(wFileT);

			fcComps.add(highComps);
			fcComps.add(lowComps);

			base.add(fcComps);
		}
		public static void doBindings()
		{
			loadB.addActionListener(chooserAction);
			cancelB.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent ae) {
				displayerDialog.setVisible(false);
			}});
		}
	}
	public static void updateField(String fieldName, String newContent)
	{
		JTextArea updateArea = null;
		String updateString = "";
		switch(fieldName) {
			case "loadWorkingDir": {
				updateArea = LoadContent.wDirectoryT;
			} break;
			case "loadWorkingFile": {
				updateArea = LoadContent.wFileT;
			} break;
		}
		final JTextArea selectArea = updateArea;
		final String pathText = updateString;
		SwingUtilities.invokeLater(new Runnable(){public void run(){
			selectArea.setText(pathText);
			selectArea.setCaretPosition(pathText.length());
		}});
	}
}
