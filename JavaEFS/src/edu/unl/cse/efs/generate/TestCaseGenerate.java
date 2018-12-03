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
package edu.unl.cse.efs.generate;

import static edu.unl.cse.efs.bkmktools.TSTBookmarking.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.accessibility.AccessibleRole;

import edu.umd.cs.guitar.awb.JavaActionTypeProvider;
import edu.umd.cs.guitar.event.ActionClass;
import edu.umd.cs.guitar.model.GUITARConstants;
import edu.umd.cs.guitar.model.XMLHandler;
import edu.umd.cs.guitar.model.data.*;
import edu.umd.cs.guitar.model.wrapper.*;
import edu.unl.cse.efs.tools.ArrayTools;
import edu.unl.cse.jontools.paths.TaskListConformance;
import edu.unl.cse.jontools.string.AlphabetIterator;

/**
 * Class bearing the main level of support for the EventFlowSlicer test case generator.
 * Takes an EFG, and hosts a few algorithms that support extracting a number of
 * test cases from that EFG.
 * @author Jonathan Saddler
 */
public class TestCaseGenerate
{
	public EFG baseGraph, postEFG;
	private GUIStructureWrapper guiStructureAdapter;
	static final int UNBOUNDED_REPEATS = -2;
	static final int NOT_DEAD_COMPLETED = 1;
	static final int DEAD_COMPLETED = -2;

	private String outputDirectory;
	private static ObjectFactory fact = new ObjectFactory();
	private List<RowType> graphRows;
	private List<EventType> allEvents;
	private List<Widget> allWidgets;
	public long firstTime;
	public long algoDurationTime, ioHandlingTime;
	public boolean[] repeatable;
	private boolean continueGeneration, highPathPromptShown;
	/** an integer array with strides **/
	public int[][] repeats;
	public Set<Integer> atomicRepeatEvents;
	public int[][] mutuallyRequiredSets;
	public int[][] exclusionSets;
	public int[][] orderSets;
	public int[][] atomicSets;
	public static final int HIGH_PATH_LENGTH= 100;

	private List<OrderGroup> orderGroups;
	private List<AtomicGroup> atomicGroups;

	public TestCaseGenerate(EFG inputFlowGraph, GUIStructure guiData, String outputDirectory)
	{
		this(inputFlowGraph, guiData, fact.createTaskList(), outputDirectory);
	}

	/**
	 * Preconditions: 	inputFlowGraph and guiData and outputDirectory are non-null.
	 *
	 * Postconditions: 	The output directory is set.
	 * 					The base graph is initialized to point to objects referenced by pacified graph.
	 * 					Constraints event arrays, event subarrays, and widgets lists are initialized.
	 * 					Edges can now be retrieved by outgoingEdgesFrom, and constraints can be checked by the test methods.
	 * 					Statistical timing variables are reset to 0-values.
	 */
	public TestCaseGenerate(EFG inputFlowGraph, GUIStructure guiData, TaskList constraints, String outputDirectory)
	{
		if(inputFlowGraph == null)
			throw new RuntimeException("Null EFG flow graph was passed to TestCaseGenerate constructor.");
		if(guiData == null)
			throw new RuntimeException("Null GUI Structure data was passed to TestCaseGenerate constructor.");
		if(outputDirectory == null)
			throw new RuntimeException("Null output directory was passed to TestCaseGenerate constructor.");

		// initialize the output directory
		this.outputDirectory = outputDirectory;
		// initialize the graph operations are based on.
		baseGraph = inputFlowGraph;
		graphRows = baseGraph.getEventGraph().getRow();
		// initialize the list of events
		allEvents = baseGraph.getEvents().getEvent();

		guiStructureAdapter = new GUIStructureWrapper(guiData);
		guiStructureAdapter.parseData();
		continueGeneration = true;
		highPathPromptShown = false;
		setupIndexSets(constraints);
		resetTimes();
	}

	private Collection<Integer> eventsOf(List<Widget> widgets)
	{
		HashSet<Integer> toReturn = new HashSet<Integer>();
		for(Widget w : widgets) {
			int next = findEvent(w);
			if(next != -1)
				toReturn.add(next);
		}
		return toReturn;
	}

	/**
	 * A repeat atomic is an atomic rule containing a certain widget
	 * w, such that w appears successively at two consecutive spots in a sequence.<br>
	 * (if w, x, and y, are widgets, the sequence (x, y, w, w) would imply w
	 * is a repeat atomic).<br>
	 * Returns the indices of repeat atomic widgets in a.
	 * @param a
	 * @return
	 */
	public Collection<Integer> detectLongTermRepeatAtomicEvents(Atomic a)
	{
		int[] found = new int[allEvents.size()];

		HashSet<Integer> repeats = new HashSet<Integer>();
//		HashSet<Integer> collected = new HashSet<Integer>();
//		HashSet<Integer> transfer = new HashSet<Integer>();
		// for each group, see which ones match those already collected,
		// then refill the set of the ones collected with the ones found.
		if(a == null || a.getAtomicGroup() == null)
			return repeats;
		for(AtomicGroup ag : a.getAtomicGroup()) {
			HashSet<Integer> collect = new HashSet<Integer>(eventsOf(ag.getWidget()));
			for(int i : collect)
				found[i]++;
		}
		for(int i = 0; i < found.length; i++)
			if(found[i] >= 2)
				repeats.add(i);
		return repeats;
	}

