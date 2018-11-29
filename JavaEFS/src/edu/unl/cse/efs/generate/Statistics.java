package edu.unl.cse.efs.generate;
import java.util.ArrayList;
import java.util.List;

import edu.umd.cs.guitar.model.data.AttributesType;
import edu.umd.cs.guitar.model.data.EFG;
import edu.umd.cs.guitar.model.data.EventGraphType;
import edu.umd.cs.guitar.model.data.EventType;
import edu.umd.cs.guitar.model.data.EventsType;
import edu.umd.cs.guitar.model.data.ObjectFactory;
import edu.umd.cs.guitar.model.data.ParameterListType;
import edu.umd.cs.guitar.model.data.PropertyType;
import edu.umd.cs.guitar.model.data.RowType;
import edu.umd.cs.guitar.model.data.StepType;
import edu.umd.cs.guitar.model.data.TestCase;

public class Statistics {
	
	public static int allNoEdges;
	public static int allReaching;
	public static int allFollows;
	public static int allRemovedEdges;
	public static ArrayList<StatisticsSet> collected;
	public static int collectedStats;
	static{
		allNoEdges = allReaching = allFollows =  allRemovedEdges = 0;
		collected = new ArrayList<StatisticsSet>();
		collectedStats = 0;
	}
	
	
	
	public static int countNumEdgesInGraph(EFG graph, boolean addToLast)
	{
		EventGraphType gt = graph.getEventGraph();
		int rowSize = gt.getRow().size();
		if(rowSize == 0)
			return 0;
		int colSize = gt.getRow().get(0).getE().size();
	
		int edgeCount, reachingCount, noEdgeCount;
		edgeCount = reachingCount = noEdgeCount = 0;
		for(int i = 0; i < rowSize; i++) 
			for(int j = 0; j < colSize; j++) {
				switch(edgeValue(gt, i, j)) {
				case NONE: 		noEdgeCount++; 
		break;	case FOLLOWS: 	reachingCount++; edgeCount++;
		break;	default:		edgeCount++;
				}
			}
		
		collect(noEdgeCount, reachingCount, edgeCount, addToLast);
		return edgeCount;
	}
	
	private static void collect(int noEdge, int reach, int all, boolean addToLast)
	{
		allNoEdges += noEdge;
		allReaching += reach;
		allFollows += all;
		
		if(addToLast) {
			if(collectedStats == 0) {
				collected.add(new StatisticsSet(noEdge, reach, all));
				collectedStats++;
			}
			else 
				collected.get(collectedStats-1).setEdgeData(noEdge, reach, all);
		}
		else {
			collected.add(new StatisticsSet(noEdge, reach, all));
			collectedStats++;
		}
	}

	public static int pathEdgesCovered(List<TestCase> testCases, EFG baseEFG, boolean addToLast)
	{
		List<EventType> allEvents = baseEFG.getEvents().getEvent();
		boolean[] initialStatus = new boolean[allEvents.size()];
		
		List<RowType> rows = baseEFG.getEventGraph().getRow();
		int numRows = rows.size();
		if(numRows < 0)
			return 0; // no edges can be covered if there are no edges.
		
		// initialize array
		boolean[][] covered = new boolean[numRows][]; // we are assuming that
		for(int i = 0; i < rows.size(); i++) 
			covered[i] = new boolean[rows.get(i).getE().size()];
		
		int coveredCount = 0;
		for(TestCase testCase : testCases) {
			if(testCase.getStep().size() == 1) {
				int e1 = -1;
				for(int j = 0; j < allEvents.size(); j++) 
					if(testCase.getStep().get(0).getEventId().equals(allEvents.get(j).getEventId()))
						e1 = j;
				if(e1 == -1)
					throw new RuntimeException("Bad indexing: TestCase Step with eId: " + testCase.getStep().get(0).getEventId() + " cannot be found in EFG.");
				initialStatus[e1] = true; // we can mark this as an initial node.
				// since there is no second event, 
				// there are no edges in this test case that we can say were covered in the EFG.
				// so we're done. 
			}
			else {
				for(int i = 0; i < testCase.getStep().size() - 1; i++) {
					StepType s1 = testCase.getStep().get(i);
					StepType s2 = testCase.getStep().get(i+1);
					int e1, e2;
					e1 = e2 = -1;
					for(int j = 0; j < allEvents.size(); j++) 
						if(s1.getEventId().equals(allEvents.get(j).getEventId())) {
							e1 = j;
							break;
						}
					for(int j = 0; j < allEvents.size(); j++)  {
						if(s2.getEventId().equals(allEvents.get(j).getEventId())) {
							e2 = j;
							break;
						}
					}
					if(e1 == -1)
						throw new RuntimeException("Bad indexing: TestCase Step with eId: " + s1.getEventId() + " cannot be found in EFG.");
					if(e2 == -1)
						throw new RuntimeException("Bad indexing: TestCase Step with eId: " + s2.getEventId() + " cannot be found in EFG.");
					
					if(i == 0)
						initialStatus[e1] = true;
					
					if(covered[e1][e2] == false) {
						coveredCount++;
						covered[e1][e2] = true;
					}
					// there is an edge between the every test case step and the next if it exists.
					// indicate this step by marking it in the covered array.
				}
			}
		}
		
		collectCovered(coveredCount, covered, initialStatus, addToLast);
		return coveredCount;		
	}
	
