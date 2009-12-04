/*
 * JPPF.
 *  Copyright (C) 2005-2009 JPPF Team. 
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jppf.process;

import java.util.Properties;

/**
 * Utility class used to generate the configuration properties of a JPPF driver,
 * to be used when executing a JPPF driver process. 
 * @author Laurent Cohen
 */
public class DriverPropertiesBuilder
{
	/**
	 * "smooth" tuning profile.
	 */
	public static final Properties SMOOTH_PROFILE = buildProfile("smooth", 500, 300, 0.2f, 10, 1.5f, 0.2f); 
	/**
	 * "agressive" tuning profile.
	 */
	public static final Properties AGGRESSIVE_PROFILE = buildProfile("aggressive", 100, 50, 0.2f, 10, 1.5f, 0.2f); 
	/**
	 * "agressive" tuning profile.
	 */
	public static final Properties AGGRESSIVE_TUNING = buildTuning(50, "aggressive", AGGRESSIVE_PROFILE); 
	/**
	 * Base config for driver 1.
	 */
	public static final Properties BASE_1 = buildBase(11111, 11112, 11113, 8000, false); 
	/**
	 * Base config for driver 2.
	 */
	public static final Properties BASE_2 = buildBase(11121, 11122, 11123, 8001, false); 
	/**
	 * Driver 1 configuration.
	 */
	public static final Properties DRIVER_1 = buildDriverConfig(BASE_1, AGGRESSIVE_TUNING, null); 
	/**
	 * Driver 2 configuration.
	 */
	public static final Properties DRIVER_2 = buildDriverConfig(BASE_2, AGGRESSIVE_TUNING, null); 

	/**
	 * Create a complete driver configuration.
	 * @param base the base config properties.
	 * @param tuning the tuning strategy properties.
	 * @param peers the peers configuration properties.
	 * @return a <code>Properties</code> instance.
	 */
	public static Properties buildDriverConfig(Properties base, Properties tuning, Properties peers)
	{
		Properties props = new Properties();
		if (base != null) props.putAll(base);
		if (tuning != null) props.putAll(tuning);
		if (peers != null) props.putAll(peers);
		return props;
	}

	/**
	 * Generate the configurationproperties for a driver.
	 * @param classPort the port number for the class server.
	 * @param appPort the port number for the class server.
	 * @param nodePort the port number for the class server.
	 * @param debugPort the debug port number.
	 * @param suspend flag to determine whether to suspend the driver JVM on startup.
	 * @return a <code>Properties</code> instance.
	 */
	public static Properties buildBase(int classPort, int appPort, int nodePort, int debugPort, boolean suspend)
	{
		Properties props = new Properties();
		props.setProperty("class.server.port", "" + classPort);
		props.setProperty("app.server.port", "" + appPort);
		props.setProperty("node.server.port", "" + nodePort);

		props.setProperty("remote.debug.port", "" + debugPort);
		props.setProperty("remote.debug.suspend", "" + suspend);

		props.setProperty("reconnect.initial.delay", "1");
		props.setProperty("reconnect.max.time", "-1");
		props.setProperty("reconnect.interval", "1");
		props.setProperty("max.memory.option", "128");

		return props;
	}

	/**
	 * Create a tuning profile configuration.
	 * @param size the initial bundle size.
	 * @param name the profile name.
	 * @param profileProps the performance profile configuration.
	 * @return a <code>Properties</code> instance.
	 */
	public static Properties buildTuning(int size, String name, Properties profileProps)
	{
		Properties props = new Properties();
		props.setProperty("task.bundle.size", "" + size);
		props.setProperty("task.bundle.strategy", name == null ? "manual" : "autotuned");
		if (name != null)
		{
			props.setProperty("task.bundle.autotuned.strategy", name);
			props.putAll(profileProps);
		}
		return props;
	}

	/**
	 * Create a tuning profile configuration.
	 * @param name the profile name.
	 * @param minSamplesToAnalyse minimum number of samples ffor analysis.
	 * @param minSamplesToCheckConvergence min number of samples for checking convergence.
	 * @param maxDeviation maximum deviation.
	 * @param maxGuessToStable max guesses before considering the bundle size stable.
	 * @param sizeRatioDeviation sie ration deviation.
	 * @param decreaseRatio decrease ratio.
	 * @return a <code>Properties</code> instance.
	 */
	public static Properties buildProfile(String name, int minSamplesToAnalyse, int minSamplesToCheckConvergence, 
		float maxDeviation, int maxGuessToStable, float sizeRatioDeviation, float decreaseRatio)
	{
		Properties props = new Properties();
		String prefix = "strategy." + name + ".";
		props.setProperty(prefix + "minSamplesToAnalyse", "" + minSamplesToAnalyse);
		props.setProperty(prefix + "minSamplesToCheckConvergence", "" + minSamplesToCheckConvergence);
		props.setProperty(prefix + "maxDeviation", "" + maxDeviation);
		props.setProperty(prefix + "maxGuessToStable", "" + maxGuessToStable);
		props.setProperty(prefix + "sizeRatioDeviation", "" + sizeRatioDeviation);
		props.setProperty(prefix + "decreaseRatio", "" + decreaseRatio);

		return props;
	}

	/**
	 * Create a tuning profile configuration.
	 * @param peerNames a space-separated list of peer names.
	 * @param peers the configurations of the peers.
	 * @return a <code>Properties</code> instance.
	 */
	public static Properties buildAllPeers(String peerNames, Properties[] peers)
	{
		Properties props = new Properties();
		props.setProperty("jppf.peers", peerNames);
		for (Properties peer: peers) props.putAll(peer);
		return props;
	}

	/**
	 * Create a tuning profile configuration.
	 * @param name the configuration name of the peer driver.
	 * @param host host where the peer driver lives.
	 * @param classPort the port number for the class server on the peer driver.
	 * @param nodePort the port number for the class server on the peer driver.
	 * @return a <code>Properties</code> instance.
	 */
	public static Properties buildPeer(String name, String host, int classPort, int nodePort)
	{
		Properties props = new Properties();
		props.setProperty("jppf.peer." + name + ".server.host", host);
		props.setProperty("node.peer." + name + ".server.port", "" + nodePort);
		props.setProperty("class.peer." + name + ".server.port", "" + classPort);
		return props;
	}
}
