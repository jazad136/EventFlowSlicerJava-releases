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

import java.util.ArrayList;
import java.util.List;

import edu.umd.cs.guitar.model.GUITARConstants;
import edu.umd.cs.guitar.model.data.AttributesType;
import edu.umd.cs.guitar.model.wrapper.ComponentTypeWrapper;

/**
 * Source for the GUIStrings class. The GUIStrings class serves as a temporary
 * holder for strings ripped out of GUI files. Each line of this arraylist
 * should contain attributes of a widget specified at predetermined lines within the 
 * input. If the constructor detects that the input to the constructor does not
 * contain any strings, the constructor will set the valid bit of this object
 * to false, as none of the methods in this class will work without the proper number of strings
 * available at each line we expect it to be found. An exception type is defined
 * here to account for such instances. 
 * 
 * This class is currently heavily dependent on line numbers. The user must be careful to ensure that the input to the constructor adheres to specific restrictions
 * on where it expects to find certain lines. 
 * 
 * The ID is typically just after the header, 0 line down from the line just after it. 
 * The Title is typically 4 lines down from the header
 * The Class 8 lines down
 * The Type 12 lines down
 * The ReplayableAction List 24 Lines down. 
 * The Description 28 lines down + a few more given the extended length of the replayable action list
 * and the tooltip 60 lines down + a few more.
 * 
 * @author Jonathan Saddler
 * @version July 2015
 */
public class GUIStrings extends ArrayList<String>
{
	public final boolean valid;
	public final boolean jaxbDerived;
	
	/**
	 * This is the maximum amount of line space allocated for the header of what we're reading. We expect to see a line containing "&lt;Attributes&gt;" at the head
	 * of the list
	 */
	public static final int H = 1;
	/**
	 * This is the maximum amount of replayable actions that GUIStrings was designed
	 * to read from GUI File string.
	 */
	public static final int MAX_REP_ACTS = 6;
	
	/**
	 * This is the maximum amount of CTHEventIDs that GUIStrings was designed to read
	 * from a GUI file string. 
	 */
	public static final int MAX_CTHEIDS = 3;
	/**
	 * This class promises not to read more than this many lines the input list of strings provided to the constructor. 
	 */
	public static final int MAX_READ = 70 + MAX_REP_ACTS;
	
	public int IDLine;
	public int TitleLine; 
	public int ClassLine;
	public int TypeLine;
	public int CTHEIDLine;
	public int RActLine;
	public int DescriptionLine;
	public int ToolTipLine;
	public ComponentTypeWrapper jaxbComp;
	public static int JUMP_TO_VALUE = 2; 
	
	public GUIStrings()
	{
		this(new ArrayList<String>());
	}
	
	/**
	 * Constructor for GUI Strings.<br><br> 
	 * Preconditions: rawInputData should begin with the string "&lt;Attributes&gt;" at the top of the list, and be followed by 
	 * 					as many lines as the user expects to retrieve from the methods of this class: at least MAX_READ lines if we expect to be able
	 * 					to parse data from every method in this class without a MissingLineException being thrown. <br><br>
	 * 					See the class description for information about where this class expects to find lines.<br><br> 
	 * Postconditions: Line number information is publicly available at
	 * 					runtime as data stored in int variables IDLine, TitleLine, ClassLine ... etc.
	 * 					
	 * @param rawInputData
	 */
	public GUIStrings(List<String> rawInputData)
	{
		super(rawInputData);
		if(rawInputData.isEmpty() || rawInputData.size() < 60) 
			valid = false;
		
		else {
			valid = true;			
			IDLine = 0 +H;
			TitleLine = 4 +H;
			ClassLine = 8 +H;
			TypeLine = 12 +H;
			CTHEIDLine = 20 +H;
			int additCTHEIDLen = CTHEIDLength()-1;
			RActLine = 24 + additCTHEIDLen + H;
			int additRActListLen = RActLength()-1; 
			DescriptionLine = 28 + H + additCTHEIDLen + additRActListLen; 
			ToolTipLine = 60 + H + additCTHEIDLen + additRActListLen;
		}
		jaxbDerived = false;
	}
	
