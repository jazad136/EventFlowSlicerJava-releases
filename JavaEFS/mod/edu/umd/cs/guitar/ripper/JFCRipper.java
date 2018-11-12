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
package edu.umd.cs.guitar.ripper;

import java.io.File;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.kohsuke.args4j.CmdLineException;

import edu.umd.cs.guitar.ripper.adapter.JFCTabFilter;
import edu.umd.cs.guitar.model.GIDGenerator;
import edu.umd.cs.guitar.model.GUITARConstants;
import edu.umd.cs.guitar.model.JFCConstants;
import edu.umd.cs.guitar.model.JFCDefaultIDGeneratorSimple;
import edu.umd.cs.guitar.model.XMLHandler;
import edu.umd.cs.guitar.model.data.AttributesType;
import edu.umd.cs.guitar.model.data.ComponentListType;
import edu.umd.cs.guitar.model.data.ComponentType;
import edu.umd.cs.guitar.model.data.Configuration;
import edu.umd.cs.guitar.model.data.FullComponentType;
import edu.umd.cs.guitar.model.data.GUIStructure;
import edu.umd.cs.guitar.model.data.ObjectFactory;
import edu.umd.cs.guitar.model.wrapper.AttributesTypeWrapper;
import edu.umd.cs.guitar.model.wrapper.ComponentTypeWrapper;
import edu.umd.cs.guitar.ripper.adapter.GRipperAdapter;
import edu.umd.cs.guitar.ripper.adapter.IgnoreSignExpandFilter;
import edu.umd.cs.guitar.ripper.plugin.GRipperPlugin;
import edu.umd.cs.guitar.util.GUIStructureInfoUtil;
import edu.umd.cs.guitar.util.DefaultFactory;
import edu.umd.cs.guitar.util.GUITARLog;

/**
 *
 * Executing class for JFCRipper
 *
 * <p>
 *
 * @author <a href="mailto:baonn@cs.umd.edu"> Bao Nguyen </a>
 */
public class JFCRipper {
	JFCRipperConfiguration CONFIG;

	/**
	 * @param CONFIG
	 */
	public JFCRipper(JFCRipperConfiguration CONFIG) {
		super();
		this.CONFIG = CONFIG;
	}

	/**
	 * Execute the jfc ripper
	 *
	 * <p>
	 *
	 * @throws CmdLineException
	 *
	 */
	Ripper ripper;

	public void execute() throws CmdLineException {
		if (CONFIG.help) {
			throw new CmdLineException("");
		}

		long nStartTime = System.currentTimeMillis();
		GUITARLog.addFileAppender(JFCRipperConfiguration.LOG_FILE);
		ripper = new Ripper(GUITARLog.log);

		// -------------------------
		// Setup configuration
		// -------------------------

		try {
			setupEnv();
			ripper.execute();
		} catch (Exception e) {
			GUITARLog.log.error("Error while ripping", e);
			System.exit(1);
		}

		GUITARLog.log.info("-----------------------------");
		GUITARLog.log.info("WINDOW SUMARY: ");
		GUIStructure dGUIStructure = ripper.getResult();
		GUIStructureInfoUtil guistructureinfoutil = new GUIStructureInfoUtil();
		guistructureinfoutil.generate(dGUIStructure, false);
		XMLHandler handler = new XMLHandler();
		handler.writeObjToFile(dGUIStructure, JFCRipperConfiguration.GUI_FILE);

		GUITARLog.log.info("-----------------------------");
		GUITARLog.log.info("OUTPUT SUMARY: ");
		GUITARLog.log.info("Number of Windows: "
				+ dGUIStructure.getGUI().size());
		GUITARLog.log.info("GUI file:" + JFCRipperConfiguration.GUI_FILE);

		ComponentListType lOpenWins = ripper.getlOpenWindowComps();
		ComponentListType lCloseWins = ripper.getlCloseWindowComp();
		ObjectFactory factory = new ObjectFactory();



		// ------------------
		// Elapsed time:
		long nEndTime = System.currentTimeMillis();
		long nDuration = nEndTime - nStartTime;
		DateFormat df = new SimpleDateFormat("HH : mm : ss: SS");

		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		GUITARLog.log.info("Ripping Elapsed: " + df.format(nDuration));
		GUITARLog.log.info("Log file: " + JFCRipperConfiguration.LOG_FILE);
		GUITARLog.log.info("-----------------------------");
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

		try {
			conf = (Configuration) handler.readObjFromFile(
					JFCRipperConfiguration.CONFIG_FILE, Configuration.class);

			if (conf == null) {
				InputStream in = getClass()
						.getClassLoader()
						.getResourceAsStream(JFCRipperConfiguration.CONFIG_FILE);
				conf = (Configuration) handler.readObjFromFile(in,
						Configuration.class);
			}

		} catch (Exception e) {
			GUITARLog.log.error("*** No configuration file."
					+ " Using an empty one ***");
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

		GRipperMonitor jMonitor = new JFCRipperMointor(CONFIG);
		ripper.setMonitor(jMonitor);
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
		GRipperAdapter jIgnoreExpand = new IgnoreSignExpandFilter(lIgnoredComps);
		ripper.addComponentFilter(jIgnoreExpand);

		// Setup tab components ripper filter
		GRipperAdapter jTab = JFCTabFilter.getInstance();
		ripper.addComponentFilter(jTab);

		// Set up IDGenerator
		GIDGenerator jIDGenerator = JFCDefaultIDGeneratorSimple.getInstance();
		ripper.setIDGenerator(jIDGenerator);

		// Setup ripper to use regex for window title matching
		if (CONFIG.USE_REG) {
			ripper.setUseRegex();
		}

		// Setup ripper to use regex for window title matching

		if(CONFIG.RANDOM_WALK_STEPS!=null){
			ripper.setRandomWalk(true);
			ripper.setMaxSteps(CONFIG.RANDOM_WALK_STEPS);
		}else{
			ripper.setRandomWalk(false);
		}


		/**
		 * Set additional GUI artifact data path The directory is created if it
		 * does not exist. If it exists, contents are not deleted
		 */
		if (CONFIG.USE_IMAGE) {
			String strDataPath = JFCRipperConfiguration.GUI_FILE + "."
					+ "data/";
			ripper.setDataPath(strDataPath);

			try {
				File file = new File(strDataPath);
				file.mkdir();
			} catch (Exception e) {
				GUITARLog.log.error("Unable to create GUI data path "
						+ strDataPath);
				throw e;
			}

			// Setup ripper to save images if specified
			ripper.setUseImage();
		}

		// Add ripper plugins
		if (CONFIG.PLUGIN_LIST != null) {
			System.out.println("Ripper plugin(s):");
			for (String pluginName : CONFIG.PLUGIN_LIST) {
				System.out.println("\t" + pluginName);
				Class<?> pluginClass = Class.forName(pluginName);
				GRipperPlugin plugin = (GRipperPlugin) pluginClass
						.newInstance();
				ripper.addPlugin(plugin);
				// if (pluginClass.isAssignableFrom(GRipperPlugin.class)) {
				//
				//
				// }
			}
		}
	}

} // End of class
