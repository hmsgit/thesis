/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wrapper.generator;

/**
 *
 * @author mahmud
 */
/*
 *    MyRandomRBFGenerator.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *    
 */

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

import java.io.Serializable;
import java.util.Random;

import moa.core.InstancesHeader;
import moa.core.MiscUtils;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.options.IntOption;
import moa.streams.InstanceStream;
import moa.tasks.TaskMonitor;

/**
 * Stream generator for a random radial basis function stream.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
public class MyRandomRBFGenerator extends AbstractOptionHandler implements
        InstanceStream {
    
    public int _curCentroidIndex;

    @Override
    public String getPurposeString() {
        return "Generates a random radial basis function stream.";
    }

    private static final long serialVersionUID = 1L;

    public IntOption modelRandomSeedOption = new IntOption("modelRandomSeed",
            'r', "Seed for random generation of model.", 1);

    public IntOption instanceRandomSeedOption = new IntOption(
            "instanceRandomSeed", 'i',
            "Seed for random generation of instances.", 1);

    public IntOption numClassesOption = new IntOption("numClasses", 'c',
            "The number of classes to generate.", 2, 2, Integer.MAX_VALUE);

    public IntOption numAttsOption = new IntOption("numAtts", 'a',
            "The number of attributes to generate.", 10, 0, Integer.MAX_VALUE);

    public IntOption numCentroidsOption = new IntOption("numCentroids", 'n',
            "The number of centroids in the model.", 50, 1, Integer.MAX_VALUE);

    protected static class Centroid implements Serializable {

        private static final long serialVersionUID = 1L;

        public double[] centre;

        public int classLabel;

        public double stdDev;
    }

    protected InstancesHeader streamHeader;

    protected Centroid[] centroids;

    protected double[] centroidWeights;

    protected Random instanceRandom;

    @Override
    public void prepareForUseImpl(TaskMonitor monitor,
            ObjectRepository repository) {
        monitor.setCurrentActivity("Preparing random RBF...", -1.0);
        generateHeader();
        generateCentroids();
        restart();
    }

    @Override
    public InstancesHeader getHeader() {
        return this.streamHeader;
    }

    @Override
    public long estimatedRemainingInstances() {
        return -1;
    }

    @Override
    public boolean hasMoreInstances() {
        return true;
    }

    @Override
    public boolean isRestartable() {
        return true;
    }

    @Override
    public void restart() {
        this.instanceRandom = new Random(this.instanceRandomSeedOption.getValue());
    }

    @Override
    public Instance nextInstance() {
        _curCentroidIndex = MiscUtils.chooseRandomIndexBasedOnWeights(this.centroidWeights,
                this.instanceRandom);
        Centroid centroid = this.centroids[_curCentroidIndex];
        int numAtts = this.numAttsOption.getValue();
        double[] attVals = new double[numAtts + 1];
        for (int i = 0; i < numAtts; i++) {
            attVals[i] = (this.instanceRandom.nextDouble() * 2.0) - 1.0;
        }
        double magnitude = 0.0;
        for (int i = 0; i < numAtts; i++) {
            magnitude += attVals[i] * attVals[i];
        }
        magnitude = Math.sqrt(magnitude);
        double desiredMag = this.instanceRandom.nextGaussian()
                * centroid.stdDev;
        double scale = desiredMag / magnitude;
        for (int i = 0; i < numAtts; i++) {
            attVals[i] = centroid.centre[i] + attVals[i] * scale;
        }
        Instance inst = new DenseInstance(1.0, attVals);
        inst.setDataset(getHeader());
        inst.setClassValue(centroid.classLabel);
        return inst;
    }

    protected void generateHeader() {
        FastVector attributes = new FastVector();
        for (int i = 0; i < this.numAttsOption.getValue(); i++) {
            attributes.addElement(new Attribute("att" + (i + 1)));
        }
        FastVector classLabels = new FastVector();
        for (int i = 0; i < this.numClassesOption.getValue(); i++) {
            classLabels.addElement("class" + (i + 1));
        }
        attributes.addElement(new Attribute("class", classLabels));
        this.streamHeader = new InstancesHeader(new Instances(
                getCLICreationString(InstanceStream.class), attributes, 0));
        this.streamHeader.setClassIndex(this.streamHeader.numAttributes() - 1);
    }

    protected void generateCentroids() {
        Random modelRand = new Random(this.modelRandomSeedOption.getValue());
        this.centroids = new Centroid[this.numCentroidsOption.getValue()];
        this.centroidWeights = new double[this.centroids.length];
        for (int i = 0; i < this.centroids.length; i++) {
            this.centroids[i] = new Centroid();
            double[] randCentre = new double[this.numAttsOption.getValue()];
            for (int j = 0; j < randCentre.length; j++) {
                randCentre[j] = modelRand.nextDouble();
            }
            this.centroids[i].centre = randCentre;
            this.centroids[i].classLabel = modelRand.nextInt(this.numClassesOption.getValue());
            this.centroids[i].stdDev = modelRand.nextDouble();
            this.centroidWeights[i] = modelRand.nextDouble();
        }
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }
}
