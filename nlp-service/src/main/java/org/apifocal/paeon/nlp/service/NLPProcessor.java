package org.apifocal.paeon.nlp.service;

import org.apache.uima.fit.factory.AggregateBuilder;

public interface NLPProcessor {

    public void initializePipeline();

    public String process(String text);

}
