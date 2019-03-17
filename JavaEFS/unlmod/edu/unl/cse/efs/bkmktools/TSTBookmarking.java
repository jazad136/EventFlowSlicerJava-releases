package edu.unl.cse.efs.bkmktools;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import edu.umd.cs.guitar.model.XMLHandler;
import edu.umd.cs.guitar.model.data.AttributesType;
import edu.umd.cs.guitar.model.data.EFG;
import edu.umd.cs.guitar.model.data.EventType;
import edu.umd.cs.guitar.model.data.ObjectFactory;
import edu.umd.cs.guitar.model.data.PropertyType;
import edu.umd.cs.guitar.model.data.StepType;
import edu.umd.cs.guitar.model.data.TestCase;
import edu.unl.cse.efs.tools.PathConformance;

public class TSTBookmarking {
	
	
	private ObjectFactory fact;
	private XMLHandler handler;
	
	private EFG backingEFG;
	private TestCase backingTST;
	private List<EventType> allEvents;
	private List<StepType> allSteps;
	private static String NAME_VERSION_SEPARATOR = ":";
	private static char NAME_VERSION_SEPARATOR_CHAR = ':';
	
	public static void main(String[] args)
	{
		String efgFile = args[0];
		String tstFile = args[1];
		String outputDirectory = PathConformance.parseApplicationPath(tstFile);
		if(outputDirectory.equals("")) 
			outputDirectory = System.getProperty("user.dir") + File.separator;
		
		try {doBookmark(outputDirectory, efgFile, tstFile);}
		catch(FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		System.out.println("Done.");
	}
	
	public TSTBookmarking(String TSTFilename, String EFGFilename) throws FileNotFoundException
	{
		File EFGFile = new File(EFGFilename);
		if(!EFGFile.exists()) 
			throw new FileNotFoundException(EFGFilename);
		
		File TSTFile = new File(TSTFilename);
		if(!TSTFile.exists()) 
			throw new FileNotFoundException(TSTFilename);
		handler = new XMLHandler();
		fact = new ObjectFactory();
		backingEFG = (EFG)handler.readObjFromFile(EFGFile, EFG.class);	
		allEvents = backingEFG.getEvents().getEvent();
		backingTST = (TestCase)handler.readObjFromFile(TSTFile, TestCase.class);
		allSteps = backingTST.getStep();
		
	}
	
	public TSTBookmarking(TestCase testCase, EFG efg) throws FileNotFoundException
	{
		handler = new XMLHandler();
		fact = new ObjectFactory();
		if(efg == null 
		|| efg.getEvents() == null 
		|| efg.getEvents().getEvent() == null 
		|| efg.getEvents().getEvent().isEmpty())
			throw new IllegalArgumentException("A null EFG or EFG with a null or empty set of events was passed to TSTBookmarking constructor.");
		
		backingEFG = efg;
		if(testCase == null)
			throw new IllegalArgumentException("A null TestCase was passed to TSTBookmarking constructor.");
		
		allEvents = backingEFG.getEvents().getEvent();
		backingTST = testCase;
		allSteps = backingTST != null ? backingTST.getStep() : new LinkedList<StepType>();
	}
	
	
	public static class TSTUnBookmarking
	{
		private TestCase backingTST;
		private List<StepType> allSteps;
		private XMLHandler handler;
		private ObjectFactory fact;
		
		public TSTUnBookmarking(String TSTFilename) throws FileNotFoundException
		{
			File TSTFile = new File(TSTFilename);
			// test to see if file exists.
			if(!TSTFile.exists())
				throw new FileNotFoundException(TSTFile.getAbsolutePath());
			handler = new XMLHandler();
			fact = new ObjectFactory();
			backingTST = (TestCase)handler.readObjFromFile(TSTFile, TestCase.class);	
			allSteps = backingTST.getStep();
		}
		
		public TSTUnBookmarking(TestCase TSTObject) 
		{
			handler = new XMLHandler();
			fact = new ObjectFactory();
			backingTST = TSTObject;	
			allSteps = backingTST.getStep();
		}
		
		public TestCase getUnBookmarked()
		{
			ArrayList<StepType> newSteps = new ArrayList<StepType>();
			for(int i = 0; i < allSteps.size(); i++) {
				StepType oldStep = allSteps.get(i);
				String oldEventId = oldStep.getEventId();
				// assuming the event Id colon is already in place
				// if it's not, do nothing
				int colonPos;
				colonPos = oldEventId.indexOf(':');
				String newEventId;
				if(colonPos != -1) 
					newEventId = oldEventId.substring(0, colonPos);
				else
					newEventId = oldEventId;
				StepType newStep = fact.createStepType();
				newStep.setEventId(newEventId);
				newStep.setReachingStep(oldStep.isReachingStep());
				newStep.getParameter().addAll(oldStep.getParameter());
				if(oldStep.getOptional() != null)  {
					AttributesType newAT = fact.createAttributesType();
					for(PropertyType aPT : oldStep.getOptional().getProperty()) {
						PropertyType newPT = fact.createPropertyType();
						newPT.setName(aPT.getName());
						newPT.getValue().addAll(aPT.getValue());
						newAT.getProperty().add(newPT);
					}
					newStep.setOptional(newAT);
				}
				newSteps.add(newStep);
			}
			
			TestCase toReturn = fact.createTestCase();
			toReturn.getStep().addAll(newSteps);
			return toReturn;
		}	
	}
	public static void doUnBookmark(String outputDirectory, String inputTSTFilename)
	{
		
	}
	public static void doBookmark(String outputDirectory, String inputEFGFilename, String inputTSTFilename) throws FileNotFoundException
	{
		TSTBookmarking marker = new TSTBookmarking(inputTSTFilename, inputEFGFilename);
		TestCase out = marker.getBookmarkedViaBookmarkedEFG();
		XMLHandler handler = new XMLHandler();
		String newFileName = PathConformance.parseApplicationName(inputTSTFilename) + "_BKMK.tst";
		newFileName = outputDirectory + newFileName;
		System.out.println("Writing file to \n\"" + newFileName + "\"");
		handler.writeObjToFile(out, newFileName);
	}
	
	/**
	 * Return a test case object containing information useful to actually replay or visualize
	 * the test case, using the bookmarked EFG provided
	 * @param inputTestCase
	 * @param inputEFGFile
	 */
	public static void getBookmarkedViaBookmarkedEFGInSitu(TestCase testCase, EFG bookmarkedEFG) throws FileNotFoundException
	{
		TSTBookmarking marker = new TSTBookmarking(testCase, bookmarkedEFG);
	}
	
	/*
	 * Incomplete
	 * @return
	 */
	public TestCase getBookmarkedViaGUIAndEFG()
	{
		return fact.createTestCase();
	}	
	
	public TestCase getBookmarkedViaBookmarkedEFG()
	{
		ArrayList<StepType> newSteps = new ArrayList<StepType>(); 
		
		for(int i = 0; i < allSteps.size(); i++) {
			StepType oldStep = allSteps.get(i);
			StepType newStep = appendStepInfoFromEvent(oldStep);
			newSteps.add(newStep);
		}
	
		TestCase toReturn = fact.createTestCase();
		toReturn.setStep(newSteps);
		return toReturn;
	}
	
	/**
	 * Find the event in the events list with the matching event id. 
	 * @param eventId
	 * @return
	 */
	public int findEvent(String targetEventId)
	{
		for(int i = 0; i < allEvents.size(); i++) {
			String eName = allEvents.get(i).getEventId();
			eName = unbookmarkedEventId(eName);
			if(eName.equals(targetEventId))
				return i;
		}
		return -1;
	}
	
	private String unbookmarkedEventId(String rawEName)
	{
		if(rawEName != null && rawEName.contains(NAME_VERSION_SEPARATOR)) {
			int nsIndex = rawEName.indexOf(NAME_VERSION_SEPARATOR_CHAR);
			rawEName = rawEName.substring(0, nsIndex);
		}
		return rawEName;
	}
	
	
	private StepType appendStepInfoFromEvent(StepType oldStep)
	{	
		int eventNum = findEvent(oldStep.getEventId());
		String newName;
		if(eventNum != -1) 
			newName = allEvents.get(eventNum).getEventId();
		
		else
			newName = oldStep.getEventId();
		StepType newStep = fact.createStepType();
		newStep.setEventId(newName);
		newStep.setReachingStep(oldStep.isReachingStep());
		newStep.setOptional(oldStep.getOptional());
		newStep.setParameter(oldStep.getParameter());
		newStep.setGUIStructure(oldStep.getGUIStructure());
		return newStep;
	}
}
