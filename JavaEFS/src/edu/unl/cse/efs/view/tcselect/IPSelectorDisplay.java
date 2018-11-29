package edu.unl.cse.efs.view.tcselect;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import javax.swing.*;

import edu.umd.cs.guitar.awb.JavaActionTypeProvider;
import edu.umd.cs.guitar.event.ActionClass;
import edu.umd.cs.guitar.model.XMLHandler;
import edu.umd.cs.guitar.model.data.ObjectFactory;
import edu.umd.cs.guitar.model.data.TaskList;
import edu.umd.cs.guitar.model.data.Widget;
import edu.unl.cse.efs.ApplicationData;
import edu.unl.cse.efs.generate.DirectionalPack;
import edu.unl.cse.efs.generate.FocusOnPack;
import edu.unl.cse.efs.generate.SearchOptions;
import edu.unl.cse.efs.generate.SearchPack;
import edu.unl.cse.efs.generate.SelectorPack;
import edu.unl.cse.efs.generate.test.TestCaseGenerate_TestAbby;
import edu.unl.cse.efs.generate.test.TestCaseGenerate_TestTim;

import static edu.unl.cse.efs.view.DecorationsRunner.*;

import edu.unl.cse.efs.view.EventFlowSlicerView;
import edu.unl.cse.efs.view.TestCaseSelector.MidContent;
import edu.unl.cse.efs.view.ft.DisplayIcon;
import edu.unl.cse.efs.view.ft.DisplayingWidgetsModel;
import edu.unl.cse.jontools.widget.TaskListConformance;

public class IPSelectorDisplay {

	private static ObjectFactory fact = new ObjectFactory();
	private static XMLHandler handler = new XMLHandler();
	public static final int FRAME_BASE_SIZE_X = 800, FRAME_BASE_SIZE_Y = 700;
	public static final float LABEL_TEXT_FONT = EventFlowSlicerView.HEADER_TEXT_FONT-3;
	public static final float LIST_TEXT_FONT = LABEL_TEXT_FONT - 3;
	public static Dimension currentWindowDimension;
	public static final DisplayIcon prototypeWidgetIcon = new DisplayIcon("mmm mmm mmm mmm mmm m");
	private static JFrame frameInUse;
	private static JDialog dialogInUse;
	private static Window windowInUse;
	private static JButton constGenB;
	private static List<Widget> inputW;
	public static ArrayList<DirectionalPack> timDirPacksOutput;
	public static ArrayList<FocusOnPack> timFocPacksOutput;
	public static ArrayList<SearchPack> abbySearchPacksOutput;
	public static List<Widget> outputScope;
	private static List<SelectorPack> outputSPack;
	public static boolean usingFrame;
	public static boolean ctrlEvent, altEvent, shiftEvent, altShiftEvent, ctrlShiftEvent;
	public static int isGreen, isMagenta, isCyan, isOrange, isYellow;
	public static boolean doTooltipRip;
	public static List<Integer> tooltipModify;
	public static void main(String[] args)
	{
		new IPSelectorDisplay(TestCaseGenerate_TestAbby.testTasklistJEditLSAbbyAuto().getWidget());
		show();
	}

	public IPSelectorDisplay(Collection<Widget> list)
	{
		inputW = new ArrayList<Widget>(list);
		constGenB = new JButton("Generate Constraints");
		constGenB.setFont(constGenB.getFont().deriveFont(LABEL_TEXT_FONT));
		timDirPacksOutput = new ArrayList<DirectionalPack>();
		timFocPacksOutput = new ArrayList<FocusOnPack>();
		abbySearchPacksOutput = new ArrayList<SearchPack>();
		outputSPack = new ArrayList<SelectorPack>();
		ctrlEvent = altEvent = shiftEvent = altShiftEvent = ctrlShiftEvent = false;
		isOrange = isCyan = isMagenta = isGreen = isYellow = -1;
		new TopContent();
		new LeftContent();
		new RightContent();
		new AbbyContent();
	}
	public static ArrayList<SelectorPack> startAndGetPacks(ApplicationData data, JFrame parentFrame)
	{
		try {
		startDialogProgrammatically(data.getWorkingTaskListFile(), parentFrame);
		} catch(InterruptedException e) {
			System.err.println("Dialog was interrupted");
		}
		ArrayList<SelectorPack> toReturn = new ArrayList<SelectorPack>();
		toReturn.addAll(timDirPacksOutput);
		toReturn.addAll(timFocPacksOutput);
		toReturn.addAll(abbySearchPacksOutput);

		JOptionPane.showMessageDialog(parentFrame, "Facet Settings created successfully.");
		return toReturn;
	}

