package nl.multicode;

import nl.multicode.model.ClusterResult;

import java.util.List;

public interface TextClusterer {

    ClusterResult cluster(List<String> input, int minimalDenominatorLength, int minimumGroupSize);
}
