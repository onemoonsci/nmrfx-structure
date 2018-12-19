import sys
import re
import jarray
import refine
import subprocess
import math
import os 
import osfiles
from refine import *
from difflib import SequenceMatcher
from java.io import InputStreamReader
from java.io import BufferedReader
from java.lang import ClassLoader
from java.util import ArrayList
from org.nmrfx.structure.chemistry import SVMPredict
from org.nmrfx.structure.chemistry import SSLayout
from org.nmrfx.structure.chemistry.io import Sequence
from org.nmrfx.structure.chemistry import Molecule
from org.nmrfx.structure.chemistry import RNALabels
from org.nmrfx.processor.datasets.peaks import Peak
from org.nmrfx.processor.datasets.peaks import PeakList
from org.nmrfx.structure.chemistry.predict import RNAStats
from org.nmrfx.structure.chemistry.predict import RNAAttributes
from java.io import FileWriter
from subprocess import check_output

wc = {'G': 'C', 'C': 'G', 'A': 'U', 'U': 'A', 'X': '', 'x': '', 'P':'p','p':'P'}
wobble = {'G': 'U', 'U': 'G', 'A': '', 'C': '', 'X': '', 'x': '','P':'p','p':'P'}

def getPairs( vienna):
    sArray = [len(vienna)]
    seqSize = array.array('i',sArray)
    ssLay = SSLayout(seqSize)
    ssLay.interpVienna(vienna)
    pairs = ssLay.getBasePairs()
    return pairs

def checkNC(pairs,index):
    nNucs = len(pairs)
    type = '-'
    if (index > 0) and (index < (nNucs-1)):
        bp0 = pairs[index-1]
        bp1 = pairs[index]
        bp2 = pairs[index+1]
        if bp0 != -1 and bp1 != -1:
            if abs(bp0-bp1) != 1:
                type = '5prime-nc'
        if bp1 != -1 and bp2 != -1:
            if abs(bp1-bp2) != 1:
                type = '3prime-nc'
    return type

def genRNAData(seqList, pairs):
    pairInfo = []
    for i, pair in enumerate(pairs):
        if pair == -1:
            partner = -1
        else:
            partner = seqList[pair]
        pairInfo.append((i, seqList[i], pair, partner))

    gnraPat = re.compile('G-[AGUC]-[AG]-A-')
    uncgPat = re.compile('U-[AGUC]-C-G-')


    matchPats = {'GC': 'Pp', 'GU': 'Pp', 'GA': 'PP', 'GG': 'PP', 'G-': 'P-', 'AC': 'Pm', 'AU': 'Pp', 'AA': 'PP',
                 'AG': 'PP', 'A-': 'P-', 'CC': 'pp', 'CU': 'pp', 'CA': 'pm', 'CG': 'pP', 'C-': 'p-', 'UC': 'pp',
                 'UU': 'pp', 'UA': 'pP', 'UG': 'pP', 'U-': 'p-', '- ': '-'}

    lastCoordEntity = ""
    elems = ('nucs', 'bp', 'str', 'influ', 'attr')
    rnaData = {}
    for elem in elems:
        rnaData[elem] = []
    for pair in pairInfo:
        (index, start, match, end) = pair
        nucRes = start.split(':')[-1]
        nuc = nucRes[0]
        coordEntity = start.split(':')[0]
        if (coordEntity != lastCoordEntity):
            if lastCoordEntity != "":
                for elem in elems:
                    rnaData[elem].append('-')
                    rnaData[elem].append('-')
            for elem in elems:
                rnaData[elem].append('-')
                rnaData[elem].append('-')

            lastCoordEntity = coordEntity

        strType = "wc"
        influ = checkNC(pairs,index)
        attr = "-"
        type = end
        endNuc = ''
        endNucRes = ''
        bp = '-'
        if type != -1:
            endNuc = end
            endNucRes = endNuc.split(':')[-1]
            bp = endNucRes[0]

        # print endNuc,strType,influ,endNucRes,nuc,bp
        rnaData['nucs'].append(nuc + bp)
        rnaData['bp'].append(matchPats[nuc + bp])
        wcMode = 0
        if wc[nuc] == bp:
            wcMode = 1
        if wobble[nuc] == bp:
            wcMode = 1
        if wcMode == 0:
            if bp == '-':
                wcMode = 0
            else:
                wcMode = 2
        rnaData['str'].append(wcMode)
        rnaData['influ'].append(influ)
        rnaData['attr'].append(attr)

    nNucs = len(rnaData['nucs'])
    lastRes = nNucs - 7
    for iRes in range(lastRes):
        strPat = rnaData['str'][iRes + 1:iRes + 7]
        if strPat == [1, 0, 0, 0, 0, 1]:
            nucList = rnaData['nucs'][iRes + 2:iRes + 6]
            loopRes = iRes + 2
            cNuc = rnaData['nucs'][loopRes]
            if cNuc == '-':
                continue
            loopType = 'tetra'
            nucTest = ''.join(nucList)
            if gnraPat.match(nucTest):
                loopType = 'gnra'
            elif uncgPat.match(nucTest):
                loopType = 'uncg'
            for loopIndex in (0, 1, 2, 3, 4):
                rnaData['attr'][loopRes + loopIndex] = loopType + str(loopIndex + 1)

    for elem in elems:
        rnaData[elem].append('-')
        rnaData[elem].append('-')

    outLines = []
    for iRes in range(nNucs):
        outLine = ""
        nuc = rnaData['nucs'][iRes + 2]
        if nuc == '-':
            continue
        for jRes in range(-2, 3):
            kRes = iRes + jRes + 2
            if (jRes == -2) or (jRes == 2):
                nuc = rnaData['bp'][kRes]
            else:
                nuc = rnaData['nucs'][kRes]
            outLine += nuc + ","
        for jRes in range(-2, 3):
            kRes = iRes + jRes + 2
            attr = rnaData['attr'][kRes]
            outLine += attr + ","
        influ = rnaData['influ'][iRes + 2]
        outLine += influ + ","
        outLine = outLine[0:-1]
        outLines.append(outLine)
    return outLines

