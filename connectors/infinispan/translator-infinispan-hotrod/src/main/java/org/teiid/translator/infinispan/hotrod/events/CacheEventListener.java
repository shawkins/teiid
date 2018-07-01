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

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryModified;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryRemoved;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.event.ClientCacheEntryCreatedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryModifiedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryRemovedEvent;
import org.teiid.logging.LogConstants;
import org.teiid.logging.LogManager;

/**
 * @author vhalbert
 *
 */
@ClientListener
public class CacheEventListener implements CacheEventListenerInterface {

	private int cnt = 0;
	private int pre = 0;

	Set<Object> key = Collections.newSetFromMap(new ConcurrentHashMap<Object, Boolean>());

	@Override
	public void reset() {
		key.clear();
		cnt = 0;
		pre = 0;
	}
	@Override
	public boolean eventsMonitored() {
		return true;
	}

	@Override
	public int getEventCount() {
		return key.size();
	}

	@Override
	public void addEvent(final Object okey) {
		key.add(okey);
	    LogManager.logTrace(LogConstants.CTX_CONNECTOR, ++pre + " [CacheEventListener] Add event (pre) " + okey);
	}

	@Override
	public void addEvents(Set<Object> keyset) {
		key.addAll(keyset);
	}

	@ClientCacheEntryModified
	public void handle(ClientCacheEntryModifiedEvent event) {
		if (pre > 0) {
			key.remove(event.getKey());
			LogManager.logTrace(LogConstants.CTX_CONNECTOR, ++cnt + " [CacheEventListener] Modified entry (post) " + event.getKey());
		}
	}

	@ClientCacheEntryCreated
	public void handle(ClientCacheEntryCreatedEvent event) {
		if (pre > 0) {
			key.remove(event.getKey());
			LogManager.logTrace(LogConstants.CTX_CONNECTOR, ++cnt + " [CacheEventListener] Created entry (post) " + event.getKey());
		}
	}

	@ClientCacheEntryRemoved
	public void handle(ClientCacheEntryRemovedEvent event) {
		if (pre > 0) {
			key.remove(event.getKey());
			LogManager.logTrace(LogConstants.CTX_CONNECTOR, ++cnt + " [CacheEventListener] Removed entry (post) " + event.getKey());
		}
	}

}
