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

import java.awt.AWTEvent;
import java.awt.AWTException;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleText;
import javax.imageio.ImageIO;

import org.netbeans.jemmy.EventTool;

import edu.umd.cs.guitar.event.EventManager;
import edu.umd.cs.guitar.event.GEvent;
import edu.umd.cs.guitar.event.JFCActionEDT;
import edu.umd.cs.guitar.event.JFCEventHandler;
import edu.umd.cs.guitar.exception.ApplicationConnectException;
import edu.umd.cs.guitar.model.GComponent;
import edu.umd.cs.guitar.model.GObject;
import edu.umd.cs.guitar.model.GUITARConstants;
import edu.umd.cs.guitar.model.GWindow;
import edu.umd.cs.guitar.model.JFCApplication2;
import edu.umd.cs.guitar.model.JFCConstants;
import edu.umd.cs.guitar.model.JFCXComponent;
import edu.umd.cs.guitar.model.JFCXWindow;
import edu.umd.cs.guitar.util.GUITARLog;
import edu.unl.cse.efs.java.JavaLaunchApplication;
import edu.unl.cse.efs.ripper.JFCRipperConfigurationEFS;
import edu.unl.cse.efs.util.ReadArguments;

/**
 * 
 * Monitor for the ripper to handle Java Swing specific features
 * 
 * @see GRipperMonitor
 * 
 * @author <a href="mailto:baonn@cs.umd.edu"> Bao Nguyen </a>
 */
public class JFCRipperMointor extends GRipperMonitor {

	
	// --------------------------
	// Configuartion Parameters
	// --------------------------

	/**
     * 
     */
	private static final int INITIAL_DELAY = 1000;

	// Logger logger;
	JFCRipperConfiguration configuration;
	JFCApplication2 application;
	List<String> sIgnoreWindowList = new ArrayList<String>();

	/**
	 * Constructor
	 * 
	 * <p>
	 * 
	 * @param configuration
	 *            ripper configuration
	 */
	public JFCRipperMointor(JFCRipperConfiguration configuration) {
		super();
		// this.logger = logger;
		this.configuration = configuration;
	}

	List<String> sRootWindows = new ArrayList<String>();

	/**
	 * Temporary list of windows opened during the expand event is being
	 * performed. Those windows are in a native form to prevent data loss.
	 * 
	 */
	volatile LinkedList<Window> tempOpenedWinStack = new LinkedList<Window>();

	volatile LinkedList<Window> tempClosedWinStack = new LinkedList<Window>();

	// volatile LinkedList<GWindow> tempGWinStack = new LinkedList<GWindow>();

