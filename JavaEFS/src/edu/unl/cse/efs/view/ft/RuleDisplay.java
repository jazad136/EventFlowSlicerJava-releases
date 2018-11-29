package edu.unl.cse.efs.view.ft;

import static edu.unl.cse.efs.view.DecorationsRunner.*;
import static edu.unl.cse.jontools.widget.TestCaseGeneratorPreparation.readRulesFromConstraintsFile;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import edu.umd.cs.guitar.model.data.Widget;
import edu.unl.cse.efs.view.ft.FittingTool.DisplayingWidgets;
import edu.unl.cse.jontools.widget.HyperList;
import edu.unl.cse.jontools.widget.TestCaseGeneratorPreparation.RuleName;

public class RuleDisplay
{
	public DisplayingWidgets dwidgets;
	public JList<DisplayIcon> rsList, rList;
	public DisplayingWidgetsModel[] modelBs;
	public DisplayingWidgetsModel modelC;
	public DisplayingWidgetsModel modelD;
	public ArrayList<HyperList<DisplayIcon>> setStore;
	public JButton addCB, removeCB, addDB, removeDB;
	public JPanel listsPanel;
	public File displayFile;
	public RuleName loadMode;
	int loadSet, loadRule;
	public boolean hasDisplayFile;
	private CListener cList;
	private DListener dList;
	private CListener.Add cAdd;
	private CListener.Remove cRemove;
	private DListener.Add dAdd;
	private DListener.Remove dRemove;
	private JSpinner maxSpinner;
	private JSpinner minSpinner;
	public static final DisplayIcon prototypeWidgetIcon = new DisplayIcon("mmmmmmmmm mmmmmmmmm");
	public static final DisplayIcon prototypeRuleIcon = new DisplayIcon("mmmmmmmm mmmm");
	public static final DisplayIcon noneIcon = DisplayingWidgetsModel.noneIcon;

	public RuleDisplay(DisplayingWidgets toWriteList)
	{
		dList = new DListener();
		cList = new CListener();

		cAdd = cList.new Add();
		cRemove = cList.new Remove();
		dAdd = dList.new Add();
		dRemove = dList.new Remove();

		dwidgets = toWriteList;

		// set up the buttons on the right of the GUI to the left of the lists


		addCB = new JButton("Add");
		addCB.getAccessibleContext().setAccessibleName("Add_ModelC_button");
		removeCB = new JButton("Remove");
		removeCB.getAccessibleContext().setAccessibleName("Remove_ModelC_button");
		addDB = new JButton("Add");
		addDB.getAccessibleContext().setAccessibleName("Add_ModelD_button");
		removeDB = new JButton("Remove");
		removeDB.getAccessibleContext().setAccessibleName("Remove_ModelC_button");


		// set up listeners for modifying rules list.
		addCB.addActionListener(cAdd);
		removeCB.addActionListener(cRemove);
		addDB.addActionListener(dAdd);
		removeDB.addActionListener(dRemove);


		// set up gui's.
		rList = new JList<DisplayIcon>();
		rList.setPrototypeCellValue(prototypeRuleIcon);
		rList.getAccessibleContext().setAccessibleName("RuleNo_list");
		rList.setVisibleRowCount(4);
		setModelList(false, rList);
		rsList = new JList<DisplayIcon>();
		rsList.setPrototypeCellValue(prototypeRuleIcon);
		rsList.getAccessibleContext().setAccessibleName("RuleSetNo_list");
		rsList.setVisibleRowCount(4);
		setModelList(true, rsList);

		modelC = new DisplayingWidgetsModel();
		modelD = new DisplayingWidgetsModel();
		listsPanel = new JPanel();
		setStore = new ArrayList<HyperList<DisplayIcon>>();
		loadMode = null;
		loadSet = loadRule = -1;
	}

	public RuleDisplay(DisplayingWidgets toWriteList, File constraintsFile)
	{
		this(toWriteList);
		if(setAndCheckFile(constraintsFile))
			hasDisplayFile = true;
	}

	public void layoutRepeat()
	{
		initialize(listsPanel, 2, 4);
		editingStartOfColumn(0);
	}
	public void layout()
	{
		initialize(listsPanel, 2, 4);
		editingStartOfColumn(0);
		place(addCB);
		place(removeCB);
		place(addDB);
		place(removeDB);
		editingStartOfColumn(1);
		JScrollPane scrollPane = new JScrollPane(rList);
		scrollPane.setMinimumSize(scrollPane.getPreferredSize());
		place(scrollPane, 2);

		scrollPane = new JScrollPane(rsList);
		scrollPane.setMinimumSize(scrollPane.getPreferredSize());
		place(scrollPane, 2);

		rsList.setVisibleRowCount(4);
		rList.setVisibleRowCount(4);
		hardenEdits();
	}


