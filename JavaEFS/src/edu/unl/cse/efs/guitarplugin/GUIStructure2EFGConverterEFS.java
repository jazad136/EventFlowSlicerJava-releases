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

import java.io.File;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import edu.umd.cs.guitar.model.XMLHandler;
import edu.umd.cs.guitar.model.data.GUIStructure;
import edu.unl.cse.efs.tools.PathConformance;
import edu.unl.cse.efs.view.EventFlowSlicerErrors;

/**
 * Entry point to convert GUI structure to Event Flow Graph supported by EFS.
 *
 * @author Jonathan Saddler
 * @version 1.0
 */
public class GUIStructure2EFGConverterEFS {

	@Option(name = "-g", usage = "GUI file", aliases = "--gui-file", required = true)
	public String GUI_FILE;

	@Option(name = "-e", usage = "EFG file ", aliases = "--efg-file", required = false)
	public String EFG_FILE = "";

	@Option(name = "-o", usage = "EFG file output directory", aliases = "--output-directory", required = false)
	public String OUTPUT_DIRECTORY;

	@Option(name = "-s", usage = "Simplify EFG", aliases = "--simplify", required = false)
	public boolean SIMPLIFY = false;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		GUIStructure2EFGConverterEFS converter = new GUIStructure2EFGConverterEFS();
		CmdLineParser parser = new CmdLineParser(converter);

		try {
			parser.parseArgument(args);
			converter.execute();
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			System.err.println();
			System.err.println("Usage: java [JVM options] "
					+ GUIStructure2EFGConverterEFS.class.getName()
					+ " [converter options] \n");
			System.err.println("where [converter options] include:");
			System.err.println();
			parser.printUsage(System.err);
		}
	}

	/**
	 *
	 */
	public void execute() {
		XMLHandler xmlHandler = new XMLHandler();

		GUIStructure gui;
		try {
			EFSEFGConverter plugin = new EFSEFGConverter();
			if(SIMPLIFY)
				plugin.handleHoverEvents = false;
			gui = (GUIStructure) xmlHandler.readObjFromFile(GUI_FILE, GUIStructure.class);
			Object graph = plugin.generate(gui);
			if(EFG_FILE.isEmpty() && !OUTPUT_DIRECTORY.isEmpty()) {
				String newFileName = OUTPUT_DIRECTORY + File.separator;
				EFG_FILE = newFileName + PathConformance.parseApplicationName(GUI_FILE) + ".EFG";
			}
			xmlHandler.writeObjToFile(graph, EFG_FILE);

		} catch (Exception e) {
			EventFlowSlicerErrors.errorOut(e);
		}

		String bars = "===========================================";
		System.out.println(bars);
		System.out.println("\tInput GUI: \t" + GUI_FILE);
		System.out.println("\tOutput EFG: \t" + EFG_FILE);
		System.out.println(bars);
	}
}
