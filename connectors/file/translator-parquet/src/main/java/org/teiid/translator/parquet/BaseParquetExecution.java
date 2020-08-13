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

package org.teiid.translator.parquet;



import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.parquet.column.page.PageReadStore;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.example.data.simple.convert.GroupRecordConverter;
import org.apache.parquet.filter2.compat.FilterCompat;
import org.apache.parquet.filter2.predicate.FilterApi;
import org.apache.parquet.filter2.predicate.FilterPredicate;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.util.HadoopInputFile;
import org.apache.parquet.io.ColumnIOFactory;
import org.apache.parquet.io.MessageColumnIO;
import org.apache.parquet.io.RecordReader;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.Type;
import org.teiid.file.VirtualFile;
import org.teiid.file.VirtualFileConnection;
import org.teiid.language.Comparison;
import org.teiid.language.LanguageObject;
import org.teiid.language.Literal;
import org.teiid.metadata.RuntimeMetadata;
import org.teiid.translator.DataNotAvailableException;
import org.teiid.translator.Execution;
import org.teiid.translator.ExecutionContext;
import org.teiid.translator.TranslatorException;

import static org.apache.parquet.filter2.predicate.Operators.*;


public class BaseParquetExecution implements Execution {
    @SuppressWarnings("unused")
    protected ExecutionContext executionContext;
    @SuppressWarnings("unused")
    protected RuntimeMetadata metadata;
    protected VirtualFileConnection connection;
    protected boolean immutable;

    // Execution state
    protected ParquetQueryVisitor visitor = new ParquetQueryVisitor();
    private VirtualFile[] parquetFiles;
    private AtomicInteger fileCount = new AtomicInteger();
    private ParquetFileReader reader;
    private MessageType schema;
    private MessageType filteredSchema;
    private RecordReader<Group> rowIterator;
    private MessageColumnIO columnIO;
    private long pageRowCount;

    public BaseParquetExecution(ExecutionContext executionContext,
                                RuntimeMetadata metadata, VirtualFileConnection connection, boolean immutable) {
        this.executionContext = executionContext;
        this.metadata = metadata;
        this.connection = connection;
        this.immutable = immutable;
    }

    public void visit(LanguageObject command) throws TranslatorException {
        this.visitor.visitNode(command);

        if (!visitor.exceptions.isEmpty()) {
            throw visitor.exceptions.get(0);
        }
    }

    @Override
    public void execute() throws TranslatorException {
        String path = this.visitor.getParquetPath();
        if(this.visitor.getTable().getProperty(ParquetMetadataProcessor.PARTITIONING_SCHEME) != null && this.visitor.getTable().getProperty(ParquetMetadataProcessor.PARTITIONING_SCHEME).equals("directory")){
            path += getDirectoryPath(this.visitor.getColumnPredicates(), this.visitor.getTable().getProperty(ParquetMetadataProcessor.PARTITIONED_COLUMNS));
        }
        this.parquetFiles = VirtualFileConnection.Util.getFiles(path, this.connection, true);
        VirtualFile nextParquetFile = getNextParquetFile();
        if (nextParquetFile != null) {
            this.rowIterator = readParquetFile(nextParquetFile);
        }
    }

    private String getDirectoryPath(HashMap<String, Comparison> predicates, String partitionedColumns) throws TranslatorException {
        String path = ""; // because we are only supporting the equality comparison as of now, otherwise we would return a list of paths to iterate over
        String[] partitionedColumnsArray = getPartitionedColumns(partitionedColumns);
        for(int i = 0; i < partitionedColumnsArray.length; i++){
          if(predicates.containsKey(partitionedColumnsArray[i])) {
              Comparison predicate = predicates.get(partitionedColumnsArray[i]);
              predicates.remove(partitionedColumnsArray[i]);
              Literal l = (Literal) predicate.getRightExpression();
              path += "/" + partitionedColumnsArray[i] + "=";
              switch (predicate.getOperator()) {
                  case EQ:
                      switch (l.getType().getName()){
                          case "java.lang.String":
                          case "java.math.BigInteger":
                          case "java.lang.Long":
                          case "java.lang.Integer":
                          case "java.lang.Boolean":
                              path += l.getValue().toString();
                              break;
                          default:
                              throw new TranslatorException("Only string, biginteger, long, integer, boolean are supported.");
                      }
                      break;
                  default: throw new TranslatorException("Only equal comparison is allowed");
              }
          }
          else {
              path += "/*";
          }
        }
    return path + "/*";
    }

    private FilterCompat.Filter getRowGroupFilter(HashMap<String, Comparison> columnPredicates) throws TranslatorException {
        if(columnPredicates.size() == 0){
            return null;
        }
//        FilterCompat.Filter rowGroupFilter;
//        LongColumn column1 = FilterApi.longColumn("id");
//        long l=3;
//        Column column2 = FilterApi.longColumn("jk");
//        FilterPredicate predicate =  FilterApi.eq(column2, l);
//        FilterPredicate predicate1 = FilterApi.and(predicate, )
        int flag = 1;
        FilterCompat.Filter rowGroupFilter;
        Iterator hashMapIterator = columnPredicates.entrySet().iterator();
        while(hashMapIterator.hasNext()){
            Map.Entry mapElement = (Map.Entry) hashMapIterator.next();
            String columnName = (String) mapElement.getKey();
            Comparison comparison = (Comparison) mapElement.getValue();
            Literal l = (Literal) comparison.getRightExpression();
            Column column;
            switch (comparison.getRightExpression().getType().getName()){
                case "java.lang.String":
                case "BinaryType":
                    column = FilterApi.binaryColumn(columnName);
                    break;
                case "java.lang.Long":
                    column = FilterApi.longColumn(columnName);
                    break;
                case "java.lang.Integer":
                    column = FilterApi.intColumn(columnName);
                    break;
                case "java.lang.Boolean":
                    column = FilterApi.booleanColumn(columnName);
                    break;
                case "java.lang.Double":
                    column = FilterApi.doubleColumn(columnName);
                    break;
                case "java.lang.Float":
                    column =FilterApi.floatColumn(columnName);
                    break;
                default:
                    throw new TranslatorException("The type " + comparison.getRightExpression().getType().getName() + " is not supported for comparison.");
            }
            switch (comparison.getOperator()){
                case EQ:
//                    FilterPredicate filterPredicate = FilterApi.eq(column, (Binary) getParquetValue(columnName, l, comparison));
                    break;
                case NE:
//                    filterPredicate =
                    break;
                case LT:
                    break;
                case LE:
                    break;
                case GT:
                    break;
                case GE:
                    break;
            }
        }
        return null;
    }