	public static void startDialogProgrammatically(File taskListFile, JFrame parentFrame) throws InterruptedException
	{
		TaskList lastTaskList= (TaskList)handler.readObjFromFile(taskListFile, TaskList.class);
		new IPSelectorDisplay(lastTaskList.getWidget());
		showDialog(parentFrame);
	}

	public static void layout()
	{

		if(usingFrame)
			frameInUse.getContentPane().setLayout(new BoxLayout(frameInUse.getContentPane(), BoxLayout.PAGE_AXIS));
		else
			dialogInUse.getContentPane().setLayout(new BoxLayout(dialogInUse.getContentPane(), BoxLayout.PAGE_AXIS));
		TopContent.layout();
		LeftContent.layout();
		RightContent.layout();
		AbbyContent.layout();
		Dimension maxMidboxSize = new Dimension(FRAME_BASE_SIZE_X, FRAME_BASE_SIZE_Y/2);
		TopContent.base.setAlignmentX(Container.CENTER_ALIGNMENT);
		TopContent.base.setMaximumSize(new Dimension(maxMidboxSize.width, maxMidboxSize.height-200));
		JPanel midBox = new JPanel();
//		midBox.setLayout(new BoxLayout(midBox, BoxLayout.LINE_AXIS));
		midBox.setMaximumSize(maxMidboxSize);
		midBox.add(LeftContent.base);
		midBox.add(RightContent.base);
		JPanel bottomBox = new JPanel();
		bottomBox.add(AbbyContent.base);
//		bottomBox.setMaximumSize(maxMidboxSize);
		// submission panel.
		JPanel submPanel = new JPanel();
		submPanel.setLayout(new BoxLayout(submPanel, BoxLayout.PAGE_AXIS));
		submPanel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
		submPanel.add(constGenB);

		windowInUse.add(TopContent.base);
		windowInUse.add(midBox);
		windowInUse.add(bottomBox);
		windowInUse.add(submPanel);

//		submPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
//		RightContent.base.setBorder(BorderFactory.createLineBorder(Color.BLACK));

	}

	public static void doBindings()
	{
		final CloseWindowHandler closeHandler = new CloseWindowHandler("Close");
		constGenB.addActionListener(closeHandler);
		windowInUse.addWindowListener(new WindowListener() {
			@Override
			public void windowOpened(WindowEvent e) 	 {}
			public void windowClosing(WindowEvent e) 	 { CloseWindowHandler.handleExit();}
			public void windowClosed(WindowEvent e) 	 { CloseWindowHandler.handleExit();}
			public void windowIconified(WindowEvent e) 	 {}
			public void windowDeiconified(WindowEvent e) {}
			public void windowActivated(WindowEvent e) 	 {}
			public void windowDeactivated(WindowEvent e) {}

		});
		LeftContent.doBindings();
		AbbyContent.doBindings();
	}

	public static List<Widget> produceColoredWidgets(DisplayingWidgetsModel list)
	{
		ArrayList<Widget> produced = new ArrayList<Widget>();

		DisplayIcon focus = list.getElementAt(isGreen);
		for(Widget w : inputW)
			if(focus.matchesIconOf(w)) {
				produced.add(w);
				break;
			}
		focus = list.getElementAt(isMagenta);
		for(Widget w : inputW)
			if(focus.matchesIconOf(w)) {
				produced.add(w);
				break;
			}
		focus = list.getElementAt(isCyan);
		for(Widget w : inputW)
			if(focus.matchesIconOf(w)) {
				produced.add(w);
				break;
			}
		focus = list.getElementAt(isOrange);
		for(Widget w : inputW)
			if(focus.matchesIconOf(w)) {
				produced.add(w);
				break;
			}
		focus = list.getElementAt(isYellow);
		for(Widget w : inputW)
			if(focus.matchesIconOf(w)) {
				produced.add(w);
				break;
			}

		return produced;
	}
	public static List<Widget> produceWidgets(DisplayingWidgetsModel list)
	{
		ArrayList<Widget> produced = new ArrayList<Widget>();
		for(DisplayIcon di : list) {
			for(Widget w : inputW)
				if(di.matchesIconOf(w)) {
					produced.add(w);
					break;
				}
		}
		return produced;
	}

