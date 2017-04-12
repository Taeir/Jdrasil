/*
 * Copyright (c) 2016-present, Max Bannach, Sebastian Berndt, Thorsten Ehlers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT
 * OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package jdrasil.algorithms.exact;

import jdrasil.graph.*;
import jdrasil.utilities.BitSetTrie;
import jdrasil.utilities.logging.JdrasilLogger;

import java.util.*;
import java.util.logging.Logger;

/**
 * Tree width has nice game theoretic characterisations, one of which is the node-search game played by a set of searchers and
 * a fugitive. The variant of the game that corresponds to tree width is the one with a visible fugitive of unbounded speed
 * (also known as the helicopter cops and robber game, defined by Robertson and Seymour).
 *
 * This class computes a winning strategy for the searchers (from which a tree decomposition can be deduced) in a bottom-up fashion.
 * In order to do so, a queue of win-configurations is maintained which is pre-filled with trivial win-configurations.
 * Then predecessor configurations (with respect to a winning strategy of the searchers) are computed for the configurations
 * in the queue, and if these configurations are win-configurations as well, they are added back to queue.
 * As larger win-configurations are "better" in the sense that we wish to find a win-configuration for the whole graph,
 * a priority queue is used that orders configurations by size.
 *
 * Computing predecessor configurations is straighted forward if the searchers move does not disconnect the graph
 * (i.e. a simple existential "fly"-move), however, it is difficult if a universal "split"-move is reconstructed
 * (i.e., if the current configuration of the game originated from a searchers move that has disconnected the graph and
 * from a corresponding fugitive move whom has chosen a connected component). In this scenario multiple win-configurations
 * (which we may or may not already have discovered) have to be glued together. These configurations correspond to branch
 * nodes in the tree decomposition.
 *
 * The "glue"-part of the algorithm is inspired by the work of Hisao Tamaki for the PACE 2016. He used this strategy
 * to implement a bottom-up version of the algorithm of Arnborg et al., which is based on similar ideas as the node-search
 * game.
 *
 * @author Max Bannach
 * @author Sebastian Berndt
 */
public class CleanAndGlue<T extends Comparable<T>> implements TreeDecomposer<T> {

    /** Jdrasils Logger */
    private final static Logger LOG = Logger.getLogger(JdrasilLogger.getName());

    /** The graph we wish to decompose in BitSet representation. */
    private BitSetGraph graph;

    /** The number of vertices of the graph. */
    private int n;

    /**
     * Queue of subgraphs \(S\) that can be cleaned if the searcher stand on \(N(S)\), i.e., winning configurations
     * for the searchers.
     *
     * As the searcher have a winning strategy if \(V\) is a winning configuration, we will use a priority queue to
     * handle big subgraphs first (in the hope of reaching \(V\) faster).
     */
    private PriorityQueue<BitSet> queue;

    /** Number of configurations processed during the run (i.e. configurations that where added to the queue) */
    private int configurations;

    /** Memorization of win-configurations that we have already considered. */
    private BitSetTrie memory;

    /** For each vertex \(v\) we store a collection of subgraphs that have \(v\) as neighbor. */
    private Map<Integer, BitSetTrie> tries;

    /** Each element added to the queue is glued from one or more previous winning configurations. */
    private Map<BitSet, BitSet[]> from;

    /**
     * Initialize data structures and transform the graph into a BitSetGraph.
     * @param graph
     */
    public CleanAndGlue(Graph<T> graph) {
        this.graph  = new BitSetGraph(graph);
        this.n      = this.graph.getN();
        this.queue  = new PriorityQueue<>( (a,b) -> Integer.compare(b.cardinality(), a.cardinality()) );
        this.memory = new BitSetTrie();
        this.from   = new HashMap<>();
        this.tries  = new HashMap<>();
    }

