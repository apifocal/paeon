package org.apifocal.paeon.nlp.service;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.factory.AggregateBuilder;

public interface NLPProcessor {

    public String process(String text);

    public AnalysisEngine getPipeline();

}