	public boolean setAndCheckFile(File widgetsFile)
	{
		displayFile = widgetsFile;
		if(widgetsFile == null)
			return false;
		if(!widgetsFile.isFile())
			return false;
		return hasDisplayFile = true;
	}

	public void loadNew(RuleName mode)
	{
		loadNewRulesFromFile(displayFile, mode);
	}

	public void loadNewRulesFromFile(File theFile, RuleName mode)
	{
		loadMode = mode;
		try {
			setAndCheckFile(theFile);
			if(!hasDisplayFile)
				throw new RuntimeException("The file provided to the display module is invalid.");
			if(!theFile.exists())
				throw new FileNotFoundException("The file provided to the display module does not exist");
			if(theFile.isDirectory())
				throw new IllegalArgumentException("File provided to the display module must not be a directory.");
			String labelString = "";
			switch(mode) {
			case REP: case STO: {
				List<HyperList<Widget>> newWidgets = readRulesFromConstraintsFile(theFile, mode);
				modelBs = new DisplayingWidgetsModel[1]; // 1 rule containing all the widgets
				modelC = new DisplayingWidgetsModel(); // 0 types of rules containing widgets
				modelD = new DisplayingWidgetsModel(); // 0 types of rule sets containing widgets

				// add all widgets to their models.
				DisplayingWidgetsModel repWidgets = new DisplayingWidgetsModel();
				for(Widget w : newWidgets.get(0)) {
					if(!dwidgets.theWidgets.contains(w))
						dwidgets.addAvailable(w);
					repWidgets.addImmediateNoFire(new DisplayIcon(w));
				}
				setStore.add(new HyperList<DisplayIcon>(repWidgets.inDisplay));
				modelBs[0] = repWidgets;

//				int[] preSelect = prepareAvailable(0);
				newModelIn(0);
				System.out.println(this);
			}
			break; case REQ: case MEX: {
				// rule specific instructions.
				if(mode == RuleName.REQ)
					labelString = "Requires Rule";
				else if(mode == RuleName.MEX)
					labelString = "Exclusion Rule";
				List<HyperList<Widget>> newWidgets = readRulesFromConstraintsFile(theFile, mode);
//				if(newWidgets.size() == 0)
//					return;
				int counter = 0;
				modelBs = new DisplayingWidgetsModel[newWidgets.get(0).lists()];
				modelC = new DisplayingWidgetsModel();
				HyperList<DisplayIcon> allIcons = new HyperList<DisplayIcon>();
				for(HyperList<Widget> list : newWidgets) {
					// each item is either a requires rule or an exclusion rule.
					for(LinkedList<Widget> rule : list.getListsIterable()) {
						DisplayingWidgetsModel newModel = new DisplayingWidgetsModel();
						for(Widget w : rule) {
							// add widget to available widgets if not there
							if(!dwidgets.theWidgets.contains(w))
								dwidgets.addAvailable(w);
							// create an icon and add it to the new model
							DisplayIcon newIcon = new DisplayIcon(w);
							newModel.addImmediateNoFire(newIcon);
						}
						// store model in modelB's immediately, and in storage.
						modelBs[counter] = newModel;
						if(allIcons.isDepthEmpty())
							allIcons.addAll(newModel.inDisplay);
						else
							allIcons.addNewList(newModel.inDisplay);
						modelC.addImmediateNoFire(new DisplayIcon(labelString, counter+1));
						counter++;
					} // one rule done.
				}
				setStore.add(allIcons);
				rList.setModel(modelC);
				newModelIn(0);

				// set the right models

				System.out.println(this);

			}

		break;  case ORD: case ATM:
			// rule specific instructions.
			if(mode == RuleName.ORD)
				labelString = "Order Rule";
			if(mode == RuleName.ATM)
				labelString = "Atomic Rule";

			List<HyperList<Widget>> newWidgets = readRulesFromConstraintsFile(theFile, mode);
			modelD = new DisplayingWidgetsModel();
			// a hyperlist contains all groups.
			// the setStore is different.
			// the setStore stores all groups in a list.
			// the setStore stores new orderings in new hyperlists.
			setStore.clear();

			int counter = 0;

			for(HyperList<Widget> bigList : newWidgets) {
				if(bigList.isDepthEmpty()) {// no order groups.
					setStore.add(new HyperList<DisplayIcon>(Arrays.asList(noneIcon)));
					modelD.addImmediateNoFire(new DisplayIcon(labelString, ++counter));
				}
				else {
					HyperList<DisplayIcon> ruleIcons = new HyperList<DisplayIcon>();
					for(LinkedList<Widget> list : bigList.getListsIterable()) {
						DisplayingWidgetsModel newModel = new DisplayingWidgetsModel();
						for(Widget w : list) {
							if(!dwidgets.theWidgets.contains(w))
								dwidgets.addAvailable(w);
							// create and add icon to model.
							newModel.addImmediateNoFire(new DisplayIcon(w));
						}
						if(newModel.getSize() == 0) {
							if(ruleIcons.isDepthEmpty()) ruleIcons.add(noneIcon);
							else ruleIcons.startNewListWith(noneIcon);
						}
						else {
							if(ruleIcons.isDepthEmpty()) ruleIcons.addAll(newModel.inDisplay);
							else ruleIcons.addNewList(newModel.inDisplay);
						}
					}
					setStore.add(ruleIcons);
					modelD.addImmediateNoFire(new DisplayIcon(labelString, (++counter)));

				}// end full rule loop
			}
			// end if
			rsList.setModel(modelD);
			loadSet = 0;
			rsList.setSelectedIndex(0);
			System.out.println(this);
			}
		}
		catch(FileNotFoundException e) {
			modelC = new DisplayingWidgetsModel();
			modelBs = new DisplayingWidgetsModel[0];
		}
	}
	public String loadModeName()
	{
		if(loadMode == null)
			return "";
		else switch(loadMode) {
			case REQ: return "Requires";
			case MEX: return "Exclusion";
			case ORD: return "Order";
			case ATM: return "Atomic";
			case STO: return "Stop";
			case REP: return "Repeat";
			default:  return "";
		}
	}