	public GUIStrings(ComponentTypeWrapper xmlInputData)
	{
		this.jaxbComp = xmlInputData;
		if(xmlInputData == null)
			valid = false;
		else
			valid = true;
		jaxbDerived = true;
	}
	
	/**
	 * Count how many CTH Event ID actions are listed in the CTH Event ID element. 
	 */
	public int CTHEIDLength()
	{
		int valuePos1 = CTHEIDLine + JUMP_TO_VALUE;
		int toReturn = 1;
		String possValue = get(valuePos1+1);
		while(possValue.contains("Value") && toReturn < MAX_CTHEIDS) {
			toReturn++;
			possValue = get(valuePos1+toReturn);
		}
		return toReturn;
	}
	/**
	 * Count how many replayable actions are listed in the ReplayableAction element. 
	 */
	public int RActLength()
	{
		int valuePos1 = RActLine + JUMP_TO_VALUE;
		int toReturn = 1;
		String possValue = get(valuePos1+1);
		while(possValue.contains("Value") && toReturn < MAX_REP_ACTS) {
			toReturn++;
			possValue = get(valuePos1+toReturn);
		}
		return toReturn;
	}
	
	
	public static String dataInValueTag(String raw)
	{
		String toReturn;
		toReturn = raw.trim();
		toReturn = toReturn.replace("<Value>", "");
		toReturn = toReturn.replace("</Value>", "");
		return toReturn;
	}
	
	
	/**
	 * Retrieve the string encapsulated in the "Title" element stored within this object (or the empty string if there is
	 * no such element stored here)  
	 * @return
	 */
	public String titleString()
	{
		if(jaxbDerived) {
			String result = jaxbComp.getFirstValueByName(GUITARConstants.TITLE_TAG_NAME);
			if(result == null) return "";
			return result;
		}
		if(TitleLine + JUMP_TO_VALUE >= size())
			throw new MissingLineException();
		String titleVal = get(TitleLine + JUMP_TO_VALUE);
		titleVal = dataInValueTag(titleVal);
		return titleVal;
		
	}
	/**
	 * Retrieve the string encapsulated in the "Class" element stored within this object (or the empty string if there is
	 * no such element stored here)  
	 * @return
	 */
	public String classString()
	{
		if(jaxbDerived) {
			String result = jaxbComp.getFirstValueByName(GUITARConstants.CLASS_TAG_NAME);
			if(result == null) return "";
			return result;
		}
		if(ClassLine + JUMP_TO_VALUE >= size())
			throw new MissingLineException();
		String classVal = get(ClassLine + JUMP_TO_VALUE);
		classVal = dataInValueTag(classVal);
		return classVal;
	}
	
	/**
	 * Retrieve the string encapsulated in the "Type" element stored within this object (or the empty string if there is
	 * no such element stored here)  
	 * @return
	 */
	public String typeString()
	{
		if(jaxbDerived) {
			String result = jaxbComp.getFirstValueByName(GUITARConstants.TYPE_TAG_NAME);
			if(result == null) return "";
			return result;
		}
		if(TypeLine + JUMP_TO_VALUE >= size())
			throw new MissingLineException();
		String typeVal = get(TypeLine + JUMP_TO_VALUE);
		typeVal = dataInValueTag(typeVal);
		return typeVal;
	}
	
	public String[] cthEventIDStrings()
	{
		if(jaxbDerived) {
			List<String> allValues = jaxbComp.getValueListByName(GUITARConstants.CTH_EVENT_ID_NAME);
			if(allValues == null) return new String[]{""};
			return allValues.toArray(new String[0]);
		}
		if(CTHEIDLine + JUMP_TO_VALUE >= size())
			throw new MissingLineException();
		String[] eidArray = new String[RActLength()];
		
		for(int i = 0; i < eidArray.length; i++) {
			eidArray[i] = get(CTHEIDLine + JUMP_TO_VALUE + i);
			eidArray[i] = dataInValueTag(eidArray[i]);
		}
		
		return eidArray;
	}
	/**
	 * Retrieve the string encapsulated in the "ReplayableAction" element stored within this object (or the empty string if there is
	 * no such element stored here)  
	 * @return
	 */
	public String[] ractStrings()
	{
		if(jaxbDerived) {
			List<String> allValues = jaxbComp.getValueListByName(GUITARConstants.EVENT_TAG_NAME);
			if(allValues == null) return new String[]{""};
			return allValues.toArray(new String[0]);
		}
		if(RActLine + JUMP_TO_VALUE >= size())
			throw new MissingLineException();
		String[] ractArray = new String[RActLength()];
		for(int i = 0; i < ractArray.length; i++) {
			ractArray[i] = get(RActLine + JUMP_TO_VALUE + i);
			ractArray[i] = dataInValueTag(ractArray[i]);
		}
		return ractArray;
	}
	