	public static List<EventType> deepCopy(List<EventType> oldEventTypes, boolean... newInitialValues)
	{
		ObjectFactory fact = new ObjectFactory();
		ArrayList<EventType> toReturn = new ArrayList<EventType>();
		boolean setInitials = newInitialValues.length > 0;
		for(int i = 0; i < oldEventTypes.size(); i++) {
			EventType oldEvent = oldEventTypes.get(i);
			EventType newEvent = fact.createEventType();
			newEvent.setEventId(oldEvent.getEventId());
			newEvent.setWidgetId(oldEvent.getWidgetId());
			newEvent.setType(oldEvent.getType());
			newEvent.setAction(oldEvent.getAction());
			for(ParameterListType plt : oldEvent.getParameterList()) {
				ParameterListType newPLT = fact.createParameterListType();
				newPLT.getParameter().addAll(plt.getParameter());
				newEvent.getParameterList().add(newPLT);
			}
			newEvent.setParameterList(new ArrayList<ParameterListType>(oldEvent.getParameterList()));
			newEvent.setName(oldEvent.getName());
			if(oldEvent.getOptional() != null)  {
				AttributesType newAT = fact.createAttributesType();
				for(PropertyType aPT : oldEvent.getOptional().getProperty()) {
					PropertyType newPT = fact.createPropertyType();
					newPT.setName(aPT.getName());
					newPT.getValue().addAll(aPT.getValue());
					newAT.getProperty().add(newPT);
				}
				newEvent.setOptional(newAT);
			}
			
			if(setInitials) 
				newEvent.setInitial(newInitialValues[i]); 
			else
				newEvent.setInitial(oldEvent.isInitial());
			toReturn.add(newEvent);
		}
		return toReturn;	
	}
	public static void collectCovered(int coveredCount, boolean[][] covered, boolean[] initialStatus, boolean addToLast)
	{
		if(addToLast) {
			if(collectedStats == 0) {
				collected.add(new StatisticsSet(coveredCount, covered, initialStatus));
				collectedStats++;
			}
			else 
				collected.get(collectedStats-1).setCoveredPathData(coveredCount, covered,  initialStatus);
		}
		else {
			collected.add(new StatisticsSet(coveredCount, covered, initialStatus));
			collectedStats++;
		}
	}
	
	/**
	 * Collect the number of edges that were removed from the EFG.
	 * @param removedEdges
	 */
	public static int countNumEdgesRemoved(EFG efg1, EFG efg2, boolean addToLast)
	{
		List<RowType> rows1 = efg1.getEventGraph().getRow();
		List<RowType> rows2 = efg2.getEventGraph().getRow();
		boolean[][] removedEdges = new boolean[rows1.size()][];
		boolean[][] allRemoved = new boolean[removedEdges.length][];
		int numRemoved = 0;
		for(int i = 0; i < rows1.size(); i++) {
			removedEdges[i] = new boolean[rows1.get(i).getE().size()];
			allRemoved[i] = new boolean[removedEdges[i].length];
			for(int j = 0; j < removedEdges[i].length; j++) {
				List<Integer> col1 = rows1.get(i).getE();
				List<Integer> col2 = rows2.get(i).getE();
				if(col1.get(j) != 0) 
					if(col2.get(j) == 0) {
						allRemoved[i][j] = true; // this edge was removed
						numRemoved++;
					}
			}
		}
		
		allRemovedEdges += numRemoved;
		if(addToLast) {
			if(collectedStats == 0) {
				collected.add(new StatisticsSet(removedEdges, numRemoved));
				collectedStats++;
			}
			else 
				collected.get(collectedStats-1).setRemovedData(removedEdges, numRemoved);
		}
		else {
			collected.add(new StatisticsSet(removedEdges, numRemoved));
			collectedStats++;
		}		
		return numRemoved;
	}
	
