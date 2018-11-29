package edu.unl.cse.jontools.widget;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.bind.Unmarshaller;

import edu.umd.cs.guitar.model.XMLHandler;
import edu.umd.cs.guitar.model.data.Atomic;
import edu.umd.cs.guitar.model.data.AtomicGroup;
import edu.umd.cs.guitar.model.data.Exclusion;
import edu.umd.cs.guitar.model.data.ObjectFactory;
import edu.umd.cs.guitar.model.data.Order;
import edu.umd.cs.guitar.model.data.OrderGroup;
import edu.umd.cs.guitar.model.data.Repeat;
import edu.umd.cs.guitar.model.data.Required;
import edu.umd.cs.guitar.model.data.Stop;
import edu.umd.cs.guitar.model.data.TaskList;
import edu.umd.cs.guitar.model.data.Widget;
import edu.unl.cse.guitarext.JavaTestInteractions;
import edu.unl.cse.jontools.paths.PathConformance;
import edu.unl.cse.jontools.paths.WildcardFiles;


/**
 * This class is responsible for preparing constraints and reading the file
 * system for the construction of inputs for a test case generator.
 *
 * This class avoids throwing exceptions as much as possible.
 * Errors are reported through the invalid bits that are set by each method.
 * @author Jonathan Saddler
 */
public class TestCaseGeneratorPreparation {

	private static ObjectFactory fact = new ObjectFactory();
	private static XMLHandler handler = new XMLHandler();
	private static LinkedList<File> invalidFiles = new LinkedList<File>();
	public static boolean reqInvalid, ordInvalid, atmInvalid, mexInvalid, repInvalid;
	public static boolean eliminateAllWindowAmbiguity = true;

	public static enum RuleName{REQ("REQ"), ORD("ORD"), ATM("ATM"), MEX("MEX"), REP("REP"), STO("STO");

		public final String folderName;
		private RuleName(String folderName)
		{
			this.folderName = folderName;
		}
	}


	// Run on input : /Users/jsaddle/Desktop/ResearchResults/CogTool-Helper-Java/CogtoolHelperResultsTest
	public static void main(String[] args)
	{
		System.out.print("Enter the top directory > ");
		try(java.util.Scanner scan = new java.util.Scanner(System.in)) {
			String fileDirectory = scan.next();
			System.out.println("\n" + fileDirectory);
			File theDir = new File(fileDirectory);
			if(theDir.exists()) {
				TaskList tl = fact.createTaskList();
//				tl = incorporateAllConstraintsFrom(theDir, tl);
				System.out.println(tl);
				System.out.println("Done.");
			}
		}
	}

	public TestCaseGeneratorPreparation(File topDirectory, List<Widget> widgets)
	{
		createDirectoryStructure(topDirectory, widgets);
	}

	public static void clearInvalidBits()
	{
		repInvalid = reqInvalid = ordInvalid = atmInvalid = mexInvalid = false;
	}


