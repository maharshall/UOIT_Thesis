#!/bin/bash

# build all source and tests
javac -cp junit-4.12.jar account/*.java account/tests/*.java Parser.java

TESTS="Test2 Test10"

for t in $TESTS
do
    echo "Running $t... "
    for i in $(seq 1 $1)
    do
        echo -n "Execution $i: "

        # start deadlock detection script
        csh checkDeadlock.sh $t account/out/$t/$t-$i.txt &

        # run test and append output to text file
        java -cp junit-4.12.jar:hamcrest-core-1.3.jar:. org.junit.runner.JUnitCore account.tests.$t > account/out/$t/$t-$i.txt

        # print status of each test
        if (grep --quiet OK account/out/$t/$t-$i.txt) then
            echo "Passed"
        elif (grep --quiet deadlock account/out/$t/$t-$i.txt) then
            echo -n "" # pkill will print "Killed" so we do nothing here
        else
            echo "Failed"
        fi

        pkill -KILL -f checkDeadlock.sh
    done
    echo "Done!"
done

# remove the old files so we don't confuse the parser
rm account/out/*/*-all.txt

# Combine all text files into one for the parser
for t in $TESTS
do
    for i in $(seq 1 $i)
    do
        cat account/out/$t/$t-$i.txt >> account/out/$t/$t-all.txt
    done
done

for t in $TESTS
do
    echo "\n======$t======"
    #parses the output and prints info to the terminal
    java Parser account/out/$t/$t-all.txt $i
done
