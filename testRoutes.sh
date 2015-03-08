#!/bin/bash
#
# Copyright (c) 2013-2015, Imperial College London
# All rights reserved.
#
# Distributed Algorithms, CO347
#
USAGE="./test.sh [topology file] [network size] [faulty process] [sleep period]"

if [ $# -lt 3 ]; then
	echo $USAGE && exit 1
fi

F=$1
N=$2
P=$3
PWD=`pwd`
SLEEP=${4:-10}

# Delete any stale routing tables
rm -f *.out

# 1.
./sysmanager.sh start NetchangeProcess $N $F
sleep $SLEEP

echo "python ${PWD}/evaluateRoutes.py $F $N"
python ${PWD}/evaluateRoutes.py $F $N

# 2.
sleep 2
java FaultInjector -m "P${P}<|>OFF"
sleep $SLEEP

echo "python ${PWD}/evaluateRoutes.py $F $N $P"
python ${PWD}/evaluateRoutes.py $F $N $P

# 3.
sleep 2
java FaultInjector -m "P${P}<|>ON"
sleep $SLEEP

echo "python ${PWD}/evaluateRoutes.py $F $N"
python ${PWD}/evaluateRoutes.py $F $N

# 4.
./sysmanager.sh stop

# Done.
echo "Done"

exit 0
