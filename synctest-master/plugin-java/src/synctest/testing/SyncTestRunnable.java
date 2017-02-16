package synctest.testing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * This is the runnable class for running tests.
 * It gets it's input from SyncTestRunner and writes
 * test output to the syncTestOutput.txt file
 * 
 * @author Alexander Marshall
 * */
public class SyncTestRunnable implements Runnable {
	SyncTestRunner 	runner;
	String 			baseDir, sourceDir, testDir, workingDir, os, cmd, arg;
	String[]		java, cmdString;
	int 			runCount;
	double 			sleepAmnt;
	URL 			location;
	BufferedWriter 	bw, bw2;
	Thread			checkDeadlock;
	
	/**
	 * Constructor for the class
	 * 
	 * @param runner	An instance of SyncTestRunner with the 
	 * 					values from the config tab of the UI
	 * 
	 * @see 			getCommandPaths()
	 * */
	public SyncTestRunnable(SyncTestRunner runner) {
		this.runner = runner;
		baseDir = runner.getBaseDir();
		sourceDir = runner.getSourceDir();
		testDir = runner.getTestDir();
		runCount = runner.getRunCount();
		sleepAmnt = runner.getSleepAmnt();
		location = getClass().getProtectionDomain().getCodeSource().getLocation();
		os = System.getProperty("os.name").toLowerCase();
		
		// set some commands based on the OS
		if(os.contains("win")) {
			cmd = "cmd";
			arg = "/c";
			java = getCommandPaths("win");
		} else if(os.contains("lin") || os.contains("mac")) {
			cmd = "bash";
			arg = "-c";
			java = getCommandPaths("lin");
		} else {
			System.out.println("OS Not Supported! Sorry.");
			System.exit(-1);
		}
	}
	
