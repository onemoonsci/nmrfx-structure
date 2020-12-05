module org.nmrfx.structure {
    exports org.nmrfx.structure.chemistry;
    exports org.nmrfx.structure.chemistry.energy;
    exports org.nmrfx.structure.noe;
    exports org.nmrfx.structure.rna;
    exports org.nmrfx.structure.tools;
    exports org.nmrfx.structure.seqassign;
    exports org.nmrfx.structure.chemistry.predict;
    requires org.nmrfx.core;
    requires commons.math3;
    requires org.apache.commons.lang3;
    requires org.apache.commons.collections4;
    requires org.yaml.snakeyaml;
    requires smile.data;
    requires smile.core;
    requires smile.interpolation;
    requires smile.math;
    requires io.netty.all;
    requires java.logging;
    requires java.desktop;
    requires com.google.common;
    requires janino;
    requires commons.compiler;
    requires jython.slim;
    requires vecmath;
    requires biojava.alignment;
    requires biojava.core;
    requires biojava.phylo;
    requires libsvm;

}
