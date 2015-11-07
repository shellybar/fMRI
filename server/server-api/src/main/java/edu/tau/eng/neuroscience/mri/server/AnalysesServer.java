package edu.tau.eng.neuroscience.mri.server;

import java.util.List;

/**
 * AnalysesServer is the public API which is called by any client
 * of the fMRI tools' suite (e.g., GUI).
 */
//TODO this is a very important part of the system but it will probably be easier to define when the UI is discussed
public interface AnalysesServer {

    void submitAnalysis(List<Integer> unitIds);

}
