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

import java.util.ArrayList;
import java.util.List;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerFactory;
import org.apache.activemq.broker.BrokerService;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BrokerSetupTest {
    private static final Logger LOG = LoggerFactory.getLogger(BrokerSetupTest.class);
    private static final String PAEON_LOCAL_CFG = "src/test/resources/META-INF/org.apifocal.paeon/paeon-nlp.cfg";
    private static final List<BrokerService> BROKERS = new ArrayList<>();
    private static final int PORT_START = 60616;

	public static final String PAEON_TEST_BROKER = "paeon.test.broker";
	public static final String PAEON_TEST_USER = "paeon.test.user";
	public static final String PAEON_TEST_PASSWORD = "paeon.test.password";
	public static final String PAEON_TEST_REPLYTO = "paeon.test.replyto";
	public static final String PAEON_TEST_REPEAT = "paeon.test.repeat";

    private static PaeonConfig PAEON_CONFIG;
    private static String brokerUrl;


    @BeforeClass
    public static void startBroker() throws Exception {
    	PAEON_CONFIG = new PaeonConfig();
    	PAEON_CONFIG.setConfigPath(PAEON_LOCAL_CFG);

        // TODO: use Properties for configuring credentials too
    	brokerUrl = PAEON_CONFIG.getProperty(PaeonConfig.PAEON_CFG_BROKER, "nio://localhost:" + PORT_START);
        String testBroker = PAEON_CONFIG.getProperty(PAEON_TEST_BROKER);
        if (testBroker != null) {
            createBroker(testBroker);
            // createBroker("one");
            // createBroker("two");
        }
    }

    @AfterClass
    public static void stopBroker() throws Exception {
        for (BrokerService b : BROKERS) {
            if (b != null) {
                b.stop();
            }
        }
    }

 
    @Test
    public void testMultipleRequests() throws Exception {
        final List<String> msgs = new ArrayList<>();

        String cu = PAEON_CONFIG.getProperty(PaeonConfig.PAEON_CFG_USER, "apollo");
        String cs = PAEON_CONFIG.getProperty(PaeonConfig.PAEON_CFG_PASSWORD, "password");
        String lq = PAEON_CONFIG.getProperty(PaeonConfig.PAEON_CFG_LISTENON, "paeon.nlp.ctakes");
        
        ConnectionFactory fc =  new ActiveMQConnectionFactory(brokerUrl);
        Connection consumerConnection = fc.createConnection(cu, cs);
        Session consumerSession = consumerConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination q = consumerSession.createQueue(lq);
        MessageConsumer consumer = consumerSession.createConsumer(q);
        consumer.setMessageListener(new PaeonMessageListener(consumerSession));
        consumerConnection.start();

        String pu = PAEON_CONFIG.getProperty(PAEON_TEST_USER, "artemis");
        String ps = PAEON_CONFIG.getProperty(PAEON_TEST_PASSWORD, "secret");
        String rt = PAEON_CONFIG.getProperty(PAEON_TEST_REPLYTO, "ctakes.replies");

        ConnectionFactory fp =  new ActiveMQConnectionFactory(brokerUrl);
        Connection producerConnection = fp.createConnection(pu, ps);
        Session producerSession = producerConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination r = producerSession.createQueue(rt);
        MessageProducer producer = producerSession.createProducer(q);
        MessageConsumer replies = producerSession.createConsumer(r);
        replies.setMessageListener(new MessageListener() {
            public void onMessage(Message message) {
                try {
                    LOG.debug("NLP Reply received: '{}'", message.getJMSMessageID());
                    if (message instanceof TextMessage) {
                        TextMessage tm = (TextMessage)message;
                        msgs.add(tm.getText());
                        LOG.debug("Paeon MessageListener text received: '{}'", tm.getText());
                    }
                } catch (JMSException e) {
                }
            }
        });
        producerConnection.start();

        try {
            String repeat = PAEON_CONFIG.getProperty(PAEON_TEST_REPEAT, "100");
            int count = Integer.valueOf(repeat);

            for (int i = 0; i < count; i++) {
                Message message = producerSession.createTextMessage("Complex provider note...");
                message.setJMSCorrelationID("CID-" + i);
                message.setJMSReplyTo(r);
                producer.send(message);
            }
            Thread.sleep(2000);

            Assert.assertEquals(count, msgs.size());
            Assert.assertNotNull(msgs.get(0));
            Assert.assertTrue(msgs.get(0).contains("result"));
        } finally {
            producerConnection.stop();
            consumerConnection.stop();
        }
    }

    public static BrokerService createBroker(String name) throws Exception {
        BrokerService b = BrokerFactory.createBroker("xbean:META-INF/org.apache.activemq/" + name + ".xml");
        if (!name.equals(b.getBrokerName())) {
            LOG.warn("Broker name mismatch; expecting '{}', found '{}'). Check configuration.", name, b.getBrokerName());
            return null;
        }
        BROKERS.add(b);
        b.start();
        b.waitUntilStarted();
        LOG.info("Broker '{}' started.", name);
        return b;
    }

}