	/**This method essentially ensures that runtime dependences between the ripper and the classes
	 * that are used to display the GUI interface of the AUT are not linked together anymore.
	 * 
	 * Following a call to this method, the windows of the interface are disposed, and
	 * classes to load the interface are no longer loaded.
	 */
	@Override
	public void cleanUp() 
	{
		if(application != null) {
			for(GWindow g : application.getAllWindow())
				closeWindow(g);
			application.disconnect();
		}		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.umd.cs.guitar.ripper.RipperMonitor#closeWindow(edu.umd.cs.guitar.
	 * model.GXWindow)
	 */
	@Override
	public void closeWindow(GWindow gWindow) {

		JFCXWindow jWindow = (JFCXWindow) gWindow;
		Window window = jWindow.getWindow();
		// A bug might happen here
		// window.setVisible(false);
		window.dispose();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.umd.cs.guitar.ripper.RipperMonitor#expand(edu.umd.cs.guitar.model
	 * .GXComponent)
	 */
	@Override
	public void expandGUI(GObject component) {

		if (component == null)
			return;

		GEvent action = new JFCActionEDT();
		action.perform(component, null);
		new EventTool().waitNoEvent(configuration.DELAY);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.umd.cs.guitar.ripper.RipperMonitor#getOpenedWindowCache()
	 */
	@Override
	public LinkedList<GWindow> getOpenedWindowCache() {

		LinkedList<GWindow> retWindows = new LinkedList<GWindow>();

		for (Window window : tempOpenedWinStack) {
			GWindow gWindow = new JFCXWindow(window);
			if (gWindow.isValid())
				retWindows.addLast(gWindow);
		}
		return retWindows;
	}

	@Override
	public LinkedList<GWindow> getClosedWindowCache() {

		LinkedList<GWindow> retWindows = new LinkedList<GWindow>();

		for (Window window : tempClosedWinStack) {
			GWindow gWindow = new JFCXWindow(window);
			if (gWindow.isValid())
				retWindows.addLast(gWindow);
		}
		return retWindows;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.umd.cs.guitar.ripper.RipperMonitor#getRootWindows()
	 */
	@Override
	public List<GWindow> getRootWindows() {

		List<GWindow> retWindowList = new ArrayList<GWindow>();

		retWindowList.clear();

		Frame[] lFrames = Frame.getFrames();

		for (Frame frame : lFrames) {

			if (!isValidRootWindow(frame))
				continue;

			AccessibleContext xContext = frame.getAccessibleContext();
			String sWindowName = xContext.getAccessibleName();

			if (sRootWindows.size() == 0
					|| (sRootWindows.contains(sWindowName))) {

				GWindow gWindow = new JFCXWindow(frame);
				retWindowList.add(gWindow);
				// frame.requestFocus();
			}
		}

		// / Debugs:
		GUITARLog.log.debug("Root window size: " + retWindowList.size());
		for (GWindow window : retWindowList) {
			GUITARLog.log.debug("Window title: " + window.getTitle());
		}

		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			GUITARLog.log.error(e);
		}
		return retWindowList;
	}

	/**
	 * Check if a root window is worth ripping
	 * 
	 * <p>
	 * 
	 * @param window
	 *            the window to consider
	 * @return true/false
	 */
	private boolean isValidRootWindow(Frame window) {

		// Check if window is valid
		// if (!window.isValid())
		// return false;

		// Check if window is visible
		if (!window.isVisible())
			return false;

		// Check if window is on screen
		// double nHeight = window.getSize().getHeight();
		// double nWidth = window.getSize().getWidth();
		// if (nHeight <= 0 || nWidth <= 0)
		// return false;

		return true;
	}

	/**
	 * 
	 * @see
	 * edu.umd.cs.guitar.ripper.RipperMonitor#isExpandable(edu.umd.cs.guitar
	 * .model.GXComponent, edu.umd.cs.guitar.model.GXWindow)
	 * 
	 * For this subclass, a component is expandable iff
	 * 1. Its title is not null, and not the empty string.
	 * 2. It is an enabled widget
	 * 3. It is a clickable widget.
	 * 4. It is not a terminal widget. 
	 * 5. It is a terminal widget. 
	 * 6. It contains an accessible text attribute. 
	 */
	@Override
   public 
	boolean isExpandable(GComponent gComponent, GWindow window) {

		JFCXComponent jComponent = (JFCXComponent) gComponent;
		// Accessible aComponent = jComponent.getAComponent();
		//
		// if (aComponent == null)
		// return false;

		Component component = jComponent.getComponent();
		AccessibleContext aContext = component.getAccessibleContext();

		String ID = gComponent.getTitle();
		if (ID == null)
			return false;

		if ("".equals(ID))
			return false;

		if (!gComponent.isEnable()) {
			GUITARLog.log.debug("Component is disabled");
			return false;
		}

		if (!isClickable(component)) {
			return false;
		}

		if (gComponent.getTypeVal().equals(GUITARConstants.TERMINAL))
			return false;

		// // Check for more details
		// AccessibleContext aContext = component.getAccessibleContext();

		if (aContext == null)
			return false;

		AccessibleText aText = aContext.getAccessibleText();

		if (aText != null)
			return false;

		return true;
	}

	/**
	 * Check if a component is click-able.
	 * 
	 * @param component
	 * @return true/false
	 */
	private boolean isClickable(Component component) {

		AccessibleContext aContext = component.getAccessibleContext();

		if (aContext == null)
			return false;

		AccessibleAction action = aContext.getAccessibleAction();

		if (action == null)
			return false;

		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.umd.cs.guitar.ripper.RipperMonitor#isIgnoredWindow(edu.umd.cs.guitar
	 * .model.GXWindow)
	 */
	@Override
	public boolean isIgnoredWindow(GWindow window) {
		String sWindow = window.getTitle();
		// TODO: Ignore template
		return (this.sIgnoreWindowList.contains(sWindow));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.umd.cs.guitar.ripper.RipperMonitor#isNewWindowOpened()
	 */
	@Override
	public boolean isNewWindowOpened() {
		return (tempOpenedWinStack.size() > 0);
		// return (tempGWinStack.size() > 0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.umd.cs.guitar.ripper.RipperMonitor#resetWindowCache()
	 */
	@Override
	public void resetWindowCache() {
		this.tempOpenedWinStack.clear();
		this.tempClosedWinStack.clear();
	}

	public class WindowOpenListener implements AWTEventListener {

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * java.awt.event.AWTEventListener#eventDispatched(java.awt.AWTEvent)
		 */
		@Override
		public void eventDispatched(AWTEvent event) {

			switch (event.getID()) {
			case WindowEvent.WINDOW_OPENED:
				processWindowOpened((WindowEvent) event);
				break;
			case WindowEvent.WINDOW_ACTIVATED:
			case WindowEvent.WINDOW_DEACTIVATED:
			case WindowEvent.WINDOW_CLOSING:
				processWindowClosed((WindowEvent) event);
				break;

			default:
				break;
			}

		}

		/**
		 * @param event
		 */
		private void processWindowClosed(WindowEvent wEvent) {
			Window window = wEvent.getWindow();
			tempClosedWinStack.add(window);
		}

		/**
		 * @param wEvent
		 */
		private void processWindowOpened(WindowEvent wEvent) {
			Window window = wEvent.getWindow();
			tempOpenedWinStack.add(window);
		}
	}

	Toolkit toolkit;

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.umd.cs.guitar.ripper.RipperMonitor#setUp()
	 */
	@Override
	public void setUp() {

		// Registering default supported events

		EventManager em = EventManager.getInstance();

		for (Class<? extends JFCEventHandler> event : JFCConstants.DEFAULT_SUPPORTED_EVENTS) {
			try {
				em.registerEvent(event.newInstance());
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// Registering customized supported event
		Class<? extends GEvent> gCustomizedEvents;

		String[] sCustomizedEventList;
		if (JFCRipperConfiguration.CUSTOMIZED_EVENT_LIST != null)
			sCustomizedEventList = JFCRipperConfiguration.CUSTOMIZED_EVENT_LIST
					.split(GUITARConstants.CMD_ARGUMENT_SEPARATOR);
		else
			sCustomizedEventList = new String[0];

		for (String sEvent : sCustomizedEventList) {
			try {
				Class<? extends GEvent> cEvent = (Class<? extends GEvent>) Class
						.forName(sEvent);
				em.registerEvent(cEvent.newInstance());
			} catch (ClassNotFoundException e) {
				GUITARLog.log.error(e);
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		// Set up parameters
		sIgnoreWindowList = JFCConstants.sIgnoredWins;

		// Start the application
		
		try {
			// jsaddler
			String[] URLs;
			if (JFCRipperConfiguration.URL_LIST != null)
				URLs = JFCRipperConfiguration.URL_LIST
						.split(GUITARConstants.CMD_ARGUMENT_SEPARATOR);
			else
				URLs = new String[0];
			URLs = JFCApplication2.convertToURLStrings(URLs);
			
			application = new JFCApplication2(JFCRipperConfiguration.MAIN_CLASS, JFCRipperConfiguration.LONG_PATH_TO_APP, JFCRipperConfiguration.USE_JAR, URLs);

			// Parsing arguments
			String[] args;
			if (JFCRipperConfiguration.ARGUMENT_LIST != null)
				args = JFCRipperConfiguration.ARGUMENT_LIST
						.split(GUITARConstants.CMD_ARGUMENT_SEPARATOR);
			else
				args = new String[0];
			final PrintStream originalOut = System.out;
			final PrintStream originalErr = System.err;
			
			application.connect(args);
			System.setOut(originalOut);
			System.setErr(originalErr);
			// Delay
			try {
				GUITARLog.log
						.info("Initial waiting: "
								+ JFCRipperConfiguration.INITIAL_WAITING_TIME
								+ "ms...");
				System.out.println("Initial waiting: "
								+ JFCRipperConfiguration.INITIAL_WAITING_TIME
								+ "ms...");
				Thread.sleep(JFCRipperConfiguration.INITIAL_WAITING_TIME);
			} catch (InterruptedException e) {
				GUITARLog.log.error(e);
			}

		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			GUITARLog.log.error(e);
			System.err.println(e);
		} catch (ApplicationConnectException e) {
			// TODO Auto-generated catch block
			GUITARLog.log.error(e);
			System.err.println(e);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			GUITARLog.log.error(e);
			System.err.println(e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.err.println(e);
		}

		// -----------------------------
		// Assign listener
		toolkit = java.awt.Toolkit.getDefaultToolkit();

		WindowOpenListener listener = new WindowOpenListener();
		toolkit.addAWTEventListener(listener, AWTEvent.WINDOW_EVENT_MASK);

	}

	/**
	 * 
	 * Add a root window to be ripped
	 * 
	 * <p>
	 * 
	 * @param sWindowName
	 *            the window name
	 */
	public void addRootWindow(String sWindowName) {
		this.sRootWindows.add(sWindowName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.umd.cs.guitar.ripper.GRipperMonitor#isWindowClose()
	 */
	@Override
	public boolean isWindowClosed()
   {
		return (tempClosedWinStack.size() > 0);
	}


   /**
    * Captures the image of a GUITAR GUI component
    * and saves it to a the specified image file.
    *
    * @param  component     GUITAR component to capture
    * @param  strFilePath   File path name to store the image
    *                          (w/o extension)
    * @return void
    */
   @Override
	public void
   captureImage(GObject component,
                String strFilePath)
   throws AWTException, IOException
	{
      BufferedImage image = null;
      try {
         JFCXComponent gComp = (JFCXComponent ) component;
         if (!gComp.getComponent().isShowing()) {
            throw new AWTException("Component is not visible");
         }

         Dimension dim = gComp.getComponent().getSize();

			// Ignore non-visible components
			if (dim.getHeight() == 0 ||
			    dim.getWidth() == 0) {
				throw new AWTException("Width or height is 0");
			}

         image = new BufferedImage(dim.width, dim.height,
                                   BufferedImage.TYPE_INT_ARGB);
         Graphics2D g2 = image.createGraphics();
         gComp.getComponent().paint(g2);
         g2.dispose();

			File outputfile = new File(strFilePath + ".png");
			ImageIO.write(image, "png", outputfile);
			GUITARLog.log.info("Saved image in " + strFilePath);
			System.gc();
		} catch (IOException e) {
			GUITARLog.log.error(e.getMessage());
	      throw e;
      } catch (AWTException e) {
			GUITARLog.log.error(e.getMessage());
         throw e;
  		} catch (OutOfMemoryError e) {
			GUITARLog.log.error("OutOfMemoryError converted to AWTException");
			throw new AWTException("Out of memory");
		}
 
	}

} // End of class
