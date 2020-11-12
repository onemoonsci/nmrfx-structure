/*
 * MX - Essential Cheminformatics
 * 
 * Copyright (c) 2007-2009 Metamolecular, LLC
 * 
 * http://metamolecular.com/mx
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.nmrfx.structure.chemistry.ring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.nmrfx.chemistry.Atom;
import org.nmrfx.chemistry.Bond;
import org.nmrfx.chemistry.Entity;
import org.nmrfx.chemistry.ITree;
import org.nmrfx.structure.chemistry.Molecule;

/**
 * @author Richard L. Apodaca <rapodaca at metamolecular.com>
 */
public class PathGraph {

    private List<PathEdge> edges;
    private List<Atom> atoms;
    private int maxRingSize;

    public PathGraph(ITree itree) {
        edges = new ArrayList();
        atoms = new ArrayList();
        maxRingSize = -1;

        loadEdges(itree);
        loadNodes(itree);
    }

    public void setMaximumRingSize(int maxRingSize) {
        this.maxRingSize = maxRingSize;
    }

    public void printPaths() {
        for (PathEdge edge : edges) {
            if (edge.isCycle()) {
                System.out.print("*");
            }

            for (Atom atom : edge.getAtoms()) {
                System.out.print(atom.getIndex() + "-");
            }

            System.out.println();
        }
    }

    public List<PathEdge> remove(Atom atom) {
        List<PathEdge> oldEdges = getEdges(atom);
        List<PathEdge> result = new ArrayList();

        for (PathEdge edge : oldEdges) {
            if (edge.isCycle()) {
                result.add(edge);
            }
        }

        oldEdges.removeAll(result);
        edges.removeAll(result);

        List<PathEdge> newEdges = spliceEdges(oldEdges);

        edges.removeAll(oldEdges);
        edges.addAll(newEdges);
        atoms.remove(atom);

        return result;
    }

    private List<PathEdge> spliceEdges(List<PathEdge> edges) {
        List<PathEdge> result = new ArrayList();

        for (int i = 0; i < edges.size(); i++) {
            for (int j = i + 1; j < edges.size(); j++) {
                PathEdge splice = edges.get(j).splice(edges.get(i), maxRingSize + 1);

                if (splice != null) {
                    result.add(splice);
                }
            }
        }

        return result;
    }

    private List<PathEdge> getEdges(Atom atom) {
        List<PathEdge> result = new ArrayList();

        for (PathEdge edge : edges) {
            if (edge.isCycle()) {
                if (edge.getAtoms().contains(atom)) {
                    result.add(edge);
                }
            } else {
                if ((edge.getSource() == atom) || (edge.getTarget() == atom)) {
                    result.add(edge);
                }
            }
        }

        return result;
    }

    private void loadEdges(ITree itree) {
        Molecule molecule;
        if (itree instanceof Molecule){
            molecule = (Molecule) itree;
        }  else {
            Entity entity = (Entity) itree;
            molecule = entity.molecule;
        }
        molecule.updateBondArray();
        List<Bond> bonds = itree.getBondList();
        for (Bond bond : bonds) {
            edges.add(new PathEdge(Arrays.asList(bond.begin, bond.end)));
        }
    }

    private void loadNodes(ITree itree) {
        for (Atom atom : itree.getAtomArray()) {
            atoms.add(atom);
        }
    }
}
