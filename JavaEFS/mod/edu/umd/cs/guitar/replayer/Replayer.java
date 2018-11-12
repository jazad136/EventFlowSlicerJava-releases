/*
 *  Copyright (c) 2009-@year@. The  GUITAR group  at the University of
 *  Maryland. Names of owners of this group may be obtained by sending
 *  an e-mail to atif@cs.umd.edu
 *
 *  Permission is hereby granted, free of charge, to any person obtaining
 *  a copy of this software and associated documentation files
 *  (the "Software"), to deal in the Software without restriction,
 *  including without limitation  the rights to use, copy, modify, merge,
 *  publish,  distribute, sublicense, and/or sell copies of the Software,
 *  and to  permit persons  to whom  the Software  is furnished to do so,
 *  subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY,  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO  EVENT SHALL THE  AUTHORS OR COPYRIGHT  HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR  OTHER LIABILITY,  WHETHER IN AN  ACTION OF CONTRACT,
 *  TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 *  SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package edu.umd.cs.guitar.replayer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edu.umd.cs.guitar.exception.ComponentNotFound;
import edu.umd.cs.guitar.exception.ReplayerStateException;
import edu.umd.cs.guitar.exception.GException;
import edu.umd.cs.guitar.model.GComponent;
import edu.umd.cs.guitar.model.GUITARConstants;
import edu.umd.cs.guitar.model.GWindow;
import edu.umd.cs.guitar.model.XMLHandler;
import edu.umd.cs.guitar.model.data.EFG;
import edu.umd.cs.guitar.model.data.GUIStructure;
import edu.umd.cs.guitar.model.data.PropertyType;
import edu.umd.cs.guitar.model.data.StepType;
import edu.umd.cs.guitar.model.data.TestCase;
import edu.umd.cs.guitar.model.wrapper.ComponentTypeWrapper;
import edu.umd.cs.guitar.model.wrapper.GUIStructureWrapper;
import edu.umd.cs.guitar.model.wrapper.GUITypeWrapper;
import edu.umd.cs.guitar.model.wrapper.PropertyTypeWrapper;
import edu.umd.cs.guitar.replayer.monitor.GTestMonitor;
import edu.umd.cs.guitar.util.GUITARLog;

/**
 * 
 * Main replayer class, monitoring the replayer's behaviors
 * 
 * Note on exception handling:
 * 
 * GUITAR related exceptions MUST be derived from GExceptions. All non
 * GException exceptions are to be considered as AUT exceptions (unless
 * explicitly stated and handled, in an itemised manner).
 * 
 * All "caught" exceptions MUST be propagated upwards unless explicitly
 * itemised.
 * 
 * <p>
 * 
 * @author <a href="mailto:baonn@cs.umd.edu"> Bao Nguyen </a>
 * @author Jonathan Saddler
 */
public abstract class Replayer {
	/**
	 * SECTION: DATA
	 * 
	 * This section contains member variables and accessor functions.
	 */

	/**
	 * Test case data
	 */
	protected TestCase tc;
	protected String sGUIFfile;
	protected String sEFGFfile;

	// Test Monitor
	protected GReplayerMonitor monitor;
	protected List<GTestMonitor> lTestMonitor = new ArrayList<GTestMonitor>();
	
	// Secondary input
	protected GUIStructureWrapper guiStructureAdapter;
	protected EFG efg;
	protected Document docGUI;
	// Log
//	Logger log = GUITARLog.log;
		
	/**
	 * @param tc
	 * @param sGUIFile
	 * @param sEFGFile
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws SAXException
	 */
	public Replayer(TestCase tc, String sGUIFile, String sEFGFile)
			throws ParserConfigurationException, SAXException, IOException {
		super();
		this.tc = tc;
		this.sGUIFfile = sGUIFile;
		this.sEFGFfile = sEFGFile;

		// Initialize GUI object
		XMLHandler handler = new XMLHandler();
		GUIStructure gui = (GUIStructure) handler.readObjFromFile(sGUIFile,
				GUIStructure.class);
		guiStructureAdapter = new GUIStructureWrapper(gui);

		// Initialize EFG object
		this.efg = (EFG) handler.readObjFromFile(sEFGFile, EFG.class);

		// Initialize EFG XML file
		DocumentBuilderFactory domFactory = DocumentBuilderFactory
				.newInstance();
		domFactory.setNamespaceAware(true);
		DocumentBuilder builder;
		builder = domFactory.newDocumentBuilder();
		docGUI = builder.parse(sGUIFile);

		// Initialize to null / disabled
		sDataPath = null;
		useImage = false;
	}

