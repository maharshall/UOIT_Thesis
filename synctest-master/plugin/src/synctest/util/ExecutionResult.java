package synctest.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class ExecutionResult {
	private int number;
	private String status;
	private File output;
	
	public ExecutionResult(int number, String status, File output) {
		this.number = number;
		this.status = status;
		this.output = output;
	}
	
	public int getExecutionNumber() {
		return this.number;
	}
	
	public String getTestStatus() {
		return this.status;
	}
	
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
