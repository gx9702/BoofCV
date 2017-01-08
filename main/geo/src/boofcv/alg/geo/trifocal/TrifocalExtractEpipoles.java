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

package boofcv.alg.geo.trifocal;

import boofcv.struct.geo.TrifocalTensor;
import georegression.struct.point.Point3D_F64;
import org.ejml.alg.dense.decomposition.svd.SafeSvd_D64;
import org.ejml.data.RowMatrix_F64;
import org.ejml.factory.DecompositionFactory_D64;
import org.ejml.interfaces.decomposition.SingularValueDecomposition_F64;
import org.ejml.ops.SingularOps_D64;

/**
 * <p>
 * Extracts the epipoles for views 2 and 3 with respect to view 1 from the trifocal tensor. Epipoles are found
 * in homogeneous coordinates.  Singular value decomposition is used to compute each vector and is robust to
 * noise and epipoles will have a norm of one..
 * </p>
 *
 * <p>Properties:</p>
 * <ul>
 *     <li> e2<sup>T</sup>*F12 = 0
 *     <li> e3<sup>T</sup>*F13 = 0
 * </ul>
 * <p>where F1i is a fundamental matrix from image 1 to i.</p>
 *
 * <p>References:</p>
 * <ul>
 * <li> R. Hartley, and A. Zisserman, "Multiple View Geometry in Computer Vision", 2nd Ed, Cambridge 2003 </li>
 * </ul>
 *
 * @author Peter Abeles
 */
public class TrifocalExtractEpipoles {

	// used to extract the null space
	private SingularValueDecomposition_F64<RowMatrix_F64> svd;

	// storage for left and right null space of the trifocal matrices
	private RowMatrix_F64 u1 = new RowMatrix_F64(3,1);
	private RowMatrix_F64 u2 = new RowMatrix_F64(3,1);
	private RowMatrix_F64 u3 = new RowMatrix_F64(3,1);
	private RowMatrix_F64 v1 = new RowMatrix_F64(3,1);
	private RowMatrix_F64 v2 = new RowMatrix_F64(3,1);
	private RowMatrix_F64 v3 = new RowMatrix_F64(3,1);

	private RowMatrix_F64 U = new RowMatrix_F64(3,3);
	private RowMatrix_F64 V = new RowMatrix_F64(3,3);

	// temporary storage for computed epipole
	private RowMatrix_F64 tempE = new RowMatrix_F64(3,1);

	public TrifocalExtractEpipoles() {
		svd = DecompositionFactory_D64.svd(3, 3, true, true, true);
		svd = new SafeSvd_D64(svd);
	}

	/**
	 * Extracts the epipoles from the trifocal tensor.  Extracted epipoles will have a norm of 1
	 * as an artifact of using SVD.
	 *
	 * @param tensor Input: Trifocal tensor.  Not Modified
	 * @param e2  Output: Epipole in image 2. Homogeneous coordinates. Modified
	 * @param e3  Output: Epipole in image 3. Homogeneous coordinates. Modified
	 */
	public void process( TrifocalTensor tensor , Point3D_F64 e2 , Point3D_F64 e3 ) {
		svd.decompose(tensor.T1);
		SingularOps_D64.nullVector(svd, true, v1);
		SingularOps_D64.nullVector(svd, false,u1);

		svd.decompose(tensor.T2);
		SingularOps_D64.nullVector(svd,true,v2);
		SingularOps_D64.nullVector(svd,false,u2);

		svd.decompose(tensor.T3);
		SingularOps_D64.nullVector(svd,true,v3);
		SingularOps_D64.nullVector(svd,false,u3);

		for( int i = 0; i < 3; i++ ) {
			U.set(i,0,u1.get(i));
			U.set(i,1,u2.get(i));
			U.set(i,2,u3.get(i));

			V.set(i, 0, v1.get(i));
			V.set(i, 1, v2.get(i));
			V.set(i, 2, v3.get(i));
		}

		svd.decompose(U);
		SingularOps_D64.nullVector(svd, false, tempE);
		e2.set(tempE.get(0), tempE.get(1), tempE.get(2));

		svd.decompose(V);
		SingularOps_D64.nullVector(svd, false, tempE);
		e3.set(tempE.get(0), tempE.get(1), tempE.get(2));
	}
}
