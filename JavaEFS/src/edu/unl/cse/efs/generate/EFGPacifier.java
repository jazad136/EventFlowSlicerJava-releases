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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import javax.accessibility.AccessibleRole;

import static edu.umd.cs.guitar.model.GUITARConstants.*;
import static javax.accessibility.AccessibleRole.*;
import edu.umd.cs.guitar.event.ActionClass;
import edu.umd.cs.guitar.graph.converter.EFG2GraphvizFixString;
import edu.umd.cs.guitar.model.XMLHandler;
import edu.umd.cs.guitar.model.data.Atomic;
import edu.umd.cs.guitar.model.data.AtomicGroup;
import edu.umd.cs.guitar.model.data.EFG;
import edu.umd.cs.guitar.model.data.EventGraphType;
import edu.umd.cs.guitar.model.data.EventType;
import edu.umd.cs.guitar.model.data.EventsType;
import edu.umd.cs.guitar.model.data.ObjectFactory;
import edu.umd.cs.guitar.model.data.OrderGroup;
import edu.umd.cs.guitar.model.data.Repeat;
import edu.umd.cs.guitar.model.data.RowType;
import edu.umd.cs.guitar.model.data.TaskList;
import edu.umd.cs.guitar.model.data.Widget;
import edu.unl.cse.efs.tools.StringTools;

/**
 * Source for the EFG Pacifier class. The EFG pacifier is a class responsible for reducing the EFG graph
 * to a test case graph.
 * @author jsaddle
 */
public class EFGPacifier 
{
	private List<EventType> allEvents;
	public EFG theGraph;
	public TaskList theList;
	private List<RowType> graphRows;
	private List<Widget> allWidgets;
	private int[] nextAdjacent;
	private EFG lastEFGCreated;
	private File lastEFGFileCreated;
	boolean logSet;
	public long firstTime;
	public long times[];
	
	public static final int RED1 = 0, RED2 = 1, RED3 = 2;
	
	/** 
	 * top level: separated exclusion groups
	 * second level: the events that belong in each.
	 */
	public int[][] mutualSets;
	
	/**
	 * Top level: separated "orders" (order sets)
	 * bottom level: indices of order groups
	 * 
	 * Note that the Widget objects of OrderGroups don't have an array representation
	 * in this class storing indicators like their indices. (It would have made for quite
	 * a complicated 3D array to have to deal with later!)
	 */
	public int[][] orderSets;
	private List<OrderGroup> orderGroups;
	public HashSet<Integer>[] L, order;
	public boolean[] dotted;
	/**
	 * List revealing the indices of all required events. 
	 */
	public int[] requiredEvents;
	
	/**
	 * List revealing the indices of required widgets that are marked "prime". 
	 */
	public int[] requiredPrimeEvents;
	
	public int[] repeatEvents;
	
	public Stack<Integer> dfsRecord;
	public ArrayList<Integer> retainList;
	
	
	
	private enum DFSType {EXP2CHILD, WOCCH, EIMAINWIN, ORDER, EXCLUSION, MEXCLUSION, EXCPRIME, REQUIRED, REQPRIME, REPEATALLCYCLE};
	private int[] colors;
	private String outputfile;
	static final int WHITE = 0, GRAY = 1, BLACK = 2;
	
	
	/**
	 * Source for the EFG Pacifier. Constructor sets up the global variables. This constructor
	 * stores special copies of the graph (the adjacency matrix and the nodes), and the constraint
	 * parameters for latter use in the reduction. 
	 * 
	 * Preconditions: outputfileName must be non-null and non-empty. <br>
	 * Postconditions: 	Copies of the graph, and the constraint list, are stored in pointers
	 * 					and 2D arrays internal to this instance of EFGPacifier. <br> 
	 * 					All lists are initialized to contain unset values (-1).
	 */
	public EFGPacifier(EFG eventFlowGraph, TaskList constraints, String outputfileBase)
	{
		
		theGraph = eventFlowGraph;
		allEvents = theGraph.getEvents().getEvent();
		graphRows = theGraph.getEventGraph().getRow();
		theList = constraints;
		allWidgets = constraints.getWidget();
		theGraph.getEvents().setEvent(allEvents);
		theGraph.getEventGraph().setRow(graphRows);
		firstTime = 0;
		times = new long[3];
		setOutputFileBase(outputfileBase);
		
		setupConstraintsSets(constraints);
	}
	private Collection<Integer> eventIndicesOf(List<Widget> widgets)
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
	public Collection<Integer> detectRepeatAtomicEvents(Atomic a)
	{
		HashSet<Integer> collected = new HashSet<Integer>();
		HashSet<Integer> repeats = new HashSet<Integer>();
		HashSet<Integer> transfer = new HashSet<Integer>();
		// for each group, see which ones match those already collected,
		// then refill the set of the ones collected with the ones found. 
		if(a == null || a.getAtomicGroup() == null) 
			return repeats;
		for(AtomicGroup ag : a.getAtomicGroup()) {
			// get the group
			transfer.addAll(eventIndicesOf(ag.getWidget()));
			// if some are in the collected set, flag them. wash out.
			collected.retainAll(transfer);
			repeats.addAll(collected);
			// those that aren't in that set, add to collected. wash in.
			collected.clear(); 
			collected.addAll(transfer);
			transfer.clear();
		}
		return repeats;
	}
	/**
	 * Initialize the containers for parameter based constriaints using the tasklist specified.
	 * It's important to recognize that repeat atomics and widgets specified in the repeat
	 * list both require self edges, so we detect whether there exist
	 * repeat widgets or repeat atomic widgets in this method, so we can deal
	 * with them later.   
	 */
	public void setupConstraintsSets(TaskList constraints)
	{
		List<Repeat> graphRepeats = constraints.getRepeat();
		List<Atomic> graphAtomics = constraints.getAtomic();
		if(graphRepeats != null || graphAtomics != null) {
			Set<Integer> someRepeats = new HashSet<Integer>();
			
			// repeat
			if(graphRepeats != null) 
				for(int i = 0; i < graphRepeats.size(); i++)
					for(int j = 0; j < graphRepeats.get(i).getWidget().size(); j++) 
						someRepeats.add(findEvent(graphRepeats.get(i).getWidget().get(j)));
			
			// repeat atomics 
			if(graphAtomics != null) 
				for(Atomic a : graphAtomics) 
					someRepeats.addAll(detectRepeatAtomicEvents(a)); // add repeat atomics
			
			// assign all repeat and repeat atomic widgets to an array.
			repeatEvents = new int[someRepeats.size()];
			int i = 0;
			for(Integer r : someRepeats) 
				repeatEvents[i++] = r;
		}
		else
			repeatEvents = new int[0];
		// repeat. Just assign the widget numbers to the array.
		
//		Repeat graphRepeats = constraints.getRepeat();
//		if(graphRepeats != null) {
//			repeatEvents = new int[graphRepeats.getWidget().size()]; 
//			for(int i = 0; i < repeatEvents.length; i++) 
//				repeatEvents[i] = findEvent(graphRepeats.getWidget().get(i));
//		}
//		else
//			repeatEvents = new int[0];
		
	}
	
	public void setOutputFileBase(String newFilename)
	{
		if(newFilename == null || newFilename.isEmpty())
			throw new RuntimeException("EFGPacifier: outputfileName provided must be non-null and non-empty");
		if(newFilename.length() > 4) {
			String relevantExt = newFilename.substring(newFilename.length()-4);
			if(relevantExt.equalsIgnoreCase(".efg"))
				newFilename = newFilename.substring(0, newFilename.length()-4);
		}
		outputfile = newFilename;
	}
	
//	/**
//	 * Returns the group that the widget specified by wNum belongs to within the order set
//	 * specified by set. 
//	 */
//	private int orderGroupInSet(int vertex, int set)
//	{
//		for(int l = 0; l < orderSets[set].length; l++) 
//			for(Widget gw : orderGroups.get(orderSets[set][l]).getWidget()) { 
//				if(vertex == findEvent(gw)) 
//					return orderSets[set][l];
//			}
//		return -1;
//	}
	
	/**
	 * Ensure that wNum doesn't precede other num in the specified ordering set.
	 * If we search through the order set specified, return false if we cannot find wNum
	 * before either finding otherNum or running out of indices to search through. 
	 * Otherwise, return true, because we found wNum first. 
	 * 
	 * Preconditions: setNum is a set index indexable into orderSets
	 * 				  wNum is a widget index indexable into allWidgets
	 * 				  otherWNum is a widget index indexable into allWidgets.
	 * @return
	 */
	private boolean orderGroupPrecedesOther(int oGroup, int otherOGroup, int set)
	{
		if(oGroup == otherOGroup)
			return false; // group membership is the same, one cannot precede the other. 
		for(int found : orderSets[set]) 
			if(found == oGroup)
				return true; // found groupNum before finding other. 
			else if(found == otherOGroup)
				return false; // found other before finding groupNum
		
		return false; // didn't find wNum or otherNum, both groupnums were invalid. 
	}
	
