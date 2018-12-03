/*  
 *  Copyright (c) 2009-@year@. The GUITAR group at the University of Maryland. Names of owners of this group may
 *  be obtained by sending an e-mail to atif@cs.umd.edu
 * 
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated 
 *  documentation files (the "Software"), to deal in the Software without restriction, including without 
 *  limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 *  the Software, and to permit persons to whom the Software is furnished to do so, subject to the following 
 *  conditions:
 * 
 *  The above copyright notice and this permission notice shall be included in all copies or substantial 
 *  portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT 
 *  LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO 
 *  EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER 
 *  IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR 
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE. 
 */
package edu.umd.cs.guitar.graph.converter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.List;

import edu.umd.cs.guitar.model.XMLHandler;
import edu.umd.cs.guitar.model.data.EFG;
import edu.umd.cs.guitar.model.data.EventType;
import edu.umd.cs.guitar.model.data.RowType;

/**
 * @author <a href="mailto:baonn@cs.umd.edu"> Bao N. Nguyen </a>
 * 
 */
public class EFG2GraphvizFixString {

	private static String OUT_DOT_FILE;
	private static String IN_EFG_FILE;
	public static final String EVENT_ID_SPLITTER = "_";
	public static final char EVENT_ID_SPLITTER_CHAR = '_';
	public static final String NAME_VERSION_SEPARATOR = ":";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("Usage:" + EFG2GraphvizFixString.class.getName() + "<efg file>  <dot file> ");
			System.exit(1);
		}

		IN_EFG_FILE = args[0];
		OUT_DOT_FILE = args[1];

		XMLHandler handler = new XMLHandler();
		EFG efg = null;
		efg = (EFG) handler.readObjFromFile(IN_EFG_FILE, EFG.class);
		if(efg == null)
			System.exit(1);
		StringBuffer result = toGraphviz(efg);
		
		try {
			// Create file
			FileWriter fstream = new FileWriter(OUT_DOT_FILE);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(result.toString());
			// Close the output stream
			out.close();
			System.out.println("DONE");
		} catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}

	}

	public static String eventIdCorrection(String eventId)
	{
		String res = eventId;
		res = res.replace(" ", EVENT_ID_SPLITTER);
		res = res.replace(".", "");
		res = res.replace("(", "");
		res = res.replace(")", "");
		res = res.replace(",", "");
		res = res.replace("|", "");
		res = res.replace("[", "");
		res = res.replace("]", "");
		res = res.replace("&", "and");
		res = res.replace("/", "");
		return res;
	}
	
	public static StringBuffer toGraphviz(EFG efg) {
		StringBuffer result = new StringBuffer();

		result.append("digraph " + "EFG" + " {" + "\n");

		// Set up node
		result.append("\t/* Nodes */\n");
		for (EventType event : efg.getEvents().getEvent()) {
			result.append("\t");

			// Set up label
			String res = event.getEventId();
			res = eventIdCorrection(res);
			result.append(res);
			result.append("[");
			result.append(" label=\"");
			result.append(res);
			result.append("\"");

			// set up initial state
			if (event.isInitial()) {
				result.append(" style=filled ");
			}
			result.append(" ]");
			result.append(";\n");
		}
		result.append("\n");
		result.append("\t/* Edges */\n");

		List<EventType> lEvents = efg.getEvents().getEvent();
		List<RowType> lRows = efg.getEventGraph().getRow();

		for (int row = 0; row < lRows.size(); row++) {
			List<Integer> lE = lRows.get(row).getE();
			for (int col = 0; col < lE.size(); col++) {
				if (lE.get(col) > 0) {
					String source = lEvents.get(row).getEventId();
					source = eventIdCorrection(source);
					String target = lEvents.get(col).getEventId();
					target = eventIdCorrection(target);
					source = source.replace(' ', EVENT_ID_SPLITTER_CHAR);
					source = source.replace(".", "");
					source = source.replace("(", "");
					source = source.replace(")", "");
					source = source.replace(",", "");
					source = source.replace("|", "");
					
					target = target.replace(' ', EVENT_ID_SPLITTER_CHAR);
					target = target.replace(".", "");
					target = target.replace("(", "");
					target = target.replace(")", "");
					target = target.replace(",", "");
					target = target.replace("|", "");
					result.append("\t");
					result.append(source + "->" + target);
					result.append(";\n");
				}
			}
		}

		result.append("}");
		return result;
	}
}
