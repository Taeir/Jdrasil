package de.uniluebeck.tcs.sat.encodings;

/**
 * IncrementalCardinalityEncoder.java
 * @author bannach, berndt
 */

import java.util.ArrayList;
import java.util.Set;

import de.uniluebeck.tcs.sat.Formula;
import pseudo.PBLib;

/**
 * In incremental SAT-solving, a SAT-solver is invoked multiple times with similar formulas.
 * The advantage is, that the solver can keep learned information about the formula and, therefore, can
 * hopefully solve later formulas faster. This is especially interesting if we wish to solve an optimization problem:
 * We build a formula that bound the solution to, say, k and as long as the formula is satisfiable we add constraints that reduce k.
 * However, since cardinality constraints are expensive, it is a resource waste to add a new cardinality constraint each time.
 * Instead, we use cardinality constraints that can be modified only by adding new clauses.
 * 
 * This class provides incremental cardinality constraints using the C++ library PBLib of Peter Steinke 
 * @url http://tools.computational-logic.org/content/pblib.php
 */
public class IncrementalCardinalityEncoder {

	/** An interface to PBLib */
	private PBLib p;
	
	/**
	 * Default constructor, initialize a JNI bridge to PBlib.
	 */
	public IncrementalCardinalityEncoder() {
		p = new PBLib();
	}

    /**
	 * Initialize and add an incremental At-Most-k cardinality constraint for the given set of
	 * variables to the formula.
	 * @param phi
	 * @param variables
	 * @param k
	 */
	public void initAMK(Formula phi, Set<Integer> variables,  int k) {
        ArrayList<ArrayList<Integer>> clauses = p.initIterAtMostK(new ArrayList<>(variables), k, phi.getHighestVariable());
        for(ArrayList<Integer> c: clauses) {
            phi.addClause(c);
        }
    }

    /**
	 * Initialize and add an incremental At-Least-k cardinality constraint for the given set of
	 * variables to the formula.
	 * @param phi
	 * @param variables
	 * @param k
	 */
	public void initALK(Formula phi, Set<Integer> variables,  int k) {
        ArrayList<ArrayList<Integer>> clauses = p.initIterAtLeastK(new ArrayList<>(variables), k, phi.getHighestVariable());
        for(ArrayList<Integer> c: clauses) {
            phi.addClause(c);
        }    
    }

    /**
	 * Improve the upper-bound of @see pbIninitIncrementalAMK. Note that this does not take any variables.
	 * @param phi
	 * @param k
	 */
	public void incrementAMK(Formula phi,  int k) {
		ArrayList<ArrayList<Integer>> clauses = p.iterAtMostK(k, phi.getHighestVariable());
        for(ArrayList<Integer> c: clauses) {
        	phi.addClause(c);        	
        }
    }
	
    /**
	 * Improve the lower-bound of @see pbIninitIncrementalALK. Note that this does not take any variables.
	 * @param phi
	 * @param k
	 */
	public void incrementALK(Formula phi,  int k) {
		ArrayList<ArrayList<Integer>> clauses = p.iterAtLeastK(k, phi.getHighestVariable());
        for(ArrayList<Integer> c: clauses) {
        	phi.addClause(c);
        }
    }

}
