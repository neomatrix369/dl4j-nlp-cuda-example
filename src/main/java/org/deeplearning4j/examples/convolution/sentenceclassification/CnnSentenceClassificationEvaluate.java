/*******************************************************************************
 * Copyright (c) 2015-2019 Skymind, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package org.deeplearning4j.examples.convolution.sentenceclassification;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.deeplearning4j.iterator.CnnSentenceDataSetIterator;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.optimize.api.InvocationType;
import org.deeplearning4j.optimize.listeners.EvaluativeListener;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

/**
 * Convolutional Neural Networks for Sentence Classification - https://arxiv.org/abs/1408.5882
 *
 * Specifically, this is the 'static' model from there
 *
 * @author Alex Black
 */
public class CnnSentenceClassificationEvaluate extends CnnSentenceClassificationRunner {

    private static Logger log = LoggerFactory.getLogger(CnnSentenceClassificationEvaluate.class);

    private static final boolean TESTING = false;

    private String modelFilePath;

    public CnnSentenceClassificationEvaluate(String modelFilePath) {
        this.modelFilePath = modelFilePath;
    }

    public void run(
            int batchSize,
            int truncateReviewsToLength,  //Truncate reviews with length (# words) greater than this
            Random rng                    //For shuffling repeatability
    ) throws Exception {
        log.info("Loading pretrained model");

        log.info(String.format("batchSize = %d", batchSize));
        log.info(String.format("truncateReviewsToLength = %d", truncateReviewsToLength));
        log.info(String.format("rng = %d", rng));

        ComputationGraph model = ComputationGraph.load(new File(modelFilePath), true);

        log.info("Number of parameters by layer:");
        for(Layer l : model.getLayers() ){
            log.info(String.format("\t%s\t%d", l.conf().getLayer().getLayerName(), l.numParams()));
        }

        //Load word vectors and get the DataSetIterators for testing
        log.info("Loading word vectors and creating DataSetIterators (this may take a moment: ~1 to 2 minutes)");
        WordVectors wordVectors = WordVectorSerializer.loadStaticModel(new File(WORD_VECTORS_PATH));
        DataSetIterator testIter = getDataSetIterator(TESTING, wordVectors, batchSize, truncateReviewsToLength, rng);

        log.info(String.format("\n\nEvaluating the model (please be patient this may take a moment): %s", modelFilePath));
        model.setListeners(
                new ValohaiMetadataCreator(10),
                new EvaluativeListener(testIter, 1, InvocationType.EPOCH_END)
        );
        Evaluation modelEvaluation = model.evaluate(testIter);

        log.info("\n\nPrinting model evaluation stats:");
        log.info(modelEvaluation.stats());
        log.info("\n\nFinished evaluating model.");

        //After training: load a single sentence and generate a prediction
        String pathFirstNegativeFile = FilenameUtils.concat(DATA_PATH, "aclImdb/test/neg/0_2.txt");
        String contentsFirstNegative = FileUtils.readFileToString(new File(pathFirstNegativeFile));
        INDArray featuresFirstNegative = ((CnnSentenceDataSetIterator)testIter).loadSingleSentence(contentsFirstNegative);

        INDArray predictionsFirstNegative = model.outputSingle(featuresFirstNegative);
        List<String> labels = testIter.getLabels();

        log.info("\n\nPredictions for first negative review:");
        for( int index=0; index<labels.size(); index++ ){
            log.info(String.format("P(%s) = %s", labels.get(index), predictionsFirstNegative.getDouble(index)));
        }
    }
}