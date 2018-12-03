/*******************************************************************************
 *    Copyright (c) 2018 Jonathan A. Saddler
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *    
 *    Contributors:
 *     Jonathan A. Saddler - initial API and implementation
 *******************************************************************************/
package edu.unl.cse.efs.view;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.FocusTraversalPolicy;
import java.awt.KeyboardFocusManager;
import java.awt.SecondaryLoop;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.swing.*;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import javax.swing.undo.UndoableEditSupport;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.bind.Unmarshaller;

import edu.umd.cs.guitar.model.XMLHandler;
import edu.umd.cs.guitar.model.data.EFG;
import edu.umd.cs.guitar.model.data.GUIStructure;
import edu.umd.cs.guitar.model.data.TaskList;
import edu.umd.cs.guitar.ripper.JFCRipperConfiguration;
import edu.unl.cse.efs.util.ReadArguments;
import edu.unl.cse.efs.ApplicationData;
import edu.unl.cse.efs.LauncherData;
import edu.unl.cse.efs.app.EventFlowSlicer;
import edu.unl.cse.efs.java.JavaLaunchApplication;
import edu.unl.cse.efs.replay.JFCReplayerConfigurationEFS;
import edu.unl.cse.efs.ripper.JFCRipperConfigurationEFS;
import edu.unl.cse.efs.tools.ArrayTools;
import edu.unl.cse.efs.tools.PathConformance;

import static edu.unl.cse.efs.view.DecorationsRunner.*;
import static java.awt.Component.*;

import java.awt.AWTKeyStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;

public class EventFlowSlicerView {
	public static JFrame frameInUse;
	public static LookAndFeel lookAndFeel;
	public static final int FRAME_BASE_SIZE_X = 600, FRAME_BASE_SIZE_Y = 685;
	public static final float HEADER_TEXT_FONT = 18;
	public static final Dimension HEADER_SPACING = new Dimension(0, 30);
	public static final Dimension BORDER_SPACING = new Dimension(0, 10);
	public static final float SUBHEADER_TEXT_FONT = 14;
	public static Dimension currentFrameDimension = new Dimension();
	public static XMLHandler handler = new XMLHandler();
	public static ApplicationData ad;
	public static LauncherData ld;
	public static EventFlowSlicerController ac;
	private static UndoManager undoM;
	private static UndoActionListener undoActions;


	public EventFlowSlicerView(ApplicationData someAD)
	{
		lookAndFeel = UIManager.getLookAndFeel();
		undoActions = new UndoActionListener();
		new MBContent();
		new TopContent();
		new BaseContent();
		ad = someAD;
		ac = new EventFlowSlicerController(ad);
		ld = new LauncherData(EventFlowSlicer.DEFAULT_JAVA_RMI_PORT);
		undoM = new UndoManager();
	}

	public EventFlowSlicerView(ApplicationData someAD, LauncherData someLD)
	{
		this(someAD);
		ld = someLD;
	}

	public static class FileChooserAction extends AbstractAction
	{
		private JTextComponent theArea;
		private FindFile chooser;
		private boolean findFilesOnly;
		private String fieldName;

		public FileChooserAction(JTextComponent pasteArea, boolean findFilesOnly)
		{
			super("...");
			this.theArea = pasteArea;
			this.findFilesOnly = findFilesOnly;
			this.fieldName = pasteArea.getAccessibleContext().getAccessibleName();
			chooser = new FindFile(new File(System.getProperty("user.dir")));
		}

		public void actionPerformed(ActionEvent ae)
		{
			if(!theArea.getText().isEmpty()) {
				File currentFile = new File(theArea.getText());
				if(currentFile.exists())
					chooser.setDefaultDirectory(new File(theArea.getText()));
			}
			String newPath = chooser.launch(findFilesOnly, frameInUse);
			if(!newPath.isEmpty())
				updateField(fieldName, newPath);


		}
	}
	public static class UndoActionListener extends AbstractAction implements UndoableEditListener
	{
		PlainDocument lastField;
		UndoableEdit lastEdit;
		public UndoActionListener()
		{
			super("Undo");
			lastField = null;
		}
		@Override
		public void actionPerformed(ActionEvent e)
		{
			doUndo();
		}
		public static void doUndo()
		{
			if(undoM.canUndo())
				undoM.undo();
			if(!undoM.canUndo())
				MBContent.undo.setEnabled(false);
		}
		@Override
		public void undoableEditHappened(final UndoableEditEvent e)
		{
			boolean added = false;
			if(e.getSource() instanceof PlainDocument) {
				PlainDocument next = (PlainDocument)e.getSource();
				if(lastField != null && next == lastField) {
					undoM.addEdit(new UndoableEdit() {
						@Override
						public void undo() throws CannotUndoException {e.getEdit().undo();}
						@Override public boolean canUndo() {return e.getEdit().canUndo();}
						@Override public void redo() throws CannotRedoException {e.getEdit().redo();}
						@Override public boolean canRedo() {return e.getEdit().canRedo();}
						@Override public void die() {e.getEdit().die();}
						@Override public boolean addEdit(UndoableEdit anEdit) {return e.getEdit().addEdit(anEdit);}
						@Override public boolean replaceEdit(UndoableEdit anEdit) {return e.getEdit().replaceEdit(anEdit);}
						// the important call.
						@Override public boolean isSignificant() {return false;}
						@Override public String getPresentationName() {return e.getEdit().getPresentationName();}
						@Override public String getUndoPresentationName() {return e.getEdit().getUndoPresentationName();}
						@Override public String getRedoPresentationName() {return e.getEdit().getRedoPresentationName();}
					});
					added = true;
				}
				else {
					lastField = next;
					lastEdit = e.getEdit();
				}
			}
			if(!added) {
				undoM.addEdit(e.getEdit());
				MBContent.undo.setEnabled(true);
			}
		}
	}
	public static class PasteAction extends AbstractAction
	{
		private JTextComponent theArea;
		public PasteAction(JTextComponent pasteArea)
		{
			this.theArea = pasteArea;
		}
		@Override
		public void actionPerformed(ActionEvent ae) {
			try {
				theArea.setText(getPasteText());
			} catch(UnsupportedFlavorException | IOException e) {
				return;
			}
		}

		public static String getPasteText() throws UnsupportedFlavorException, IOException
		{
			Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
		    Transferable t = c.getContents(frameInUse);
		    if (t == null)
		        return "";
		    return "" + t.getTransferData(DataFlavor.stringFlavor);
		}
		public static void pasteInSelectedTextArea()
		{

		}
	}
	public static class TopContent
	{
		public static JPanel topPanel;
		public static JLabel banner;
		public static JButton startB, stopB;
		public static JButton editConsB, ripB;
		public static JButton genTB, startRB;
		public static JButton selHelp;
		public static JTextField selT;

		public static JLabel selLabel;
		public static JPanel capGroup, rulGroup, ripGroup, genGroup, repGroup;