def getType(attrs):
    canonical=True
    helix=True
    tetraloop = False
    isMisMatch=False
    nucList = attrs[0:5]
    attrList = attrs[5:10]
    for attrVal in attrList[1:4]:
        #  check this fixme
        if attrVal != "-":
            helix = False
   
    for attrVal in attrList[0:5]:
        #  check this fixme
        if attrVal != "-":
            canonical = False
   
    for nucVal in nucList[0:5]:
        if not checkWC(nucVal, True):
            canonical = False

    for nucVal in nucList[1:4]:
        if not checkWC(nucVal, True):
            helix = False
   
    for attrVal in attrList[1:4]:
        if attrVal.startswith("gnra"):
            tetraloop=True
        elif attrVal.startswith("tetr"):
            tetraloop=True

    return (helix, canonical, tetraloop)

def checkWC(nuc,strict):
    matched = True
    isWC = False
    if nuc == "-":
        matched = False
    else:
        n1,n2 = nuc
        if n1 == "-":
            matched = False
        if n2 == "-":
            matched = False
        if matched:
            if n2 == wc[n1]:
                isWC = True
            elif not strict:
                if n2 == wobble[n1]:
                    isWC = True
    return isWC

def getError(atom,helix,canonical,tetraloop):
    global svmSDevMap
    mode="c"
    if not helix:
       if tetraloop:
           mode="nc"
       else:
           mode="o"
    else:
       if not canonical:
           mode="nc"

    errorVal= svmSDevMap[atom,mode]
    return errorVal


def predictFromAttr(seqList, outLines):
    loadRNAShifts()
    atoms = (
        'AC2','AC8','GC8','CC5','UC5','CC6','UC6','AC1p','GC1p','CC1p','UC1p','AC2p','GC2p','CC2p',
        'UC2p','AC3p','GC3p','CC3p','UC3p','AC4p','AC5p','GC4p','GC5p','CC4p','CC5p','UC4p','UC5p',
        'AH8','GH8','AH2','CH5','UH5','CH6','UH6','AH1p','GH1p',
        'CH1p','UH1p','AH2p','GH2p','CH2p','UH2p','AH3p','GH3p','CH3p','UH3p','GH1','UH3','UH4p',
        'UH5p','UH5pp','CH4p','CH41','CH42','CH5p','CH5pp','AH4p','AH5p','AH5pp','AH61','AH62',
        'GH21','GH22','GH4p','GH5p','GH5pp','AN1','AN3','AN6','AN7','AN9','CN1','CN3','CN4','GN1',
        'GN2','GN7','GN9','UN1','UN3')
    types = ('nuc1', 'nuc2', 'nuc3', 'nuc4', 'nuc5', 'pos1', 'pos2', 'pos3', 'pos4', 'pos5','nuc')
    RNAAttributes.setTypes(types)
    result = {}
    for (outLine, res) in zip(outLines, seqList):
        values = outLine.split(',')
        (helix,canonical,tetraloop) = getType(values)
        attrValues = '_'.join(values)
        RNAAttributes.put(res, attrValues)
        attributes = {}
        for type, value in zip(types, values):
            attributes[type] = value
        for atom in atoms:
            targetNuc = atom[0]
            targetAtom = atom[1:]
            nucValues = values[0:5]
            nuc = nucValues[2][0]
            if nuc == targetNuc:
                shift = svmPredict(atom, attributes)
                errorVal = getError(atom,helix,canonical,tetraloop)
                #print attributes,res, atom, shift,errorVal
                atomAttr = atom + '_' + attrValues
                rStats = RNAStats.get(atomAttr, True)
                RNAAttributes.putStats(res+"."+targetAtom.replace("p","'"), rStats)
                if rStats != None:
                    nAvg = rStats.getN()
                    mean = rStats.getMean()
                    sdev = rStats.getSDev()
                    min = rStats.getMin()
                    max = rStats.getMax()
                    nSVM = 4.0
                    totalShifts = nSVM + float(nAvg)
                    f = nSVM / totalShifts
                    wshift = f * shift + (1.0 - f) * mean
                    shift = wshift
                    #print res,targetNuc,targetAtom,attrValues,shift
                result[res + '.' + targetAtom] = (shift, errorVal)
    return result


