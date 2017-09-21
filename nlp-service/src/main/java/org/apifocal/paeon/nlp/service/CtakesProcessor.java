package org.apifocal.paeon.nlp.service;

import com.google.common.collect.Lists;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.ctakes.assertion.medfacts.cleartk.PolarityCleartkAnalysisEngine;
import org.apache.ctakes.assertion.medfacts.cleartk.UncertaintyCleartkAnalysisEngine;
import org.apache.ctakes.chunker.ae.Chunker;
import org.apache.ctakes.clinicalpipeline.ClinicalPipelineFactory;
import org.apache.ctakes.constituency.parser.ae.ConstituencyParser;
import org.apache.ctakes.contexttokenizer.ae.ContextDependentTokenizerAnnotator;
import org.apache.ctakes.core.ae.SentenceDetector;
import org.apache.ctakes.core.ae.SimpleSegmentAnnotator;
import org.apache.ctakes.core.ae.TokenizerAnnotatorPTB;
import org.apache.ctakes.core.cc.pretty.plaintext.PrettyTextWriter;
import org.apache.ctakes.dependency.parser.ae.ClearNLPDependencyParserAE;
import org.apache.ctakes.dependency.parser.ae.ClearNLPSemanticRoleLabelerAE;
import org.apache.ctakes.dictionary.lookup2.ae.AbstractJCasTermAnnotator;
import org.apache.ctakes.dictionary.lookup2.ae.DefaultJCasTermAnnotator;
import org.apache.ctakes.dictionary.lookup2.ae.JCasTermAnnotator;
import org.apache.ctakes.lvg.ae.LvgAnnotator;
import org.apache.ctakes.postagger.POSTagger;
import org.apache.ctakes.temporal.ae.*;
import org.apache.ctakes.temporal.eval.Evaluation_ImplBase;
import org.apache.ctakes.temporal.pipelines.FullTemporalExtractionPipeline;
import org.apache.ctakes.typesystem.type.refsem.Event;
import org.apache.ctakes.typesystem.type.refsem.EventProperties;
import org.apache.ctakes.typesystem.type.textsem.EventMention;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.collection.base_cpm.CasObjectProcessor;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collection;


