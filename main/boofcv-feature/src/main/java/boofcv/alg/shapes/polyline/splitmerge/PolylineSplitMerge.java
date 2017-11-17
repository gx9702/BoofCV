/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.alg.shapes.polyline.splitmerge;

import boofcv.misc.CircularIndex;
import georegression.geometry.UtilPolygons2D_I32;
import georegression.metric.Distance2D_F64;
import georegression.struct.line.LineParametric2D_F64;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;
import org.ddogleg.struct.LinkedList;
import org.ddogleg.struct.LinkedList.Element;

import java.util.List;

/**
 * Fits a polyline to a contour by fitting the simplest model and adding more sides to it and then
 * seeing if the lines can be merged together. First it always finds
 * an approximation of a triangle which minimizes the error. A side is added to the current polyline by finding
 * the side that when split will reduce the error by the largest amount. This is repeated until no sides can be
 * split or the maximum number of sides has been reached.
 *
 * TODO redo description
 *
 * TODO Sampling of a side
 *
 * TODO how sides are scored
 *
 *
 * The polyline will always be in counter-clockwise ordering.
 *
 * @author Peter Abeles
 */
public class PolylineSplitMerge {
	// todo Change how score is computed to minimize overflow
	// TODO non loop version
	// TODO Figure out why it sometimes picks a not so great corner
	// TODO doesn't select a good corner when the other wise is parallel to it. happens when going from 2 to 3 corners.
	// TODO need to add a way to abort early if it's obvious the shape is too complex

	// Can it assume the shape is convex? If so it can reject shapes earlier
	private boolean convex = false;

	// maximum number of sides it will consider
	private int maxSides = 20;
	// minimum number of sides that will be considered for the best polyline
	private int minSides = 3;

	// The minimum length of a side
	private int minimumSideLength = 10;

	// how many corners past the max it will fit a polygon to
	private int extraConsider = 4;

	// When selecting the best model how much is a split penalized
	private double cornerScorePenalty = 0.25;

	// If the score of a side is less than this it is considered a perfect fit and won't be split any more
	private double thresholdSideSplitScore = 1;

	// maximum number of points along a side it will sample when computing a score
	// used to limit computational cost of large contours
	int maxNumberOfSideSamples = 50;

	// work space for side score calculation
	private LineParametric2D_F64 line = new LineParametric2D_F64();

	// the corner list that's being built
	LinkedList<Corner> list = new LinkedList<>();
	FastQueue<Corner> corners = new FastQueue<>(Corner.class,true);

	private SplitSelector splitter = new MaximumLineDistance();
	private SplitResults resultsA = new SplitResults();
	private SplitResults resultsB = new SplitResults();

	// List of all the found polylines and their score
	private FastQueue<CandidatePolyline> polylines = new FastQueue<>(CandidatePolyline.class,true);
	private CandidatePolyline bestPolyline;

	public boolean process(List<Point2D_I32> contour ) {
		list.reset();
		corners.reset();
		polylines.reset();
		bestPolyline = null;

		if( contour.size() < 3 )
			return false;

		if( !findInitialTriangle(contour) )
			return false;
		savePolyline(contour.size());

		// by finding more corners than necessary it can recover from mistakes previously
		int limit = maxSides+extraConsider;
		while( list.size() < limit ) {
			if( !increaseNumberOfSidesByOne(contour) ) {
				break;
			}
		}
		// remove corners and recompute scores. If the result is better it will be saved
		while( true ) {
			if( !selectAndRemoveCorner(contour) ) {
				break;
			}
		}

		bestPolyline = null;
		double bestScore = Double.MAX_VALUE;
		for (int i = minSides-3; i < Math.min(maxSides-2,polylines.size); i++) {
			if( polylines.get(i).score < bestScore ) {
				bestPolyline = polylines.get(i);
				bestScore = bestPolyline.score;
			}
		}

		return true;
	}

