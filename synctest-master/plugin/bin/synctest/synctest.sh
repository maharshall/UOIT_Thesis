# This script is run by the SyncTest plugin

BASE_DIR=$1
PATH_TO_SRC=$2
PATH_TO_TST=$3
LOOP_COUNT=$4

# make directory for outputs if it doesn't already exist
mkdir $BASE_DIR/out
PATH_TO_OUT=$BASE_DIR/out

# remove the old output files (if they exist)
rm $PATH_TO_OUT/*.txt*

echo "-----------VARS-----------"
echo "BASE_DIR: $BASE_DIR"
echo "PATH_TO_SRC: $PATH_TO_SRC"
echo "PATH_TO_TST: $PATH_TO_TST"
echo "PATH_TO_OUT: $PATH_TO_OUT"
echo "LOOP_COUNT : $LOOP_COUNT"
echo "SLEEP AMNT : $5"
echo "--------------------------"

# compile all source and test files
javac -cp junit-4.12.jar $PATH_TO_SRC/*.java $PATH_TO_TST/*.java

RUN_TESTS=""

for entry in $PATH_TO_TST/*.java
do
    # get the package name from one of the test files
    RUN_TESTS=$(grep package $entry | awk '{print $2}' | sed 's/;$//')
    break # there's almost definitely a better way to do this
done

# Remove path and extension from tests (for running tests)
TESTS=$(ls $PATH_TO_TST/*.java | xargs -n 1 basename)
TESTS=$(echo "$TESTS" | cut -f 1 -d '.' )

cp junit-4.12.jar $BASE_DIR/..
cp hamcrest-core-1.3.jar $BASE_DIR/..


if [[ !  -z  $param  ]]; then
    cd $BASE_DIR
else
    cd $BASE_DIR/..
fi
    
for t in $TESTS
do
    echo "Running: $t"
    for i in $(seq 1 $LOOP_COUNT)
    do
        # start deadlock detection script
        csh checkDeadlock.sh $t $PATH_TO_OUT/$t-$i.txt $5 &

        # run test and redirect output to text file
        java -cp junit-4.12.jar:hamcrest-core-1.3.jar:. org.junit.runner.JUnitCore $RUN_TESTS.$t > $PATH_TO_OUT/$t-$i.txt

        # print status of each test
        if (grep --quiet OK $PATH_TO_OUT/$t-$i.txt) then
            echo "Execution $i/$LOOP_COUNT: Passed"
        elif (grep --quiet deadlock $PATH_TO_OUT/$t-$i.txt) then
            echo "Execution $i/$LOOP_COUNT: Deadlocked"
        elif (grep --quiet Failures $PATH_TO_OUT/$t-$i.txt) then
            echo "Execution $i/$LOOP_COUNT: Failed"
        else
            echo "Execution $i/$LOOP_COUNT: Error"
        fi
        
        # kill the deadlock detection script, or it will run forever
        pkill -KILL -f checkDeadlock.sh
    done
    echo "$t Completed"
done

echo "all tests finished"
rm junit-4.12.jar
rm hamcrest-core-1.3.jar