	public void resetTimes()
	{
		firstTime = algoDurationTime = ioHandlingTime = 0;
	}
	public int runAlgorithmAndWriteResultsToOutputDirectory(boolean unbookmarkOutput) throws IOException
	{
		List<TestCase> output = runAlgorithm();
		ioHandlingTime = System.currentTimeMillis();
		if(unbookmarkOutput) {
			// remove the traces of bookmarking from the outputted test case.
			List<TestCase> holder = new LinkedList<TestCase>(output);
			output.clear();
			TSTUnBookmarking tstB;
			for(TestCase tc : holder) {
				tstB = new TSTUnBookmarking(tc);
				output.add(tstB.getUnBookmarked());
			}
		}

		File outputDir = new File(outputDirectory + File.separator + "TC");
		if(!outputDir.exists())
			outputDir.mkdirs();

		System.out.println("Writing " + output.size() + " testcase files to ...\n" + outputDir);

		AlphabetIterator aIt = new AlphabetIterator();
		FileOutputStream[] allFos = new FileOutputStream[output.size()];
		XMLHandler handler = new XMLHandler();
		for(int i = 0; i < allFos.length; i++) {
			File newFile = new File(outputDir.getAbsolutePath() + File.separator + "output_testcase_" + aIt.next() + ".tst");
			allFos[i] = new FileOutputStream(newFile);
			handler.writeObjToFile(output.get(i), allFos[i]);
		}
		for(int i = 0; i < allFos.length; i++)
			allFos[i].close();
		ioHandlingTime = System.currentTimeMillis() - ioHandlingTime;
		return output.size();
	}

	/**
	 * Ensure that the structures this class uses to detect membership in parameterized constraint
	 * patterns are initialized so the algorithm can be run.
	 *
	 * Preconditions: 	the events list must be initialized.
	 * Postconditions: 	Widgets lists, constraints event arrays, and constraints event subarrays are initialized.
	 */
	public void setupIndexSets(TaskList constraints)
	{
		boolean doSetup = true;
		if(constraints == null)
			doSetup = false;

		if(doSetup && constraints.getWidget() != null)
			allWidgets = constraints.getWidget();
		else
			allWidgets = new ArrayList<Widget>();

		if(doSetup && constraints.getExclusion() != null) {
			List<Exclusion> graphExclusions = constraints.getExclusion();
			exclusionSets = new int[graphExclusions.size()][];
			for(int i = 0; i < graphExclusions.size(); i++) {
				List<Widget> oneSet = graphExclusions.get(i).getWidget();
				int[] excSet = new int[oneSet.size()];
				for(int j = 0; j < oneSet.size(); j++)
					excSet[j] = findEvent(oneSet.get(j));

				exclusionSets[i] = excSet;
			}
		}
		else
			exclusionSets = new int[0][0];

		// atomics, just assign groups to sets, not widgets to groups

		atomicGroups = new ArrayList<AtomicGroup>();
		atomicRepeatEvents = new HashSet<Integer>();
		boolean setAtomics = false;
		if(doSetup && constraints.getAtomic() != null) {
			List<Atomic> graphAtomics = constraints.getAtomic();
			atomicSets = new int[graphAtomics.size()][];
			int setNum = -1;
			int groupNum = -1;
			for(Atomic a : graphAtomics) {
				setNum++;
				List<AtomicGroup> oneGroup = a.getAtomicGroup();
				if(oneGroup.size() == 0 || oneGroup.get(0) == null
				|| oneGroup.get(0).getWidget() == null || oneGroup.get(0).getWidget().isEmpty())
					continue;
				int[] groupSet = new int[oneGroup.size()];
				for(int i = 0; i < groupSet.length; i++) {
					atomicGroups.add(oneGroup.get(i));
					groupSet[i] = ++groupNum;
				}
				atomicSets[setNum] = groupSet; // stores the numbers of the related groups at the next 2D-array location
				setAtomics = true;
				atomicRepeatEvents.addAll(detectLongTermRepeatAtomicEvents(a));
			}
		}
		if(!setAtomics) {
			atomicSets = new int[0][0];
		}


		// order, just assign groups to sets, not widgets to groups

		orderGroups = new ArrayList<OrderGroup>();
		boolean setOrders = false;
		if(doSetup && constraints.getOrder() != null) {
			List<Order> graphOrders = constraints.getOrder();
			orderSets = new int[graphOrders.size()][];
			int setNum = -1;
			int groupNum = -1;
			for(Order o : graphOrders) {
				setNum++;
				List<OrderGroup> oneGroup = o.getOrderGroup();
				if(oneGroup.size() == 0 || oneGroup.get(0) == null
				|| oneGroup.get(0).getWidget() == null || oneGroup.get(0).getWidget().isEmpty())
					continue;
				int[] groupSet = new int[oneGroup.size()];
				for(int i = 0; i < groupSet.length; i++) {
					orderGroups.add(oneGroup.get(i));
					groupSet[i] = ++groupNum;
				}
				orderSets[setNum] = groupSet; // stores the numbers of the related groups in this groupset.
				setOrders = true;
			}
		}
		if(!setOrders)
			orderSets = new int[0][0];

		// required. Just assign widgets to sets.
		if(doSetup && constraints.getRequired() != null) {
			List<Required> graphRequireds = constraints.getRequired();
			mutuallyRequiredSets = new int[graphRequireds.size()][];
			for(int i = 0; i < graphRequireds.size(); i++) {
				List<Widget> oneSet = graphRequireds.get(i).getWidget();
				int[] reqSet = new int[oneSet.size()];
				for(int j = 0; j < oneSet.size(); j++)
					reqSet[j] = findEvent(oneSet.get(j));

				mutuallyRequiredSets[i] = reqSet;
			}
		}
		else
			mutuallyRequiredSets = new int[0][0];

		// repeat. assign widget numbers to an array, and repeatabilities to a hashmap
		if(doSetup && constraints.getRepeat() != null) {
			repeatable = new boolean[allEvents.size()];
			repeats = new int[allEvents.size()][0];
			List<Repeat> graphRepeats = constraints.getRepeat();
			for(int i = 0; i < graphRepeats.size(); i++) {
				Repeat enclosure = graphRepeats.get(i);
				List<Widget> oneSet = enclosure.getWidget();
				// test the rule
				int nextMin = enclosure.testAndReturnMinBound(-1);
				int nextMax = enclosure.testAndReturnMaxBound(-1);
				if(nextMin == -1 || nextMax == -1)
					continue;
				// apply to widgets.
				for(Widget w : oneSet) {
					int nextIdx = findEvent(w);
					if(nextIdx != -1) {
						// make that widget repeatable
						repeatable[nextIdx] = true;
						// extend its repeat object.
						int nextPos = repeats[nextIdx].length;
						repeats[nextIdx] = ArrayTools.extendRangeInt(nextPos+2, repeats[nextIdx]);
						repeats[nextIdx][nextPos] 	= nextMin;
						repeats[nextIdx][nextPos+1] = nextMax;
					}
				}
			}
		}
		else {
			repeatable = new boolean[0];
			repeats = new int[0][0];
		}
//		if(doSetup && graphRepeats != null && graphRepeats.getWidget().size() > 0) {
//			repeatableEvents = new int[graphRepeats.getWidget().size()];
//			List<Widget> repeatWidgets = graphRepeats.getWidget();
//			for(int i = 0; i < repeatWidgets.size(); i++)
//				repeatableEvents[i] = findEvent(repeatWidgets.get(i));
//		}
//		else
//			repeatableEvents = new int[0];

	}