    /**
     * Checks if the given configuration S is a win-configuration under the assumption that the configurations in
     * from are win-configurations. S is assumed to be a predecessor of from the node-search game and, thus, we have
     * only to check if there is a valid move from S to from.
     *
     * If S is a win-configuration, this method will add it to the priority queue for further processing. However, this
     * will only be done if necessary. The method applies multiply pruning rules to avoid this circumstance.
     *
     * If S is a win-configuration for the whole graph, i.e., the start of a winning strategy of the searchers, this method
     * returns true. In this case no configurations has to be further processed, a tree decomposition of width k has been
     * found. In all other cases (i.e., independent of the fact that S is a win-configuration or not, and the fact that S
     * is added to the queue or not) this method will return false.
     *
     * @param S the configuration that should be offered
     * @param k tree width that is tested
     * @param from array of win-configurations from which S was glued (may be at least one)
     * @return
     */
    private boolean offer(BitSet S, int k, BitSet... from) {

        // Prune 1: we have to handle each configuration just ones
        if (memory.contains(S)) return false;

        // Prune 2: if the configuration requires to many searchers, we can prune it as well
        // The configuration needs searchers on its border (otherwise the fugitive could escape), as well as the searchers
        // that are used to produce the next configuration by "fly" on the new vertices (as the fugitive can move in between).
        BitSet neighbors = graph.exteriorBorder(S);
        BitSet delta = (BitSet) S.clone();
        for (BitSet f : from) delta.andNot(f);
        if (neighbors.cardinality() + delta.cardinality() > k + 1) return false; // not enough searchers

        // Prune 3: if we have handled a superset of S and N(S), we can prune S
        BitSet mask = (BitSet) S.clone();
        mask.or(neighbors);
        if (memory.getSuperSets(mask).iterator().hasNext()) { memory.insert(S); return false; }

        // Prune 4: if we have handled a superset S' of S such that N(S') is a subset of N(S) we can prune
        for (BitSet Sprime : memory.getSuperSets(S)) {
            BitSet neighborsPrime = graph.exteriorBorder(Sprime);
            boolean cut = true;
            for (int v = neighborsPrime.nextSetBit(0); v >= 0; v = neighborsPrime.nextSetBit(v+1)) {
                if (!neighbors.get(v)) { cut = false; break; }
            }
            if (cut) { memory.insert(S); return false; }
        }

        // we will add S to the queue, store how we have glued it
        this.from.put(S, from);

        // Prune 5: check if we have already a win-configuration that is actually a win-strategy for the whole graph
        // In this case we have found a decomposition of width k.
        if (S.cardinality() >= n - k - 1) {
            // create extra bag for remaining vertices
            if (S.cardinality() < n) {
                BitSet all = new BitSet();
                all.set(0, n);
                this.from.put(all, new BitSet[]{S});
            }
            return true;
        }

        // Store the new win-configuration in the priority queue.
        queue.offer(S);
        memory.insert(S);

        // done, but have to decompose further
        return false;
    }

