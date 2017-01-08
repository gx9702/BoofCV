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

package boofcv.abst.geo.f;

import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.abst.geo.GeoModelEstimatorNto1;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.GeoModelEstimatorN;
import boofcv.struct.geo.QueueMatrix;
import org.ddogleg.fitting.modelset.DistanceFromModel;
import org.ejml.data.RowMatrix_F64;

/**
 * Implementation of {@link GeoModelEstimatorNto1} for epipolar matrices.
 *
 * @author Peter Abeles
 */
public class EstimateNto1ofEpipolar
		extends GeoModelEstimatorNto1<RowMatrix_F64,AssociatedPair>
		implements Estimate1ofEpipolar
{
	public EstimateNto1ofEpipolar(GeoModelEstimatorN<RowMatrix_F64, AssociatedPair> alg,
								  DistanceFromModel<RowMatrix_F64, AssociatedPair> distance,
								  int numTest) {
		super(alg, distance, new QueueMatrix(3,3), numTest);
	}

	@Override
	protected void copy(RowMatrix_F64 src, RowMatrix_F64 dst) {
		dst.set(src);
	}
}
