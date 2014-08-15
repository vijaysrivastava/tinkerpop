package com.tinkerpop.gremlin.neo4j.process.graph.step.map;

import com.tinkerpop.gremlin.neo4j.structure.Neo4jGraph;
import com.tinkerpop.gremlin.process.Traversal;
import com.tinkerpop.gremlin.process.graph.step.map.FlatMapStep;
import com.tinkerpop.gremlin.structure.Element;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.ResourceIterator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class Neo4jCypherStep<S, E> extends FlatMapStep<S, Map<String, E>> {

    private final Neo4jGraph graph;
    private final String query;
    private final Map<String, Object> params;
    private final ExecutionEngine cypher;

    public Neo4jCypherStep(final String query, final Traversal traversal) {
        this(query, new HashMap<>(), traversal);
    }

    public Neo4jCypherStep(final String query, final Map<String, Object> params, final Traversal traversal) {
        super(traversal);
        this.query = query;
        this.params = params;
        this.graph = (Neo4jGraph) traversal.memory().get("g").get();
        this.cypher = (ExecutionEngine) traversal.memory().get("cypher").get();
        this.setFunction(traverser -> {
            final S s = traverser.get();
            final List<Object> ids = new ArrayList<>();
            extractIds(s, ids);
            params.put("traversalIds", ids);
            final ExecutionResult result = cypher.execute(query, params);
            final ResourceIterator<Map<String, Object>> itty = result.iterator();
            return itty.hasNext() ? new Neo4jCypherIterator(itty, graph) : new ArrayList().iterator();
        });
    }

    private static void extractIds(Object s, final List<Object> ids) {
        if (s instanceof Element) {
            ids.add(((Element) s).id());
        } else if (s instanceof Long) {
            ids.add(s);
        } else if (s instanceof Integer) {
            ids.add(((Integer) s).longValue());
        } else if (s instanceof Iterable) {
            extractIds(((Iterable) s).iterator(), ids);
        } else if (s instanceof Iterator) {
            while (((Iterator) s).hasNext()) {
                extractIds(((Iterator) s).next(), ids);
            }
        }
    }

}
