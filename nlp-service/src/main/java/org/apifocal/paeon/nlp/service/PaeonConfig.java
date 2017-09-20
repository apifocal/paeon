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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JMS consumer sample.
 */
public class PaeonConfig {
    private static final Logger LOG = LoggerFactory.getLogger(PaeonConfig.class);

	public static final String DEFAULT_CONFIG_FILE = "paeon-nlp.cfg";
	public static final String DEFAULT_CONFIG_DIR = "/etc/paeon";
	public static final String PAEON_CONFIG_DIR = ".paeon";
	public static final String PAEON_CONFIG_ENV = "PAEON_CONFIG";

	public static final String PAEON_CFG_BROKER = "paeon.broker";
	public static final String PAEON_CFG_USER = "paeon.user";
	public static final String PAEON_CFG_PASSWORD = "paeon.password";
	public static final String PAEON_CFG_LISTENON = "paeon.listenon";

	private final Properties config = new Properties();
	private Path configPath;

	/** 
	 * Paeon uses a .properties file based configuration named 'paeon-nlp.cfg'
	 * The default location for the configuration file is '/etc/paeon/' (Linux default),
	 * but that is overwritten by the configuration in '$USER_HOME/.paeon/' or by
	 * a configuration placed in a directory defined by the '$PAEON_CONFIG'.
	 * Paeon services cannot run without an admin provided configuration, so there
	 * cannot be defaults. Therefore, unit tests should use the getProperty(String, String)
	 * variant to provide default values.
	 */

	public PaeonConfig() {
		configPath = firstValid(envConfigPath(), userConfigPath(), systemConfigPath());
		reset();
	}

	private void reset() {
		config.clear();
		if (configPath != null) {
			try {
			    LOG.info("Loading Paeon configuration from: {}", configPath);
				config.load(Files.newInputStream(configPath, StandardOpenOption.READ));
			} catch (IOException e) {
				LOG.warn("Failed to load Paeon configuration at {}. Reason: {}", configPath, e.getLocalizedMessage());
			}
		} else {
		    LOG.warn("Paeon configuration not found on this host");
		}
	}

	public String getConfigPath() {
		return configPath == null ? null : configPath.toString();
	}

	public void setConfigPath(String path) {
		this.configPath = firstValid(Paths.get(path));
		reset();
	}

	public String getProperty(String key) {
		return config.getProperty(key);
	}

	public String getProperty(String key, String def) {
		return config.getProperty(key, def);
	}


	public Path systemConfigPath() {
		return Paths.get(DEFAULT_CONFIG_DIR, DEFAULT_CONFIG_FILE);
	}

	public Path userConfigPath() {
		String home = System.getProperty("user.home");
		return home == null ? null : Paths.get(home, PAEON_CONFIG_DIR, DEFAULT_CONFIG_FILE);
	}

	public Path envConfigPath() {
		String env = System.getProperty(PAEON_CONFIG_ENV);
		return env == null ? null : Paths.get(env);
	}

	public Path firstValid(Path... paths) {
		for (Path p : paths) {
			if (p!= null && Files.exists(p) && Files.isRegularFile(p)) {
				return p;
			}
		}
		return null;
	}
}
