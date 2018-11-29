package edu.unl.cse.efs.util;

/**
 * Operating system detector
 * @author Jonathan Saddler (adapted from ideas by Amanda Swearngin)
 */
public class OSDetector {
	public static enum Type {WINDOWS, MACINTOSH, SOLARIS, LINUX, UNKNOWN};
	
	public static Type parseOSName()
	{	
		String osName = System.getProperty("os.name").toLowerCase();
		if(osName.contains("win")) 			return Type.WINDOWS;
		else if(osName.contains("mac"))		return Type.MACINTOSH;
		else if(osName.contains("nix") || osName.contains("nux"))	return Type.LINUX;
		else if(osName.contains("sunos"))	return Type.SOLARIS;
		return Type.UNKNOWN;
	}
	
	
	public static boolean isWindows()
	{
		String osName = System.getProperty("os.name").toLowerCase();
		return osName.contains("win");
	}
	public static boolean isMac()
	{
		String osName = System.getProperty("os.name").toLowerCase();
		return osName.contains("mac");
	}
	
	public static boolean isUnix()
	{
		String osName = System.getProperty("os.name").toLowerCase();
		return osName.contains("nix") || osName.contains("nux");
	}
	
}