	/**
	 * This method inserts parameters from the constraints file by mapping
	 * each StepType in steps to a widget from the constraints file, and copying the parameter
	 * string into the StepType. Returned is the list of modified StepType objects.
	 * @param steps
	 * @return
	 */
	private List<StepType> insertParamsAndSteps(List<StepType> steps)
	{
		try {
			for(int i=0; i<steps.size(); i++) {
				// NOTE, DOESN'T SUPPORT MULTIPLE EVENT ID'S IN DIFFERENT WINDOWS jsaddle
				String targetEId = steps.get(i).getEventId();
				Widget w = allWidgets.get(findWidgetByEventId(targetEId));
				List<String> params = new ArrayList<String>();
				String param  = w.getParameter();
				if(param==null) continue; // if there is no parameter.
				params.add(param);

//				String role = targetEId != null && !targetEId.isEmpty() ? targetEId.substring(0, targetEId.indexOf("_")) : "";
				steps.get(i).setParameter(params);
				//insert extra steps for typing events to move the cursor
				if(w.getAction().equals("Type")) {
//				if(w.getAction().equals("Type") && !role.equals(AccessibleRole.PANEL.toDisplayString())){

					// add cursor step and cursor appendix arguments for text steps.

					// get indices
					String[] paramTokens = param.split(GUITARConstants.NAME_SEPARATOR);
					int textIndex = 0;
					int cursorIndex = 0;
					if(paramTokens.length>2) textIndex = Integer.parseInt(paramTokens[2]);
					if(paramTokens.length>3) cursorIndex = Integer.parseInt(paramTokens[3]);
					// add cursor appendix if necessary
					String addOn = "";
					if(paramTokens.length <= 2)
						addOn += GUITARConstants.NAME_SEPARATOR + textIndex;
					if(paramTokens.length <= 3)
						addOn += GUITARConstants.NAME_SEPARATOR + cursorIndex;
					if(!addOn.isEmpty()) { // change the parameter we saved previously.
						param += addOn;
						params.set(0, param);
						steps.get(i).setParameter(params);
					}
					// cursor step
					StepType step = fact.createStepType();
					step.setEventId(targetEId);
					step.setReachingStep(false);
					String textParam = "Cursor" + GUITARConstants.NAME_SEPARATOR + cursorIndex + GUITARConstants.NAME_SEPARATOR + textIndex;

					List<String> sParams = new ArrayList<String>();
					sParams.add(textParam);
					step.setParameter(sParams);
					steps.add(i, step);

					i++;
				}
			}
		} catch(Exception e) {
			System.err.println("Param insertion issue: " + e.getClass().getSimpleName());
		}

		return steps;
	}

