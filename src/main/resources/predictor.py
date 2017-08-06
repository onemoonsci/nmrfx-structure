import sys
from refine import *
import os
import osfiles
import runpy
import rnapred
from org.yaml.snakeyaml import Yaml
from java.io import FileInputStream


homeDir =  os.getcwd( )
dataDir = homeDir + '/'
outDir = os.path.join(homeDir,'output')

argFile = sys.argv[-1]

if argFile.endswith('.yaml'):
    input = FileInputStream(argFile)
    yaml = Yaml()
    data = yaml.load(input)

    refiner=refine()
    osfiles.setOutFiles(refiner,dataDir, 0)
    refiner.rootName = "temp"
    refiner.loadFromYaml(data,0)
    mol = refiner.molecule
    vienna = data['rna']['vienna']
    rnapred.predictFromSequence(mol,vienna)
    rnapred.dumpPredictions(mol)
elif argFile.endswith('.pdb'):
    refiner=refine()
    refiner.readPDBFile(argFile)
    shifts = refiner.predictShifts()
    for atomShift in shifts:
        aname,shift = atomShift
        print aname,shift
