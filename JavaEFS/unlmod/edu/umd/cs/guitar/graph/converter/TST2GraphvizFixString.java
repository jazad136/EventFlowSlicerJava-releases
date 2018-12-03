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
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.RuleBasedCollator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.bind.Unmarshaller;

import edu.umd.cs.guitar.model.XMLHandler;
import edu.umd.cs.guitar.model.data.StepType;
import edu.umd.cs.guitar.model.data.TestCase;
import edu.unl.cse.efs.tools.PathConformance;
import edu.unl.cse.efs.tools.StringTools;
import edu.unl.cse.efs.tools.WildcardFiles;

/**
 *
 * This class is designed to create a graphical representation of the content of a TST
 * file.  A test case is simply a sequence of events happening one after the other.
 * This source code simply creates a file containing nodes representing all
 * events of a TST, and when rendered connects each node starting from the first event step
 * found in the file, to its successor step, and connects that successor to its successor, continuing until
 * the next to last successor is connected to a node representing the last step in the file.
 * The graph can then be rendered by the Graphviz application.
 *
 * In case of special characters, this class also has extra source to replace
 * any characters that will prevent the Graphviz application from opening, with
 * Graphviz-compatible characters
 *
 * @author Jonathan Saddler (jsaddle)
 * @version July 7, 2015
 */
public class TST2GraphvizFixString {


	private static File OUT_DOT_FILE;
	private static File[] IN_TST_FILES;

	/**
	 * If the user wishes to reverse the effects of bookmarking in the resulting DOT
	 * file, then they should specify the -bk flag in the arguments, which will turn
	 * this mode on, and remove the bookmarked widget numbered-name preceding any colons
	 * found within the EventId's of this TST file. reverse_bookmarking_mode removes
	 * the numbered name before the colon in an EventId as well as the colon itself, from
	 * the resulting graph node.
	 */
	private static boolean REVERSE_BOOKMARKING_MODE;
	private static boolean PARSE_ONE;
	public static int organization;
	private static final String EVENT_ID_SPLITTER = "_";

	/**
	 * @param args
	 */


	public static void main(String[] args)
	{
		organization = 0;
		parseArgs(args);
		System.out.println("Input accepted...");
		doConvertAndWriteToFile(OUT_DOT_FILE, IN_TST_FILES);
	}

