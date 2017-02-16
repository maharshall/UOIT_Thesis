package account.src;
import java.io.*;
import java.lang.StringBuilder;
import java.util.Random;

/*
 * Title:        Software Testing course
 * Description:  The goal of the exercise is implementing a  program which demonstrate  a parallel bug.
 * In the exercise we have two accounts.The program enable tranfering  money from one account to the other.
 * Although the functions were defended by locks (synchronize) there exists an interleaving which we'll experience a bug.
 * Copyright:    Copyright (c) 2003
 * Company:      Haifa U.
 * @author Zoya Shaham and  Maya Maimon
 * @version 1.0
 */

public class Main {

    public boolean less, more;
    Random rnd;

    public Main() {
        less = false;
        more = false;
        rnd = new Random();
    }

    protected String randStr() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder bld = new StringBuilder();
        while (bld.length() < 5) {
            int index = (int)(rnd.nextFloat() * chars.length());
            bld.append(chars.charAt(index));
        }
        String generated = bld.toString();
        return generated;
    }

    public void Runner(String[] in) {

        PrintStream out=null;

        try {
            if(in.length > 0){
                ManageAccount.num = 2;
                if(in.length == 1){ //the concurrency is optional
                    ManageAccount.num = Integer.parseInt(in[0]);
                    System.out.println("Running program with " + in[0] + " accounts.");
                } else if(in.length > 1){
                        System.err.println("The program can accept only one argument");
                        System.exit(1);
                }
            }

            // System.out.println("The Initial values:");
            ManageAccount[] bank = new ManageAccount[ManageAccount.num];

            String[] accountName = new String[ManageAccount.num];
            for(int i = 0; i < ManageAccount.num; i++) {
                accountName[i] = randStr();
            }

            for(int j = 0; j < ManageAccount.num; j++) {
                if(j == 0 && rnd.nextInt(10) == 3) {
                    bank[j] = new ManageAccount(accountName[j], 99, j);
                } else {
                    bank[j] = new ManageAccount(accountName[j], 100, j);
                }
                // ManageAccount.accounts[j].print();
            }

            //start the threads
            for (int k = 0; k < ManageAccount.num; k++){
                bank[k].start();
            }

            // wait until all are finished
            for (int k = 0; k < ManageAccount.num; k++){
                bank[k].join();
            }

            // System.out.println("The final values:");
            // ManageAccount.printAllAccounts();

            //updating the output file
            for (int k = 0;k < ManageAccount.num; k++){
                if(ManageAccount.accounts[k].amount < 300){
                    less = true;
                } else if(ManageAccount.accounts[k].amount > 300){
                    more = true;
                }
            }
            if((less == true)&&(more == true))
                System.out.println("<There is amount with more than 300 and there is amount with less than 300, No Lock>");
            if((less == false)&&(more == true))
                System.out.println("<There is amount with more than 300, No Lock>");
            if((less == true)&&(more == false))
                System.out.println("<There is amount with less than 300, No Lock>");
            if((less == false)&&(more == false))
                System.out.println("<All amounts are 300, None>");
        } catch(Exception e){ //FileNotFound, Security
           e.printStackTrace();
        }


    }//end of function main
}//end of class Main