	public String spacedRulesListing(int numSpaces)
	{
		String toReturn = "";
		boolean rep = loadMode == RuleName.REP;
		for(int i = 0; i < setStore.size(); i++) {
			HyperList<DisplayIcon> ruleCons = setStore.get(i);
			if(!rep)
				toReturn += "\n\nRule " + (i+1);

			for(int j = 0; j < ruleCons.lists(); j++) {
				if(!rep)
					toReturn += "\n" + sp(numSpaces) + "Group " + (j+1) + ":";
				int newSpaces = rep ? numSpaces : numSpaces + 2;
				if(ruleCons.list(j).isEmpty())
					toReturn += "\n" + sp(newSpaces) + "(none)";
				else
					for(DisplayIcon di : ruleCons.list(j))
						if(di.compareString.equals("none"))
							toReturn += "\n" + sp(newSpaces) + "(none)";
						else
							toReturn += "\n" + sp(newSpaces) + di.secondString;
			}
		}
		return toReturn;
	}
	/**
	 * Print a string representing the contents of displaying widgets list.
	 */
	public String toString()
	{
		boolean rep = loadMode == RuleName.REP;
		int numRules = rep ? setStore.get(0).size() : setStore.size();
		String toReturn = "\nLoaded " + numRules + " " + loadModeName() + " Rule(s)...";
		toReturn += spacedRulesListing(2);
		return toReturn;
	}

	/**
	 * Get the indices of the right display icons to move from the available
	 * list to the new list.
	 * @return
	 */
	public int[] prepareAvailable(int index)
	{
		DisplayingWidgetsModel nextModel = modelBs[index];
		if(nextModel.isDisplayEmpty())
			return new int[0];
		if(nextModel.inDisplay.get(0).equals(DisplayingWidgetsModel.noneIcon))
			return new int[0];

		int[] toReturn = new int[nextModel.getSize()];
		int next = 0;
		// Find the indices of the widgets in the available list that need swapping.
		for(DisplayIcon di : nextModel)
			toReturn[next++] = dwidgets.availableIndex(di);
		// and return them.
		return toReturn;
	}


	/**
	 * Pushes updates from the display into storage, so that they can be read using the
	 * modeledWidgets and modeledRules methods.
	 * @param ruleIndex
	 */
	public void modelOut()
	{
		if(loadRule != -1) {
			setStore.get(loadSet != -1 ? loadSet : 0).setList(loadRule, dwidgets.modelB.inDisplay);
			modelBs[loadRule] = new DisplayingWidgetsModel(dwidgets.modelB);
		}
	}