	public static List<Widget> modifyWidgetsWithSearchTerms(List<Widget> toModify, Widget baseSearch)
	{
		int rem = toModify.indexOf(baseSearch);
		tooltipModify = new ArrayList<Integer>();
		toModify.remove(rem);
		for(int i = 0; i < AbbyContent.termsM.getSize(); i++)
			for(int j = 0; j < inputW.size(); j++) {
				Widget w = inputW.get(j);
				if(AbbyContent.termsM.inDisplay.get(i).matchesIconOf(w)) {
					Widget newSearch = baseSearch.copyOf(fact);
					newSearch.setParameter(TaskListConformance.TARGET_PHOLDER_KEYWORD + "_" + w.getEventID());
					toModify.add(newSearch);
					tooltipModify.add(toModify.size()-1);
					break;
				}
			}

		return toModify;
	}
	public static ArrayList<SearchPack> generateSearchPacks(List<Widget> inScope, int type, List<Widget> aux)
	{
		ArrayList<SearchPack> toReturn = new ArrayList<SearchPack>();
		if(type == 2) {
			Widget hOpen = aux.get(0);
			Widget hClose = aux.get(2);
			Widget hList = aux.get(3);
			Widget hRun = aux.get(4);

			ArrayList<Widget> searches = new ArrayList<Widget>();
			int firstSearchIndex = inputW.size()-1;

			for(int i = 0; i < AbbyContent.termsM.getSize(); i++) {
				Widget nextSearch = inScope.get(firstSearchIndex+i);
				searches.add(nextSearch);
			}

			List<Widget> hoverSearchers = DisplayingWidgetsModel.mappedWidgets(AbbyContent.termsM.inDisplay, inScope);
			Comparator<Widget> properListOrder = new Widget.IDComparator();
			hoverSearchers.sort(properListOrder);
			searches.sort(properListOrder);
			SearchOptions o = new SearchOptions(hOpen, hClose, hRun, hList, searches, hoverSearchers);
			toReturn.add(new SearchPack(inScope, o));
		}

		return toReturn;

	}
	public static ArrayList<Widget> allNormalButtons(List<Widget> scope, List<Widget> omit)
	{
		ArrayList<Widget> toReturn = new ArrayList<Widget>();
		for(Widget w : scope) {
			String clickAct = JavaActionTypeProvider.getTypeFromActionHandler(ActionClass.ACTION.actionName);
			if(w.getType().equals(clickAct))
				toReturn.add(w);
		}
		toReturn.removeAll(omit);
		return toReturn;
	}
	public static ArrayList<FocusOnPack> generateFocusOnPacks(int type)
	{
		List<Widget> focused = new ArrayList<Widget>(produceWidgets(LeftContent.rightListM));
		ArrayList<FocusOnPack> toReturn = new ArrayList<FocusOnPack>();
		if(type == 1 || type == 2)
			toReturn.add(new FocusOnPack(focused));
		return toReturn;
	}
	public static ArrayList<DirectionalPack> generateDirectionalPacks(int type)
	{
		List<Widget> output = new ArrayList<Widget>(inputW);
		ArrayList<DirectionalPack> toReturn = new ArrayList<DirectionalPack>();

		if(type == 1) {
			if(TopContent.leftDirectionB.isSelected() || TopContent.bidirectionB.isSelected())
				toReturn.add(new DirectionalPack(false, false, output));
			if(TopContent.rightDirectionB.isSelected() || TopContent.bidirectionB.isSelected())
				toReturn.add(new DirectionalPack(true, false, output));
		}
		else if(type == 2) {
			if(TopContent.leftDirectionB.isSelected() || TopContent.bidirectionB.isSelected())
				toReturn.add(new DirectionalPack(false, true, output));
			if(TopContent.rightDirectionB.isSelected() || TopContent.bidirectionB.isSelected())
				toReturn.add(new DirectionalPack(true, true, output));
		}
		return toReturn;
	}

	public static void show()
	{
		windowInUse = frameInUse = setupFrame(FRAME_BASE_SIZE_X, FRAME_BASE_SIZE_Y);
		usingFrame = true;
		layout();
		LeftContent.leftListM.fireChanges();
		AbbyContent.helpM.fireChanges();
		TopContent.setDefaultSelected();
		doBindings();
		// show the frame
		SwingUtilities.invokeLater(new Runnable(){public void run(){
			frameInUse.pack();
			frameInUse.setVisible(true);
			currentWindowDimension = frameInUse.getSize();
		}});
	}
	public static void showDialog(JFrame parentFrame) throws InterruptedException
	{
		windowInUse = dialogInUse = setupDialog(FRAME_BASE_SIZE_X, FRAME_BASE_SIZE_Y, parentFrame);
		usingFrame = false;
		layout();
		TopContent.setDefaultSelected();
		LeftContent.leftListM.fireChanges();
		AbbyContent.helpM.fireChanges();
		doBindings();

		// show the dialog.
		dialogInUse.pack();
		SwingUtilities.invokeLater(new Runnable(){public void run(){
			currentWindowDimension = dialogInUse.getSize();
		}});
		dialogInUse.setVisible(true);
	}
	public static JDialog setupDialog(int width, int height, JFrame parentFrame)
	{
		// setup the frame

		JDialog window = new JDialog(parentFrame, "EventFlowSlicer");
		window.setModalityType(ModalityType.APPLICATION_MODAL);
		window.setPreferredSize(new Dimension(width, height));
		return window;
	}

