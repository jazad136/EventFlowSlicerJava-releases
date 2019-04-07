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
package edu.unl.cse.efs.view.ft;

import javax.swing.*;
import edu.umd.cs.guitar.model.XMLHandler;
import edu.umd.cs.guitar.model.data.ObjectFactory;
import edu.umd.cs.guitar.model.data.TaskList;
import edu.umd.cs.guitar.model.data.Widget;
import edu.unl.cse.efs.ApplicationData;
import edu.unl.cse.efs.tools.HyperList;
import edu.unl.cse.efs.tools.ReportTranslation;
import javax.swing.text.PlainDocument;

import java.awt.*;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.FocusTraversalPolicy;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static edu.unl.cse.efs.view.ft.InvalidWidgetException.Attribute.*;
import static edu.unl.cse.efs.tools.TestCaseGeneratorPreparation.*;
import static edu.unl.cse.efs.view.DecorationsRunner.*;

/**
 * This class builds a GUI that allows a user to edit constraints xml files,
 * including their widgets and parameterized rules.
 * @author Jonathan Saddler
 */
public class FittingTool {
	public static Dimension currentFrameDimension = new Dimension();
	public static JFrame dialogParentFrame;
	public static WidgetForm additionForm;
	public static ReportFrame report;
	public static JButton previousButton, nextButton;
	public static JPanel prevPan, nextPan, labelPan;
	public static JLabel topLabel, secLabel;
	public static TaskList workingTaskList;
	public static DisplayingWidgets getList;
	public static RuleDisplay putLists;
	public static final float LIST_TEXT_FONT = 14;
	public static final float HEADER_TEXT_FONT = 18;
	public static final int FRAME_BASE_SIZE_X = 800, FRAME_BASE_SIZE_Y = 500;
	public static Controller progressListeners;
	public static DisplayIcon prototypeWidgetIcon, prototypeRuleIcon;
	public static DisplayIcon noneIcon;

	public static ObjectFactory fact = new ObjectFactory();
	public static XMLHandler handler = new XMLHandler();
	
	static{
		noneIcon = new DisplayIcon("none");
		prototypeWidgetIcon = new DisplayIcon("mmmmmmmmm mmmmmmmmm");
		prototypeRuleIcon = new DisplayIcon("mmmmmmmm mmmm");
	}
	
	public static JDialog setupDialog(int width, int height, JFrame parentFrame)
	{
		// setup the frame
		JDialog window = new JDialog(parentFrame, "Fitting Tool");
		window.setModalityType(ModalityType.APPLICATION_MODAL);
		window.setPreferredSize(new Dimension(width, height));
		return window;
	}
	public static JFrame setupFrame(int width, int height)
	{
		// setup the frame
		JFrame frame = new JFrame("Fitting Tool");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setPreferredSize(new Dimension(width, height));
		return frame;
	}
	public static void show(JDialog window)
	{
		window.pack();
		currentFrameDimension = window.getSize(); // at the latest possible state, get the size.
		window.setVisible(true);
	}
	public static void show(JFrame frame)
	{
		frame.pack();
		frame.setVisible(true);
		currentFrameDimension = frame.getSize(); // at the latest possible state, get the size.
	}
	public static void startAndReadTo(ApplicationData data, JFrame parentFrame)
	{
		startDialogProgrammatically(data.getWorkingTaskListFile(), "r", parentFrame);
	}
	public static void startAndReadTo(ApplicationData data)
	{
		startProgrammatically(data.getWorkingTaskListFile(), "r");
	}
	public static void startDialogProgrammatically(final File widgetFile, final String ruleString, final JFrame parentFrame)
	{	
		dialogParentFrame = parentFrame;
		SwingUtilities.invokeLater(new Runnable(){public void run(){
			JDialog dialog = setupDialog(FRAME_BASE_SIZE_X, FRAME_BASE_SIZE_Y, parentFrame);
			RuleName ruleMode = null;
			switch(ruleString) {
				case "p": ruleMode = RuleName.REP; break;
				case "e": ruleMode = RuleName.MEX; break;
				case "o": ruleMode = RuleName.ORD; break;
				case "a": ruleMode = RuleName.ATM; break;
				case "r": 
				default: ruleMode = RuleName.REQ;
			}
			progressListeners = new Controller(dialog, widgetFile, ruleMode);
			nextButton.addActionListener(progressListeners.nextListener);
			previousButton.addActionListener(progressListeners.previousListener);
			show(dialog);
			Controller.writeFile(progressListeners.state, progressListeners.writeFile);
			JOptionPane.showMessageDialog(parentFrame, "Constraints written successfully");
		}});
	}
	public static void startProgrammatically(final File widgetFile, final String ruleString)
	{	
		SwingUtilities.invokeLater(new Runnable(){public void run(){
			JFrame frame = setupFrame(FRAME_BASE_SIZE_X, FRAME_BASE_SIZE_Y);
			RuleName ruleMode = null;
			switch(ruleString) {
				case "p": ruleMode = RuleName.REP; break;
				case "e": ruleMode = RuleName.MEX; break;
				case "o": ruleMode = RuleName.ORD; break;
				case "a": ruleMode = RuleName.ATM; break;
				case "r": 
				default: ruleMode = RuleName.REQ;
			}
			progressListeners = new Controller(frame, widgetFile, ruleMode);
			nextButton.addActionListener(progressListeners.nextListener);
			previousButton.addActionListener(progressListeners.previousListener);
			show(frame);}});
	}
	
