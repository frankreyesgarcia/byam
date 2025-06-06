package com.redislabs.redisgraph;

import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.params.ScriptParams;
import redis.clients.jedis.util.SafeEncoder;

import java.io.Closeable;
import java.util.List;
import java.util.Map;

/**
 * An interface which aligned to Jedis Pipeline interface
 */
public interface RedisGraphPipeline extends Pipeline, Closeable {

    /**
     * Execute a Cypher query.
     * @param graphId a graph to perform the query on
     * @param query Cypher query
     * @return a response which builds the result set with the query answer.
     */
    Response<ResultSet> query(String graphId, String query);

    /**
     * Execute a Cypher read-only query.
     * @param graphId a graph to perform the query on
     * @param query Cypher query
     * @return a response which builds the result set with the query answer.
     */
    Response<ResultSet> readOnlyQuery(String graphId, String query);

    /**
     * Executes a cypher query with parameters.
     * @param graphId a graph to perform the query on.
     * @param query Cypher query.
     * @param params parameters map.
     * @return  a response which builds the result set with the query answer.
     */
    Response<ResultSet> query(String graphId, String query, Map<String, Object> params);

    /**
     * Executes a cypher read-only query with parameters.
     * @param graphId a graph to perform the query on.
     * @param query Cypher query.
     * @param params parameters map.
     * @return  a response which builds the result set with the query answer.
     */
    Response<ResultSet> readOnlyQuery(String graphId, String query, Map<String, Object> params);

    /**
     * Executes a cypher query with parameters and timeout.
     * @param graphId a graph to perform the query on.
     * @param query Cypher query.
     * @param params parameters map.
     * @param timeout
     * @return  a response which builds the result set with the query answer.
     */
    Response<ResultSet> query(String graphId, String query, Map<String, Object> params, long timeout);

    /**
     * Executes a cypher read-only query with parameters and timeout.
     * @param graphId a graph to perform the query on.
     * @param query Cypher query.
     * @param params parameters map.
     * @param timeout
     * @return  a response which builds the result set with the query answer.
     */
    Response<ResultSet> readOnlyQuery(String graphId, String query, Map<String, Object> params, long timeout);

    /**
     * Invokes stored procedures without arguments
     * @param graphId a graph to perform the query on
     * @param procedure procedure name to invoke
     * @return a response which builds result set with the procedure data
     */
    Response<ResultSet> callProcedure(String graphId, String procedure);

    /**
     * Invokes stored procedure with arguments
     * @param graphId a graph to perform the query on
     * @param procedure procedure name to invoke
     * @param args procedure arguments
     * @return a response which builds result set with the procedure data
     */
    Response<ResultSet> callProcedure(String graphId, String procedure, List<String> args);

    /**
     * Invoke a stored procedure
     * @param graphId a graph to perform the query on
     * @param procedure - procedure to execute
     * @param args - procedure arguments
     * @param kwargs - procedure output arguments
     * @return a response which builds result set with the procedure data
     */
    Response<ResultSet> callProcedure(String graphId, String procedure, List<String> args  , Map<String, List<String>> kwargs);

    /**
     * Deletes the entire graph
     * @param graphId graph to delete
     * @return a response which builds the delete running time statistics
     */
    Response<String> deleteGraph(String graphId);

    /**
     * Synchronize pipeline by reading all responses. This operation close the pipeline. Whenever
     * possible try to avoid using this version and use Pipeline.sync() as it won't go through all the
     * responses and generate the right response type (usually it is a waste of time).
     * @return A list of all the responses in the order you executed them.
     */
    List<Object> syncAndReturnAll();

    /**
     * Synchronize pipeline by reading all responses. This operation close the pipeline. In order to
     * get return values from pipelined commands, capture the different Response&lt;?&gt; of the
     * commands you execute.
     */
    void sync();

    /**
     * Blocks until all the previous write commands are successfully transferred and acknowledged by
     * at least the specified number of replicas. If the timeout, specified in milliseconds, is
     * reached, the command returns even if the specified number of replicas were not yet reached.
     * @param replicas successfully transferred and acknowledged by at least the specified number of
     *          replicas
     * @param timeout the time to block in milliseconds, a timeout of 0 means to block forever
     * @return the number of replicas reached by all the writes performed in the context of the
     *         current connection
     */
    Response<Long> waitReplicas(int replicas, long timeout);
}