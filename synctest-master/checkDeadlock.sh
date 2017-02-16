#!/bin/csh

set GREPFILE = "$2.grep"

#First set upper bound to terminate this process
set x = 0
while ($x == 0)
	sleep 2

	pkill -QUIT -f java.+$1

	grep deadlock $2 > $GREPFILE

	if (! -z $GREPFILE) then
		# Deadlock detected in $1
		pkill -KILL -f java.+$1
		exit 1
	endif
end