	public Replayer()
	{
		// 	allows the subclass to initialize all variables
	}
	/**
	 * jsaddler commentary: Initializes a new replayer using a String parameter for 
	 * the test case file. 
	 * 
	 * Preconditions: 	testCaseFile must be the name of a valid test case
	 * 					file on the file system.
	 * Postconditions: 	The replayer is instantiated. 
	 */
	public Replayer(String testcaseFile, String GUIFile, String EFGFile)
			throws ParserConfigurationException, SAXException, IOException 
	{
		// jsaddler: wow.
		this((TestCase) (new XMLHandler()).readObjFromFile(testcaseFile, TestCase.class),
				GUIFile, EFGFile);
	}

	/**
	 * Time out for the replayer TODO: Move to a monitor
	 */


	/**
	 * Path for storing artifacts
	 */
	protected String sDataPath;

	/**
	 * Set the path where the replayer can find artifacts.
	 * 
	 * @param sDataPath
	 *            Name of path where the replayer finds artifacts
	 */
	public void setDataPath(String sDataPath) {
		this.sDataPath = sDataPath;
	}

	/**
	 * Use image based identification when possible.
	 */
	protected boolean useImage = false;

	/**
	 * useImage accessor
	 */
	public void setUseImage() {
		this.useImage = true;
	}

	/**
	 * SECTION: LOGIC
	 * 
	 * This section contains the core logic for replaying a GUITAR testcase.
	 */

	/**
	 * Parse and run test case.
	 * 
	 * @throws GException
	 * 
	 */
	public abstract void execute() throws ComponentNotFound, IOException;
	/**
	 * Execute a single step in the test case
	 * 
	 * <p>
	 * 
	 * TODO: Rewrite the test monitor
	 * 
	 * @param step
	 * @throws ComponentNotFound
	 * @throws ReplayerStateException
	 */
	protected abstract void executeStep(StepType step) 
			throws ComponentNotFound, ReplayerStateException, IOException;

	/**
	 * Get container window corresponding to a given widget ID.
	 * This function looks up the GUI structure to extract the
	 * window title of the window containing the widget.
	 *
	 * @param  sWidgetID Widget ID for which container window is required
	 *
	 * @return String    Window title of window containing sWidgetID
	 */
	protected String getWindowName(String sWidgetID)
	{
		String sWindowName = null;

		XPath xpath = XPathFactory.newInstance().newXPath();
		XPathExpression expr;
		Object result;
		NodeList nodes;

		try {
			String xpathExpression = "/GUIStructure/GUI[Container//Property[Name=\""
					+ GUITARConstants.ID_TAG_NAME
					+ "\" and Value=\""
					+ sWidgetID
					+ "\"]]/Window/Attributes/Property[Name=\""
					+ GUITARConstants.TITLE_TAG_NAME + "\"]/Value/text()";
			expr = xpath.compile(xpathExpression);
			result = expr.evaluate(docGUI, XPathConstants.NODESET);
			nodes = (NodeList) result;

			if (nodes.getLength() > 0) {
				sWindowName = nodes.item(0).getNodeValue();
			}

		} catch (XPathExpressionException e) {
			/*
			 * Not propagating. Return value is set to NULL instead. Caller must
			 * check.
			 */
			GUITARLog.log.error(e);
		}

		return sWindowName;
	}

	/**
	 * Get the replayer monitor
	 * 
	 * @return the replayer monitor
	 */
	public GReplayerMonitor getMonitor() {
		return monitor;
	}

	/**
	 * @param monitor
	 *            the replayer monitor to set
	 */
	public void setMonitor(GReplayerMonitor monitor) {
		this.monitor = monitor;
	}

	/**
	 * 
	 * Add a test monitor. A test monitor executes events after each step of the replayer has been executed.
	 * 
	 * <p>
	 * 
	 * @param aTestMonitor
	 */
	public void addTestMonitor(GTestMonitor aTestMonitor) {
		aTestMonitor.setReplayer(this);
		this.lTestMonitor.add(aTestMonitor);
	}

	/**
	 * Remove a test monitor
	 * 
	 * <p>
	 * 
	 * @param mTestMonitor
	 */
	public void removeTestMonitor(GTestMonitor mTestMonitor) {
		this.lTestMonitor.remove(mTestMonitor);
	}

