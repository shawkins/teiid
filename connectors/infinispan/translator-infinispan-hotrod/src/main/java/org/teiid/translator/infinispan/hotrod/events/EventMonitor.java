/*
 * Copyright Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags and
 * the COPYRIGHT.txt file distributed with this work.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.teiid.translator.infinispan.hotrod.events;

import java.util.HashMap;
import java.util.Map;

import org.infinispan.client.hotrod.RemoteCache;
import org.teiid.logging.LogConstants;
import org.teiid.logging.LogManager;

/**
 * @author vhalbert
 *
 */
final public class EventMonitor {
	
	static Map<String, CacheEventListenerInterface> ALIAS_CACHE_EVENTS = new HashMap<String, CacheEventListenerInterface>(); 
	
	private static final CacheEventNoOpListener NOOP_LISTENER = new CacheEventNoOpListener();

	/**
	 * Should be only called by the direct query that triggers the cleaning of the cache before starting materialization
	 * @param cache
	 */
	public static synchronized void addListenerInstance(@SuppressWarnings("rawtypes") RemoteCache cache) {	
		CacheEventListener m = (CacheEventListener) ALIAS_CACHE_EVENTS.get(cache.getName());
		if (m == null) {
			m = new CacheEventListener();
			ALIAS_CACHE_EVENTS.put(cache.getName(), m);
		    cache.addClientListener(m);
		}
		m.reset();		
	}
	
	public static CacheEventListenerInterface getListenerInstance(String cacheName) {
		synchronized (EventMonitor.class) {
	        if (ALIAS_CACHE_EVENTS.containsKey(cacheName)) {
	            return ALIAS_CACHE_EVENTS.get(cacheName);
	        }
        }
		
	    LogManager.logTrace(LogConstants.CTX_CONNECTOR, "[MaterializationEventMonitor] getListenerInstance is returning CacheEventNoOPListener for cache " + cacheName);
		return NOOP_LISTENER;
	}
	
}
