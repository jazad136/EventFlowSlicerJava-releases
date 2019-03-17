/*******************************************************************************
 *    Copyright (c) 2018 Jonathan A. Saddler
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *    
 *    Contributors:
 *     Jonathan A. Saddler - initial API and implementation
 *******************************************************************************/
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