	/**
	 * Is the event pointed to by vertex a repeat event? 
	 */
	public boolean implicatesRepeatEvent(int vertex)
	{
		for(int i : repeatEvents) 
			if(vertex == i)
				return true;
		return false;
	}
	
	/**
	 * What Order listings does this vertex appear in?  
	 * @param vertex
	 * @return
	 */
	public Integer[] implicatedOrderSets(int vertex)
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
	
	
//	public int findEventUsingWidgetClues(Widget w, GUIStructure gui)
//	{
//		// search through parent, actionHandler, name, and type to find widget.
//	}
	/**
	 * What order groups does widget specified by widget num appear in? 
	 * @return
	 */
	public Integer[] implicatedOrderGroupsOfEvent(int vertex)
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
	
	public int implicatedOrderGroupWithinSet(int vertex, int set)
	{
		int[] targetSet = orderSets[set];
		ArrayList<Integer> vertexGroups = new ArrayList<Integer>(Arrays.asList(implicatedOrderGroupsOfEvent(vertex)));
		for(int setGroup : targetSet)
			if(vertexGroups.contains(setGroup))
				return setGroup;
		return -1;
	}
	/**
	 * What exclude groups does the vertex specified appear in?
	 */
	public Integer[] implicatedExcludeGroupsOfEvent(int vertex)
	{
		ArrayList<Integer> implicated = new ArrayList<Integer>();
		for(int i = 0; i < mutualSets.length; i++) {
			for(int j = 0; j < mutualSets[i].length; j++) 
				if(vertex == mutualSets[i][j]) {
					implicated.add(i);
					break;
				}
		}
		return implicated.toArray(new Integer[0]);
	}
	
	/**
	 * Is the event pointed to by vertex a required event? If so, returns the index of
	 * that widget object in the list provided. Otherwise returns -1. 
	 */
	public int implicatedRequiredIndex(int vertex)
	{
		for(int i = 0; i < requiredEvents.length; i++) 
			if(vertex == requiredEvents[i])
				return i;
		return -1;
	}
	
	public int implicatedRequiredPrime(int vertex)
	{
		for(int i = 0; i < requiredPrimeEvents.length; i++) {
			if(requiredPrimeEvents[i] == vertex)
				return i;
		}
		return -1;
	}
	
	
	public boolean illegalByRepeat(int row, int col, int[] path)
	{
		if(colors[col] != GRAY)
			return false;
		return false;
	}
	
	/**
	 * Search the ordering of the order specified by set to see which of the two: 
	 * g1, 
	 * or some other integer found in the list provided also belonging to the same ordering set as g1, 
	 * are found first in the ordering specified by set.
	 * Returns either g1 or an element of various other groups depending on which is found first
	 * in the ordering specified by set. 
	 * Returns -1 if neither g1 or any elements in variousOtherGroups can be found in the set specified
	 * by set.
	 */
	public int firstFoundInSet(int g1, Collection<Integer> variousOtherGroups, int set)
	{
		if(g1 == -1)
			return -1; // won't find this group id in the order sets. 

		ArrayList<Integer> competitors = new ArrayList<Integer>();
		for(int vg : variousOtherGroups) 
			for(int g : orderSets[set]) 
				if(vg == g) 
					competitors.add(g);
		
		for(int next : orderSets[set]) 
			if(next == g1)
				return g1; // found that g1 sits in order before any other competing order group.
			else if(competitors.contains(next)) 
				return next; // found a competing order group that sits in an order above g1.
		
		
		return -1; // didn't find either the group or any related order groups in question in this order group
	}
	public void grayColor(int node)
	{
		colors[node] = GRAY;
	}
	
	public void setDotted(int node)
	{
		dotted[node] = true;
	}
	
	public void blackColor(int node)
	{	
		colors[node] = BLACK;
	}
	
