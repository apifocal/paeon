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

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JMS consumer sample.
 */
public class PaeonMessageListener implements MessageListener {
    private static final Logger LOG = LoggerFactory.getLogger(PaeonMessageListener.class);

	// replies are sent from the same session
	private final Session session;
	private final MessageProducer producer;

	public PaeonMessageListener(Session session) throws JMSException {
		this.session = session;
		this.producer = session.createProducer(null); // TODO or maybe a configurable DLQ
	}

	@Override
	public void onMessage(Message message) {
		// TODO: use guava assertion for message not null?
		try {
			String content = null;
			String result = "";
	        if (message instanceof TextMessage) {
	        	content = ((TextMessage)message).getText();

	        	LOG.debug("Paeon MessageListener text received '{}'", content);
	        	// TODO: pass it onto cTakes processor (careful, could be null)
	        	result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><result/>";
	        }

			Destination rt = message.getJMSReplyTo();
			if (rt != null) {
				String cid = message.getJMSCorrelationID();
				// TODO: check that cid is not null? shouldn't be for request/reply
				LOG.debug("Sending reply with correlation id '{}' back to '{}'", cid, rt);
				Message reply = session.createTextMessage(result);
				reply.setJMSCorrelationID(cid);
				producer.send(rt, reply);
			}
		} catch (JMSException e) {
			// FIXME: don't ignore forever
		}

	}


}
