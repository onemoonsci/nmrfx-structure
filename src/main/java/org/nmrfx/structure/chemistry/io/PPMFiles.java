/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.nmrfx.structure.chemistry.io;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.nmrfx.structure.chemistry.Atom;
import org.nmrfx.structure.chemistry.Compound;
import org.nmrfx.structure.chemistry.Entity;
import org.nmrfx.structure.chemistry.Molecule;
import org.nmrfx.structure.chemistry.PPMv;

/**
 *
 * @author Bruce Johnson
 */
public class PPMFiles {

    public static void writePPM(Molecule molecule, FileWriter chan, int ppmSet, boolean refMode) throws IOException {
        int i;
        String result = null;

        molecule.updateAtomArray();

        i = 0;
        List<Atom> atoms = molecule.getAtomArray();

        for (Atom atom : atoms) {
            PPMv ppmV;
            if (refMode) {
                ppmV = atom.getRefPPM(ppmSet);
            } else {
                ppmV = atom.getPPM(ppmSet);
            }
            if ((ppmV != null) && ppmV.isValid()) {
                Entity entity = atom.getEntity();
                if (entity instanceof Compound) {
                    String shortName = atom.getShortName();
                    String fullName = atom.getFullName();
                    String name;
                    if (atom.getEntity().molecule.entities.size() == 1) {
                        name = shortName;
                    } else {
                        name = fullName;
                    }
                    double ppm = ppmV.getValue();
                    if (refMode) {
                        result = String.format("%s\t%.4f\n", name, ppm);
                    } else {
                        int stereo = ppmV.getAmbigCode();
                        atom.getBMRBAmbiguity();
                        result = String.format("%s\t%.3f\t%d\n", name, ppm, stereo);

                    }
                    chan.write(result);
                }
            }
        }
    }

    public static void readPPM(Molecule molecule, Path path, int ppmSet, boolean refMode) {

        try (BufferedReader fileReader = Files.newBufferedReader(path)) {
            while (true) {
                String line = fileReader.readLine();
                if (line == null) {
                    break;
                }
                String sline = line.trim();
                if (sline.length() == 0) {
                    continue;
                }
                if (sline.charAt(0) == '#') {
                    continue;
                }
                String[] sfields = line.split("\t", -1);
                if (sfields.length > 1) {
                    String atomRef = sfields[0];
                    Atom atom = Molecule.getAtomByName(atomRef);
                    if (atom == null) {
                        System.out.println("no atom " + atomRef);
                    } else {
                        double ppm = Double.parseDouble(sfields[1]);
                        if (refMode) {
                            atom.setRefPPM(ppmSet, ppm);
                        } else {
                            atom.setPPM(ppmSet, ppm);

                        }
                    }
                }
            }
        } catch (IOException ioE) {

        }
    }
}
/*
        foreach atom $atoms {
        set ppm [nv_atom elem ppm ${atom}_$ppmSet]
        set stereo [nv_atom elem stereo $atom]
        if {![string is double -strict $stereo]} {
            set stereo 1
        }
        if {($ppm != {}) && ($ppm> -990.0)} {
            set aname [nv_atom elem aname $atom]
            set seq [nv_atom elem seq $atom]
            if {!$fullFormat} {
                if {$ppmSet eq "R"} {
                    set eppm [nv_atom elem eppm ${atom}_$ppmSet]
                    puts $f1 [format "%3d.%-4s %9.3f %9.3f" $seq $aname $ppm $eppm]
                } else {
                    puts $f1 [format "%3d.%-4s %9.3f %1d" $seq $aname $ppm $stereo]
                }
            } else {
                if {$ppmSet eq "R"} {
                    set eppm [nv_atom elem eppm ${atom}_$ppmSet]
                    puts $f1 [format "%15s %9.3f %9.3f" $atom $ppm $eppm]
                } else {
                    puts $f1 [format "%15s %9.3f %1d" $atom $ppm $stereo]
                }
            }
        }
    }

 */