	public static class Test {
		public static void main(final String[] args)
		{
			File theFile = new File("");
			if(args.length > 1) { 
				theFile = new File(args[1]);
				workingTaskList = (TaskList)handler.readObjFromFile(theFile, TaskList.class);
			}
			if(args.length == 0 || args[0].equals("test"))
			{
				System.out.println("Testing...");
				final File widgetFile = theFile;
				SwingUtilities.invokeLater(new Runnable(){public void run(){
					JFrame frame = setupFrame(FRAME_BASE_SIZE_X, FRAME_BASE_SIZE_Y);
					if(args.length > 2) {
						RuleName ruleMode = null;
						switch(args[2].toLowerCase()) {
						case "r": ruleMode = RuleName.REQ; break;
						case "e": ruleMode = RuleName.MEX; break;
						case "o": ruleMode = RuleName.ORD; break;
						case "a": ruleMode = RuleName.ATM; break;
						case "w": default:
						}
						if(ruleMode == null)
							progressListeners = new Controller(frame, widgetFile, RuleName.REQ);
						else
							progressListeners = new Controller(frame, widgetFile, ruleMode);	
					}
					else {
						progressListeners = new Controller(frame, widgetFile, RuleName.REQ);
					}
					
					nextButton.addActionListener(progressListeners.nextListener);
					previousButton.addActionListener(progressListeners.previousListener);
					show(frame);
				}});
			}
		}
	}
	
	
	public static class WidgetForm extends AbstractAction
	{
		public FocusTraversalPolicy ftp;
		public JLabel addAWidget, nLabel, eLabel, wLabel, tLabel, aLabel, pLabel;
		public JTextField name, eventId, window, type, action, parameter;
		public JTextField[] formFields;
		public JButton addButton, dropButton;
		public JPanel formPanel;
		public DisplayingWidgets dwidgets;
		int formColumns = 20;
		float labelFSize = 14;
		public Window frameToUse;
		
		public WidgetForm(DisplayingWidgets availableList, Window frameToUse)
		{
			super("Add");
			this.frameToUse = frameToUse;
			this.dwidgets = availableList;
			formPanel = new JPanel();
		}
		
		public void layout()
		{	
			initialize(formPanel, 3, 7);
			int next = 0;
			editingStartOfRow(next++);
			addAWidget = spanningLabel("Add_a_widget", HEADER_TEXT_FONT, "Add a Widget", 3);
			editingStartOfRow(next++); 
			addButton = button("Add_button", "Add");
			eLabel = label("EventID_label", labelFSize, "EventId:");
			eventId = field("EventID_field", formColumns);
			editingStartOfRow(next++); jumpRowOrColumn();
			nLabel = label("Name_Label", labelFSize, "Name:");
			name = field("Name_field", formColumns);
			editingStartOfRow(next++); jumpRowOrColumn();
			tLabel = label("Type_label", labelFSize, "Type:");
			type = field("Type_field", formColumns);
			editingStartOfRow(next++); jumpRowOrColumn();
			wLabel = label("Window_label", labelFSize, "Window:");
			window = field("Window_field", formColumns);
			editingStartOfRow(next++); jumpRowOrColumn();
			aLabel = label("Action_label", labelFSize, "Action:");
			action = field("Action_field", formColumns);
			editingStartOfRow(next++); jumpRowOrColumn(); 
			//label("optional_label", labelFSize-2, "(optional)");
			pLabel = label("Parameter_label", labelFSize, "Parameter:");
			parameter = field("Parameter_field", formColumns);
			
			formFields = new JTextField[]{eventId, name, type, window, action, parameter};
			addButton.setAction(this);
			eventId.setAction(this);
			name.setAction(this);
			type.setAction(this);
			window.setAction(this);
			action.setAction(this);
			parameter.setAction(this);
			ftp = new FocusTraversalPolicy(){
				final ArrayList<Component> order = 
						new ArrayList<Component>(Arrays.asList(formFields));
				@Override
				public Component getDefaultComponent(Container aContainer) {return name;}
				@Override
				public Component getComponentBefore(Container aContainer, Component aComponent) {
					int idx = order.indexOf(aComponent)-1;
					if(idx < 0)
						idx = formFields.length-1;
					return order.get(idx);
				}
				@Override
				public Component getComponentAfter(Container focusCycleRoot, Component aComponent) 
				{	
					int idx = (order.indexOf(aComponent)+1)%order.size();
					return order.get(idx);
				}
				@Override
				public Component getFirstComponent(Container aContainer) {return formFields[0];}
				@Override
				public Component getLastComponent(Container aContainer) {return formFields[formFields.length-1];}
			};
			formPanel.setFocusTraversalPolicy(ftp);
			hardenEdits();
			
		}