	/**
	 * @return
	 */
	public void createNewSet()
	{

	}

	/**
	 * Code that creates a brand new group to draw from
	 */
	public void createNewGroup()
	{

	}
	/**
	 * This method transfers needed widgets to the right-side list
	 * of the displaying widgets list, and carries out necessary gui related actions
	 * related to the transferral, and the transferral only.
	 * Does not do anything to the rs list or r list.
	 * Postconditions: 	Widgets specified in preSelect are transferred to the right
	 * 					The variables loadRule is set to index,
	 * 					The variable loadSet is set to setIndex
	 */
	public void newModelIn(int index, int setIndex, int[] preSelect)
	{
		// add the widgets verbatim to the model in mind.


		dwidgets.transferWidgetsToTheRight(preSelect);
		if(dwidgets.modelA.getSize() > 0)
			dwidgets.list.ensureIndexIsVisible(0);
		loadRule = index;
	}

	/**
	 * Postconditions: Widgets specified in preSelect are transferred to the right.
	 * 				   The variable loadRule is set to index.
	 * @param index
	 * @param preSelect
	 */
	public void newModelIn(int targetRule)
	{
		int[] preSelect = prepareAvailable(targetRule);
		dwidgets.transferWidgetsToTheRight(preSelect);
		if(dwidgets.modelA.getSize() > 0)
			dwidgets.list.ensureIndexIsVisible(0);
		if(modelC.getSize() > 0) {
			loadRule = -1;
			rList.ensureIndexIsVisible(targetRule);
			rList.setSelectedIndex(targetRule);
		}
		loadRule = targetRule;
	}

	/**
	 * Load in a new set of order rules or atomic rules
	 * @param bigSetIndex
	 */
	public void newBigSetIn(int bigSetIndex, int targetRuleToLoad)
	{
		// for a particular big set, get all the information out pertaining
		// to the widgets we need to model for this object
		HyperList<DisplayIcon> modelCons = setStore.get(bigSetIndex);
		modelBs = new DisplayingWidgetsModel[modelCons.lists()];
		modelC = new DisplayingWidgetsModel();
		int counter = 0;
		for(List<DisplayIcon> list : modelCons.getListsIterable()) {
			DisplayingWidgetsModel newModel = new DisplayingWidgetsModel();
			for(DisplayIcon di : list)
				newModel.addImmediateNoFire(di);
			modelBs[counter] = newModel;
			modelC.addImmediateNoFire(new DisplayIcon(loadModeName() + " Group", (counter+1)));
			counter++;
		}
//		loadSet = -1;
//		rsList.setSelectedIndex(bigSetIndex);
//		loadSet = bigSetIndex;

		loadRule = -1;
		rList.setModel(modelC);
		newModelIn(targetRuleToLoad);
		loadRule = 0;
		loadSet = bigSetIndex;

	}
	public JList<DisplayIcon> setModelList(boolean modelD, JList<DisplayIcon> theList)
	{
		if(modelD) {
			this.rsList = theList;
			rsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		}
		else {
			this.rList = theList;
			rList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		}

		boolean foundC = false;
		boolean foundD = false;
		if(this.rsList != null && rList != null) {
			for(ListSelectionListener rsl : rList.getListSelectionListeners())
				if(!foundC && rsl instanceof CListener)
					foundC = true;
				else if(!foundD && rsl instanceof DListener)
					foundD = true;

			if(!foundC) {
				rList.addListSelectionListener(cList);
			}
			if(!foundD) {

				rsList.addListSelectionListener(dList);
			}

		}

		return theList;
	}
	public void programmaticallySetRListIndex(int highlight)
	{
		int oldRule = highlight;
		loadRule = -1;
		rList.ensureIndexIsVisible(highlight);
		rList.setSelectedIndex(highlight);
		loadRule = oldRule;
	}

	private static String sp(int num)
	{
		if(num == 0)
			return "";
		else return " " + sp(num-1);
	}