	public static String tcSubfolderName()
	{
		return "TC";
	}
	/**
	 *
	 * @return
	 */
	public List<TestCase> runAlgorithm()
	{
		firstTime = System.currentTimeMillis();

		ArrayList<TestCase> toReturn = new ArrayList<TestCase>();
		int e = exclusionSets.length;
		int o = orderSets.length;
		int a = atomicSets.length*2;
		int[] orig_ec = new int[e];
		int[] orig_oc = new int[o];
		int[] orig_ac = new int[a];
		// initialize the path maps.
		for(int i = 0; i < e; i++)
			orig_ec[i] = -1;
		for(int i = 0; i < o; i++)
			orig_oc[i] = -1;
		for(int i = 0; i < atomicSets.length; i++)
			orig_ac[i] = -1;

		List<Integer> orig_path = new LinkedList<Integer>();

		// run the algorithm
		for(int init : getAllInitial())
			toReturn.addAll(DFSVisitGenerate(init, orig_ec, orig_oc, orig_ac, orig_path));

		for(int i = 0; i < toReturn.size(); i++) {
			List<StepType> steps = toReturn.get(i).getStep();
			toReturn.get(i).setStep(insertParamsAndSteps(steps));
		}


		algoDurationTime = System.currentTimeMillis() - firstTime;

		System.out.println("\n--\n--\n--Test Cases--\n--\n--\n");
		for(int i = 0; i < toReturn.size(); i++)
			System.out.println((i+1) + ":\n\n" + toReturn.get(i) + "\n--");

		// find out how many edges were not covered.

		int originalEdges = Statistics.countNumEdgesInGraph(baseGraph, false);
		int coveredEdges = Statistics.pathEdgesCovered(toReturn, baseGraph, false);

		Statistics.StatisticsSet lastStats = Statistics.collected.get(Statistics.collectedStats-1);
		EFG minimalGraph = Statistics.constructPathCoverEFG(baseGraph,
				lastStats.coveredData,
				lastStats.eventInitialStatuses);
		// record cycle statistics.
		Cyclomatic.calculateCyclomaticComplexityOf(minimalGraph, true);
		Cyclomatic.StatisticsSet cycleStats = (Cyclomatic.StatisticsSet)Statistics.collected.get(Statistics.collectedStats-1);
		int numEdges = cycleStats.follows;
		int numNodes = minimalGraph.getEventGraph().getRow().size();

		boolean cyclesDetected = cycleStats.cyclesDetected >= 1;
		String cycleStatement = cyclesDetected ? "were" : "were not";
		int lastCyclomNo = cycleStats.cyclomaticNumber;

		// print statistics
		System.out.println("Minimal EFG has " + coveredEdges + " edges.");
		int numRemoved = originalEdges - coveredEdges;
		System.out.println("A difference of " + numRemoved + " edges from TestCaseGenerate input graph.");
		System.out.println("Cycles " + cycleStatement  + " detected in minEFG, and cyclomatic number = " + lastCyclomNo + " " +
				"(" + numEdges + " - " + numNodes + " + 2)\n");
		// print the final EFG
		System.out.println("New Minimal EFG:\n" + minimalGraph);


		postEFG = minimalGraph;
		return toReturn;
	}

	public ArrayList<Integer> activeAtomics(int[] aC)
	{
		ArrayList<Integer> toReturn = new ArrayList<Integer>();
		for(int j = 0; j < atomicSets.length; j++)
			if(aC[j] != -1)
				toReturn.add(j);
		return toReturn;
	}
	/**
	 * Very small method used to generate test cases using the input graph.
	 * @param i
	 * @param oC
	 * @param eC
	 * @param path
	 * @return
	 */
	public List<TestCase> DFSVisitGenerate(int i, int[] eC, int[] oC, int[] aC, List<Integer> path)
	{
		ArrayList<TestCase> retCases = new ArrayList<TestCase>();
		ArrayList<Integer> setsActive = activeAtomics(aC);

		int[] eC_new = Arrays.copyOf(eC, eC.length);
		int[] oC_new = Arrays.copyOf(oC, oC.length);
		int[] aC_new = Arrays.copyOf(aC, aC.length);
		ArrayList<Integer> path_new = new ArrayList<Integer>(path);
		// check path exclusion
		for(int k : implicatedExcludeGroupsOfEvent(i))
			if(testAndSetExclusion(i, k, eC_new))
				return new LinkedList<TestCase>();

		// check path order
		for(int k : implicatedOrderSetsOfEvent(i))
			if(testAndSetOrder(i, k, oC_new))
				return new LinkedList<TestCase>();

		boolean badAtomic = false;
		boolean goodAtomic = false;
		for(int k = 0; k < atomicSets.length; k++)
			if(testAndSetAtomic(i, k, aC_new, setsActive))
				badAtomic = true;
			else
				goodAtomic = true;
		if(badAtomic && !goodAtomic) // only return if we've broken all the rules.
			return new LinkedList<TestCase>();

		// check path repeat
		if(testHyperRepeatFailure(i, path, aC))
			return new LinkedList<TestCase>();
		if(testPathHasEnoughOfRepeatableWidgets(path_new))
		// recursive case:
			path_new.add(i);

		if(checkRetainRequirements(path_new, aC_new))
			retCases.add(generateSteps(path_new));
		// search downstream for more paths, and
		// collect the ones found downstream in a container.
		for(int j : edgesOutgoingFrom(i)) {
			List<TestCase> downstream = DFSVisitGenerate(j, eC_new, oC_new, aC_new, path_new);
			retCases.addAll(downstream);
		}
		// return the test cases found in a container to the caller.
		return retCases;
	}

	/**
	 * Check potential test case
	 * for meeting the important requirements for saving a test case.
	 * Including if the path:<br>
	 * 	ends in a main window,<br>
	 * 	contains all elements required by the required constraints,<br>
	 * 	does not simply open and close a window.
	 * 	completes all relevant atomic sets.
	 *
	 * @param finalPath
	 * @return
	 */
	protected boolean checkRetainRequirements(List<Integer> finalPath, int[] aC)
	{
		 // NOTE: testAndSetAtomicComplete has to come first!
		return testAndSetAtomicComplete(finalPath, aC)
				&& testPathEndsInMainWindow(finalPath)
				&& testPathMeetsRequired(finalPath)
				&& testPathMeetsWOCCH(finalPath);
	}
	/*
	 * Accessors
	 */
	/**
	 * What event does this widget's event id point us to
	 * @param w
	 * @return
	 */
	protected int findEvent(Widget w)
	{
		for(int i = 0; i < allEvents.size(); i++)
			if(w.getEventID().equals(allEvents.get(i).getEventId()))
				return i;
		return -1;
	}

	protected int findWidgetByAttributes(String eventId, String action, String window)
	{
		for(int i = 0; i < allWidgets.size(); i++) {
			if(allWidgets.get(i).getEventID().equals(eventId) && allWidgets.get(i).getWindow().equals(window))
				return i;

			String actionType = JavaActionTypeProvider.getTypeFromActionHandler(action);
			if(TaskListConformance.matchingNonNullCoreNames(allWidgets.get(i).getEventID(), eventId)
			&& actionType.equalsIgnoreCase(allWidgets.get(i).getAction())
			&& allWidgets.get(i).getWindow().equals(window))
				return i;
		}
		return -1;
	}