	public void pacifyInputGraphAndWriteResultsRev1()
	{
		File myFile1rs = new File(outputfile + "_g1rs.EFG");
		File myFile2mw = new File(outputfile + "_g2mw.EFG");
		File myFile3ec = new File(outputfile + "_g2ec.EFG");
		File myFile4wo = new File(outputfile + "_g3wo.EFG");
		File myFile5ex = new File(outputfile + "_g4ex.EFG");
		File myFile6or = new File(outputfile + "_g4or.EFG");
		try(FileOutputStream outStream1 = new FileOutputStream(myFile1rs);
			FileOutputStream outStream2 = new FileOutputStream(myFile2mw);
			FileOutputStream outStream3 = new FileOutputStream(myFile3ec);
			FileOutputStream outStream4 = new FileOutputStream(myFile4wo);
			FileOutputStream outStream5 = new FileOutputStream(myFile5ex);
			FileOutputStream outStream6 = new FileOutputStream(myFile6or);
		)
		{
			System.out.println(theGraph);
			removeEdgesIllegalBySelfLoops();
			System.out.println(theGraph);
			writeGraphTo(outStream1);
			
			DFS(DFSType.EIMAINWIN);
			System.out.println(theGraph);
			writeGraphTo(outStream2);
			
			try {
				DFS(DFSType.EXP2CHILD);
				System.out.println(theGraph);
				writeGraphTo(outStream3);
			} catch(IllegalArgumentException e) {
				System.out.println("Detected illegal EFG node: " + e.getMessage());
				System.out.println("Cannot complete EXP2CHILD reduction");
				System.out.println("Partial output:\n" + theGraph);
			}
			
			DFS(DFSType.WOCCH);
			System.out.println(theGraph);
			writeGraphTo(outStream4);
			
			if(mutualSets.length != 0) {
				DFS(DFSType.EXCLUSION);
				System.out.println(theGraph);
				writeGraphTo(outStream5);
			}
			if(orderSets.length != 0) {
				DFS(DFSType.ORDER);
				System.out.println(theGraph);
				writeGraphTo(outStream6);
			}
		} 
		catch(FileNotFoundException e) 
		{
			System.err.println("EFGPacifier: Could not write to file specified.\n"
					+ "Directory structure of filepath does not exist.");
		}
		catch(IOException e) 
		{
			System.err.println("EFGPacifier: Could not write to file.");
		}
	}
	
	
	public void pacifyInputGraphAndWriteResultsRev2()
	{
		// some old order and exclusion algorithms
		File myFile1rs = new File(outputfile + "_g1rs.EFG");
		File myFile2ec = new File(outputfile + "_g2ec.EFG");
		File myFile3wo = new File(outputfile + "_g3wo.EFG");
		File myFile4ex = new File(outputfile + "_g4ex.EFG");
		File myFile5or = new File(outputfile + "_g5or.EFG");
		try(FileOutputStream oS1 = new FileOutputStream(myFile1rs);
			FileOutputStream oS2 = new FileOutputStream(myFile2ec);
			FileOutputStream oS3 = new FileOutputStream(myFile3wo);
			FileOutputStream oS4 = new FileOutputStream(myFile4ex);
			FileOutputStream oS5 = new FileOutputStream(myFile5or);
		)
		{
			
			System.out.println("original:\n" + theGraph);
			removeEdgesIllegalBySelfLoops();
			System.out.println("" + 1 + ":\n" + theGraph);
			writeGraphTo(oS1);
			
			try {
				DFS(DFSType.EXP2CHILD);
				System.out.println("" + 2 + ":\n" + theGraph);
				writeGraphTo(oS2);
			} catch(IllegalArgumentException e) {
				System.out.println("Detected illegal EFG node: " + e.getMessage());
				System.out.println("Cannot complete EXP2CHILD reduction");
				System.out.println("Partial output:\n" + theGraph);
			}
			
			DFS(DFSType.WOCCH);
			System.out.println("" + 3 + ":\n" + theGraph);
			writeGraphTo(oS3);
			
			if(mutualSets.length != 0) {
				DFS(DFSType.EXCLUSION);
				System.out.println("" + 4 + ":\n" + theGraph);
				writeGraphTo(oS4);
			}
			
			if(orderSets.length != 0) {
				DFS(DFSType.ORDER);
				System.out.println("" + 5 + ":\n" + theGraph);
				writeGraphTo(oS5);
			}
		} 
		
		catch(FileNotFoundException e) 
		{
			System.err.println("EFGPacifier: Could not write to file specified.\n"
					+ "Directory structure of filepath does not exist.");
		}
		catch(IOException e) 
		{
			System.err.println("EFGPacifier: Could not write to file.");
		}
		
	}
	
	
	public EFG pacifyInputGraphAndWriteResultsRev3()
	{
		// newest: with myra's solution for exclusion
		File myFileOrig = new File(outputfile + ".EFG");
		File myFile1rs = new File(outputfile + "_g1rs.EFG");
		File myFile2ec = new File(outputfile + "_g2ec.EFG");
		File myFile3wo = new File(outputfile + "_g3wo.EFG");
		
		try(
			FileOutputStream oS0 = new FileOutputStream(myFileOrig);
			FileOutputStream oS1 = new FileOutputStream(myFile1rs);
			FileOutputStream oS2 = new FileOutputStream(myFile2ec);
			FileOutputStream oS3 = new FileOutputStream(myFile3wo);
		)
		{
			// original:
			Statistics.countNumEdgesInGraph(theGraph, true);
			Cyclomatic.calculateCyclomaticComplexityOf(theGraph, true);
			Cyclomatic.StatisticsSet lastStats = (Cyclomatic.StatisticsSet)Statistics.collected.get(Statistics.collectedStats-1);
			int lastAll = lastStats.follows;
			int lastNo = lastStats.noEdge;
			boolean cyclesDetected = lastStats.cyclesDetected >= 1;
		
			int lastCyclomNo = lastStats.cyclomaticNumber;
			
			// print stats. 
			System.out.println("" + lastAll + " connections there, " + lastNo + " not there.");
			System.out.println("\tCycles detected: " + cyclesDetected + ", Cyclomatic Number: " + lastCyclomNo);
			System.out.println("original:\n" + theGraph);
			
			
			lastEFGCreated = copyOf(theGraph);
			lastEFGFileCreated = myFileOrig;
			writeGraphTo(oS0);
			
			// take the time the simulation starts.
			firstTime = System.currentTimeMillis();
			
			// self loops
			removeEdgesIllegalBySelfLoops();
			writeGraphTo(oS1);
			// take time. 
			times[RED1] = System.currentTimeMillis() - firstTime;
			Statistics.countNumEdgesInGraph(theGraph, false);
			Statistics.countNumEdgesRemoved(lastEFGCreated, theGraph, true);
			Cyclomatic.calculateCyclomaticComplexityOf(theGraph, true);
			
			lastStats = (Cyclomatic.StatisticsSet)Statistics.collected.get(Statistics.collectedStats-1);
			lastAll = lastStats.follows;
			lastNo = lastStats.noEdge;
			int lastRemoved = lastStats.removed;
			cyclesDetected = lastStats.cyclesDetected >= 1;
			lastCyclomNo = lastStats.cyclomaticNumber;
			System.out.println("" + lastAll + " connections there, " + lastNo + " not there.");
			System.out.println("" + lastRemoved + " connections removed.");
			System.out.println("\tCycles detected: " + cyclesDetected + ", Cyclomatic Number: " + lastCyclomNo);
			System.out.println("1-RS:\n" + theGraph);
			
			lastEFGCreated = copyOf(theGraph);
			lastEFGFileCreated = myFile1rs;
			
			// expand to child
			try {
				// take time.
				times[RED2] = System.currentTimeMillis();
				DFS(DFSType.EXP2CHILD);
				writeGraphTo(oS2);
				// take time.
				times[RED2] = System.currentTimeMillis() - times[RED2];
				
				Statistics.countNumEdgesInGraph(theGraph, false);
				Statistics.countNumEdgesRemoved(lastEFGCreated, theGraph, true);
				Cyclomatic.calculateCyclomaticComplexityOf(theGraph, true);
				lastStats = (Cyclomatic.StatisticsSet)Statistics.collected.get(Statistics.collectedStats-1);
				lastAll = lastStats.follows;
				lastNo = lastStats.noEdge;
				lastRemoved = lastStats.removed;
				cyclesDetected = lastStats.cyclesDetected >= 1;
				lastCyclomNo = lastStats.cyclomaticNumber;
				System.out.println("" + lastAll + " connections there, " + lastNo + " not there.");
				System.out.println("" + lastRemoved + " connections removed.");
				System.out.println("\tCycles detected: " + cyclesDetected + ", Cyclomatic Number: " + lastCyclomNo);
				System.out.println("2-EC:\n" + theGraph);
		
				lastEFGCreated = copyOf(theGraph);
				lastEFGFileCreated = myFile2ec;
				
			} catch(IllegalArgumentException e) {
				System.out.println("Detected illegal EFG node: " + e.getMessage());
				System.out.println("Cannot complete EXP2CHILD reduction");
				System.out.println("Partial output:\n" + theGraph);
			}
			
			// take time
			times[RED3] = System.currentTimeMillis();
			DFS(DFSType.WOCCH);
			writeGraphTo(oS3);
			// take time.
			times[RED3] = System.currentTimeMillis() - times[RED3]; 
			// get statistics.
			Statistics.countNumEdgesInGraph(theGraph, false);
			Statistics.countNumEdgesRemoved(lastEFGCreated, theGraph, true);
			Cyclomatic.calculateCyclomaticComplexityOf(theGraph, true);
			lastStats = (Cyclomatic.StatisticsSet)Statistics.collected.get(Statistics.collectedStats-1);
			lastAll = lastStats.follows;
			lastNo = lastStats.noEdge;
			lastRemoved = lastStats.removed;
			cyclesDetected = lastStats.cyclesDetected >= 1;
			lastCyclomNo = lastStats.cyclomaticNumber;
			
			System.out.println("" + lastAll + " connections there, " + lastNo + " not there.");
			System.out.println("" + lastRemoved + " connections removed.");
			System.out.println("\tCycles detected: " + cyclesDetected + ", Cyclomatic Number: " + lastCyclomNo);
			System.out.println("3-WO:\n" + theGraph);
			
			
			lastEFGCreated = copyOf(theGraph);
			lastEFGFileCreated = myFile3wo;
		}
			
		catch(FileNotFoundException e) 
		{
			System.err.println("EFGPacifier: Could not write to file specified.\n"
					+ "Directory structure of filepath does not exist.");
		}
		catch(IOException e) 
		{
			System.err.println("EFGPacifier: Could not write to file.");
		}
		return lastEFGCreated;
	}
	public EFG pacifyInputGraphAndWriteResultsRev6()
	{
		File myFileOrig = new File(outputfile + ".EFG");
		File myFile1wo = new File(outputfile + "_g1wo.EFG");
		File myFile2rs = new File(outputfile + "_g2rs.EFG");
		File myFile3ec = new File(outputfile + "_g3ec.EFG");
		
		try(
			FileOutputStream oS0 = new FileOutputStream(myFileOrig);
			FileOutputStream oS1 = new FileOutputStream(myFile1wo);
			FileOutputStream oS2 = new FileOutputStream(myFile2rs);
			FileOutputStream oS3 = new FileOutputStream(myFile3ec);
		) {
			// original:
			Statistics.countNumEdgesInGraph(theGraph, true);
			Cyclomatic.calculateCyclomaticComplexityOf(theGraph, true);
			Cyclomatic.StatisticsSet lastStats = (Cyclomatic.StatisticsSet)Statistics.collected.get(Statistics.collectedStats-1);
			int lastAll = lastStats.follows;
			int lastNo = lastStats.noEdge;
			boolean cyclesDetected = lastStats.cyclesDetected >= 1;
			int lastCyclomNo = lastStats.cyclomaticNumber;
			int lastRemoved;
			// print stats. 
			System.out.println("" + lastAll + " connections there, " + lastNo + " not there.");
			System.out.println("\tCycles detected: " + cyclesDetected + ", Cyclomatic Number: " + lastCyclomNo);
			System.out.println("original:\n" + theGraph);
			
			
			lastEFGCreated = copyOf(theGraph);
			lastEFGFileCreated = myFileOrig;
			writeGraphTo(oS0);
			// take the time the simulation starts.
			firstTime = System.currentTimeMillis();
			// step 1
			// step 1
			// window open close
			{
				DFS(DFSType.WOCCH);
				writeGraphTo(oS2);
				// take time
				times[RED1] = System.currentTimeMillis() - firstTime;
				
				// get stats
				Statistics.countNumEdgesInGraph(theGraph, false);
				Statistics.countNumEdgesRemoved(lastEFGCreated, theGraph, true);
				Cyclomatic.calculateCyclomaticComplexityOf(theGraph, true);
				lastStats = (Cyclomatic.StatisticsSet)Statistics.collected.get(Statistics.collectedStats-1);
				lastAll = lastStats.follows;
				lastNo = lastStats.noEdge;
				lastRemoved = lastStats.removed;
				cyclesDetected = lastStats.cyclesDetected >= 1;
				lastCyclomNo = lastStats.cyclomaticNumber;
				System.out.println("" + lastAll + " connections there, " + lastNo + " not there.");
				System.out.println("" + lastRemoved + " connections removed.");
				System.out.println("\tCycles detected: " + cyclesDetected + ", Cyclomatic Number: " + lastCyclomNo);
				System.out.println("1-WO:\n" + theGraph);
		
				lastEFGCreated = copyOf(theGraph);
				lastEFGFileCreated = myFile1wo;
			}

			// step two: self edges	
			{
				// take time
				times[RED2] = System.currentTimeMillis();
				// do the reduction
				removeEdgesIllegalBySelfLoops();
				writeGraphTo(oS1);
				// take time again
				times[RED2] = System.currentTimeMillis() - times[RED2];
				
				// statistics
				Statistics.countNumEdgesInGraph(theGraph, false);
				Statistics.countNumEdgesRemoved(lastEFGCreated, theGraph, true);
				Cyclomatic.calculateCyclomaticComplexityOf(theGraph, true);
				
				// print results. 
				lastStats = (Cyclomatic.StatisticsSet)Statistics.collected.get(Statistics.collectedStats-1);
				lastAll = lastStats.follows;
				lastNo = lastStats.noEdge;
				lastRemoved = lastStats.removed;
				cyclesDetected = lastStats.cyclesDetected >= 1;
				lastCyclomNo = lastStats.cyclomaticNumber;
				System.out.println("" + lastAll + " connections there, " + lastNo + " not there.");
				System.out.println("" + lastRemoved + " connections removed.");
				System.out.println("\tCycles detected: " + cyclesDetected + ", Cyclomatic Number: " + lastCyclomNo);
				System.out.println("2-RS:\n" + theGraph);
				lastEFGCreated = copyOf(theGraph);
				lastEFGFileCreated = myFile2rs;
			}
			
			// TODO: step 3, expand to child. add catch condition at the base/change the code below, including 3-WO strings to 3-EC.  
			try {
				// take time
				times[RED3] = System.currentTimeMillis();
				DFS(DFSType.EXP2CHILD);
				writeGraphTo(oS3);
				// take time again.
				times[RED3] = System.currentTimeMillis() - times[RED3]; 
				// get statistics.
				Statistics.countNumEdgesInGraph(theGraph, false);
				Statistics.countNumEdgesRemoved(lastEFGCreated, theGraph, true);
				Cyclomatic.calculateCyclomaticComplexityOf(theGraph, true);
				lastStats = (Cyclomatic.StatisticsSet)Statistics.collected.get(Statistics.collectedStats-1);
				lastAll = lastStats.follows;
				lastNo = lastStats.noEdge;
				lastRemoved = lastStats.removed;
				cyclesDetected = lastStats.cyclesDetected >= 1;
				lastCyclomNo = lastStats.cyclomaticNumber;
				
				System.out.println("" + lastAll + " connections there, " + lastNo + " not there.");
				System.out.println("" + lastRemoved + " connections removed.");
				System.out.println("\tCycles detected: " + cyclesDetected + ", Cyclomatic Number: " + lastCyclomNo);
				System.out.println("3-EC:\n" + theGraph);
				
				lastEFGCreated = copyOf(theGraph);
				lastEFGFileCreated = myFile3ec;
			}
			catch(IllegalArgumentException e) {
				System.out.println("Detected illegal EFG node: " + e.getMessage());
				System.out.println("Cannot complete EXP2CHILD reduction");
				System.out.println("Partial output:\n" + theGraph);
			}
		}
		catch(FileNotFoundException e) {
			System.err.println("EFGPacifier: Could not write to file specified.\n"
					+ "Directory structure of filepath does not exist.");
		}
		catch(IOException e)  {
			System.err.println("EFGPacifier: Could not write to file.");
		}
		
		return lastEFGCreated;
	}
	public EFG pacifyInputGraphAndWriteResultsRev4()
	{
		// newest: with myra's solution for exclusion
		File myFileOrig = new File(outputfile + ".EFG");
		File myFile1rs = new File(outputfile + "_g1ec.EFG");
		File myFile2ec = new File(outputfile + "_g2rs.EFG");
		File myFile3wo = new File(outputfile + "_g3wo.EFG");
		
		try(
			FileOutputStream oS0 = new FileOutputStream(myFileOrig);
			FileOutputStream oS1 = new FileOutputStream(myFile1rs);
			FileOutputStream oS2 = new FileOutputStream(myFile2ec);
			FileOutputStream oS3 = new FileOutputStream(myFile3wo);
		)
		{
			// original:
			Statistics.countNumEdgesInGraph(theGraph, true);
			Cyclomatic.calculateCyclomaticComplexityOf(theGraph, true);
			Cyclomatic.StatisticsSet lastStats = (Cyclomatic.StatisticsSet)Statistics.collected.get(Statistics.collectedStats-1);
			int lastAll = lastStats.follows;
			int lastNo = lastStats.noEdge;
			boolean cyclesDetected = lastStats.cyclesDetected >= 1;
			int lastCyclomNo = lastStats.cyclomaticNumber;
			int lastRemoved;
			// print stats. 
			System.out.println("" + lastAll + " connections there, " + lastNo + " not there.");
			System.out.println("\tCycles detected: " + cyclesDetected + ", Cyclomatic Number: " + lastCyclomNo);
			System.out.println("original:\n" + theGraph);
			
			
			lastEFGCreated = copyOf(theGraph);
			lastEFGFileCreated = myFileOrig;
			writeGraphTo(oS0);
			// take the time the simulation starts.
			firstTime = System.currentTimeMillis();
			// step 1
			// expand to child
			try {
				
				DFS(DFSType.EXP2CHILD);
				writeGraphTo(oS2);
				// take time
				times[RED1] = System.currentTimeMillis() - firstTime;
				
				// get stats
				Statistics.countNumEdgesInGraph(theGraph, false);
				Statistics.countNumEdgesRemoved(lastEFGCreated, theGraph, true);
				Cyclomatic.calculateCyclomaticComplexityOf(theGraph, true);
				lastStats = (Cyclomatic.StatisticsSet)Statistics.collected.get(Statistics.collectedStats-1);
				lastAll = lastStats.follows;
				lastNo = lastStats.noEdge;
				lastRemoved = lastStats.removed;
				cyclesDetected = lastStats.cyclesDetected >= 1;
				lastCyclomNo = lastStats.cyclomaticNumber;
				System.out.println("" + lastAll + " connections there, " + lastNo + " not there.");
				System.out.println("" + lastRemoved + " connections removed.");
				System.out.println("\tCycles detected: " + cyclesDetected + ", Cyclomatic Number: " + lastCyclomNo);
				System.out.println("1-EC:\n" + theGraph);
		
				lastEFGCreated = copyOf(theGraph);
				lastEFGFileCreated = myFile2ec;
				
			} catch(IllegalArgumentException e) {
				System.out.println("Detected illegal EFG node: " + e.getMessage());
				System.out.println("Cannot complete EXP2CHILD reduction");
				System.out.println("Partial output:\n" + theGraph);
			}
			
			// step two: self edges
			{
				// take time
				times[RED2] = System.currentTimeMillis();
				// do the reduction
				removeEdgesIllegalBySelfLoops();
				writeGraphTo(oS1);
				// take time again
				times[RED2] = System.currentTimeMillis() - times[RED2];
				
				// statistics
				Statistics.countNumEdgesInGraph(theGraph, false);
				Statistics.countNumEdgesRemoved(lastEFGCreated, theGraph, true);
				Cyclomatic.calculateCyclomaticComplexityOf(theGraph, true);
				
				// print results. 
				lastStats = (Cyclomatic.StatisticsSet)Statistics.collected.get(Statistics.collectedStats-1);
				lastAll = lastStats.follows;
				lastNo = lastStats.noEdge;
				lastRemoved = lastStats.removed;
				cyclesDetected = lastStats.cyclesDetected >= 1;
				lastCyclomNo = lastStats.cyclomaticNumber;
				System.out.println("" + lastAll + " connections there, " + lastNo + " not there.");
				System.out.println("" + lastRemoved + " connections removed.");
				System.out.println("\tCycles detected: " + cyclesDetected + ", Cyclomatic Number: " + lastCyclomNo);
				System.out.println("2-RS:\n" + theGraph);
				lastEFGCreated = copyOf(theGraph);
				lastEFGFileCreated = myFile1rs;
			}
			// step 3 window open close cannot happen. 
			{
				// take time
				times[RED3] = System.currentTimeMillis();
				DFS(DFSType.WOCCH);
				writeGraphTo(oS3);
				// take time again.
				times[RED3] = System.currentTimeMillis() - times[RED3]; 
				// get statistics.
				Statistics.countNumEdgesInGraph(theGraph, false);
				Statistics.countNumEdgesRemoved(lastEFGCreated, theGraph, true);
				Cyclomatic.calculateCyclomaticComplexityOf(theGraph, true);
				lastStats = (Cyclomatic.StatisticsSet)Statistics.collected.get(Statistics.collectedStats-1);
				lastAll = lastStats.follows;
				lastNo = lastStats.noEdge;
				lastRemoved = lastStats.removed;
				cyclesDetected = lastStats.cyclesDetected >= 1;
				lastCyclomNo = lastStats.cyclomaticNumber;
				
				System.out.println("" + lastAll + " connections there, " + lastNo + " not there.");
				System.out.println("" + lastRemoved + " connections removed.");
				System.out.println("\tCycles detected: " + cyclesDetected + ", Cyclomatic Number: " + lastCyclomNo);
				System.out.println("3-WO:\n" + theGraph);
				
				lastEFGCreated = copyOf(theGraph);
				lastEFGFileCreated = myFile3wo;
			}
		}	
		catch(FileNotFoundException e) {
			System.err.println("EFGPacifier: Could not write to file specified.\n"
					+ "Directory structure of filepath does not exist.");
		}
		catch(IOException e)  {
			System.err.println("EFGPacifier: Could not write to file.");
		}
		
		return lastEFGCreated;
	}
	/**
	 * Newest: with Myra's solution for exclusion
	 */
//	public EFG pacifyInputGraphAndWriteResultsRev3()
//	{
//		// newest: with myra's solution for exclusion
//		File myFileOrig = new File(outputfile + ".EFG");
//		File myFile1rs = new File(outputfile + "_g1rs.EFG");
//		File myFile2ec = new File(outputfile + "_g2ec.EFG");
//		File myFile3wo = new File(outputfile + "_g3wo.EFG");
//		
//		try(
//			FileOutputStream oS0 = new FileOutputStream(myFileOrig);
//			FileOutputStream oS1 = new FileOutputStream(myFile1rs);
//			FileOutputStream oS2 = new FileOutputStream(myFile2ec);
//			FileOutputStream oS3 = new FileOutputStream(myFile3wo);
//		)
//		{
//			// original:
//			Statistics.countNumEdgesInGraph(theGraph, true);
//			Statistics.StatisticsSet lastStats = Statistics.collected.get(Statistics.collectedStats-1);
//			int lastAll = lastStats.follows;
//			int lastNo = lastStats.noEdge;
//			
//			System.out.println("" + lastAll + " connections there, " + lastNo + " not there.");
//			System.out.println("original:\n" + theGraph);
//			
//			writeGraphTo(oS0);
//			lastEFGCreated = copyOf(theGraph);
//			lastEFGFileCreated = myFileOrig;
//			
//			// self loops
//			removeEdgesIllegalBySelfLoops();
//			Statistics.countNumEdgesInGraph(theGraph, false);
//			Statistics.countNumEdgesRemoved(lastEFGCreated, theGraph, true);
//			Cyclomatic.calculateCyclomaticComplexityOf(theGraph, true);
//			
//			lastStats = Statistics.collected.get(Statistics.collectedStats-1);
//			lastAll = lastStats.follows;
//			lastNo = lastStats.noEdge;
//			int lastRemoved = lastStats.removed;
//			System.out.println("" + lastAll + " connections there, " + lastNo + " not there.");
//			System.out.println("" + lastRemoved + " connections removed.");
//			System.out.println("" + 1 + ":\n" + theGraph);
//			
//			writeGraphTo(oS1);
//			lastEFGCreated = copyOf(theGraph);
//			lastEFGFileCreated = myFile1rs;
//			
//			// expand to child
//			try {
//				DFS(DFSType.EXP2CHILD);
//				Statistics.countNumEdgesInGraph(theGraph, false);
//				Statistics.countNumEdgesRemoved(lastEFGCreated, theGraph, true);
//				lastStats = Statistics.collected.get(Statistics.collectedStats-1);
//				lastAll = lastStats.follows;
//				lastNo = lastStats.noEdge;
//				lastRemoved = lastStats.removed;
//				System.out.println("" + lastAll + " connections there, " + lastNo + " not there.");
//				System.out.println("" + lastRemoved + " connections removed.");
//				System.out.println("" + 2 + ":\n" + theGraph);
//		
//				writeGraphTo(oS2);
//				lastEFGCreated = copyOf(theGraph);
//				lastEFGFileCreated = myFile2ec;
//				
//			} catch(IllegalArgumentException e) {
//				System.out.println("Detected illegal EFG node: " + e.getMessage());
//				System.out.println("Cannot complete EXP2CHILD reduction");
//				System.out.println("Partial output:\n" + theGraph);
//			}
//			
//			DFS(DFSType.WOCCH);
//			Statistics.countNumEdgesInGraph(theGraph, false);
//			Statistics.countNumEdgesRemoved(lastEFGCreated, theGraph, true);
//			lastStats = Statistics.collected.get(Statistics.collectedStats-1);
//			lastAll = lastStats.follows;
//			lastNo = lastStats.noEdge;
//			lastRemoved = lastStats.removed;
//			System.out.println("" + lastAll + " connections there, " + lastNo + " not there.");
//			System.out.println("" + lastRemoved + " connections removed.");
//			System.out.println("" + 3 + ":\n" + theGraph);
//			
//			
//			writeGraphTo(oS3);
//			lastEFGCreated = copyOf(theGraph);
//			lastEFGFileCreated = myFile3wo;
//		} 
//			
//		catch(FileNotFoundException e) 
//		{
//			System.err.println("EFGPacifier: Could not write to file specified.\n"
//					+ "Directory structure of filepath does not exist.");
//		}
//		catch(IOException e) 
//		{
//			System.err.println("EFGPacifier: Could not write to file.");
//		}
//		
//		return lastEFGCreated;
//	}

