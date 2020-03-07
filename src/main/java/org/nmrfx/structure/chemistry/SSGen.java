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
package org.nmrfx.structure.chemistry;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author bajlabuser
 */
public class SSGen {

    public int tracker = 0;
    public String viennaSeq;
    public Molecule molecule;
    public static String type;
    public List<SecondaryStructure> structures = new ArrayList<SecondaryStructure>();
    public List<Residue> residues;

    public SSGen(Molecule mol, String vienna) {
        molecule = mol;
        viennaSeq = vienna;
        genRNAResidues();
        pairTo();
        secondaryStructGen();

    }

    public SSGen(Molecule mol) {
        molecule = mol;
        char[] vSeq = mol.getViennaSequence();
        String vienna = new String(vSeq);
        viennaSeq = vienna;
        genRNAResidues();
        pairTo();
        secondaryStructGen();
    }

    public void genRNAResidues() {
        List<Residue> resList = new ArrayList<>();
        for (Polymer polymer : molecule.getPolymers()) {
            if (polymer.isRNA()) {
                for (Residue residue : polymer.getResidues()) {
                    resList.add(residue);
                }
            }
            residues = resList;
        }
    }

    public void pairTo() {
        SSLayout ssLay = new SSLayout(viennaSeq.length());
        ssLay.interpVienna(viennaSeq, residues);
    }

    public static SecondaryStructure classifyRes(List<Residue> residues) {
        if (residues != null && !(residues.isEmpty())) {
            if (null != type) {
                switch (type) {
                    case "junction":
                        SecondaryStructure J = new Junction(residues);
                        return J;
                    case "nonloop": {
                        SecondaryStructure L = new NonLoop(residues);
                        return L;
                    }
                    case "bulge":
                        SecondaryStructure B = new Bulge(residues);
                        return B;
                    case "internalLoop":
                        SecondaryStructure IL = new InternalLoop(residues);
                        return IL;
                    case "loop": {
                        SecondaryStructure L = new Loop(residues);
                        return L;
                    }
                    case "helix": {
                        SecondaryStructure H = new Helix(residues);
                        return H;
                    }
                    default:
                        break;
                }
            }
        }
        return null;
    }

    public List<Residue> resList() {
        List<Residue> currentSS = new ArrayList<>();
        List<Residue> ssType = new ArrayList<>();
        boolean add = false;
        if (residues.get(tracker).pairedTo == null) {
            while (tracker < residues.size() && residues.get(tracker).pairedTo == null) {
                currentSS.add(residues.get(tracker));
                tracker++;
            }
            Residue ssFirstRes = currentSS.get(0);
            Residue ssLastRes = currentSS.get(currentSS.size() - 1);
            Residue firstRes = residues.get(0);
            Residue lastRes = residues.get(residues.size() - 1);
            Residue resBefore = ssFirstRes.getPrevious();
            Residue resAfter = ssLastRes.getNext();

            if (ssLastRes == lastRes || ssFirstRes == firstRes) { //last residue or first residue (string of non pairing all the way to end)
                add = true;
                type = "nonloop"; //instead of calling first residue, call last residue
            } else if (resBefore.pairedTo == resAfter) { //loop
                add = true;
                type = "loop";
            } else if (resBefore.pairedTo.iRes < resAfter.pairedTo.iRes) { //junction
                add = true;
                type = "junction";
            } else if (resBefore.pairedTo.getPrevious().pairedTo == null && (resBefore.iRes < resBefore.pairedTo.iRes)) { //second half of internal loop
                Residue otherLoopRes = resBefore.pairedTo.getPrevious();
                while (otherLoopRes.pairedTo == null) {
                    currentSS.add(otherLoopRes);
                    otherLoopRes = otherLoopRes.previous;
                }
                add = true;
                type = "internalLoop";
            } else if (resBefore.iRes < resBefore.pairedTo.iRes) { //left side 
                boolean leftBulge = resBefore.pairedTo.getPrevious().pairedTo != null;
                if (leftBulge) {
                    add = true;
                    type = "bulge";
                }
            } else if (resBefore.iRes > resBefore.pairedTo.iRes) { //right side
                boolean rightBulge = resAfter.pairedTo.getNext().pairedTo != null;
                if (rightBulge) {
                    add = true;
                    type = "bulge";
                }
            }
            if (add) {
                ssType.addAll(currentSS);
            }
            return ssType;
        } else if (residues.get(tracker).pairedTo != null) {
            while (tracker < residues.size() && residues.get(tracker).pairedTo != null) {
                if (residues.get(tracker).iRes < residues.get(tracker).pairedTo.iRes) {
                    ssType.add(residues.get(tracker));
                    ssType.add(residues.get(tracker).pairedTo);
                    tracker++;
                } else {
                    tracker++;
                }
            }
            type = "helix";

            return ssType;
        }
        return null;
    }

    public void secondaryStructGen() {
        while (tracker < residues.size()) {
            SecondaryStructure ss = classifyRes(resList());
            if (ss != null) {
                for (Residue residues : ss.secResidues) {
                    residues.secStruct = ss;
                }
                ss.size = ss.secResidues.size();
                structures.add(ss);
            }
        }
    }

}
