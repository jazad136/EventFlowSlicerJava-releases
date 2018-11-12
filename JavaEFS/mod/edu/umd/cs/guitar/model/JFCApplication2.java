/*
 *  Copyright (c) 2009-@year@. The GUITAR group at the University of Maryland. Names of owners of this group may
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
package edu.umd.cs.guitar.model;

import java.awt.Window;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import edu.umd.cs.guitar.exception.ApplicationConnectException;

/**
 * Implementation for {@link GApplication} for Java Swing
 *
 * @see GApplication
 *
 * @author <a href="mailto:baonn@cs.umd.edu"> Bao Nguyen </a>
 */
public class JFCApplication2 extends GApplication
{
	// Run on input: /Users/jsaddle/apps/CrosswordSage/CrosswordSage.jar
	// Run on input: /Users/jsaddle/apps/0_OlderApps/RadioButton/bin/Project.class
//	public static void main(String[] args)
//	{
//		try (Scanner scanner = new Scanner(System.in)) {
//			System.out.println("Enter classpath followed by Jar or Class file.");
//			String input = scanner.nextLine();
//			String[] splitInput = input.split("\t\n\r\f");
//
//			if((new File(splitInput[splitInput.length-1])).exists())
//				new JFCApplication2(splitInput[splitInput.length-1], true, true);
//		}
//		catch(Exception e) {
//			throw new RuntimeException(e);
//		}
//	}

	private static final String[] URL_PREFIX = { "file:", "jar:", "http:" };
	public static final int iInitialDelay = 3000;
//	private HashSet<Window> unrelatedWindows;
	private Class<?> cClass;
	private List<URL> appURLs;
	private final String appEntrance;
	@SuppressWarnings("unused")
	private final String packageRoot;
	/**
	 * Application with jar file
	 * jsaddler: Application URL's are elements that have to be added to the classpath in order
	 * for the jar or class file stated in entrance can be run.
	 *
	 * This array should never be empty, unless the class we're
	 * attempting to load has already been loaded from the current classpath.
	 *
	 * the Jar or Class file specified in entrance MUST contain a main method.
	 *
	 * @param entrance 	either main jar file path or main class name
	 * @param useJar 	true if <code> entrance </code> is a jar file
	 * @param URLs	 	application URLs
	 *
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */



	public JFCApplication2(String simpleEntrance, boolean useJar, String[] URLs)
			throws ClassNotFoundException, IOException
	{
//		this(concatenatePathFromElements(fullPathToMainClass, simpleEntrance), useJar, URLs);
		this(simpleEntrance, simpleEntrance, useJar, URLs);
	}

	public JFCApplication2(String entrance, String fullPathToApp, boolean useJar, String[] URLs)
			throws ClassNotFoundException, IOException {
		super();

		// main class name to start the application
		String mainClass;

		// check if main class is loaded from a jar file

		if (useJar) {
			appEntrance = entrance;
			packageRoot = entrance;
			InputStream is;
			is = new FileInputStream(fullPathToApp);
			JarInputStream jarStream = new JarInputStream(is);
			Manifest mf = (Manifest) jarStream.getManifest();

//			for(ManifestDataEntry e : ManifestDataRipper.getAllManifestData(mf))
//				System.out.println(e);
			mainClass = mf.getMainAttributes().getValue("Main-Class");
			if(mainClass == null || mainClass.isEmpty())
				mainClass = entrance;

			jarStream.close();
			this.cClass = initializeMainClass(mainClass, URLs);
		}
		else {
			this.cClass = initializeMainClass(entrance, URLs);
			appEntrance = entrance;
			packageRoot = fullPathToApp.substring(0, fullPathToApp.length() - entrance.length());
		}
	}

//	public JFCApplication2(String entrance, boolean useJar, boolean parseManifest)
//			throws ClassNotFoundException, IOException
//	{
//		super();
//		appEntrance = entrance;
//		packageRoot = entrance;
//		String mainClass;
//		ArrayList<String> listOfUrls = new ArrayList<String>();
//		// a comprehensizse jar file parsing: jsaddler
//		if (useJar) {
//			InputStream is;
//			is = new FileInputStream(entrance);
//			JarInputStream jarStream = new JarInputStream(is);
//			Manifest mf = (Manifest) jarStream.getManifest();
//
//			mainClass = "";
//			for(ManifestDataEntry e : ManifestDataRipper.getAllManifestData(mf)) {
//				if(e.getKey().equals("Main-Class"))
//					mainClass = e.getValue();
//
//				String entrancePath = parseApplicationPath(appEntrance, true);
//				if(e.getKey().equals("Class-Path")) {
//					StringTokenizer classPathVars = new StringTokenizer(e.getValue());
//					while(classPathVars.hasMoreTokens()) {
//						String nextCP = classPathVars.nextToken();
//						listOfUrls.add("file:" + entrancePath + nextCP);
//					}
//				}
//			}
//
//			jarStream.close();
//			if(mainClass.isEmpty())
//				throw new ClassNotFoundException();
//		}
//		else {
//			mainClass = entrance;
//		}
//		listOfUrls.add("file:" + entrance);
//		this.cClass = initializeMainClass(mainClass, listOfUrls.toArray(new String[listOfUrls.size()]));
//	}