	/**
	 * The actual method which runs tests
	 * 
	 * @see 		getPackageName()
	 * @see			CheckDeadlock.java
	 * */
	@Override
	public void run() {		
		try {
			// Create output directory or clear it if it exists
			File outDir = new File(baseDir+"/out");
			if(!outDir.exists()) {
				outDir.mkdir();
			} else {
				for(File file: outDir.listFiles()) file.delete();
			}
			
			// Get array of test files from the directory
			File[] testFiles = new File(testDir).listFiles(new FilenameFilter() {
				public boolean accept(File f, String s) {
					if(s.contains("java")) return true;
					return false;
				}
	    	});
			
			// Check for a package and set the working directory accordingly
			String packTest = getPackageName(testFiles[0]);
			if(packTest == null) {
				workingDir = baseDir;
			} else {
				if(os.contains("win")) {
					workingDir = baseDir+"\\..";
				} else {
					workingDir = baseDir+"/..";
				}
			}
			
			// Set the command for running the tests
			if(os.contains("win")) {
				cmdString = new String[]{"\""+java[1]+"\"","-cp", "junit-4.12.jar;hamcrest-core-1.3.jar;"+workingDir, "org.junit.runner.JUnitCore"};
			} else if(os.contains("lin") || os.contains("mac")) {
				cmdString = new String[]{java[1],"-cp", "junit-4.12.jar:hamcrest-core-1.3.jar:.", "org.junit.runner.JUnitCore"};
			}
			
			File junit = new File(location.getFile()+"src/synctest/junit-4.12.jar");
			File hamcrest = new File(location.getFile()+"src/synctest/hamcrest-core-1.3.jar");
			
			// Copy jar files to the testing directory
			Path moveFrom = FileSystems.getDefault().getPath(junit.getAbsolutePath());
			Path moveTo = FileSystems.getDefault().getPath(workingDir+"/"+junit.getName());
			Files.copy(moveFrom,  moveTo, StandardCopyOption.REPLACE_EXISTING);
			moveFrom = FileSystems.getDefault().getPath(hamcrest.getAbsolutePath());
			moveTo = FileSystems.getDefault().getPath(workingDir+"/"+hamcrest.getName());
			Files.copy(moveFrom,  moveTo, StandardCopyOption.REPLACE_EXISTING);
			
			// Compile all source and test files
			ProcessBuilder compile = new ProcessBuilder(cmd, arg, "\""+java[0]+"\" -cp junit-4.12.jar "+sourceDir+"/*.java "+testDir+"/*.java");
			compile.directory(new File(workingDir));
			Process compileProc = compile.start();
			
			// Make sure everything is compiled before moving on
			compileProc.waitFor();
			compileProc.destroy();
			
			outerLoop:
			for(int t = 0; t < testFiles.length; t++) {
				// Cancel button pressed, stop what we're doing
				if(Thread.currentThread().isInterrupted()) break;
				String pack = getPackageName(testFiles[t]);
				String test = testFiles[t].getName().split("\\.")[0];
				
				// Write to output file so UI thread can read it
				bw = new BufferedWriter(new FileWriter(location.getFile()+"src/synctest/testing/syncTestOutput.txt", true));
				bw.append("Running: "+test+"\n");
				bw.close();
				
				
				for(int i = 1; i <= runCount; i++) {
					if(Thread.currentThread().isInterrupted()) break outerLoop;
					ProcessBuilder run = new ProcessBuilder(cmd, arg, cmdString[0]+" "+cmdString[1]+" "+cmdString[2]+" "+cmdString[3]+" "+pack+"."+test);
					
					run.directory(new File(workingDir));
					
					// Start the process for running the test as well as the deadlock detection process
					Process runProc = run.start();
					checkDeadlock = new Thread(new CheckDeadlock(outDir.toString(), test, i, runCount, sleepAmnt, runProc));
					checkDeadlock.start();
					BufferedReader br = new BufferedReader(new InputStreamReader(runProc.getInputStream()));
					String line;

					bw2 = new BufferedWriter(new FileWriter(outDir+"/"+test+"-"+i+".txt"));
					bw2.append("<"+test+" -- "+i+">");
					bw2.close();
					
					// Get the output for the test and write it to the output file
					while ((line = br.readLine()) != null) {
						// User has pressed the cancel button
						if(Thread.currentThread().isInterrupted()) break outerLoop;
						
						bw = new BufferedWriter(new FileWriter(location.getFile()+"src/synctest/testing/syncTestOutput.txt", true));
						bw2 = new BufferedWriter(new FileWriter(outDir+"/"+test+"-"+i+".txt", true));
						bw2.append(line+"\n");
						
						// If a deadlock is found it will get written to the output file by the checkDeadlock thread
						if(line.contains("OK")) {
							bw.append("Execution "+i+"/"+runCount+" Passed\n");
						} else if(line.contains("Failures")) {
							bw.append("Execution "+i+"/"+runCount+" Failed\n");
						}
						
						bw.close();
						bw2.close();
					}
					
					// Wait until the test is finished before moving on
					// Stop the deadlock detection thread if it is still running
					runProc.waitFor();
					runProc.destroy();
					checkDeadlock.interrupt();
					br.close();
				}
				
				// Let the UI know the tests are finished
				bw = new BufferedWriter(new FileWriter(location.getFile()+"src/synctest/testing/syncTestOutput.txt", true));
				bw.append(test+" Completed\n");
				bw.close();
			}
			
			// Be absolutely sure the deadlock detection thread is killed
			checkDeadlock.interrupt();
			bw = new BufferedWriter(new FileWriter(location.getFile()+"src/synctest/testing/syncTestOutput.txt", true));
			bw.append("all tests finished\n");
			bw.close();
			
			// Remove jar files from the testing directory
			moveFrom = FileSystems.getDefault().getPath(workingDir+"/"+junit.getName());
			Files.delete(moveFrom);
			moveFrom = FileSystems.getDefault().getPath(workingDir+"/"+hamcrest.getName());
			Files.delete(moveFrom);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * A method to to get locations of the javac and java
	 * commands based on the user's OS
	 * 
	 * @param os	A string containing "lin", "mac", or "win"
	 * 
	 * @return 		A string array of the javac and java locations*/
	public String[] getCommandPaths(String os) {
		try {
			if(os.equals("win")) {
				// Use the Windows 'where' command to find javac and java
				Process where = (new ProcessBuilder("where", "javac")).start();
				BufferedReader br = new BufferedReader(new InputStreamReader(where.getInputStream()));
				String javac = br.readLine();
				br.close();
				where.destroy();
				
				where = (new ProcessBuilder("where", "java")).start();
				br = new BufferedReader(new InputStreamReader(where.getInputStream()));
				String java = br.readLine();
				br.close();
				where.destroy();
				
				return new String[] {javac, java};
			} else if(os.equals("lin")) {
				// Use the UNIX 'which' command to find javac and java
				Process which = (new ProcessBuilder("which", "javac")).start();
				BufferedReader br = new BufferedReader(new InputStreamReader(which.getInputStream()));
				String javac = br.readLine();
				br.close();
				which.destroy();
				
				which = (new ProcessBuilder("which", "java")).start();
				br = new BufferedReader(new InputStreamReader(which.getInputStream()));
				String java = br.readLine();
				br.close();
				which.destroy();
				
				return new String[] {javac, java};
			} else {
				return null;
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * A method to check the first n lines of a file for
	 * a package name. I tried to find a more elegant solution
	 * but this is what I came up with
	 * 
	 * @param file	The file to check for a package name
	 * 
	 * @return		The package name
	 * */
	public String getPackageName(File file) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			int i = 0; String line;
			while((line = br.readLine()) != null && i < 50) {
				i++;
				if(line.contains("package")) {
					String pack = line.split(" ")[1];
					pack = pack.substring(0,  pack.length()-1);
					br.close();
					return pack;
				}
			}
			br.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
}