		public void clearForm() {for(JTextField f : formFields) f.setText("");}
		public void submitForm() throws WidgetAlreadyExistsException, InvalidWidgetException
		{
			Widget nw = fact.createWidget();
			nw.setEventID(eventId.getText().trim());			
			if(nw.getEventID().isEmpty()) 	throw new InvalidWidgetException(EVENT_ID);
			
			nw.setName(name.getText().trim());
			
			nw.setType(type.getText().trim());
			if(nw.getType().isEmpty()) 		throw new InvalidWidgetException(TYPE);
			
			nw.setWindow(window.getText().trim());
			if(nw.getWindow().isEmpty())  	throw new InvalidWidgetException(WINDOW);
			
			nw.setAction(action.getText().trim());
			if(nw.getAction().isEmpty()) 	throw new InvalidWidgetException(ACTION);
			
			if(!parameter.getText().trim().isEmpty()) nw.setParameter(parameter.getText().trim());	
			
			
			int existing = dwidgets.addAvailable(nw);
			if(existing == -1) {
				Controller.writeFile(Controller.State.W, progressListeners.writeFile);
				eventId.setText(""); 	name.setText("");
				type.setText(""); 		window.setText("");
				action.setText("");		parameter.setText("");
			}
			else
				throw new WidgetAlreadyExistsException(dwidgets.theWidgets.get(existing).getEventID());
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			if(e.getSource() == parameter || e.getSource() == addButton) {
				try {submitForm();}
				catch(WidgetAlreadyExistsException ex) {
					JOptionPane.showMessageDialog(frameToUse, 
					"<html>The widget specified contains attributes that match another<br>"
					+ "widget already present in the two lists above.<br>"
					+ "Matches widget ID: " + ex.widgetName + "</html>");
					return;
				}
				catch(InvalidWidgetException ex) {
					JOptionPane.showMessageDialog(frameToUse, 
					"<html>This widget cannot be added<br>"
					+ "The " + ex.cause.nameString + " string was not provided.</html");
					return;
				}
				if(e.getSource() == parameter) 
					KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent();
			}
			else if(e.getSource() instanceof JTextField) {
				KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent();
				//JTextField next = (JTextField)ftp.getComponentAfter(null, (JTextField)e.getSource());
			}
		}
	}
	public static void displayingWidgets()
	{	
		if(getList.hasDisplayFile)
			getList.loadNew();
		editingStartOfColumn(0);
		(getList.setModelList(true, listInViewport("Available List", LIST_TEXT_FONT, getList.modelA))).setPrototypeCellValue(prototypeWidgetIcon);
		
		editingStartOfColumn(3);
		(getList.setModelList(false, listInViewport("Constraints File List", LIST_TEXT_FONT, getList.modelB))).setPrototypeCellValue(prototypeWidgetIcon);
		editingRow(0, 1);
		place(getList.addRemovePanel, 2);
	}
	public static void secondRow(int row, int additionFormWidth, Window frameToUse)
	{
		editingStartOfRow(row);
		additionForm = new WidgetForm(getList, frameToUse);
		place(additionForm.formPanel, additionFormWidth);
		jumpRowOrColumn();
		place(putLists.listsPanel);
	}
//	public static void reportFrame(int column, int height)
//	{
//		editingStartOfColumn(column);
//		report = new ReportFrame();
//		place(report.textFrame, height);
//	}
	
	public FittingTool(File widgetFile)
	{
		topLabel = new JLabel();
		secLabel = new JLabel();
		topLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		secLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		previousButton = new JButton();
		previousButton.getAccessibleContext().setAccessibleName("Previous_button");
		nextButton = new JButton("");
		nextButton.getAccessibleContext().setAccessibleName("Next_button");
		labelPan = new JPanel();
		prevPan = new JPanel();
		nextPan = new JPanel();		
		prevPan.add(previousButton);
		nextPan.add(nextButton);
		labelPan.setLayout(new BoxLayout(labelPan, BoxLayout.PAGE_AXIS));
		labelPan.add(topLabel);
//		labelPan.add(secLabel);
		workingTaskList = (TaskList)handler.readObjFromFile(widgetFile, TaskList.class);
	}