	/**
	 * Initializes the main class field in this application instance
	 * and opens the application windows if they exist.
	 *
	 * @param sClassName
	 * @param sURLs
	 * @return
	 * @throws MalformedURLException if strings within the sURLs represent invalid URL strings
	 * @throws ClassNotFoundException if the class referred to by sClassName cannot be found on the local file system
	 */
	@SuppressWarnings("resource")
	private Class<?> initializeMainClass(String sClassName, String[] sURLs)
			throws MalformedURLException, ClassNotFoundException
	{
		URLClassLoader sysLoader =
				(URLClassLoader)ClassLoader.getSystemClassLoader();
		RuntimeJarFileLoader myLoader = new RuntimeJarFileLoader(sysLoader);
		appURLs = new LinkedList<URL>();

		String classToFind = sClassName;
		if(classToFind.isEmpty())
			throw new RuntimeException("JFCApplication2: Empty Class file provided to initialize method");
		if(classToFind.charAt(0) == File.separatorChar)
			classToFind = classToFind.substring(1);
		classToFind = classToFind.replace(File.separator,".");

		// Additional URLs passed by arguments
		for (String sURL : sURLs) {
			for (String pref : URL_PREFIX) {
				// if the URL is valid.
				if (sURL.startsWith(pref)) {
					// append it to the list of url's
					URL appURL = new URL(sURL);
					appURLs.add(appURL);
					break;
				}
			}
		}
		for(URL locator : appURLs)
			myLoader.addURL(locator);

//		myLoader = myLoader.newOnTop();
		Field sysLField;
		try{sysLField = ClassLoader.class.getDeclaredField("scl");} // Get system class loader
		catch(NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
		sysLField.setAccessible(true); // Set accessible
		try {sysLField.set(null, myLoader);}
		catch(IllegalAccessException e) {
			throw new RuntimeException(e);
		}

		// get the class.
		return Class.forName(classToFind, true, myLoader);
	}

	public static String getFullClassName(String classFileName, RuntimeJarFileLoader cl) throws IOException {
        File file = new File(classFileName);

        FileChannel roChannel;
        try(RandomAccessFile rac = new RandomAccessFile(file, "r");) {
        	roChannel = rac.getChannel();
        	ByteBuffer bb = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, (int)roChannel.size());
        	Class<?> theClass = cl.getClass((String)null, bb, (ProtectionDomain)null);
        	return theClass.getName();
        }
    }

	/**
	 * Return the name of the runtime package of the class found.
	 * @return
	 */
	public String getPackageName()
	{
		String x = cClass.getName();
		int index = x.lastIndexOf(".");
		if(index != -1) {
	        String y = x.substring(0, x.lastIndexOf("."));
	        if (y.length() > 0)
//	            System.out.println("package "+y+";\n\r");
	        	return y;
		}
		return "";
	}
	/**
	 * Starts the application under test using reflection on the class or jar file provided.
	 * @see edu.umd.cs.guitar.model.GApplication#start()
	 */
	@Override
	public void connect() throws ApplicationConnectException {
		String[] args = new String[0];
		connect(args);
	}