	public static EFG copyOf(EFG theGraph)
	{
		ObjectFactory fact = new ObjectFactory();
		EFG toReturn = fact.createEFG();
		EventGraphType egt = fact.createEventGraphType();
		EventsType ev = fact.createEventsType();
		toReturn.setEvents(ev);
		toReturn.setEventGraph(egt);
		for(int i = 0; i < theGraph.getEventGraph().getRow().size(); i++) {
			RowType rt = fact.createRowType();
			rt.getE().addAll(theGraph.getEventGraph().getRow().get(i).getE());
			toReturn.getEventGraph().getRow().add(rt);
		}
		toReturn.getEvents().getEvent().addAll(theGraph.getEvents().getEvent());
		return toReturn;
	}
	public EFG getLastGraphCreated()
	{
		return lastEFGCreated;
	}
	
	public File getLastFileCreated()
	{
		return lastEFGFileCreated;
	}
	
	public void initDFS()
	{
		colors = new int[graphRows.size()];
	}
	
	public int[] initCurrents(int[] currents)
	{
		for(int i = 0; i < currents.length; i++)
			currents[i] = -1;
		return currents;
	}
	
	
	
	public void initDFS(int[] initColors)
	{
		this.colors = Arrays.copyOf(initColors, initColors.length);
	}
	
