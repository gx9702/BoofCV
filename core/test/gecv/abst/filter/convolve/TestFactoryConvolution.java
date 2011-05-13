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

package gecv.abst.filter.convolve;

import gecv.abst.filter.FilterInterface;
import gecv.alg.drawing.impl.BasicDrawing_I8;
import gecv.alg.filter.convolve.ConvolveExtended;
import gecv.alg.filter.convolve.ConvolveImageNoBorder;
import gecv.alg.filter.convolve.ConvolveNormalized;
import gecv.alg.filter.convolve.KernelFactory;
import gecv.core.image.UtilImageFloat32;
import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.convolve.Kernel1D_I32;
import gecv.struct.convolve.Kernel2D_F32;
import gecv.struct.convolve.Kernel2D_I32;
import gecv.struct.image.*;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;


/**
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class TestFactoryConvolution {

	int radius = 2;
	Random rand = new Random(2342);

	int width = 30;
	int height = 40;

	@Test
	public void convolve1D_F32() {
		Kernel1D_F32 kernel = KernelFactory.random1D_F32(radius,0,5,rand);

		FilterInterface<ImageFloat32,ImageFloat32> conv;

		ImageFloat32 input = new ImageFloat32(width,height);
		ImageFloat32 found = new ImageFloat32(width,height);
		ImageFloat32 expected = new ImageFloat32(width,height);

		UtilImageFloat32.randomize(input,rand,0,5);

		// CHECK NO BORDER
		conv = FactoryConvolution.convolve( kernel,ImageFloat32.class,ImageFloat32.class,BorderType.SKIP,true);
		conv.process(input,found);
		ConvolveImageNoBorder.horizontal(kernel,input,expected,false);
		GecvTesting.assertEquals(expected,found,0,1e-4f);

		// CHECK EXTENDED
		conv = FactoryConvolution.convolve( kernel,ImageFloat32.class,ImageFloat32.class,BorderType.EXTENDED,true);
		conv.process(input,found);
		ConvolveExtended.horizontal(kernel,input,expected);
		GecvTesting.assertEquals(expected,found,0,1e-4f);

		// CHECK NORMALIZED
		conv = FactoryConvolution.convolve( kernel,ImageFloat32.class,ImageFloat32.class,BorderType.NORMALIZED,true);
		conv.process(input,found);
		ConvolveNormalized.horizontal(kernel,input,expected);
		GecvTesting.assertEquals(expected,found,0,1e-4f);
	}

	@Test
	public void convolve1D_I32() {

		Kernel1D_I32 kernel = KernelFactory.random1D_I32(radius,0,5,rand);

		FilterInterface conv;

		ImageUInt8 input = new ImageUInt8(width,height);
		ImageSInt16 found = new ImageSInt16(width,height);
		ImageSInt16 expected = new ImageSInt16(width,height);

		BasicDrawing_I8.randomize(input,rand,0,5);

		// CHECK NO BORDER
		conv = FactoryConvolution.convolve( kernel,ImageUInt8.class,ImageSInt16.class,BorderType.SKIP,true);
		conv.process(input,found);
		ConvolveImageNoBorder.horizontal(kernel,input,expected,false);
		GecvTesting.assertEquals(expected,found,0);

		// CHECK EXTENDED
		conv = FactoryConvolution.convolve( kernel,ImageUInt8.class,ImageSInt16.class,BorderType.EXTENDED,true);
		conv.process(input,found);
		ConvolveExtended.horizontal(kernel,input,expected);
		GecvTesting.assertEquals(expected,found,0);

		// CHECK NORMALIZED
		ImageUInt8 found8 = new ImageUInt8(width,height);
		ImageUInt8 expected8 = new ImageUInt8(width,height);
		conv = FactoryConvolution.convolve( kernel,ImageUInt8.class,ImageUInt8.class,BorderType.NORMALIZED,true);
		conv.process(input,found8);
		ConvolveNormalized.horizontal(kernel,input,expected8);
		GecvTesting.assertEquals(expected8,found8,0);
	}

	@Test
	public void convolve2D_F32() {
		Kernel2D_F32 kernel = KernelFactory.random2D_F32(radius,0,5,rand);

		FilterInterface<ImageFloat32,ImageFloat32> conv;

		ImageFloat32 input = new ImageFloat32(width,height);
		ImageFloat32 found = new ImageFloat32(width,height);
		ImageFloat32 expected = new ImageFloat32(width,height);

		UtilImageFloat32.randomize(input,rand,0,5);

		// CHECK NO BORDER
		conv = FactoryConvolution.convolve( kernel,ImageFloat32.class,ImageFloat32.class,BorderType.SKIP);
		conv.process(input,found);
		ConvolveImageNoBorder.convolve(kernel,input,expected);
		GecvTesting.assertEquals(expected,found,0,1e-4f);

		// CHECK EXTENDED
		conv = FactoryConvolution.convolve( kernel,ImageFloat32.class,ImageFloat32.class,BorderType.EXTENDED);
		conv.process(input,found);
		ConvolveExtended.convolve(kernel,input,expected);
		GecvTesting.assertEquals(expected,found,0,1e-4f);

		// CHECK NORMALIZED
//		conv = FactoryConvolution.convolve( kernel,ImageFloat32.class,ImageFloat32.class,BorderType.NORMALIZED);
//		conv.process(input,found);
//		ConvolveNormalized.convolve(kernel,input,expected);
//		GecvTesting.assertEquals(expected,found,0,1e-4f);
		fail("Add renormalizing 2D kernels");
	}

	@Test
	public void convolve2D_I32() {

		Kernel2D_I32 kernel = KernelFactory.random2D_I32(radius,0,5,rand);

		FilterInterface conv;

		ImageUInt8 input = new ImageUInt8(width,height);
		ImageSInt16 found = new ImageSInt16(width,height);
		ImageSInt16 expected = new ImageSInt16(width,height);

		BasicDrawing_I8.randomize(input,rand,0,5);

		// CHECK NO BORDER
		conv = FactoryConvolution.convolve( kernel,ImageUInt8.class,ImageSInt16.class,BorderType.SKIP);
		conv.process(input,found);
		ConvolveImageNoBorder.convolve(kernel,input,expected);
		GecvTesting.assertEquals(expected,found,0);

		// CHECK EXTENDED
		conv = FactoryConvolution.convolve( kernel,ImageUInt8.class,ImageSInt16.class,BorderType.EXTENDED);
		conv.process(input,found);
		ConvolveExtended.convolve(kernel,input,expected);
		GecvTesting.assertEquals(expected,found,0);

		// CHECK NORMALIZED
//		ImageUInt8 found8 = new ImageUInt8(width,height);
//		ImageUInt8 expected8 = new ImageUInt8(width,height);
//		conv = FactoryConvolution.convolve( kernel,ImageUInt8.class,ImageUInt8.class,BorderType.NORMALIZED);
//		conv.process(input,found8);
//		ConvolveNormalized.convolve(kernel,input,expected8);
//		GecvTesting.assertEquals(expected8,found8,0);
		fail("Add renormalizing 2D kernels");
	}
}
