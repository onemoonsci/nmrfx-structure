import sys
import glob
import os.path
from org.nmrfx.structure.chemistry import Molecule
from org.nmrfx.structure.chemistry import SuperMol
from org.nmrfx.structure.chemistry.io import PDBFile
import refine
from java.util import TreeSet

def median(values):
    values.sort()
    if (len(values) == 0):
        return None
    elif (len(values) == 1):
        return values[0]
    else:
        n = len(values)
        v1 = values[n/2]
        v2 = values[(n-1)/2]
        return (v1+v2)/2.0
     
    

def loadPDBModels(files):
    fileName = files[0]
    pdb = PDBFile()
    molecule = pdb.read(fileName)
    iFile = 1
    for file in files:
        pdb.readCoordinates(file,iFile,False)
        iFile += 1
    return molecule

def findRepresentative(mol, resNums='*',atomNames="ca,c,n,o,p,o5',c5',c4',c3',o3'"):
    treeSet = TreeSet()
    structures = mol.getStructures()
    for structure in structures:
        if structure != 0: 
            treeSet.add(structure)
    nFiles = treeSet.size()

    doSelections(resNums,atomNames)
    sup = SuperMol(mol)
    mol.setActiveStructures(treeSet)
    
    superResults = sup.doSuper(-1, -1, False)
    totalRMS = 0.0
    averageToI = {}
    n = len(superResults)
    for superResult in superResults:
        iFix = superResult.getiFix()
        iMove = superResult.getiMove()
        rms = superResult.getRms()
        totalRMS += rms
        if iFix not in averageToI:
            averageToI[iFix] = 0.0
        averageToI[iFix] += rms
    minRMS = 1.0e6
    #for i in range(1,nFiles+1):
    for i in treeSet:
        averageToI[i] /= nFiles-1
        if averageToI[i] < minRMS:
            minRMS = averageToI[i]
            minIndex = i
    avgRMS = totalRMS/len(superResults)
    return (minIndex, minRMS, avgRMS)

def findCore(mol, minIndex,atomNames="ca,c,n,o,p,o5',c5',c4',c3',o3'"):
    atomNameList = atomNames.split(',')
    sup = SuperMol(mol)
    superResults = sup.doSuper(minIndex, -1, True)
    mol.calcRMSD()
    polymers = mol.getPolymers()
    resRMSs = []
    resValues = []
    for polymer in polymers:
        residues = polymer.getResidues()
        for residue in residues:
            atoms = residue.getAtoms('*')
            resSum = 0.0
            nAtoms = 0
            for atom in atoms:
                aName = atom.getName().lower()
                if aName in atomNameList:
                    resSum += atom.getBFactor()
                    nAtoms += 1
            if nAtoms > 0:
                resRMS = resSum/nAtoms
                resRMSs.append(resRMS)
                resValues.append((polymer.getName(),residue.getNumber(),resRMS))
    med = median(resRMSs)

    coreRes = []
    lastPolymer = ""
    state = "out"
    (polymer,lastNum,rms) = resValues[-1]
    for (polymer,num,rms) in resValues:
        last = ""
        if rms < 2.0*med:
            newState = "in"
        else:
            newState = "out"           
        if state == "out":
            if newState == "out":
                pass
            else:
                start = num
                #coreRes.append(num)
        else:
            if newState == "out":
                coreRes.append((start,lastRes))
            elif num == lastNum:
                coreRes.append((start,num))
                #coreRes.append(num)
        state = newState
        lastRes = num

    resSelect = []
    for (start,end) in coreRes:
        if (start == end):
           resSelect.append(start)
        else:
           resSelect.append(start+'-'+end)
    return resSelect

def doSelections(resSelects, atomSelect):
    polymer = 'A'
    Molecule.selectAtoms("*.*")
    Molecule.setAtomProperty(2,False)
    for resSelect in resSelects:
        selection = resSelect+'.'+atomSelect
        Molecule.selectAtoms(selection)
        Molecule.setAtomProperty(2,True)


def superImpose(mol, target,resSelect,atomSelect="ca,c,n,o,p,o5',c5',c4',c3',o3'"):
    doSelections(resSelect,atomSelect)
    sup = SuperMol(mol)
    superResults = sup.doSuper(target, -1, True)

def saveModels(mol,dir,base):
    active = mol.getActiveStructures()
    for i in active:
        outFile = os.path.join(dir,base+str(i)+'.pdb')
        refine.savePDB(mol, outFile, i)

def runSuper(files,newBase='super'):    
    mol = loadPDBModels(files)
    polymers = mol.getPolymers()
    if len(polymers) > 0:
        (minI,rms,avgRMS) = findRepresentative(mol)
        print 'repModel',minI,'rms',rms,'avgrms',avgRMS
        coreRes = findCore(mol, minI)
        print 'coreResidues',coreRes
        superImpose(mol, minI, coreRes)
    else:
        (minI,rms,avgRMS) = findRepresentative(mol,'*','c*,n*,o*,p*')
        print 'repModel',minI,'rms',rms,'avgrms',avgRMS
        coreRes = ['*']
        superImpose(mol, minI, coreRes,'c*,n*,o*,p*')
    (dir,fileName) = os.path.split(files[0])
    (base,ext) = os.path.splitext(fileName)

    saveModels(mol, dir,newBase)