	private static void endLabels()
	{
		topLabel.setText("Done.");
		topLabel.setFont(topLabel.getFont().deriveFont(16f));
		secLabel.setText("Please click the Done button to close the tool.");
		secLabel.setFont(secLabel.getFont().deriveFont(14f));
		previousButton.setText("Back to Constraints");
		nextButton.setText("Done");
		
	}
	
//	private static void widgetsLabels()
//	{
//		topLabel.setText("First, Select Widgets");
//		topLabel.setFont(topLabel.getFont().deriveFont(16f));
//		secLabel.setText("to be Used Within Constraints");
//		secLabel.setFont(secLabel.getFont().deriveFont(14f));
//		previousButton.setText("Back");
//		nextButton.setText("To Constraints");
//	}
	
	private static void rulesLabels(RuleName type)
	{
		topLabel.setFont(topLabel.getFont().deriveFont(16f));
		switch(type) {
		case REQ:
			topLabel.setText("1. Edit Requires Constraints.");
			secLabel.setText("");
			previousButton.setText("Back");
			nextButton.setText("Next");
			
break;	case MEX:
			topLabel.setText("2. Edit Exclusion Constraints.");
			secLabel.setText("");
			previousButton.setText("<html><center>Back to<br>Requires</center></html>");
			nextButton.setText("Next");
break;	case ORD:
			topLabel.setText("3. Edit Order Constraints.");
			secLabel.setText("");
			previousButton.setText("<html><center>Back to<br>Exclusion</center></html>");
			nextButton.setText("Next");
break;	case REP:
			topLabel.setText("4. Edit Repeat Constraints.");
			secLabel.setText("");
			previousButton.setText("<html><center>Back to<br>Atomic</center></html>");
			nextButton.setText("Next");
break; 	case STO:
			topLabel.setText("5. Edit Stop Constraints.");
			secLabel.setText("");
			previousButton.setText("<html><center>Back to<br>Repeat</center></html>");
			nextButton.setText("Next");
break;	case ATM:
			topLabel.setText("6. Edit Atomic Constraints.");
			topLabel.setFont(topLabel.getFont().deriveFont(16f));
			secLabel.setText("");
			previousButton.setText("<html><center>Back to<br>Stop</center></html>");
			nextButton.setText("Finish");
		}
	}

	private static JPanel topPanel()
	{
		JPanel top = new JPanel();
		prevPan.add(previousButton);
		nextPan.add(nextButton);
		top.add(prevPan);
		// setup labels in the center
		labelPan.add(topLabel);
		labelPan.add(secLabel);
//		GroupLayout layout = new GroupLayout(top);
//		layout.setHorizontalGroup(layout.createSequentialGroup()
//				.addGroup(layout.createParallelGroup()
//						.addComponent(prevPan)
//						.addComponent(labelPan)
//						.addComponent(nextPan))
//				);
//		layout.setVerticalGroup(layout.createSequentialGroup()
//			.addGroup(layout.createParallelGroup()
//					.addComponent(prevPan))
//			.addGroup(layout.createParallelGroup()
//					.addComponent(labelPan))
//			.addGroup(layout.createParallelGroup()
//					.addComponent(nextPan))
//			);
//		top.setLayout(layout);
		top.setLayout(new BoxLayout(top, BoxLayout.LINE_AXIS));
		top.add(prevPan);
		top.add(labelPan);
		top.add(nextPan);

		return top;
	}
	public static void setupContent(Window frameToUse, File widgetFile, Controller.State state)
	{
		new FittingTool(widgetFile);
		frameToUse.setLayout(new BoxLayout(((RootPaneContainer)frameToUse).getContentPane(), BoxLayout.PAGE_AXIS));
		JPanel top = topPanel();
		frameToUse.add(top);
		if(state != Controller.State.END) {
			if(widgetFile.getPath().isEmpty()) {  
				getList = new DisplayingWidgets();
				putLists = new RuleDisplay(getList);
			}
			else {  
				getList = new DisplayingWidgets(widgetFile.getAbsoluteFile());
				putLists = new RuleDisplay(getList, widgetFile.getAbsoluteFile());
			}
			JPanel lists = initializeNew(4, 3);
			lists.setPreferredSize(new Dimension(currentFrameDimension.width-10, currentFrameDimension.height/3));
			displayingWidgets();
			secondRow(1, 2, frameToUse);
			hardenEdits();
			frameToUse.add(lists);
			additionForm.layout();
			putLists.layout();
			putLists.loadNew(state.ruleName);
		}
	}
	
	
	public static class DisplayingWidgets
	{
		// policy: correct for errors at the low level. 
		// policy: set what the user specifies.
		// thus: the provided file is set no matter what.
		// if the file is null, throw an exception.
		// widgets
		JList<DisplayIcon> list, list2; 
		List<Widget> theWidgets;
		JButton addB, removeB; JPanel addRemovePanel;
		ArrayList<Widget> labels; 
		DisplayingWidgetsModel modelA, modelB;
		File displayFile;
		public AddListener addListener;
		public RemoveListener removeListener;
		boolean hasDisplayFile, buttonsReady;
		int[] silentSelection;
		
