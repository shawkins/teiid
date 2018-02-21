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

package org.teiid.resource.adapter.google.sheets;

import static org.teiid.resource.adapter.google.sheets.ClientConstants.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.teiid.translator.google.api.SpreadsheetOperationException;
import org.teiid.translator.google.api.UpdateSet;
import org.teiid.translator.google.api.metadata.Column;
import org.teiid.translator.google.api.metadata.SpreadsheetInfo;
import org.teiid.translator.google.api.metadata.Worksheet;
import org.teiid.translator.google.api.result.RowsResult;
import org.teiid.translator.google.api.result.UpdateResult;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;


/**
 * High level api for accessing sheets via the visualization and gdata/sheets services
 */
public class SheetsAPI {

	private Sheets service;
	private OAuth2HeaderFactory headerFactory;

	public SheetsAPI(OAuth2HeaderFactory headerFactory) {
	    this.headerFactory = headerFactory;
	    this.service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, headerFactory.getCredential())
                .setApplicationName("GdataSpreadsheetBrowser") //$NON-NLS-1$
                .build();
	}

	public Spreadsheet getSpreadsheet(String spreadsheetId) throws IOException {
        return this.service.spreadsheets().get(spreadsheetId).execute();
	}
	
    /**
     * Updates spreadsheet rows. 
     * 
     * @param spreadsheetKey  key that identifies spreadsheet
     * @param worksheetID  id that identifies worksheet
     * @param criteria  update criteria
     * @param updateSet  fields that should be updated
     * @param allColumns 
     * @return number of updated rows
     */
	public UpdateResult update(String spreadsheetKey, String worksheetID, String criteria, List<UpdateSet> updateSet, List<Column> allColumns) {
	    throw new UnsupportedOperationException();
	}
	
	/**
	 * Deletes spreadsheet rows 
	 * 
	 * @param spreadsheetKey  key that identifies spreadsheet
	 * @param worksheetID  id that identifies worksheet
	 * @param criteria  delete criteria
	 * @return number of deleted rows
	 */
	public UpdateResult delete(String spreadsheetKey, String worksheetID, String criteria) {
	    throw new UnsupportedOperationException();
	}
	
    /**
     * Insert row into spreadsheet
     * @param spreadsheetKey  key that identifies spreadsheet
     * @param pairs  key that identifies worksheet
     * @param worksheet name - value pair that should be inserted into spreadsheet
     * @return 1 if the row is successfully inserted
     */
	public UpdateResult insert(String spreadsheetId, Map<String, Object> pairs, Worksheet worksheet) {
        ValueRange content = new ValueRange();

        List<Object> row = new ArrayList<>();
        for (String label : worksheet.getColumns().keySet()) {
            Object value = pairs.get(label);
            if (value != null) {
                if (value instanceof String) {
                    value = "'" + value; //$NON-NLS-1$
                } else if(!(value instanceof Boolean || value instanceof Double)) {
                    value = value.toString();
                } //else directly supported
            }
            row.add(value);
        }
        
        content.setValues(Arrays.asList(row));
        
		try {
            service.spreadsheets().values()
            .append(spreadsheetId, worksheet.getName(), content)
            .setValueInputOption("USER_ENTERED") //$NON-NLS-1$ -- TODO: this could be configurable
            .execute();
        } catch (IOException e) {
            throw new SpreadsheetOperationException("Error inserting spreadsheet row", e);
        }
	    
		return new UpdateResult(1, 1);
	}
	
    /**
     * Most important method that will issue query [1] to specific worksheet. The columns in the query
     * should be identified by their real alphabetic name (A, B, C...). 
     * 
     * There is one important restriction to query. It should not contain offset and limit clauses.
     * To achieve functionality of offset and limit please use corresponding parameters in this method.
     * 
     * 
     * [1] https://developers.google.com/chart/interactive/docs/querylanguage
     * 
     * @param query The query defined in [1]
     * @param batchSize How big portions of data should be returned by one roundtrip to Google.
     * @return Iterable RowsResult that will actually perform the roundtrips to Google for data 
     */
    public RowsResult executeQuery(SpreadsheetInfo info, String worksheetTitle,
            String query, int batchSize, Integer offset, Integer limit) {
    
        String key = info.getSpreadsheetKey();
        
        RowsResult result = new RowsResult(new DataProtocolQueryStrategy(this.headerFactory, key,worksheetTitle,query), batchSize);
        if (offset!= null)
            result.setOffset(offset);
        if (limit != null)
            result.setLimit(limit);
        
        return result;
    }
    
    public List<Column> getMetadata(String key, String worksheetTitle) {
        DataProtocolQueryStrategy dpqs = new DataProtocolQueryStrategy(this.headerFactory, key,worksheetTitle,"SELECT *"); //$NON-NLS-1$
        dpqs.getResultsBatch(0, 1);
        return dpqs.getMetadata();
    }
    
}
