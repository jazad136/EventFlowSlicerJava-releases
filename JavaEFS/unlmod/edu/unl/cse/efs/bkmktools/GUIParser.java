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

package edu.unl.cse.efs.bkmktools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import edu.umd.cs.guitar.model.GUITARConstants;
import edu.umd.cs.guitar.model.data.AttributesType;
import edu.umd.cs.guitar.model.data.ObjectFactory;
import edu.umd.cs.guitar.model.data.PropertyType;
import edu.umd.cs.guitar.model.wrapper.ComponentTypeWrapper;
import edu.umd.cs.guitar.model.wrapper.GUIStructureWrapper;

/**
 * Source for the GUIParser class. The GUIParser is a class that will assist
 * in reading GUIString objects as raw text from GUI file objects. Users of the class
 * can use the findWidgetWithID method to find a widget with a specific ID number
 * in the GUI file, and return a GUIStrings object containing the lines that ought to
 * be associated with that string (the user should carefully note the documentation for that
 * method) 
 * @author jsaddle
 *
 */
public class GUIParser {
	
	public static void main(String[] args)
	{
		GUIStrings retStrings = null;
		try {
			GUIParser myParser = new GUIParser(new File(args[0]));
			retStrings = myParser.findWidgetWithID(args[1]);
		} catch(IOException | SecurityException e) {
			System.out.println(e);
			retStrings = new GUIStrings();
		}
		System.out.println(retStrings);
	}
	
	private ArrayList<String> allLines;
	private GUIStructureWrapper guiXML;
	private final ObjectFactory fact;
	
	public GUIParser(File fileOfTypeGUI) throws IOException, FileNotFoundException
	{
		if(!fileOfTypeGUI.exists()) 
			throw new FileNotFoundException(fileOfTypeGUI.getPath());
		fact = new ObjectFactory();
		allLines = new ArrayList<String>(Files.readAllLines(Paths.get(fileOfTypeGUI.toURI()), Charset.defaultCharset()));
	}
	
	public GUIParser(GUIStructureWrapper guiXMLWithTree)
	{
		fact = new ObjectFactory();
		guiXML = guiXMLWithTree;
	}
	
	public GUIStrings lookupViaJAXB(String widgetId) 
	{
		AttributesType signature = fact.createAttributesType();
		PropertyType idProp = fact.createPropertyType();
		idProp.setName(GUITARConstants.ID_TAG_NAME);
		idProp.getValue().add(widgetId);
		signature.getProperty().add(idProp);
		ComponentTypeWrapper result = guiXML.getComponentBySignaturePreserveTree(signature);
		if(result == null)
			throw new RuntimeException("Lookup failed");
		
		return new GUIStrings(result);
	}
	
	/** 
	 *  Note: This method can be used with either of two widget types that are used
	 *  to derive GUI objects via the ripper referenced and designed by Amanda Swearngin
	 *  for her master's thesis: the extended widget containing aprox (80 lines of text)
	 *  and the smaller widget version (conaining approx 16 lines). 
	 *  
	 *  The user must take care to check the length (size()) of the GUIStrings object 
	 *  returned to detect the version before pulling strings using the getter methods
	 *  of the instance. 
	 *  
	 *  If no widget is found matching the specified widgetId, an object of 0 lines
	 *  will be the object returned, having been marked invalid (object.valid == false).
	 *  
	 *  Preconditions: 	This class was properly initialized with a non
	 *  				the file specified should not contain more than 2,147,483,648 lines (that's 2^31 the maximum size of an arraylist before it gets buggy)
	 *  Postconditions: 
	 */
	public GUIStrings findWidgetWithID(String widgetNumberName)
	{
		for(int i = 0; i < allLines.size(); i++) {
			if(allLines.get(i).contains(widgetNumberName)) {
				int attLine = i - 3; // should contain the attributes tag
				int finLine = i+GUIStrings.MAX_READ-3;
				List<String> sublist = allLines.subList(attLine, finLine);
				return new GUIStrings(sublist);
			}
		}
		return new GUIStrings();
	}
}
