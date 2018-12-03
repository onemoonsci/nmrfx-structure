from org.nmrfx.structure.chemistry.io import PDBFile
from org.nmrfx.structure.chemistry.io import SDFile
from org.nmrfx.structure.chemistry.io import Sequence
from org.nmrfx.structure.chemistry import Molecule
from java.util import ArrayList
def updateAtomArray():
    ''' Updates the molecule atom array '''
    mol = Molecule.getActive()
    mol.updateAtomArray()

def readPDB(fileName, isCompound = False):
    ''' Reads a pdb file and modifies the static Molecule object.
        isCompound is used to specify if the file should be read in
        as a ligand or small molecule.
        Important note to take into consideration:
           if isCompound is false, HETATM fields will be ignored in file and
           the file will be read in as a sequence, ultimately creating
           polyer(s)

        This command returns either None or a compound depending on whether
        the isCompound is true.
    '''
    compound = None
    pdb = PDBFile()
    if not isCompound:
        pdb.readSequence(fileName,0)
    else:
        compound = pdb.readResidue(fileName, None, Molecule.getActive(), None)
    updateAtomArray()
    return compound
def readSDF(fileName, newMolecule = False):
    sdf = SDFile()
    molecule = Molecule.getActive() if not newMolecule else None
    compound = sdf.read(fileName, None, molecule, None)
    updateAtomArray()
    return compound

def readSequenceString(polymerName, sequence):
    ''' Creates a polymer from the sequence provided with the name of polymerName
        The sequence input can either be a chain of characters but will only work
        if the desired polymer is RNA. If creating a polymer for a protein,
        sequence must be a list using the 3 letter code.
    '''
    seqAList = ArrayList()
    seqAList.addAll(sequence)
    seqReader = Sequence()
    seqReader.read(polymerName, seqAList, "")
    updateAtomArray()

def readSequence(seqFile, convert=False):
    if convert:
        import os
        import osfiles
        dir = os.path.dirname(seqFile)
        seqFile = osfiles.convertSeqFile(seqFile,dir)
    seqReader = Sequence()
    seqReader.read(seqFile)
    updateAtomArray()

def readYaml(file):
    from java.io import FileInputStream
    from org.yaml.snakeyaml import Yaml

    input = FileInputStream(file)
    yaml = Yaml()
    data = yaml.load(input)
    return data