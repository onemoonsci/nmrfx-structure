/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nmrfx.structure.chemistry.energy;

import org.apache.commons.math3.util.FastMath;
import org.nmrfx.structure.chemistry.Atom;
import static org.nmrfx.structure.chemistry.energy.AtomMath.RADJ;
import org.nmrfx.structure.chemistry.predict.Predictor;
import org.nmrfx.structure.fastlinear.FastVector3D;

/**
 *
 * @author brucejohnson
 */
public class EnergyDistancePairs extends EnergyPairs {

    double[] disSq;
    double[] rLow2;
    double[] rLow;

    public EnergyDistancePairs(EnergyCoords eCoords) {
        super(eCoords);
    }

    public void addPair(int i, int j, int iUnit, int jUnit, double r0) {
        resize(nPairs + 1);
        int iPair = nPairs;
        iAtoms[iPair] = i;
        jAtoms[iPair] = j;
        iUnits[iPair] = iUnit;
        jUnits[iPair] = jUnit;

        this.rLow[iPair] = r0;
        rLow2[iPair] = r0 * r0;
        weights[iPair] = 1.0;
        derivs[iPair] = 0.0;
        nPairs = iPair + 1;

    }

    void resize(int size) {
        if ((iAtoms == null) || (iAtoms.length < size)) {
            super.resize(size);
            int newSize = iAtoms.length;
            disSq = resize(disSq, newSize);
            rLow = resize(rLow, newSize);
            rLow2 = resize(rLow2, newSize);
        }
    }

    public double calcRepel(boolean calcDeriv, double weight) {
        FastVector3D[] vecCoords = eCoords.getVecCoords();
        double sum = 0.0;
        for (int i = 0; i < nPairs; i++) {
            int iAtom = iAtoms[i];
            int jAtom = jAtoms[i];
            FastVector3D iV = vecCoords[iAtom];
            FastVector3D jV = vecCoords[jAtom];
            double r2 = iV.disSq(jV);
            disSq[i] = r2;
            derivs[i] = 0.0;
            viol[i] = 0.0;
            if (r2 <= rLow2[i]) {
                double r = FastMath.sqrt(r2);
                double dif = rLow[i] - r;
                viol[i] = weights[i] * weight * dif * dif;
                sum += viol[i];
                if (calcDeriv) {
                    //  what is needed is actually the derivative/r, therefore
                    // we divide by r
                    // fixme problems if r near 0.0 so we add small adjustment.  Is there a better way???
                    derivs[i] = -2.0 * weights[i] * weight * dif / (r + RADJ);
                }
            }
        }
//        System.out.println("repel " + nPairs + " " + sum);

        return sum;
    }

    public ViolationStats getError(int i, double limitVal, double weight) {
        String modeType = "Rep";
        Atom[] atoms = eCoords.atoms;
        int iAtom = iAtoms[i];
        int jAtom = jAtoms[i];
        double r2 = disSq[i];
        double r = FastMath.sqrt(r2);
        double dif = 0.0;
        if (r2 <= rLow2[i]) {
            r = FastMath.sqrt(r2);
            dif = rLow[i] - r;
        }
        String result = "";
        ViolationStats stat = null;
        if (Math.abs(dif) > limitVal) {
            double energy = weights[i] * weight * dif * dif;
            stat = new ViolationStats(1, atoms[iAtom].getFullName(), atoms[jAtom].getFullName(), r, rLow[i], 0.0, energy, eCoords);
        }

        return stat;
    }
}