	public class CListener implements ListSelectionListener
	{
		class Add implements ActionListener {
			public void actionPerformed(ActionEvent ae)
			{
				// make correct icon name.
				String iconName = loadModeName();
				switch(loadMode) {
					case REQ: case MEX: iconName += " Rule"; break;
					default: iconName += " Group";
				}
				// add the icon to the model.
//				String newName = iconName + (modelC.getSize()+1);
				modelC.add(new DisplayIcon(iconName, modelC.getSize()+1));
				modelC.fireChanges();
				// update the background models.
				int modifyList = loadSet != -1 ? loadSet : 0;
				int newRuleIndex = setStore.get(modifyList).lists(); // changed to lists()
				DisplayingWidgetsModel[] newModelBs = new DisplayingWidgetsModel[newRuleIndex+1];
				for(int i = 0; i < modelBs.length; i++)
					newModelBs[i] = modelBs[i];
				newModelBs[newRuleIndex] = new DisplayingWidgetsModel();
				modelBs = newModelBs;
				setStore.get(modifyList).addNewList();

				// update the GUI.
				//Ensure that the index in the list points to the group we added.
				//Ensure that modelB has the model we wish to display which should be empty.
				rList.ensureIndexIsVisible(newRuleIndex);
				rList.setSelectedIndex(newRuleIndex);
				System.out.print(spacedRulesListing(2));
				// depending on the status of loadSet, add a new list to the current rule set.
			}
		}
		class Remove implements ActionListener {
			public void actionPerformed(ActionEvent ae)
			{
				if(modelC.getSize() == 1)
					return;
				int modifySet = loadSet == -1 ? 0 : loadSet;
				int modifyRule = loadRule;

				// remove icon from the GUI
				modelC.remove(modelC.getSize()-1);
				// remove from the models.
				DisplayingWidgetsModel[] newModelBs = new DisplayingWidgetsModel[modelBs.length-1];
				for(int i = 0, u = 0; i < newModelBs.length; i++, u++) {
					newModelBs[i] = modelBs[u];
					if(u+1 == loadRule) u++; // do this once to leave the old widget model out.
				}




				if(modifyRule == 0 && modelBs.length == 0){

				}
				else {
				// make the change obvious in the GUI
					int highlight = modifyRule != 0 ? modifyRule-1 : modifyRule;
					rList.ensureIndexIsVisible(highlight);
					rList.setSelectedIndex(highlight);
				}
				modelBs = newModelBs;
				setStore.get(modifySet).removeList(modifyRule);
				modelC.fireChanges();
				System.out.print(spacedRulesListing(2));
			}
		}




		public void valueChanged(ListSelectionEvent e)
		{
			int ruleIndex = rList.getSelectedIndex();
			if(!e.getValueIsAdjusting()) {
				if(loadRule != -1) {
					// old model out
					modelOut();
					dwidgets.resetModels(true);
					// new model in.
					newModelIn(ruleIndex);
				}
			}
		}
	}

	public class DListener implements ListSelectionListener
	{
		class Add implements ActionListener {
			public void actionPerformed(ActionEvent ae)
			{
				String groupingName = " Rule";
				// add icon to model
				modelD.add(new DisplayIcon(loadModeName() + groupingName, (modelD.getSize()+1)));
				modelD.fireChanges();
//				modelD.addImmediateNoFire(new DisplayIcon(loadModeName() + groupingName, (modelD.getSize()+1)));

				//add a new list to the current rule set.
//				setStore.get(modifyList).addNewList();
				int newSetIndex = setStore.size();

				setStore.add(new HyperList<DisplayIcon>(Arrays.asList(noneIcon)));
				// ensure the index is visible
				rsList.ensureIndexIsVisible(newSetIndex);
				// start the chain reaction that sets up the groups.
				rsList.setSelectedIndex(newSetIndex);
				// update the background models
			}
		}

		class Remove implements ActionListener {
			public void actionPerformed(ActionEvent ae)
			{
				if(modelD.getSize() == 0)
					return;
				int modifySet = loadSet == -1 ? 0 : loadSet;
				int modifyRule = loadRule;

				// remove an icon from the GUI
				modelD.remove(modelD.getSize()-1);
				// remove from the models.
				if(modifyRule == 0 && modelBs.length == 0){
					loadSet = loadRule = -1;
				}
				else {
					// make the change obvious in the GUI
					int highlight = modifySet != 0 ? modifySet-1 : modifySet;
					rsList.ensureIndexIsVisible(highlight);
					rsList.setSelectedIndex(highlight);
				}
				setStore.remove(modifySet);
				modelD.fireChanges();
				System.out.print(spacedRulesListing(2));
			}
		}

		@Override
		public void valueChanged(ListSelectionEvent e)
		{
			if(!e.getValueIsAdjusting()) {
				int s = rsList.getSelectedIndex();
				if(loadSet != -1 && s >= 0) {
					modelOut();
					dwidgets.resetModels(true);
					newBigSetIn(s, 0);
				}

			}
		}
	}

}

