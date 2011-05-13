/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.abst.filter.derivative;

import gecv.alg.filter.derivative.HessianThree;
import gecv.struct.image.ImageFloat32;


/**
 * @author Peter Abeles
 */
public class HessianXY_Three_F32 implements HessianXY<ImageFloat32, ImageFloat32> {

	@Override
	public void process(ImageFloat32 inputImage , ImageFloat32 derivXX, ImageFloat32 derivYY, ImageFloat32 derivXY ) {
		HessianThree.process(inputImage, derivXX ,derivYY, derivXY,true);
	}

	@Override
	public int getBorder() {
		return 2;
	}
}