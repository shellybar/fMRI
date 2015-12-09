package edu.tau.eng.neuroscience.mri.server;

import java.util.List;

/**
 * AnalysesServer is the public API which is called by any client
 * of the fMRI tools' suite (e.g., GUI).
 */
public interface AnalysesServer {

    void submitAnalysis(List<Integer> unitIds);

}