	/**
	 * Use the parameters to construct an EFG consisting only of the edges specified
	 * in the array covered. The array covered should be a nxn matrix of the
	 * same height and length as the events graph type stored in baseEFG. 
	 * @return
	 */
	public static EFG constructPathCoverEFG(EFG baseEFG, boolean[][] covered, boolean[] initialStatuses)
	{
		ObjectFactory fact = new ObjectFactory();
		EFG pathCoverEFG = fact.createEFG();
		List<RowType> oldRows = baseEFG.getEventGraph().getRow();
		EventGraphType egt = fact.createEventGraphType();
		for(int i = 0; i < covered.length; i++) {
			RowType newRow = fact.createRowType();
			ArrayList<Integer> rowInts = new ArrayList<Integer>();
			for(int j = 0; j < covered[i].length; j++)
				if(covered[i][j]) 	rowInts.add(oldRows.get(i).getE().get(j));
				else 				rowInts.add(0);
			newRow.getE().addAll(rowInts);
			egt.getRow().add(newRow);
		}
		EventsType evt = fact.createEventsType();
		List<EventType> newEvents = deepCopy(baseEFG.getEvents().getEvent(), initialStatuses);
		evt.setEvent(newEvents);
//		for(int i = 0; i < baseEFG.getEvents().getEvent().size(); i++) 
//			evt.getEvent().add(baseEFG.getEvents().getEvent().get(i));
		
		pathCoverEFG.setEventGraph(egt);
		pathCoverEFG.setEvents(evt);
		return pathCoverEFG;
	}
	
	private static FollowsType edgeValue(EventGraphType gt, int row, int col)
	{
		int edgeValue = gt.getRow().get(row).getE().get(col);
		switch(edgeValue) {
		case 0 : return FollowsType.NONE;
		case 1 : return FollowsType.FOLLOWS;
		case 2 : return FollowsType.REACHING;
		}
		return FollowsType.FOLLOWS; // value should be 1
	}
	
	public enum FollowsType {
		NONE(0), FOLLOWS(1), REACHING(2);
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
	
	public static class StatisticsSet
	{
		public int noEdge;
		public int reaching;
		public int follows;
		public int removed;
		public int covered;
		public boolean[][] edgeRemovedData;
		public boolean[][] coveredData;
		public boolean[] eventInitialStatuses;
		public boolean hasEdgeData, hasRemoveData, hasPathData;
		
		protected StatisticsSet()
		{
			
		}
		
		public StatisticsSet(int no, int reach, int follow)
		{
			noEdge = no;
			reaching = reach;
			follows = follow;
			hasEdgeData = true;
		}
		
		public StatisticsSet(boolean[][] removedData, int numRemoved) 
		{
			this.edgeRemovedData = removedData;
			hasRemoveData = true;
		}
		
		public StatisticsSet(int edgesCovered, boolean[][] coveredData, boolean[] initialStatus)
		{
			this.covered = edgesCovered;
			this.coveredData = coveredData;
			this.eventInitialStatuses = initialStatus;
			hasPathData = true;
		}
		
		
		public void setEdgeData(int no, int reach, int follow)
		{
			noEdge = no;
			reaching = reach;
			follows = follow;
			hasEdgeData = true;
		}
		public void setRemovedData(boolean[][] removedData, int numRemoved)
		{
			this.edgeRemovedData = removedData;
			this.removed = numRemoved;
			hasRemoveData = true;
		}
		
		public void setCoveredPathData(int edgesCovered, boolean[][] coveredData, boolean[] initialStatus)
		{
			this.covered = edgesCovered;
			this.coveredData = coveredData;
			this.eventInitialStatuses = initialStatus;
			hasRemoveData = true;
		}
	}
	
	
}
