/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package edu.unl.cse.efs.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import static java.nio.file.FileVisitResult.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Tools to help lookup files on a file system using a glob (*) character, 
 * in a predictable manner
 * @author Jonathan A. Saddler
 */
public class WildcardFiles {
	public static void main(String[] args)
	{
		try {
			for(File f : findFiles("TC/o", "2.tst")) {
				System.out.println(f);
			};
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static List<File> findFiles(String preWildCardString, String postWildCardString) throws IOException
	{
		final ArrayList<File> found = new ArrayList<File>();
		String pathToFile;
		String appPath = PathConformance.parseApplicationPath(preWildCardString);
		if(preWildCardString.isEmpty() || appPath.isEmpty()) 
			pathToFile = System.getProperty("user.dir");	
		else
			pathToFile = appPath;
		
		FileSystem fileSystem = FileSystems.getDefault();

		String globString = preWildCardString + "**" + postWildCardString;
		final PathMatcher myMatcher = fileSystem.getPathMatcher(
						"glob:" + globString);
		Path thisPath = Paths.get(pathToFile);
		Files.walkFileTree(thisPath, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
				if(!attrs.isDirectory() || !attrs.isSymbolicLink())
					if(myMatcher.matches(file))
						found.add(file.toFile());
				return CONTINUE;
			}
		 
			// Invoke the pattern matching
			// method on each directory.
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
				return CONTINUE;
			}
		 
			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) {
				System.err.println(exc);
				return CONTINUE;
			}
		});
		return found;
	}

	public static class Old
	{
//		else {
//		try {
//		Files.walkFileTree(Paths.get(outputDirectory), new SimpleFileVisitor<Path>(){
//			public FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException{
//				Files.delete(file);
//				return FileVisitResult.CONTINUE;
//			}
//			public FileVisitResult postVisitDirectory(), new SimpleFileVisitor
//		});
//		} catch(IOException e) {
//			throw new RuntimeException(e);
//		}
//	}
	}
}
