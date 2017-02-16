package synctest.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * A storage class for individual execution results
 * 
 * @author Alexander Marshall
 * */
public class ExecutionResult {
	private int number;
	private String status;
	private File output;
	
	public ExecutionResult(int number, String status, File output) {
		this.number = number;
		this.status = status;
		this.output = output;
	}
	
	/**
	 * @return 		The execution number of this execution
	 * */
	public int getExecutionNumber() {
		return this.number;
	}
	
	/**
	 * @return 		The result of this execution
	 * */
	public String getTestStatus() {
		return this.status;
	}
	
	/**
	 * Stores the output file in a single string
	 * 
	 * @return 		A string containing the output file contents
	 * */
	public String getOutputFile() {
		String raw = "";
		try {
			BufferedReader br = new BufferedReader(new FileReader(this.output));
			String line;

			while((line = br.readLine()) != null) {
				raw += line+"\n";
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return raw;
	}
	
}
