#!/bin/bash

PATH_TO_SRC=$(zenity --file-selection --directory --title="Select the directory for source files")
PATH_TO_TST=$(zenity --file-selection --directory --title="Select the directory for test files")
PATH_TO_OUT=$(zenity --file-selection --directory --title="Select the directory for ouput files")
LOOP_COUNT=$(zenity --entry --title="Synctest" --text="Enter the amound of times to run each test:" --entry-text "10")

PATH_TO_SRC=$(echo $PATH_TO_SRC | sed -e 's/.*synctest\///g')
PATH_TO_TST=$(echo $PATH_TO_TST | sed -e 's/.*synctest\///g')
PATH_TO_OUT=$(echo $PATH_TO_OUT | sed -e 's/.*synctest\///g')

# check for java source files
if(!(ls $PATH_TO_SRC | grep --quiet java)) then
    echo "\nError: $PATH_TO_SRC does not contain java files!\n"
    exit
fi

# check for java test files
if(!(ls $PATH_TO_TST | grep --quiet java)) then
    echo "\nError: $PATH_TO_TST does not contain java files!\n"
    exit
fi

# check if output directory exists
if [ ! -d "$PATH_TO_OUT" ]; then
  echo "\nError: $PATH_TO_OUT does not exist!\n"
  exit
fi

# build all source and tests
javac -cp jar/junit-4.12.jar $PATH_TO_SRC/*.java $PATH_TO_TST/*.java Parser.java

# convert format from account/tests to account.test
RUN_TEST=$(echo "$PATH_TO_TST" | tr / .)

# Remove path and extension from tests
TESTS=$(ls $PATH_TO_TST/*.java | xargs -n 1 basename)
TESTS=$(echo "$TESTS" | cut -f 1 -d '.' )

for t in $TESTS
do
    echo "Running $t... "
    for i in $(seq 1 $LOOP_COUNT)
    do
        echo -n "Execution $i: "

        # start deadlock detection script
        csh checkDeadlock.sh $t $PATH_TO_OUT/$t-$i.txt &

        # run test and redirect output to text file
        java -cp jar/junit-4.12.jar:jar/hamcrest-core-1.3.jar:. org.junit.runner.JUnitCore $RUN_TEST.$t > $PATH_TO_OUT/$t-$i.txt

        # print status of each test
        if (grep --quiet OK $PATH_TO_OUT/$t-$i.txt) then
            echo "Passed"
        elif (grep --quiet deadlock $PATH_TO_OUT/$t-$i.txt) then
            echo -n "" # pkill will print "Killed" so we do nothing here
        elif (grep --quiet Failures $PATH_TO_OUT/$t-$i.txt) then
            echo "Failed"
        else
            echo "This shouldn't happen!"
        fi

        # kill the deadlock detection script, or it will run forever
        pkill -KILL -f checkDeadlock.sh
    done
done

# remove the old files so we don't confuse the parser
rm $PATH_TO_OUT/*-all.txt

# Combine all text files into one for the parser
for t in $TESTS
do
    for i in $(seq 1 $i)
    do
        cat $PATH_TO_OUT/$t-$i.txt >> $PATH_TO_OUT/$t-all.txt
    done
done

rm results.txt
for t in $TESTS
do
    echo "======$t======" >> results.txt
    #parses the output and prints info to the terminal
    java Parser $PATH_TO_OUT/$t-all.txt $LOOP_COUNT >> results.txt
done

zenity --text-info --title="Synctest Results" --filename="results.txt"