	/**
	 * Start the DFS search for edges to remove, and then remove them
	 * @param reason
	 */
	@SuppressWarnings("unchecked")
	public void DFS(DFSType reason)
	{
		initDFS();
		// do the initials first. 
		Integer[] initialNodes = getAllInitial();
		
		switch(reason) {
			
			
			case MEXCLUSION : {
				int[] currents = new int[mutualSets.length];
				initCurrents(currents);
				for(int i : initialNodes)
					DFSVerifyVisitMyra(i, reason, currents);
			}
			case EXP2CHILD :  
			case WOCCH :  
			case EIMAINWIN : 
			{
				for(int i : initialNodes) 
					if(colors[i] == WHITE)
						DFSVisit(i, reason);
				for(int i = 0; i < allEvents.size(); i++) {
					if(colors[i] == WHITE)
						DFSVisit(i, reason);
				}
			}
			break; case EXCLUSION :
			{
				// initialize exclusion parameters.
				L = (HashSet<Integer>[]) new HashSet<?>[allEvents.size()];
				for(int i = 0; i < L.length; i++) 
					L[i] = new HashSet<Integer>();
				dotted = new boolean[allEvents.size()];
				nextAdjacent = new int[allEvents.size()];
				
				for(int i : initialNodes) 
					DFSValidateVisit(i, reason, true, new int[mutualSets.length]);	
			}

			break; case ORDER :  
			{
				Set<Integer> rootProblems;
				nextAdjacent = new int[allEvents.size()];
				dotted = new boolean[allEvents.size()];
				order = new HashSet[allEvents.size()];
				for(int i = 0; i < allEvents.size(); i++)  
					order[i] = new HashSet<Integer>();
				
				
				for(int i : initialNodes) {
					rootProblems = DFSSmartVisit(i, reason, false, initCurrents(new int[orderSets.length]));
					
					if(!rootProblems.isEmpty())
						removeInitial(i);
					nextAdjacent = new int[allEvents.size()];
					dotted = new boolean[allEvents.size()];
					for(int j = 0; j < order.length; j++)
						order[j].clear();
					
				}
			}
			break; case REQUIRED : 
			{
				if(requiredEvents.length == 0) 
					break;
				boolean[] startActives = new boolean[requiredEvents.length];
				int req = -1;
				ArrayList<Integer> primes = new ArrayList<Integer>();
				int[] blackArray = new int[graphRows.size()];
				for(req = 0; req < requiredEvents.length; req++) {
					if(implicatedRequiredIndex(req) != -1) 
						if(colors[req] == WHITE) {
							if(DFSCoverVisit(req, DFSType.REQUIRED, startActives)) {
								primes.add(req);
								for(int i = 0; i < allEvents.size(); i++) 
									if(implicatedRequiredIndex(i) != -1 && colors[i] == BLACK)
										blackArray[i] = BLACK;
								initDFS(blackArray);
							}
						}
				}
				initDFS();
				if(!primes.isEmpty()) {
					requiredPrimeEvents = new int[primes.size()];
					for(int i = 0; i < primes.size(); i++)
						requiredPrimeEvents[i] = primes.get(i);
					
					boolean startViolation;
					for(int i : initialNodes) {
						startViolation = DFSVerifyVisit(i, DFSType.REQPRIME);
						if(startViolation) 
							removeInitial(i); // can't use this node to create a test case path.
											  // if we can't find a required path.
					}
				}
				else {
					// we weren't able to connect the required nodes together
					// in a subgraph, so we'll never be able to connect
					// them together in any larger one, 
					// including a larger graph containing the initial nodes. 
					ObjectFactory fact = new ObjectFactory();
					theGraph.setEventGraph(fact.createEventGraphType());
					theGraph.setEvents(fact.createEventsType());
				}
			}
			break; case REQPRIME : 
			break; case EXCPRIME : 
			break; case REPEATALLCYCLE :
			break;
		}
		
	}

