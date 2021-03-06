///////////////////////////////
//  Makumba, Makumba tag library
//  Copyright (C) 2000-2003  http://www.makumba.org
//
//  This library is free software; you can redistribute it and/or
//  modify it under the terms of the GNU Lesser General Public
//  License as published by the Free Software Foundation; either
//  version 2.1 of the License, or (at your option) any later version.
//
//  This library is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//  Lesser General Public License for more details.
//
//  You should have received a copy of the GNU Lesser General Public
//  License along with this library; if not, write to the Free Software
//  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
//
//  -------------
//  $Id$
//  $Name$
/////////////////////////////////////

package org.makumba.commons;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * Graph which can do a topological sort of its elements. In our case, the elements are MultipleKeys of form tags.
 * 
 * @author http://www.java2s.com/Code/Java/Collections-Data-Structure/Topologicalsorting.htm
 * @author Manuel Bernhardt <manuel@makumba.org>
 * @version $Id$
 */
public class GraphTS<E> {
    private final int MAX_VERTS = 40;

    private Vertex<E> vertexList[]; // list of vertices

    private int matrix[][]; // adjacency matrix

    private int numVerts; // current number of vertices

    private Vector<E> sortedArray;

    private Map<E, Integer> nodeNumbers;

    public GraphTS() {
        init();
    }

    @SuppressWarnings("unchecked")
    public void init() {
        nodeNumbers = new HashMap<E, Integer>();
        vertexList = new Vertex[MAX_VERTS];
        matrix = new int[MAX_VERTS][MAX_VERTS];
        numVerts = 0;
        for (int i = 0; i < MAX_VERTS; i++) {
            for (int k = 0; k < MAX_VERTS; k++) {
                matrix[i][k] = 0;
            }
        }
        // due to java not accepting generic array creation, we use a vector, but we need to fill it with dummy data
        // first
        sortedArray = new Vector<E>(MAX_VERTS);
        for (int i = 0; i < MAX_VERTS; i++) {
            sortedArray.add(null);
        }
    }

    public int addVertex(E key) {
        int n = numVerts++;
        vertexList[n] = new Vertex<E>(key);
        nodeNumbers.put(key, n);
        return numVerts - 1;
    }

    public void addEdge(int start, int end) {
        matrix[start][end] = 1;
    }

    public void addEdge(E start, E end) {
        Integer s = nodeNumbers.get(start);
        if (s == null) {
            throw new RuntimeException("Node " + start + " was not added to graph");
        }
        Integer e = nodeNumbers.get(end);
        if (e == null) {
            throw new RuntimeException("Node " + end + " was not added to graph");
        }
        addEdge(s, e);
    }

    public void displayVertex(int v) {
        System.out.print(vertexList[v].key);
    }

    public Vector<E> getSortedKeys() {
        // due to our using of a vector, trim to the real size here to avoid giving out null elements
        this.sortedArray.setSize(nodeNumbers.size());
        this.sortedArray.trimToSize();
        return this.sortedArray;
    }

    public void topo() {// toplogical sort
        while (numVerts > 0) {// while vertices remain,
            // get a vertex with no successors, or -1
            int currentVertex = noSuccessors();
            if (currentVertex == -1) {// must be a cycle
                System.out.println("ERROR: Graph has cycles");
                return;
            }
            // insert vertex label in sorted array (start at end)
            sortedArray.set(numVerts - 1, vertexList[currentVertex].key);

            deleteVertex(currentVertex); // delete vertex
        }

        // vertices all gone; display sortedArray
        /*
         * System.out.print("Topologically sorted order: "); for (int j = 0; j < orig_nVerts; j++) {
         * System.out.println("*** Key number " + j); MultipleKey thisKey = sortedArray[j]; Iterator it =
         * thisKey.iterator(); while (it.hasNext()) { System.out.println("*** *** Key content: " + it.next()); } }
         * System.out.println("");
         */
    }

    public int noSuccessors() { // returns vert with no successors (or -1 if no such verts)
        boolean isEdge; // edge from row to column in adjMat

        for (int row = 0; row < numVerts; row++) {
            isEdge = false; // check edges
            for (int col = 0; col < numVerts; col++) {
                if (matrix[row][col] > 0) {// if edge to another,
                    isEdge = true;
                    break; // this vertex has a successor try another
                }
            }
            if (!isEdge) {// if no edges, has no successors
                return row;
            }
        }
        return -1; // no
    }

    public void deleteVertex(int delVert) {
        if (delVert != numVerts - 1) // if not last vertex, delete from vertexList
        {
            for (int j = delVert; j < numVerts - 1; j++) {
                vertexList[j] = vertexList[j + 1];
            }

            for (int row = delVert; row < numVerts - 1; row++) {
                moveRowUp(row, numVerts);
            }

            for (int col = delVert; col < numVerts - 1; col++) {
                moveColLeft(col, numVerts - 1);
            }
        }
        numVerts--; // one less vertex
    }

    private void moveRowUp(int row, int length) {
        for (int col = 0; col < length; col++) {
            matrix[row][col] = matrix[row + 1][col];
        }
    }

    private void moveColLeft(int col, int length) {
        for (int row = 0; row < length; row++) {
            matrix[row][col] = matrix[row][col + 1];
        }
    }

    public static void main(String[] args) {
        GraphTS<Character> g = new GraphTS<Character>();
        g.addVertex('A'); // 0
        g.addVertex('B'); // 1
        g.addVertex('C'); // 2
        g.addVertex('D'); // 3
        g.addVertex('E'); // 4
        g.addVertex('F'); // 5
        g.addVertex('G'); // 6
        g.addVertex('H'); // 7

        g.addEdge(0, 3); // AD
        g.addEdge(0, 4); // AE
        g.addEdge(1, 4); // BE
        g.addEdge(2, 5); // CF
        g.addEdge(3, 6); // DG
        g.addEdge(4, 6); // EG
        g.addEdge(5, 7); // FH
        g.addEdge(6, 7); // GH

        g.topo(); // do the sort
    }
}

class Vertex<E> {
    protected E key;

    public Vertex(E key) {
        this.key = key;
    }

}
