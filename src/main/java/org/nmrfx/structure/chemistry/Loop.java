/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nmrfx.structure.chemistry;

import java.util.*;

/**
 *
 * @author bajlabuser
 */
public class Loop extends SecondaryStructure {

    public static int localind = 0;
    public int li = 0;
    public String name;

    public Loop(String id, List<Residue> Lpresidues) {
        name = id;
        li = localind;
        gi = globalind;
        secresidues = Lpresidues;
        globalind++;
        localind++;

    }

    @Override
    public String toString() {
        return name + getGlobalInd() + ":" + getLocalInd();
    }

    public int getGlobalInd() {
        return gi;
    }

    public int getLocalInd() {
        return li;
    }

    public List<Residue> getResidues() {
        return secresidues;
    }

    public void getInvolvedRes() {
        for (Residue residue : secresidues) {
            System.out.print(residue.getName() + residue.resNum + " ");
        }
    }
}