		public void resetModels(boolean addNone)
		{
			modelA.fireChanges();
			modelB.fireChanges();
			if(!modelB.isDisplayEmpty()) {
				for(int i = modelB.getSize()-1; i >= 0; i--) 
					modelA.add(modelB.remove(i));
				if(addNone)
					modelB.add(noneIcon);
				modelA.fireChanges();
				modelB.fireChanges();
			}
		}
		
		
		/**
		 * Returns true if an index was found. 
		 * @param toSelect
		 * @return
		 */
		public boolean setAvailableSelection(Widget... toSelect)
		{
			int[] indices = new int[modelA.getSize()];
			int next = 0;
			for(Widget w : toSelect) {
				int index = modelA.findWidget(w);
				if(index != -1) 
					indices[next++] = index;
			}	
			list.setSelectedIndices(Arrays.copyOf(indices, next));
			return next > 0;
		}
		
		public DisplayingWidgets()
		{			
			theWidgets = new ArrayList<Widget>();
			addRemovePanel = new JPanel();
			addRemovePanel.setLayout(new BoxLayout(addRemovePanel, BoxLayout.PAGE_AXIS));
			addB = new JButton(">");
			addB.getAccessibleContext().setAccessibleName("Add_Button");
			removeB = new JButton("<");
			removeB.getAccessibleContext().setAccessibleName("Remove_Button");
			addRemovePanel.add(addB);
			addRemovePanel.add(removeB);
			modelA = new DisplayingWidgetsModel();
			modelB = new DisplayingWidgetsModel();
			labels = new ArrayList<Widget>();
		}
		
		public DisplayingWidgets(File widgetsFile)
		{
			this();
			hasDisplayFile = setAndCheckFile(widgetsFile);
		}
		
		public boolean availableContains(DisplayIcon di)
		{
			if(modelA == null)
				return false;
			return modelA.inDisplay.contains(di);
		}
		
		public int availableIndex(DisplayIcon di)
		{
			return modelA.findIcon(di);
		}
		
		public JList<DisplayIcon> setModelList(boolean modelA, JList<DisplayIcon> theList)
		{
			if(modelA) this.list = theList;
			else  this.list2 = theList;
			
			if(this.list != null && this.list2 != null) {
				addB.addActionListener(new AddListener());
				removeB.addActionListener(new RemoveListener());
				buttonsReady = true;
			}
			return theList;
		}
		
		public String toString()
		{
			String toReturn = sp(2) + "Loaded...";
			toReturn += modelA.printSpacedListing(2+2);
			toReturn += "\n" + sp(2) + "To Load..."; 
			toReturn += modelB.printSpacedListing(2+2);
			return toReturn;
		}		
		
		/**
		 * Ensures that the file specified is a non-null object, that can be potentially read from the file system.
		 * @param widgetsFile
		 * @return
		 */
		public boolean setAndCheckFile(File widgetsFile)
		{
			displayFile = widgetsFile;
			if(widgetsFile == null) 
				return false;
			if(!widgetsFile.isFile())
				return false;
			return true;
		}
		
		public void loadNew()
		{
			if(!hasDisplayFile)
				throw new RuntimeException("The file provided to the Widgets JList is invalid");
			loadNewFromFile(displayFile);
		}
		
		public void removeAvailable(DisplayIcon... icons)
		{
			for(DisplayIcon icon : icons) 
				modelA.remove(icon);
			modelA.fireChanges();
		}
		public int addAvailable(Widget w)
		{
			int existing = theWidgets.indexOf(w); 
			if(existing != -1)
				return existing;
			theWidgets.add(w);
			modelA.add(w);
			modelA.fireChanges();
			return -1;
		}
		
		public void loadNewFromFile(File theFile)
		{
			try {
				hasDisplayFile = setAndCheckFile(theFile);
				if(!hasDisplayFile)
					throw new RuntimeException("The file provided to the Widgets JList is invalid");
				if(!theFile.exists())
					throw new FileNotFoundException("File provided to the Widgets JList does not exist.");
				if(theFile.isDirectory())
					throw new IllegalArgumentException("File provided to the display module must not be a directory.");
				theWidgets = readWidgetsFromConstraintsFile(theFile);
				Collections.sort(theWidgets);
				resetModels(false);
				modelA.clear();
				modelB.clear();
				modelA = new DisplayingWidgetsModel(theWidgets);
				modelB.add(noneIcon);
				
				modelA.fireChanges();
				modelB.fireChanges();
				System.out.println(this);
			} 
			catch(IOException e) {
				System.err.println(e);
				hasDisplayFile = false;
			}
		}
		