	public static JFrame setupFrame(int width, int height)
	{
		// setup the frame
		JFrame frame = new JFrame("EventFlowSlicer");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setPreferredSize(new Dimension(width, height));
		return frame;
	}

	public static class CloseWindowHandler extends AbstractAction implements Runnable
	{
		private static final long serialVersionUID = 1L;
		public static boolean saved;
		public CloseWindowHandler(String displayString)
		{
			super(displayString);
		}
		public void actionPerformed(ActionEvent ae)
		{
			handleExit();
		}
		public void run()
		{
			handleExit();
		}
		public static int getFacetType()
		{
			if(RightContent.radioGroup.getSelection() == RightContent.type1R.getModel()) {
				return 1;
			}
			else if(RightContent.radioGroup.getSelection() == RightContent.type2R.getModel()) {
				return 2;
			}
			else
				return 3;
		}
		public static void handleExit()
		{

			if(!saved) {

				if(RightContent.radioGroup.getSelection() == RightContent.type1R.getModel()) {
					timDirPacksOutput = generateDirectionalPacks(1);
					timFocPacksOutput = generateFocusOnPacks(1);
					outputScope = new ArrayList<Widget>(inputW);
				}
				else if(RightContent.radioGroup.getSelection() == RightContent.type2R.getModel()) {
					if(isGreen == -1) {
						JOptionPane.showMessageDialog(windowInUse,
								"<html><center>A help window open button was not selected.</center><br>"
								+ "<center>Use ALT+Click to select one from the list.</center></html>");
						return;
					}
					if(isMagenta == -1) {
						JOptionPane.showMessageDialog(windowInUse,
								"<html><center>A help window search field was not selected.</center><br>"
								+ "<center>Use SHIFT+Click to select one from the list.</center></html>");
						return;
					}
					if(isCyan == -1) {
						JOptionPane.showMessageDialog(windowInUse,
								"<html><center>A help window close button was not selected.</center><br>"
								+ "<center>Use SHIFT+ALT+Click to select one from the list.</center></html>");
						return;
					}
					if(isOrange == -1) {
						JOptionPane.showMessageDialog(windowInUse,
								"<html><center>A help window search list item was not selected.</center><br>"
								+ "<center>Use CTRL+Click to select one from the list.</center></html>");
						return;
					}
					if(isYellow == -1) {
						JOptionPane.showMessageDialog(windowInUse,
								"<html><center>A help window search activation item was not selected.</center><br>"
								+ "<center>Use CTRL+SHIFT+Click to select one from the list.</center></html>");
						return;
					}
					if(AbbyContent.termsM.isDisplayEmpty()) {
						JOptionPane.showMessageDialog(windowInUse,
								"<html><center>Please use the '>' arrow button</center><br>"
								+ "<center>to select at least one search term.</center></html>");
						return;
					}

					int result = JOptionPane.showConfirmDialog(windowInUse,
							"<html><center>If the application has not been ripped,</center><br>"
							+ "<center>Should EFS rip tooltips from the search widgets specified?");
					if(result == JOptionPane.CANCEL_OPTION)
						return;
					else {
						doTooltipRip = result == JOptionPane.YES_OPTION;
						timDirPacksOutput = generateDirectionalPacks(2);
						timFocPacksOutput = generateFocusOnPacks(2);
						List<Widget> aux = new ArrayList<Widget>(produceColoredWidgets(AbbyContent.helpM));
						List<Widget> modified = new ArrayList<Widget>(inputW);
						if(doTooltipRip) {
							modified = modifyWidgetsWithSearchTerms(modified, aux.get(1));

						}
						abbySearchPacksOutput = generateSearchPacks(modified, 2, aux);
						outputScope = modified;
					}
				}

				outputSPack = new ArrayList<SelectorPack>(timDirPacksOutput);
				outputSPack.addAll(timFocPacksOutput);
				outputSPack.addAll(abbySearchPacksOutput);
				saved = true;
				windowInUse.dispose();
			}
		}
	}
	public static class RightContent
	{
		static JPanel base;
		static JLabel personaLbl, type12Lbl, type1Lbl, type2Lbl;
		static JRadioButton type12R, type1R, type2R;
		static JLabel type1Lbl1, type1Lbl2, type2Lbl1, type2Lbl2, type12Lbl1, type12Lbl2;
		static ButtonGroup radioGroup;
		public RightContent()
		{
			personaLbl = new JLabel("<html><center>3. Information Processing<br>Persona</center><html>");
			Font rightLabelFont = personaLbl.getFont().deriveFont(LABEL_TEXT_FONT);
			Font boldLabelFont = rightLabelFont.deriveFont(Font.BOLD);
			personaLbl.setFont(boldLabelFont);
			type12Lbl = new JLabel("Type-1 + Type-2");
			type12Lbl.setFont(rightLabelFont);
			type12R = new JRadioButton();
			type12R.setFont(rightLabelFont);

			type1Lbl = new JLabel("Type-1\n(Tim-Like)");
			type1Lbl.setFont(rightLabelFont);
			type1R = new JRadioButton();
			type2Lbl = new JLabel("Type-2\n(Abby-Like)");
			type2Lbl.setFont(rightLabelFont);
			// --
			type1R = new JRadioButton("Type-1 (Tim-like)");
			type2R = new JRadioButton("Type-2 (Abby-like)");
			type12R = new JRadioButton("Type-1 + Type-2");
			type1R.setFont(rightLabelFont);
			type2R.setFont(rightLabelFont);
			type12R.setFont(rightLabelFont);

			radioGroup = new ButtonGroup();
			radioGroup.add(type1R);
			radioGroup.add(type2R);
			radioGroup.add(type12R);
			type12R.setEnabled(false);
			radioGroup.setSelected(type1R.getModel(), true);
			base = new JPanel();

		}

//		public static void alignments()
//		{
//			base.setAlignmentX(JComponent.CENTER_ALIGNMENT);
//			base.setAlignmentY(JComponent.TOP_ALIGNMENT);
//
//		}
		public static void layout()
		{
//			alignments();
			base.setLayout(new BoxLayout(base, BoxLayout.PAGE_AXIS));
			JPanel ipTypes = new JPanel();
			ipTypes.setLayout(new BoxLayout(ipTypes, BoxLayout.PAGE_AXIS));
			JPanel typeRadios = new JPanel();
			typeRadios.setLayout(new BoxLayout(typeRadios, BoxLayout.PAGE_AXIS));
			typeRadios.add(type1R);
			typeRadios.add(type2R);
			typeRadios.add(type12R);
//			base.add(personaLbl);
			base.add(typeRadios);
			base.add(ipTypes);
		}
	}
	public static class TopContent
	{
		static JPanel base;
		static JLabel selectorLbl, iProcessingLbl, preferenceLbl;
		static JRadioButton leftDirectionB, rightDirectionB, bidirectionB, directB, indirectB;
		static ButtonGroup radioGroup;
		TopContent()
		{
			selectorLbl = new JLabel("Test Case Selector");
			Font topLblFont = selectorLbl.getFont().deriveFont(LABEL_TEXT_FONT);
			selectorLbl.setFont(topLblFont);
			iProcessingLbl = new JLabel("Information Processing");
			iProcessingLbl.setFont(topLblFont);
			preferenceLbl = new JLabel("1. Horizontal Examination Preference");
			preferenceLbl.setFont(topLblFont);
			leftDirectionB = new JRadioButton("Left-to-Right");
			Font buttonLabelFont = selectorLbl.getFont().deriveFont(LABEL_TEXT_FONT-2);
			leftDirectionB.setFont(buttonLabelFont);
			rightDirectionB = new JRadioButton("Right-to-Left");
			rightDirectionB.setFont(buttonLabelFont);
			bidirectionB = new JRadioButton("Bidirectional");
			bidirectionB.setFont(buttonLabelFont);
			radioGroup = new ButtonGroup();
			radioGroup.add(leftDirectionB);
			radioGroup.add(rightDirectionB);
			radioGroup.add(bidirectionB);
		}
		public static void layout()
		{
			base = new JPanel();
			base.setLayout(new BoxLayout(base, BoxLayout.PAGE_AXIS));
			selectorLbl.setAlignmentX(JComponent.CENTER_ALIGNMENT);
			iProcessingLbl.setAlignmentX(JComponent.CENTER_ALIGNMENT);
			preferenceLbl.setAlignmentX(JComponent.CENTER_ALIGNMENT);
			base.add(selectorLbl);
			base.add(iProcessingLbl);
			base.add(preferenceLbl);

			JPanel radioPane = new JPanel();
			Dimension radioPaneMaxSize = new Dimension(FRAME_BASE_SIZE_X/2, FRAME_BASE_SIZE_Y/2);
			Dimension radioPanePrefSize = new Dimension(FRAME_BASE_SIZE_X/2, FRAME_BASE_SIZE_Y/10 * 4);
			radioPane.setMaximumSize(radioPaneMaxSize);

			radioPane.setLayout(new BoxLayout(radioPane, BoxLayout.PAGE_AXIS));
			radioPane.setBorder(BorderFactory.createTitledBorder(
					BorderFactory.createLineBorder(Color.GRAY),
					"Select a Preference."));
			leftDirectionB.setAlignmentX(JComponent.CENTER_ALIGNMENT);
			rightDirectionB.setAlignmentX(JComponent.CENTER_ALIGNMENT);
			bidirectionB.setAlignmentX(JComponent.CENTER_ALIGNMENT);
			radioPane.add(leftDirectionB);
			radioPane.add(rightDirectionB);
			radioPane.add(bidirectionB);
			base.add(radioPane);
		}