	public void writeGraphTo(OutputStream os)
	{
		XMLHandler handler = new XMLHandler();
		handler.writeObjToFileNoClose(theGraph, os);
	}
	
	
	
	
	/**
	 * Removes all edges that are illegal because of their self loops.
	 */
	private void removeEdgesIllegalBySelfLoops()
	{
		for(int i = 0; i < graphRows.size(); i++) {
			if(!implicatesRepeatEvent(i))
				removeEdge(i, i);
		}
	}
	
	
	public boolean allSatisfied(boolean[] currentList)
	{
		for(int i = 0; i < currentList.length; i++)
			if(!currentList[i])
				return false;
		return true;
	}
	
	public boolean allSatisfied(int[] currentList)
	{
		for(int i = 0; i < currentList.length; i++)
			if(currentList[i] == -1)
				return false;
		return true;
	}
	
	
	public boolean DFSVerifyVisit(int i, DFSType reason)
	{
		grayColor(i);
		if(reason == DFSType.REQPRIME){ 
			int reqIndex = implicatedRequiredPrime(i);
			if(reqIndex != -1)  
				return true;
		}
		
		
		Integer[] outgoing = edgesOutgoingFrom(i);
		boolean foundPrime = false;
		ArrayList<Integer> notFoundOutgoing = new ArrayList<Integer>();
		for(int j : outgoing) {
			if(reason == DFSType.REQPRIME) {
				foundPrime = DFSVerifyVisit(j, reason);
				if(!foundPrime)
					notFoundOutgoing.add(j);
			}
		}
		
		// post
		if(foundPrime) // only if we found a prime in the rough
			for(int j : notFoundOutgoing)
				removeEdge(i,j);
		
		return foundPrime; // yes if we found a prime, no if we did not. 
	}
	
	public boolean DFSCoverVisit(int i, DFSType reason, boolean[] olds)
	{
		grayColor(i);
		boolean[] currents = Arrays.copyOf(olds, olds.length);
		Integer[] groups = null;
		if(reason == DFSType.REQUIRED) {
			int reqIndex = implicatedRequiredIndex(i);
			if(reqIndex == -1) 
				groups = new Integer[0];
			else {
				groups = new Integer[]{reqIndex};
				for(int set : groups)
					currents[set] = true;
				if(allSatisfied(currents)) // if we just satisfied the requirement. 
					return true;
			}
		}
		if(reason == DFSType.EXCPRIME) {
			groups = implicatedExcludeGroupsOfEvent(i);
			for(int set : groups)
				currents[set] = true;
			if(allSatisfied(currents))
				return true;
		}
		
		Integer[] outgoing = edgesOutgoingFrom(i);
		ArrayList<Integer> badOutgoing = new ArrayList<Integer>();
		boolean foundYes = false;
		for(int j : outgoing) {
			if(reason == DFSType.REQUIRED) 
				if(colors[j] == WHITE || colors[j] == BLACK) {
					foundYes = DFSCoverVisit(j, reason, currents);
					if(foundYes)
						break;
				}
			if(reason == DFSType.EXCPRIME) {
				if(DFSCoverVisit(j, reason, currents)) 
					foundYes = true;
				else 
					badOutgoing.add(j);
			}
		}
		
		if(reason == DFSType.EXCPRIME) {
			if(!foundYes)
				return false;
			else {
				for(int j : badOutgoing)
					removeEdge(i, j);
				return true;
			}
		} 
		blackColor(i);
		return foundYes;
	}
	
	/**
	 * Return the list of widgets that are detected as "new on this exclusion path":<br> 
	 * Given the event specified by row, is its widget on the same path as one specified for its exclusion
	 * group? If so and the widget of this event is not already listed in currents, 
	 * it is a new widget on an active exclusion path: Since new widgets on active exclusion paths
	 * need to be recorded, we return it in the returned arraylist. 
	 */
	private boolean newlyDetectedOnExclusionPath(int row, int excIndex, int[] currents)
	{
		int wNum = widgetForEvent(allEvents.get(row));
		if(currents[excIndex] != -1 && currents[excIndex] != wNum) // this widget is new on this path. 
			return true;
		else
			return false; // either there was no other widget in row's set on this path, or we hit the same widget twice. 
		

	}
	
	/**
	 * Returns true if Edge between row and col should be removed due to active mutual exclusion
	 * constraints (i.e. a widget in col's mutual exclusion group is already active. 
	 */
	public int illegalByMutualExclusion(int row, int col, int[] mutualActive)
	{
		EventType child = allEvents.get(col);
		int cWidgNum = widgetForEvent(child);
		Integer[] groups = implicatedExcludeGroupsOfEvent(col);
		for(int mg : groups) {
			// if mutual exclusion in this widget's mutual exclusion set is
			// already active, then this edge has to go.
			if(mutualActive[mg] != -1 
			&& mutualActive[mg] != cWidgNum)
				return mg;
		}
		return -1;
	}
	
	private boolean shouldBreakOrderPathsFromNode(int vertex, Set<Integer> problemSet, boolean[] forRemoval)
	{
		for(int i = 0; i < forRemoval.length; i++) 
			if(forRemoval[i] == false)
				return true; // if one vertex isn't marked for removal, removing the rest won't destroy the graph. 
	
		for(int i : implicatedOrderSets(vertex)) 
			if(problemSet.contains(i)) 		// if an order group of v is in the problem set
				return true;
		
		return false;
	}

	/**
	 * Returns a set of order sets depending on whether edge between row and col should be removed
	 * due to active order set constraints. No valus are returned if the 
	 * edge should not be removed. Otherwise, the values returned are the sets that caused the disruption.
	 */
	private Integer[] reachableAndOutOfOrderFrom(int downstreamVertex, int[] currents)
	{
		Set<Integer> underGroups = order[downstreamVertex];
		ArrayList<Integer> illegalSets = new ArrayList<Integer>();
		
		for(int k = 0; k < orderSets.length; k++) 
			if(currents[k] != -1) {
				int found = firstFoundInSet(currents[k], underGroups, k);
				if(found != currents[k] && found != -1) 
					illegalSets.add(k);
			}
		return illegalSets.toArray(new Integer[0]);
		// no order rule was broken if child does not precede any active order sets. 
	}

	/**
	 * Answer the question: of the exclusion points at current stake, how can we best solve the problems that
	 * are incumbent of the L list of the child specified?
	 */
	public ArrayList<Integer> solveableByMutualExclusion(int col, boolean[] points)
	{
		ArrayList<Integer> solveable = new ArrayList<Integer>();
		for(int excIndex = 0; excIndex < points.length; excIndex++) 
			if(points[excIndex] && L[col].contains(excIndex)) 
				solveable.add(excIndex);
		
		return solveable;
	}
	
