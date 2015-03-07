import os
import time

experiments = [("lecture-6","6","P1"),("line-10","10","P1"), 
               ("ring-10","10","P1"),("star-10","10","P2"),
               ("tree-15","15","P8"),("tri-graph-10","10","P5")]

os.system("make clean && make")


for topology, n, faulty in experiments:
	os.system("./sysmanager.sh start NetchangeProcess " + n + " networks/"+topology+".txt")
	time.sleep(10)
	os.system("java FaultInjector -m \'"+faulty+"<|>OFF\'")
	time.sleep(10)
	os.system("java FaultInjector -m \'"+faulty+"<|>ON\'")
	time.sleep(10)
	os.system("./sysmanager.sh stop")
	os.system("gnuplot -e \"network=\'"+topology+"\'\" plot.gnu")
	print "Looking for errors..."
	os.system("cat *.out | grep \"Error\"")
	print "_________________________________"

