#!/bin/csh

set GREPFILE = "$2.grep"

#First set upper bound to terminate this process
set x = 0
while ($x == 0)
	# check for deadlocks every n seconds
    sleep $3

	# dump threads, grep output
    pkill -QUIT -f java.+$1

    grep deadlock $2 > $GREPFILE

	if (! -z $GREPFILE) then
		# deadlock found, kill process
        pkill -KILL -f java.+$1
		exit 1
	endif
end
