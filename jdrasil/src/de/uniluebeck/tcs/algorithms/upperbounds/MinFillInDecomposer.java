package de.uniluebeck.tcs.algorithms.upperbounds;

/**
 * MinFillinDecomposer.java
 * @author bannach
 */

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import de.uniluebeck.tcs.App;
import de.uniluebeck.tcs.algorithms.EliminationOrderDecomposer;
import de.uniluebeck.tcs.graph.Graph;
import de.uniluebeck.tcs.graph.TreeDecomposer;
import de.uniluebeck.tcs.graph.TreeDecomposition;
import de.uniluebeck.tcs.graph.TreeDecomposition.TreeDecompositionQuality;

/**
 * This class implements the min fill-in heuristic to compute a tree-decomposition. The heuristic eliminates the vertex that adds
 * the least amount of edges when eliminated next. Ties are broken randomly.
 * @param <T>
 */
public class MinFillInDecomposer<T extends Comparable<T>> implements TreeDecomposer<T>, Serializable {

	private static final long serialVersionUID = 1L;

	/** The graph to be decomposed. */
	private final Graph<T> graph;
	
	/** Source of randomness. */
	private final Random dice;
	
	/** The permutation that is computed. */
	private List<T> permutation;
	
	/** Which algorithm to use? Choices: GreedyFillIn and SparsestSubgraph (c.f. Bodlaender, Upper Bounds) */
	public enum Algo{
		GreedyFillIn, 
		SparsestSubgraph;
	}
	
	private Algo toRun;
	/**
	 * The algorithm is initialized with a graph that should be decomposed.
	 * @param graph
	 */
	public MinFillInDecomposer(Graph<T> graph) {
		this.graph = graph;
		this.dice = App.getSourceOfRandomness();
		setToRun(Algo.GreedyFillIn);
	}
	
	/**
	 * The algorithm is initialized with a graph that should be decomposed and a seed for randomness.
	 * @param graph
	 * @param dice
	 */
	public MinFillInDecomposer(Graph<T> graph, Random dice) {
		this.graph = graph;
		this.dice = dice;
		setToRun(Algo.GreedyFillIn);
	}
	
	/**
	 * Computes the next vertex to be eliminated with respect to the min fill-in heuristic.
	 * The vertex that adds the least amount of vertices when eliminates is choose, ties are broken randomly.
	 * @param G
	 * @return
	 */
	private T nextVertex(Graph<T> G) {
		int min = Integer.MAX_VALUE;
		List<T> best = new LinkedList<>();
		
		// search for the best vertex
		for (T v : G.getVertices()) {
			int value = G.getFillInValue(v);
			if(toRun == Algo.SparsestSubgraph)
				value -= G.getNeighborhood(v).size();
			// update data
			if (value < min) {
				min = value;
				best.clear();;
				best.add(v);
			} else if (value == min) {
				best.add(v);				
			}
		}
		
		// done
		return best.get(dice.nextInt(best.size()));
	}
	
	/**
	 * Returns the elimination order computed by call().
	 * @return
	 */
	public List<T> getPermutation() {
		return permutation;
	}
	
	@Override
	public TreeDecomposition<T> call() throws Exception {
		return call(graph.getVertices().size()+1);
	}
	
	public TreeDecomposition<T> call(int upper_bound) throws Exception {
		
		// catch the empty graph
		if (graph.getVertices().size() == 0) return new TreeDecomposition<T>(graph);

		List<T> permutation = new LinkedList<T>();
		Graph<T> workingCopy = graph.copy();
		// compute the permutation
		for (int i = 0; i < graph.getVertices().size(); i++) {
			if (Thread.currentThread().isInterrupted()) throw new Exception();
			
			T v = nextVertex(workingCopy);
			if(workingCopy.getNeighborhood(v).size() >= upper_bound){
				// Okay, this creates a clique of size >= upper_bound + 1, I can abort!
				return null;
			}
			permutation.add(v);
			workingCopy.eliminateVertex(v);
		}
		
		this.permutation = permutation;
		return new EliminationOrderDecomposer<T>(graph, permutation, TreeDecompositionQuality.Heuristic).call();
	}

	@Override
	public TreeDecompositionQuality decompositionQuality() {
		return TreeDecompositionQuality.Heuristic;
	}

	@Override
	public TreeDecomposition<T> getCurrentSolution() {
		return null;
	}

	public Algo getToRun() {
		return toRun;
	}

	public void setToRun(Algo toRun) {
		this.toRun = toRun;
	}
	
}