    /**
     * Tries do decompose the given graph into a tree decomposition of width \(k\).
     * This method will compute a winning strategy of \(k+1\) searchers in the classic node-search game for tree width (i.e.,
     * helicopter cops and robber) in a bottom-up fashion: A queue with game configurations that are win-configurations
     * for the searchers is pre-filled with trivial win-configurations; from this queue configurations are extracted and
     * predecessor configurations of the game are computed, which are in turn added to the queue if they are also win-configurations.
     *
     * This is straighted forward if the searchers move does not disconnect the graph (i.e. a simple existential "fly"-move),
     * however, it is difficult if a universal "split"-move is reconstructed (i.e., if the current configuration of the game
     * originated from a searchers move that has disconnected the graph and from a corresponding fugitive move whom has
     * chosen a connected component). In this scenario multiple win-configurations (which we may or may not already have
     * discovered) have to be glued together. These configurations correspond to branch nodes in the tree decomposition.
     *
     * Win-configurations that should be further processed are stored in a priority queue that delivers the biggest subgraph first.
     * This increases the change that we quickly find a cleaning strategy for the whole graph.
     *
     * @param k
     * @return
     */
    private boolean decompose(int k) {
        // init data structures
        queue.clear();
        memory.clear();
        from.clear();
        tries.clear();
        for (int v = 0; v < n; v++) tries.put(v, new BitSetTrie());
        configurations = 0;

        // pre-fill the queue with trivial win-configurations
        for (int v = 0; v < n; v++) {
            BitSet S = new BitSet();
            S.set(v);
            graph.saturate(S);
            if (offer(S, k)) return true;
        }

        // handle the queue
        while (!queue.isEmpty()) {
            configurations++;
            // get currently largest win-configuration
            BitSet S = queue.poll();
            BitSet delta = graph.exteriorBorder(S);

            // handle the neighbors of S
            for (int v = delta.nextSetBit(0); v >= 0; v = delta.nextSetBit(v+1)) {

                // 1. add S to the trie of v
                tries.get(v).insert(S);

                // 2. try to extend S by removing a searcher from v, i.e., find a direct predecessor configuration
                BitSet newS = (BitSet) S.clone();
                newS.set(v);
                graph.saturate(newS);
                if (offer(newS, k, S)) return true;

                // 3. try to glue S to other win-configurations
                Stack<BitSet> stack = new Stack<>();
                stack.push(S);
                while (!stack.isEmpty()) {
                    BitSet current = (BitSet) stack.pop().clone();
                    BitSet currentNeighbors = graph.exteriorBorder(current);
                    BitSet mask = (BitSet) current.clone();
                    mask.or(currentNeighbors);
                    mask.flip(0,n);
                    for (BitSet toGlue : tries.get(v).getSubSets(mask)) {
                        BitSet glueNeighbors = graph.exteriorBorder(toGlue);
                        glueNeighbors.or(currentNeighbors);
                        if (glueNeighbors.cardinality() > k+1) continue; // not enough cops
                        newS = (BitSet) current.clone();
                        newS.or(toGlue);

                        int absorbable = graph.absorbable(newS);
                        if (absorbable < 0 || absorbable == v) {
                            BitSet tmp = (BitSet) newS.clone();
                            tmp.set(v); // may prevent us from offering
                            graph.saturate(tmp);
                            if (offer(tmp, k, current, toGlue)) return true;
                        }
                        if (absorbable < 0) {
                            from.put(newS, new BitSet[]{current, toGlue});
                            stack.push(newS);
                        }

                    }
                }
            }

        }

        // queue empty -> failed to clean -> tree width is larger then k
        return false;
    }

    /**
     * Extract a tree decomposition from a cleaning strategy of the searchers.
     * Should be called after a run of @see decompose that has returned true
     * @param S
     * @param td
     * @return
     */
    private Bag<T> extractTreeDecomposition(BitSet S, TreeDecomposition<T> td) {
        // 1. create current bag
        BitSet bagVertices = (BitSet) S.clone();
        for (BitSet f : from.get(S)) bagVertices.andNot(f); // reduce to delta
        bagVertices.or(graph.exteriorBorder(S));            // add neighbors
        Bag<T> bag = td.createBag(graph.getVertexSet(bagVertices));

        // 2. compute children and glue to them
        for (BitSet child : from.get(S)) {
            Bag<T> childBag = extractTreeDecomposition(child, td);
            td.addTreeEdge(bag, childBag);
        }

        // 3. return bag
        return bag;
    }

    @Override
    public TreeDecomposition<T> call() throws Exception {

        int k = 1;
        while (!decompose(k)) {
            LOG.info(String.format("tree width >= %2d ( %4d configurations )", k, configurations));
            k++;
        }
        LOG.info(String.format("tree width == %2d ( %4d configurations )", k, configurations));

        // extract constructed tree decomposition
        TreeDecomposition<T> td = new TreeDecomposition<T>(graph.getGraph());
        BitSet all = new BitSet();
        all.set(0,n);
        extractTreeDecomposition(all, td);

        // done
        return td;
    }

    @Override
    public TreeDecomposition<T> getCurrentSolution() { return null; }

    @Override
    public TreeDecomposition.TreeDecompositionQuality decompositionQuality() {
        return TreeDecomposition.TreeDecompositionQuality.Exact;
    }
}