def loadResource(resourceName):
    cl = ClassLoader.getSystemClassLoader()
    istream = cl.getResourceAsStream(resourceName)
    lines = ""
    if istream == None:
        raise Exception("Cannot find '" + resourceName + "' on classpath")
    else:
        reader = InputStreamReader(istream)
        breader = BufferedReader(reader)
        while True:
            line = breader.readLine()
            if line == None:
                break
            if lines != '':
                lines += '\n'
            lines += line
        breader.close()
    return lines


# following should start working in Jython 2.7.1
# def loadResource(resourceName):
#    data = pyproc.__loader__.get_data(resourceName)
#    return data

def setupRNASVM():
    global svm
    global svmAttrMap
    global svmSDevMap
    svm = SVMPredict()
    svmAttrMap = {}
    svmSDevMap = {}
    resourceName = "data/rnasvm/svattr.txt"
    content = loadResource(resourceName)
    lines = content.split('\n')
    for line in lines:
        fields = line.split()
        atomName = fields[0]
        attrType = fields[1]
        attrValues = fields[2:]
        svmAttrMap[atomName, attrType] = attrValues
        if not (atomName, 'attrs') in svmAttrMap:
            svmAttrMap[atomName, 'attrs'] = []
        svmAttrMap[atomName, 'attrs'].append(attrType)
    resourceName = "data/rnapredsdev.txt"
    content = loadResource(resourceName)
    lines = content.split('\n')
    for line in lines:
        fields = line.split()
        atomName = fields[0]
        type = fields[1]
        sdev = float(fields[2])
        svmSDevMap[atomName,type] = sdev

def loadRNAShifts():
    if not RNAStats.loaded():
        RNAStats.readFile('data/rnadata.txt')

def svmGetAttrs(atomName, attrs):
    global svmAttrMap
    output = []
    for attrType in svmAttrMap[atomName, 'attrs']:
        for attrValue in svmAttrMap[atomName, attrType]:
            if attrValue == attrs[attrType]:
                output.append(1)
            else:
                output.append(0)
    return output


def svmPredict(atomName, attributes):
    global svm
    try:
        isinstance(svm, SVMPredict)
    except:
        setupRNASVM()

    output = svmGetAttrs(atomName, attributes)
    nValues = len(output)
    p = jarray.array(output, 'd')
    result = svm.predict(atomName, p)
    return result


def getFullSequence(molecule):
    seqList = []
    for polymer in molecule.getPolymers():
        for residue in polymer.getResidues():
            seqList.append(polymer.getName() + ':' + residue.getName() + residue.getNumber())
    return seqList

def setPredictions(molecule, predPPMs, refMode=True):
    molecule.updateAtomArray()
    for atomName in predPPMs:
        ppm,errorVal = predPPMs[atomName]
        if atomName[-1] == 'p':
            atomName = atomName[0:-1] + "'"
            if atomName[-2] == 'p':
                atomName = atomName[0:-2] + "''"
        atom = Molecule.getAtomByName(atomName)
        if refMode:
            atom.setRefPPM(ppm)
            atom.setRefError(errorVal)
        else:
            atom.setPPM(ppm)


def dumpPredictions(molecule, refMode=True):
    polymers = molecule.getPolymers()
    if len(polymers) > 1:
        useFull = True
    else:
        useFull = False
    for polymer in polymers:
        for residue in polymer.getResidues():
            for atom in residue.getAtoms():
                if refMode:
                    ppmV = atom.getRefPPM(0)
                else:
                    ppmV = atom.getPPM(0)
                if (ppmV != None):
                    if atom.parent.active and atom.active:
                         if useFull:
                             name = atom.getFullName()
                         else:
                             name = atom.getShortName()
                         print "%s %.2f %s %s" % (name,ppmV.getValue(),RNAAttributes.getStats(atom),RNAAttributes.get(atom))

def predictFromSequence(molecule = None, vienna = None):
    if molecule == None:
        molecule = Molecule.getActive()
    if vienna == None:
        vienna = molecule.getDotBracket()
    pairs = getPairs(vienna)
    seqList = getFullSequence(molecule)
    outLines = genRNAData(seqList, pairs)
    predPPMs = predictFromAttr(seqList, outLines)
    setPredictions(molecule, predPPMs)