		public List<Widget> modeledWidgets()
		{
			LinkedList<Widget> toReturn = new LinkedList<Widget>();
			
			for(DisplayIcon di : modelB) 
				for(Widget w : theWidgets) {
					DisplayIcon testDI = new DisplayIcon(w);
					if(di.equals(testDI))
						toReturn.add(w);
				}
			return toReturn;
		}
		public void transferWidgetsToTheRight(int[] indices, boolean noRemove)
		{			
			if(indices.length == 0)
				return;
			// make changes
			for(int index : indices) {
				if(modelA.getElementAt(index).equals(noneIcon))
					continue;
				DisplayIcon removed = modelA.remove(index);
				modelB.add(removed);
			}
			// can't get further without having selected some index. 
			boolean otherHasNone = modelB.isDisplayEmpty();
		    // remove the none widget
		    if(otherHasNone) 
		    	modelB.remove(0);
		    
		    modelA.fireChanges(); 
		    modelB.fireChanges();
		}
		
		public void transferWidgetsToTheRight(int[] indices)
		{			
			if(indices.length == 0)
				return;
			// make changes
			for(int index : indices) {
				if(modelA.getElementAt(index).equals(noneIcon))
					continue;
				DisplayIcon removed = modelA.remove(index);
				modelB.add(removed);
			}
			// can't get further without having selected some index. 
			boolean otherHasNone = modelB.isDisplayEmpty();
		    // remove the none widget
		    if(otherHasNone) 
		    	modelB.remove(0);
		    
		    modelA.fireChanges(); 
		    modelB.fireChanges();
		}
		
		
		public void transferWidgetsToTheLeft(int[] indices)
		{
			for(int index : indices) {
				if(modelB.getElementAt(index).equals(noneIcon))
					continue;
				DisplayIcon removed = modelB.remove(index);
				modelA.add(removed);
			}
			
		    boolean hasNone = modelA.isDisplayEmpty();
		    if(hasNone)
		    	modelA.remove(0);
		    // remove the none widget
		    modelA.fireChanges();
		    modelB.fireChanges();
		}
		
		public class AddListener implements ActionListener
		{
			public void actionPerformed(ActionEvent ae)
			{
				if(!buttonsReady)
					return;
				int[] indices = list.getSelectedIndices();
				if(indices.length == 0){
					list.setSelectedIndex(0);
					list.ensureIndexIsVisible(0);
					return;
				}
				
				transferWidgetsToTheRight(indices);
				
				boolean isEmpty = modelA.getSize() == 0;
				if (isEmpty) { 
			        modelA.add(noneIcon);
			        modelA.fireChanges();
			    }
			    else { //Select an index.
			    	int select = indices[0];
			        if (select == modelA.getSize()) {
			            //removed item in last position
			            select--;
			        }
			        list.setSelectedIndex(select);
			        list.ensureIndexIsVisible(select);
			    }
			}
		}
		
		public class RemoveListener implements ActionListener
		{
			public void actionPerformed(ActionEvent ae)
			{
				if(!buttonsReady) 
					return;
				int[] indices = list2.getSelectedIndices();
				if(indices.length == 0){
					list2.setSelectedIndex(0); 
					list2.ensureIndexIsVisible(0);
					return;
				}
				transferWidgetsToTheLeft(indices);
				boolean otherIsEmpty = modelB.getSize() == 0;
			    if (otherIsEmpty) {  //Nobody's left, disable firing.
			        modelB.add(noneIcon);
			        modelB.fireChanges();
			    }
			    else { //Select a new index.
			    	int select = indices[0];
			        if (select == modelB.getSize()) {
			            //removed item in last position
			            select--;
			        }
			        list2.setSelectedIndex(select);
			        list2.ensureIndexIsVisible(select);
			    }
			}
		}
	}
	
	
	public static class ReportFrame
	{
		public JTextArea textFrame;
		public static String reportHeader = "Saved Constraints Report:\n";
		
		public ReportFrame()
		{
			buildTextFrame();
			textFrame.setEditable(false);
			textFrame.setEnabled(false);
		}
		public void buildTextFrame()
		{
			textFrame = new JTextArea(new PlainDocument(), reportHeader, 10, 50);
			
		}
		public void writeReport()
		{
			String report = reportHeader;
			report += reportFor("\n--\n", RuleName.values());
			textFrame.setText(report);
				
		}
		public String reportFor(String separator, RuleName... print)
		{
			String toReturn = "";
			for(RuleName r : print) {
				switch(r) {
				case REP: toReturn += ReportTranslation.repeatReport(workingTaskList) + separator; 
		break;  case MEX: toReturn += ReportTranslation.exclusionReport(workingTaskList) + separator;
		break;  case REQ: toReturn += ReportTranslation.requiresReport(workingTaskList) + separator;
		break;  case ORD: toReturn += ReportTranslation.orderReport(workingTaskList) + separator;
//		break;  case STO: toReturn += ReportTranslation.stopReport(workingTaskList) + separator;
		break;  case ATM: toReturn += ReportTranslation.atomicReport(workingTaskList) + separator;
				}
			}
			return toReturn;
		}
	}
	