	public static TaskList overwriteWidgets(TaskList baseTaskList, List<Widget> forNewRule)
	{
		if(baseTaskList == null)
			baseTaskList = fact.createTaskList();
		List<Widget> base = baseTaskList.getWidget();
		base.clear();
		for(Widget w : forNewRule)
			base.add(w);
		return baseTaskList;
	}
	public static TaskList overwriteRule(TaskList baseTaskList, HyperList<Widget> forNewRule, RuleName rule)
	{
		if(baseTaskList == null)
			baseTaskList = fact.createTaskList();
		switch(rule) {
		case REQ: {
			List<Required> baseRequired = baseTaskList.getRequired();
			baseRequired.clear();
			for(List<Widget> list : forNewRule.getListsIterable()) {
				// for each list within, create a new Required rule.
				Required newRul = fact.createRequired();
				for(Widget w : list)
					if(w != null)
						newRul.getWidget().add(w);
				baseRequired.add(newRul);
			}
			return baseTaskList;
		}
		case MEX: {
			List<Exclusion> baseExclusion = baseTaskList.getExclusion();
			baseExclusion.clear();
			for(List<Widget> list : forNewRule.getListsIterable()) {
				// for each list within, create a new exclusion rule.
				Exclusion newRul = fact.createExclusion();
				for(Widget w : list)
					if(w != null)
						newRul.getWidget().add(w);
				baseExclusion.add(newRul);
			}
			return baseTaskList;
		}

		case REP: {
//			boolean rList = false;
//			if(forNewRule instanceof RepeatList)
//				rList = true;
			List<Repeat> baseRepeat = baseTaskList.getRepeat();
			baseRepeat.clear();
			for(List<Widget> list: forNewRule.getListsIterable()) {
				Repeat newRul = fact.createRepeat();
				for(Widget w : list)
					if(w != null)
						newRul.getWidget().add(w);
//				if(rList) {
//					RepeatList rL = (RepeatList)forNewRule;
//					// do something special here.
//				}
				baseRepeat.add(newRul);
			}
			return baseTaskList;
		}
		case STO: {
			List<Stop> baseStop = baseTaskList.getStop();
			baseStop.clear();
			for(List<Widget> list: forNewRule.getListsIterable()) {
				Stop newRul = fact.createStop();
				for(Widget w : list)
					if(w != null)
						newRul.getWidget().add(w);
				baseStop.add(newRul);
			}
			return baseTaskList;
		}
		case ORD: {
			List<Order> baseOrder = baseTaskList.getOrder();
			baseOrder.clear();
			extendRule(baseTaskList, forNewRule, RuleName.ORD);
			return baseTaskList;
		}
		case ATM: {
			List<Atomic> baseAtomic = baseTaskList.getAtomic();
			baseAtomic.clear();
			extendRule(baseTaskList, forNewRule, RuleName.ATM);
			return baseTaskList;
		}}
		return baseTaskList;
	}

	/**
	 * Only works for Atomic Rule and Order Rule.
	 * @param baseTaskList
	 * @param forNewRule
	 * @param rule
	 * @return
	 */
	public static TaskList extendRule(TaskList baseTaskList, HyperList<Widget> forNewRule, RuleName rule)
	{
		if(rule == RuleName.ORD) {
			List<Order> baseOrder = baseTaskList.getOrder();
			Order newSet = fact.createOrder();
			for(List<Widget> list : forNewRule.getListsIterable()) {
				// for each list within create one order group
				OrderGroup newG = fact.createOrderGroup();
				for(Widget w : list)
					if(w != null)
						newG.getWidget().add(w);
				newSet.getOrderGroup().add(newG);
			}
			baseOrder.add(newSet);
		}
		else if(rule == RuleName.ATM) {
			List<Atomic> baseAtomic = baseTaskList.getAtomic();
			Atomic newSet = fact.createAtomic();
			for(List<Widget> list : forNewRule.getListsIterable()) {
				// for each list within, create one atomic group
				AtomicGroup newG = fact.createAtomicGroup();
				for(Widget w : list)
					if(w != null)
						newG.getWidget().add(w);
				newSet.getAtomicGroup().add(newG);
			}
			baseAtomic.add(newSet);
		}
		return baseTaskList;
	}
	/**
	 * Try to progress through the parsing of all constraints from the folders in topDirectory.
	 * if an attempt to read one of the constraints directories fails or succeeds,
	 * progress to the next type of constraint so that it can be parsed next.<br><br>
	 *
	 * Return the task list containing the combination of the old constraints in tasklist with the
	 * new constraints read from this operation.
	 */
//	public static TaskList incorporateAllConstraintsFrom(File topDirectory, TaskList baseTasklist)
//	{
//		RuleName currentOperand = RuleName.MEX;
//		while(currentOperand != RuleName.REP) {
//			try {
//				// try to progress through the parsing of all constraints from the folders in topDirectory.
//				// if one fails or succeeds, progress to the next type of constraint so that it can be parsed next.
//				switch(currentOperand) {
//				case MEX: baseTasklist.getExclusion().addAll(readExclusionDirectory(topDirectory));
//				currentOperand = RuleName.ORD;
//				case ORD: baseTasklist.getOrder().addAll(readOrderDirectory(topDirectory));
//				currentOperand = RuleName.ATM;
//				case ATM: baseTasklist.getAtomic().addAll(readAtomicDirectory(topDirectory));
//				currentOperand = RuleName.REQ;
//				case REQ: baseTasklist.getRequired().addAll(readRequiredDirectory(topDirectory));
//				currentOperand = RuleName.REP;
//				case REP: baseTasklist.getRepeat().addAll(readRepeatDirectory(topDirectory));
//				}
//			} catch(Exception e) {
//				switch(currentOperand) {
//				case MEX: mexInvalid = true; currentOperand = RuleName.ORD; break;
//				case ORD: ordInvalid = true; currentOperand = RuleName.ATM; break;
//				case ATM: atmInvalid = true; currentOperand = RuleName.REQ; break;
//				case REQ: reqInvalid = true; currentOperand = RuleName.REP; break;
//				case REP: repInvalid = true;
//				}
//			}
//		}
//		return baseTasklist;
//	}

