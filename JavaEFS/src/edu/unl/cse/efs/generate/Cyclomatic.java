package edu.unl.cse.efs.generate;

import java.util.*;

import edu.umd.cs.guitar.model.data.EFG;
import edu.umd.cs.guitar.model.data.EventGraphType;
import edu.umd.cs.guitar.model.data.ObjectFactory;
import edu.umd.cs.guitar.model.data.RowType;

public class Cyclomatic extends Statistics 
{	
	static final int WHITE = 0, GRAY = 1, BLACK = 2;
	public static int calculateCyclomaticComplexityOf(EFG graph, boolean addToLast)
	{
		int numSCCs;
		
		EventGraphType gt = graph.getEventGraph();
		List<RowType> gtRows = gt.getRow();
		
		int rowSize = gtRows.size();
		if(rowSize == 0) {
			collect(0, 0, addToLast);
			return 0;
		}
		
		int[] results = dfsAndCollectFinishPlusOne(rowSize, gtRows);
		numSCCs = results[results.length-1];

		int numNodes = rowSize;
		int numEdges = countNumEdgesInGraph(graph, addToLast);
//		numSCCs = results[results.length-1];
		
		int cyclomatic = numEdges - numNodes + 2;
		collect(numSCCs, cyclomatic, true);
		return cyclomatic;
	}
	
	public static List<RowType> rowsTransposeOf(List<RowType> gtRows)
	{
		List<RowType> outputRows = new ArrayList<RowType>();
		int rowSize = gtRows.size();
		if(rowSize == 0) 
			return outputRows;
		ObjectFactory fact = new ObjectFactory();
		// initialize the output to be the correct size.  
		for(int i = 0; i < gtRows.size(); i++) {
			RowType newRow = fact.createRowType();
			for(int j = 0; j < gtRows.get(i).getE().size(); j++) 
				newRow.getE().add(0);
			outputRows.add(newRow);
		}
		
		// compute the transpose. 
		for(int i = 0; i < gtRows.size(); i++) 
			for(int j = 0; j < gtRows.get(i).getE().size(); j++) 
				outputRows.get(j).getE().set(i, gtRows.get(i).getE().get(j));
		
		// return the transpose. 
		return outputRows;
	}
	public static void collect(int cycles, int complexity, boolean addToLast)
	{
		if(addToLast) {
			if(collectedStats == 0) {
				collected.add(new Cyclomatic.StatisticsSet(cycles, complexity));
				collectedStats++;
			}
			else {
				Statistics.StatisticsSet last = collected.get(collectedStats-1);
				if(last instanceof Cyclomatic.StatisticsSet) {
					((Cyclomatic.StatisticsSet)last).setCycleData(cycles, complexity);
					collected.set(collectedStats-1, last);
				}
				else {
					Cyclomatic.StatisticsSet newSet = new Cyclomatic.StatisticsSet(last);
					newSet.setCycleData(cycles, complexity);
					collected.set(collectedStats-1, newSet);
				}
			}
		}
		else {
			collected.add(new Cyclomatic.StatisticsSet(cycles, complexity));
			collectedStats++;
		}
	}
	
	
	/**
	 * This runs an iterative DFS procedure on the graph specified by gtRows, selecting sources in the order they are
	 * specified in the orderedVertices array.
	 * Return data collected from dfs plus one of the following: 
	 * if stageOne is true, collect information regarding the number of cycles found in this procedure.
	 * otherwise, collect information about the number of components, used to search this array.
	 * 
	 * @param rowSize
	 * @param stageOne
	 * @param gtRows
	 * @return
	 */
	public static int[] dfsAndCollectFinishPlusOne(int rowSize, List<RowType> gtRows)
	{
		LinkedList<Integer> nodeStack = new LinkedList<Integer>();
		if(rowSize == 0)
			return new int[]{0}; // no nodes, no cycles. 
		
		int[] orderedVertices = new int[rowSize];
		for(int i = 0; i < orderedVertices.length; i++) 
			orderedVertices[i] = i;
		
		// initialize a DFS array.  
		int[] colors = new int[rowSize];
		int[] start = new int[rowSize];
		int[] finishing = new int[rowSize];
		int[] next = new int[rowSize];
		int nextNode = pickUncolored(orderedVertices, colors);
		grayColor(nextNode, colors);
		int timer = 1;
		start[nextNode] = timer;
		nodeStack.push(nextNode);
		
		int cycleCount = 0;
		
		while(nextNode != -1) { 
			Integer[] outgoing = edgesOutgoingFrom(gtRows, nextNode);
			int picked = pickNext(nextNode, next, outgoing);
			if(picked == -1) { // no outgoing children of nextNode left to pick.
				timer++;
				finishing[nextNode] = timer;
				blackColor(nextNode, colors);
				nodeStack.pop();
				
				if(!nodeStack.isEmpty()) // the tree has not been exhausted, search for more children of parent of nextNode  	
					nextNode = nodeStack.peek();
				else { 
					nextNode = pickUncolored(orderedVertices, colors); // pick a new node in the forest.
					nodeStack.push(nextNode);
					nextNode = nodeStack.peek();
				}
			}
			else { // now check the child picked.  
				if(colors[picked] == WHITE) { // if white, record a 
					nextNode = picked;
					timer++;
					start[nextNode] = timer;
					grayColor(nextNode, colors);
					nodeStack.push(nextNode);
				}
				else if(colors[picked] == GRAY) // if gray, record a cycle found.
					cycleCount++;
				// if black, do nothing
			}
		}
		
		int[] dataCollected = new int[rowSize + 1];
		for(int i = 0; i < rowSize; i++)
			dataCollected[i] = finishing[i];
		dataCollected[rowSize] = cycleCount;
		return dataCollected;
	}
	
	
	public static class ByNodeFinish implements Comparator<Integer>
	{
		private int[] finishingTimes;
		public ByNodeFinish(int[] finishing)
		{
			finishingTimes = new int[finishing.length];
			for(int i = 0; i < finishing.length; i++)
				finishingTimes[i] = finishing[i];
		}
		public int compare(Integer o1, Integer o2) 
		{
			return -Integer.compare(finishingTimes[o1], finishingTimes[o2]);
		}
		
	}
	private static void blackColor(int vertex, int[] colors)
	{
//		for(int i = 0; i < orderedVertices.length; i++) 
//			if(vertex == orderedVertices[i]) {
//				colors[i] = BLACK;
//				return;
//			}
		colors[vertex] = BLACK;	
	}
	
	
	private static void grayColor(int vertex, int[] colors)
	{
		colors[vertex] = GRAY;
	}
	