	private void printCurrent( List<Point2D_I32> contour ) {
		System.out.print(list.size()+"  Indexes[");
		Element<Corner> e = list.getHead();
		while( e != null ) {
			System.out.print(" "+e.object.index);
			e = e.next;
		}
		System.out.println(" ]");
		System.out.print("   Errors[");
		e = list.getHead();
		while( e != null ) {
			String split = e.object.splitable ? "T" : "F";
			System.out.print(String.format(" %6.1f %1s",e.object.sideError,split));
			e = e.next;
		}
		System.out.println(" ]");
		System.out.print("      Pos[");
		e = list.getHead();
		while( e != null ) {
			Point2D_I32 p = contour.get(e.object.index);
			System.out.print(String.format(" %3d %3d,",p.x,p.y));
			e = e.next;
		}
		System.out.println(" ]");
	}

	/**
	 * Saves the current polyline
	 */
	boolean savePolyline( int contourSize ) {
		// if a polyline of this size has already been saved then over write it
		CandidatePolyline c;
		if( list.size() <= polylines.size+2 ) {
			c = polylines.get( list.size()-3 );
		} else {
			c = polylines.grow();
			c.score = Double.MAX_VALUE;
		}

		double foundScore = computeScore(list,computeCornerPenalty(contourSize,cornerScorePenalty));

		// only save the results if it's an improvement
		if( c.score > foundScore ) {
			c.score = foundScore;
			c.splits.reset();
			Element<Corner> e = list.getHead();
			double maxSideError = 0;
			while (e != null) {
				maxSideError = Math.max(maxSideError,e.object.sideError);
				c.splits.add(e.object.index);
				e = e.next;
			}
			c.maxSideError = maxSideError;
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Computes the score for a list
	 */
	static double computeScore( LinkedList<Corner> list , double cornerPenalty ) {
		double sumSides = 0;
		Element<Corner> e = list.getHead();
		while( e != null ) {
			sumSides += e.object.sideError;
			e = e.next;
		}

		return sumSides + cornerPenalty*list.size();
	}

	static double computeCornerPenalty( int contourSize , double parameter ) {
		return contourSize*parameter;
	}

	boolean findInitialTriangle(List<Point2D_I32> contour) {
		// find the first estimate for a corner
		int cornerSeed = findCornerSeed(contour);

		// see if it can reject the contour immediately
		if( convex ) {
			if( !sanityCheckConvex(contour,0,cornerSeed))
				return false;
		}

		// Select the second corner.
		splitter.selectSplitPoint(contour,0,cornerSeed,resultsA);
		splitter.selectSplitPoint(contour,cornerSeed,0,resultsB);

		if( splitter.compareScore(resultsA.score,resultsB.score) >= 0 ) {
			addCorner(resultsA.index);
			addCorner(cornerSeed);
		} else {
			addCorner(cornerSeed);
			addCorner(resultsB.index);
		}

		// Select the third corner. Initial triangle will be complete now
		int index0 = list.getHead().object.index;
		int index1 = list.getHead().next.object.index;

		splitter.selectSplitPoint(contour,index1,index0,resultsA);
		addCorner(resultsA.index);

		// enforce CCW requirement
		ensureTriangleIsCCW(contour);

		// TODO recompute the seed corner? maximum distance from some arbitrary point is kinda arbitrary

		// Score each side
		Element<Corner> e = list.getHead();
		while( e != null ) {
			Element<Corner> n = e.next;
			double error;
			if( n == null ) {
				error = computeSideError(contour,e.object.index, list.getHead().object.index);
			} else {
				error = computeSideError(contour,e.object.index, n.object.index);
			}
			e.object.sideError = error;
			e = n;
		}

		// Compute what would happen if a side was split
		e = list.getHead();
		while( e != null ) {
			computePotentialSplitScore(contour,e);
			e = e.next;
		}

		return true;
	}

	/**
	 * Ensure that thr triangle is in CCW ordering
	 */
	void ensureTriangleIsCCW(List<Point2D_I32> contour ) {
		Element<Corner> e = list.getHead();
		Corner a = e.object;e=e.next;
		Corner b = e.object;e=e.next;
		Corner c = e.object;

		Point2D_I32 pa = contour.get(a.index);
		Point2D_I32 pb = contour.get(b.index);
		Point2D_I32 pc = contour.get(c.index);

		// see if it is in clock-wise ordering
		if( UtilPolygons2D_I32.isPositiveZ(pa,pb,pc) ) {
			list.reset();
			list.pushHead(c);
			list.pushHead(b);
			list.pushHead(a);
		}
	}

	Element<Corner> addCorner( int where ) {
		Corner c = corners.grow();
		c.reset();
		c.index = where;
		list.pushTail(c);
		return list.getTail();
	}

	/**
	 * Increase the number of sides in the polyline. This is done greedily selecting the side which would improve the
	 * score by the most of it was split.
	 * @param contour Contour
	 * @return true if a split was selected and false if not
	 */
	boolean increaseNumberOfSidesByOne(List<Point2D_I32> contour) {
//		System.out.println("increase number of sides by one. list = "+list.size());
		Element<Corner> selected = selectCornerToSplit();

		// No side can be split
		if( selected == null )
			return false;

		// Update the corner who's side was just split
		selected.object.sideError = selected.object.splitError0;
		// split the selected side and add a new corner
		Corner c = corners.grow();
		c.reset();
		c.index = selected.object.splitLocation;
		c.sideError = selected.object.splitError1;
		Element<Corner> cornerE = list.insertAfter(selected,c);

		// compute the score for sides which just changed
		computePotentialSplitScore(contour,cornerE);
		computePotentialSplitScore(contour,selected);

		// Save the results
//		printCurrent(contour);
		savePolyline(contour.size());

		return true;
	}

	/**
	 * Selects the best side to split the polyline at.
	 * @return the selected side or null if the score will not be improved if any of the sides are split
	 */
	Element<Corner> selectCornerToSplit() {
		Element<Corner> selected = null;
		double bestChange = convex ? 0 : -Double.MAX_VALUE;

		// Pick the side that if split would improve the overall score the most
		Element<Corner> e = list.getHead();
		while( e != null ) {
			Corner c = e.object;
			if( !c.splitable) {
				e = e.next;
				continue;
			}

			// compute how much better the score will improve because of the split
			double change = c.sideError - c.splitError0 - c.splitError1;
			// it was found that selecting for the biggest change tends to produce better results
			if( change < 0 ) {
				change = -change;
			}
			if( change > bestChange ) {
				bestChange = change;
				selected = e;
			}
			e = e.next;
		}

		return selected;
	}

	/**
	 * Selects a corner that can be removed and results in the best score after it has been removed.
	 * @param contour
	 * @return true if a corner was removed
	 */
	boolean selectAndRemoveCorner(List<Point2D_I32> contour ) {
		if( list.size() <= 3 )
			return false;

		Element<Corner> target = list.getHead();
		Element<Corner> best = null;
		double bestScore = -Double.MAX_VALUE;
		double newEdgeScore = -1;

		double cornerPenalty = computeCornerPenalty(contour.size(), cornerScorePenalty);

		while( target != null ) {
			Element<Corner> p = previous(target);
			Element<Corner> n = next(target);

			// just contributions of the corners in question
			double before = p.object.sideError + target.object.sideError + cornerPenalty;
			double after = computeSideError(contour, p.object.index, n.object.index);

			if( before-after > bestScore ) {
				bestScore = before-after;
				newEdgeScore = after;
				best = target;
			}
			target = target.next;
		}

		// See if the new shape has a better score. if so save the results
		if( best != null ) {
//			System.out.println("removing a corner idx="+target.object.index);
			// Note: the corner is "lost" until the next contour is fit. Not worth the effort to recycle
			Element<Corner> p = previous(best);
			p.object.sideError = newEdgeScore;
			list.remove(best);
			computePotentialSplitScore(contour,p);
			savePolyline(contour.size());
			return true;
		}
		return false;
	}

	/**
	 * The seed corner is the point farther away from the first point. In a perfect polygon with no noise this should
	 * be a corner.
	 */
	static int findCornerSeed(List<Point2D_I32> contour ) {
		Point2D_I32 a = contour.get(0);

		int best = -1;
		double bestDistance = -Double.MAX_VALUE;

		for (int i = 1; i < contour.size(); i++) {
			Point2D_I32 b = contour.get(i);

			double d = distanceSq(a,b);
			if( d > bestDistance ) {
				bestDistance = d;
				best = i;
			}
		}

		return best;
	}

	/**
	 * Scores a side based on the sum of Euclidean distance squared of each point along the line. Euclidean squared
	 * is used because its fast to compute
	 *
	 * @param indexA first index. Inclusive
	 * @param indexB last index. Exclusive
	 */
	double computeSideError(List<Point2D_I32> contour , int indexA , int indexB ) {
		assignLine(contour, indexA, indexB, line);

		// don't sample the end points because the error will be zero by definition
		int numSamples;
		double sumOfDistances = 0;
		if( indexB >= indexA ) {
			int length = indexB-indexA-1;
			numSamples = Math.min(length,maxNumberOfSideSamples);
			for (int i = 0; i < numSamples; i++) {
				int index = indexA+1+length*i/numSamples;
				Point2D_I32 p = contour.get(index);
				sumOfDistances += Distance2D_F64.distanceSq(line,p.x,p.y);
			}
			// scale the error to the actual length in pixels
			sumOfDistances *= length/(double)numSamples;
		} else {
			int length = contour.size()-indexA-1 + indexB;
			numSamples = Math.min(length,maxNumberOfSideSamples);
			for (int i = 0; i < numSamples; i++) {
				int where = length*i/numSamples;
				int index = (indexA+1+where)%contour.size();
				Point2D_I32 p = contour.get(index);
				sumOfDistances += Distance2D_F64.distanceSq(line,p.x,p.y);
			}
			sumOfDistances *= length/(double)numSamples;
		}

		return sumOfDistances;
	}

	/**
	 * Computes the split location and the score of the two new sides if it's split there
	 */
	void computePotentialSplitScore( List<Point2D_I32> contour , Element<Corner> e0 )
	{
		Element<Corner> e1 = next(e0);

		e0.object.splitable = canBeSplit(contour.size(),e0);

		if( e0.object.splitable ) {
			setSplitVariables(contour, e0, e1);
		}
	}

	/**
	 * Selects and splits the side defined by the e0 corner. If convex a check is performed to
	 * ensure that the polyline will be convex still.
	 */
	void setSplitVariables(List<Point2D_I32> contour, Element<Corner> e0, Element<Corner> e1) {
		splitter.selectSplitPoint(contour, e0.object.index, e1.object.index, resultsA);

		// if convex only perform the split if it would result in a convex polygon
		if( convex ) {
			Point2D_I32 a = contour.get(e0.object.index);
			Point2D_I32 b = contour.get(resultsA.index);
			Point2D_I32 c = contour.get(next(e0).object.index);

			if (UtilPolygons2D_I32.isPositiveZ(a, b, c)) {
				e0.object.splitable = false;
				return;
			}
		}

		e0.object.splitLocation = resultsA.index;
		e0.object.splitError0 = computeSideError(contour, e0.object.index, resultsA.index);
		e0.object.splitError1 = computeSideError(contour, resultsA.index, e1.object.index);

		if( e0.object.splitLocation >= contour.size() )
			throw new RuntimeException("Egads");
	}

	/**
	 * Determines if the side can be split again. A side can always be split as long as
	 * its >= the minimum length or that the side score is larger the the split threshold
	 */
	boolean canBeSplit( int contourSize , Element<Corner> e0 ) {
		Element<Corner> e1 = next(e0);

		int length = CircularIndex.distanceP(e0.object.index,e1.object.index,contourSize);
		if( length < minimumSideLength ) {
			return false;
		}

		return e0.object.sideError > thresholdSideSplitScore*length;
	}

	/**
	 * Returns the next corner in the list
	 */
	Element<Corner> next( Element<Corner> e ) {
		if( e.next == null ) {
			return list.getHead();
		} else {
			return e.next;
		}
	}

	/**
	 * Returns the previous corner in the list
	 */
	Element<Corner> previous( Element<Corner> e ) {
		if( e.previous == null ) {
			return list.getTail();
		} else {
			return e.previous;
		}
	}

	/**
	 * For convex shapes no point along the contour can be farther away from A is from B. Thus the maximum number
	 * of points can't exceed a 1/2 circle.
	 *
	 * NOTE: indexA is probably the top left point in the contour, since that's how most contour algorithm scan
	 * but this isn't known for sure. If it was known you could make this requirement tighter.
	 *
	 * @param contour Contour points
	 * @param indexA index of first point
	 * @param indexB index of second point
	 * @return if it passes the sanity check
	 */
	static boolean sanityCheckConvex( List<Point2D_I32> contour , int indexA , int indexB )
	{
		double d = Math.sqrt(distanceSq(contour.get(indexA),contour.get(indexB)));

		// conservative upper bounds would be 1/2 a circle.
		int maxAllowed = (int)(Math.PI*d+0.5);

		if( indexA > indexB ) {
			int tmp = indexA;
			indexA = indexB;
			indexB = tmp;
		}

		int length0 = CircularIndex.subtract(indexA,indexB,contour.size());
		int length1 = CircularIndex.subtract(indexB,indexA,contour.size());

		if( length0 > maxAllowed || length1 > maxAllowed )
			return false;

		return true;
	}

	/**
	 * Using double prevision here instead of int due to fear of overflow in very large images
	 */
	static double distanceSq( Point2D_I32 a , Point2D_I32 b ) {
		double dx = b.x-a.x;
		double dy = b.y-a.y;

		return dx*dx + dy*dy;
	}

	/**
	 * Assigns the line so that it passes through points A and B.
	 */
	public static void assignLine(List<Point2D_I32> contour, int indexA, int indexB, LineParametric2D_F64 line) {
		Point2D_I32 endA = contour.get(indexA);
		Point2D_I32 endB = contour.get(indexB);

		line.p.x = endA.x;
		line.p.y = endA.y;
		line.slope.x = endB.x-endA.x;
		line.slope.y = endB.y-endA.y;
	}

	public FastQueue<CandidatePolyline> getPolylines() {
		return polylines;
	}

	public CandidatePolyline getBestPolyline() {
		return bestPolyline;
	}

	/**
	 * Storage for results from selecting where to split a line
	 */
	static class SplitResults
	{
		public int index;
		public double score;
	}

	/**
	 * Corner in the polyline. The side that this represents is this corner and the next in the list
	 */
	public static class Corner
	{
		public int index;
		public double sideError;
		// if this side was to be split this is where it would be split and what the scores
		// for the new sides would be
		public int splitLocation;
		public double splitError0, splitError1;

		// if a side can't be split (e.g. too small or already perfect)
		public boolean splitable;

		public void reset() {
			index = -1;
			sideError = -1;
			splitLocation = -1;
			splitError0 = splitError1 = -1;
			splitable = true;
		}
	}

	public static class CandidatePolyline
	{
		public GrowQueue_I32 splits = new GrowQueue_I32();
		public double score;
		public double maxSideError;
	}

	public boolean isConvex() {
		return convex;
	}

	public void setConvex(boolean convex) {
		this.convex = convex;
	}

	public int getMaxSides() {
		return maxSides;
	}

	public void setMaxSides(int maxSides) {
		this.maxSides = maxSides;
	}

	public int getMinimumSideLength() {
		return minimumSideLength;
	}

	public void setMinimumSideLength(int minimumSideLength) {
		this.minimumSideLength = minimumSideLength;
	}

	public double getCornerScorePenalty() {
		return cornerScorePenalty;
	}

	public void setCornerScorePenalty(double cornerScorePenalty) {
		this.cornerScorePenalty = cornerScorePenalty;
	}

	public double getThresholdSideSplitScore() {
		return thresholdSideSplitScore;
	}

	public void setThresholdSideSplitScore(double thresholdSideSplitScore) {
		this.thresholdSideSplitScore = thresholdSideSplitScore;
	}

	public int getMaxNumberOfSideSamples() {
		return maxNumberOfSideSamples;
	}

	public void setMaxNumberOfSideSamples(int maxNumberOfSideSamples) {
		this.maxNumberOfSideSamples = maxNumberOfSideSamples;
	}

	public void setSplitter(SplitSelector splitter) {
		this.splitter = splitter;
	}

	public int getMinSides() {
		return minSides;
	}

	public void setMinSides(int minSides) {
		this.minSides = minSides;
	}

	public int getExtraConsider() {
		return extraConsider;
	}

	public void setExtraConsider(int extraConsider) {
		this.extraConsider = extraConsider;
	}
}