		public List<DisplayIcon> gatherSelection(JList<DisplayIcon> target)
		{
			return target.getSelectedValuesList();
		}

		public static void setDefaultSelected()
		{
			radioGroup.setSelected(leftDirectionB.getModel(), true);
		}
	}

	public static class AbbyContent
	{
		static JList<DisplayIcon> availList, termsList;
		static JTextField selectedOpen, selectedSearch;
		static JScrollPane soPane, ssPane, tlPane, aPane;
		static JTextField termsField;
		static JLabel availableWLbl, helpButtonLbl, searchFieldLbl, searchTermsLbl, searchTermsDirLbl;
		static JPanel base;
		static DisplayingWidgetsModel helpM, termsM;
		static JButton addWB, removeWB;
		public AbbyContent()
		{
			helpButtonLbl = new JLabel("Help Button");
			availableWLbl = new JLabel("Use These Search Requirements");
			searchFieldLbl = new JLabel("Search Field");
			searchTermsLbl = new JLabel("<html>Search After Hovering Over...</html>");
			searchTermsDirLbl = new JLabel("<html>To add: type here and press Enter.</html>");
			helpM = new DisplayingWidgetsModel(inputW);
			termsM = new DisplayingWidgetsModel();
			availList = new JList<DisplayIcon>(helpM);
			availList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			addWB = new JButton(">");
			removeWB = new JButton("<");
			selectedOpen = new JTextField(10);
			selectedSearch = new JTextField(10);
			termsList = new JList<DisplayIcon>(termsM);
			aPane = new JScrollPane(availList);
			soPane = new JScrollPane(selectedOpen);
			tlPane = new JScrollPane(termsList);
			termsField = new JTextField(20);

			base = new JPanel();
		}

