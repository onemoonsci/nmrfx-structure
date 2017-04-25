/*
 * NMRFx Structure : A Program for Calculating Structures 
 * Copyright (C) 2004-2017 One Moon Scientific, Inc., Westfield, N.J., USA
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.nmrfx.structure.chemistry.search;

import java.util.*;

public class MTree {

    Vector nodes = null;
    ArrayList<MNode> pathNodes = new ArrayList<>();

    public MTree() {
        nodes = new Vector();
    }

    public MNode addNode() {
        int id = nodes.size();
        MNode node = new MNode(id);
        nodes.add(node);

        return (node);
    }

    public void addEdge(int i, int j) {
        MNode iNode = (MNode) nodes.elementAt(i);
        MNode jNode = (MNode) nodes.elementAt(j);
        iNode.addNode(jNode);
        jNode.addNode(iNode);
    }

    public void sortNodes() {
        int nNodes = nodes.size();

        for (int j = 0; j < nNodes; j++) {
            MNode cNode = (MNode) nodes.elementAt(j);
            cNode.sortNodes();
        }
    }

    public ArrayList<MNode> getPathNodes() {
        return pathNodes;
    }

    public int[] broad_path(int iStart) {
        pathNodes.clear();
        int nNodes = nodes.size();
        int[] path = new int[nNodes];
        int nodesAdded = 1;
        int next = 0;
        int m = 0;
        MNode cNode = null;
        MNode nNode = null;

        for (int j = 0; j < nNodes; j++) {
            cNode = (MNode) nodes.elementAt(j);
            cNode.shell = -1;
            cNode.parent = null;
            cNode.pathPos = -1;
        }

        cNode = (MNode) nodes.elementAt(iStart);
        cNode.shell = 0;
        path[0] = iStart;
        pathNodes.add(cNode);

        for (int j = 0; j < nNodes; j++) {
            if (j >= nodesAdded) {
                return (path);
            }

            m = (path[j] & 0xFF);
            cNode = (MNode) nodes.elementAt(m);

            for (int i = 0; i < cNode.nodes.size(); i++) {
                next = ((MNode) cNode.nodes.get(i)).getID();
                nNode = (MNode) nodes.elementAt(next);

                if (nNode.shell == -1) {
                    //System.out.println(j+" "+i+" "+m+" "+nodesAdded+" "+next+" "+cNode.shell);
                    path[nodesAdded++] = ((cNode.shell + 1) << 8) + next;
                    pathNodes.add(nNode);
                    nNode.shell = cNode.shell + 1;
                    nNode.parent = cNode;
                    nNode.pathPos = nodesAdded - 1;
                }
            }
        }

        return path;
    }

    /**
     * Does a bread-first search through a tree starting at element iStart Returns an array of node indices describing
     * the path through the tree. Each pair of entries in the tree describes an edge. The edges are in order of the
     * bread-first search
     *
     */
    public int[] broad_path2(int iStart) {
        int nNodes = nodes.size();

        //System.out.println("nNodes "+nNodes);
        int[] path = new int[40 * nNodes];

        for (int i = 0; i < path.length; i++) {
            path[i] = -1;
        }

        int nodesAdded = 1;
        int next = 0;
        int m = 0;
        MNode cNode = null;
        MNode nNode = null;

        for (int j = 0; j < nNodes; j++) {
            cNode = (MNode) nodes.elementAt(j);
            cNode.shell = -1;
        }

        cNode = (MNode) nodes.elementAt(iStart);

        //cNode.shell=0;
        path[0] = 0;
        path[1] = iStart;

        int j = 0;

        /* loop through each node in the current path, adding edges from the current node to all adjacent nodes.
         * The path keeps growing as edges are added, so keep checking for edges from the last earliest unchecked node.
         * Finish when all nodes up to and including the last node added have been checked.
         **/
        while (((j * 2) + 1) <= ((2 * (nodesAdded - 1)) + 1)) {
            // System.out.println(" j "+j+" "+nodesAdded);
            // m is the node index (and cNode the node with that index) in the path to be checked during this iteration
            m = (path[(j * 2) + 1] & 0xFF);
            cNode = (MNode) nodes.elementAt(m);

            if (cNode.shell != -1) {
                j++;

                continue;
            }

            cNode.shell = j;

            //System.out.println(cNode.nodes.size());
            // Loop over all the nodes connected to the current node
            for (int i = 0; i < cNode.nodes.size(); i++) {
                next = ((MNode) cNode.nodes.get(i)).getID();
                nNode = (MNode) nodes.elementAt(next);

                // only add edges to nodes that haven't been an origin node (those with shell == -1)
                if (nNode.shell == -1) {
                    if (path.length <= ((2 * nodesAdded) + 1)) {
                        System.out.println(j + " " + i + " " + nNodes + " "
                                + path.length + " " + ((2 * nodesAdded) + 1));
                        System.out.println("out of space");

                        return null;
                    }

                    //System.out.println(j+" "+i+" "+m+" "+nodesAdded+" "+next+" "+cNode.shell);
                    path[2 * nodesAdded] = m; // node index of current node
                    path[(2 * nodesAdded) + 1] = next; // node index of the node this edge connects
                    nodesAdded++;

                    //nNode.shell = cNode.shell+1;
                }
            }

            j++;
        }

        return path;
    }

    public int[] depthFirstPath(int start) {
        int nNodes = nodes.size();

        for (int j = 0; j < nNodes; j++) {
            MNode cNode = (MNode) nodes.elementAt(j);
            cNode.shell = -1;
        }

        MNode sNode = (MNode) nodes.elementAt(start);
        ArrayList path = new ArrayList();
        path.add(sNode);
        sNode.shell = 0;
        depthFirstPath(sNode, path);

        int[] iPath = new int[path.size()];

        for (int i = 0, n = path.size(); i < n; i++) {
            MNode cNode = (MNode) path.get(i);
            iPath[i] = cNode.getID() + ((cNode.shell) << 8);
        }

        return iPath;
    }

    public void depthFirstPath(MNode cNode, ArrayList path) {
        for (int i = 0; i < cNode.nodes.size(); i++) {
            MNode nNode = ((MNode) cNode.nodes.get(i));

            if (nNode.shell == -1) {
                nNode.shell = cNode.shell + 1;
                path.add(nNode);
                depthFirstPath(nNode, path);
            }
        }
    }
}
