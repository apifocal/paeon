/*
 * Copyright 2017 apifocal LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apifocal.paeon.nlp.service;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertNotNull;


public class CtakesProcessorTest {
    private static final Logger LOG = LoggerFactory.getLogger(CtakesProcessorTest.class);
    private NLPProcessor nlp;

    private static final String noteNutritios = "Dr. Nutritious\n" +
            " \n" +
            "Medical Nutrition Therapy for Hyperlipidemia\n" +
            "Referral from: Julie Tester, RD, LD, CNSD\n" +
            "Phone contact: (555) 555-1212\n" +
            "Height: 144 cm Current Weight: 45 kg Date of current weight: 02-29-2001\n" +
            "Admit Weight: 53 kg BMI: 18 kg/m2\n" +
            "Diet: General\n" +
            "Daily Calorie needs (kcals): 1500 calories, assessed as HB + 20% for activity.\n" +
            "Daily Protein needs: 40 grams, assessed as 1.0 g/kg.\n" +
            "Pt has been on a 3-day calorie count and has had an average intake of 1100 calories.\n" +
            "She was instructed to drink 2-3 cans of liquid supplement to help promote weight gain.\n" +
            "She agrees with the plan and has my number for further assessment. May want a Resting\n" +
            "Metabolic Rate as well. She takes an aspirin a day for knee pain.";

    @Before
    public void setUp() throws Exception {
        this.nlp = new CtakesProcessor();
    }

    @After
    public void tearDown() throws Exception {
        this.nlp = null;
    }

    @Test
    public void testProcess() throws Exception {
        String analizedText = nlp.process(noteNutritios);
        assertNotNull("We should have received something from the NLP processor", analizedText);
    }

    @Test
    public void testInitialization() throws Exception {
        AnalysisEngine pipeline = nlp.getPipeline();
        assertNotNull("Failed to create a valid cTAKES pipeline", pipeline);
    }
}
