package synctest.util;

import java.util.Vector;

public class Result {
	private String name, raw;
	private double pass, fail, error, deadlock, total;
	private Vector<ExecutionResult> executionResults;

	public Result(String name, double pass, double fail, double error, double deadlock, double total, String raw) {
		this.name = name;
		this.pass = pass;
		this.fail = fail;
		this.error = error;
		this.deadlock = deadlock;
		this.total = total;
        this.raw = raw;
	}
	
	public Result(String name, Vector<ExecutionResult> executionResults) {
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
	
	public void printResult() {
		System.out.println("Test: " 	+ name);
		System.out.println("Pass: " 	+ (int)pass);
		System.out.println("Fail: " 	+ (int)fail);
		System.out.println("Error: " 	+ (int)error);
		System.out.println("Deadlock: " + (int)deadlock);
		System.out.println("Total: " 	+ (int)total);
	}

	public String getName() {
		return name;
	}
	
	public Vector<ExecutionResult> getExecutionResults() {
		return this.executionResults;
	}
	
	public double getPass() {
		return pass;
	}
	
	public double getFail() {
		return fail;
	}
	
	public double getError() {
		return error;
	}
	
	public double getDeadlock() {
		return deadlock;
	}
	
	public double getTotal() {
		return total;
	}
    
	public String getRaw() {
		return raw;
	}
}
