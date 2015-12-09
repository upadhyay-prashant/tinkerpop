/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.tinkerpop.gremlin.spark.process.computer.groovy.plugin;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.tinkerpop.gremlin.groovy.util.TestableConsolePluginAcceptor;
import org.apache.tinkerpop.gremlin.hadoop.Constants;
import org.apache.tinkerpop.gremlin.hadoop.structure.HadoopGraph;
import org.apache.tinkerpop.gremlin.hadoop.structure.io.gryo.GryoInputFormat;
import org.apache.tinkerpop.gremlin.process.computer.ranking.pagerank.PageRankVertexProgram;
import org.apache.tinkerpop.gremlin.spark.AbstractSparkTest;
import org.apache.tinkerpop.gremlin.spark.groovy.plugin.SparkGremlinPlugin;
import org.apache.tinkerpop.gremlin.spark.process.computer.SparkHadoopGraphProvider;
import org.apache.tinkerpop.gremlin.spark.structure.Spark;
import org.apache.tinkerpop.gremlin.spark.structure.io.PersistedOutputRDD;
import org.apache.tinkerpop.gremlin.spark.structure.io.gryo.GryoSerializer;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.util.GraphFactory;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class SparkGremlinPluginTest extends AbstractSparkTest {

    @Before
    public void setupTest() {
        try {
            this.console = new TestableConsolePluginAcceptor();
            final SparkGremlinPlugin plugin = new SparkGremlinPlugin();
            plugin.pluginTo(this.console);
            this.console.eval("import " + PageRankVertexProgram.class.getPackage().getName() + ".*");
        } catch (final Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    ///////////////////

    private TestableConsolePluginAcceptor console;

    @Test
    public void shouldSupportBasicRDDOperations() throws Exception {
        String rddLocation = "target/test-output/graph-1";
        final Configuration configuration = new BaseConfiguration();
        configuration.setProperty("spark.master", "local[4]");
        configuration.setProperty("spark.serializer", GryoSerializer.class.getCanonicalName());
        configuration.setProperty(Graph.GRAPH, HadoopGraph.class.getName());
        configuration.setProperty(Constants.GREMLIN_HADOOP_INPUT_LOCATION, SparkHadoopGraphProvider.PATHS.get("tinkerpop-modern.kryo"));
        configuration.setProperty(Constants.GREMLIN_HADOOP_GRAPH_INPUT_FORMAT, GryoInputFormat.class.getCanonicalName());
        configuration.setProperty(Constants.GREMLIN_SPARK_GRAPH_OUTPUT_RDD, PersistedOutputRDD.class.getCanonicalName());
        configuration.setProperty(Constants.GREMLIN_HADOOP_JARS_IN_DISTRIBUTED_CACHE, false);
        configuration.setProperty(Constants.GREMLIN_HADOOP_OUTPUT_LOCATION, rddLocation);
        configuration.setProperty(Constants.GREMLIN_SPARK_PERSIST_CONTEXT, true);
        Graph graph = GraphFactory.open(configuration);

        Spark.create("local[4]");

        assertEquals(0, ((List<String>) this.console.eval("spark.ls()")).size());

        this.console.addBinding("graph", graph);
        this.console.eval("graph.compute(SparkGraphComputer).program(PageRankVertexProgram.build().iterations(1).create()).submit().get()");
        assertEquals(1, ((List<String>) this.console.eval("spark.ls()")).size());
        assertEquals(Constants.getGraphLocation(rddLocation) + " [Memory Deserialized 1x Replicated]", ((List<String>) this.console.eval("spark.ls()")).get(0));

        rddLocation = "target/test-output/graph-2";
        this.console.eval("graph.configuration().setProperty('" + Constants.GREMLIN_HADOOP_OUTPUT_LOCATION + "','" + rddLocation + "')");
        this.console.eval("graph.compute(SparkGraphComputer).program(PageRankVertexProgram.build().iterations(1).create()).submit().get()");
        assertEquals(2, ((List<String>) this.console.eval("spark.ls()")).size());
        assertTrue(((List<String>) this.console.eval("spark.ls()")).contains(Constants.getGraphLocation(rddLocation) + " [Memory Deserialized 1x Replicated]"));

        this.console.eval("spark.rm('target/test-output/graph-2/~g')");
        assertEquals(1, ((List<String>) this.console.eval("spark.ls()")).size());
        assertTrue(((List<String>) this.console.eval("spark.ls()")).contains("target/test-output/graph-1/~g [Memory Deserialized 1x Replicated]"));

        assertEquals(6, IteratorUtils.count(((Iterator<Object>) this.console.eval("spark.head('target/test-output/graph-1/~g')"))));

        this.console.eval("spark.rmr('target/test-output/graph-*')");
        assertEquals(0, ((List<String>) this.console.eval("spark.ls()")).size());

        //////
        rddLocation = "target/test-output/graph-1";
        this.console.eval("graph.configuration().setProperty('" + Constants.GREMLIN_HADOOP_OUTPUT_LOCATION + "','" + rddLocation + "')");
        this.console.eval("graph.compute(SparkGraphComputer).program(PageRankVertexProgram.build().iterations(1).create()).submit().get()");

        rddLocation = "target/test-output/graph-2";
        this.console.eval("graph.configuration().setProperty('" + Constants.GREMLIN_HADOOP_OUTPUT_LOCATION + "','" + rddLocation + "')");
        this.console.eval("graph.compute(SparkGraphComputer).program(PageRankVertexProgram.build().iterations(1).create()).submit().get()");

        rddLocation = "target/test-output/x";
        this.console.eval("graph.configuration().setProperty('" + Constants.GREMLIN_HADOOP_OUTPUT_LOCATION + "','" + rddLocation + "')");
        this.console.eval("graph.compute(SparkGraphComputer).program(PageRankVertexProgram.build().iterations(1).create()).submit().get()");

        assertEquals(3, ((List<String>) this.console.eval("spark.ls()")).size());
        this.console.eval("spark.rmr('target/test-output/graph-*')");
        assertEquals(1, ((List<String>) this.console.eval("spark.ls()")).size());
        this.console.eval("spark.rmr('*')");
        assertEquals(0, ((List<String>) this.console.eval("spark.ls()")).size());

        //
    }
}