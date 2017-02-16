package account.src;

/*
 * Title:        Software Testing course
 * Description:  The goal of the exercise is implementing a  program which demonstrate  a parallel bug.
 * In the exercise we have two accounts.The program enable tranfering  money from one account to the other.
 * Although the functions were defended by locks (synchronize) there exists an interleaving which we'll experience a bug.
 * Copyright:    Copyright (c) 2003
 * Company:      Haifa U.
 * @author Maya Maimon
 * @version 1.0
 */

public class ManageAccount extends Thread {
    Account account;
    static Account[] accounts = new Account[1000000];
    static int num = 2;   //the number of the accounts
    static int accNum = 0;    //index to insert the next account
    int i;  //the index

    public ManageAccount(String name, double amount, int nnum) {
        account = new Account(name, amount, nnum);
        i = accNum;
        accounts[i] = account;
        accNum = (accNum+1)%num;  //the next index in a cyclic order
    }

    public void run() {
        account.deposit(300);
        account.withdraw(100);
        Account acc = accounts[(i+1)%num]; //transfering to the next account
        account.transfer(acc, 100);

    }

    static public void printAllAccounts(){
        for (int j=0;j<num;j++){
            if( ManageAccount.accounts[j]!=null){
                ManageAccount.accounts[j].print();
            }
        }
    }

    public boolean isPrime(int number) {
        int sqrt = (int) Math.sqrt(number) + 1;
        for (int x = 2; x < sqrt; x++) {
            if (number % x == 0) {
                return false;
            }
        }
        return true;
    }

}//end of class ManageAccount
