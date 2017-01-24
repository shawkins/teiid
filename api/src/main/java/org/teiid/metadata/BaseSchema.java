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

package org.teiid.metadata;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.teiid.connector.DataPlugin;
import org.teiid.core.util.StringUtil;
import org.teiid.metadata.BaseColumn.NullType;
import org.teiid.metadata.Datatype.Type;

/**
 * Defines a base schema that is the holder for namespace and type information
 */
public class BaseSchema extends AbstractMetadataRecord {
    
    static final String TEIID_RESERVED = "teiid_"; //$NON-NLS-1$
    private static final String TEIID_SF = "teiid_sf"; //$NON-NLS-1$
    private static final String TEIID_RELATIONAL = "teiid_rel"; //$NON-NLS-1$
    private static final String TEIID_WS = "teiid_ws"; //$NON-NLS-1$
    private static final String TEIID_MONGO = "teiid_mongo"; //$NON-NLS-1$
    private static final String TEIID_ODATA = "teiid_odata"; //$NON-NLS-1$
    private static final String TEIID_ACCUMULO = "teiid_accumulo"; //$NON-NLS-1$
    private static final String TEIID_EXCEL = "teiid_excel"; //$NON-NLS-1$
    private static final String TEIID_JPA = "teiid_jpa"; //$NON-NLS-1$
    private static final String TEIID_HBASE = "teiid_hbase"; //$NON-NLS-1$
    private static final String TEIID_SPATIAL = "teiid_spatial"; //$NON-NLS-1$
    private static final String TEIID_LDAP = "teiid_ldap"; //$NON-NLS-1$
    private static final String TEIID_REST = "teiid_rest"; //$NON-NLS-1$
    private static final String TEIID_PI = "teiid_pi"; //$NON-NLS-1$

    public static final String SF_URI = "{http://www.teiid.org/translator/salesforce/2012}"; //$NON-NLS-1$
    public static final String WS_URI = "{http://www.teiid.org/translator/ws/2012}"; //$NON-NLS-1$
    public static final String MONGO_URI = "{http://www.teiid.org/translator/mongodb/2013}"; //$NON-NLS-1$
    public static final String ODATA_URI = "{http://www.jboss.org/teiiddesigner/ext/odata/2012}"; //$NON-NLS-1$
    public static final String ACCUMULO_URI = "{http://www.teiid.org/translator/accumulo/2013}"; //$NON-NLS-1$
    public static final String EXCEL_URI = "{http://www.teiid.org/translator/excel/2014}"; //$NON-NLS-1$
    public static final String JPA_URI = "{http://www.teiid.org/translator/jpa/2014}"; //$NON-NLS-1$
    public static final String HBASE_URI = "{http://www.teiid.org/translator/hbase/2014}"; //$NON-NLS-1$
    public static final String SPATIAL_URI = "{http://www.teiid.org/translator/spatial/2015}"; //$NON-NLS-1$
    public static final String LDAP_URI = "{http://www.teiid.org/translator/ldap/2015}"; //$NON-NLS-1$
    public static final String REST_URI = "{http://teiid.org/rest}"; //$NON-NLS-1$
    public static final String PI_URI = "{http://www.teiid.org/translator/pi/2016}"; //$NON-NLS-1$

    public static final Map<String, String> BUILTIN_NAMESPACES;
    static {
        Map<String, String> map = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
        map.put(TEIID_RELATIONAL, AbstractMetadataRecord.RELATIONAL_URI.substring(1, AbstractMetadataRecord.RELATIONAL_URI.length()-1));
        map.put(TEIID_SF, SF_URI.substring(1, SF_URI.length()-1));
        map.put(TEIID_WS, WS_URI.substring(1, WS_URI.length()-1));
        map.put(TEIID_MONGO, MONGO_URI.substring(1, MONGO_URI.length()-1));
        map.put(TEIID_ODATA, ODATA_URI.substring(1, ODATA_URI.length()-1));
        map.put(TEIID_ACCUMULO, ACCUMULO_URI.substring(1, ACCUMULO_URI.length()-1));
        map.put(TEIID_EXCEL, EXCEL_URI.substring(1, EXCEL_URI.length()-1));
        map.put(TEIID_JPA, JPA_URI.substring(1, JPA_URI.length()-1));
        map.put(TEIID_HBASE, HBASE_URI.substring(1, HBASE_URI.length()-1));
        map.put(TEIID_SPATIAL, SPATIAL_URI.substring(1, SPATIAL_URI.length()-1));
        map.put(TEIID_LDAP, LDAP_URI.substring(1, LDAP_URI.length()-1));
        map.put(TEIID_REST, REST_URI.substring(1, REST_URI.length()-1));
        map.put(TEIID_PI, PI_URI.substring(1, PI_URI.length()-1));
        BUILTIN_NAMESPACES = Collections.unmodifiableMap(map);
    }
    