	protected int findWidgetByEventId(String eventId)
	{
		for(int i = 0; i < allWidgets.size(); i++)
			if(allWidgets.get(i).getEventID().equals(eventId))
				return i;
		return -1;
	}

	protected Integer[] edgesOutgoingFrom(int vertex)
	{
		ArrayList<Integer> toReturn = new ArrayList<Integer>();
		List<Integer> row = graphRows.get(vertex).getE();
		int rowSize = row.size();
		for(int i = 0; i < rowSize; i++)
			if(row.get(i) > 0)
				toReturn.add(i);
		return toReturn.toArray(new Integer[0]);
	}

	/**
	 * Utilize the information stored in the EFG provided
	 * to extract necessary information to fill a JAXB StepType construct
	 * based on the events provided in path.
	 */
	public StepType generateStep(int row)
	{
		StepType st = fact.createStepType();
		EventType rowEvent = allEvents.get(row);
		st.setEventId(rowEvent.getEventId());
		st.setReachingStep(false);
		return st;
	}

	public int widgetForEvent(EventType event)
	{
		for(int i = 0; i < allWidgets.size(); i++)
			if(event.getWidgetId().equals(allWidgets.get(i).getName()))
				return i;
		return -1;
	}

	public Step generateNextStep(int row)
	{
		Step st = fact.createStep();
		EventType rowEvent = allEvents.get(row);

		st.setEventId(rowEvent.getEventId());
		st.setReachingStep(false);
		int widgetNum = widgetForEvent(rowEvent);
		if(widgetNum == -1) {
			AttributesType signature = ComponentTypeWrapper.trimToSignatureQuality(rowEvent.getOptional());
			ComponentTypeWrapper ew = guiStructureAdapter.getComponentBySignaturePreserveTree(signature);
			if(ew == null)
				throw new RuntimeException("Cannot find event " + rowEvent.getEventId() + " in GUIStructure file");

			st.setWindowId(ew.getWindow().getTitle());
			st.setAction(rowEvent.getAction());
			st.setParameter("");
		}
		else {
			Widget rowWidget = allWidgets.get(widgetNum);
			st.setParameter(rowWidget.getParameter());
			st.setAction(rowWidget.getAction());
			st.setWindowId(rowWidget.getWindow());
		}
		return st;
	}


	public TestCase generateSteps(List<Integer> path)
	{
		TestCase newTestCase = fact.createTestCase();

		for(int i : path)
			newTestCase.getStep().add(generateStep(i));
		return newTestCase;
	}

	/// TEST AND SET METHODS FOR THE STACK VARIABLES USED IN DFS GENERATE.


	/**
	 * Does vertex i break exclusion rule k?
	 * @param i
	 * @param eC
	 * @return
	 */
	private boolean testAndSetExclusion(int i, int k, int[] eC)
	{
		if(eC[k] != -1 && eC[k] != i)
			return true;
		eC[k] = i;
		return false;
	}


	/**
	 * Does vertex i break order rule k?
	 * @param i
	 * @param k
	 * @param oC
	 * @return
	 */
	private boolean testAndSetOrder(int i, int k, int[] oC)
	{
		int groupOfI = implicatedOrderGroupInSet(i, k);
//		if(oC[k] != groupOfI && oC[k] != -1 && oC[k] > groupOfI)
		if(oC[k] != groupOfI && oC[k] != -1 && groupOfI < oC[k])
			return true;
		oC[k] = groupOfI;

		return false;
	}