public class CtakesProcessor implements NLPProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(CtakesProcessor.class);

    private static final NumberFormat SecondsFormatter = new DecimalFormat("#0.00000");

    private static final String OUTPUT_TYPE_XML = "xml";
    private static final String OUTPUT_TYPE_PRETTY = "pretty";

    // Reuse the pipeline for demo purposes
    private AnalysisEngine defaultPipeline = null;

    public CtakesProcessor() {
        initializePipeline();
    }

    /**
     * Creates an aggregated AnalysisEngine Builder for the NLP Pipeline
     * <p>
     * Based on https://github.com/healthnlp/examples/blob/master/ctakes-temporal-demo
     */
    public AggregateBuilder createAnalysisEngineBuilder() {
        AggregateBuilder builder = new AggregateBuilder();

        try {
            //  builder.add(ClinicalPipelineFactory.getFastPipeline());
            //	builder.add( ClinicalPipelineFactory.getTokenProcessingPipeline() );
            builder.add(AnalysisEngineFactory.createEngineDescription(SimpleSegmentAnnotator.class));
            builder.add(SentenceDetector.createAnnotatorDescription());
            builder.add(TokenizerAnnotatorPTB.createAnnotatorDescription());
            builder.add(LvgAnnotator.createAnnotatorDescription());
            builder.add(ContextDependentTokenizerAnnotator.createAnnotatorDescription());
            builder.add(POSTagger.createAnnotatorDescription());
            builder.add(Chunker.createAnnotatorDescription());
            builder.add(ClinicalPipelineFactory.getStandardChunkAdjusterAnnotator());
            builder.add(AnalysisEngineFactory.createEngineDescription(Evaluation_ImplBase.CopyNPChunksToLookupWindowAnnotations.class));
            builder.add(AnalysisEngineFactory.createEngineDescription(Evaluation_ImplBase.RemoveEnclosedLookupWindows.class));

            // TODO: the dictionary should be provided through a ProfileProvider.getDictiorary() (profile = customer = user)
            builder.add(AnalysisEngineFactory.createEngineDescription(DefaultJCasTermAnnotator.class,
                    AbstractJCasTermAnnotator.PARAM_WINDOW_ANNOT_KEY,
                    "org.apache.ctakes.typesystem.type.textspan.Sentence",
                    JCasTermAnnotator.DICTIONARY_DESCRIPTOR_KEY,
                    "org/apache/ctakes/dictionary/lookup/fast/sno_rx_16ab.xml")
            );
            builder.add(ClearNLPDependencyParserAE.createAnnotatorDescription());
            builder.add(PolarityCleartkAnalysisEngine.createAnnotatorDescription());
            builder.add(UncertaintyCleartkAnalysisEngine.createAnnotatorDescription());
            builder.add(AnalysisEngineFactory.createEngineDescription(ClearNLPSemanticRoleLabelerAE.class));
            builder.add(AnalysisEngineFactory.createEngineDescription(ConstituencyParser.class));

            // Add BackwardsTimeAnnotator
            builder.add(BackwardsTimeAnnotator
                    .createAnnotatorDescription("/org/apache/ctakes/temporal/ae/timeannotator/model.jar"));

            // Add EventAnnotator
            builder.add(EventAnnotator
                    .createAnnotatorDescription("/org/apache/ctakes/temporal/ae/eventannotator/model.jar"));

            builder.add(AnalysisEngineFactory.createEngineDescription(FullTemporalExtractionPipeline.CopyPropertiesToTemporalEventAnnotator.class));

            // Add Document Time Relative Annotator
            //link event to eventMention
            builder.add(AnalysisEngineFactory.createEngineDescription(AddEvent.class));

            builder.add(DocTimeRelAnnotator
                    .createAnnotatorDescription("/org/apache/ctakes/temporal/ae/doctimerel/model.jar"));

            // Add Event to Event Relation Annotator
            builder.add(EventTimeSelfRelationAnnotator
                    .createEngineDescription("/org/apache/ctakes/temporal/ae/eventtime/20150629/model.jar"));

            // Add Event to Event Relation Annotator
            builder.add(EventEventRelationAnnotator
                    .createAnnotatorDescription("/org/apache/ctakes/temporal/ae/eventevent/20150630/model.jar"));

        } catch (MalformedURLException e) {
            // FIXME: don't ignore forever
            LOG.error("Failed to create LVG Annotator: '{}'", e.getMessage());
        } catch (ResourceInitializationException e) {
            // FIXME: don't ignore forever
            LOG.error("Failed to create Annotator: '{}'", e.getMessage());
        }

        return builder;
    }

    private JCas createCommonAnalysisSystem() {
        try {
            return getPipeline().newJCas();
        } catch (ResourceInitializationException e) {
            // FIXME: don't ignore forever
            LOG.warn("Failed to create Java Common Analysis System");
        }

        // FIXME: Nullable
        return null;
    }

    public void initializePipeline() {
        LOG.info("Initilizing Pipeline ...");

        try {
            defaultPipeline = createAnalysisEngineBuilder().createAggregate();
        } catch (ResourceInitializationException e) {
            // FIXME: don't ignore forever
            defaultPipeline = null;
        }
    }

    public AnalysisEngine getPipeline() {
        return defaultPipeline;
    }

    private String getTimeElapsedInSeconds(long start) {
        return SecondsFormatter.format((System.currentTimeMillis() - start) / 1000d);
    }

    public String process(String text) {
        // remember when the processing started to be able to log it further
        long start = System.currentTimeMillis();
        JCas jcas = createCommonAnalysisSystem();
        jcas.setDocumentText(text);

        try {
            getPipeline().process(jcas);
        } catch (AnalysisEngineProcessException e) {
            LOG.error("jCAS: Failed to process text", e);
        }

        String result = formatResults(jcas, OUTPUT_TYPE_XML);
        jcas.reset();

        LOG.info("Processed in '{}' secs", getTimeElapsedInSeconds(start) );
        return result;
    }

    private String toPrettyFormat(JCas jcas) {
        StringBuffer sb = new StringBuffer();
        StringWriter sw = new StringWriter();
        BufferedWriter writer = new BufferedWriter(sw);
        Collection<Sentence> sentences = JCasUtil.select(jcas,
                Sentence.class);
        for (Sentence sentence : sentences) {
            try {
                PrettyTextWriter.writeSentence(jcas, sentence, writer);
            } catch (IOException e) {
                // FIXME:
                LOG.error("Could not serialize jCAS to PRETTY", e);
            }

        }

        try {
            writer.close();
        } catch (IOException e) {
            // FIXME:
            LOG.error("Could not close string Writer", e);
        }
        sb.append(sw.toString());
        return sb.toString();
    }

    private String toXmlFormat(JCas jcas) {
        StringBuffer sb = new StringBuffer();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            XmiCasSerializer.serialize(jcas.getCas(), output);
        } catch (SAXException e) {
            // FIXME:
            LOG.error("Could not serialize to XML", e);
        }
        sb.append(output.toString());
        try {
            output.close();
        } catch (IOException e) {
            // FIXME:
            LOG.error("Could not close XML Writer", e);
        }

        return sb.toString();
    }

    private String formatResults(JCas jcas, String format) {

        Collection<TOP> annotations = JCasUtil.selectAll(jcas);
        if (OUTPUT_TYPE_PRETTY.equalsIgnoreCase(format)) {
            return toPrettyFormat(jcas);
        }

        // if (OUTPUT_TYPE_XML.equalsIgnoreCase(format)) {
        // default
        return toXmlFormat(jcas);
        // }
    }

    public static class AddEvent extends org.apache.uima.fit.component.JCasAnnotator_ImplBase {

        @Override
        public void process(JCas jCas) throws AnalysisEngineProcessException {
            for (EventMention emention : Lists.newArrayList(JCasUtil.select(
                    jCas,
                    EventMention.class))) {
                EventProperties eventProperties = new org.apache.ctakes.typesystem.type.refsem.EventProperties(jCas);

                // create the event object
                Event event = new Event(jCas);

                // add the links between event, mention and properties
                event.setProperties(eventProperties);
                emention.setEvent(event);

                // add the annotations to the indexes
                eventProperties.addToIndexes();
                event.addToIndexes();
            }
        }
    }
}