		public TopContent() {
			banner = new JLabel("EventFlowSlicer");
//			banner2 = new JLabel("GUI Tool");
			banner.setFont(banner.getFont().deriveFont(HEADER_TEXT_FONT));
//			banner2.setFont(banner.getFont().deriveFont(HEADER_TEXT_FONT-2));
			banner.setAlignmentX(CENTER_ALIGNMENT);

			capGroup = new JPanel();
			capGroup.setBorder(BorderFactory.createTitledBorder("1. Capture"));
			rulGroup = new JPanel();
			rulGroup.setBorder(BorderFactory.createTitledBorder("2. Constraints"));
			ripGroup = new JPanel();
			ripGroup.setBorder(BorderFactory.createTitledBorder("3. Rip"));
			genGroup = new JPanel();
			genGroup.setBorder(BorderFactory.createTitledBorder("4. Generate"));
			repGroup = new JPanel();
			repGroup.setBorder(BorderFactory.createTitledBorder("5. Replay"));

			startB = new JButton("Start");
			stopB = new JButton("Stop");
			editConsB = new JButton("Edit Constraints");
			ripB = new JButton("Rip Application");
			genTB = new JButton("Generate Test Cases");
			startRB = new JButton("Start");
			selT = new JTextField("", 5);

			selLabel = new JLabel("<html>Replay these test cases:<br>(leave blank to select all)</html>");
			selLabel.setFont(selLabel.getFont().deriveFont(11f));

			selHelp = new JButton("Help");
		}

		public static void layout()
		{
			topPanel = initializeNew(3, 3);
			column1Setup();
			column2Setup();
			column3Setup();
		}
		public static void column1Setup()
		{
			editingStartOfColumn(0);
			capGroup.setLayout(new BoxLayout(capGroup, BoxLayout.PAGE_AXIS));
			capGroup.add(startB);
			capGroup.add(stopB);
			topAnchor();
			place(capGroup);
		}
		public static void column2Setup()
		{
			editingStartOfColumn(1);
			// settings
			JPanel twoGroups = new JPanel();
			twoGroups.setLayout(new BoxLayout(twoGroups, BoxLayout.PAGE_AXIS));
			ripGroup.setLayout(new BoxLayout(ripGroup, BoxLayout.PAGE_AXIS));
			ripGroup.add(ripB);
			rulGroup.setLayout(new BoxLayout(rulGroup, BoxLayout.PAGE_AXIS));
			rulGroup.add(editConsB);
			// layout
			twoGroups.add(rulGroup);
			twoGroups.add(Box.createVerticalStrut(20));
			twoGroups.add(ripGroup);
			place(twoGroups);
		}
		public static void column3Setup()
		{
			editingStartOfColumn(2);
			JPanel twoGroups = new JPanel();
			twoGroups.setLayout(new BoxLayout(twoGroups, BoxLayout.PAGE_AXIS));
			genGroup.setLayout(new BoxLayout(genGroup, BoxLayout.PAGE_AXIS));
			genGroup.add(genTB);

			repGroup.setLayout(new BoxLayout(repGroup,BoxLayout.PAGE_AXIS));
			repGroup.add(selLabel);
//			Dimension maxSize = selT.getMaximumSize();
//			maxSize.width = 150;
//			selT.setMaximumSize(maxSize);
			repGroup.add(selT);
			repGroup.add(startRB);
			twoGroups.add(genGroup);
			twoGroups.add(repGroup);
			place(twoGroups);
		}
	}
	public static class Test {
		public static void main(final String[] args)
		{
			if(args.length == 0 || args[0].equals("test")) {
				System.out.println("Testing...");
				JFrame frame = setupFrame(FRAME_BASE_SIZE_X, FRAME_BASE_SIZE_Y);
				new SetupView(frame, new ApplicationData());
				show(frame);
			}
		}
	}
	public static void show(final JFrame frame)
	{
		frameInUse = frame;
		TopContent.startB.setEnabled(true);
		TopContent.stopB.setEnabled(false);
		TopContent.startB.addActionListener(new CaptureAction(frame, true));
		TopContent.stopB.addActionListener(new CaptureAction(frame, false));
		TopContent.editConsB.addActionListener(new ConstraintsAction(frame));
		TopContent.ripB.addActionListener(new RipAction(frame));
		TopContent.genTB.addActionListener(new GenerateAction(frame));
		TopContent.startRB.addActionListener(new ReplayAction(frame));

		SwingUtilities.invokeLater(new Runnable(){public void run(){
			frame.pack();
			frame.setVisible(true);
			currentFrameDimension = frame.getSize();
		}});
	}
	/**
	 * Make sure to call on the EDT
	 */
	public static void minimizeFrame()
	{
		frameInUse.setState(JFrame.ICONIFIED);
	}

	public static JFrame setupFrame(int width, int height)
	{
		// setup the frame
		JFrame frame = new JFrame("EventFlowSlicer");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setPreferredSize(new Dimension(width, height));
		return frame;
	}


	public static void setupContent(JFrame frameToUse)
	{
		frameToUse.getContentPane().setLayout(new BoxLayout(frameToUse.getContentPane(), BoxLayout.PAGE_AXIS));
		// the menubar
		MBContent.layout();
		frameToUse.setJMenuBar(MBContent.menuBar);
		frameToUse.add(Box.createRigidArea(HEADER_SPACING));
		frameToUse.add(TopContent.banner);
//		frameToUse.add(TopContent.banner2);
		// top buttons
		TopContent.layout();
		hardenEdits();
		frameToUse.add(TopContent.topPanel);
		// bottom fields
		BaseContent.layout();
		hardenEdits();
		frameToUse.add(BaseContent.basePanel);
		frameToUse.add(Box.createRigidArea(BORDER_SPACING));
	}

	public static class ShutdownAction extends AbstractAction
	{
		int shutdownCode;
		public ShutdownAction(int code, String displayString)
		{
			super(displayString);
			shutdownCode = code;
		}
		public void actionPerformed(ActionEvent ae)
		{
			System.exit(shutdownCode);
		}
	}
	public static class MBContent
	{
		public static JMenuBar menuBar;
		public static JMenu edit;
		public static JMenuItem paste, exit, undo;
		public MBContent()
		{
			edit = new JMenu("Edit");
			exit = new JMenuItem("Exit");
			paste = new JMenuItem(new DefaultEditorKit.PasteAction());
			paste.setText("Paste");
			exit = new JMenuItem(new ShutdownAction(0, "Exit"));
			undo = new JMenuItem(undoActions);
			undo.setEnabled(false);

			paste.setMnemonic(KeyEvent.VK_P);
			exit.setMnemonic(KeyEvent.VK_E);
			undo.setMnemonic(KeyEvent.VK_U);
		}
		public static void layout()
		{
			menuBar = new JMenuBar();
			edit.add(paste);
			edit.add(exit);
			edit.add(undo);
			menuBar.add(edit);

		}

	}