	/**
	 * Retrieve the string encapsulated in the "Description" element stored within this object (or the empty string if there is
	 * no such element stored here)  
	 * @return
	 */
	public String descString()
	{
		if(jaxbDerived) {
			String result = jaxbComp.getFirstValueByName(GUITARConstants.DESCRIPTION_TAG_NAME);
			if(result == null) return "";
			return result;
		}
		if(DescriptionLine + JUMP_TO_VALUE >= size())
			throw new MissingLineException();
		String descVal = get(DescriptionLine + JUMP_TO_VALUE);
		descVal = dataInValueTag(descVal);
		return descVal;
	}
	

	/**
	 * Retrieve the string encapsulated in the "ToolTipText" element stored within this object (or the empty string if there is
	 * no such element stored here)  
	 * @return
	 */
	public String tooltipString()
	{
		if(jaxbDerived) {
			String result = jaxbComp.getFirstValueByName(GUITARConstants.TOOLTIPTEXT_TAG_NAME);
			if(result == null) return "";
			return result;
		}
		if(ToolTipLine + JUMP_TO_VALUE >= size())
			throw new MissingLineException();
		String toolVal = get(ToolTipLine + JUMP_TO_VALUE);
		toolVal = dataInValueTag(toolVal);
		return toolVal;
	}
	
	/**
	 * Retrieve a string containing all the Strings stored in this element. 
	 */
	public String toString()
	{
		String toReturn = "";
		
		if(jaxbDerived) {
			String[] signProps = new String[]{
				GUITARConstants.TITLE_TAG_NAME,
				GUITARConstants.CLASS_TAG_NAME,
				GUITARConstants.TYPE_TAG_NAME,
				GUITARConstants.EVENT_TAG_NAME,
				GUITARConstants.DESCRIPTION_TAG_NAME,
				GUITARConstants.TOOLTIPTEXT_TAG_NAME
			};
			AttributesType allAtts = jaxbComp.getSubAttributes(java.util.Arrays.asList(signProps));
			
			for(int i = 0; i < allAtts.getProperty().size(); i++) { 
				String name = allAtts.getProperty().get(i).getName();
				String value = allAtts.getProperty().get(i).getValue().get(0);
				for(int j = 0; i < allAtts.getProperty().get(i).getValue().size(); j++)
					value += "\n" + allAtts.getProperty().get(i).getValue().get(j);
				toReturn += i + name + ": " + value + "\n";
			}
			return toReturn;
		}
		for(int i = 0; i < size(); i++) {
			if(i == IDLine) toReturn += "\n";
			else if(i == TitleLine)	toReturn += "\n";
			else if(i == ClassLine)	toReturn += "\n";
			else if(i == TypeLine) toReturn += "\n";
			else if(i == RActLine) toReturn += "\n";
			else if(i == DescriptionLine) toReturn += "\n";
			else if(i == ToolTipLine) toReturn += "\n";
			toReturn += i + ": " + get(i).trim() + "\n";
		}
		return toReturn;
	}
	
	/**
	 * Source for the StringNotAvailableException. This class was designed
	 * to report to the caller of a method of GUIStrings when a certain attribute
	 * cannot be returned as it cannot be retrieved from this list. The attribute
	 * may exist at a line that is not accessible, or the method cannot perform
	 * necessary operations due to malformed or missing lines. 
	 * @author jsaddle
	 *
	 */
	public class MissingLineException extends RuntimeException
	{
		public MissingLineException()
		{
			
		}
	}
	
	
}
