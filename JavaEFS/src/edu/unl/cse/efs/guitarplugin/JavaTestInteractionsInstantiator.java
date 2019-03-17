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
package edu.unl.cse.efs.guitarplugin;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import edu.umd.cs.guitar.model.GWindow;
import edu.umd.cs.guitar.model.JFCXComponent;
import edu.umd.cs.guitar.model.JFCXWindow;
import edu.umd.cs.guitar.ripper.plugin.GRipperAfter;
import edu.umd.cs.guitar.ripper.plugin.GRipperBefore;
import edu.unl.cse.efs.guitarplugin.JFCIDGeneratorEFS;
import edu.unl.cse.guitarext.HeadTable;
import edu.unl.cse.guitarext.JavaTestInteractions;
import edu.unl.cse.guitarext.ReplayInteraction;
import edu.unl.cse.guitarext.StateDump;

public class JavaTestInteractionsInstantiator implements GRipperBefore, GRipperAfter {
	
	public static ArrayList<JavaTestInteractions> testInteractions;
	public StateDump outputNamesFile;
	boolean writeNames;
	boolean writeWidgets;
	
	/**
	 * Preconditions: namesFile is not null and points to a valid file on the file system. 
	 * 				  format is not null and points to a valid format
	 * @param namesFile
	 * @param format
	 */
	public JavaTestInteractionsInstantiator(String namesFile, String... format)
	{
		testInteractions = new ArrayList<JavaTestInteractions>();
		outputNamesFile = new StateDump(namesFile);
		writeNames = true;
		if(format.length > 0) {
			if(format[0].equalsIgnoreCase("widgets")) 
				outputNamesFile.setPropertyWriteFormat(StateDump.PropertyWrite.WIDGET);
		}
	}
	
	public JavaTestInteractionsInstantiator()
	{
		testInteractions = new ArrayList<JavaTestInteractions>();
		writeNames = false;
	}
	/**
	 * Wait a few seconds before ripping 
	 */
	@Override
	public void beforeRipping() 
	{
		HeadTable.allInteractions = new ArrayList<>();
		JFCXComponent.nowRipping = true;
	}
	
	@Override
	public void afterRipping() 
	{
		JFCXComponent.nowRipping = false;
		if(writeNames) {
			try {JFCIDGeneratorEFS.outputNamesFile.writeWidgetPropertiesToFile();} 
			catch(IOException e) {
				throw new RuntimeException("Flowbehind: Failed to write names file to disk.");
			}
		}
	}
	
//	@Override
//	public void beforeRippingWindow(GWindow window) 
//	{
//		JavaTestInteractions newInteractions = getValidInteractions(window);
//		writeInteractions(newInteractions);
//		HeadTable.allInteractions.add(newInteractions);
//	}
	
	@SuppressWarnings("unused")
	private void writeInteractions(JavaTestInteractions interactions)
	{
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputNamesFile))) {
			for(ReplayInteraction ri : interactions) {
				bw.append(ri.toString());
				bw.newLine();
			}
		} catch(IOException e) {
			System.err.println("Flowbehind: Failed to write names file to disk.");
		}
	}
	
	public JavaTestInteractions getValidInteractions(GWindow myWindow)
	{
		JavaTestInteractions nextIModule;
		nextIModule = new JavaTestInteractions();
		nextIModule.scanWindowForInteractions(((JFCXWindow)myWindow).getWindow());
		testInteractions.add(nextIModule);
		return nextIModule;
	}

//	@Override
//	public void afterRippingComponnent(GObject component, GWindow window) 
//	{
//		if(component != null && writeNames) {
//			JFCXComponent javaComponent = (JFCXComponent)component;
//			ComponentType myCompType = javaComponent.extractProperties();
//			AttributesType myAtts  = myCompType.getAttributes();
//			outputNamesFile.addExtraAttributes(myAtts);
////			outputNamesFile.addExtraProperties(javaComponent.getIDProperties());
//			
//		}
//	}
}
