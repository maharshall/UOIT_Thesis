package synctest.testing;

/**
 * This class is used to pass values between the UI (SyncTestView)
 * and the runnable class for running test (SyncTestRunnable)
 * 
 * @author Alexander Marshall
 * */
public class SyncTestRunner {

	private String baseDir, sourceDir, testDir;
	private double sleepAmnt;
	private int runCount;
	
	/**
	 * Constructor for the class
	 * 
	 * @param base 		The base directory for the testing project
	 * @param source	The testing project directory for source code
	 * @param test		The testing project directory for test files
	 * @param sleep		The amount of seconds to sleep between deadlock checks
	 * @param runs		The amount of times to run each test
	 * */
	public SyncTestRunner(String base, String source, String test, String sleep, String runs) {
		this.baseDir = base;
		this.sourceDir = source;
		this.testDir = test;
		this.sleepAmnt = Double.valueOf(sleep);
		this.runCount = Integer.valueOf(runs);
	}
	
	/**
	 * @return 		The base directory of the testing project
	 * */
	public String getBaseDir() {
		return baseDir;
	}
	
	/**
	 * @return		The source code directory of the testing project
	 * */
	public String getSourceDir() {
		return sourceDir;
	}
	
	/**
	 * @return 		The test code directory of the testing project
	 * */
	public String getTestDir() {
		return testDir;
	}
	
	/**
	 * @return 		The amount of seconds to sleep between deadlock checks
	 * */
	public double getSleepAmnt() {
		return sleepAmnt;
	}
	
	/**
	 * @return 		The amount of times to run each test
	 * */
	public int getRunCount() {
		return runCount;
	}
}
