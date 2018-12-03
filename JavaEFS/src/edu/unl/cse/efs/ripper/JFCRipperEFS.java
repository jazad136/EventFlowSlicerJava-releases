/*
 *  Copyright (c) 2009, 2018-@year@. The  GUITAR group  at the University of
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
package edu.unl.cse.efs.ripper;

import java.io.File;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import edu.umd.cs.guitar.ripper.JFCRipperConfiguration;
import edu.umd.cs.guitar.ripper.JFCRipperMointor;
import edu.umd.cs.guitar.ripper.Ripper;
import edu.umd.cs.guitar.ripper.adapter.JFCTabFilter;
import edu.umd.cs.guitar.model.GUITARConstants;
import edu.umd.cs.guitar.model.JFCConstants;
import edu.umd.cs.guitar.model.XMLHandler;
import edu.umd.cs.guitar.model.data.AttributesType;
import edu.umd.cs.guitar.model.data.ComponentListType;
import edu.umd.cs.guitar.model.data.ComponentType;
import edu.umd.cs.guitar.model.data.Configuration;
import edu.umd.cs.guitar.model.data.FullComponentType;
import edu.umd.cs.guitar.model.data.GUIStructure;
import edu.umd.cs.guitar.model.data.TaskList;
import edu.umd.cs.guitar.model.data.Widget;
import edu.umd.cs.guitar.model.wrapper.AttributesTypeWrapper;
import edu.umd.cs.guitar.model.wrapper.ComponentTypeWrapper;
import edu.umd.cs.guitar.ripper.adapter.IgnoreSignExpandFilter;
import edu.umd.cs.guitar.ripper.plugin.GRipperPlugin;
import edu.umd.cs.guitar.util.GUIStructureInfoUtil;
import edu.umd.cs.guitar.util.DefaultFactory;
import edu.umd.cs.guitar.util.GUITARLog;
import edu.unl.cse.efs.guitaradapter.JFCRulesFilter;
import edu.unl.cse.efs.guitarplugin.JFCIDGeneratorEFS;
import edu.unl.cse.efs.guitarplugin.JavaTestInteractionsInstantiator;
import edu.unl.cse.jontools.paths.TaskListConformance;

/**
 * 
 * Executing class for JFCRipperFlowbehind
 * 
 * 
 * @author Jonathan Saddler 
 */
public class JFCRipperEFS {
	
	private JFCRipperConfigurationEFS config;
	
	Ripper ripper;
	JFCRulesFilter rulesFilt;
	JFCIDGeneratorEFS jIDGeneratorFB;
	
	/**
	 * @param CONFIG
	 */
	public JFCRipperEFS(JFCRipperConfigurationEFS CONFIG) {
		super();
		this.config = CONFIG;
	}
	
	public static void main(String[] args)
	{
		JFCRipperConfigurationEFS configuration = new JFCRipperConfigurationEFS();
		final JFCRipperEFS jfcRipperFB = new JFCRipperEFS(configuration);
		jfcRipperFB.runRipper(args);
	}
	
	public JFCRulesFilter getRulesFilter()
	{
		return rulesFilt;
	}
	public GUIStructure runRipper(String[] args)
	{
		CmdLineParser parser = new CmdLineParser(config);
		GUIStructure rippedStructure = null;
		try {
			parser.parseArgument(args);
			rippedStructure = execute();
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			System.err.println();
			System.err.println("Usage: java [JVM options] "
                            + JFCRipperEFS.class.getName()
                            + " [Ripper options] \n");
			System.err.println("where [Ripper options] include:");
			System.err.println();
			parser.printUsage(System.err);
			throw new RuntimeException(e);
		}
		return rippedStructure;
	}