		public static void layout()
		{
			sizing();
			pushOld(base, 4, 4);
			column1();
			column2();
//			column3();
			column4();
			hardenEdits();
		}
		public static void adjustColors(int adjIdx, boolean adding)
		{
			if(!adding) {
				if(isGreen == adjIdx)
					isGreen = -1;
				else if(isGreen > adjIdx)
					isGreen--;
				if(isOrange == adjIdx)
					isOrange = -1;
				else if(isOrange > adjIdx)
					isOrange--;

				if(isCyan == adjIdx)
					isCyan = -1;
				else if(isCyan > adjIdx)
					isCyan--;
				if(isMagenta == adjIdx)
					isMagenta = -1;
				else if(isMagenta > adjIdx)
					isMagenta--;
			}
			else {
				if(isGreen > adjIdx)
					isGreen++;
				if(isOrange > adjIdx)
					isOrange++;
				if(isCyan > adjIdx)
					isCyan++;
				if(isMagenta > adjIdx)
					isMagenta++;
			}
		}
		public static void doBindings()
		{
			addWB.addActionListener(new ActionListener() {public void actionPerformed(ActionEvent ae)
			{
				List<DisplayIcon> selected = availList.getSelectedValuesList();
				if(!selected.isEmpty()) {
					for(DisplayIcon di : selected) {
						helpM.remove(di);
						termsM.add(di);
					}
					for(int i : availList.getSelectedIndices())
						adjustColors(i, false);

					helpM.fireChanges();
					termsM.fireChanges();
				}
			}
			});;
			removeWB.addActionListener(new ActionListener() {public void actionPerformed(ActionEvent ae) {
				List<DisplayIcon> selected = termsList.getSelectedValuesList();
				if(!selected.isEmpty()) {
					for(DisplayIcon di : selected) {
						termsM.remove(di);
						helpM.add(di);
					}

					termsM.fireChanges();
					helpM.fireChanges();
					for(DisplayIcon di : selected) {
						int i = helpM.findIcon(di);
						adjustColors(i, false);
					}
				}
			}});
			availList.addMouseListener(new MouseAdapter() {public void mousePressed(MouseEvent me) {
				int onMask = MouseEvent.ALT_DOWN_MASK;
				int offMask = MouseEvent.SHIFT_DOWN_MASK | MouseEvent.CTRL_DOWN_MASK;
				if((me.getModifiersEx() & (onMask | offMask)) == onMask) {
					altEvent = true;
					shiftEvent = false;
					altShiftEvent = false;
					ctrlEvent = false;
					ctrlShiftEvent = false;
				}
				else {
					onMask = MouseEvent.SHIFT_DOWN_MASK;
					offMask = MouseEvent.ALT_DOWN_MASK | MouseEvent.CTRL_DOWN_MASK;
					if((me.getModifiersEx() & (onMask | offMask)) == onMask) {
						shiftEvent = true;
						altEvent = false;
						altShiftEvent = false;
						ctrlEvent = false;
						ctrlShiftEvent = false;
					}
					else {
						onMask = MouseEvent.SHIFT_DOWN_MASK | MouseEvent.ALT_DOWN_MASK;
						offMask = MouseEvent.CTRL_DOWN_MASK;
						if((me.getModifiersEx() & (onMask | offMask)) == onMask) {
							altShiftEvent = true;
							altEvent = false;
							shiftEvent = false;
							ctrlEvent = false;
							ctrlShiftEvent = false;
						}
						else {
							onMask = MouseEvent.CTRL_DOWN_MASK;
							offMask = MouseEvent.SHIFT_DOWN_MASK | MouseEvent.ALT_DOWN_MASK;
							if((me.getModifiersEx() & (onMask | offMask)) == onMask) {
								ctrlEvent = true;
								altEvent = false;
								shiftEvent = false;
								altShiftEvent = false;
								ctrlShiftEvent = false;

							}
							else {
								onMask = MouseEvent.CTRL_DOWN_MASK | MouseEvent.SHIFT_DOWN_MASK;
								offMask = MouseEvent.ALT_DOWN_MASK;
								if((me.getModifiersEx() & (onMask | offMask)) == onMask) {
									ctrlShiftEvent = true;
									ctrlEvent = false;
									altEvent = false;
									shiftEvent = false;
									altShiftEvent = false;
								}
								else
									altEvent = shiftEvent = altShiftEvent = ctrlEvent = ctrlShiftEvent = false;
							}
						}
					}
				}

			}});
			availList.setCellRenderer(new DefaultListCellRenderer() {
				@Override
				public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
					Component current = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
					if(cellHasFocus) {
						if(altEvent) {
							isGreen = index;
							setBackground(Color.GREEN);
						}
						else if(shiftEvent) {
							isMagenta = index;
							setBackground(Color.MAGENTA);
						}
						else if(altShiftEvent) {
							isCyan = index;
							setBackground(Color.CYAN);
						}
						else if(ctrlEvent) {
							isOrange = index;
							setBackground(Color.ORANGE);
						}
						else if(ctrlShiftEvent) {
							isYellow = index;
							setBackground(Color.YELLOW);
						}
					}
					else {
						if(isGreen == index)
							setBackground(Color.GREEN);
						else if(isMagenta == index)
							setBackground(Color.MAGENTA);
						else if(isCyan == index)
							setBackground(Color.CYAN);
						else if(isOrange == index)
							setBackground(Color.ORANGE);
						else if(isYellow == index)
							setBackground(Color.YELLOW);
					}
					return current;
				}
			});
			termsField.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent ae){
				if(!termsField.getText().isEmpty()) {
					termsM.add(new DisplayIcon(termsField.getText()));
					termsM.fireChanges();
					termsField.setText("");
				}
			}});

		}
		public static void sizing()
		{
			Font availableFont = availableWLbl.getFont().deriveFont(LABEL_TEXT_FONT);
			Font listLabelFont = availableFont.deriveFont(Font.ITALIC);
			availableWLbl.setFont(availableFont);
			helpButtonLbl.setFont(listLabelFont);
			searchFieldLbl.setFont(listLabelFont);
			searchTermsLbl.setFont(listLabelFont);
//			selectedOpen.setMinimumSize(new Dimension(100, 20));
//			selectedSearch.setMinimumSize(new Dimension(100, 20));

		}

		public static void column1()
		{
			editingStartOfColumn(0);
			place(availableWLbl);
			place(aPane, 3);

		}
		public static void column2()
		{
			editingStartOfColumn(1);
			jumpRowOrColumn();
//			place(helpButtonLbl);
//			place(searchFieldLbl);
			place(addWB);
			place(removeWB);
		}
