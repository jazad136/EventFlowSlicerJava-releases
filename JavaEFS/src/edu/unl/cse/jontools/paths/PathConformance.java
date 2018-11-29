package edu.unl.cse.jontools.paths;

import java.io.File;

import edu.unl.cse.jontools.string.StringTools;

public class PathConformance {
	/**
	 * Return the path part of the application path specified, 
	 * with separator char appended to the end.
	 * 
	 * Preconditions: 	rawPath is a valid path name
	 * Postconditions: 	The path is returned with the separator '/'|'\' appended to the end. 
	 */
	public static String parseApplicationPath(String rawPath)
	{
		File file = new File(rawPath);
		String toReturn = file.getPath();
		toReturn = toReturn.substring(0, toReturn.lastIndexOf(File.separatorChar)+1);
		return toReturn;
	}
	
	/**
	 * Return the filename part of the application path specified. This includes
	 * just the name of the file specified by raw path (no dots or separators).
	 * 
	 * Preconditions: raw path is a valid path name. 
	 * Postconditions: The application name is returned without any separators or extension data.  
	 * @param rawPath
	 * @return
	 */
	public static String parseApplicationName(String rawPath)
	{
		// get the filename
		File file = new File(rawPath);
		String toReturn = file.getName();
		// remove the extension.
		if(toReturn.contains("."))
			toReturn = toReturn.substring(0, toReturn.lastIndexOf('.'));
		// return the application name without the extension.
		return toReturn;
	}
	
	/**
	 * Return the extension part of the application path specified. 
	 * 
	 * Preconditions: 	rawPath is a valid path name 
	 * Postconditions: 	The extension specifier is returned containing the dot and the extension name
	 * 					if rawPath contains one. Otherwise, the empty string is returned. 
	 * @param rawPath
	 * @return
	 */
	public static String parseApplicationExtension(String rawPath)
	{
		File file = new File(rawPath);
		String toReturn = file.getPath();
		if(toReturn.contains(".")) 
			return toReturn.substring(toReturn.lastIndexOf('.'));
		else 
			return "";
	}
	
	/**
	 * Remove any unnecessary characters from this filename that
	 * cannot be read in by a DOT file.
	 * @param potential
	 * @return
	 */
	public static String sanitizeFilenameStringForDOT(String potential)
	{
		potential = potential.replace("\\", "");
		potential = potential.replace(".", "");
		potential = potential.replace("/", "");
		potential = potential.replace(":", "");
		potential = potential.replace(";", "");
		potential = potential.replace("%", "");
		return potential;
	}
	/**
	 * Concatenates a path from the partial elements of a long path provided as arguments. If the final character 
	 * at the end of any one of the elements is not a file separator character, it is appended between elements.
	 * No character is ever appended to the end of the last element.
	 * @param elements
	 * @return
	 */
	public static String concatenatePathFromElements(String... elements)
	{
		String toReturn = "";
		if(elements.length > 0) {
			toReturn = elements[0];
			for(int i = 1; i < elements.length; i++) 
				if(toReturn.charAt(toReturn.length()-1) != File.separatorChar) 
					toReturn += File.separator + elements[i];
				else 
					toReturn += elements[i];
		}
		return toReturn;
	}
	
	/**
	 * Reduce a raw path to only what characters occur before the last slash found in the filename
	 * @param rawPath
	 * @return
	 */
	public static String pathShrinker(String rawPath)
	{
		if(StringTools.charactersIn(rawPath, File.separatorChar) >= 1) {
			int lastSepPos = rawPath.lastIndexOf(File.separatorChar); 
			return rawPath.substring(0, lastSepPos + 1); // only take what happens before the last slash.
		}
		return rawPath;
	}
	
	/**
	 * Reduce a path designated by a file object to refer to the root of the application, with the help from
	 * content of the package class name.
	 * @param path
	 * @param packageClassName
	 * @return
	 */
	public static String packageSensitiveApplicationLocation(File path, String packageClassName)
	{
		if(path == null || path.getAbsolutePath().isEmpty())
			return "";
		String locationString = parseApplicationPath(path.getAbsolutePath());
		String appName = parseApplicationName(path.getAbsolutePath());
		
		packageClassName = packageClassName.replace(".",File.separator);
		int pLoc = (path + appName).indexOf(packageClassName);
		if(pLoc != -1)
			locationString = locationString.substring(0, pLoc); // trim off the beginning of the full class string.
		return locationString;
	}
}
