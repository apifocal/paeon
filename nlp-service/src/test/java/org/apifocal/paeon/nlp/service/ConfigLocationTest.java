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

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ConfigLocationTest {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigLocationTest.class);
    private static final String PAEON_LOCAL_CFG = "src/test/resources/META-INF/org.apifocal.paeon/paeon-nlp.cfg";
    private static final String PAEON_DEFAULT_DEST = "paeon.nlp.ctakes";

    @Test
    public void testNoConfig() throws Exception {
    	PaeonConfig cfg = new PaeonConfig();
    	String path = cfg.getConfigPath();
    	if (path != null) {
    		LOG.warn("Paeon configuration ");
    	}
    	// If local configuration is present on the host, it should not be the one from the source code
        Assert.assertNotEquals(PAEON_LOCAL_CFG, path);
    }

    @Test
    public void testLocalConfig() throws Exception {
    	PaeonConfig cfg = new PaeonConfig();
    	cfg.setConfigPath(PAEON_LOCAL_CFG);

    	Assert.assertEquals(PAEON_LOCAL_CFG, cfg.getConfigPath());
    	Assert.assertEquals(PAEON_DEFAULT_DEST, cfg.getProperty(PaeonConfig.PAEON_CFG_LISTENON));
    }
}
