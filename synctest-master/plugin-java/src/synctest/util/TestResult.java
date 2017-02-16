package synctest.util;

import java.util.Vector;

/**
 * A storage class for a set of test results
 * 
 * @author Alexander Marshall
 * */
public class TestResult {
	private String name;
	private double pass, fail, error, deadlock, total;
	private Vector<ExecutionResult> executionResults;

	/**
	 * An older version of the constructor that is no longer used
	 * 
	 * @param name 		The test class
	 * @param pass		The amount of executions that passed
	 * @param fail		The amount of executions that failed
	 * @param error		The amount of executions that resulted in an error
	 * @param deadlock	The amount of executions that deadlocked
	 * @param total		The total amount of executions*/
	public TestResult(String name, double pass, double fail, double error, double deadlock, double total) {
		this.name = name;
		this.pass = pass;
		this.fail = fail;
		this.error = error;
		this.deadlock = deadlock;
		this.total = total;
	}
	
	/**
	 * The constructor for the class
	 * 
	 * @param name				The test class
	 * @param executionResults	A vector containing each execution result*/
	public TestResult(String name, Vector<ExecutionResult> executionResults) {
		this.name = name;
		this.executionResults = executionResults;
		
		// Update counts based on execution results vector
		for(int i = 0; i < executionResults.size(); i++) {
			total += 1.0;
			if(executionResults.get(i).getTestStatus().equals("pass")) {
				pass += 1.0;
			} else if(executionResults.get(i).getTestStatus().equals("fail")) {
				fail += 1.0;
			} else if(executionResults.get(i).getTestStatus().equals("error")) {
				error += 1.0;
			} else if(executionResults.get(i).getTestStatus().equals("deadlock")) {
				deadlock += 1.0;
			}
		}
	}
	
	/**
	 * Store the results in a string
	 * 
	 * @return 		A string containing this tests results*/
	public String toString() {
		String result = 
				"Test: " 	 + name 	 	 + "\n" +
				"-------------------------------\n" +
				"Pass: " 	 + (int)pass 	 + "\n" +
				"Fail: " 	 + (int)fail 	 + "\n" +
				"Error: " 	 + (int)error 	 + "\n" +
				"Deadlock: " + (int)deadlock + "\n" +
				"Total: " 	 + (int)total    + "\n\n";
		return result;
	}
	
	/**
	 * Prints this tests results
	 * */
	public void printResult() {
		System.out.println("Test: " 	+ name);
		System.out.println("Pass: " 	+ (int)pass);
		System.out.println("Fail: " 	+ (int)fail);
		System.out.println("Error: " 	+ (int)error);
		System.out.println("Deadlock: " + (int)deadlock);
		System.out.println("Total: " 	+ (int)total);
	}

	/**
	 * @return 		The test name
	 * */
	public String getName() {
		return name;
	}
	
	/**
	 * @return 		A vector containing execution results
	 * */
	public Vector<ExecutionResult> getExecutionResults() {
		return this.executionResults;
	}
	
	/**
	 * @return 		The amount of executions that passed
	 * */
	public double getPass() {
		return pass;
	}
	
	/**
	 * @return 		The amount of executions that failed
	 * */
	public double getFail() {
		return fail;
	}

	/**
	 * @return 		The amount of executions that resulted in an error
	 * */
	public double getError() {
		return error;
	}
	
	/**
	 * @return 		The amount of executions that deadlocked
	 * */
	public double getDeadlock() {
		return deadlock;
	}
	
	/**
	 * @return 		The total amount of executions
	 * */
	public double getTotal() {
		return total;
	}
}
