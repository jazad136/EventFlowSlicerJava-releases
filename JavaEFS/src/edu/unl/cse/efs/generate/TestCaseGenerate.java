package edu.unl.cse.efs.generate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import javax.accessibility.AccessibleRole;

import edu.umd.cs.guitar.awb.JavaActionTypeProvider;
import edu.umd.cs.guitar.event.ActionClass;
import edu.umd.cs.guitar.model.GUITARConstants;
import edu.umd.cs.guitar.model.XMLHandler;
import edu.umd.cs.guitar.model.data.*;
import edu.umd.cs.guitar.model.wrapper.*;
import edu.unl.cse.efs.bkmktools.TSTBookmarking.TSTUnBookmarking;
import edu.unl.cse.efs.tools.AlphabetIterator;
import edu.unl.cse.efs.tools.ArrayTools;
import edu.unl.cse.efs.tools.LocationComparator;
import edu.unl.cse.efs.tools.TaskListConformance;
import edu.unl.cse.guitarext.JavaTestInteractions;

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
	private Splicer splicer;
	public TaskList constraintsRef;
	private String outputDirectory;
	private static ObjectFactory fact = new ObjectFactory();
	private List<RowType> graphRows;
	private List<EventType> allEvents;
	private List<Widget> allWidgets;
	public long firstTime;
	public long algoDurationTime, ioHandlingTime;
	public int[][] similar;
	/** an array describing what widgets are repeatable */
	public boolean[] repeatable;
	/** an array describing what widgets force test cases to stop */
	public boolean[] stoppable;
	/** an integer describing the number of stops in this instance */
	public int numStops;
	/** an integer array with strides describing how long certain widgets may repeat*/
	public int[][] repeats;
	/** an array describing the intersection between atomic groups and repeat groups*/
	public Set<Integer> atomicRepeatEvents;
	/** An array that describes what widgets belong to mutually required groups */
	public int[][] mutuallyRequiredSets;
	/** An array describing what widgets belong to exclusion groups */
	public int[][] exclusionSets;
	/** An array describing what widgets share the same identification, but potentially different parameters */
	public int[][] mutualSets;
	/** An array describing what groups belong to order sets */
	public int[][] orderSets;
	/** An array describing what groups belong to atomic sets */
	public int[][] atomicSets;
	public static final int HIGH_PATH_LENGTH= 100;
	/** Barely used */
	private boolean continueGeneration, highPathPromptShown;

	private LocationComparator xDirectionComparator;
	private List<OrderGroup> orderGroups;
	private List<AtomicGroup> atomicGroups;
	private List<Required> graphRequireds;
	private List<Stop> graphStops;


	/**
	 * Preconditions: 	inputFlowGraph and guiData and outputDirectory are non-null.
	 *
	 * Postconditions: 	The output directory is set.
	 * 					The base graph is initialized to point to objects referenced by pacified graph.
	 * 					Constraints event arrays, event subarrays, and widgets lists are initialized.
	 * 					Edges can now be retrieved by outgoingEdgesFrom, and constraints can be checked by the test methods.
	 * 					Statistical timing variables are reset to 0-values.
	 */
	public TestCaseGenerate(EFG inputFlowGraph, GUIStructure guiData, String outputDirectory)
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
		graphRequireds = new LinkedList<Required>();
		graphStops = new LinkedList<Stop>();
	}

	public String getOutputDirectory()
	{
		return outputDirectory;
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


	public int runSliceAlgorithmAndWriteResultsToOutputDirectory(boolean unbookmarkOutput) throws IOException
	{
		List<TestCase> output = runSliceAlgorithm();
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


	public void setupIndexSets(TaskList constraints, Collection<SelectorPack> selectors)
	{
		if(selectors.isEmpty() || selectors == null)
			setupIndexSets(constraints);
		else {
			LinkedList<SelectorPack> rest = new LinkedList<SelectorPack>(selectors);
			SelectorPack first = rest.pop();
			setupIndexSets(constraints, first, rest.toArray(new SelectorPack[0]));
		}
	}

	public Order setupSearchOrder(Widget wOpen, Widget wSearch, Widget wRun, Widget wList, Widget wClose)
	{
		Order o = fact.createOrder();

		OrderGroup og1 = fact.createOrderGroup();
		og1.getWidget().add(wOpen);
		o.getOrderGroup().add(og1);

		OrderGroup og2 = fact.createOrderGroup();
		og2.getWidget().add(wSearch);
		o.getOrderGroup().add(og2);
		// added
		OrderGroup og3 = fact.createOrderGroup();
		og3.getWidget().add(wRun);
		o.getOrderGroup().add(og3);
		// added
		OrderGroup og4 = fact.createOrderGroup();
		og4.getWidget().add(wList);
		o.getOrderGroup().add(og4);

		OrderGroup og5 = fact.createOrderGroup();
		og5.getWidget().add(wClose);
		o.getOrderGroup().add(og5);

		return o;
	}

	public LinkedList<LinkedList<Integer>> paths(int from, int to)
	{
		LinkedList<LinkedList<Integer>> paths = new LinkedList<LinkedList<Integer>>();
		LinkedList<Integer> path = new LinkedList<Integer>();
		LinkedList<LinkedList<Integer>> elements = new LinkedList<LinkedList<Integer>>();
		int COLOR_WHITE = 0;
		int COLOR_GRAY = 1;
		int COLOR_BLACK = 2;
		int[] color = new int[allEvents.size()];

		for(int i = 0; i < allEvents.size(); i++) {
			color[i] = COLOR_WHITE;
		}


		elements.push(relevantEdgeListOutgoing(from, to, color));

		path.add(from);
		LinkedList<Integer> js;
		LinkedList<Integer> removed = new LinkedList<Integer>();
		int i = from;
		int j = -1;
		while(!elements.isEmpty()) {
			js = elements.peek();
			if(js.isEmpty()) {
				elements.pop();
				i = path.removeLast();
				removed.add(i);
				color[i] = COLOR_BLACK;
				continue;
			}
			if(!removed.isEmpty()) {
				for(int r : removed)
					color[r] = COLOR_WHITE;
				removed.clear();
				if(!path.isEmpty())
					i = path.peekLast();
				else
					i = from;
			}
			j = js.pop();
			path.add(j);
			if(j == to) {
				boolean violateOrder = false;
				for(int k : implicatedOrderSetsOfEvent(j))
					if(testOrder(i, j, k)) {
						violateOrder = true;
						break;
					}
				if(!violateOrder) {
					paths.add(new LinkedList<>(path));
					path.removeLast();
				}
			}
			else if(color[j] == COLOR_WHITE) {
				color[j] = COLOR_GRAY;
				elements.push(relevantEdgeListOutgoing(j, to, color));
			}
			else if(color[j] == COLOR_GRAY) {
				color[j] = COLOR_BLACK;
				path.removeLast();
//				elements.push(relevantEdgeListOutgoing(j, to, color));
			}
			i = j;
		}

		return paths;
	}


	public List<TestCase> SliceVisit(int i, List<Integer> path)
	{
		List<TestCase> retCases = new LinkedList<TestCase>();
		ArrayList<Integer> path_new = new ArrayList<Integer>(path);
		path_new.add(i);
		if(testPathMeetsRequired(path_new)) {
			TestCase out = generateSliceSteps(path_new);
			retCases.add(out);
		}
//		if(testPathMustStop(path_new))
//			return retCases;
//
//		for(int j : splicer.edges.get(i)) {
		ArrayList<Integer> outgoing = splicer.edges.get(i);
		if(testPathMustStop(path_new)) {
			LinkedList<Integer> unmet = requiredsUnmet(path_new);
			if(unmet.size() > 0) {
				// we have not met all requires rules.
				LinkedList<Integer> newOuts = new LinkedList<Integer>(outgoing);
				newOuts.retainAll(unmet);
				if(!newOuts.isEmpty())
					// we will focus only on those that are required.
					outgoing = new ArrayList<>(newOuts);
			}
			else
				return retCases; // we've met all requires rules, and should stop.
		}

		// search downstream for more paths, and
		// collect the ones found downstream in a container.
		for(int j : outgoing) {
			retCases.addAll(SliceVisit(j, path_new));
		}
		return retCases;
	}
	public void setupSliceSets(TaskList constraints, SelectorPack... packs)
	{
		graphRequireds.clear();
		graphStops.clear();
		ArrayList<SelectorPack> packsList = new ArrayList<SelectorPack>(Arrays.asList(packs));
		allWidgets = constraints.getWidget();
		int dPackI = nextDirectionalPack(packsList, -1);
		splicer = new Splicer(allEvents);
		boolean hasSearch = false;
		SearchPack sp = null;
		int sPackI = nextSearchPack(packsList, -1);
		EventType openEvent = null;
		EventType closeEvent = null;
		List<Slice> searchSlices = new ArrayList<Slice>();

		int fPackI = nextFocusPack(packsList, -1);
		if(fPackI != -2) {
			FocusOnPack fp = (FocusOnPack)packsList.get(fPackI);
			List<Stop> focusStops = stopsByFocus(fp.list);
			graphStops.addAll(focusStops);
		}
		if(sPackI != -1) {
			hasSearch = true;
			sp = (SearchPack)packsList.get(sPackI);
			Widget wO = allWidgets.get(sp.helpOpen);
			Widget wS = sp.getSearchPerformer(0);
			Widget wR = allWidgets.get(sp.helpRun);
			Widget wL = allWidgets.get(sp.helpList);
			Widget wC = allWidgets.get(sp.helpClose);
			Order searchOrder = setupSearchOrder(wO, wS, wR, wL, wC);
			isolatedOrderIndexSet(searchOrder);
			LinkedList<LinkedList<EventType>> ePaths = eventPaths(wO, wS);

			EventType listEvent = allEvents.get(findEvent(wL));
			EventType runEvent = allEvents.get(findEvent(wR));
			openEvent = allEvents.get(findEvent(wO));
			closeEvent = allEvents.get(findEvent(wC));
			if(!ePaths.isEmpty()) {
				LinkedList<EventType> list = ePaths.get(0);
				list.remove(0); // remove open button.
				list.add(runEvent);
				list.add(listEvent);
				for(int i = 0; i < sp.searchPerformers.size(); i++) {
					Widget subj = sp.forSearch.searchSubjects.get(i);
//					Widget subj = sp.list.get(sp.searchSubjects.get(i));
					EventType subject = allEvents.get(findEvent(subj));
//					Widget perf = sp.list.get(sp.searchPerformers.get(i));
					Widget perf = sp.forSearch.searchExamples.get(i);
					EventType performer = allEvents.get(findEvent(perf));
					list.set(list.size()-3, performer); // modify the slice
					Slice newS = fact.createSlice(list); // add the slice.
					newS.setMain(subject); // the important widget to sort by is the subject.
					searchSlices.add(newS);
				}
			}
			// ensure that every test case closes the help window.
			Required r = fact.createRequired();
			r.getWidget().add(wC);
			graphRequireds.add(r);
		}
		List<DirectionalPack> dps = new LinkedList<DirectionalPack>();
		if(dPackI != -1) {
			do {
				DirectionalPack dp = (DirectionalPack)packsList.get(dPackI);
				dps.add(dp);
				// if hovers have to come first, then all are required in every test case.
				if(dp.allHoversFirst) {
					for(Widget w : dp.hovers) {
						Required r = fact.createRequired();
						r.getWidget().add(w);
						graphRequireds.add(r);
					}
					if(hasSearch) {
						for(Widget w : sp.getSearchPerformers()) {
							Required r = fact.createRequired();
							r.getWidget().add(w);
							graphRequireds.add(r);
						}
					}
				}
				splicer.add(directionSlices(dp.hovers, dp.fromRight));
				if(hasSearch) {
					splicer.add(searchSlices);
				}
				List<Widget> importantOther = SelectorPack.filterClicksFromHovers(dp.other, dp.hovers);
				splicer.add(directionSlices(importantOther, dp.fromRight));
				dPackI = nextDirectionalPack(packsList, dPackI);
			} while(dPackI != -1);


		}
		if(hasSearch)
			splicer.abbySpliceConstruction(dps, openEvent, closeEvent);
		else
			splicer.timSpliceConstruction(dps);
	}
	public boolean hasMoreSlices()
	{
		return splicer.hasMoreSlices();
	}
	public boolean advanceSplicer()
	{
		splicer.advance();
		return splicer.hasMoreSlices();
	}
	public void setupSliceIndexSets(TaskList constraints)
	{
		boolean doSetup = true;
		if(constraints == null)
			doSetup = false;
		constraintsRef = fact.createTaskList();
		if(doSetup) {
			constraintsRef.getRequired().addAll(constraints.getRequired());
			constraintsRef.getExclusion().addAll(constraints.getExclusion());
			constraintsRef.getRepeat().addAll(constraints.getRepeat());
			constraintsRef.getOrder().addAll(constraints.getOrder());
			constraintsRef.getAtomic().addAll(constraints.getAtomic());
			constraintsRef.getStop().addAll(constraints.getStop());
		}


		// required. Just assign widgets to sets.
		if(doSetup && constraints.getRequired() != null) {
			for(Required r : constraints.getRequired())
				if(!graphRequireds.contains(r))
					graphRequireds.add(r);

		}

		if(!graphRequireds.isEmpty()) {
			mutuallyRequiredSets = new int[graphRequireds.size()][];
			for(int i = 0; i < graphRequireds.size(); i++) {
				List<Widget> oneSet = graphRequireds.get(i).getWidget();
				int[] reqSet = new int[oneSet.size()];
				for(int j = 0; j < oneSet.size(); j++) {
					reqSet[j] = splicer.findSlice(oneSet.get(j));
				}
				mutuallyRequiredSets[i] = reqSet;
			}
		}
		else
			mutuallyRequiredSets = new int[0][0];

		if(doSetup && constraints.getExclusion() != null) {
			List<Exclusion> graphExclusions = constraints.getExclusion();
			exclusionSets = new int[graphExclusions.size()][];
			for(int i = 0; i < graphExclusions.size(); i++) {
				List<Widget> oneSet = graphExclusions.get(i).getWidget();
				int[] excSet = new int[oneSet.size()];
				for(int j = 0; j < oneSet.size(); j++)
					excSet[j] = splicer.findSlice(oneSet.get(j));
				exclusionSets[i] = excSet;
			}
		}
		else
			exclusionSets = new int[0][0];

		if(doSetup && constraints.getStop() != null) {
			for(Stop r : constraints.getStop())
				if(!graphStops.contains(r))
					graphStops.add(r);
		}

		if(!graphStops.isEmpty()) {
			numStops = 0;
			stoppable = new boolean[splicer.currentN()];
			for(int i = 0; i < graphStops.size(); i++) {
				Stop enclosure = graphStops.get(i);
				List<Widget> oneStop = enclosure.getWidget();
				for(Widget w : oneStop) {
					int nextIdx = splicer.findSlice(w);
					stoppable[nextIdx] = true;
				}
			}
			for(int i = 0; i < stoppable.length; i++)
				if(stoppable[i])
					numStops++;
		}
		else {
			stoppable = new boolean[splicer.currentN()];
			numStops = 0;
		}
	}
	public List<Atomic> searchPackAtomics(SearchPack sp)
	{
		List<Atomic> graphAtomics = new ArrayList<Atomic>();
		Widget wO = allWidgets.get(sp.helpOpen);
		Widget wR = allWidgets.get(sp.helpRun);
		Widget wS = sp.getSearchPerformer(0);
		Widget wL = allWidgets.get(sp.helpList);
		Widget wC = allWidgets.get(sp.helpClose);
		Order searchOrder = setupSearchOrder(wO, wS, wR, wL, wC);
		isolatedOrderIndexSet(searchOrder);
		List<Widget> searches = new LinkedList<Widget>();
		for(String s : sp.searchTerms) {
			Widget newW = wS.copyOf(fact);
			newW.setParameter(s);
			searches.add(newW);
		}
		LinkedList<LinkedList<Widget>> wPaths = paths(wO, wS);
		// for all paths to the search field.
		for(int i = 0; i < wPaths.size(); i++) {
			// create a base atomic group.
			Atomic nextA = atomicPath(wPaths.get(i));
			// remove the last group that uses the search field.
			nextA.getAtomicGroup().remove(nextA.getAtomicGroup().size()-1);
			// fan out to all the search fields
			AtomicGroup last = fact.createAtomicGroup();
			last.getWidget().addAll(searches);
			nextA.getAtomicGroup().add(last);
			// fan in to the run button, and list button.
			last = fact.createAtomicGroup();
			last.getWidget().add(wR);
			nextA.getAtomicGroup().add(last);
			last = fact.createAtomicGroup();
			last.getWidget().add(wL);
			nextA.getAtomicGroup().add(last);
			// finish the rule.
			graphAtomics.add(nextA);
		}
		return graphAtomics;
	}
	public void setupIndexSets(TaskList constraints, SelectorPack selector, SelectorPack... others)
	{
		boolean doSetup = true;
		if(constraints == null)
			doSetup = false;

		constraintsRef = fact.createTaskList();
		if(doSetup) {
			constraintsRef.getRequired().addAll(constraints.getRequired());
			constraintsRef.getExclusion().addAll(constraints.getExclusion());
			constraintsRef.getRepeat().addAll(constraints.getRepeat());
			constraintsRef.getOrder().addAll(constraints.getOrder());
			constraintsRef.getAtomic().addAll(constraints.getAtomic());
			constraintsRef.getStop().addAll(constraints.getStop());
		}
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

		LinkedList<SelectorPack> allPacks = new LinkedList<SelectorPack>(Arrays.asList(others));
		allPacks.push(selector);
		int sPackI = nextSearchPack(allPacks, -1);
		List<Order> graphOrders = constraints.getOrder() == null ? new ArrayList<Order>() : new ArrayList<Order>(constraints.getOrder());
		List<Atomic> graphAtomics = constraints.getAtomic() == null ? new ArrayList<Atomic>() : new ArrayList<Atomic>(constraints.getAtomic());
		int dPackI = hasDirectionalPack(selector, others);
		if(dPackI != -2) {
			DirectionalPack dp = (dPackI == -1) ? (DirectionalPack)selector : (DirectionalPack)others[dPackI];
			if(dp.allHoversFirst) {
//				List<Order> hoverOrders = orderByHoverThenDirection(dp.other, dp.hovers, dp.fromRight);
//				graphOrders.addAll(hoverOrders);
//				constraintsRef.getOrder().addAll(hoverOrders);
				if(sPackI == -1) {
					List<Atomic> hoverAtomics = atomicHoversOnly(dp.hovers, dp.fromRight);
					graphAtomics.addAll(hoverAtomics);
					constraintsRef.getAtomic().addAll(graphAtomics);
				}
				List<Order> hoverOrders = orderByHoverThenDirection(dp.other, dp.hovers, dp.fromRight);
				graphOrders.addAll(hoverOrders);
				constraintsRef.getOrder().addAll(hoverOrders);
			}
			else {

				if(sPackI == -1) {
					Order directional = orderedClicksByDirectionThenHover(dp.other, dp.hovers, dp.fromRight);
					List<Atomic> dirAtomic = atomicGroupsOfTwo(directional.getOrderGroup());
					graphAtomics.addAll(dirAtomic);
					graphOrders.add(directional);
					constraintsRef.getOrder().add(directional);
					constraintsRef.getAtomic().addAll(dirAtomic);
				}
				else {
					SearchPack sp = (SearchPack)allPacks.get(sPackI);
					List<Widget> relatedClicks = TaskListConformance.normalClickVariantsFromHovers(constraints, dp.hovers);
					Order directional = orderedClicksByOneOfEach(dp.hovers, sp.getSearchPerformers(), relatedClicks, dp.fromRight);
					graphOrders.add(directional);
					constraintsRef.getOrder().add(directional);
				}

			}
		}


		if(sPackI != -1) {
			SearchPack sp = (SearchPack)allPacks.get(sPackI);
			graphAtomics.addAll(searchPackAtomics(sp));
			sPackI = nextSearchPack(allPacks, sPackI);
		}

		// order, just assign groups to sets, not widgets to groups

		orderGroups = new ArrayList<OrderGroup>();
		boolean setOrders = false;
		if(doSetup && graphOrders != null) {

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

		// atomics, just assign groups to sets, not widgets to groups

		atomicGroups = new ArrayList<AtomicGroup>();
		atomicRepeatEvents = new HashSet<Integer>();
		boolean setAtomics = false;
		if(doSetup && graphAtomics != null) {
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
			repeatable = new boolean[allEvents.size()];
			repeats = new int[0][0];
		}
		// Stop constraints. Just assign widgets to a single set
		List<Stop> graphStops = constraints.getStop();
		int fPackI = hasFocusOnPack(selector, others);
		if(fPackI != -2) {
			FocusOnPack fp = (fPackI == -1) ? (FocusOnPack)selector : (FocusOnPack)others[fPackI];
			if(graphStops == null)
				graphStops = new ArrayList<Stop>();
			List<Stop> focusStops = stopsByFocus(fp.list);
			graphStops.addAll(focusStops);
			constraintsRef.getStop().addAll(graphStops);
		}

		if(doSetup && constraints.getStop() != null) {
			stoppable = new boolean[allEvents.size()];
			for(int i = 0; i < graphStops.size(); i++) {
				Stop enclosure = graphStops.get(i);
				List<Widget> oneStop = enclosure.getWidget();
				for(Widget w : oneStop) {
					int nextIdx = findEvent(w);
					stoppable[nextIdx] = true;
				}
			}
			numStops = 0;
			for(int i = 0; i < stoppable.length; i++)
				if(stoppable[i])
					numStops++;
		}
		else
			stoppable = new boolean[allEvents.size()];

	}
	public int hasDirectionalPack(SelectorPack first, SelectorPack... others)
	{
		if(first instanceof DirectionalPack)
			return -1;
		for(int i = 0; i < others.length; i++)
			if(others[i] instanceof DirectionalPack)
				return i;
		return -2;
	}

	public int hasFocusOnPack(SelectorPack first, SelectorPack... others)
	{
		if(first instanceof FocusOnPack)
			return -1;
		for(int i = 0; i < others.length; i++)
			if(others[i] instanceof FocusOnPack)
				return i;
		return -2;
	}
	public int nextFocusPack(List<SelectorPack> list, int after)
	{
		for(int i = after+1; i < list.size(); i++)
			if(list.get(i) instanceof FocusOnPack)
				return i;
		return -1;
	}
	public int nextDirectionalPack(List<SelectorPack> list, int after)
	{
		for(int i = after+1; i < list.size(); i++)
			if(list.get(i) instanceof DirectionalPack)
				return i;
		return -1;
	}
	public int nextSearchPack(List<SelectorPack> list, int after)
	{
		for(int i = after+1; i < list.size(); i++)
			if(list.get(i) instanceof SearchPack)
				return i;
		return -1;
	}
	public Widget createParamWidgetFrom(Widget w, String addParameter)
	{
		Widget newW = fact.createWidget();
		newW.setEventID(w.getEventID());
		newW.setName(w.getName());
		newW.setParent(w.getParent());
		newW.setType(w.getType());
		newW.setWindow(w.getWindow());
		newW.setParameter(addParameter);
		return newW;
	}

	public Atomic atomicPath(List<Widget> widgets)
	{
		Atomic newA = fact.createAtomic();
		for(Widget w : widgets) {
			AtomicGroup ag = fact.createAtomicGroup();
			ag.getWidget().add(w);
			newA.getAtomicGroup().add(ag);
		}
		return newA;
	}

	public List<Slice> toSlices(LinkedList<LinkedList<EventType>> lists)
	{
		ArrayList<Slice> toReturn = new ArrayList<>();
		for(List<EventType> let : lists)
			toReturn.add(fact.createSlice(let));
		return toReturn;
	}
	public void isolatedOrderIndexSet(Order o)
	{
		orderSets = new int[1][];
		orderSets[0] = new int[0];
		orderGroups = new ArrayList<OrderGroup>();
		int groupNum = -1;
		List<OrderGroup> oneGroup = o.getOrderGroup();
		if(oneGroup.size() == 0 || oneGroup.get(0) == null
		|| oneGroup.get(0).getWidget() == null || oneGroup.get(0).getWidget().isEmpty())
			return;
		int[] groupSet = new int[oneGroup.size()];
		for(int i = 0; i < groupSet.length; i++) {
			orderGroups.add(oneGroup.get(i));
			groupSet[i] = ++groupNum;
		}
		orderSets[0] = groupSet; // stores the numbers of the related groups in this groupset.
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
		constraintsRef = fact.createTaskList();

		if(constraints == null)
			doSetup = false;
		else {
			constraintsRef.getRequired().addAll(constraints.getRequired());
			constraintsRef.getExclusion().addAll(constraints.getExclusion());
			constraintsRef.getRepeat().addAll(constraints.getRepeat());
			constraintsRef.getOrder().addAll(constraints.getOrder());
			constraintsRef.getAtomic().addAll(constraints.getAtomic());
			constraintsRef.getStop().addAll(constraints.getStop());
		}
		if(doSetup && constraints.getWidget() != null) {
			allWidgets = constraints.getWidget();
		}
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

		// Stop constraints. Just assign widgets to a single set
		List<Stop> graphStops = constraints.getStop();
		if(doSetup && constraints.getStop() != null) {
			stoppable = new boolean[allEvents.size()];
			for(int i = 0; i < graphStops.size(); i++) {
				Stop enclosure = graphStops.get(i);
				List<Widget> oneStop = enclosure.getWidget();
				for(Widget w : oneStop) {
					int nextIdx = findEvent(w);
					stoppable[nextIdx] = true;
				}
			}
			numStops = 0;
			for(int i = 0; i < stoppable.length; i++)
				if(stoppable[i])
					numStops++;
		}
		else
			stoppable = new boolean[allEvents.size()];
	}
	/**
	 * For each widget x specified in hovers, this method first
	 * 1. finds a corresponding widget in no-hovers with a matching hovering event specified.
	 * 2. adds these to a list y
	 * 3. creates an atomic group that forces x to immediately appear before at least one
	 * of the elements in y.
	 */
	public List<Atomic> atomicOrderByHover(List<Widget> noHovers, List<Widget> hovers)
	{
		ArrayList<Atomic> toReturn = new ArrayList<Atomic>();
		List<Widget> matching = new LinkedList<Widget>();
		for(Widget h : hovers) {
			int eidH = findEvent(h);
			if(eidH == -1)
				continue;
			String hWId = allEvents.get(eidH).getWidgetId();
			for(Widget n : noHovers) {
				int eidN = findEvent(n);
				if(eidH == -1)
					continue;
				String nWId = allEvents.get(eidN).getWidgetId();
				// if we have matching widget for this hover
				if(hWId.equals(nWId)) { // add it to matching.
					matching.add(n);
				}
			}
			if(!matching.isEmpty()) {
				Atomic od = fact.createAtomic();
				AtomicGroup g1 = fact.createAtomicGroup();
				g1.getWidget().add(h);
				AtomicGroup g2 = fact.createAtomicGroup();
				g2.getWidget().addAll(matching);
				od.getAtomicGroup().add(g1);
				od.getAtomicGroup().add(g2);
				toReturn.add(od);
			}
			matching.clear();
		}
		return toReturn;
	}


	public List<Slice> directionSlices(List<Widget> elements, boolean fromRight)
	{
		xDirectionComparator = new LocationComparator(guiStructureAdapter, allWidgets, allEvents);
		ArrayList<Slice> toReturn = new ArrayList<Slice>();
		Collection<LinkedList<Widget>> out = xDirectionComparator.getMappedWidgets(elements, fromRight).values();
		for(LinkedList<Widget> o : out) {
			for(Widget w : o) {
				int e = findEvent(w);
				toReturn.add(fact.createSlice(allEvents.get(e)));
			}
		}
		return toReturn;
	}

	public List<Slice> slicesToDirectionSlices(List<Slice> elements, boolean fromRight)
	{
		xDirectionComparator = new LocationComparator(guiStructureAdapter, allWidgets, allEvents);
		ArrayList<Slice> toReturn = new ArrayList<Slice>();
		List<Widget> mains = new LinkedList<Widget>();
		for(Slice e : elements) {
			EventType et = e.getMainEvent();
			String firstParam = "";
			if(et.hasAnyParameterLists())
				firstParam = et.getParameterList().get(0).getParameter().get(0);
			int wId = findWidgetByEventIdAndParam(et.getEventId(), firstParam);
			mains.add(allWidgets.get(wId));
		}
		Collection<LinkedList<Widget>> out = xDirectionComparator.getMappedWidgets(mains, fromRight).values();
		for(LinkedList<Widget> o : out) {
			for(Widget w : o) {
				int e = findEvent(w);
				toReturn.add(fact.createSlice(allEvents.get(e)));
			}
		}
		return toReturn;
	}

//	public Slice joinByMatchingHoverAndClick(List<Widget> noHovers, List<Widget> hovers, boolean fromRight)
//	{
//		xDirectionComparator = new OrderComparator(guiStructureAdapter, allWidgets, allEvents);
//		// remember to separate
//		Collection<LinkedList<Widget>> orderedHovers = xDirectionComparator.getMappedWidgets(hovers, fromRight).values();
//		Collection<LinkedList<Widget>> orderedNons = xDirectionComparator.getMappedWidgets(noHovers, fromRight).values();
//
//		LinkedList<Widget> collected = new LinkedList<Widget>();
//		int minSize;
//		Iterator<LinkedList<Widget>> hIt = orderedHovers.iterator();
//		Iterator<LinkedList<Widget>> nIt = orderedHovers.iterator();
//		Iterator<LinkedList<Widget>> maxIt;
//		if(orderedHovers.size() < orderedNons.size()) {
//			minSize = orderedHovers.size();
//			maxIt = hIt;
//		}
//		else {
//			minSize = orderedNons.size();
//			maxIt = nIt;
//		}
//		for(int i = 0; i < minSize; i++) {
//
//			collected.addAll(hIt.next());
//			collected.addAll(nIt.next());
//		}
//		while(maxIt.hasNext())
//			collected.addAll(maxIt.next());
//		return fact.createSlice(widgetEventsOf(collected));
//	}
//	public Slice sliceByHoverThenDirection(List<Widget> noHovers, List<Widget> hovers, boolean fromRight)
//	{
//		xDirectionComparator = new OrderComparator(guiStructureAdapter, allWidgets, allEvents);
//		Collection<LinkedList<Widget>> orderedHovers = xDirectionComparator.getMappedWidgets(hovers, fromRight).values();
//		Collection<LinkedList<Widget>> orderedNons = xDirectionComparator.getMappedWidgets(noHovers, fromRight).values();
//
//		LinkedList<Widget> collected = new LinkedList<Widget>();
//
//		for(LinkedList<Widget> list : orderedHovers)
//			for(Widget h : list)
//				collected.add(h);
//
//		for(LinkedList<Widget> list : orderedNons)
//			for(Widget n : list)
//				collected.add(n);
//
//		return fact.createSlice(widgetEventsOf(collected));
//	}

	public List<EventType> widgetEventsOf(List<Widget> widgets)
	{
		List<EventType> toReturn = new LinkedList<EventType>();
		for(Widget w : widgets) {
			int e = findEvent(w);
			if(e != -1)
				toReturn.add(allEvents.get(e));
		}
		return toReturn;
	}
	public List<Atomic> atomicHoversOnly(List<Widget> hovers, boolean fromRight)
	{
		xDirectionComparator = new LocationComparator(guiStructureAdapter, allWidgets, allEvents);
		Collection<LinkedList<Widget>> orderedHovers = xDirectionComparator.getMappedWidgets(hovers, fromRight).values();
		ArrayList<Atomic> toReturn = new ArrayList<>();
		Atomic a = fact.createAtomic();
		AtomicGroup g;

		for(LinkedList<Widget> list : orderedHovers) {
			for(Widget h : list) {
				g = fact.createAtomicGroup();
				g.getWidget().add(h);
				a.getAtomicGroup().add(g);
			}
		}
		toReturn.add(a);
		return toReturn;
	}
	/**
	 * For each widget x specified in hovers, this method first<br>
	 * 1. finds a corresponding widget in no-hovers with a matching hovering event specified.<br>
	 * 2. adds these to a list y<br>
	 * 3. creates an order group that forces x to appear at some point before any elements in y.
	 */
	public List<Order> orderByHoverThenDirection(List<Widget> noHovers, List<Widget> hovers, boolean fromRight)
	{
		xDirectionComparator = new LocationComparator(guiStructureAdapter, allWidgets, allEvents);
		Collection<LinkedList<Widget>> orderedHovers = xDirectionComparator.getMappedWidgets(hovers, fromRight).values();
		Collection<LinkedList<Widget>> orderedNons = xDirectionComparator.getMappedWidgets(noHovers, fromRight).values();

		ArrayList<Order> toReturn = new ArrayList<Order>();
		Order o = fact.createOrder();
		OrderGroup og;

		for(LinkedList<Widget> list : orderedHovers) {
			for(Widget h : list) {
				og = fact.createOrderGroup();
				og.getWidget().add(h);
				o.getOrderGroup().add(og);
			}
		}

		for(LinkedList<Widget> list : orderedNons) {
			for(Widget n : list) {
				og = fact.createOrderGroup();
				og.getWidget().add(n);
				o.getOrderGroup().add(og);
			}
		}
		toReturn.add(o);
		return toReturn;
	}

	public List<Stop> stopsByFocus(List<Widget> input)
	{
		LinkedList<Stop> toReturn = new LinkedList<Stop>();
		for(Widget w : input) {
			Stop newStop = fact.createStop();
			newStop.getWidget().add(w);
			toReturn.add(newStop);
		}
		return toReturn;
	}

	public static List<Atomic> atomicGroupsOfTwo(List<OrderGroup> orderedDoubles)
    {
    	List<Atomic> toReturn = new ArrayList<Atomic>();
    	for(int i = 0; i < orderedDoubles.size(); i+=2) {
    		Atomic next = fact.createAtomic();
    		AtomicGroup nextG = fact.createAtomicGroup();
        	AtomicGroup nextG2 = fact.createAtomicGroup();
        	nextG.getWidget().addAll(orderedDoubles.get(i).getWidget());
    		nextG2.getWidget().addAll(orderedDoubles.get(i+1).getWidget());
    		next.getAtomicGroup().add(nextG);
    		next.getAtomicGroup().add(nextG2);
    		toReturn.add(next);
    	}
    	return toReturn;
    }
	public Order orderedClicksByOneOfEach(List<Widget> hovers, List<Widget> searches, List<Widget> others, boolean fromRight)
	{
		xDirectionComparator = new LocationComparator(guiStructureAdapter, allWidgets, allEvents);
		// everything is already sorted in order.
		Collection<LinkedList<Widget>> hoverVals = xDirectionComparator.getMappedWidgets(hovers, fromRight).values();
		// reorder the clicks and hovers and searches by the same criteria.
		LinkedHashSet<Widget> newHovers, newSearch, newOther;
		Order bigO = fact.createOrder();
		newSearch = new LinkedHashSet<>();
		newOther = new LinkedHashSet<>();
		newHovers = new LinkedHashSet<>();
		for(LinkedList<Widget> hw : hoverVals) {
			for(Widget h : hw) {
				// get the original index this widget maps to
				int bmIdx = xDirectionComparator.backMap.get(h);
				newHovers.add(h);
				newSearch.add(searches.get(bmIdx));
				newOther.add(searches.get(bmIdx));
			}
		}
		Iterator<Widget> hIt = newHovers.iterator();
		Iterator<Widget> sIt = newSearch.iterator();
		Iterator<Widget> oIt = newSearch.iterator();
		while(hIt.hasNext()) {
			OrderGroup gh, gs, go;
			gh = fact.createOrderGroup();
			gh.getWidget().add(hIt.next());
			bigO.getOrderGroup().add(gh);

			gs = fact.createOrderGroup();
			gs.getWidget().add(sIt.next());
			bigO.getOrderGroup().add(gs);

			go = fact.createOrderGroup();
			go.getWidget().add(oIt.next());
			bigO.getOrderGroup().add(go);
		}
		return bigO;
	}
	/**
	 * A method that creates an order rule that orders widgets specified in input
	 * in the order appear on the X axis (this information is retrieved GUI file provided to the constructor
	 * of this TestCaseGenerate object.) Widgets that appear at the same x position appear in the same order
	 * groups.
	 *
	 * If fromRight is false, then widgets are ordered from left to right. If fromRight is true
	 * widgets are ordered from right to left.
	 * @param input
	 * @param fromRight
	 * @return
	 */
	// orderByDirection(input, false)
	public Order orderedClicksByDirectionThenHover(List<Widget> clicks, List<Widget> hovers, boolean fromRight)
	{
		xDirectionComparator = new LocationComparator(guiStructureAdapter, allWidgets, allEvents);
		// remember to separate
		TreeMap<Integer, LinkedList<Widget>> map= xDirectionComparator.getMappedWidgets(clicks, fromRight);
		Order bigO = fact.createOrder();
		Collection<LinkedList<Widget>> ns = map.values();
		List<Widget> matchingH = new LinkedList<Widget>();
		Iterator<Widget> hIt;
		for(LinkedList<Widget> list : ns)
			for(Widget n : list) {
				int eidN = findEvent(n);
				if(eidN == -1)
					continue;
				String nWId = allEvents.get(eidN).getWidgetId();
				hIt = hovers.iterator();
				while(hIt.hasNext()) {
					Widget h = hIt.next();
					int eidH = findEvent(h);
					if(eidH == -1)
						continue;
					String hWId = allEvents.get(eidH).getWidgetId();
					// if we have matching widget for this hover
					if(hWId.equals(nWId)) { // add it to matching.
						matchingH.add(h);
						hIt.remove(); // don't consider this hover event anymore.
					}
				}
				if(!matchingH.isEmpty()) {
					for(Widget w : matchingH) {
						OrderGroup gh = fact.createOrderGroup();
						gh.getWidget().add(w);
						bigO.getOrderGroup().add(gh);
					}
					OrderGroup gn = fact.createOrderGroup();
					gn.getWidget().add(n);
					bigO.getOrderGroup().add(gn);
					matchingH.clear();
				}
			}

		return bigO;
	}

	public LinkedList<LinkedList<EventType>> eventPaths(Widget from, Widget to)
	{
		int fEvent = findEvent(from);
		int tEvent = findEvent(to);
		LinkedList<LinkedList<EventType>> toReturn = new LinkedList<>();
		LinkedList<LinkedList<Integer>> elements = paths(fEvent, tEvent);
		for(LinkedList<Integer> li : elements) {
			LinkedList<EventType> eventPath = new LinkedList<EventType>();
			for(int i : li)
				eventPath.add(allEvents.get(i));
			toReturn.add(eventPath);
		}
		return toReturn;
	}
	public LinkedList<LinkedList<Widget>> paths(Widget from, Widget to)
	{
		int fEvent = findEvent(from);
		int tEvent = findEvent(to);

		LinkedList<LinkedList<Integer>> elements = paths(fEvent, tEvent);
		LinkedList<LinkedList<Widget>> toReturn = new LinkedList<LinkedList<Widget>>();
		for(LinkedList<Integer> li : elements) {
			LinkedList<Widget> nextPath = new LinkedList<Widget>();
			List<EventType> eventPath = new ArrayList<EventType>();
			for(int i : li)
				eventPath.add(allEvents.get(i));

			for(int i = 0; i < eventPath.size(); i++) {
				EventType et = eventPath.get(i);
				String firstParam = "";
				if(et.hasAnyParameterLists())
					firstParam = et.getParameterList().get(0).getParameter().get(0);
				int wi = findWidgetByEventIdAndParam(et.getEventId(), firstParam);
				if(wi != -1)
					nextPath.add(allWidgets.get(wi));
			}
			toReturn.add(nextPath);
		}
		return toReturn;
	}

	public int findEvent(EventType et)
	{
		for(int i = 0; i < allEvents.size(); i++)
			if(allEvents.get(i).getEventId().equals(et.getEventId()))
				if(EventType.parameterMatch(allEvents.get(i), et))
					return i;

		return -1;
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

				StepType targetStep = steps.get(i);
				String targetEId = targetStep.getEventId();
				String targetP;
				if(targetStep.hasOneParameter())
					targetP = steps.get(i).getParameter().get(0);
				else
					targetP = "";
				Widget w = allWidgets.get(findWidgetByEventIdAndParam(targetEId, targetP));
//				List<String> params = new ArrayList<String>();
//				String param  = w.getParameter();
//				if(param==null) continue; // if there is no parameter.
//				params.add(param);
//				steps.get(i).setParameter(params);
				if(targetP.isEmpty()) continue;
				String param = targetStep.getParameter().get(0);
				//insert extra steps for typing events to move the cursor
				if(w.getAction().equals("Type")) {
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
						targetStep.getParameter().set(0, param);
//						steps.get(i).setParameter(params);
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
	public List<TestCase> runSliceAlgorithm()
	{
		firstTime = System.currentTimeMillis();
		List<TestCase> toReturn;
		List<Integer> orig_path = new LinkedList<Integer>();
		toReturn = SliceVisit(0, orig_path);
		firstTime = firstTime - System.currentTimeMillis();
		algoDurationTime = System.currentTimeMillis() - firstTime;

		System.out.println("\n--\n--\n--Test Cases--\n--\n--\n");
		for(int i = 0; i < toReturn.size(); i++)
			System.out.println((i+1) + ":\n\n" + toReturn.get(i) + "\n--");

		calculateMinimalEFG(toReturn);
		return toReturn;
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
		int[] orig_wc = new int[1];
		// initialize the path maps.
		for(int i = 0; i < e; i++)
			orig_ec[i] = -1;
		for(int i = 0; i < o; i++)
			orig_oc[i] = -1;
		for(int i = 0; i < atomicSets.length; i++)
			orig_ac[i] = -1;

		List<Integer> orig_path = new LinkedList<Integer>();

		// run the algorithm
		Integer[] initialNodes = getAllInitial();
		for(int init : initialNodes)
			toReturn.addAll(DFSVisitGenerate(init, orig_ec, orig_oc, orig_ac, orig_wc, orig_path));

		for(int i = 0; i < toReturn.size(); i++) {
			List<StepType> steps = toReturn.get(i).getStep();
			toReturn.get(i).setStep(insertParamsAndSteps(steps));
		}


		algoDurationTime = System.currentTimeMillis() - firstTime;

		System.out.println("\n--\n--\n--Test Cases--\n--\n--\n");
		for(int i = 0; i < toReturn.size(); i++)
			System.out.println((i+1) + ":\n\n" + toReturn.get(i) + "\n--");

		calculateMinimalEFG(toReturn);
		return toReturn;
	}

	private void calculateMinimalEFG(List<TestCase> outputTestCases)
	{
		// find out how many edges were not covered.
		int originalEdges = Statistics.countNumEdgesInGraph(baseGraph, false);
		int coveredEdges = Statistics.pathEdgesCovered(outputTestCases, baseGraph, false);

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
	public List<TestCase> DFSVisitGenerate(int i, int[] eC, int[] oC, int[] aC, int[] wC, List<Integer> path)
	{
		ArrayList<TestCase> retCases = new ArrayList<TestCase>();
		ArrayList<Integer> setsActive = activeAtomics(aC);

		int[] eC_new = Arrays.copyOf(eC, eC.length);
		int[] oC_new = Arrays.copyOf(oC, oC.length);
		int[] aC_new = Arrays.copyOf(aC, aC.length);
		int[] wC_new = Arrays.copyOf(wC, wC.length);
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
//		// recursive case:
		path_new.add(i);

		if(checkRetainRequirements(path_new, aC_new, wC_new)) {
			retCases.add(generateSteps(path_new));
		}
		Integer[] outgoing = edgesOutgoingFrom(i);
		if(testPathMustStop(path_new)) {
			LinkedList<Integer> unmet = requiredsUnmet(path_new);
			if(unmet.size() > 0) {
				// we have not met all requires rules.
				LinkedList<Integer> newOuts = new LinkedList<Integer>(Arrays.asList(outgoing));
				newOuts.retainAll(unmet);
				if(!newOuts.isEmpty())
					// we will focus only on those that are required.
					outgoing = newOuts.toArray(new Integer[0]);
			}
			else
				return retCases; // we've met all requires rules, and should stop.
		}

		// search downstream for more paths, and
		// collect the ones found downstream in a container.
		for(int j : outgoing) {
			List<TestCase> downstream = DFSVisitGenerate(j, eC_new, oC_new, aC_new, wC_new, path_new);
			retCases.addAll(downstream);
		}
		// return the test cases found in a container to the caller.
		return retCases;
	}


	/*
	 * public List<TestCase> DFSVisitGenerate(int i, int[] eC, int[] oC, int[] aC, int[] wC, List<Integer> path)
	{
		if(i == 7) {
			int stop = 1;
		}

		ArrayList<TestCase> retCases = new ArrayList<TestCase>();
		ArrayList<Integer> setsActive = activeAtomics(aC);

		int[] eC_new = Arrays.copyOf(eC, eC.length);
		int[] oC_new = Arrays.copyOf(oC, oC.length);
		int[] aC_new = Arrays.copyOf(aC, aC.length);
		int[] wC_new = Arrays.copyOf(wC, wC.length);
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
//		// recursive case:
		path_new.add(i);

		if(checkRetainRequirements(path_new, aC_new, wC_new)) {
			retCases.add(generateSteps(path_new));
			if(testPathMustStop(path_new))
				return retCases;
		}
		// search downstream for more paths, and
		// collect the ones found downstream in a container.
		for(int j : edgesOutgoingFrom(i)) {
			List<TestCase> downstream = DFSVisitGenerate(j, eC_new, oC_new, aC_new, wC_new, path_new);
			retCases.addAll(downstream);
		}
		// return the test cases found in a container to the caller.
		return retCases;
	}
	 */

	protected boolean checkRetainRequirements(List<Integer> finalPath, int[] aC, int[] wC)
	{
		 // NOTE: testAndSetAtomicComplete has to come first!
		return testAndSetAtomicComplete(finalPath, aC)
				&& testPathEndsInMainWindow(finalPath, wC)
				&& testPathMeetsRequired(finalPath)
				&& testPathMeetsWOCCH(finalPath)
				&& testPathHasEnoughOfRepeatableWidgets(finalPath);
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
			if(w.getEventID().equals(allEvents.get(i).getEventId())) {
				if(parameterMatch(allEvents.get(i), w))
					return i;
			}
		return -1;
	}


	public static boolean parameterMatch(EventType et, Widget w)
	{
		if(w.getParameter() == null || w.getParameter().isEmpty()) {
			return et.hasAnyParameterLists() == false;
		}
		if(!et.hasAnyParameterLists())
			return false;
		String wParam = w.getParameter();
		String eParam = et.getParameterList().get(0).getParameter().get(0);
		return eParam.equals(wParam);
	}
	public static int findEvent(Widget w, List<EventType> events)
	{
		for(int i = 0; i < events.size(); i++)
			if(w.getEventID().equals(events.get(i).getEventId()))
				return i;
		return -1;
	}
	/**
	 * action, eventID, and window.
	 * @param eventId
	 * @param action
	 * @param window
	 * @return
	 */
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


	protected int findWidgetByEventIdAndParam(String eventId, String param)
	{
		if(param == null || param.isEmpty())
			return findWidgetByEventId(eventId);

		for(int i = 0; i < allWidgets.size(); i++)
			if(allWidgets.get(i).getEventID().equals(eventId)) {
				if(allWidgets.get(i).getParameter().equals(param))
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

	protected LinkedList<Integer> relevantEdgeListOutgoing(int i, int windowOf, int[] color)
	{
		LinkedList<Integer> toReturn = edgeListOutgoingFrom(i);
		String thisWid = allEvents.get(windowOf).getWidgetId();
		ComponentTypeWrapper thisComp = guiStructureAdapter.getComponentFromID(thisWid);
		GUITypeWrapper thisWin = thisComp.getWindow();
		String thisTitle = thisWin.getTitle();
		Iterator<Integer> rIt = toReturn.iterator();
		while(rIt.hasNext()) {
			int j = rIt.next();
			String wIdOfLast = allEvents.get(j).getWidgetId();
			ComponentTypeWrapper lastComp = guiStructureAdapter.getComponentFromID(wIdOfLast);
			GUITypeWrapper lastWindow = lastComp.getWindow();
			String lastTitle = lastWindow.getTitle();
			String lastType = allEvents.get(j).getType();
			// if the windows are not the same.
			if(!JavaTestInteractions.windowTitlesAreSame(thisTitle, lastTitle))
				rIt.remove();
			// if the event is a hover event
			else if(allEvents.get(j).getEventId().contains("HOVER")) {
				rIt.remove();
			}
			// if the event ID's are similar
			else if(allEvents.get(j).getEventId().equals(allEvents.get(i).getEventId()))
				rIt.remove();
			// if the event is already colored
			else if(color[j] != 0 || i == j)
				rIt.remove();
			// if the event is a terminal widget that will close the window.
			else if(lastType.equals(GUITARConstants.TERMINAL))
				rIt.remove();

			else {
				// if j comes before i in an ordering.
				for(int k : implicatedOrderSetsOfEvent(i))
					if(testOrder(i, j, k)) {
						rIt.remove();
						break;
					}
			}
		}
		return toReturn;
	}
	protected LinkedList<Integer> edgeListOutgoingFrom(int vertex)
	{
		LinkedList<Integer> toReturn = new LinkedList<Integer>();
		List<Integer> row = graphRows.get(vertex).getE();
		int element = 0;
		for(int e : row) {
			if(e > 0)
				toReturn.add(element);
			element++;
		}
		return toReturn;
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
		if(rowEvent.hasAnyParameterLists()) {
			st.setParameter(new ArrayList<String>(rowEvent.getParameterList().get(0).getParameter()));
		}
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

	public TestCase generateSliceSteps(List<Integer> slicePath)
	{
		List<Integer> realSteps = new LinkedList<Integer>();
		for(int i : slicePath) {
			Slice allEvents = splicer.storage.get(0).get(i);
			for(EventType et : allEvents)
				realSteps.add(findEvent(et));
		}
		return generateSteps(realSteps);
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
//		if(exclusionSets[k].length == 1)
//			return true;
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
		if(oC[k] != groupOfI && oC[k] != -1 && groupOfI < oC[k])
			return true;
		oC[k] = groupOfI;

		return false;
	}

	private boolean testOrder(int i, int j, int k)
	{
		int groupOfI = implicatedOrderGroupInSet(i, k);
		int groupOfJ = implicatedOrderGroupInSet(j, k);
		if(groupOfI != -1 && groupOfJ != -1 && groupOfI > groupOfJ)
			return true;
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
	 * Test if continuing to add to this path will violate a stop constraint.
	 */
	private boolean testPathMustStop(List<Integer> path)
	{
		if(path.isEmpty())
			return false;
		if(numStops == 0)
			return false;

		boolean[] stopped = new boolean[stoppable.length];
		int stopCount = 0;
		for(int v : path) {
			if(stoppable[v]) {
				if(!stopped[v]) {
					stopped[v] = true;
					stopCount++;
				}
			}
		}
		return stopCount == numStops;
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
			for(int i = 1; i < repeats[i].length; i+=2)
				if(count > repeats[vertex][i])
					return true;

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
	private boolean testPathEndsInMainWindow(List<Integer> myPath, int[] wC)
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
				wC[0] = 1;
				return false;
		}
		if(lastComp.getFirstValueByName(GUITARConstants.CLASS_TAG_NAME).equals(AccessibleRole.COMBO_BOX.toDisplayString())) {
			wC[0] = 1;
			return false;
		}

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

		if(windowsAvailableAfterFirstEvent.size()!=1) {
			wC[0] = 1;
			return false;
		}
			// too many windows open


		for(GUITypeWrapper windowsAvailable : windowsAvailableAfterFirstEvent)
			if(!windowsAvailable.isRoot()) {
				wC[0] = 1;
				return false;
			}
		wC[0] = 0;
		// some windows open are not root windows.



//		return true;
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

	private LinkedList<Integer> requiredsUnmet(List<Integer> path)
	{
		LinkedList<Integer> toReturn = new LinkedList<>();
		boolean[] unmet = new boolean[allEvents.size()];
		foundMutual:
		for(int[] set : mutuallyRequiredSets) {
			for(int r: set)
				if(path.contains(r))
					continue foundMutual;
				// if we find an index in path associated
				// with at least a single vertex in this required set.
				// continue to the next set to check it.
			for(int i = 0; i < set.length; i++)
				unmet[set[i]] = true;
			// having found no match for the vertices in set, return false.
		}

		for(int i = 0; i < unmet.length; i++)
			if(unmet[i])
				toReturn.add(i);
		return toReturn;
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
	public String toString()
	{
		return constraintsRef == null ? "(no constraints reference)" : constraintsRef.toString();
	}
}

