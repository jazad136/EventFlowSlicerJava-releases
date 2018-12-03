/*	
 *  Copyright (c) 2009, 2018-@year@. The GUITAR group at the University of Maryland. Names of owners of this group may
 *  be obtained by sending an e-mail to atif@cs.umd.edu
 * 
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated 
 *  documentation files (the "Software"), to deal in the Software without restriction, including without 
 *  limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 *	the Software, and to permit persons to whom the Software is furnished to do so, subject to the following 
 *	conditions:
 * 
 *	The above copyright notice and this permission notice shall be included in all copies or substantial 
 *	portions of the Software.
 *
 *	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT 
 *	LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO 
 *	EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER 
 *	IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR 
 *	THE USE OR OTHER DEALINGS IN THE SOFTWARE. 
 */
package edu.unl.cse.efs.guitarplugin;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import edu.umd.cs.guitar.model.XMLHandler;
import edu.umd.cs.guitar.model.data.EFG;
import edu.umd.cs.guitar.model.data.GUIMap;
import edu.umd.cs.guitar.model.data.GUIStructure;
import edu.umd.cs.guitar.ripper.plugin.GraphConverter;
import edu.umd.cs.guitar.util.GUITARLog;
import edu.unl.cse.efs.view.EventFlowSlicerErrors;

/**
 * Entry point to convert GUI structure to Event Flow Graph supported by EFS. 
 * 
 * @author Jonathan Saddler
 * @version 1.0
 */
public class GUIStructure2EFGConverterEFS {

	@Option(name = "-g", usage = "GUI file", aliases = "--gui-file", required = true)
	public String GUI_FILE = "GUITAR-Default.GUI";

	@Option(name = "-e", usage = "EFG file ", aliases = "--efg-file", required = false)
	public String EFG_FILE = "GUITAR-Default.EFG";

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
			gui = (GUIStructure) xmlHandler.readObjFromFile(GUI_FILE, GUIStructure.class);
			Object graph = plugin.generate(gui);
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
