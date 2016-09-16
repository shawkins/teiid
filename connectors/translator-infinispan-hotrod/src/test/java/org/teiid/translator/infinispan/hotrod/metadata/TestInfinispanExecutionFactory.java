/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */
package org.teiid.translator.infinispan.hotrod.metadata;

import static org.junit.Assert.assertNotNull;

import org.jboss.as.quickstarts.datagrid.hotrod.query.domain.PersonCacheSource;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.teiid.language.Select;
import org.teiid.translator.ExecutionContext;
import org.teiid.translator.TranslatorException;
import org.teiid.translator.infinispan.hotrod.InfinispanExecutionFactory;
import org.teiid.translator.infinispan.hotrod.InfinispanHotRodConnection;
import org.teiid.translator.object.ObjectExecution;
import org.teiid.translator.object.testdata.person.PersonSchemaVDBUtility;

@SuppressWarnings("nls")
public class TestInfinispanExecutionFactory {
	
	protected static InfinispanExecutionFactory TRANSLATOR;

	
	@Mock
	private ExecutionContext context;
	
	@Mock
	private Select command;
	
	
	private InfinispanHotRodConnection connection = PersonCacheSource.createConnection(true);
	
    @BeforeClass
    public static void setUp() throws TranslatorException {
        TRANSLATOR = new InfinispanExecutionFactory();
        TRANSLATOR.start();
    }	

	@Before public void beforeEach() throws Exception{	
 
		MockitoAnnotations.initMocks(this);
    }

	@Test public void testFactory() throws Exception {

		ObjectExecution exec = (ObjectExecution) TRANSLATOR.createExecution(command, context, PersonSchemaVDBUtility.RUNTIME_METADATA, connection);
		
		assertNotNull(exec);
		assertNotNull(TRANSLATOR.getMetadataProcessor());
	}	
	
}