	/**
	 * Starts the application under test using reflection on the class or jar file provided.
	 * @see edu.umd.cs.guitar.model.GApplication#connect(java.lang.String[])
	 */
	@SuppressWarnings("unused")
	@Override
	public void connect(String[] args) throws ApplicationConnectException {

		Method mainMethod;

		String userDir = System.getProperty("user.dir");
		String appPath = getAppPathViaCodeSource();
		setCurrentWorkingDirectory(appPath);
		userDir = System.getProperty("user.dir");

		try {
			mainMethod = cClass.getMethod("main", new Class[] { String[].class });
			if (mainMethod != null)
				mainMethod.invoke(null, new Object[] { args });
			else
				throw new ApplicationConnectException();
		}
		catch (NoSuchMethodException e) {
			System.out.println("Coundn't find main method for the application");
		}
		catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean disconnect()
	{
		// System URLs
		URLClassLoader sysLoader = (URLClassLoader) ClassLoader
				.getSystemClassLoader();

		ArrayList<URL> urls = new ArrayList<URL> (Arrays.asList(sysLoader.getURLs()));

		urls.removeAll(appURLs);


		// turn the list into an array
		URL[] arrayNewURLs = (urls.toArray(new URL[urls.size()]));
		// ---------------

		// pass the new array to the class loader.
		ClassLoader loader = URLClassLoader.newInstance(arrayNewURLs, sysLoader);

		// put the class name in correct format, separated by dots and not slashes.
		String sClassName = this.cClass.getCanonicalName().replace("/",".");

		// get the class.
		boolean worked = false;
		try {
			Class.forName(sClassName, false, loader);
		} catch(ClassNotFoundException e) {
			worked = true;
		}
		return worked;
	}

	/**
	 * Attempts to get as many unique java.awt.Window instances from this Java Runtime as possible
	 * in the current state and return them as a set of GWindows.
	 * Utilizes Window.getWindows and Window.getAllOwnedWindows() from the
	 * java.awt.Window class.
	 *
	 * @see edu.umd.cs.guitar.model.GApplication#getAllWindow()
	 */
	@Override
	public Set<GWindow> getAllWindow()
	{
//		Frame[] windows = Frame.getFrames(); -- jsaddler: this caused so many problems....
		Window[] windows = Window.getWindows();
		Set<GWindow> retWindows = new HashSet<GWindow>();

		for(Window aWindow : windows) {
			GWindow gWindow = new JFCXWindow(aWindow);
			if (gWindow.isValid())
				retWindows.add(gWindow);
			Set<GWindow> lOwnedWins = getAllOwnedWindow(aWindow);

			for (GWindow aOwnedWins : lOwnedWins)
				if (aOwnedWins.isValid())
					retWindows.add(aOwnedWins);
		}
		return retWindows;
	}

	private Set<GWindow> getAllOwnedWindow(Window parent)
	{
		Set<GWindow> retWindows = new HashSet<GWindow>();
		Window[] lOwnedWins = parent.getOwnedWindows();
		for (Window aOwnedWin : lOwnedWins) {
			retWindows.add(new JFCXWindow(aOwnedWin));
			Set<GWindow> lOwnedWinChildren = getAllOwnedWindow(aOwnedWin);

			retWindows.addAll(lOwnedWinChildren);
		}
		return retWindows;
	}

	/**
	 * Return the class in this JFC application that contains the main method
	 * of the application. This main class is called when
	 * connect() is invoked.
	 *
	 * Preconditions: 	(none)
	 * Postconditions: 	The main class of this JFCApplication
	 * 					containing the main method to be invoked
	 * 					is returned from the function.
	 */
	public Class<?> getJavaMainClass()
	{
		return cClass;
	}

//	public static void testAutoLoad(String[] args)
//	{
//		java.util.Scanner scanner = new java.util.Scanner(System.in);
//		System.out.println("Please enter a jar filename.");
//		String entrance = scanner.nextLine();
//
//		try {
//			JFCApplication2 myJFCA = new JFCApplication2(entrance, true, true);
//			System.out.println(myJFCA.getAppPathViaCodeSource());
//			myJFCA.connect();
//		}
//		catch(IOException e)
//		{
//			scanner.close();
//			throw new RuntimeException("Could not load JFCApplication: IOException:\n"
//					+ e.getCause() + "\n" + e.getMessage());
//		}
//		catch(ClassNotFoundException e)
//		{
//			scanner.close();
//			throw new RuntimeException("Could not find main class.\n"
//					+ e.getCause() + "\n" + e.getMessage());
//		}
//		scanner.close();
//	}

	public static String[] convertToURLStrings(String... paths) throws MalformedURLException
	{
		String[] toReturn = new String[paths.length];

//		for (int i = 0; i < paths.length; i++) {
//			toReturn[i] = (new File(paths[i]).toURI().toURL().toString());
//
//		}

		for (int i = 0; i < paths.length; i++) {
			toReturn[i] = (new File(paths[i]).toURI().toURL().toString());
			int extPos = paths[i].lastIndexOf('.');
			if(extPos != -1 && paths[i].substring(extPos).equals(".jar"))
				toReturn[i] = "jar:" + toReturn[i] + "!/";
		}
		return toReturn;
	}



	public String getAppPathViaCodeSource()
	{
		String toReturn = "";
		URL classURL = cClass.getProtectionDomain().getCodeSource().getLocation();
		toReturn = classURL.getFile();
		return toReturn;
	}


	/**
	 * Set the current working directory to point to newDirectoryName
	 * @param newDirectoryName
	 * @return
	 */
	public static boolean setCurrentWorkingDirectory(String newDirectoryName)
	{
		boolean result = false;
		File directory;

        directory = new File(newDirectoryName).getAbsoluteFile();
        if (directory.exists()) {
        	String absolutePath = directory.getAbsolutePath();
        	result = (System.setProperty("user.dir", absolutePath) != null);
        }


        return result;
	}

	public void testMain(String[] args) throws ClassNotFoundException
	{
		// open a terminal interaction session, and scan terminal
		java.util.Scanner scanner = new java.util.Scanner(System.in);
		System.out.println("Please enter a jar filename.");
		String entrance = scanner.nextLine();
		System.out.println("Any URL's? Type (no) when done");

		// scan for urls
		ArrayList<String> urls = new ArrayList<String>();
		try {
			while(true) {
				String response = scanner.nextLine();
				if(response.equals("(no)"))
					break;
				URL resourceURL = (new File(response)).toURI().toURL();
				urls.add(resourceURL.toString());
				System.out.println("Any URL's? Type (no) when done");
			}
		}
		catch(MalformedURLException e) {
			scanner.close();
			throw new RuntimeException("Malformed URL: \n" + e.getCause() + "\n" + e.getMessage());
		}
		// done scanning.

		System.out.println("--Main class retrieved.--\n");

		try {
			JFCApplication2 myJFCA = new JFCApplication2(entrance, entrance, true, urls.toArray(new String[0]));
			myJFCA.connect();
		}
		catch(MalformedURLException e) {
			scanner.close();
			throw new RuntimeException("Malformed URL: \n" + e.getCause() + "\n" + e.getMessage());
		}
		catch(IOException e) {
			scanner.close();
			throw new RuntimeException("Could not load JFCApplication: IOException: \n" + e.getCause() + "\n" + e.getMessage());
		}

		Runtime.getRuntime().traceMethodCalls(true);
		Runtime.getRuntime().traceInstructions(true);
		System.out.println("--Application connnected.--\n");

		scanner.close();
	}

	public static String parseApplicationPath(String rawPath, boolean includeSlash)
	{
		File file = new File(rawPath);
		String toReturn = file.getPath();
		int slashPos = toReturn.lastIndexOf(File.separatorChar);
		if(slashPos != -1) {
			if(includeSlash)
				toReturn = toReturn.substring(0, slashPos+1);
			else
				toReturn = toReturn.substring(0, slashPos);
		}

		return toReturn;
	}


	private class RuntimeJarFileLoader extends URLClassLoader {
		URL[] oldURLs;
		ArrayList<URL> all;
		ArrayList<URL> added;
		public RuntimeJarFileLoader(URLClassLoader classLoader)
		{
			super(classLoader.getURLs());
			URL[] urls = classLoader.getURLs();
			all = new ArrayList<URL>();
			oldURLs = new URL[urls.length];
			for(int i = 0; i < oldURLs.length; i++) {
				oldURLs[i] = urls[i];
				all.add(urls[i]);
			}

			added = new ArrayList<URL>();

//			super(classLoader.getURLs());
		}


		protected RuntimeJarFileLoader(URL[] URLs)
		{
			super(URLs);
			added = new ArrayList<URL>();
			all = new ArrayList<URL>(Arrays.asList(URLs));
		}

		public Class<?> getClass(String s, ByteBuffer b, ProtectionDomain pd)
		{
			return defineClass(s, b, pd);
		}

		@Override
		public void addURL(URL url)
		{
			super.addURL(url);
			added.add(url);
			all.add(url);
		}


	}
}