	/**
	 * Does vertex i break atomic rule k? If not update the progress of the atomic
	 * sequence in aC to reflect the new state.
	 * @return
	 */
	private boolean testAndSetAtomic(int i, int k, int[] aC, ArrayList<Integer> setsActive)
	{
		if(aC[atomicSets.length+k] < 0)
			return true; // checked atomicity state, path has already failed.
		Integer[] groupsOfI = implicatedAtomicGroupsOfEvent(i);


		// if this event is not an atomic event and we're not in an atomic sequence at this time.
		if(groupsOfI.length == 0 && aC[k] == -1)
			return setsActive.isEmpty() == false; // we failed if there are groups active we're blocking.

		// If event doesn't belong to this set, and the set is not active, we're in the wrong sequence.
		ArrayList<Integer> iGroupsInSet = implicatedAtomicGroupsInSet(i, k);
		if(iGroupsInSet.isEmpty() && aC[k] == -1)
			return true; // i is an active widget outside of its atomic group.

		int nextA = aC[k]+1; // nextA is the next group in this atomic sequence.
		if(iGroupsInSet.contains(atomicSets[k][nextA])) {
			if(!setsActive.isEmpty() && !setsActive.contains(k)) // if the sets currently active don't contain this set.
				return true; // we've messed up an atomic somewhere else,
							 // and cannot pronounce this atomic good. and cannot update the progress either.
			aC[k]++; // we succeeded.
			return false;
		}
		// this widget is not fit for the atomic group required at this time.
		aC[k] = -1;
		if(aC[atomicSets.length+k] == 0)
			aC[atomicSets.length+k] = -1; // set is dead.
		else if(aC[atomicSets.length+k] == 1)
			aC[atomicSets.length+k] = -2; // set is completed and dead

		return true;
	}
	/**
	 * Test if adding this vertex to the path will cause the
	 * vertex to be repeated too much in the path provided.
	 *
	 * Approach: False until proven true.
	 */
	private boolean testHyperRepeatFailure(int vertex, List<Integer> path, int[] aC)
	{
//	    if(path.size() > HIGH_PATH_LENGTH) {
//	    	if(highPathPromptShown) {
//		    	try(Scanner sc = new Scanner(System.in))
//				{
//				    System.out.println("Redundant behavior has been detected "
//				  		+ "in the current generation: Over " + HIGH_PATH_LENGTH + " elements in path.");
//				    System.out.print("Continue with generation? y/n > ");
//				    String response = sc.nextLine();
//				    while(!response.equalsIgnoreCase("y") && !response.equalsIgnoreCase("n"))
//					    response = sc.nextLine();
//				    highPathPromptShown = true;
//				    if(response.equalsIgnoreCase("n"))
//					    continueGeneration = false;
//				}
//		    	catch(NoSuchElementException e)
//		    	{
//		    	}
//	    	}
//	    	if(!continueGeneration)
//	    		return true;
//		}



		if(!path.contains(vertex))
			return false; // widget has not been seen yet
		// this vertex has been seen before and is not in the repeat set.
		// it might be atomic and not bound to a repeat rule.

		// this if clause is mainly to disqualify paths that go back through
		// atomic sets for vertices that are not repeatable.
		if(atomicRepeatEvents.contains(vertex) && !repeatable[vertex]) {
			for(int k : implicatedAtomicSetsOfEvent(vertex)) {
				if(aC[atomicSets.length + k] == 1)
					return true; // one of this events atomics is completed.
								 // It was used at least once, and it can't be used again.
				else if(aC[atomicSets.length + k] == -2)
					return true; // one of this events atomics is completed. It was used at least once.
			}
			return false; // none of this event's atomics has been used,
						  // it is allowed to repeat before finishing its atomic.
		}
		// this vertex may be bound to a repeat rule.
		if(repeatable[vertex]) {
			// count up the number of times I've seen this vertex
			int count = 0;
			for(int p : path)
				if(vertex == p)
					count++;

			// if this count exceeds one of my maximum repeat values stored
			// in the repeat strides array, return true, as we have failed.
			if(repeats[vertex].length == 0) { // if there is no repeat entry
				if(count > 1) // if the vertex appears more than once.
					return true; // return true as we have failed.
			}
			else {
				for(int i = 1; i < repeats[vertex].length; i+=2) {
					if(count > repeats[vertex][i]) // if the vertex appears greater than max
						return true; // return true as we have failed.
				}
			}

			// otherwise, we're good for now.
			return false;
		}

		// if not return true, this widget is not allowed to repeat according to NoRepeat global rule,
		return true;
	}


	/**
	 * IMPORTED from RandomSequenceLengthCoverage. Test to see if path ends in a main window.
	 * Returns true if path leads to an event that lies in the main window. Returns false
	 * if path leads to a state where (a) the main window is not present, or (b)
	 * is present but is not the only one open.
	 */
	private boolean testPathEndsInMainWindow(List<Integer> myPath)
	{
		int lastI = myPath.get(myPath.size()-1);
		EventType last = allEvents.get(lastI);
		//Check AFTER
		String wIDofLast = last.getWidgetId();
		ComponentTypeWrapper lastComp = guiStructureAdapter.getComponentFromID(wIDofLast);
		if(lastComp == null)
			throw new RuntimeException("Invalid event was specified to the test case generator.\n"
					+ "Event " + last.getEventId() + " could not be found in the GUI model file provided");
		if(	!last.getType().equals(GUITARConstants.SYSTEM_INTERACTION) &&
			!last.getType().equals(GUITARConstants.TERMINAL)) {
				return false;
		}
		if(lastComp.getFirstValueByName(GUITARConstants.CLASS_TAG_NAME).equals(AccessibleRole.COMBO_BOX.toDisplayString()))
			return false;

		GUITypeWrapper window = lastComp.getWindow();
		//check if the root window is the only window available after the last event
		// Terminal event, current window is closed
		Set<GUITypeWrapper> windowsTopAfterFirstEvent = new HashSet<GUITypeWrapper>();
		// ------------------------
		// Check invoked windows
		List<GUITypeWrapper> windowsInvoked = lastComp.getInvokedWindows();

		if (GUITARConstants.TERMINAL.equals(last.getType())) {
			ComponentTypeWrapper invoker = window.getInvoker();
			if (invoker != null) {
				windowsTopAfterFirstEvent.add(invoker.getWindow());
			}

			// Non terminal
		} else {
			// New window opened
			if(windowsInvoked!=null){
				if (windowsInvoked.size() > 0) {
					windowsTopAfterFirstEvent.addAll(windowsInvoked);
				}
				// No window opened/closed
				else {
					windowsTopAfterFirstEvent.add(window);
				}
			}
		}


		// -----------------------------------------
		// Check if the 2nd event is reachable after the 1st one

		Set<GUITypeWrapper> windowsAvailableAfterFirstEvent = new HashSet<GUITypeWrapper>();
		// get all windows available after event 1
		for (GUITypeWrapper wnd : windowsTopAfterFirstEvent) {
			Set<GUITypeWrapper> avaiableWindows = wnd.getAvailableWindowList();
			if (avaiableWindows != null)
				windowsAvailableAfterFirstEvent.addAll(avaiableWindows);
		}

		if(windowsAvailableAfterFirstEvent.size()!=1)
			return false;
			// too many windows open


		for(GUITypeWrapper windowsAvailable : windowsAvailableAfterFirstEvent)
			if(!windowsAvailable.isRoot())
				return false;
				// some windows open are not root windows.

		return true;
	}