	public static class BaseContent
	{
		public static JLabel[] areaLabel;
		public static JTextArea[] areas;
		public static JTextArea appFilePath;
		public static JTextArea customMainClassString;
		public static JTextArea argsAppFilePath, argsVMFilePath;
		public static JTextArea outputDirectoryPath;
		public static JTextArea constraintsFilePath, eventFlowGraphFilePath, guiStructureFilePath;
		public static JTextArea ripConfigurationFilePath;
		public static JTextArea testCaseDirectoryPath;
		public static JButton[] chooserButtons;
		private static FocusTraversalPolicy ftp;
		private static JPanel basePanel;
		public BaseContent()
		{
			areaLabel = new JLabel[10];
			areaLabel[0] = new JLabel("<html><b>Output Directory</b></html>");
			areaLabel[1] = new JLabel("<html><b>Application File</b></html>");
			areaLabel[2] = new JLabel("<html><b>Custom Main Class Name</b></html>");
			areaLabel[3] = new JLabel("<html><b>App Arguments File</b></html>");
			areaLabel[4] = new JLabel("<html><b>VM Arguments File</b></html>");
			areaLabel[5] = new JLabel("<html><b>Constraints File</b></html>");
			areaLabel[6] = new JLabel("<html><b>Rip Configuration File</b></html>");
			areaLabel[7] = new JLabel("<html><b>Input GUI Structure File</b></html>");
			areaLabel[8] = new JLabel("<html><b>Input Event Flow Graph File</b></html>");
			areaLabel[9] = new JLabel("<html><b>Input Test Case Directory</b></html>");
			// areas
			outputDirectoryPath = new JTextArea(1, 15);
			outputDirectoryPath.getAccessibleContext().setAccessibleName("outputDirectoryPath");
			appFilePath = new JTextArea(1, 15);
			appFilePath.getAccessibleContext().setAccessibleName("appFilePath");
			customMainClassString = new JTextArea(1, 15);
			customMainClassString.getAccessibleContext().setAccessibleName("customMainClassString");
			argsAppFilePath = new JTextArea(1, 15);
			argsAppFilePath.getAccessibleContext().setAccessibleName("argsAppFilePath");
			argsVMFilePath = new JTextArea(1, 15);
			argsVMFilePath.getAccessibleContext().setAccessibleName("argsVMFilePath");
			constraintsFilePath = new JTextArea(1, 15);
			constraintsFilePath.getAccessibleContext().setAccessibleName("constraintsFilePath");
			eventFlowGraphFilePath = new JTextArea(1, 15);
			eventFlowGraphFilePath.getAccessibleContext().setAccessibleName("eventFlowGraphFilePath");
			ripConfigurationFilePath = new JTextArea(1, 15);
			ripConfigurationFilePath.getAccessibleContext().setAccessibleName("ripConfigurationFilePath");
			guiStructureFilePath = new JTextArea(1, 15);
			guiStructureFilePath.getAccessibleContext().setAccessibleName("guiStructureFilePath");
			testCaseDirectoryPath = new JTextArea(1, 15);
			testCaseDirectoryPath.getAccessibleContext().setAccessibleName("testCaseDirectoryPath");
			areas = new JTextArea[]{
					outputDirectoryPath,
					appFilePath,
					customMainClassString,
					argsAppFilePath,
					argsVMFilePath,
					constraintsFilePath,
					ripConfigurationFilePath,
					guiStructureFilePath,
					eventFlowGraphFilePath,
					testCaseDirectoryPath};

			ftp = new FocusTraversalPolicy(){
				final ArrayList<Component> order =
						new ArrayList<Component>(Arrays.asList(areas));
				@Override
				public Component getDefaultComponent(Container aContainer) {return outputDirectoryPath;}
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

			//buttons
			chooserButtons = new JButton[areas.length];
			chooserButtons[0] = new JButton(new FileChooserAction(areas[0], false));
			chooserButtons[areas.length-1] = new JButton(new FileChooserAction(areas[0], false));
			for(int i = 1; i < areas.length-1; i++)
				if(areas[i] != customMainClassString)
					chooserButtons[i] = new JButton(new FileChooserAction(areas[i], true));


		}
		public static void layout()
		{
			basePanel = initializeNew(3, 10);
			basePanel.setFocusCycleRoot(true);
			basePanel.setMaximumSize(new Dimension(FRAME_BASE_SIZE_X, 5*FRAME_BASE_SIZE_Y/12));
			basePanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
			column1Setup();
			column2Setup();
			column3Setup();
			basePanel.setFocusTraversalPolicy(ftp);
		}
		private static void column1Setup()
		{
			editingStartOfColumn(0);
			for(int i = 0; i < areaLabel.length; i++)
				place(areaLabel[i]);
		}
		private static void column2Setup()
		{
			editingStartOfColumn(1);

			for(int i = 0; i < areas.length; i++) {
				areas[i].getDocument().addUndoableEditListener(undoActions);
				/**
				 * Thanks goes out to:
				 * http://stackoverflow.com/questions/5042429/how-can-i-modify-the-behavior-of-the-tab-key-in-a-jtextarea
				 */
//				areas[i].setAlignmentX(LEFT_ALIGNMENT);
				JScrollPane sp = new JScrollPane(areas[i]);
				sp.getHorizontalScrollBar().setFocusable(false);
				sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
				sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

				Set<AWTKeyStroke> set = new HashSet<AWTKeyStroke>( areas[i].getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS ) );
		        set.add( KeyStroke.getKeyStroke( "TAB" ));
		        set.add(KeyStroke.getKeyStroke( "ENTER" ));
		        areas[i].setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, set );

		        set = new HashSet<AWTKeyStroke>( areas[i].getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS ) );
		        set.add( KeyStroke.getKeyStroke( "shift TAB" ));
		        set.add( KeyStroke.getKeyStroke( "shift ENTER" ));
		        areas[i].setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, set );

