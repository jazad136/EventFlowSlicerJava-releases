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
package edu.unl.cse.guitarext;
import java.io.File;


import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.bind.Unmarshaller;

import edu.umd.cs.guitar.model.data.*;
/**
 *
 * @author Jonathan A. Saddler
 *
 */
public class GUITARDataFiles {
	@SuppressWarnings("serial")
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
