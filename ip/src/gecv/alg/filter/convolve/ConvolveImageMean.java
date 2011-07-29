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

package gecv.alg.filter.convolve;

import gecv.alg.InputSanityCheck;
import gecv.alg.filter.convolve.noborder.ImplConvolveMean;
import gecv.alg.filter.convolve.normalized.ConvolveNormalized_JustBorder;
import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.convolve.Kernel1D_I32;
import gecv.struct.image.*;


/**
 * <p>
 * Convolves a mean filter across the image.  The mean value of all the pixels are computed inside the kernel.
 * </p>
 *
 * @author Peter Abeles
 */
public class ConvolveImageMean {

	/**
	 * Performs a horizontal 1D convolution which computes the mean value of elements
	 * inside the kernel.
	 *
	 * @param input The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param radius Kernel size.
	 */
	public static void horizontal(ImageFloat32 input, ImageFloat32 output, int radius) {
		InputSanityCheck.checkSameShape(input , output);

		Kernel1D_F32 kernel = FactoryKernel.table1D_F32(radius,true);
		ConvolveNormalized_JustBorder.horizontal(kernel, input ,output );
		ImplConvolveMean.horizontal(input, output, radius, true);
	}

	/**
	 * Performs a vertical 1D convolution which computes the mean value of elements
	 * inside the kernel.
	 *
	 * @param input The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param radius Kernel size.
	 */
	public static void vertical(ImageFloat32 input, ImageFloat32 output, int radius) {
		InputSanityCheck.checkSameShape(input , output);

		Kernel1D_F32 kernel = FactoryKernel.table1D_F32(radius,true);
		ConvolveNormalized_JustBorder.vertical(kernel, input ,output );
		ImplConvolveMean.vertical(input, output, radius, true);
	}

	/**
	 * Performs a horizontal 1D convolution which computes the mean value of elements
	 * inside the kernel.
	 *
	 * @param input The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param radius Kernel size.
	 */
	public static void horizontal(ImageUInt8 input, ImageInt8 output, int radius) {
		InputSanityCheck.checkSameShape(input , output);

		Kernel1D_I32 kernel = FactoryKernel.table1D_I32(radius);
		ConvolveNormalized_JustBorder.horizontal(kernel, input, output);
		ImplConvolveMean.horizontal(input, output, radius, true);
	}

	/**
	 * Performs a vertical 1D convolution which computes the mean value of elements
	 * inside the kernel.
	 *
	 * @param input The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param radius Kernel size.
	 */
	public static void vertical(ImageUInt8 input, ImageInt8 output, int radius) {
		InputSanityCheck.checkSameShape(input , output);

		Kernel1D_I32 kernel = FactoryKernel.table1D_I32(radius);
		ConvolveNormalized_JustBorder.vertical(kernel, input, output);
		ImplConvolveMean.vertical(input, output, radius, true);
	}

	/**
	 * Performs a horizontal 1D convolution which computes the mean value of elements
	 * inside the kernel.
	 *
	 * @param input The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param radius Kernel size.
	 */
	public static void horizontal(ImageSInt16 input, ImageInt16 output, int radius) {
		InputSanityCheck.checkSameShape(input , output);

		Kernel1D_I32 kernel = FactoryKernel.table1D_I32(radius);
		ConvolveNormalized_JustBorder.horizontal(kernel, input, output);
		ImplConvolveMean.horizontal(input, output, radius, true);
	}

	/**
	 * Performs a vertical 1D convolution which computes the mean value of elements
	 * inside the kernel.
	 *
	 * @param input The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param radius Kernel size.
	 */
	public static void vertical(ImageSInt16 input, ImageInt16 output, int radius ) {
		InputSanityCheck.checkSameShape(input , output);

		Kernel1D_I32 kernel = FactoryKernel.table1D_I32(radius);
		ConvolveNormalized_JustBorder.vertical(kernel, input, output);
		ImplConvolveMean.vertical(input, output, radius, true);
	}
}
