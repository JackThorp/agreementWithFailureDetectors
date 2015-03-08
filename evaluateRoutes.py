#
# evaluateRoutes.py
#
# Copyright (c) 2013-2015, Imperial College London
# All rights reserved.
#
# Distributed Algorithms, CO347
#
import networkx as nx
import sys

class Network:
	
	nodes = {}
	
	def __init__ (self, topology, nodeCount, rtBaseDir = "./"):
		for i in range(1, nodeCount+1):
			self.nodes[i] = Node(i, rtBaseDir)
	
	def getDistance (self, src, dest):
		return len(self.getPath(src, dest)) - 1
	
	def getPath (self, src, dest):
		path = []
		node = src
		path.append(node)
		i = 0
		while node != dest and i < 20:
			node = self.nodes[node].getNextHop(dest)
			path.append(node)
			i+=1
		return path

class Node:
  
	nodeId = -1

	def __init__ (self, nodeId, baseDir = "./"):
		self.rt = RoutingTable ()
		self.nodeId = nodeId
		filename = "{0}P{1}-rt-1.out".format(baseDir, nodeId)
		self.rt.loadRoutingTable (filename)
	
	def getDistance (self, dest, noSourcePenalty = 1):
		plusOne = 0
		if dest != self.nodeId:
			plusOne = noSourcePenalty
		return self.rt.rt[dest]['hops'] + plusOne
	
	def getNextHop(self, dest):
		return self.rt.rt[dest]['nextHop']

class RoutingTable:
	
	neighborCount = 0 
	
	def __init__ (self):
		self.rt = {}
		self.neighCount = 0

	def loadRoutingTable (self, filename):
		with open(filename, 'r') as f:
			lines = f.readlines()
		for line in lines:
			parts = line.split(':')
			if int(parts[0]) == 65535:
				continue
			if parts[1].strip() == "local":
				parts[1] = parts[0] # self.nodeId
			elif parts[1].strip() == "undefined":
				parts[1] = parts[0] #self.nodeId
			self.rt[int(parts[0])] = {'nextHop': int(parts[1])}

class Routing:
	
	nodes = []
	edges = []
	failedNodes = []
	topologyDir = "./"
	
	minDistance = 0
	routingTableDistance = 0
	diff = 0
	
	def __init__ (self, nodeCount, topologyFile):
		self.topologyFile = topologyFile
		self.nodeCount = nodeCount
	
	def loadTopologyFile (self):
		topology = "{0}{1}".format(self.topologyDir, self.topologyFile)
		nodes = [n for n in range (1, self.nodeCount + 1) if n not in self.failedNodes]
		with open(topology, 'r') as f:
			lines = f.readlines()
		self.edges = []
		n1 = 0
		for line in lines :
			n1 += 1
			parts = line.split(',')
			for n2 in range (n1, self.nodeCount + 1):
				if parts[n2-1].strip() != "1":
					continue
				if n1 >= n2:
					continue
				if n1 in self.failedNodes or n2 in self.failedNodes:
					continue
				self.edges.append((n1, n2))
	
	def createGraph(self):
		self.G = nx.Graph()
		self.G.add_nodes_from(self.nodes)
		self.G.add_edges_from(self.edges)
	
	def getMinDistance (self, n1, n2):
		path = nx.shortest_path (self.G, n1, n2)
		return len(path) - 1
	
	def toString (self):
		print "{0} {1} {2} {3:.4}%\n".format(self.minDistance, self.routingTableDistance, self.diff, self.diff / (float(self.minDistance) / 100))
	
	def loadNetwork (self):
		self.net = Network (self.topologyFile, self.nodeCount)
	
	def checkRoutingTables (self):
		error = False
		print "Checking routing tables..."
		for n1 in range (1, self.nodeCount + 1):
			for n2 in range (1, self.nodeCount + 1):
				if n1 not in self.failedNodes and n2 not in self.failedNodes:
					distance = self.net.getDistance (n1, n2)
					minDistance = self.getMinDistance(n1, n2)
					if distance != minDistance:
						error = True
						print "n1 {0} n2 {1} distance {2} minDistance {3}".format(n1, n2, distance, minDistance)
		if not error:
			print "OK"


if __name__ == "__main__":
	
	topologyFile = sys.argv[1]
	nodeCount = int(sys.argv[2]) 
	
	r = Routing (nodeCount, topologyFile)
	r.failedNodes = []
	
	try :
		if sys.argv[3] != "":
			print "Failed node is {0}".format(sys.argv[3])
			r.failedNodes = [ int(sys.argv[3]) ]
	except IndexError:
		pass
	
	r.loadTopologyFile ()
	r.createGraph ()
	r.loadNetwork ()
	r.checkRoutingTables ()
	
	sys.exit(0)