		        /**
		         * Thanks goes out to:
		         * http://docs.oracle.com/javase/tutorial/uiswing/components/generaltext.html
		         */
//		        final JTextArea theArea = areas[i];
//		        InputMap im = areas[i].getInputMap();
//		        ActionMap am = areas[i].getActionMap();
//		        im.put(KeyStroke.getKeyStroke("ctrl Z"), "undoNow"+i);
//		        am.put("undoNow"+i, new AbstractAction(){public void actionPerformed(ActionEvent ae){
//
//		        }});
				place(sp);
			}
		}
		private static void column3Setup()
		{
			editingStartOfColumn(2);
			for(int i = 0; i < chooserButtons.length; i++)
				if(chooserButtons[i] == null)
					jumpRowOrColumn();
				else {
//					chooserButtons[i].setAlignmentX(LEFT_ALIGNMENT);
					place(chooserButtons[i]);
				}
		}

	}
	public static class SetupView
	{

		public SetupView(JFrame frameToUse, ApplicationData ad, LauncherData ld)
		{
			new EventFlowSlicerView(ad, ld);
			setupContent(frameToUse);
		}
		public SetupView(JFrame frameToUse, ApplicationData ad)
		{
			new EventFlowSlicerView(ad);
			setupContent(frameToUse);
		}
	}

	/**
	 * The field will always be updated to a long path because ApplicationData is
	 * set to only handle full paths at this time, and the resulting field is what
	 * is reported by application data.
	 *
	 * An empty string passed here will result in the last thing saved being loaded
	 * to be displayed in the field.
	 * @param fieldName
	 * @param newContent
	 */
	public static void updateField(String fieldName, String newContent)
	{
		JTextArea updateArea = null;
		String updateString = "";
		switch(fieldName) {
		case "appFilePath": {
			if(!newContent.isEmpty())
				ad.setAppFilePath(newContent);
			updateString = ad.getAppFile().getAbsolutePath();
			updateArea = BaseContent.appFilePath;
		}
break;  case "outputDirectoryPath": {// output directory is handled specially
			if(!newContent.isEmpty()) {
				ad.setOutputDirectory(newContent);
				updateString = new File(newContent).getAbsolutePath();
			}
			else {
				String outputText = ad.getOutputDirectory().getAbsolutePath();
				String dFiller = ad.getSubdirectoryFiller();
				updateString = outputText.replace(dFiller, "");
			}
			updateArea = BaseContent.outputDirectoryPath;
		}
break;	case "customMainClassString": {
			if(!newContent.isEmpty())
				ad.setCustomMainClass(newContent);
			updateString = ad.getCustomMainClass();
			updateArea = BaseContent.customMainClassString;
        }
break;	case "argsAppFilePath": {
			if(!newContent.isEmpty())
				ad.setArgumentsAppFile(newContent);
			updateString = ad.getArgumentsAppFile().getAbsolutePath();
			updateArea = BaseContent.argsAppFilePath;
        }
break;	case "argsVMFilePath": {
			if(!newContent.isEmpty())
				ad.setArgumentsVMFile(newContent);
			updateString = ad.getArgumentsVMFile().getAbsolutePath();
			updateArea = BaseContent.argsVMFilePath;
		}
break;	case "constraintsFilePath": {
			if(!newContent.isEmpty())
				ad.setWorkingTaskListFile(newContent);
			updateArea = BaseContent.constraintsFilePath;
			updateString = ad.getWorkingTaskListFile().getAbsolutePath();
		}
break;	case "guiStructureFilePath": {
			if(!newContent.isEmpty())
				ad.setWorkingGUIFile(newContent);
			updateArea = BaseContent.guiStructureFilePath;
			updateString = ad.getWorkingGUIFile().getAbsolutePath();
		}
break;	case "eventFlowGraphFilePath": {
			if(!newContent.isEmpty())
				ad.setWorkingEFGFile(newContent);
			updateArea = BaseContent.eventFlowGraphFilePath;
			updateString = ad.getWorkingEFGFile().getAbsolutePath();
		}
break;	case "testCaseDirectoryPath": {
			if(!newContent.isEmpty())
				ad.setWorkingTestCaseDirectory(newContent);
			updateArea = BaseContent.testCaseDirectoryPath;
			updateString = ad.getWorkingTestCaseDirectory().getAbsolutePath();
		}
break;	case "ripConfigurationFilePath": {
			if(!newContent.isEmpty())
				ad.setRipConfigurationFile(newContent);
			updateArea = BaseContent.ripConfigurationFilePath;
			updateString = ad.getRipConfigurationFile().getAbsolutePath();
		}}

		final JTextArea selectArea = updateArea;
		final String pathText = updateString;
		SwingUtilities.invokeLater(new Runnable(){public void run(){
			selectArea.setText(pathText);
			selectArea.setCaretPosition(pathText.length());
		}});
	}
	public static void updateField(String fieldName)
	{
		updateField(fieldName, "");
	}
	/**
	 * These methods require that an ApplicationData be set in this eventflowslicer view first.
	 * @param appFile
	 */
	public static void updateAppFile()
	{
		final String pathText = ad.getAppFile().getAbsolutePath();
		SwingUtilities.invokeLater(new Runnable(){public void run(){
			BaseContent.appFilePath.setText(pathText);
			BaseContent.appFilePath.setCaretPosition(pathText.length());
		}});
	}
	public static void updateOutputDirectory()
	{
		String outputText = ad.getOutputDirectory().getAbsolutePath();
		String dFiller = ad.getSubdirectoryFiller();
		final String pathText = outputText.replace(dFiller, "");
		SwingUtilities.invokeLater(new Runnable(){public void run(){
			BaseContent.outputDirectoryPath.setText(pathText);
			BaseContent.outputDirectoryPath.setCaretPosition(pathText.length());
		}});
	}
	public static void updateCustomMainClass()
	{
		final String text = ad.getCustomMainClass();
		SwingUtilities.invokeLater(new Runnable(){public void run(){
			BaseContent.customMainClassString.setText(text);
			BaseContent.customMainClassString.setCaretPosition(text.length());
		}});
	}
	public static void updateArgsAppFile()
	{
		final String pathText = ad.getArgumentsAppFile().getAbsolutePath();
		SwingUtilities.invokeLater(new Runnable(){public void run(){
			BaseContent.argsAppFilePath.setText(pathText);
			BaseContent.argsAppFilePath.setCaretPosition(pathText.length());
		}});
	}
	public static void updateArgsVMFile()
	{
		final String pathText = ad.getArgumentsVMFile().getAbsolutePath();
		SwingUtilities.invokeLater(new Runnable(){public void run(){
			BaseContent.argsVMFilePath.setText(pathText);
			BaseContent.argsVMFilePath.setCaretPosition(pathText.length());
		}});

	}
	public static void updateConstraintsFile()
	{
		final String pathText = ad.getWorkingTaskListFile().getAbsolutePath();
		SwingUtilities.invokeLater(new Runnable(){public void run(){
			BaseContent.constraintsFilePath.setText(pathText);
			BaseContent.constraintsFilePath.setCaretPosition(pathText.length());
		}});
	}
	public static void updateGUIFile()
	{
		final String pathText = ad.getWorkingGUIFile().getAbsolutePath();
		SwingUtilities.invokeLater(new Runnable(){public void run(){
			BaseContent.guiStructureFilePath.setText(pathText);
			BaseContent.guiStructureFilePath.setCaretPosition(pathText.length());
		}});
	}
	public static void updateEFGFile()
	{
		final String pathText = ad.getWorkingEFGFile().getAbsolutePath();
		SwingUtilities.invokeLater(new Runnable(){public void run(){
			BaseContent.eventFlowGraphFilePath.setText(pathText);
			BaseContent.eventFlowGraphFilePath.setCaretPosition(pathText.length());
		}});
	}
	public static void updateTestCaseDirectory()
	{
		final String pathText = ad.getWorkingTestCaseDirectory().getAbsolutePath();
		SwingUtilities.invokeLater(new Runnable(){public void run(){
			BaseContent.testCaseDirectoryPath.setText(pathText);
			BaseContent.testCaseDirectoryPath.setCaretPosition(pathText.length());
		}});
	}
	public static void updateRipConfigurationFile()
	{
		final String pathText = ad.getRipConfigurationFile().getAbsolutePath();
		SwingUtilities.invokeLater(new Runnable(){public void run(){
			BaseContent.ripConfigurationFilePath.setText(pathText);
			BaseContent.ripConfigurationFilePath.setCaretPosition(pathText.length());
		}});
	}

	public static void updateReplayTestCases()
	{
		final String testCaseText = ld.launchSelectionArguments;
		SwingUtilities.invokeLater(new Runnable(){public void run() {
			TopContent.selT.setText(testCaseText);
			TopContent.selT.setCaretPosition(testCaseText.length());
		}});
	}
	public static class CaptureAction implements ActionListener
	{
		EventFlowSlicerController control;
		JFrame headFrame;
		boolean start;
		public CaptureAction(JFrame headFrame, boolean start)
		{
			this.headFrame = headFrame;
			this.start = start;
		}
		public void startButtonEvents()
		{
			ad.setOutputDirectory(BaseContent.outputDirectoryPath.getText());
			File setDir = new File(ad.getOutputDirectoryProvided());
			if(!ad.hasOutputDirectory()) {
				JOptionPane.showMessageDialog(headFrame,
						"<html>Please provide a path to an output directory in the<br>"
						+ "<b>Output Directory</b> field.");
				return;
			}
			if(!setDir.exists()) {
				JOptionPane.showMessageDialog(headFrame,
						"<html>Directory provided in<br>"
						+ "<b>Output Directory</b> field is invalid or cannot be found on the file system.<br>"
						+ "Please provide a valid path to a directory where output can be stored.");
				return;
			}
			// assign variables.
			ad.setAppFilePath(BaseContent.appFilePath.getText());
			if(!ad.hasAppFile()) {
				JOptionPane.showMessageDialog(headFrame,
						"<html>Please provide a path to an application in the<br><b>Application File Path</b><br>Field</html>");
				return;
			}
			if(!ad.applicationFileExists()) {
				JOptionPane.showMessageDialog(headFrame,
						"<html>File provided in<br>"
						+ "<b>Application File</b> field is invalid or cannot be found on the file system.<br>"
						+ "Please provide a valid path to an application file.</html>");
				return;
			}

			ad.setArgumentsAppFile(BaseContent.argsAppFilePath.getText());
			if(ad.hasArgumentsAppFile() && !ad.argumentsAppFileExists()) {
				JOptionPane.showMessageDialog(headFrame,
						"<html>File specified in<br>"
						+ "<b>App Arguments File Path</b><br>"
						+ "field, could not be found on the file system");
				return;
			}

			ad.setArgumentsVMFile(BaseContent.argsVMFilePath.getText());
			if(ad.hasArgumentsVMFile() && !ad.argumentsVMFileExists()) {
				JOptionPane.showMessageDialog(headFrame,
						"<html>File specified in<br>"
						+ "<b>VM Arguments File Path</b><br>"
						+ "field, could not be found on the file system<br>");
				return;
			}
			ad.setCustomMainClass(BaseContent.customMainClassString.getText());


			String fileStem = ad.getAppFile().getName();
			int classPt = fileStem.lastIndexOf(".class");
			if(classPt != -1)
				fileStem = fileStem.substring(0, classPt);
			else {
				int jarPt = fileStem.lastIndexOf(".jar");
				if(jarPt == -1)
					fileStem = fileStem.substring(0, jarPt);
			}

			ad.setWorkingTaskListFile(BaseContent.constraintsFilePath.getText());
			if(ad.hasWorkingTaskListFile()) {
				if(ad.workingTaskListFileExists()) { // provided does exist
					int confirmation = JOptionPane.showConfirmDialog(headFrame,
							new String[]{"<html>A file was provided in the <b>Constraints File</b> field that<br>"
								+ "exists on the file system.</html>",
								"<html>Would you like to overwrite this file at the end of the capture operation?</html>"},
							"Overwrite File?",
							JOptionPane.YES_NO_OPTION);
					if(confirmation != JOptionPane.YES_OPTION) {
						return;
					} // else just continue.
				}
				else { // provided does not exist
					int confirmation = JOptionPane.showConfirmDialog(headFrame,
							new String[]{"<html>The file provided in the <b>Constraints File</b> field<br>"
								+ "does <b>not</b> exist on the file system.</html>",
								"<html>A new file \"constraints.xml\" will be written to the<br>"
								+ "<b>Output Directory</b> at the end of the capture operation.</html>",
								"<html>Would you like to continue?</html>"},
							"Continue with Capture?",
							JOptionPane.YES_NO_OPTION);
					if(confirmation != JOptionPane.YES_OPTION) {
						return;
					}
					else {
						// set the new file.

//						String newFile = ad.getOutputDirectory().getAbsolutePath() + File.separator + ad.getConstraintsFileAppend() + ".xml";
//						ad.setWorkingTaskListFile(newFile);
						ad.setDefaultWorkingTaskListFile();
						// populate the GUI
						updateConstraintsFile();
					}
				}
			}
			else { // none provided.
				// set the new file
				String newFile = ad.getOutputDirectory().getAbsolutePath() + File.separator + ad.getConstraintsFileAppend() + ".xml";
				ad.setWorkingTaskListFile(newFile);
				// populate the GUI
				updateConstraintsFile();
			}

			TopContent.startB.setEnabled(false);
			TopContent.stopB.setEnabled(true);
			minimizeFrame();
			// start capture sequence.
			ac.startCapture(ld);
		}
		public void stopButtonEvents()
		{
			// Disable stop button, re-enable start button.
			TopContent.startB.setEnabled(true);
			TopContent.startB.setText("Redo");
			TopContent.stopB.setEnabled(false);
			// stop the capture sequence
			ac.stopCapture();
			// reset the look and feel
			try {UIManager.setLookAndFeel(lookAndFeel);}
			catch(UnsupportedLookAndFeelException e)
			{
				System.err.println("EventFlowSlicerView: Could not reset look and feel.\n"
						+ "Buttons may appear to look strange in resulting interface.");
			}

			// save the file to the system.
			String file = ac.writeTaskListFile();
//			ac.writeTaskListFile();
			String printString = "";
			int left, right;
			int limit = 45;
			// for every 45 characters.
			for(left = 0, right = limit; right + 5 < file.length(); right = left + limit) {
				String check = file.substring(left, right);
				int slashPos = check.lastIndexOf(File.separatorChar);
				if(slashPos != -1) {
					printString += check.substring(0, slashPos+1) + "<br>";
					left = left + slashPos + 1;
				}
				else {
					printString += check;
					left += limit;
				}
				//printString += file.substring(left, right) + "<br>";
			}

			printString += file.substring(left);
			JOptionPane.showMessageDialog(headFrame,
					new String[]{"<html>Constraints file output was written to:</html>",
						"<html><b>" + printString + "</b></html>"}
					);
		}
		public void actionPerformed(ActionEvent ae)
		{
			if(start) 	startButtonEvents();
			else   		stopButtonEvents();
		}
	}
	public static class ConstraintsAction implements ActionListener
	{
		private final JFrame headFrame;
		public ConstraintsAction(JFrame headFrame)
		{
			this.headFrame = headFrame;
		}
		public void actionPerformed(ActionEvent ae)
		{
			// output directory
			ad.setOutputDirectory(BaseContent.outputDirectoryPath.getText());
			File setDir = new File(ad.getOutputDirectoryProvided());
			if(!ad.hasOutputDirectory()) {
				JOptionPane.showMessageDialog(headFrame,
						"<html>Please provide a path to an output directory in the<br>"
						+ "<b>Output Directory</b> field.");
				return;
			}
			if(!setDir.exists() || !setDir.isDirectory()) {
				JOptionPane.showMessageDialog(headFrame,
						"<html>Directory provided in<br>"
						+ "<b>Output Directory</b> field is invalid or cannot be found on the file system.<br>"
						+ "Please provide a valid path to a directory where output can be stored.");
				return;
			}
			String workingText = BaseContent.constraintsFilePath.getText();
			ad.setWorkingTaskListFile(workingText);
			if(!ad.hasWorkingTaskListFile())
				JOptionPane.showMessageDialog(headFrame, "<html>Please provide a valid constraints file in the <br><b>Constraints File</b><br> field</html>");
			else if(!ad.workingTaskListFileExists())
				JOptionPane.showMessageDialog(headFrame, "Constraints File\n\'" + workingText + "\'\ndoes not exist on the file system");
			else {
				// Validate via JAXB
				try {
					JAXBContext context = JAXBContext.newInstance(TaskList.class);
					Unmarshaller um = context.createUnmarshaller();
		    		Object myFile = JAXBIntrospector.getValue(um.unmarshal(ad.getWorkingTaskListFile()));
		    		if(!(myFile instanceof TaskList)) {
		    			JOptionPane.showMessageDialog(headFrame,
		    					"<html>File provided in<br>"
		    					+ "<b>Constraints File</b><br>"
		    					+ "field is a " + myFile.getClass() + " file.</html>");
		    			return;
		    		}
		    		ac.setWorkingTaskList((TaskList)myFile);
				}
				catch(JAXBException e) {
					JOptionPane.showMessageDialog(headFrame, new String[]{
	    					"<html>Invalid file passed to<br>"
	    					+ "<b>Constraints File</b> Field:<br>"
							+ "Errors found in XML syntax or structure. See below for a message<br><br></html>",
							e.getLinkedException() == null ? e.getMessage() : e.getLinkedException().getMessage()});
					return;
				}
				ad.setDefaultWorkingTaskListFile();
				ac.writeTaskListFile();
				ac.startFittingDialog(headFrame);
				updateConstraintsFile();
			}
		}
	}

	public static class RipAction implements ActionListener
	{
		JFrame headFrame;
		public RipAction(JFrame headFrame)
		{
			this.headFrame = headFrame;
		}
		public void actionPerformed(ActionEvent ae)
		{
			// output directory
			ad.setOutputDirectory(BaseContent.outputDirectoryPath.getText());
			File setDir = new File(ad.getOutputDirectoryProvided());
			if(!ad.hasOutputDirectory()) {
				JOptionPane.showMessageDialog(headFrame,
						"<html>Please provide a path to an output directory in the<br>"
						+ "<b>Output Directory</b> field.");
				return;
			}
			if(!setDir.exists() || !setDir.isDirectory()) {
				JOptionPane.showMessageDialog(headFrame,
						"<html>Directory provided in<br>"
						+ "<b>Output Directory</b> field is invalid or cannot be found on the file system.<br>"
						+ "Please provide a valid path to a directory where output can be stored.");
				return;
			}
			// app file path and custom main class.
			ad.setAppFilePath(BaseContent.appFilePath.getText());
			// check and report methods.
			if(!ad.hasAppFile()) {
				JOptionPane.showMessageDialog(headFrame,
						"<html>Please provide a path to an application in the<br><b>Application File</b> field</html>");
				return;
			}
			if(!ad.applicationFileExists()) {
				JOptionPane.showMessageDialog(headFrame,
						"<html>File provided in<br>"
						+ "<b>Application File</b> field is invalid or cannot be found on the file system.<br>"
						+ "Please provide a valid path to an application file.</html>");
				return;
			}
			ad.setCustomMainClass(BaseContent.customMainClassString.getText());

			ad.setDefaultWorkingGUIFile();
			ad.setDefaultWorkingEFGFile();



			// need tasklist file, need main class, need GUI file.
			// Task List File
			ad.setWorkingTaskListFile(BaseContent.constraintsFilePath.getText());
			if(!ad.hasWorkingTaskListFile()) {
				JOptionPane.showMessageDialog(headFrame,
						"<html>Please provide a path to a constraints file in the<br>"
						+ "<b>Constraints File</b> field.</html>");
				return;
			}
			if(!ad.workingTaskListFileExists()) {
				JOptionPane.showMessageDialog(headFrame,
						"<html>File provided in<br>"
						+ "<b>Constraints File</b> field is invalid or cannot be found on the file system.<br>"
						+ "Please provide a valid path to a constraints file.</html>");
				return;
			}
			try {
				JAXBContext context = JAXBContext.newInstance(TaskList.class);
				Unmarshaller um = context.createUnmarshaller();
	    		Object myFile = JAXBIntrospector.getValue(um.unmarshal(ad.getWorkingTaskListFile()));
	    		if(!(myFile instanceof TaskList)) {
	    			JOptionPane.showMessageDialog(headFrame,
	    					"<html>File provided in<br>"
	    					+ "<b>Constraints File</b><br>"
	    					+ "field is a " + myFile.getClass() + " file.</html>");
	    			return;
	    		}
	    		ac.setWorkingTaskList((TaskList)myFile);
			}
			catch(JAXBException e) {
				JOptionPane.showMessageDialog(headFrame, new String[]{
    					"<html>Invalid file passed to<br>"
    					+ "<b>Constraints File</b> Field:<br>"
						+ "Errors found in XML syntax or structure. See below for a message<br><br></html>",
						e.getLinkedException() == null ? e.getMessage() : e.getLinkedException().getMessage()});
				return;
			}

			ad.setRipConfigurationFile(BaseContent.ripConfigurationFilePath.getText());
			if(ad.hasRipConfigurationFile() && !ad.ripConfigurationFileExists()) {
				JOptionPane.showMessageDialog(headFrame,
						"<html>File provided in <br>"
						+ "<b>Rip Configuration File</b> field is invalid or cannot be found on the file system.<br>"
						+ "Please provide a valid path to a rip configuration file");
				return;
			}
			// set all ripper configuration variables.

			JFCRipperConfigurationEFS.MAIN_CLASS = ad.getAppFile().getAbsolutePath();
			if(JavaLaunchApplication.launchesJar(JFCRipperConfigurationEFS.MAIN_CLASS))
				JFCRipperConfiguration.USE_JAR = true;

			JFCRipperConfigurationEFS.RULES_FILE = ad.getWorkingTaskListFile().getAbsolutePath();
			String pSensitivePath;
			if(!ad.hasCustomMainClass()) {
				JFCRipperConfigurationEFS.MAIN_CLASS = ad.getAppFile().getAbsolutePath();
			}
			else {
				JFCRipperConfigurationEFS.MAIN_CLASS = ad.getCustomMainClass();
				pSensitivePath = PathConformance.packageSensitiveApplicationLocation(ad.getAppFile(), ad.getCustomMainClass());
				JFCRipperConfigurationEFS.URL_LIST = pSensitivePath;
			}
			JFCRipperConfigurationEFS.INFER_WIDGETS = true;
			JFCRipperConfigurationEFS.INITIAL_WAITING_TIME = ApplicationData.openWaitTime;
			JFCRipperConfigurationEFS.CONFIG_FILE = ad.hasRipConfigurationFile() ? ad.getRipConfigurationFile().getAbsolutePath() : "";
			String colonArgs;
			colonArgs = "";
			if(ad.hasArgumentsAppFile())
				JFCRipperConfigurationEFS.ARGUMENT_LIST = ReadArguments.colonDelimAppArgumentsFrom(colonArgs);
			JFCRipperConfigurationEFS.GUI_FILE = ad.getWorkingGUIFile().getAbsolutePath();

			minimizeFrame();

			SecondaryLoop waitLoop = Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();
			ac.ripOutsideVM(waitLoop);
			waitLoop.enter();

			String message = ac.setRipTasklist();

			updateConstraintsFile();
			updateGUIFile();
			updateEFGFile();

			maximizeFrame();
			JOptionPane.showMessageDialog(headFrame, message);
		}
	}

	public static class GenerateAction implements ActionListener
	{
		JFrame headFrame;
		public GenerateAction(JFrame headFrame)
		{
			this.headFrame = headFrame;
		}
		public void actionPerformed(ActionEvent ae)
		{
			// start generation process.
			ad.setOutputDirectory(BaseContent.outputDirectoryPath.getText());
			File setDir = new File(ad.getOutputDirectoryProvided());
			if(!ad.hasOutputDirectory()) {
				JOptionPane.showMessageDialog(headFrame,
						"<html>Please provide a path to an output directory in the<br>"
						+ "<b>Output Directory</b> field.");
				return;
			}
			if(!setDir.exists()) {
				JOptionPane.showMessageDialog(headFrame,
						"<html>Directory provided in<br>"
						+ "<b>Output Directory</b> field is invalid or cannot be found on the file system.<br>"
						+ "Please provide a valid path to a directory where output can be stored.");
				return;
			}

			// Constraints File
			{
				ad.setWorkingTaskListFile(BaseContent.constraintsFilePath.getText());
				if(!ad.hasWorkingTaskListFile()) {
					JOptionPane.showMessageDialog(headFrame,
							"<html>Please provide a path to a constraints file in the<br>"
							+ "<b>Constraints File</b><br>"
							+ "Field</html>");
					return;
				}
				if(!ad.workingTaskListFileExists()) {
					JOptionPane.showMessageDialog(headFrame,
							"<html>File provided in<br>"
							+ "<b>Constraints File</b> field cannot be found on the file system.<br>"
							+ "Please provide a valid path to a Constraints File.</html>");
					return;
				}
				// Validate via JAXB
				try {
					JAXBContext context = JAXBContext.newInstance(TaskList.class);
					Unmarshaller um = context.createUnmarshaller();
		    		Object myFile = JAXBIntrospector.getValue(um.unmarshal(ad.getWorkingTaskListFile()));
		    		if(!(myFile instanceof TaskList)) {
		    			JOptionPane.showMessageDialog(headFrame,
		    					"<html>File provided in<br>"
		    					+ "<b>Constraints File</b><br>"
		    					+ "field is a " + myFile.getClass() + " file.</html>");
		    			return;
		    		}
		    		ac.setWorkingTaskList((TaskList)myFile);
				}
				catch(JAXBException e) {
					JOptionPane.showMessageDialog(headFrame, new String[]{
	    					"<html>Invalid file passed to<br>"
	    					+ "<b>Constraints File</b> Field:<br>"
							+ "Errors found in XML syntax or structure. See below for a message<br><br></html>",
							e.getLinkedException() == null ? e.getMessage() : e.getLinkedException().getMessage()});
					return;
				}
			}
			// GUI File
			{
				ad.setWorkingGUIFile(BaseContent.guiStructureFilePath.getText());
				if(!ad.hasWorkingGUIFile()) {
					JOptionPane.showMessageDialog(headFrame,
							"<html>Please provide a path to an input GUI file in the<br>"
							+ "<b>Input GUI Structure File</b><br>"
							+ "Field</html>");
					return;
				}
				if(!ad.workingGUIFileExists()) {
					JOptionPane.showMessageDialog(headFrame,
							"<html>File provided in<br>"
							+ "<b>Input GUI Structure File</b> field cannot be found on the file system.<br>"
							+ "Please provide a valid path to a GUI file.</html>");
					return;
				}
				try {
					JAXBContext context = JAXBContext.newInstance(GUIStructure.class);
					Unmarshaller um = context.createUnmarshaller();
		    		Object myFile = JAXBIntrospector.getValue(um.unmarshal(ad.getWorkingGUIFile()));
		    		if(!(myFile instanceof GUIStructure)) {
		    			JOptionPane.showMessageDialog(headFrame,
		    					"<html>File provided in<br>"
		    					+ "<b>Input GUI Structure</b><br>"
		    					+ "field is a " + myFile.getClass() + " file.</html>");
		    			return;
		    		}
		    		ac.setWorkingGUIStructure((GUIStructure)myFile);
				}
				catch(JAXBException e) {
					JOptionPane.showMessageDialog(headFrame, new String[]{
	    					"<html>Invalid file passed to<br>"
	    					+ "<b>Input GUI Structure File</b> Field:<br>"
							+ "Errors found in XML syntax or structure. See below for a message<br><br></html>",
							e.getLinkedException() == null ? e.getMessage() : e.getLinkedException().getMessage()});
					return;
				}
			}

			// EFG File
			{
				ad.setWorkingEFGFile(BaseContent.eventFlowGraphFilePath.getText());
				if(!ad.hasWorkingEFGFile()) {
					JOptionPane.showMessageDialog(headFrame,
							"<html>Please provide a path to an input EFG file in the<br>"
							+ "<b>Input Event Flow Graph File</b><br>"
							+ "Field</html>");
					return;
				}
				if(!ad.workingEFGFileExists()) {
					JOptionPane.showMessageDialog(headFrame,
							"<html>File provided in<br>"
							+ "<b>Input Event Flow Graph File</b> field is invalid or cannot be found on the file system.<br>"
							+ "Please provide a valid path to an EFG file.</html>");
					return;
				}
				try {
					// check if this file contains EFG data
					JAXBContext context = JAXBContext.newInstance(EFG.class);
					Unmarshaller um = context.createUnmarshaller();
		    		Object myFile = JAXBIntrospector.getValue(um.unmarshal(ad.getWorkingEFGFile()));
		    		if(!(myFile instanceof EFG)) {
		    			JOptionPane.showMessageDialog(headFrame,
		    					"<html>File provided to <br>"
		    					+ "<b>Input Event Flow Graph File</b><br>"
		    					+ "field is a " + myFile.getClass() + " file.</html>");
		    			return;
		    		}
		    		ac.setWorkingEventFlow((EFG)myFile);
				}
				catch(JAXBException e) {
					JOptionPane.showMessageDialog(headFrame, new String[]{
	    					"<html>Invalid file passed to<br>"
	    					+ "<b>Input GUI Structure File</b> Field:<br>"
							+ "Errors found in XML syntax or structure. See below for a message<br><br></html>",
							e.getLinkedException() == null ? e.getMessage() : e.getLinkedException().getMessage()});
					return;
				}
			}

			// the default running type is the original used in the thesis.
			if(!ad.getOutputDirectory().exists())
				if(!ad.getOutputDirectory().mkdirs())
					JOptionPane.showMessageDialog(headFrame, "Generation output directory could not be created.");
			ac.bookmarkEFG(); // prepare the EFG
			ac.relabelConstraintsWidgets(); // prepare the constraints
			ac.setupGeneratorLogFile();
			try {
				int testCases = ac.startGeneratingTestCases(ld);
				JOptionPane.showMessageDialog(headFrame, "(" + testCases + ") Test Cases were generated successfully");
			} catch(IOException e) {
				JOptionPane.showMessageDialog(headFrame, e.getMessage());
			}
			ad.setDefaultWorkingTestCaseDirectory();
			updateTestCaseDirectory();
		}
	}




	public static class ReplayAction implements ActionListener
	{
		JFrame headFrame;
		public ReplayAction(JFrame headFrame)
		{
			this.headFrame = headFrame;
		}
		public void actionPerformed(ActionEvent ae)
		{
			// output directory
			ad.setOutputDirectory(BaseContent.outputDirectoryPath.getText());
			File setDir = new File(ad.getOutputDirectoryProvided());
			if(!ad.hasOutputDirectory()) {
				JOptionPane.showMessageDialog(headFrame,
						"<html>Please provide a path to an output directory in the<br>"
						+ "<b>Output Directory</b> field.");
				return;
			}
			if(!setDir.exists() || !setDir.isDirectory()) {
				JOptionPane.showMessageDialog(headFrame,
						"<html>Directory provided in<br>"
						+ "<b>Output Directory</b> field is invalid or cannot be found on the file system.<br>"
						+ "Please provide a valid path to a directory where output can be stored.");
				return;
			}

			// Application File
			ad.setAppFilePath(BaseContent.appFilePath.getText());
			if(!ad.hasAppFile()) {
				JOptionPane.showMessageDialog(headFrame,
						"<html>Please provide a path to an application in the<br><b>Application File Path</b><br>Field</html>");
				return;
			}
			if(!ad.applicationFileExists()) {
				JOptionPane.showMessageDialog(headFrame,
						"<html>File provided in<br>"
						+ "<b>Application File</b> field is invalid or cannot be found on the file system.<br>"
						+ "Please provide a valid path to an application file.</html>");
				return;
			}
			// Arguments AppFile
			ad.setArgumentsAppFile(BaseContent.argsAppFilePath.getText());
			if(ad.hasArgumentsAppFile() && !ad.argumentsAppFileExists()) {
				JOptionPane.showMessageDialog(headFrame,
						"<html>File specified in<br>"
						+ "<b>App Arguments File Path</b><br>"
						+ "field, could not be found on the file system");
				return;
			}
			// Arguments VMFile.
			ad.setArgumentsVMFile(BaseContent.argsVMFilePath.getText());
			if(ad.hasArgumentsVMFile() && !ad.argumentsVMFileExists()) {
				JOptionPane.showMessageDialog(headFrame,
						"<html>File specified in<br>"
						+ "<b>VM Arguments File Path</b><br>"
						+ "field, could not be found on the file system<br>");
				return;
			}
			// custom main
			ad.setCustomMainClass(BaseContent.customMainClassString.getText());


			// GUI File
			{
				ad.setWorkingGUIFile(BaseContent.guiStructureFilePath.getText());
				if(!ad.hasWorkingGUIFile()) {
					JOptionPane.showMessageDialog(headFrame,
							"<html>Please provide a path to an input GUI file in the<br>"
							+ "<b>Input GUI Structure File</b><br>"
							+ "Field</html>");
					return;
				}
				if(!ad.workingGUIFileExists()) {
					JOptionPane.showMessageDialog(headFrame,
							"<html>File provided in<br>"
							+ "<b>Input GUI Structure File</b> field is invalid or cannot be found on the file system.<br>"
							+ "Please provide a valid path to a GUI file.</html>");
					return;
				}
				try {
					JAXBContext context = JAXBContext.newInstance(GUIStructure.class);
					Unmarshaller um = context.createUnmarshaller();
		    		Object myFile = JAXBIntrospector.getValue(um.unmarshal(ad.getWorkingGUIFile()));
		    		if(!(myFile instanceof GUIStructure)) {
		    			JOptionPane.showMessageDialog(headFrame,
		    					"<html>File provided to <br>"
		    					+ "<b>Input GUI Structure</b><br>"
		    					+ "field is a " + myFile.getClass() + " file.</html>");
		    			return;
		    		}
		    		ac.setWorkingGUIStructure((GUIStructure)myFile);
				}
				catch(JAXBException e) {
					JOptionPane.showMessageDialog(headFrame, new String[]{
	    					"<html>Invalid file passed to<br>"
	    					+ "<b>Input GUI Structure File</b> Field:<br>"
							+ "Errors found in XML syntax or structure. See below for a message<br><br></html>",
							e.getLinkedException() == null ? e.getMessage() : e.getLinkedException().getMessage()});
					return;
				}
			}
			// EFG File
			{
				ad.setWorkingEFGFile(BaseContent.eventFlowGraphFilePath.getText());
				if(!ad.hasWorkingEFGFile()) {
					JOptionPane.showMessageDialog(headFrame,
							"<html>Please provide a path to an input EFG file in the<br>"
							+ "<b>Input Event Flow Graph File</b><br>"
							+ "Field</html>");
					return;
				}
				if(!ad.workingEFGFileExists()) {
					JOptionPane.showMessageDialog(headFrame,
							"<html>File provided in<br>"
							+ "<b>Input Event Flow Graph File</b> field is invalid or cannot be found on the file system.<br>"
							+ "Please provide a valid path to an EFG file.</html>");
					return;
				}
				try {
					// check if this file contains EFG data
					JAXBContext context = JAXBContext.newInstance(EFG.class);
					Unmarshaller um = context.createUnmarshaller();
		    		Object myFile = JAXBIntrospector.getValue(um.unmarshal(ad.getWorkingEFGFile()));
		    		if(!(myFile instanceof EFG)) {
		    			JOptionPane.showMessageDialog(headFrame,
		    					"<html>File provided to <br>"
		    					+ "<b>Input Event Flow Graph File</b><br>"
		    					+ "field is a " + myFile.getClass() + " file.</html>");
		    			return;
		    		}
		    		ac.setWorkingEventFlow((EFG)myFile);
				}
				catch(JAXBException e) {
					JOptionPane.showMessageDialog(headFrame, new String[]{
	    					"<html>Invalid file passed to<br>"
	    					+ "<b>Input GUI Structure File</b> Field:<br>"
							+ "Errors found in XML syntax or structure. See below for a message<br><br></html>",
							e.getLinkedException() == null ? e.getMessage() : e.getLinkedException().getMessage()});
					return;
				}
			}
			// test case directory
			ad.setWorkingTestCaseDirectory(BaseContent.testCaseDirectoryPath.getText());
			if(!ad.hasWorkingTestCaseDirectory()) {
				JOptionPane.showMessageDialog(headFrame,
						"<html>Please provide a path to a test case directory in the<br>"
						+ "<b>Input Test Case Directory</b> field.");
				return;
			}
			if(!ad.workingTestCaseDirectoryExists()) {
				JOptionPane.showMessageDialog(headFrame,
						"<html>Directory provided in<br>"
						+ "<b>Input Test Case Directory</b> field is invalid or cannot be found on the file system.<br>"
						+ "Please provide a valid path to a folder containing test cases.</html>");
				return;
			}
			// launch selection arguments
			String selection = TopContent.selT.getText();
			if(!selection.isEmpty()) {
				if(ArrayTools.bibleNotationType(selection, false) == -1)
					JOptionPane.showMessageDialog(headFrame, new String[]{
							"Test Case Selection Parameters in \"Replay\" box could not be parsed.",
							"This box can receive only valid selection parameters that follow the pattern:",
							"\"<directory_number>:<TC_number_1>,<TC_number_2>,...\" or ",
							"\"<min_TC>-<maxTC>.\""});
				else
					ld.setLaunchSelectionArguments(selection);
			}

			String pSensitivePath;
			if(!ad.hasCustomMainClass()) {
				JFCReplayerConfigurationEFS.MAIN_CLASS = ad.getAppFile().getAbsolutePath();
				pSensitivePath = "";
			}
			else {
				JFCReplayerConfigurationEFS.MAIN_CLASS = ad.getCustomMainClass();
				pSensitivePath = PathConformance.packageSensitiveApplicationLocation(ad.getAppFile(), ad.getCustomMainClass());
				JFCReplayerConfigurationEFS.URL_LIST = pSensitivePath;
			}
			minimizeFrame();
			ac.bookmarkEFG();
			ac.startReplay(ld);
		}
	}
	public static void maximizeFrame()
	{
		final java.awt.Frame mainWindow = frameInUse;
		// set up a maximizer subprocedure
		Runnable maximizer = new Runnable() { public void run() {
			KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
			mainWindow.setVisible(true);
			mainWindow.toFront();
			mainWindow.repaint();
			mainWindow.requestFocus();
		}};

		// attempt to run it on the event dispatch thread.
		if(EventQueue.isDispatchThread())
			maximizer.run();
		else
			try {SwingUtilities.invokeAndWait(maximizer);}
			catch(InterruptedException | InvocationTargetException e) {
				Thread.currentThread().interrupt();
			}
		System.out.println("EventFlowSlicerView: EventFlowSlicer window was maximized.\n");
	}
}