	public static void parseArgs(String[] args)
	{
		boolean oFound, tst1Found, tst2Found, orgFound;
		oFound = tst1Found = tst2Found = orgFound = false;

		ArrayList<String> argsLeft = new ArrayList<String>(Arrays.asList(args));

		Iterator<String> argsIt = argsLeft.iterator();
		String target;
		ArrayList<File> tstFiles = new ArrayList<File>();
		while(argsIt.hasNext()) {
			target = argsIt.next();
			if(target.equals("-org")) {
				if(orgFound)
					throw new IllegalArgumentException("-org parameter cannot be defined twice.");
				orgFound = true;
				try {
					argsIt.remove();
					String org = argsIt.next();
					argsIt.remove();
					organization = Integer.parseInt(org);
				} catch(NoSuchElementException | IllegalArgumentException e) {
					throw new IllegalArgumentException("Invalid argument passed to -org parameter.\n"
							+ "Organization strategy must be a valid integer >= 0.");
				}
			}
			else if(target.equals("-o")) {
				if(oFound)
					throw new IllegalArgumentException("-o parameter cannot be defined twice.");
				oFound = true;
				try {
					argsIt.remove();
					String newFilename = argsIt.next();
					File newFile = new File(newFilename);
					if(	newFile.isDirectory()
					||	newFilename.charAt(newFilename.length()-1) == File.separatorChar)
						throw new IllegalArgumentException();
					argsIt.remove();
					OUT_DOT_FILE = newFile;
				}
				catch(NoSuchElementException | IllegalArgumentException e) {
					throw new IllegalArgumentException("Invalid argument passed to -o parameter.\n"
							+ "Argument passed to -o must be a valid output .dot filename.");
				}
			}
			else {
				if(!tst1Found) tst1Found = true;
				else tst2Found = true;
				try {
					argsIt.remove();

					if(target.contains("*")) {
						String filename = target;
						if(StringTools.charactersIn(filename, '*') >= 2)
							throw new IllegalArgumentException("Invalid wildcard argument passed to the program.\n"
									+ "Input argument containing multiple asterisks is invalid.");
						int starPos = filename.indexOf('*');
						String prePosString = filename.substring(0, starPos);
						String postPosString = filename.substring(starPos+1);
						File currentFile = null;
						try {
							JAXBContext context = JAXBContext.newInstance(TestCase.class);
							Unmarshaller um = context.createUnmarshaller();
							ArrayList<File> retrieved = new ArrayList<File>(WildcardFiles.findFiles(prePosString, postPosString));
							for(File iF : retrieved) {
								currentFile = iF;
								// test to see if the file is a valid TST
								if(!iF.exists())
					    			continue; // skip invalid files
					    		else if(iF.isDirectory())
					    			continue; // skip directories found.

					    		Object myFile;
					    		try { myFile = JAXBIntrospector.getValue(um.unmarshal(iF));}
					    		catch(JAXBException e) {
					    			continue;  // this in particular is not a problem, just an invalid file
					    		}
					    		if(!(myFile instanceof TestCase))
					    			continue;
					    		// passed all the tests, add the file.
					    		tstFiles.add(iF);
							}
							if(tstFiles.isEmpty()) {
								System.err.println("WARNING: Arguments specified by " + filename + " contained no valid input TestCase files");

							}
							else {
								tst1Found = true;
								if(tstFiles.size() > 1)
									tst2Found = true;
							}
						}
						catch(IOException e) {
							throw new IllegalArgumentException("An access violation occurred while parsing input file arguments:\n"
									+ e.getMessage());
						}
						catch(JAXBException e) {
							String fileString =  currentFile != null ? currentFile.getName() : "";
							throw new IllegalArgumentException("An XML parsing violation occurred while parsing input file arguments:\n"
									+ fileString + ": " + e.getLinkedException().getMessage());
						}
					}
					else {
						File newFile = new File(target);
						if(newFile.isDirectory()) throw new IllegalArgumentException();
						tstFiles.add(newFile);
						tst1Found = true;
					}
				}
				catch(NoSuchElementException | IllegalArgumentException e) {
					throw new IllegalArgumentException("Invalid argument passed to -o parameter.\n"
							+ "Argument passed to -o must be a valid output file.");
				}
			}
		}
		IN_TST_FILES = tstFiles.toArray(new File[0]);
		if(!tst1Found)
			throw new IllegalArgumentException("At least one TST file must be provided.");
		if(!tst2Found)
			PARSE_ONE = true;
		if(!oFound)
			throw new IllegalArgumentException("The output file was not specified.");

		String folderPath = PathConformance.parseApplicationPath(OUT_DOT_FILE.getAbsolutePath());
		if(!folderPath.isEmpty()) {
			File folder = new File(folderPath);
			if(!folder.exists())
				throw new IllegalArgumentException("Directory of output file specified does not exist");
		}
	}
	public static void doConvertAndWriteToFile(File outputFile, File[] inTST)
	{
		TestCase[] testCasesRead = new TestCase[inTST.length];
		try {
			XMLHandler handler = new XMLHandler();
			for(int i = 0; i < inTST.length; i++) {
				File tFile = inTST[i];
				TestCase tst = null;
				tst = (TestCase) handler.readObjFromFileThrowExceptions(tFile, TestCase.class);
				if(tst == null) {
					System.err.println("Unable to read TST file.\nNow exiting...");
					System.exit(1);
				}
				testCasesRead[i] = tst;
			}
		} catch(JAXBException e) {
			System.err.println("Unable to read TST file.\n" + e.getMessage());
			System.err.println("Now exiting...");
			System.exit(1);
		}

		StringBuffer result = toGraphvizCompressLabel(testCasesRead, organization);
		System.out.println("Writing all input files to:\n" + outputFile);

		try {
			// Create file
			FileWriter fstream = new FileWriter(outputFile);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(result.toString());
			// Close the output stream
			out.close();
			System.out.println("Done.");
		} catch (IOException e) {// Catch exception if any
			System.err.println("Error: Unable to write test cases to file: " + e.getMessage());
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
		res = res.replace("-", "");

		if(REVERSE_BOOKMARKING_MODE) {
			int colIndex = res.lastIndexOf(':');
			if(colIndex != -1)
				res = res.substring(colIndex+1);
		}
		else
			res = res.replace(":", "");
		return res;
	}
	public interface TSTLabels
	{
		public void add(int theNumber);
		public void addTo(int position, int theNumber);
		public String getFormattedLabel(int position);
	}
	public static class Organizer extends ArrayList<ArrayList<Integer>> implements TSTLabels
	{
		private boolean compress;
		public Organizer()
		{
			this.compress = false;
		}

		public Organizer(boolean compress)
		{
			this.compress = compress;
		}

		public void add(int theNumber)
		{
			ArrayList<Integer> newList = new ArrayList<Integer>();
			newList.add(theNumber);
			super.add(newList);
		}

		public void addTo(int position, int theNumber)
		{
			get(position).add(theNumber);
		}

		public String getFormattedLabel(int position)
		{
			if(compress) {

			}
			//[ label="x,y,...,z
			String formattedLabel = "";
			ArrayList<Integer> output = get(position);
			Collections.sort(output);
			Iterator<Integer> outIt = output.iterator();
			formattedLabel += outIt.next();
			int count = 0;
			while(outIt.hasNext()) {
//				formattedLabel += ","+outIt.next();
				if(count == 6) {
					formattedLabel += ",\n" + outIt.next();
					count = 0;
				}
				else {
					formattedLabel += ","+outIt.next();
					count++;
				}

			}
			return "[ label=\"" + formattedLabel;
		}
	}
	public static class SetOrganizer extends ArrayList<HashSet<Integer>> implements TSTLabels
	{
		public void add(int theNumber)
		{
			HashSet<Integer> newSet = new HashSet<Integer>();
			newSet.add(theNumber);
			super.add(newSet);
		}
		public void addTo(int position, int theNumber)
		{
			get(position).add(theNumber);
		}
		public String getFormattedLabel(int position)
		{
			//[ label="x,y,...,z
			String formattedLabel = "";
			LinkedList<Integer> output = new LinkedList<Integer>(get(position));
			Collections.sort(output);
			Iterator<Integer> outIt = output.iterator();
			formattedLabel += outIt.next();
			while(outIt.hasNext())
				formattedLabel += ","+outIt.next();
			return "[ label=\"" + formattedLabel;
		}
	}
	public static StringBuffer toGraphvizCompressLabel(TestCase[] tsts, int labelOrganization)
	{
		if(labelOrganization <= 0)
			return toGraphviz(tsts);

		StringBuffer result = new StringBuffer();

		ArrayList<String> eventNames = new ArrayList<String>();
		ArrayList<String> edgeNames = new ArrayList<String>();
		// Set up node
		StringBuffer nResult, eResult;
		TSTLabels labels;
		if(labelOrganization == 1) // not so tight
			labels = new Organizer();
		else // very tight.
			labels = new SetOrganizer();

		ArrayList<StringBuffer> ends = new ArrayList<StringBuffer>();
		nResult = new StringBuffer();
		eResult = new StringBuffer();

		// setup:
		ArrayList<StepType> currentSteps = new ArrayList<StepType>(tsts[0].getStep());
		for(int i = 0; i < currentSteps.size(); i++) {
			StepType step = currentSteps.get(i);
			String res = step.getEventId();
			res = eventIdCorrection(res);
			if(!eventNames.contains(res))
				eventNames.add(res);
		}
		// for the first file.

		for(int i = 0; i < currentSteps.size() - 1; i++) {
			// add an edge.
			// get edge information.
			StepType step = currentSteps.get(i);
			String source = eventIdCorrection(step.getEventId());
			String target = eventIdCorrection(currentSteps.get(i+1).getEventId());
			String edgeName = source + "->" + target;

			// add new edge specification or append a new label.
			if(!edgeNames.contains(edgeName)) {
				labels.add(i+1);
				edgeNames.add(edgeName);
			}
			else
				labels.addTo(edgeNames.indexOf(edgeName), i+1);

			// add new edge ending regardless.
			StringBuffer customResult = new StringBuffer();
			customResult.append(" fontcolor=\"");
			customResult.append("red");
			customResult.append("\"");
			customResult.append(" ];");
			ends.add(customResult);
		}

		// for the rest of the files
		for(int t = 1; t < tsts.length; t++) {
			// some edges are hardened already.
			 // or add to the nodes, and reference the new nodes.
			currentSteps = new ArrayList<StepType>(tsts[t].getStep());
			int formerN2;
			for(int formerN1 = 0; formerN1 <= currentSteps.size()-2; formerN1++) {
				formerN2 = formerN1+1;
				StepType step1 = currentSteps.get(formerN1);
				String step1EId = eventIdCorrection(step1.getEventId());
				StepType step2 = currentSteps.get(formerN2);
				String step2EId = eventIdCorrection(step2.getEventId());
				// reference them,
				int n1 = eventNames.indexOf(step1EId);
				int n2 = eventNames.indexOf(step2EId);

				// or add to the nodes, and reference the new nodes.
				if(n1 == -1) {
					eventNames.add(step1EId);
					n1 = eventNames.size()-1;
				}
				if(n2 == -1) {
					eventNames.add(step2EId);
					n2 = eventNames.size()-1;
				}

				// construct the edge name for this target edge
				String source = eventNames.get(n1);
				String target = eventNames.get(n2);
				String edgeName = source + "->" + target;

				int e = edgeNames.indexOf(edgeName);
				if(e == -1) {
					// add an edge to the file.
					edgeNames.add(edgeName);
					e = edgeNames.size()-1;

					// append a new label for this edge.
					labels.add(formerN2);

					// append a new customization for this edge.
					StringBuffer customResult = new StringBuffer();
					customResult.append(" fontcolor=\"");
					customResult.append("red");
					customResult.append("\"");
					customResult.append(" ];");
					ends.add(customResult);

				}
				else
					labels.addTo(e, formerN2);
			}
		}

		// build nodes.
		nResult.append("\t/* Nodes */\n");
		for(int i = 0; i < eventNames.size(); i++) {
			nResult.append("\t");
			nResult.append(eventNames.get(i));

			// node label
			nResult.append("[");
			nResult.append(" label=\"");
			nResult.append(eventNames.get(i));
			nResult.append("\"");
			// set up initial state
			if(i == 0)
				nResult.append(" style=filled");
			// complete node
			nResult.append(" ]");
			nResult.append(";\n");
		}



		// build edges
		eResult.append("\t/* Edges */\n");
		for(int i = 0; i < edgeNames.size(); i++) {
			eResult.append("\t");
			eResult.append(edgeNames.get(i));
			String formattedLabel = labels.getFormattedLabel(i);
			eResult.append(formattedLabel);
//			eResult.append(labels.get(i));
			eResult.append("\"");
			eResult.append(ends.get(i));
			eResult.append("\n");
		}


		// build string buffer containing the entire file.
		result.append("digraph " + "EFG" + " {" + "\n");
		result.append(nResult);
		result.append("\n");
		result.append(eResult);
		result.append("}\n");

		return result; // return the string buffer containing the file.
	}
	public static StringBuffer toGraphviz(TestCase[] tsts)
	{
		StringBuffer result = new StringBuffer();

		ArrayList<String> eventNames = new ArrayList<String>();
		ArrayList<String> edgeNames = new ArrayList<String>();
		// Set up node
		StringBuffer nResult, eResult;
		ArrayList<StringBuffer> labels = new ArrayList<StringBuffer>();
		ArrayList<StringBuffer> ends = new ArrayList<StringBuffer>();
		nResult = new StringBuffer();
		eResult = new StringBuffer();

		// setup:
		ArrayList<StepType> currentSteps;
		currentSteps = new ArrayList<StepType>(tsts[0].getStep());
		for(int i = 0; i < currentSteps.size(); i++) {
			StepType step = currentSteps.get(i);
			String res = step.getEventId();
			res = eventIdCorrection(res);
			eventNames.add(res);
		}

		for(int i = 0; i < tsts[0].getStep().size(); i++) {
			currentSteps = new ArrayList<StepType>(tsts[0].getStep());
			StepType step = currentSteps.get(i);
			// add new node
			String nextId = eventIdCorrection(step.getEventId());
			if(eventNames.contains(nextId))
				continue;
			eventNames.add(nextId);
			// add new edge
			if(i <= currentSteps.size()-2) {
				int n1, n2;
				n1 = i;
				n2 = i+1;

				String source = eventNames.get(n1);
				String target = eventNames.get(n2);

				String edgeName = source + "->" + target;
				edgeNames.add(edgeName);


				StringBuffer lResult = new StringBuffer();
				lResult.append("[");
				lResult.append(" label=\"");
				lResult.append(" " + n2);
				labels.add(lResult);

				StringBuffer customResult = new StringBuffer();
				customResult.append(" fontcolor=\"");
				customResult.append("red");
				customResult.append("\"");
				customResult.append(" ];");
				ends.add(customResult);
			}
		}

		// for the rest of the files
		for(int t = 1; t < tsts.length; t++) {
			// some edges are hardened already.
			 // or add to the nodes, and reference the new nodes.
			currentSteps = new ArrayList<StepType>(tsts[t].getStep());
			int formerN2;
			for(int formerN1 = 0; formerN1 <= currentSteps.size()-2; formerN1++) {
				formerN2 = formerN1+1;
				StepType step1 = currentSteps.get(formerN1);
				String step1EId = eventIdCorrection(step1.getEventId());
				StepType step2 = currentSteps.get(formerN2);
				String step2EId = eventIdCorrection(step2.getEventId());
				// reference them,
				int n1 = eventNames.indexOf(step1EId);
				int n2 = eventNames.indexOf(step2EId);

				// or add to the nodes, and reference the new nodes.
				if(n1 == -1) {
					eventNames.add(step1EId);
					n1 = eventNames.size()-1;
				}
				if(n2 == -1) {
					eventNames.add(step2EId);
					n2 = eventNames.size()-1;
				}

				// construct the edge name for this target edge
				String source = eventNames.get(n1);
				String target = eventNames.get(n2);
				String edgeName = source + "->" + target;

				int e = edgeNames.indexOf(edgeName);
				if(e == -1) {
					// add an edge to the file.
					edgeNames.add(edgeName);
					e = edgeNames.size()-1;

					// append a new label for this edge.
					StringBuffer lResult = new StringBuffer();
					lResult.append("[");
					lResult.append(" label=\"");
					lResult.append(" " + formerN2);
					labels.add(lResult);

					// append a new customization for this edge.
					StringBuffer customResult = new StringBuffer();
					customResult.append(" fontcolor=\"");
					customResult.append("red");
					customResult.append("\"");
					customResult.append(" ];");
					ends.add(customResult);

				}
				else {
					// customize the label for this edge
					StringBuffer lResult = labels.get(e);
					lResult.append("," + formerN2);
					labels.set(e, lResult);
				}
			}
		}

		// build nodes.
		nResult.append("\t/* Nodes */\n");
		for(int i = 0; i < eventNames.size(); i++) {
			nResult.append("\t");
			nResult.append(eventNames.get(i));

			// node label
			nResult.append("[");
			nResult.append(" label=\"");
			nResult.append(eventNames.get(i));
			nResult.append("\"");
			// set up initial state
			if(i == 0)
				nResult.append(" style=filled");
			// complete node
			nResult.append(" ]");
			nResult.append(";\n");
		}



		// build edges
		eResult.append("\t/* Edges */\n");
		for(int i = 0; i < edgeNames.size(); i++) {
			eResult.append("\t");
			eResult.append(edgeNames.get(i));
			eResult.append(labels.get(i));
			eResult.append("\"");
			eResult.append(ends.get(i));
			eResult.append("\n");
		}


		// build string buffer containing the entire file.
		result.append("digraph " + "EFG" + " {" + "\n");
		result.append(nResult);
		result.append("\n");
		result.append(eResult);
		result.append("}\n");

		return result; // return the string buffer containing the file.
	}


	public static StringBuffer toGraphviz(TestCase tst)
	{
		StringBuffer result = new StringBuffer();

		result.append("digraph " + "EFG" + " {" + "\n");
		ArrayList<String> newEventNames = new ArrayList<String>();
		// Set up node
		result.append("\t/* Nodes */\n");
		int index = -1;

		for(StepType step : tst.getStep()) {
			index++;
			result.append("\t");
			// Set up label
			String res = eventIdCorrection(step.getEventId());

			newEventNames.add(res);
			result.append(res);
			result.append("[");
			result.append(" label=\"");
			result.append(res);
			result.append("\"");

			// set up initial state
			if(index == 0)
				result.append(" style=filled");

			result.append(" ]");
			result.append(";\n");
		}
		result.append("\n");
		result.append("\t/* Edges */\n");

		int n2;
		for (int n1 = 0; n1 < newEventNames.size()-1; n1++) {
			n2 = n1+1;
			String source = newEventNames.get(n1);
			String target = newEventNames.get(n2);

			result.append("\t");
			result.append(source + "->" + target);

			result.append("[");
			result.append(" label=\"");
			result.append(" " + n2);
			result.append("\"");

			result.append(" fontcolor=\"");
			result.append("red");
			result.append("\"");

			result.append(" ]");
			result.append(";\n");
		}
		result.append("}");
		return result;
	}
	/*
	 * StepType step = currentSteps.get(i);
			// add new node
			String nextId = eventIdCorrection(step.getEventId());
//			if(!eventNames.contains(nextId))
//				eventNames.add(nextId);
			// add new edge
			if(i <= currentSteps.size()-2) {
//				int n1, n2;
				int n2;
//				n1 = i;
				n2 = i+1;
				String source = nextId;
				String target = eventIdCorrection(currentSteps.get(n2).getEventId());
//				String n2Id = eventIdCorrection(currentSteps.get(n2).getEventId());
//				String source = eventNames.get(nextId);
//				String target = eventNames.get(n2Id);

				String edgeName = source + "->" + target;
				edgeNames.add(edgeName);

				labels.add(n2);

				StringBuffer customResult = new StringBuffer();
				customResult.append(" fontcolor=\"");
				customResult.append("red");
				customResult.append("\"");
				customResult.append(" ];");
				ends.add(customResult);
	 */
}