	/**
	 * Removes from the provided list of widgets any
	 * duplicates or widgets with null, empty, or useless eventId values
	 * and returns the processed list.
	 */
	private static List<Widget> removeUselessWidgets(List<Widget> widgets)
	{
		ArrayList<Widget> toWrite = new ArrayList<Widget>(widgets);
		HashMap<String, ArrayList<String>> windowMap = new HashMap<String, ArrayList<String>>();

		Iterator<Widget> wIt = toWrite.iterator();
		Widget nextWriteCandidate;
		while(wIt.hasNext()) {
			nextWriteCandidate = wIt.next();
			if(nextWriteCandidate.getEventID() == null
			|| nextWriteCandidate.getEventID().isEmpty()
			|| nextWriteCandidate.getEventID().equals(JavaTestInteractions.hasNoID)) {
				wIt.remove(); // can't write events with useless event Ids
			}
			else {
				String eId = nextWriteCandidate.getEventID();
				String wId = nextWriteCandidate.getWindow();
				if(windowMap.containsKey(eId)) {
					if(windowMap.get(eId).contains(wId))
						wIt.remove(); // can't write the same event window combo twice.
					else
						windowMap.get(eId).add(wId); // can separate window titles later
				}
				else {
					windowMap.put(eId, new ArrayList<String>(Arrays.asList(wId)));
				}
			}
		}

		return toWrite;
	}