	public static Integer[] edgesOutgoingFrom(List<RowType> graphRows, int vertex)
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
	 * Return the vertex next in line given the vertex specified by the method.
	 */
	public static int pickNext(int vertex, int[] next, Integer[] outgoing)
	{
		if(outgoing.length == 0)
			return -1;
		if(next[vertex] == outgoing.length)
			return -1; // there are no more outgoing edges to pick at this time. 
		next[vertex]++;
		return outgoing[next[vertex]-1];
	}
	private static int pickUncolored(int[] vertices, int[] colors)
	{
		for(int i = 0; i < vertices.length; i++)
			if(colors[vertices[i]] == WHITE) 
				return vertices[i];
		return -1;
	}
	
	public static class StatisticsSet extends Statistics.StatisticsSet
	{
		public int cyclesDetected;
		
		public int cyclomaticNumber;
		
		public StatisticsSet(int numCycles, int cyclomaticNumber)
		{
			this.cyclesDetected = numCycles;
			this.cyclomaticNumber = cyclomaticNumber;
		}
		
		public StatisticsSet(Statistics.StatisticsSet otherSet)
		{
			if(otherSet.hasEdgeData) {
				noEdge = otherSet.noEdge;
				reaching = otherSet.reaching;
				follows = otherSet.follows;
			}
			if(otherSet.hasPathData) {
				covered = otherSet.covered;
				coveredData = otherSet.coveredData;
				eventInitialStatuses = otherSet.eventInitialStatuses;
			}
			
			if(otherSet.hasRemoveData) {
				edgeRemovedData = otherSet.edgeRemovedData;
				removed = otherSet.removed; 
			}
		}
		
		public void setCycleData(int numCycles, int cyclomaticNumber)
		{
			this.cyclesDetected = numCycles;
			this.cyclomaticNumber = cyclomaticNumber;
		}
		
		
	}
}