//		public static void column3()
//		{
//			editingStartOfColumn(2);
//			jumpRowOrColumn();
//			place(selectedOpen);
//			place(selectedSearch);
//		}
		public static void column4()
		{
			editingStartOfColumn(3);
			place(searchTermsLbl);
			place(tlPane, 2);
		}
	}


	public static class LeftContent
	{
		static JList<DisplayIcon> leftList, rightList, pool;
		static DisplayingWidgetsModel leftListM, rightListM;
		static JLabel availableWLbl, rightLbl, reqConstraintsLbl;
//		availableRLbl,
		static JButton addWB, removeWB;
//		addRB, removeRB;
		static JPanel base;
		public LeftContent()
		{
			availableWLbl = new JLabel("Available Widgets");
			Font availableFont = availableWLbl.getFont().deriveFont(LABEL_TEXT_FONT);
			availableWLbl.setFont(availableFont);
			rightLbl = new JLabel("Widgets in Rule");
			rightLbl.setFont(availableFont);
			addWB = new JButton(">");
			removeWB = new JButton("<");

			leftListM = new DisplayingWidgetsModel(inputW);
			rightListM = new DisplayingWidgetsModel();

			Font listFont = availableFont.deriveFont(LIST_TEXT_FONT);

			leftList = new JList<DisplayIcon>(leftListM);
			leftList.setPrototypeCellValue(prototypeWidgetIcon);
			leftList.setFont(listFont);

			rightList = new JList<DisplayIcon>(rightListM);
			rightList.setPrototypeCellValue(prototypeWidgetIcon);
			rightList.setFont(listFont);

			reqConstraintsLbl = new JLabel("2. Requires Constraints");
			Font boldLeftFont = availableFont.deriveFont(Font.BOLD);
			reqConstraintsLbl.setFont(boldLeftFont);
			base = new JPanel();
		}


		public static void doBindings()
		{
			addWB.addActionListener(new ActionListener() {public void actionPerformed(ActionEvent ae)
			{
				List<DisplayIcon> selected = leftList.getSelectedValuesList();
				if(!selected.isEmpty()) {
					for(DisplayIcon di : selected) {
						leftListM.remove(di);
						rightListM.add(di);
					}
					leftListM.fireChanges();
					rightListM.fireChanges();
				}
			}
			});;
			removeWB.addActionListener(new ActionListener() {public void actionPerformed(ActionEvent ae) {
				List<DisplayIcon> selected = rightList.getSelectedValuesList();
				if(!selected.isEmpty()) {
					for(DisplayIcon di : selected) {
						rightListM.remove(di);
						leftListM.add(di);
					}
					rightListM.fireChanges();
					leftListM.fireChanges();
				}
			}});
		}

		public static void layout()
		{
			base = new JPanel();
			base.setLayout(new BoxLayout(base, BoxLayout.PAGE_AXIS));
			JPanel constr = new JPanel();
			initialize(constr, 3, 6);
			column0(0);
			column1(1);
			column2(2);
			hardenEdits();
			reqConstraintsLbl.setAlignmentX(JComponent.CENTER_ALIGNMENT);
			base.add(reqConstraintsLbl);
			base.add(constr);
		}
		public static void column0(int col)
		{
			editingStartOfColumn(col);
			place(availableWLbl);
			JScrollPane sp = new JScrollPane(leftList);
			sp.setMinimumSize(sp.getPreferredSize());
			place(sp, 2);
		}
		public static void column1(int col)
		{
			editingStartOfColumn(col);
			jumpRowOrColumn();
			place(addWB);
			place(removeWB);
			jumpRowOrColumn();
//			place(addRB);
//			place(removeRB);
		}
		public static void column2(int col)
		{
			editingStartOfColumn(col);
			place(rightLbl);
			JScrollPane sp = new JScrollPane(rightList);
			sp.setMinimumSize(sp.getPreferredSize());
			place(sp, 2);
//			place(availableRLbl);
//			JScrollPane sp2 = new JScrollPane();
//			sp2.setMinimumSize(sp.getPreferredSize());
//			place(sp2, 2);
		}

	}




}