	public static void createDirectoryStructure(File top, List<Widget> widgets)
	{
		File[] tops = new File[RuleName.values().length];

		for( RuleName name: RuleName.values()) {
			File vw = new File(top, name.folderName + File.separator);
			vw.mkdirs();
			if(!vw.isDirectory())
				throw new IllegalArgumentException("Directory proglem: structure rooted at directory \n" + top + "\n for CogToolHelper expermiment could not be created.");
			tops[name.ordinal()] = vw;
		}
		// make subfolders, if the first one worked, then the rest should work as well.

		(new File(tops[RuleName.REQ.ordinal()], "1" + File.separator)).mkdir();
		File ordtop = new File(tops[RuleName.ORD.ordinal()], "1" + File.separator);
		ordtop.mkdir();

		(new File(ordtop, "1" + File.separator)).mkdir(); // subfolder

		File atmtop = new File(tops[RuleName.ATM.ordinal()], "1" + File.separator);
		atmtop.mkdir();
		(new File(atmtop, "1" + File.separator)).mkdir(); // subfolder
		(new File(tops[RuleName.MEX.ordinal()], "1" + File.separator)).mkdir();

		// eliminate null ID widgets

		// add a new file to collected.

		widgets = removeUselessWidgets(widgets);
		// write these widgets to a file.
		boolean addAllWindowsLater = false;
		FileOutputStream[] fos = new FileOutputStream[widgets.size()];
		try {
			try {
				// add a new file to be written to disk
				ArrayList<File> collected = new ArrayList<File>();
				ArrayList<String> collectedWindows = new ArrayList<String>();
				boolean[] writeNew = new boolean[widgets.size()];
				File aFile;
				for(int i = 0; i < widgets.size(); i++) {
					String filename = widgets.get(i).getEventID();
					filename = PathConformance.sanitizeFilenameStringForDOT(filename);
					aFile = new File(top, filename);
					int existingIndex = collected.indexOf(aFile);
					collected.add(aFile);
					int current = collected.size()-1;
					if(existingIndex == -1) {
						existingIndex = collectedWindows.indexOf(widgets.get(i).getWindow());
						if(existingIndex != -1) {
							filename = filename + "_win" + widgets.get(i).getWindow();
							aFile = new File(top, filename);
						}
						collected.set(current, aFile);
						writeNew[i] = true;
					}
					else {
						Widget newest = widgets.get(i);
						Widget older = widgets.get(existingIndex);
						// if windows are equal but names are the same,
						if(newest.getWindow() != null && !newest.getWindow().equals(older.getWindow())) {
							if(eliminateAllWindowAmbiguity)
								addAllWindowsLater = true; // solve all ambiguities later.
							else {
								// change the file names to reflect an ambiguity that must be solved.
								String sameId = widgets.get(existingIndex).getEventID();
								String windowA = older.getWindow();
								String windowB = newest.getWindow();
								collected.set(existingIndex, new File(top, sameId + "_win[" + windowA + "]"));
								collected.set(current, new File(top, sameId + "_win[" + windowB + "]"));
							}
							writeNew[i] = true;
						}
						else // avoid writing the same file twice by flagging this file.
							writeNew[i] = false;
					}
				}
				for(int i = 0; i < widgets.size(); i++)
					if(writeNew[i]) {
						if(addAllWindowsLater) {
							String window = widgets.get(i).getWindow();
							String old = widgets.get(i).getEventID();
							old = PathConformance.sanitizeFilenameStringForDOT(old);
							collected.set(i, new File(top, old + "_win[" + window + "]"));
						}
						fos[i] = new FileOutputStream(collected.get(i));
					}

//				for(int i = 0; i < widgets.size(); i++) {
				for(int i = widgets.size()-1; i >= 0; i--) {
					if(writeNew[i]) {
						TaskList widgetTL = (new ObjectFactory()).createTaskList();
						widgetTL.getWidget().add(widgets.get(i));
						handler.writeObjToFile(widgetTL, fos[i]); // write each widget to a file named by its widget name.

					}
				}

//				for(int i = 0; i < fos.length; i++)
				for(int i = fos.length-1; i >= 0; i--)
					if(writeNew[i])
						fos[i].close();

			}
			catch(Exception e) {
				for(int i = 0; i < fos.length; i++)
					fos[i].close();
				System.out.println(e);
			}
		}
		catch(Exception e) {
			System.out.println(e);
		}
	}


	/**
	 * Read widgets grouped together in subfolders in the MEX subdirectory of topDirectory, and return them
	 * stored int a repeat object
	 * @param topDirectory
	 * @return
	 * @throws IOException
	 */
	private static List<Exclusion> readExclusionDirectory(File topDirectory) throws IOException
	{
		List<Exclusion> toReturn = new ArrayList<Exclusion>();
		// create the exclusion object
		File mexDir = new File(topDirectory, RuleName.MEX.folderName + File.separator);
		int nextDirOrdinal = 1;
		File nextDir = new File(mexDir, nextDirOrdinal + File.separator);

		// does the subdirectory contain a directory named "1"?
		// if so, read all widgets from this directory and store them in the nth exclusion rule.
		// start at directory named 1, then continue to directory named n+1.
		boolean foundNext = nextDir.exists();
		while(foundNext) {
			Exclusion nextObject = fact.createExclusion();
			nextObject.getWidget().addAll(readWidgetsFrom(nextDir));
			toReturn.add(nextObject);
			// does the subdirectory contain another directory namd "n+1", where 1 is first n?
			// if so read all the widgets from this directory and store them in the n+1'th exclusion constraint.
			nextDirOrdinal++;
			nextDir = new File(mexDir, nextDirOrdinal + File.separator);
			foundNext = nextDir.exists();
		}

		// if subdirectory does not contain directory named n, end the search, and return the list of required objects.
		return toReturn;
	}