	/**
	 * Return true if this path contains all the required events. Return false if path pasts the test.
	 */
	private boolean testPathMeetsRequired(List<Integer> path)
	{
		foundMutual:
		for(int[] set : mutuallyRequiredSets) {
			for(int r: set)
				if(path.contains(r))
					continue foundMutual;
				// if we find an index in path associated
				// with at least a single vertex in this required set.
				// continue to the next set to check it.
			return false;
			// having found no match for the vertices in set, return false.
		}

		return true;
		// having checked all sets, and found a vertex representing each in path.
		// return true.
	}

	private boolean testAndSetAtomicComplete(List<Integer> path, int[] aC)
	{
		// if the last vertex in the path is an atomic vertex.
		// then is it the last event type in its set?
		// if so then we complete the atomic.
		// otherwise, the path does not complete the atomic sets
		if(atomicSets.length == 0)
			return true;
		if(path.isEmpty())
			return true; // if there are no vertices, then we do not have an incomplete atomic set.

		// are all progress bars empty? (have -1 or have reached end of sequence)

//		HashSet<Integer> complete = new HashSet<Integer>();
		boolean allComplete = true;
		for(int i = 0; i < atomicSets.length; i++)
			if(aC[i] >= 0 && aC[i] < atomicSets[i].length-1) {
				allComplete = false;
				break;
			}

		boolean goodAtomics = true;
		if(!allComplete) {
			HashSet<Integer> activeW = new HashSet<Integer>();
			HashSet<Integer> completedW = new HashSet<Integer>();
			// find the events left over from the other ones.
			for(int i = 0; i < atomicSets.length; i++) {
				if(aC[i] >= 0) { // for sets with actives
					if(aC[i] == atomicSets[i].length-1) // for recently completed sets.
						for(int j = 0; j < atomicSets[i].length; j++)
							// record events of completedSet.
							completedW.addAll(eventsOf(atomicGroups.get(atomicSets[i][j]).getWidget()));

					else // for sets definitely with leftovers.
						for(int j = 0; j <= aC[i]; j++)
							// record events of active set.
							activeW.addAll(eventsOf(atomicGroups.get(atomicSets[i][j]).getWidget()));
				}
			}
			// there can be no "stray" active atomic widgets
			// (widgets that don't belong to a previously completed atomic set)
			// (read up on this in the literature)
			// if there are, atomicity is not possible.
			activeW.removeAll(completedW);
			goodAtomics = activeW.isEmpty();
		}

		// reset completes to inactives
		boolean[] complete = new boolean[atomicSets.length];
		boolean someComplete = false;
		for(int i = 0; i < atomicSets.length; i++)
			if(aC[i] == atomicSets[i].length-1) {
				complete[i] = someComplete = true;
				// this atomic set has been completed.
				aC[atomicSets.length + i] = 1;
			}
		// reset the state if we completed a run.
		if(someComplete) {
			for(int i = 0; i < atomicSets.length; i++) {
				aC[i] = -1;
				if(aC[atomicSets.length + i] == -1)
					aC[atomicSets.length + i] = 0; // restore to a not dead not completed state.
				else if(aC[atomicSets.length + i] == -2)
					aC[atomicSets.length + i] = 1; // restore to a not dead completed state
			}
		}
		else
			for(int i = 0; i < atomicSets.length; i++)
				if(aC[atomicSets.length + i] < 0)
					aC[i] = -1; // this atomic set has died.
		return goodAtomics;
	}


	/**
	 * Get events in the current graph labeled as initial nodes.
	 * @return
	 */
	private Integer[] getAllInitial()
	{
		ArrayList<Integer> toReturn = new ArrayList<Integer>();
		for(int i = 0; i < allEvents.size(); i++)
			if(allEvents.get(i).isInitial())
				toReturn.add(i);
		return toReturn.toArray(new Integer[0]);
	}

	/**
	 * What order groups does widget specified by widget num appear in?
	 * @return
	 */
	private Integer[] implicatedOrderGroupsOfEvent(int vertex)
	{
		ArrayList<Integer> implicatedGroups = new ArrayList<Integer>();
		for(int i = 0; i < orderGroups.size(); i++)
			for(Widget gw : orderGroups.get(i).getWidget()) {
				int eNum = findEvent(gw);
				if(eNum == vertex) {
					implicatedGroups.add(i);
					break;
				}
			}
		return implicatedGroups.toArray(new Integer[0]);
	}

	/**
	 * Return an array containing the atomic groups that the event specified by vertex
	 * is implicated in.
	 * @param vertex
	 * @return
	 */
	private Integer[] implicatedAtomicGroupsOfEvent(int vertex)
	{
		if(atomicGroups.size() == 0) // if there are no atomic groups to search through, don't search.
			return new Integer[0];

		ArrayList<Integer> implicatedGroups = new ArrayList<Integer>();

		for(int i = 0; i < atomicGroups.size(); i++) {
			for(Widget gw : atomicGroups.get(i).getWidget()) {
				int eNum = findEvent(gw);
				if(eNum == vertex) {
					implicatedGroups.add(i);
					break;
				}
			}
		}
		return implicatedGroups.toArray(new Integer[0]);
	}

	private Integer[] implicatedOrderSetsOfEvent(int vertex)
	{
		ArrayList<Integer> implicated = new ArrayList<Integer>();
		for(int i = 0; i < orderSets.length; i++) {
			for(int j = 0; j < orderSets[i].length; j++)
				for(int g : implicatedOrderGroupsOfEvent(vertex))
					if(g == orderSets[i][j]) {
						implicated.add(i);
						break;
					}
		}
		return implicated.toArray(new Integer[0]);
	}