    private Object getParquetValue(String columnName, Literal l, Comparison comparison) throws TranslatorException {
        switch (comparison.getRightExpression().getType().getName()){
            case "java.lang.String":
                Binary binary = Binary.fromString((String) l.getValue());
                return binary;
            case "java.lang.Long":
                break;
            case "java.lang.Integer":
                break;
            case "java.lang.Boolean":
                break;
            case "java.lang.Double":
                break;
            case "java.lang.Float":
                break;
            case "BinaryType":
                break;
            default:
                throw new TranslatorException("The type " + comparison.getRightExpression().getType().getName() + " is not supported for comparison.");
        }
        return null;
    }

    private RecordReader<Group> readParquetFile(VirtualFile parquetFile) throws TranslatorException {
        try (InputStream parquetFileStream = parquetFile.openInputStream(!immutable)) {
           return readParquetFile(parquetFile, parquetFileStream);
        } catch (IOException e) {
            throw new TranslatorException(e);
        }
    }

    private RecordReader<Group> readParquetFile(VirtualFile parquetFile, InputStream parquetFileStream) throws TranslatorException {
       try {
           File localFile = createTempFile(parquetFileStream);
           Path path = new Path(localFile.toURI());
           Configuration config = new Configuration();
//           LongColumn column = FilterApi.longColumn("id");
//           long l=3;
//           FilterCompat.Filter rowGroupFilter = FilterCompat.get(FilterApi.lt(column, l));
           FilterCompat.Filter rowGroupFilter = getRowGroupFilter(this.visitor.getColumnPredicates());
           reader = ParquetFileReader.open(HadoopInputFile.fromPath(path, config));
           schema = reader.getFooter().getFileMetaData().getSchema();
           filteredSchema = getFilteredSchema(schema, this.visitor.getProjectedColumnNames());
           columnIO = new ColumnIOFactory().getColumnIO(filteredSchema);
           PageReadStore pages = reader.readNextRowGroup();
           pageRowCount = pages.getRowCount();
           return columnIO.getRecordReader(pages, new GroupRecordConverter(filteredSchema));
       } catch (IOException e){
           throw new TranslatorException(e);
       }
    }

    private MessageType getFilteredSchema(MessageType schema, List<String> expectedColumnNames) {
        List<Type> allColumns = schema.getFields();
        List<Type> selectedColumns = new ArrayList<>();
        for(String columnName: expectedColumnNames) {
            selectedColumns.add(allColumns.get(schema.getFieldIndex(columnName)));
        }
        MessageType filteredSchema = new MessageType(schema.getName(), selectedColumns);
        return filteredSchema;
    }


    private String getExtension(String name) {
        String extension = "";
        int i = name.lastIndexOf('.');
        if (i > 0) {
            extension = name.substring(i+1);
        }
        return extension;
    }

    private File createTempFile(InputStream parquetFileStream) throws IOException {
        File tempFile = File.createTempFile(getClass().getSimpleName(), ".tmp");
        tempFile.deleteOnExit();
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            IOUtils.copyBytes(parquetFileStream, out, 131072);
        }
        return tempFile;
    }

    public Group nextRow() throws TranslatorException, DataNotAvailableException {
            try {
                Group row = nextRowInternal();
                if (row == null) {
                    return null;
                }
                return row;
            } catch (IOException e) {
                throw new TranslatorException(e);
            }
    }

    private Group nextRowInternal() throws IOException, TranslatorException {
        while (rowIterator != null) {
            if (pageRowCount-- <= 0) {
                this.rowIterator = null;
                PageReadStore nextRowGroup = this.reader.readNextRowGroup();
                if(nextRowGroup == null){
                    VirtualFile nextParquetFile = getNextParquetFile();
                    if (nextParquetFile == null) {
                        return null;
                    }
                    this.rowIterator = readParquetFile(nextParquetFile);
                    continue;
                } else{
                    this.pageRowCount = nextRowGroup.getRowCount();
                    this.rowIterator = columnIO.getRecordReader(nextRowGroup, new GroupRecordConverter(filteredSchema));
                }
            }
            return rowIterator.read();
        }

        return null;
    }

    protected VirtualFile getNextParquetFile() {
        while (this.parquetFiles.length > this.fileCount.get()) {
            VirtualFile f = this.parquetFiles[this.fileCount.getAndIncrement()];
            if ("parquet".equals(getExtension(f.getName()))) {
                return f;
            }
        }
        return null;
    }

    private String[] getPartitionedColumns(String partitionedColumns) {
        String[] partitionedColumnList;
        partitionedColumnList = partitionedColumns.split(",");
        return partitionedColumnList;
    }

    @Override
    public void close() {

    }

    @Override
    public void cancel() throws TranslatorException {

    }
}