	private static String sp(int num)
	{
		if(num == 0)
			return "";
		else return " " + sp(num-1);
	}
	
	public static class Controller 
	{
		enum State{
			W(null, "Widgets", false),
			RQ(RuleName.REQ, "Requires", false),
			M(RuleName.MEX, "Exclusion", false),
			O(RuleName.ORD, "Order", true),
			RP(RuleName.REP, "Repeat", false),
			S(RuleName.STO, "Stop", false),
			A(RuleName.ATM, "Atomic", true),
			END(null, "End", false);

			private final RuleName ruleName;
			private final String labelString;
			final boolean isComplexRule;
			State(RuleName element, String labelString, boolean isComplexRule)
			{
				this.ruleName = element; 
				this.labelString = labelString;
				this.isComplexRule = isComplexRule;
			}
			
			public boolean isLastState()
			{
				return State.values().length-1 == ordinal();
			}
			public boolean isFirstState()
			{
				return this == RQ;
			}
			public boolean isNextToLastState()
			{
				return State.values().length-2 == ordinal();
			}
		}
		
		public NextListener nextListener;
		public PreviousListener previousListener;
		boolean isDialog;
		File writeFile;
		boolean hasWriteFile;
		boolean hasRuleState;
		public State state;
		private Window frameToUse;
		
		public Controller(Window frameToUse, File widgetsFile, RuleName stateSpec)
		{
			if(frameToUse instanceof JDialog)
				isDialog = true;
			this.frameToUse = frameToUse;
			hasWriteFile = setAndCheckFile(widgetsFile);
			for(State s : State.values()) {
				if(s.ruleName == stateSpec) {
					state = s;
					hasRuleState = true;
				}
			}
			if(!hasRuleState)
				state = State.RQ;
			previousListener = new PreviousListener();
			nextListener = new NextListener();
			manageState(state, frameToUse, widgetsFile);
			
		}
		
		public Controller(Window frameToUse, State stateSpec)
		{
			if(frameToUse instanceof JDialog)
				isDialog = true;
			this.frameToUse = frameToUse;
			hasWriteFile = false;
			hasRuleState = false;
			state = stateSpec;
			previousListener = new PreviousListener();
			nextListener = new NextListener();
			manageState(state, frameToUse, new File(""));
		}
		
		private static void manageState(State currentState, Window frameToUse, File widgetsFile)
		{
			if(currentState.isFirstState()) { 
				setupContent(frameToUse, widgetsFile, currentState);
				rulesLabels(currentState.ruleName);
				// can navigate forward
				previousButton.setEnabled(false);
				nextButton.setEnabled(true);
			}
			else if(!currentState.isLastState()){
				setupContent(frameToUse, widgetsFile, currentState); 
				rulesLabels(currentState.ruleName);
				// can freely navigate
				previousButton.setEnabled(true);
				nextButton.setEnabled(true);
			}
			else {
				setupContent(frameToUse, widgetsFile, currentState);
				endLabels();
				// can navigate backward.
				previousButton.setEnabled(true);
				nextButton.setEnabled(true);
			}
		}
		public static void writeFile(State state, File writeFile)
		{
			if(state == State.W) {
				workingTaskList = overwriteWidgets(workingTaskList, Writeout.allWidgets());
				System.out.println("\nFitting Tool: Writing Widgets to\n  \'" + writeFile + "\'");
				handler.writeObjToFile(workingTaskList, writeFile.getAbsolutePath());
				System.out.println("Fitting Tool: Done.\n");
			}
			else if(Writeout.bigMaps() > 0) {
				switch(state) {
					case RQ : case M: {
						workingTaskList = overwriteRule(workingTaskList, Writeout.modeledRuleSetAtIndex(0), state.ruleName);
						System.out.println("Fitting Tool: Writing " + state.labelString + " to\n  \'" + writeFile + "\'");
						handler.writeObjToFile(workingTaskList, writeFile.getAbsolutePath());
						System.out.println("Fitting Tool: Done.");
					}
					case O: case A: {
						workingTaskList = overwriteRule(workingTaskList, Writeout.modeledRuleSetAtIndex(0), state.ruleName);
						int ruleNums = Writeout.bigMaps();
						for(int i = 1; i < ruleNums; i++)
							workingTaskList = extendRule(workingTaskList, Writeout.modeledRuleSetAtIndex(i), state.ruleName);
						System.out.println("Fitting Tool: Writing " + state.labelString + " to\n  \'" + writeFile + "\'");
						handler.writeObjToFile(workingTaskList, writeFile.getAbsolutePath());
						System.out.println("Fitting Tool: Done.");
					}
					case RP: case S: {
						workingTaskList = overwriteRule(workingTaskList, Writeout.modeledRuleSetAtIndex(0), state.ruleName);
						System.out.println("Fitting Tool: Writing " + state.labelString + " to\n  \'" + writeFile + "\'");
						handler.writeObjToFile(workingTaskList, writeFile.getAbsolutePath());
						System.out.println("Fitting Tool: Done.");
					}
				}
			}
		}
		
