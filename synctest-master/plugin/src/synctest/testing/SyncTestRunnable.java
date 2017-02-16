package synctest.testing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URL;

public class SyncTestRunnable implements Runnable {
	SyncTestRunner runner;
	
	public SyncTestRunnable(SyncTestRunner runner) {
		this.runner = runner;
	}
	
	@Override
	public void run() {
		String baseDir = runner.getBaseDir();
		String sourceDir = runner.getSourceDir();
		String testDir = runner.getTestDir();
		int runCount = runner.getRunCount();
		double sleepAmnt = runner.getSleepAmnt();
				
		try {
			// Create a process builder for running the shell script
			ProcessBuilder pb = new ProcessBuilder("/bin/bash", "synctest.sh", baseDir, sourceDir, testDir, 
					Integer.toString(runCount), Double.toString(sleepAmnt));
			
			URL location = getClass().getProtectionDomain().getCodeSource().getLocation();
			pb.directory(new File(location.getFile()+"src/synctest"));
			Process p = pb.start();
			
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			
			// Collect output from the shell script, write it to the output file
			while ((line = br.readLine()) != null) {
				BufferedWriter bw = new BufferedWriter(new FileWriter(
						location.getFile()+"src/synctest/testing/syncTestOutput.txt", true));
				bw.append(line+"\n");
				bw.close();
			}
			
			br.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
