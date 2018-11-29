package edu.unl.cse.guitarext;
import java.io.File;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.bind.Unmarshaller;

import edu.umd.cs.guitar.model.data.*;
public class GUITARDataFiles {
	public static class WrongFileTypeException extends IllegalArgumentException
	{
		public final String expected, read;
		public WrongFileTypeException(String fileTypeExpected, String fileTypeRead)
		{
			expected = fileTypeExpected;
			read = fileTypeRead;
		}
		public String getMessage()
		{
			return "Expected: " + expected + ".\n Read: " + read;
		}
	}
	public static GUIStructure getGUIFile(File fromFile) throws WrongFileTypeException, JAXBException
	{

		JAXBContext context = JAXBContext.newInstance(GUIStructure.class);
		Unmarshaller um = context.createUnmarshaller();
		Object myFile = JAXBIntrospector.getValue(um.unmarshal(fromFile));
		if (!(myFile instanceof GUIStructure))
			throw new WrongFileTypeException("GUIStructure", myFile.getClass().getSimpleName());
		return (GUIStructure)myFile;

	}
	public static EFG getEFGFile(File fromFile) throws WrongFileTypeException, JAXBException
	{
		JAXBContext context = JAXBContext.newInstance(EFG.class);
		Unmarshaller um = context.createUnmarshaller();
		Object myFile = JAXBIntrospector.getValue(um.unmarshal(fromFile));
		if (!(myFile instanceof EFG))
			throw new WrongFileTypeException("GUIStructure", myFile.getClass().getSimpleName());
		return (EFG)myFile;
	}

	public static TestCase getTestCaseFile(File fromFile) throws WrongFileTypeException, JAXBException
	{
		JAXBContext context = JAXBContext.newInstance(TestCase.class);
		Unmarshaller um = context.createUnmarshaller();
		Object myFile = JAXBIntrospector.getValue(um.unmarshal(fromFile));
		if (!(myFile instanceof TestCase))
			throw new WrongFileTypeException("GUIStructure", myFile.getClass().getSimpleName());
		return (TestCase)myFile;
	}
	public static TaskList getRulesFile(File fromFile) throws WrongFileTypeException, JAXBException
	{
		JAXBContext context = JAXBContext.newInstance(EFG.class);
		Unmarshaller um = context.createUnmarshaller();
		Object myFile = JAXBIntrospector.getValue(um.unmarshal(fromFile));
		if (!(myFile instanceof EFG))
			throw new WrongFileTypeException("GUIStructure", myFile.getClass().getSimpleName());
		return (TaskList)myFile;
	}
}