	public void clear(int[] array)
	{
		for(int i = 0; i < array.length; i++)
			array[i] = -1;
	}
	
	
	public Set<Integer> DFSValidateVisit(int i, DFSType reason, boolean breakPath, int[] olds)
	{
		grayColor(i);
		Integer[] groups = null;
		int[] currents;
		boolean[] exclusionPoints = new boolean[olds.length];
		
		switch(reason) {
			case EXCLUSION: {
				HashSet<Integer> newForL = new HashSet<Integer>();
				if(breakPath) { 
					currents = new int[olds.length];
					for(int set = 0; set < olds.length; set++)
						currents[set] = -1;
				}
				else { 
					currents = Arrays.copyOf(olds, olds.length);
					for(int k = 0; i < currents.length; i++) 
						if(currents[k] != -1)
							newForL.add(k);
				}
				L[i].addAll(newForL);
				newForL.clear();
				breakPath = false;
				groups = implicatedExcludeGroupsOfEvent(i);
				for(int set : groups) {
					newForL.add(set);
					if(newlyDetectedOnExclusionPath(i, set, currents))
						breakPath = true;
					currents[set] = i;
				}
				L[i].addAll(newForL);
				newForL.clear();
				
				Integer[] outgoing = edgesOutgoingFrom(i);
				ArrayList<Integer> badGroups = new ArrayList<Integer>();
				ArrayList<Integer> badVertices = new ArrayList<Integer>();
				if(outgoing.length == 0)
					dotted[i] = true;
				else{
					int jIdx;
					for(jIdx = nextAdjacent[i]; jIdx < outgoing.length; jIdx++) {
						nextAdjacent[i]++;
						if(jIdx == outgoing.length-1) 
							dotted[i] = true;
						int j = outgoing[jIdx];
						if(!dotted[j]) 
							DFSValidateVisit(j, reason, breakPath, currents);
						
						ArrayList<Integer> solveable = solveableByMutualExclusion(j, exclusionPoints);
						if(!solveable.isEmpty()) {
							badVertices.add(j);
							badGroups.addAll(solveable);
						}
					}
				}
				if(outgoing.length == badVertices.size()) 
					L[i].addAll(badGroups);
				else  {
					while(!badVertices.isEmpty()) 
						removeEdge(i,badVertices.remove(0));
					L[i].removeAll(badGroups);
				}
			}
				
	break;	default:
			
				
		}

		return L[i];
		
	}
	public Set<Integer> DFSVerifyVisitMyra(int i, DFSType reason, int[] olds)
	{
		grayColor(i);
		HashSet<Integer> toReturn = new HashSet<Integer>();
		for(int j : edgesOutgoingFrom(i)) {
			int[] currents = Arrays.copyOf(olds, olds.length);
			Integer[] groups = implicatedExcludeGroupsOfEvent(j);
			for(int g : groups) {
				if(newlyDetectedOnExclusionPath(j, g, currents)) {
					removeEdge(i,j);
					clear(currents);
				}
				currents[g] = j;
				if(colors[j] == WHITE)
					DFSVerifyVisitMyra(j, reason, currents);
			}
		}
		return toReturn;
	}
	public Set<Integer> DFSSmartVisit(int i, DFSType reason, boolean startNewPath, int[] olds)
	{
		
		int[] currents = new int[olds.length];
		if(startNewPath) {
			for(int k = 0; k < currents.length; k++) 
				currents[k] = -1;
			startNewPath = false;
		}
		else
			for(int k = 0; k < currents.length; k++) 
				currents[k] = olds[k];
		// we might need to clear the currents array if a 
		// path starts at i and doesn't flow through i. 
		
		Integer[] sets = null;
		if(reason == DFSType.ORDER) {
			sets = implicatedOrderSets(i);
			for(int set : sets)  {
				order[i].add(set);
				int iGroup = implicatedOrderGroupWithinSet(i, set);
				if(currents[set] != -1 && orderGroupPrecedesOther(iGroup, currents[set], set)) // if I have  
					startNewPath = true; // this will potentially be the start of a bad path. 
				currents[set] = iGroup;
			}
		}
		
		Integer[] outgoing = edgesOutgoingFrom(i);
		boolean[] marked = new boolean[outgoing.length];
		boolean[] badOutgoingSets = new boolean[currents.length];
		HashSet<Integer> L = new HashSet<Integer>();
		if(outgoing.length == 0)
			setDotted(i);
		else {
			if(reason == DFSType.ORDER) {
				int jIdx;
				for(jIdx = nextAdjacent[i]; jIdx < outgoing.length; jIdx++) {
					nextAdjacent[i]++;
					if(jIdx == outgoing.length-1) 
						dotted[i] = true;
					int j = outgoing[jIdx];
					if(!dotted[j]) 
						L.addAll(DFSSmartVisit(j, reason, startNewPath, currents));
					
					order[i].addAll(order[j]);
					for(int k = 0; k < currents.length; k++) {
						if(order[j].contains(k)) { // 
							Integer[] illegalSets = reachableAndOutOfOrderFrom(j, currents);
							if(illegalSets.length > 0) {
								L.add(k);
								for(int ill : illegalSets)  
									badOutgoingSets[ill] = true;
								marked[jIdx] = true;
							}
						}
					}
				}
			}
		}
		if(outgoing.length > 0) 
			if(reason == DFSType.ORDER) {
				if(shouldBreakOrderPathsFromNode(i, L, marked)) {
					for(int jIdx = 0; jIdx < outgoing.length; jIdx++) {
						int j = outgoing[jIdx];
						if(marked[jIdx]) 
							removeEdge(i, j);
					}
					// remove all the offending edges
					for(int k = 0; k < badOutgoingSets.length; k++) 
						if(badOutgoingSets[k] == true) 
							order[i].remove(k);
					return new HashSet<Integer>();
				}
				return L;
			}
		return L;
	}
	
	
	/**
	 * Remove edges exiting source i according to the different rules specified by reason.
	 */
	public void DFSVisit(int i, DFSType reason)
	{
		grayColor(i);
		Integer[] outgoing = edgesOutgoingFrom(i);
		for(int j : outgoing) {
			if(reason == DFSType.EXP2CHILD) {
				if(illegalByExpandTo(i, j)) 
					removeEdge(i,j);
			}
			else if(reason == DFSType.WOCCH) {
				if(illegalByWindowOpenClose(i, j)) 
					removeEdge(i,j);
			}
			else if(reason == DFSType.EIMAINWIN) {
				if(illegalByTransitionNonInitialToInitial(i, j)) 
					removeEdge(i, j);
			}
			
			if(colors[j] == WHITE) 
				DFSVisit(j, reason); 
		}
		blackColor(i);
	}
		
	public int widgetForEvent(EventType event)
	{
		for(int i = 0; i < allWidgets.size(); i++)
			if(event.getWidgetId().equals(allWidgets.get(i).getName()))
				return i;
		return -1;	
	}
	

	/**
	 * What event does this widget's event id point us to
	 * @param w
	 * @return
	 */
	public int findEvent(Widget w)
	{
		for(int i = 0; i < allEvents.size(); i++) 
			if(w.getEventID().equals(allEvents.get(i).getEventId()))
				return i;
		return -1;
	}	
	
	/**
	 * Return true if toCheck has an event id containing the display string of the role specified.
	 */
	private boolean eventHasSubRole(AccessibleRole role, EventType toCheck)
	{
		String eventId = toCheck.getEventId();
		int bookmarkColon = eventId.indexOf(EFG2GraphvizFixString.NAME_VERSION_SEPARATOR);
		String parentRole;
		if(bookmarkColon == -1) 
			parentRole = eventId.substring(0, eventId.indexOf(EFG2GraphvizFixString.EVENT_ID_SPLITTER_CHAR));
		else {
			String toConsider = eventId.substring(bookmarkColon+1);
			parentRole = toConsider.substring(0, toConsider.indexOf(EFG2GraphvizFixString.EVENT_ID_SPLITTER_CHAR));
		}
		return parentRole.contains(role.toDisplayString());
	}
	
	/**
	 * Return true if toCheck has an event id matching the display string of the role specified. 
	 */
	private boolean eventHasRole(AccessibleRole role, EventType toCheck)
	{	
		String eventId = toCheck.getEventId();
		int bookmarkColon = eventId.indexOf(EFG2GraphvizFixString.NAME_VERSION_SEPARATOR);
		String parentRole;
		if(bookmarkColon == -1) 
			parentRole = eventId.substring(0, eventId.indexOf(EFG2GraphvizFixString.EVENT_ID_SPLITTER_CHAR));
		else {
			String toConsider = eventId.substring(bookmarkColon+1);
			parentRole = toConsider.substring(0, toConsider.indexOf(EFG2GraphvizFixString.EVENT_ID_SPLITTER_CHAR));
		}
		return role.toDisplayString().equals(parentRole);
	}
	