		public class PreviousListener implements ActionListener
		{
			@Override
			public void actionPerformed(ActionEvent e) {
				if(state.isFirstState()) 
					return;
				// write a rule
				if(state != State.END) // was not in end state
					writeFile(state, writeFile);
				// change state
				state = State.values()[state.ordinal()-1];
				
				// setup new frame. 
				frameToUse.setVisible(false);
				Window oldFrame = frameToUse;
				// setup new frame
				if(isDialog)
					frameToUse = setupDialog(FRAME_BASE_SIZE_X, FRAME_BASE_SIZE_Y, dialogParentFrame);
				else
					frameToUse = setupFrame(FRAME_BASE_SIZE_X, FRAME_BASE_SIZE_Y);
				manageState(state, frameToUse, writeFile);
				nextButton.addActionListener(progressListeners.nextListener);
				previousButton.addActionListener(progressListeners.previousListener);
				if(isDialog)
					show((JDialog)frameToUse);
				else
					show((JFrame)frameToUse);
				oldFrame.dispose();
			}
			
		}
		public class NextListener implements ActionListener {
			/**
			 * Advances the screen to the next setup. 
			 */
			public void actionPerformed(ActionEvent ae)
			{
				if(state.isLastState())  {
					frameToUse.dispose();
					return;
				}
				
				putLists.modelOut();
				writeFile(state, writeFile);
				state = State.values()[state.ordinal()+1];
				frameToUse.setVisible(false);
				Window oldFrame = frameToUse;
				// setup new frame
				if(isDialog)
					frameToUse = setupDialog(FRAME_BASE_SIZE_X, FRAME_BASE_SIZE_Y, dialogParentFrame);
				else
					frameToUse = setupFrame(FRAME_BASE_SIZE_X, FRAME_BASE_SIZE_Y);
				manageState(state, frameToUse, writeFile);
				nextButton.addActionListener(progressListeners.nextListener);
				previousButton.addActionListener(progressListeners.previousListener);
				if(isDialog)
					show((JDialog)frameToUse);
				else
					show((JFrame)frameToUse);
				oldFrame.dispose();
			}
		}

		/**
		 * Ensures that the file specified is a non-null object, that can be potentially read from the file system.
		 * @param widgetsFile
		 * @return
		 */
		public boolean setAndCheckFile(File widgetsFile)
		{
			writeFile = widgetsFile;
			if(widgetsFile == null) 
				return false;
			if(!widgetsFile.isFile())
				return false;
			return true;
		}
	}
	

	public static List<Widget> mappedWidgets(Iterable<DisplayIcon> dwm)
	{
		LinkedList<Widget> toReturn = new LinkedList<Widget>();
		for(DisplayIcon ruleDi : dwm)
			for(Widget w : getList.theWidgets) 
				if(ruleDi.matchesIconOf(w)) {
					toReturn.add(w);
					break;
				}
		return toReturn;
	}

	
	
	public static class Writeout {
		
		public static HyperList<DisplayIcon> getStoredIcons(int ruleIndex)
		{
			if(ruleIndex >= bigMaps())
				return new HyperList<DisplayIcon>();
			putLists.modelOut();
			return putLists.setStore.get(ruleIndex);
		}
		public static int bigMaps()
		{
			return putLists.setStore.size();
		}
		
		public static List<Widget> allWidgets()
		{
			return getList.theWidgets;
		}
		
		
		public static HyperList<Widget> modeledRuleSetAtIndex(int ruleIndex)
		{
			HyperList<Widget> toReturn = new HyperList<Widget>();
			for(LinkedList<DisplayIcon> icons : getStoredIcons(ruleIndex).getListsIterable()) {
				List<Widget> newWidgets = mappedWidgets(icons);
				if(!newWidgets.isEmpty()) {
					if(toReturn.isDepthEmpty())
						toReturn.addAll(newWidgets);
					else
						toReturn.addNewList(newWidgets);
				}
			}
			return toReturn;	
		}
	}
}