	/**
    * 
    */
	private void setupEnv() throws Exception {
		// --------------------------
		// Terminal list
		// Try to find absolute path first then relative path
		XMLHandler handler = new XMLHandler();
		Configuration conf = null;
		
		File configFile = new File(JFCRipperConfigurationEFS.CONFIG_FILE);
		if(!configFile.exists()) {
			System.err.println("\n*** Configuration file not found. ***\n"
							 + "*** Using an empty one. ***\n");
		}
		else {
		try {
			conf = (Configuration) handler.readObjFromFile(
					JFCRipperConfigurationEFS.CONFIG_FILE, Configuration.class);

			if (conf == null) {
				InputStream in = getClass()
						.getClassLoader()
						.getResourceAsStream(JFCRipperConfigurationEFS.CONFIG_FILE);
				conf = (Configuration) handler.readObjFromFile(in,
						Configuration.class);
			}

		} catch (Exception e) {
			System.err.println("*** Failed to read configuration file.***\n"
							 + "*** Using an empty one ***");
		}
		}

		if (conf == null) {
			DefaultFactory df = new DefaultFactory();
			conf = df.createDefaultConfiguration();
		}

		List<FullComponentType> cTerminalList = conf.getTerminalComponents()
				.getFullComponent();

		for (FullComponentType cTermWidget : cTerminalList) {
			ComponentType component = cTermWidget.getComponent();
			AttributesType attributes = component.getAttributes();

			if (attributes != null) {
				JFCConstants.sTerminalWidgetSignature
						.add(new AttributesTypeWrapper(component
								.getAttributes()));
			}
		}

		JFCRipperMointor jMonitor = new JFCRipperMointor(config);

		/*
		 * ***********
		 * ***********
		 * ADAPTERS!!!
		 * ***********
		 * ***********
		 */
		List<FullComponentType> lIgnoredComps = new ArrayList<FullComponentType>();
		ComponentListType ignoredComponentList = conf.getIgnoredComponents();
		if (ignoredComponentList != null) {
			for (FullComponentType fullComp : ignoredComponentList
					.getFullComponent()) {
				ComponentType comp = fullComp.getComponent();

				// TODO: Shortcut here
				if (comp == null) {
					ComponentType win = fullComp.getWindow();
					ComponentTypeWrapper winAdapter = new ComponentTypeWrapper(
							win);
					String sWindowTitle = winAdapter
							.getFirstValueByName(GUITARConstants.TITLE_TAG_NAME);
					if (sWindowTitle != null)
						JFCConstants.sIgnoredWins.add(sWindowTitle);
				} else
					lIgnoredComps.add(fullComp);
			}
		}
		
		// --------------------------
		// Ignore components xml
		IgnoreSignExpandFilter jIgnoreExpand = new IgnoreSignExpandFilter(lIgnoredComps);
		ripper.addComponentFilter(jIgnoreExpand);

		// Setup tab components ripper filter
		JFCTabFilter jTab = (JFCTabFilter)JFCTabFilter.getInstance();
		ripper.addComponentFilter(jTab);
		
		// Setup IgnoreComopnentsByType filter
		rulesFilt = new JFCRulesFilter();
		rulesFilt.setMonitor(jMonitor);
		rulesFilt.ignoreComponents(jIgnoreExpand);
		rulesFilt.expandTabs(jTab);
		ripper.addComponentFilter(rulesFilt);
		
		
		
		/* **************
		 * **************
		 * Set up Monitor
		 * **************
		 * **************
		 */
		ripper.setMonitor(jMonitor);
		
		/* **************
		 * **************
		 * Set up ID Generator
		 * **************
		 * **************
		 */
		jIDGeneratorFB = JFCIDGeneratorEFS.getInstance();
		ripper.setIDGenerator(jIDGeneratorFB);
		/*
		 * **********************
		 * **********************
		 * random walk and images
		 * **********************
		 * **********************
		 */
		/**
		 * Set additional GUI artifact data path The directory is created if it
		 * does not exist. If it exists, contents are not deleted
		 */

		/* **********
		 * **********
		 * PLUGINS!!!
		 * **********
		 * **********
		 */
		// for flowbehind. 
		String namesFileProvided = JFCRipperConfigurationEFS.NAMES_FILE;  
		if(!(namesFileProvided== null || namesFileProvided.isEmpty()))
			ripper.addPlugin(new JavaTestInteractionsInstantiator(namesFileProvided));
		else
			ripper.addPlugin(new JavaTestInteractionsInstantiator());
		
		// Add ripper plugins
		if (config.PLUGIN_LIST != null) {
			System.out.println("Ripper plugin(s):");
			for (String pluginName : config.PLUGIN_LIST) {
				System.out.println("\t" + pluginName);
				Class<?> pluginClass = Class.forName(pluginName);
				GRipperPlugin plugin = (GRipperPlugin) pluginClass
						.newInstance();
				ripper.addPlugin(plugin);
			}
		}
		
	}
	/**
	 * Execute the jfc ripper
	 * 
	 * <p>
	 * 
	 * @throws CmdLineException
	 * 
	 */

	public GUIStructure execute() {
		long nStartTime = System.currentTimeMillis();
		
		ripper = new Ripper(GUITARLog.log);
		GUITARLog.addFileAppender(JFCRipperConfiguration.LOG_FILE);
		// -------------------------
		// Setup configuration
		// -------------------------

		try {
			setupEnv();
			
			ripper.execute();
		} catch (Exception e) {
			System.err.println("Error while ripping: ");
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("-----------------------------");
		System.out.println("WINDOW SUMARY: ");
		XMLHandler handler = new XMLHandler();
		// generate ID's that we missed earlier.
		GUIStructure dGUIStructure = ripper.getResult();
		GUIStructure dFilterGUIStructure = rulesFilt.getResult();
		dGUIStructure.getGUI().addAll(dFilterGUIStructure.getGUI());
		jIDGeneratorFB.generateID(dFilterGUIStructure);
		
		;
		
		// append what we missed earlier to the head GUI structure. 
//		dGUIStructure.getGUI().addAll(dFilterGUIStructure.getGUI());
		// log all the window names, if a log is installed. 
		GUIStructureInfoUtil guistructureinfoutil = new GUIStructureInfoUtil();
		guistructureinfoutil.generate(dGUIStructure, false);
		
		
		
		// write the rip file. 
//		File x = new File(JFCRipperFlowbehindConfiguration.GUI_FILE);
//		if(!x.exists()) 
//			new File(JFCRipperFlowbehindConfiguration.GUI_FILE).mkdirs();
		
		handler.writeObjToFile(dGUIStructure, JFCRipperConfigurationEFS.GUI_FILE);
		
		
		// output some summary data.
		System.out.println("-----------------------------");
		System.out.println("OUTPUT SUMARY: ");
		System.out.println("Number of Windows: "
				+ dGUIStructure.getGUI().size());
		System.out.println("GUI file:" + JFCRipperConfigurationEFS.GUI_FILE);

		// ------------------
		// Elapsed time:
		long nEndTime = System.currentTimeMillis();
		long nDuration = nEndTime - nStartTime;
		DateFormat df = new SimpleDateFormat("HH : mm : ss: SS");

		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		System.out.println("Ripping Elapsed: " + df.format(nDuration));
		System.out.println("Log file: " + JFCRipperConfigurationEFS.LOG_FILE);
		System.out.println("-----------------------------");
		
		return dGUIStructure;
	}

	

} // End of class
