#!/bin/bash


# This is the main file for Synctest
# syncTest.sh takes four arguments:
#     1. The path to the source files
#     2. The path to the test files
#     3. The path to the output directory, and
#     4. The number of times to run each test
#
# Currently the script requires that paths DO NOT end with '/' but I plan to change this

if [ "$#" -ne 4 ]; then
  echo -e "\nUsage: sh syncTest.sh path/to/src path/to/tests path/to/output loopCount\n"
  exit
fi

PATH_TO_SRC=$1
PATH_TO_TST=$2
PATH_TO_OUT=$3
LOOP_COUNT=$4

# check for java source files
if(!(ls $PATH_TO_SRC | grep --quiet java)) then
  echo -e "\nError: $PATH_TO_SRC does not contain java files!\n"
  exit
fi

# check for java test files
if(!(ls $PATH_TO_TST | grep --quiet java)) then
  echo -e "\nError: $PATH_TO_TST does not contain java files!\n"
  exit
fi

# check if output directory exists
if [ ! -d "$PATH_TO_OUT" ]; then
  echo -e "\nError: $PATH_TO_OUT does not exist!\n"
  exit
fi

echo "-----------VARS-----------"
echo "PATH_TO_SRC: $PATH_TO_SRC"
echo "PATH_TO_TST: $PATH_TO_TST"
echo "PATH_TO_OUT: $PATH_TO_OUT"
echo "LOOP_COUNT : $LOOP_COUNT"
echo "--------------------------"

# compile all source and test files, as well as the parser
javac -cp jar/junit-4.12.jar $PATH_TO_SRC/*.java $PATH_TO_TST/*.java Parser.java

# convert format from account/tests to account.test (for running tests)
RUN_TEST=$(echo "$PATH_TO_TST" | tr / .)

# Remove path and extension from tests (for running tests)
TESTS=$(ls $PATH_TO_TST/*.java | xargs -n 1 basename)
TESTS=$(echo "$TESTS" | cut -f 1 -d '.' )

for t in $TESTS
do
    echo "Running $t... "
    for i in $(seq 1 $LOOP_COUNT)
    do
        # start deadlock detection script
        csh checkDeadlock.sh $t $PATH_TO_OUT/$t-$i.txt &

        # run test and redirect output to text file
        java -cp jar/junit-4.12.jar:jar/hamcrest-core-1.3.jar:. org.junit.runner.JUnitCore $RUN_TEST.$t > $PATH_TO_OUT/$t-$i.txt

        # print status of each test
        if (grep --quiet OK $PATH_TO_OUT/$t-$i.txt) then
            echo "Execution $i: Passed"
        elif (grep --quiet deadlock $PATH_TO_OUT/$t-$i.txt) then
            echo "Execution $i: Deadlocked"
        elif (grep --quiet Failures $PATH_TO_OUT/$t-$i.txt) then
            echo "Execution $i: Failed"
        else
            echo "Execution $i: This shouldn't happen!"
        fi

        # kill the deadlock detection script, or it will run forever
        pkill -KILL -f checkDeadlock.sh
    done
done

# remove the old files so they don't affect results
rm $PATH_TO_OUT/*-all.txt

# Combine all text files into one for the parser
for t in $TESTS
do
    for i in $(seq 1 $i)
    do
        cat $PATH_TO_OUT/$t-$i.txt >> $PATH_TO_OUT/$t-all.txt
    done
done

for t in $TESTS
do
    echo "\n======$t======"
    #parse the output and print info to the terminal
    java Parser $PATH_TO_OUT/$t-all.txt $i
done
