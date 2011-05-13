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

package gecv.struct.convolve;


/**
 * Base class for all convolution kernels.
 *
 * @author Peter Abeles
 */
public abstract class KernelBase {
	public int width;

	/**
	 * The kernel's width.  This is an odd number.
	 *
	 * @return Kernel's width.
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * The radius is defined as the width divided by two.
	 *
	 * @return The kernel's radius.
	 */
	public int getRadius() {
		return width / 2;
	}

	/**
	 * Returns the dimension of this kernel, 1D or 2D.
	 *
	 * @return Kernel's dimension
	 */
	public abstract int getDimension();
}
