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

import java.util.Set;

/**
 * @author vhalbert
 *
 */
public interface CacheEventListenerInterface {

	/**
	 * Called to add a single event to the list to be monitored for an event from JDG
	 * @param okey
	 */
	void addEvent(Object okey);
	
	/**
	 * Called to add a set of events to the list to be monitored for an event from JDG
	 * @param keyset
	 */
	void addEvents(Set<Object> keyset);
	
	/**
	 * Called to determine the number of events still in the queue to be listened for
	 * @return int
	 */
	int getEventCount();
	
	/**
	 * Utility method to differentiate between the actual listener and the noop listener.
	 * @return boolean
	 */
	boolean eventsMonitored();
	
	/**
	 * Called to clear out any events that are being monitored and reset counters.
	 */
	void reset();

}