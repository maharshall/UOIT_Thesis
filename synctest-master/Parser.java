import java.io.*;
import java.util.regex.*;

public class Parser {
	public static void main(String[] args) {
		Pattern pass = Pattern.compile("OK");
		Pattern fail = Pattern.compile("Failures");
		Pattern err = Pattern.compile("(?d)[)] test");
		Pattern assrt = Pattern.compile("at account.tests.Test(.*).test(.*)[(]Test(.*).java:(.*)[)]");
		String errors = "";

		int passCount = 0;
		int failCount = 0;
    	int testsRun = Integer.parseInt(args[1]);

		try {
			File file = new File(args[0]);
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;

			while((line = br.readLine()) != null) {
				Matcher m0 = pass.matcher(line);
				Matcher m1 = fail.matcher(line);
				Matcher m2 = err.matcher(line);
				Matcher m3 = assrt.matcher(line);

				while(m0.find()) {
					passCount++;
				}

				while(m1.find()) {
					failCount++;
				}

				while(m2.find()) {
					errors += "\n" + (failCount+1) + ")" + br.readLine();
				}

				while(m3.find()) {
					//open test file
					File testFile = new File("account/tests/Test"+m3.group(1)+".java");
					BufferedReader testBR = new BufferedReader(new FileReader(testFile));
					//go to the line where the error is
					for(int i = 0; i < Integer.parseInt(m3.group(4)); i ++) {
						line = testBR.readLine();
					}
					//get rid of extra spaces
					line = line.replaceAll(" ", "");
					//print line to terminal
					errors += " @ Test"+m3.group(1)+".java:"+m3.group(4)+"\n  " + line + "\n";
				}

			}

      	double percent = ((double) passCount/(double) testsRun)*100;
			System.out.println("Tests Run: " + testsRun);
			System.out.println("Passed: " + passCount);
			System.out.println("Failed: " + failCount);
			System.out.println("Deadlocked: " + (testsRun - (failCount+passCount)));

//			if(failCount > 0) {
//				System.out.println("=====ERRORS=====");
//				System.out.println(errors);
//			}

      	br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