	/**
	 * Read widgets grouped together directories of the order group directories under the ORD subdirectory of topDirectory.
	 * (topDir/ORD.folderName/n/m/(...widgets...)) and return all groups stored in an Order object.
	 * @param topDirectory
	 * @return
	 */
	private static List<Order> readOrderDirectory(File topDirectory) throws IOException
	{
		List<Order> toReturn = new ArrayList<Order>();

		// does the subdirectory contain a directory named "1"?
		// if so, search this folder for a directory named 1 as a subdirectory of this directory.
		// start at directory named 1, then continue to directory named n+1.

		File ordDir = new File(topDirectory, RuleName.ORD.folderName + File.separator);
		int nextSetOrdinal = 1;
		int nextGroupOrdinal;
		File nextSetDir = new File(ordDir, nextSetOrdinal + File.separator);
		boolean foundNext = nextSetDir.exists();

		while(foundNext) {
			nextGroupOrdinal = 1;
			File nextDir = new File(nextSetDir, nextGroupOrdinal + File.separator);
			boolean foundNextGroup = nextDir.exists();
			Order nextOrder = fact.createOrder();

			while(foundNextGroup) {
				// add the last group of widgets found to the current order object.
				OrderGroup nextGroup = fact.createOrderGroup();
				List<Widget> groupWidgets = readWidgetsFrom(nextDir);
				nextGroup.getWidget().addAll(groupWidgets);
				nextOrder.getOrderGroup().add(nextGroup);

				// does the subdirectory contain another directory namd "n+1", where 1 is first n?
				// if so read all the widgets from this directory and store them in the n+1'th order group constraint.
				nextGroupOrdinal++;
				nextDir = new File(nextSetDir, nextGroupOrdinal + File.separator);
				foundNextGroup = nextDir.exists();

			}
			// if subdirectory of directory n (m+1) does not exist, finish directory n and move on to directory
			// n+1 rather than m+1.
			// add the current order object to the list of returned objects.
			toReturn.add(nextOrder);
			nextSetOrdinal++;
			nextSetDir = new File(ordDir, nextSetOrdinal + File.separator);
			foundNext = nextSetDir.exists();
		}
		return toReturn;
	}