	/**
	 * Returns the atomic sets this vertex is associated with.
	 */
	private Integer[] implicatedAtomicSetsOfEvent(int vertex)
	{
		Integer[] vertexAtomicGroups = implicatedAtomicGroupsOfEvent(vertex);

		if(vertexAtomicGroups.length == 0 || atomicSets.length == 0)
			return new Integer[0];	// if this vertex belongs to no atomic groups.
								// or there are no atomic sets to check for membership.

		Set<Integer> implicated = new HashSet<Integer>();
		for(int i = 0; i < atomicSets.length; i++)
			for(int j = 0; j < atomicSets[i].length; j++)
				for(int g : vertexAtomicGroups) {
					if(g == atomicSets[i][j]) {
						implicated.add(i);
						break;
					}
				}

		return implicated.toArray(new Integer[0]);
	}

	/**
	 * Returns all groups pertaining to the vertex specified in the specified set.
	 * @param vertex
	 * @param set
	 * @return
	 */
	private ArrayList<Integer> implicatedAtomicGroupsInSet(int vertex, int set)
	{
		ArrayList<Integer> toReturn = new ArrayList<Integer>();
		int[] targetSet = atomicSets[set];
		ArrayList<Integer> vertexGroups = new ArrayList<Integer>(Arrays.asList(implicatedAtomicGroupsOfEvent(vertex)));
		for(int setGroup : targetSet)
			if(vertexGroups.contains(setGroup))
				toReturn.add(setGroup);
		return toReturn;
	}


	/**
	 * What order group does this vertex belong to in the specified set?
	 * @param vertex
	 * @param set
	 * @return
	 */
	private int implicatedOrderGroupInSet(int vertex, int set)
	{
		int[] targetSet = orderSets[set];
		ArrayList<Integer> vertexGroups = new ArrayList<Integer>(Arrays.asList(implicatedOrderGroupsOfEvent(vertex)));
		for(int setGroup : targetSet)
			if(vertexGroups.contains(setGroup))
				return setGroup;
		return -1;
	}

	private Integer[] implicatedExcludeGroupsOfEvent(int vertex)
	{
		ArrayList<Integer> implicated = new ArrayList<Integer>();
		for(int i = 0; i < exclusionSets.length; i++) {
			for(int j = 0; j < exclusionSets[i].length; j++)
				if(vertex == exclusionSets[i][j]) {
					implicated.add(i);
					break;
				}
		}
		return implicated.toArray(new Integer[0]);
	}

	/**
	 * Return true if the event passed is a page tab list, or a combo box event that expands some other component for
	 * accessibility to other events beneath it.
	 */
	private boolean expands(EventType event)
	{
		String wIDofLast = event.getWidgetId();
		String actionOfLast = event.getAction();
		ComponentTypeWrapper lastComp = guiStructureAdapter.getComponentFromID(wIDofLast);
		String compClass = lastComp.getFirstValueByName(GUITARConstants.CLASS_TAG_NAME);

		if(compClass != null) {
			if( compClass.equals(AccessibleRole.COMBO_BOX.toDisplayString()) &&
				actionOfLast.equals(ActionClass.ACTION.actionName))
				 return true;

			else if(compClass.equals(AccessibleRole.PAGE_TAB_LIST.toDisplayString()))
				return true;
		}
		return false;
	}
	/**
	 * Returns true if there is a direct link in the path specified from an event that opens and another that closes a window,
	 * or an indirect link consisting of only structural events between the opening and closing event.
	 * Otherwise, if no windows are opened and immediately closed, false is returned.
	 */
	private boolean testPathMeetsWOCCH(List<Integer> path)
	{
		boolean justOpened = false;
		for(int i = 0; i < path.size(); i++) {
			EventType event = allEvents.get(path.get(i));
			if(justOpened){
				if(event.getType().equals(GUITARConstants.TERMINAL))
					return false;
				else if(expands(event))
					continue; // ignore this expanding event.
				else
					justOpened = false; // otherwise, we have interrupted opening a window with a useful event.
			}
			if(event.getType().equals(GUITARConstants.RESTRICED_FOCUS) // CHECK FOR THE MISSPELLING TOO.
			|| event.getType().equals(GUITARConstants.RESTRICTED_FOCUS)
			|| event.getType().equals(GUITARConstants.UNRESTRICED_FOCUS) // CHECK FOR THE MISSPELLING TOO
			|| event.getType().equals(GUITARConstants.UNRESTRICTED_FOCUS)) {
				justOpened = true;
			}
//			ComponentTypeWrapper lastComp = guiStructureAdapter.getComponentFromID(event.getWidgetId());
//			String compClass = lastComp.getFirstValueByName(GUITARConstants.CLASS_TAG_NAME);
//			if(compClass!= null && compClass.equals(AccessibleRole.PAGE_TAB_LIST.toDisplayString()))

		}
		return true; // no window open and close happening so we're good.
	}

	/**
	 * Test if the path has "enough" of every vertex found in this path according
	 * to repeat rules. A path does not have enough of a vertex V from path,
	 * if V is specified in a repeat rule R, and V does not appear > (R's minBound) times in path.
	 * If so, return false. Otherwise, return true.
	 */
	private boolean testPathHasEnoughOfRepeatableWidgets(List<Integer> path)
	{
		for(int vertex : path) {
			if(repeatable[vertex]) {

				// count up the number of times I've seen this vertex
				int count = 0;
				for(int p : path)
					if(vertex == p)
						count++;

				// if this count is at least one of my minimum repeat values stored
				// in the repeat strides array, return true.
				for(int i = 0; i < repeats.length; i+=2)
					if(count >= repeats[vertex][i])
						return true;

				// else return false.
				return false;
			}
		}
		return true; // the path has enough copies of all repeatable widgets in path.
	}
}