    protected Map<String, String> namespaces;
    protected Map<String, Datatype> dataTypes;
    protected Map<String, Datatype> enterpriseDataTypes;
    
    public void addNamespace(String prefix, String uri) {
        if (uri == null || uri.indexOf('}') != -1) {
            throw new MetadataException(DataPlugin.Event.TEIID60018, DataPlugin.Util.gs(DataPlugin.Event.TEIID60018, uri));
        }
        
        if (StringUtil.startsWithIgnoreCase(prefix, MetadataFactory.TEIID_RESERVED)) {
            String validURI = MetadataFactory.BUILTIN_NAMESPACES.get(prefix);
            if (validURI == null || !uri.equals(validURI)) {
                throw new MetadataException(DataPlugin.Event.TEIID60017, DataPlugin.Util.gs(DataPlugin.Event.TEIID60017, prefix));
            }
        }
        
        if (this.namespaces == null) {
             this.namespaces = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
        }
        this.namespaces.put(prefix, uri);
    }
    
    public void addDomain(String name, String baseType, Integer precision, Integer scale, boolean notNull) {
        //TODO: allow named array types
        // requires either storing the dimension on the datatype, or using a holder
        /*int dimensions = 0;
        while (DataTypeManager.isArrayType(baseType)) {
            baseType = DataTypeManager.getComponentType(baseType);
            dimensions++;
        }*/
        Datatype base = getDataTypes().get(baseType);
        if (base == null || !base.isBuiltin()) {
            throw new MetadataException(DataPlugin.Event.TEIID60032, DataPlugin.Util.gs(DataPlugin.Event.TEIID60032, baseType));
        }
        Datatype dataType = base.clone();
        dataType.setName(name);
        dataType.setBasetypeName(baseType);
        dataType.setType(Type.UserDefined);
        //dataType.setUUID(uuid);
        
        if (precision != null) {
            dataType.setPrecision(precision);
        }
        if (scale != null) {
            dataType.setScale(scale);
        }
        if (notNull) {
            dataType.setNullType(NullType.No_Nulls);
        }
        
        addDatatype(dataType);
    }
    
    public Map<String, String> getNamespaces() {
        if (this.namespaces == null) {
            return Collections.emptyMap();
        }
        return this.namespaces;
    }
    
    public static String resolvePropertyKey(BaseSchema baseSchema, String key) {
        int index = key.indexOf(':');
        if (index > 0 && index < key.length() - 1) {
            String prefix = key.substring(0, index);
            String uri = BUILTIN_NAMESPACES.get(prefix);
            if (uri == null && baseSchema != null) {
                uri = baseSchema.getNamespaces().get(prefix);
            }
            if (uri != null) {
                key = '{' +uri + '}' + key.substring(index + 1, key.length());
            }
            //TODO warnings or errors if not resolvable 
        }
        return key;
    }
    
    /**
     * Add a Datatype
     * @param datatype
     */
    public void addDatatype(Datatype datatype) {
        if (this.dataTypes == null) {
            this.dataTypes = new TreeMap<String, Datatype>(String.CASE_INSENSITIVE_ORDER);
        }
        if (this.dataTypes.containsKey(datatype.getName())) {
            throw new MetadataException(DataPlugin.Event.TEIID60033, DataPlugin.Util.gs(DataPlugin.Event.TEIID60033, datatype.getName()));
        }
        this.dataTypes.put(datatype.getName(), datatype);
    }

    /**
     * Add an enterprise type (typically a Designer defined type extension)- typically not called
     * @param datatype
     */
    public void addEnterpriseDatatype(Datatype datatype) {
        //we have to hold these separately, as the built-in/runtime types should be considered
        //unmodifiable.
        //
        //however we still have an issue in that designer treats these as vdb scoped, while
        //we're treating them as schema scoped.  any refinement of the type system
        //should correct this.
        //
        //TODO: should throw an exception if there is a conflict with a built-in type
        if (this.enterpriseDataTypes == null) {
            this.enterpriseDataTypes = new TreeMap<String, Datatype>(String.CASE_INSENSITIVE_ORDER);
        }
        this.enterpriseDataTypes.put(datatype.getUUID(), datatype);
    }

    /**
     * Get an enterprise type (typically a Designer defined type extension) by uuid.
     * @param name
     * @return
     */
    public Datatype getEnterpriseDatatype(String uuid) {
        if (this.enterpriseDataTypes == null) {
            return null;
        }
        return this.enterpriseDataTypes.get(uuid);
    }
    
    /**
     * get runtime types keyed by runtime name, which is
     * a type name known to the Teiid engine
     * @return
     */
    public Map<String, Datatype> getDataTypes() {
        return this.dataTypes;
    }
    
    public void setDataTypes(Map<String, Datatype> runtimeTypes) {
        this.dataTypes = new TreeMap<String, Datatype>(String.CASE_INSENSITIVE_ORDER);
        this.dataTypes.putAll(runtimeTypes);
    }

}