	/**
	 * Wait for a window to appear. This is a blocking call.
	 *
	 * Two methods of waiting for the new window are used.
	 *    *  useImage - use image based identification 
	 *    * !useImage - use "window title" based identification
	 * 
	 * @param sWindowTitle Window title of window to wait for
	 */
	protected GWindow replayerWaitForWindow(String sWindowTitle) throws IOException
	{
		/*
		 * This is a blocking call. Waits until window appears. Uses a regex
		 * based match if specified in the command line.
		 */
		GWindow gWindowByImage = null;
		GWindow gWindowByTitle = null;

		if (useImage) {
		   String strUUID = null;

			// Find GUI Window image for comparing
			GUITypeWrapper guiTypeWrapperParent = guiStructureAdapter
					.getGUIByTitle(sWindowTitle);
			PropertyType propertyType =
			   (guiTypeWrapperParent != null) ? guiTypeWrapperParent
					.getWindowProperty(GUITARConstants.UUID_TAG_NAME) : null;
			if (guiTypeWrapperParent != null) {
				strUUID = propertyType.getValue().get(0);

			   gWindowByImage = monitor.waitForWindow(sWindowTitle,
			                   sDataPath + "/"
				 			    	 + strUUID + ".png");
			}

			// Write ripped and replay image pair to log
			if (gWindowByImage != null) {
				monitor.writeMatchedComponents(gWindowByImage.getContainer(),
			   	                            sDataPath + File.separatorChar + 
			      	                         strUUID + ".png");
			}
		}

		// Always determine using the title based method
		gWindowByTitle = monitor.getWindow(sWindowTitle);

		// Determine if the two methods differed
		if (gWindowByTitle != null &&
			 gWindowByImage != null) {
			if (gWindowByTitle.hashCode() != gWindowByImage.hashCode()) {
				GUITARLog.log.info("Window identification mismatch");
				GUITARLog.log.info("Image based " + gWindowByImage.getTitle() +
				                   "Title based " + gWindowByTitle.getTitle());
			}
		}
		if (useImage && gWindowByTitle != null && gWindowByImage == null) {
			GUITARLog.log.info("Window identification mismatch");
			GUITARLog.log.info("Image based is null. " +
			                   "Title based " + gWindowByTitle.getTitle());
		}
		if (useImage && gWindowByTitle == null && gWindowByImage != null) {
			GUITARLog.log.info("Window identification mismatch");
			GUITARLog.log.info("Image based " + gWindowByImage.getTitle() +
			                   "Title based is null");
		}

		return useImage ? gWindowByImage : gWindowByTitle;
	}

	/**
	 * Locate the component corresponding to sWidgetID inside gwindow.
	 *
	 * @param gWindow   Window in which to look for component
	 * @param sWidgetID Widget ID of widget to look for
	 * @param componentTypeWrapper Component corresponding to sWidgetID
	 *
	 * @returns GComponent of located window 
	 */
	@SuppressWarnings("unused")
	GComponent replayerSearchComponent(GWindow gWindow, String sWidgetID, 
			ComponentTypeWrapper componentTypeWrapper) throws IOException 
	{
		GComponent container = gWindow.getContainer();

		GComponent gComponentByImage = null;
		GComponent gComponentByProp  = null;

		if (useImage) {
		   String strUUID = null;

			// Find GUI Window image for comparing
			ComponentTypeWrapper compTypeWrapper = guiStructureAdapter
					.getComponentFromID(sWidgetID);
			if (compTypeWrapper != null) {
				strUUID =
				   (compTypeWrapper != null) ? compTypeWrapper
					.getFirstValueByName(GUITARConstants.UUID_TAG_NAME) : null;
					
					// baonn says: Because we decided not to put the image feature directly in
					// the core, I commented it out so the Replayer is still compatible with other
					// components 
//
//				try {
// 					gComponentByImage =
//					   container.searchComponentByImage(sDataPath +
//						                                 "/" + strUUID + ".png");
//				} catch (IOException e) {
//					throw new ReplayerStateException();
//				}
			}

			// Write ripped and replay image pair to log
//			if (gComponentByImage != null) {
//				monitor.writeMatchedComponents(gComponentByImage,
//			   	                            sDataPath + File.separatorChar + 
//			      	                         strUUID + ".png");
//			}
		}

		// Always perform property based identification
		List<PropertyType> ID =
		   monitor.selectIDProperties(componentTypeWrapper
			.getDComponentType());
		List<PropertyTypeWrapper> IDAdapter =
			new ArrayList<PropertyTypeWrapper>();
		for (PropertyType p : ID) {
			IDAdapter.add(new PropertyTypeWrapper(p));
		}
		gComponentByProp = container.getFirstChild(IDAdapter);

		// Determine if the two methods were differed
		if (gComponentByProp != null &&
			 gComponentByImage != null) {
			if (gComponentByProp.hashCode() != gComponentByImage.hashCode()) {
				GUITARLog.log.info("Component identification mismatch");
				GUITARLog.log.info("Image based " + gComponentByImage.getTitle() +
				                   " Prop based " + gComponentByProp.getTitle());
			}
		}
		if (useImage && gComponentByProp != null && gComponentByImage == null) {
			GUITARLog.log.info("Component identification mismatch");
			GUITARLog.log.info("Image based is null. " +
			                   " Prop based " + gComponentByProp.getTitle());
		}
		if (useImage && gComponentByProp == null && gComponentByImage != null) {
			GUITARLog.log.info("Component identification mismatch");
			GUITARLog.log.info("Image based " + gComponentByImage.getTitle() +
			                   " Prop based is null");
		}

		return useImage ? gComponentByImage : gComponentByProp;
	}
	
} // End of class