	/**
	 * Read widgets grouped together directories of the order group directories under the ORD subdirectory of topDirectory.
	 * (topDir/ORD.folderName/n/m/(...widgets...)) and return all groups stored in an Order object.
	 * @param topDirectory
	 * @return
	 */
	private static List<Atomic> readAtomicDirectory(File topDirectory) throws IOException
	{
		List<Atomic> toReturn = new ArrayList<Atomic>();

		// does the subdirectory contain a directory named "1"?
		// if so, search this folder for a directory named 1 as a subdirectory of this directory.
		// start at directory named 1, then continue to directory named n+1.

		File ordDir = new File(topDirectory, RuleName.ATM.folderName + File.separator);
		int nextSetOrdinal = 1;
		int nextGroupOrdinal;
		File nextSetDir = new File(ordDir, nextSetOrdinal + File.separator);
		boolean foundNext = nextSetDir.exists();

		while(foundNext) {
			nextGroupOrdinal = 1;
			File nextDir = new File(nextSetDir, nextGroupOrdinal + File.separator);
			boolean foundNextGroup = nextDir.exists();
			Atomic nextAtomic = fact.createAtomic();

			while(foundNextGroup) {
				// add the last group of widgets found to the current order object.
				AtomicGroup nextGroup = fact.createAtomicGroup();
				List<Widget> groupWidgets = readWidgetsFrom(nextDir);
				nextGroup.getWidget().addAll(groupWidgets);
				nextAtomic.getAtomicGroup().add(nextGroup);

				// does the subdirectory contain another directory namd "n+1", where 1 is first n?
				// if so read all the widgets from this directory and store them in the n+1'th order group constraint.
				nextGroupOrdinal++;
				nextDir = new File(nextSetDir, nextGroupOrdinal + File.separator);
				foundNextGroup = nextDir.exists();

			}
			// if subdirectory of directory n (m+1) does not exist, finish directory n and move on to directory
			// n+1 rather than m+1.
			// add the current order object to the list of returned objects.
			toReturn.add(nextAtomic);
			nextSetOrdinal++;
			nextSetDir = new File(ordDir, nextSetOrdinal + File.separator);
			foundNext = nextSetDir.exists();
		}

		return toReturn;
	}
	/**
	 * Read widgets grouped together in folders in the REQ subdirectory of topDirectory, and
	 * return them stored in a repeat object
	 * @param topDirectory
	 * @return
	 */
	private static List<Required> readRequiredDirectory(File topDirectory) throws IOException
	{
		List<Required> toReturn = new ArrayList<Required>();


		// does the subdirectory contain a directory named "1"?
		// if so, read all widgets from this directory and store them in the nth required rule.
		// start at directory named 1, then continue to directory named n+1.
		File reqDir = new File(topDirectory, RuleName.REQ.folderName + File.separator);
		int nextDirOrdinal = 1;
		File nextDir = new File(reqDir, nextDirOrdinal + File.separator);
		boolean foundNext = nextDir.exists();

		while(foundNext) {
			Required nextObject = fact.createRequired();
			nextObject.getWidget().addAll(readWidgetsFrom(nextDir));
			toReturn.add(nextObject);
			// does the subdirectory contain another directory namd "n+1", where 1 is first n?
			// if so read all the widgets from this directory and store them in the n+1'th required constraint.
			nextDirOrdinal++;
			nextDir = new File(reqDir, nextDirOrdinal + File.separator);
			foundNext = nextDir.exists();
		}

		// if subdirectory does not contain directory named n, end the search, and return the list of required objects.
		return toReturn;
	}
	/**
	 * Read widgets stored in the REP directory, and return them stored in a Repeat object.
	 * @param topDirectory
	 * @return
	 * @throws IOException
	 */
	private static List<Repeat> readRepeatDirectory(File topDirectory) throws IOException
	{
		Repeat toReturn = fact.createRepeat();
		File repFile = new File(topDirectory, RuleName.REP.folderName + File.separator);
		if(!repFile.exists())
			return new LinkedList<Repeat>();
		toReturn.getWidget().addAll(readWidgetsFrom(repFile));
		return Arrays.asList(new Repeat[]{toReturn});
	}