	/**
	 * Tests two events parent and child, to see if child is below parent in parent's combo box hierarchy.
	 * Child is a child of parent iff parent and child event id name identifiers are equal,
	 * and if the action of the parent is a click action, while the action of the child
	 * is a select action.
	 */
	private boolean parentOfComboChild(EventType parent, EventType child)
	{
		try {
			String parentName = parent.getEventId();
			String childName = child.getEventId();
			
			// we must remove the first part of the event identifier the former version of the event identifier before continuing. 
			if(childName.contains(EFG2GraphvizFixString.NAME_VERSION_SEPARATOR)) 
				childName = childName.substring(childName.indexOf(EFG2GraphvizFixString.NAME_VERSION_SEPARATOR)+1); 
			if(parentName.contains(EFG2GraphvizFixString.NAME_VERSION_SEPARATOR)) 
				parentName = parentName.substring(parentName.indexOf(EFG2GraphvizFixString.NAME_VERSION_SEPARATOR)+1);
			// we must compare the core names 
			
			int[] pSeps = StringTools.findNCharactersIn(parentName, '_', 2);
			int[] cSeps = StringTools.findNCharactersIn(childName, '_', 2);
			String parentSubname = parentName.substring(pSeps[0]+1, pSeps[1]);
			String parentAction = parent.getAction();
			
			String childSubname = childName.substring(cSeps[0]+1, cSeps[1]);
			String childAction = child.getAction();

			boolean subnamesEqual = parentSubname.equals(childSubname);
			boolean compatibleActions = parentAction.equals(ActionClass.ACTION.actionName)
					&& childAction.equals(ActionClass.PARSELECT.actionName);
			return subnamesEqual && compatibleActions;
		} 
		catch(ArrayIndexOutOfBoundsException e) {
			System.out.println("parent: " + parent.getEventId() + " child: " + child.getEventId());
			throw new IllegalArgumentException("Detected list or list item event in EFG without a matching widget.");
		}
		catch(IndexOutOfBoundsException e) {
			System.out.println("parentw: " + parent.getEventId() + " childw: " + child.getEventId());
			throw new IllegalArgumentException("Detected list or list item event in EFG with an invalid expand hierarchy structure.\n");
		}
	}
	
	/**
	 * Preconditions: Parent and child should be of same hierarchical structure. 
	 * @param parent
	 * @param child
	 * @return
	 */
	private boolean parentOfChild(EventType parent, EventType child)
	{
		try {
			String parentName = parent.getEventId();
			String childName = child.getEventId();
			if(!childName.contains("|")) // a child of some parent always contains | to indicate hierarchy 
				return false;
			// we must remove the first part of the event identifier the former version of the event identifier before continuing. 
			if(childName.contains(EFG2GraphvizFixString.NAME_VERSION_SEPARATOR)) 
				childName = childName.substring(childName.indexOf(EFG2GraphvizFixString.NAME_VERSION_SEPARATOR)+1); 
			if(parentName.contains(EFG2GraphvizFixString.NAME_VERSION_SEPARATOR)) 
				parentName = parentName.substring(parentName.indexOf(EFG2GraphvizFixString.NAME_VERSION_SEPARATOR)+1);
			
			String parentConstruct = childName.substring(childName.indexOf('_')+1); // since the widget id has to have this underscore. 
			parentConstruct = parentConstruct.substring(0, parentConstruct.lastIndexOf('|'));
			int[] pSeps = StringTools.findNCharactersIn(parentName, '_', 2);
			String targetParentName = parentName.substring(pSeps[0]+1, pSeps[1]);
			return parentConstruct.equals(targetParentName);
		} 
		catch(ArrayIndexOutOfBoundsException e) {
			System.out.println("parent: " + parent.getEventId() + " child: " + child.getEventId());
			throw new IllegalArgumentException("Detected list or list item event in EFG without a matching widget.");
		}
		catch(IndexOutOfBoundsException e) {
			System.out.println("parentw: " + parent.getEventId() + " childw: " + child.getEventId());
			throw new IllegalArgumentException("Detected list or list item event in EFG with an invalid expand hierarchy structure.\n");
		}
	}
	
	public boolean illegalByExpandTo(int row, int col)
	{
		EventType parent = allEvents.get(row);
		EventType child = allEvents.get(col);
		
		
		
		if(parent.getType().equals(EXPAND)) {
			if(eventHasRole(MENU_BAR, parent)) { // handler for menu bars and child menus
				if(!eventHasRole(MENU, child)) 
					return true;
				if(!parentOfChild(parent, child))
					return true;
			}
			
			else if(eventHasRole(MENU, parent)) { // handler for menus and child menu items. 
				if(!eventHasRole(MENU_ITEM, child) && !(eventHasRole(MENU, child))) // if the child is not either a menu item or submenu,
					return true;
				if(!parentOfChild(parent, child))
					return true;
			}
			else if(eventHasSubRole(COMBO_BOX, parent)) { // handler for combo boxes and child combo box list items.
				if(!eventHasSubRole(COMBO_BOX, child))
					return true;
				if(!parentOfComboChild(parent, child)) 
					return true;
			}
			else if(eventHasRole(LIST, parent)) { // handler for flat lists, and child list items. 
				if(!eventHasRole(LIST_ITEM, child))
					return true;
				if(!parentOfChild(parent, child))
					return true;
			}
		}
		return false;
	}

	private boolean illegalByWindowOpenClose(int row, int col)
	{
		EventType parent = allEvents.get(row);
		EventType child = allEvents.get(col);
		
		if(parent.getType().equals(RESTRICED_FOCUS) // CHECK FOR THE MISSPELLING TOO. 
		|| parent.getType().equals(RESTRICTED_FOCUS)
		|| parent.getType().equals(UNRESTRICED_FOCUS) // CHECK FOR THE MISSPELLING TOO.
		|| parent.getType().equals(UNRESTRICTED_FOCUS)
		)
			if(child.getType().equals(TERMINAL))
				return true;
		return false;
	}
	
	private boolean illegalByTransitionNonInitialToInitial(int row, int col)
	{
		EventType parent = allEvents.get(row);
		EventType child = allEvents.get(col);
		
		if(!parent.isInitial() && child.isInitial())
			return true;
		return false;
	}

	private void removeEdge(int row, int col)
	{
		graphRows.get(row).getE().set(col, 0);
	}
	
	private void removeInitial(int row)
	{
		allEvents.get(row).setInitial(false);
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
	
	private Integer[] edgesOutgoingFrom(int vertex)
	{
		ArrayList<Integer> toReturn = new ArrayList<Integer>();
		List<Integer> row = graphRows.get(vertex).getE();
		int rowSize = row.size();
		for(int i = 0; i < rowSize; i++) 
			if(row.get(i) > 0)
				toReturn.add(i);
		return toReturn.toArray(new Integer[0]);
	}
	
	
	public enum FollowsType {
		NONE(0), FOLLOWS(1), REACHING(2), FOLLOWS_TO_INITIAL(3);
		public final int guitarInt;
	
		FollowsType(int guitarInt)
		{
			this.guitarInt = guitarInt;
		}
	
		public static FollowsType typeForConst(int guitarInteger)
		{
			switch(guitarInteger) {
			case 0 : return NONE;
			case 2 : return REACHING;
			}
			return FOLLOWS;
		}
		public boolean isEdge()
		{
			if(this == NONE)
				return false;
			return true;
		}
	}
	
	

	
	public static class Old
	{
//		private FollowsType edgeValue(int row, int col)
//		{
//			int edgeValue = graphRows.get(row).getE().get(col);
//			switch(edgeValue) {
//			case NO_EDGE		: return FollowsType.NONE;
//			case REACHING_EDGE 	: return FollowsType.REACHING; 
//			}
//			return FollowsType.FOLLOWS; // value should be 1
//		}
//		private FollowsType[] edgeTypesOutgoingFrom(int vertex)
//		{
//			ArrayList<FollowsType> toReturn = new ArrayList<FollowsType>();
//			for(Integer i : graphRows.get(vertex).getE()) {
//				if(i > 0){
//					if(i == 1) {
//						if(allEvents.get(i).isInitial())
//							toReturn.add(FollowsType.FOLLOWS_TO_INITIAL);
//						else
//							toReturn.add(FollowsType.FOLLOWS);
//					}
//					else if(i == 2)
//						toReturn.add(FollowsType.REACHING);
//				}
//			}
//			return toReturn.toArray(new FollowsType[0]);
//		}
	}
}
