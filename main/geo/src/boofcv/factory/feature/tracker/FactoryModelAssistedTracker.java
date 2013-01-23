/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.feature.tracker;

import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.tracker.ModelAssistedTracker;
import boofcv.abst.feature.tracker.PkltConfig;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.alg.feature.tracker.AssistedPyramidKltTracker;
import boofcv.alg.interpolate.InterpolateRectangle;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.pyramid.PyramidUpdaterDiscrete;
import org.ddogleg.fitting.modelset.ModelFitter;
import org.ddogleg.fitting.modelset.ModelMatcher;

/**
 * @author Peter Abeles
 */
public class FactoryModelAssistedTracker {
	/**
	 * Pyramid KLT feature tracker.
	 *
	 * @see boofcv.struct.pyramid.PyramidUpdaterDiscrete
	 *
	 * @param scaling       Scales in the image pyramid. Recommend [1,2,4] or [2,4]
	 * @param featureRadius Size of the tracked feature.  Try 3 or 5
	 * @param configExtract Configuration for extracting features
	 * @param imageType     Input image type.
	 * @param derivType     Image derivative  type.
	 * @return KLT based tracker.
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand,Model,Info>
	ModelAssistedTracker<I,Model,Info> klt(int scaling[],
										   int featureRadius,
										   ConfigGeneralDetector configExtract,
										   ModelMatcher<Model, Info> matcherInitial,
										   ModelMatcher<Model, Info> matcherFinal,
										   ModelFitter<Model, Info> modelRefiner,
										   Class<I> imageType, Class<D> derivType) {
		PkltConfig<I, D> config =
				PkltConfig.createDefault(imageType, derivType);
		config.pyramidScaling = scaling;
		config.featureRadius = featureRadius;

		GeneralFeatureDetector<I, D> detector =
				FactoryPointSequentialTracker.createShiTomasi(configExtract, config.typeDeriv);

		InterpolateRectangle<I> interpInput = FactoryInterpolation.<I>bilinearRectangle(config.typeInput);
		InterpolateRectangle<D> interpDeriv = FactoryInterpolation.<D>bilinearRectangle(config.typeDeriv);

		ImageGradient<I,D> gradient = FactoryDerivative.sobel(config.typeInput, config.typeDeriv);

		PyramidUpdaterDiscrete<I> pyramidUpdater = FactoryPyramid.discreteGaussian(config.typeInput, -1, 2);

		return new AssistedPyramidKltTracker<I, D,Model,Info>(config,pyramidUpdater,detector,
				gradient,interpInput,interpDeriv,matcherInitial,matcherFinal,modelRefiner);
	}
}