	/**
	 * Lookup the groups that exist under exclusion rules from a constraints file.
	 * This method assumes that Widget objects within widget tasklists files cannot turn up null
	 * , and that neither can an Order or Atomic.
	 * @param tlFile
	 * @return
	 */
	public static List<HyperList<Widget>> readRulesFromConstraintsFile(File widgetTLFile, RuleName mode)
	{
		TaskList baseTaskList;
		JAXBContext context;
		try{ context = JAXBContext.newInstance(TaskList.class);}
			catch(JAXBException e) {throw new RuntimeException("An JAXB XML recognizer error occurred: " + mode + " Rule could not be read.");}

		try {
			// test to see if the file is a valid TST
	    	if(!widgetTLFile.exists())
	    		recordInvalidFile(widgetTLFile);
	    	else if(widgetTLFile.isDirectory())
	    		recordInvalidFile(widgetTLFile);
	    	Unmarshaller um = context.createUnmarshaller();
	    	Object myTL = JAXBIntrospector.getValue(um.unmarshal(widgetTLFile));
	    	if(!(myTL instanceof TaskList))
	    		recordInvalidFile(widgetTLFile);
	    	baseTaskList = (TaskList)myTL;

			switch(mode) {
			case REQ: {
				ArrayList<HyperList<Widget>> collected = new ArrayList<HyperList<Widget>>();
				List<Required> base = baseTaskList.getRequired();
				HyperList<Widget> forNewRule = new HyperList<Widget>();
				// for each required rule, create a new list within
				LinkedList<Widget> addList = new LinkedList<Widget>();
				for(Required list : base) {
					for(Widget w : list.getWidget())
						addList.add(w);
					if(forNewRule.isDepthEmpty())
						forNewRule.addAll(addList);
					else
						forNewRule.addNewList(addList);
					addList.clear();
				}
				collected.add(forNewRule);
				return collected;
			}
			case MEX: {
				ArrayList<HyperList<Widget>> collected = new ArrayList<HyperList<Widget>>();
				List<Exclusion> base = baseTaskList.getExclusion();
				HyperList<Widget> forNewRule = new HyperList<Widget>();
				// for each exclusion rule, create a new list within
				for(Exclusion list : base) {
					if(forNewRule.isDepthEmpty())
						forNewRule.addAll(list.getWidget());
					else
						forNewRule.addNewList(list.getWidget());
				}
				collected.add(forNewRule);
				return collected;
			}
			case STO: {
				ArrayList<HyperList<Widget>> collected = new ArrayList<HyperList<Widget>>();
				List<Stop> base = baseTaskList.getStop();
				HyperList<Widget> forNewRule = new HyperList<Widget>();
				// for each repeat rule, create a new list within.
				LinkedList<Widget> addList = new LinkedList<Widget>();
				for(Stop list : base) {
					for(Widget w : list.getWidget())
						addList.add(w);
					if(forNewRule.isDepthEmpty()) 	forNewRule.addAll(addList);
					else							forNewRule.addNewList(addList);
					addList.clear();
				}
				collected.add(forNewRule);
				return collected;
			}
			case REP: {
				/*
				 * ArrayList<HyperList<Widget>> collected = new ArrayList<HyperList<Widget>>();
				List<Required> base = baseTaskList.getRequired();
				HyperList<Widget> forNewRule = new HyperList<Widget>();
				// for each required rule, create a new list within
				LinkedList<Widget> addList = new LinkedList<Widget>();
				for(Required list : base) {
					for(Widget w : list.getWidget())
						addList.add(w);
					if(forNewRule.isDepthEmpty())
						forNewRule.addAll(addList);
					else
						forNewRule.addNewList(addList);
					addList.clear();
				}
				collected.add(forNewRule);
				return collected;
				 */
				ArrayList<HyperList<Widget>> collected = new ArrayList<HyperList<Widget>>();
				List<Repeat> base = baseTaskList.getRepeat();
				HyperList<Widget> forNewRule = new HyperList<Widget>();
				// for each repeat rule, create a new list within.
				LinkedList<Widget> addList = new LinkedList<Widget>();
				for(Repeat list : base) {
					for(Widget w : list.getWidget())
						addList.add(w);
					if(forNewRule.isDepthEmpty()) 	forNewRule.addAll(addList);
					else							forNewRule.addNewList(addList);
					addList.clear();
				}
				collected.add(forNewRule);
				return collected;
//				// for each repeat widget, add a single widget to an overall list.
//				if(base != null)
//					for(Widget w : base.getWidget())
//						collected.add(w);
//				ArrayList<HyperList<Widget>> toReturn = new ArrayList<HyperList<Widget>>();
//				toReturn.add(new HyperList<Widget>(collected));
//				return toReturn;
			}
			case ORD: {
				ArrayList<HyperList<Widget>> collected = new ArrayList<HyperList<Widget>>();
				HyperList<Widget> forNewRule = new HyperList<Widget>();
				List<Order> base = baseTaskList.getOrder();
				for(Order list : base) {
					forNewRule.clear();
					// for each new rule create one hyperlist to represent all groups in that order rule
					for(OrderGroup g : list.getOrderGroup())
						if(g != null) {
							if(forNewRule.isDepthEmpty())
								forNewRule.addAll(g.getWidget());
							else
								forNewRule.addNewList(g.getWidget());
						}
					collected.add(forNewRule);
				}
				if(base.isEmpty())
					collected.add(forNewRule); // can't have an order set with 0 elements.

				return collected;
			}
			case ATM: {
				ArrayList<HyperList<Widget>> collected = new ArrayList<HyperList<Widget>>();
				List<Atomic> base = baseTaskList.getAtomic();
				HyperList<Widget> forNewRule = new HyperList<Widget>();
				for(Atomic list : base) {
					forNewRule.clear();
					// for each new rule create one hyperlist to
					// represent all groups in that order rule
					for(AtomicGroup g : list.getAtomicGroup())
						if(g != null) {
							if(forNewRule.isDepthEmpty())
								forNewRule.addAll(g.getWidget());
							else
								forNewRule.addNewList(g.getWidget());
						}
					collected.add(forNewRule);
				}
				if(base.isEmpty())
					collected.add(forNewRule);
				return collected;
			}
		}
		} catch(JAXBException e) {recordInvalidFile(widgetTLFile);}
		return new LinkedList<HyperList<Widget>>();
	}
	/**
	 * Lookup the widgets list in a single constraints xml tfile.
	 * @param widgetTLFile
	 * @return
	 * @throws IOException
	 */
	public static List<Widget> readWidgetsFromConstraintsFile(File widgetTLFile) throws IOException
	{
		List<Widget> collected = new ArrayList<Widget>();
		TaskList nextList;
		JAXBContext context;
		try{ context = JAXBContext.newInstance(TaskList.class);}
			catch(JAXBException e) {throw new RuntimeException("An JAXB XML recognizer error occurred: repeat widgets could not be read.");}

		try {
			// test to see if the file is a valid TST
	    	if(!widgetTLFile.exists())
	    		recordInvalidFile(widgetTLFile);
	    	else if(widgetTLFile.isDirectory())
	    		recordInvalidFile(widgetTLFile);
	    	Unmarshaller um = context.createUnmarshaller();
	    	Object myTL = JAXBIntrospector.getValue(um.unmarshal(widgetTLFile));
	    	if(!(myTL instanceof TaskList))
	    		recordInvalidFile(widgetTLFile);
	    	nextList = (TaskList)myTL;
			collected.addAll(nextList.getWidget());
		} catch(JAXBException e) {recordInvalidFile(widgetTLFile);}
		return collected;
	}

	public static List<Widget> readWidgetsFrom(File widgetTLDirectory) throws IOException
	{
		List<File> repFiles = WildcardFiles.findFiles(widgetTLDirectory.getAbsolutePath(), "");
		List<Widget> collected = new ArrayList<Widget>();
		TaskList nextList;
		JAXBContext context;
		try{ context = JAXBContext.newInstance(TaskList.class);}
		catch(JAXBException e) {throw new RuntimeException("An JAXB XML recognizer error occurred: repeat widgets could not be read.");}

		for(File f : repFiles) {
			try {
				// test to see if the file is a valid TST
		    	if(!f.exists())
		    		continue; // skip invalid files
		    	else if(f.isDirectory())
		    		continue; // skip directories found.
		    	Unmarshaller um = context.createUnmarshaller();
		    	Object myTL = JAXBIntrospector.getValue(um.unmarshal(f));
		    	if(!(myTL instanceof TaskList))
		    		continue;
		    	nextList = (TaskList)myTL;
				collected.addAll(nextList.getWidget());
			} catch(JAXBException e) {
				recordInvalidFile(f);
				continue;
			}
		}
		return collected;
	}

	private static void recordInvalidFile(File file)
	{
		invalidFiles.add(file);
	}
}
