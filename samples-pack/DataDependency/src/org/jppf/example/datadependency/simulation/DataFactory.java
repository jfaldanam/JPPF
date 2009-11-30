/*
 * JPPF.
 * Copyright (C) 2005-2009 JPPF Team.
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

package org.jppf.example.datadependency.simulation;

import java.util.*;

import org.jppf.example.datadependency.model.*;

/**
 * Utility class to generate trade and market data objects for simulation.
 * @author Laurent Cohen
 */
public class DataFactory
{
	/**
	 * Random number generator.
	 */
	private Random random = new Random(System.currentTimeMillis());

	/**
	 * Generate a random number in the specified range.
	 * @param min the minimum random value.
	 * @param max the maximum random value.
	 * @return a pseudo-random number in the specified range.
	 */
	public int getRandomInt(int min, int max)
	{
		return min + random.nextInt(max - min + 1);
	}

	/**
	 * Generate the specified number of data market objects.
	 * Each generated object has an id in the format &quot;Dn&quot; where <i>n</i> is a sequence number.
	 * @param n the number of objects to generate.
	 * @return a list of <code>MarketData</code> instances.
	 */
	public List<MarketData> generateDataMarketObjects(int n)
	{
		List<MarketData> result = new ArrayList<MarketData>();
		for (int i=1; i<=n; i++) result.add(new MarketData("D" + i));
		return result;
	}
	
	/**
	 * Generate a list of trade objects with their dependencies.
	 * Each generated object has an id in the format &quot;Tn&quot; where <i>n</i> is a sequence number.
	 * The dependencies are randomly chosen from the specified list of data amrket objects.
	 * and their number varies between the specified min and max values.
	 * @param nbTrades the number of trade objects to generate.
	 * @param dataList the list of market data objects to create the dependencies from.
	 * @param minData the minimum number of dependencies per trade (inclusive).
	 * @param maxData the maximum number of dependencies per trade (inclusive).
	 * @return a list of <code>Trade</code> instances.
	 */
	public List<Trade> generateTradeObjects(int nbTrades, List<MarketData> dataList, int minData, int maxData)
	{
		List<Trade> result = new ArrayList<Trade>();
		for (int i=1; i<nbTrades; i++)
		{
			Trade trade = new Trade("T" + i);
			int n = getRandomInt(minData, maxData);
			SortedSet<String> dependencies = trade.getDataDependencies();
			List<Integer> indices = new LinkedList<Integer>();
			for (int k=0; k<dataList.size(); k++) indices.add(k);
			for (int j=0; j<n; j++)
			{
				int p = indices.remove(random.nextInt(indices.size()));
				dependencies.add(dataList.get(p).getId());
			}
			result.add(trade);
		}
		return result;
	}
}
