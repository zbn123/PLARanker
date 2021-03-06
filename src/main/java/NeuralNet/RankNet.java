package NeuralNet;

import Entities.RankList;
import org.apache.log4j.Logger;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by vibhor.go on 11/14/16.
 */

public class RankNet{
    private int inputSize;
    private int outputSize=1;
    protected List<Layer> layers;
    private int nHiddenLayer=1;
    private int nHiddenNodeperLayer=10;
    private static int nIterations=200;
    private List<RankList> trainSamples;
    private List<RankList> validationSamples;
    private List<RankList> testSamples;
    protected double bestScoreonValidation=0.0;
    private ArrayList<ArrayList<Double>> bestValidationModel= new ArrayList<ArrayList<Double>>();
    protected static Scorer mrrScorer= new MRRScorer();
    private static DecimalFormat doubleFormat = new DecimalFormat("#.0000");
    private static Logger logger = Logger.getLogger(RankNet.class.getName());
    public RankNet(List<RankList> trainSamples, List<RankList> validationSamples,List<RankList> testSamples, int inputSize, int outputSize)
    {
        this.inputSize=inputSize;
        this.outputSize=outputSize;
        this.trainSamples= trainSamples;
        this.validationSamples=validationSamples;
        this.testSamples=testSamples;
        this.layers= new ArrayList<Layer>();
    }

    public RankNet(){}

    public void initialize()
    {
        layers.add(new Layer(inputSize+1));
        for(int i=0;i<nHiddenLayer;i++)layers.add(new Layer(nHiddenNodeperLayer));
        layers.add(new Layer(outputSize));
        for(int i=1;i<layers.size();i++)layers.get(i).connectLayer(layers.get(i-1));
        Layer inputLayer= layers.get(0);
        //connect bias to all layers
        //System.out.println(inputLayer.neurons.size());
        Neuron biasNeuron=inputLayer.neurons.get(inputSize);
        for(int i=2;i<layers.size();i++){

            layers.get(i).connectNeuron(biasNeuron);

        }
        for(int i=0;i<layers.size()-1;i++)bestValidationModel.add(new ArrayList<Double>());

    }

    public void setInput(RankList rl)
    {
        List<Neuron> inputLayerNeurons= layers.get(0).neurons;
//        System.out.println(inputLayerNeurons.size()+" "+rl.listFeatures.get(0).size());
        for(List<Double> sampleFeatures: rl.listFeatures)
        {
            for(int i=0;i<sampleFeatures.size();i++)
            {
                inputLayerNeurons.get(i).outputs.add(sampleFeatures.get(i)) ;
            }
            inputLayerNeurons.get(inputLayerNeurons.size()-1).outputs.add(1.0d);
        }
        for(int i=1;i<layers.size();i++)
        {
            layers.get(i).setInput(rl.listFeatures.size());
        }

    }

    public void clearInputs()
    {
        for(Layer layer: layers)
        {
            for(Neuron neuron: layer.neurons)
            {
                neuron.outputs.clear();
            }
        }
    }

    public ArrayList<ArrayList<Integer>> batchFeedForward(RankList rankList,int [] sortIndex)
    {
        for(int i=0;i<rankList.listFeatures.size();i++)
        {
            for(int j=1;j<layers.size();j++)
            {
                layers.get(j).computeOutput(i);
            }
        }
        ArrayList<ArrayList<Integer> > pairmap= new ArrayList<ArrayList<Integer> >();
        for(int i=0;i<rankList.listFeatures.size();i++)
        {
            int k=0;
            ArrayList<Integer> pairmap_i=new ArrayList<Integer>();
            for(int j=0;j<rankList.listFeatures.size();j++)
            {
                if((rankList.targetValues.get(i)>rankList.targetValues.get(j))&&(sortIndex[i]>sortIndex[j])){
                    pairmap_i.add(j);
                }
            }
            pairmap.add(pairmap_i);
        }
        return pairmap;
    }

    public ArrayList<ArrayList<Double> > computePairWeight(ArrayList<ArrayList<Integer> > pairmap)
    {
        return null;
    }

    public void batchBackPropagate(ArrayList<ArrayList<Integer> > pairMap, ArrayList<ArrayList<Double> >pairWeight)
    {
        ArrayList<Double> computedDeltai= new ArrayList<Double>();
        ArrayList<Double> computedDeltaj= new ArrayList<Double>();
        for(int i=0;i<pairMap.size();i++)
        {
            computedDeltaj= new ArrayList<Double>();
            Layer outputLayer= layers.get(layers.size()-1);
            for(Neuron neuron: outputLayer.neurons){
                neuron.computeOuterDeltas(pairMap, pairWeight, i);
            }
            for(int j=layers.size()-2;j>0;j--)
            {
                layers.get(j).computeDelta(pairMap,pairWeight,i);
            }
            for(int j=layers.size()-1;j>0;j--)
            {
                layers.get(j).updateWeight(pairMap,i);
            }
            computedDeltai.add(layers.get(layers.size()-1).neurons.get(0).getDelta_i());
            for(int k=0;k<layers.get(layers.size()-1).neurons.get(0).inLinks.size();k++)
                    computedDeltaj.add(layers.get(layers.size()-1).neurons.get(0).inLinks.get(k).getWeightAdjustment());
            //System.out.print(computedDeltaj.toString()+"  ");
            //System.out.print(Arrays.toString(layers.get(layers.size()-1).neurons.get(0).getDelta_j()));
            //System.out.println("  "+pairMap.get(i));
        }
        //System.out.println();
        //System.out.println(computedDeltai.toString());

        //System.out.println();
    }

    public RankList reorder(RankList rankList, int [] sortIndex)
    {

        return new RankList(rankList, sortIndex);
    }

    public void trainIteration()
    {
//        for(Synapse s: layers.get(layers.size()-3).neurons.get(0).inLinks)
//        {
//            System.out.print(s.getWeight()+" ");
//        }
//        System.out.println();
//        for(Synapse s: layers.get(layers.size()-3).neurons.get(1).inLinks)
//        {
//            System.out.print(s.getWeight()+" ");
//        }
//        System.out.println();

        for(RankList rl:trainSamples)
        {
            //if(!rl.getViewid().equalsIgnoreCase("B28EE0B31C03D224A3ECE63F1BEBC096"))continue;
            ArrayList<Double> scoresOnModel=eval(rl);
            int [] sortIndex=getSortedIndices(scoresOnModel, false);
            RankList rankList= reorder(rl,sortIndex);
            clearInputs();
            setInput(rankList);
            ArrayList<ArrayList<Integer> > pairmap= batchFeedForward(rankList,sortIndex);
            ArrayList<ArrayList<Double> > pairWeight = computePairWeight(pairmap);
            //System.out.println(Arrays.toString(sortIndex));
            batchBackPropagate(pairmap,pairWeight);
//            for(ArrayList<Double> doubleArrayList:rankList.listFeatures)
//            System.out.println(doubleArrayList);
//            System.out.println(rankList.targetValues);
//            System.out.println(rl.getViewid());
//            System.out.println();
//            System.out.println();
        }
        //System.out.println();
        //System.out.println();

    }

    public void train()
    {
        Double score=0.01;
        Double esoriginalScore=0.0;
        Double prevScore=0.0;
        Double idealScore=0.0;
        RankList predictedrankList;
        for(int i=0;i<nIterations;i++)
        {


            if(validationSamples!=null)
            {
                score=0.0;
                esoriginalScore=0.0;
                for(RankList rl:validationSamples)
                {

                    ArrayList<Double> scorefromNetwork=eval(rankedProducts(rl));
                    //System.out.println(scorefromNetwork.toString());
                    if (i==0)esoriginalScore+=mrrScorer.score(rl);
                    score+=mrrScorer.score(rankedProducts(rl));
                }
                if(i==0)esoriginalScore/=validationSamples.size();
                if(i==0)System.out.println("esoriginalScore: "+esoriginalScore);
                score/=validationSamples.size();
                System.out.println(score+" iteration: "+i);
                if(score>bestScoreonValidation)
                {
                    bestScoreonValidation=score;
                    saveBestValidationModel();
                }
            }
            Layer outputLayer=layers.get(layers.size()-1);
            for(Synapse synapse:outputLayer.neurons.get(0).inLinks)
                System.out.print(String.format("%.4f",synapse.getWeight())+" ");
            System.out.println();
            trainIteration();
            //if(score<prevScore)Neuron.learningRate*=0.8;
            prevScore=score;

        }
        if (validationSamples!=null && (bestScoreonValidation-0.0)>0.000001)loadBestValidationModel();

        score=0.0;
        idealScore=0.0;
        for (RankList rankList:trainSamples)
        {
            predictedrankList=rankedProducts(rankList);
            Double temp= mrrScorer.score(predictedrankList);
            //for(int i=0;i<predictedrankList.targetValues.size();i++)System.out.print(predictedrankList.targetValues.get(i)+" ");
            System.out.println(rankList.getViewid()+" "+temp);
            score+=temp;
            idealScore+=mrrScorer.getIdealScore(rankList);
        }
        score/=trainSamples.size();
        idealScore/=trainSamples.size();
        logger.info("Score on training data "+ score);
        logger.info("Ideal Score on training data "+idealScore);


        if (validationSamples!=null)
        {
            score=0.0;
            idealScore=0.0;
            for (RankList rankList:validationSamples)
            {
                score+=mrrScorer.score(rankedProducts(rankList));
                idealScore+=mrrScorer.getIdealScore(rankList);
            }
            score/=validationSamples.size();
            idealScore/=validationSamples.size();
            logger.info("Score on validation data "+ score);
            logger.info("Ideal Score on validation data "+idealScore);
        }

        if (testSamples!=null)
        {
            score=0.0;
            idealScore=0.0;
            esoriginalScore=0.0;
            for (RankList rankList:testSamples)
            {
                score+=mrrScorer.score(rankedProducts(rankList));
                esoriginalScore+=mrrScorer.score(rankList);
                idealScore+=mrrScorer.getIdealScore(rankList);
            }
            esoriginalScore/=testSamples.size();
            score/=testSamples.size();
            idealScore/=testSamples.size();
            logger.info("network Score on test data "+ score);
            logger.info("elasticsearch score on test data "+esoriginalScore);
            logger.info("ideal Score on test data "+idealScore);
        }

    }

    public void saveBestValidationModel()
    {
        for(int i=0;i<layers.size()-1;i++)
        {
            bestValidationModel.get(i).clear();
            bestValidationModel.set(i,layers.get(i).getOutlinkWeights());
            System.out.println("best validation model layer "+i+" size "+bestValidationModel.get(i).size());
        }
    }

    public void loadBestValidationModel()
    {
        for (int i=0;i<layers.size()-1;i++)
        {
            System.out.print("loading layer " + i + " ");
            System.out.println(bestValidationModel.get(i).size());
            layers.get(i).setOutlinkWeights(bestValidationModel.get(i));
        }
    }

    public void setLayerWeights(ArrayList<ArrayList<Double>> layerWeights)
    {
        for (int i=0;i<layers.size()-1;i++)
        {
            layers.get(i).setOutlinkWeights(layerWeights.get(i));
        }
    }


    public int[] getSortedIndices(ArrayList<Double> scores, boolean asc)
    {
        int[] sortedIndex = new int[scores.size()];
        for(int i=0;i<sortedIndex.length;i++)sortedIndex[i]=i;
        int swapIndex, temp;
        for(int i=0;i<sortedIndex.length;i++)
        {
            swapIndex=i;
            for(int j=i+1;j<sortedIndex.length;j++)
            {

                if((scores.get(sortedIndex[j])<scores.get(sortedIndex[swapIndex])) && asc==true)
                    swapIndex=j;
                else if((scores.get(sortedIndex[j])>scores.get(sortedIndex[swapIndex])) && asc==false)
                    swapIndex=j;
            }
            temp=sortedIndex[swapIndex];
            sortedIndex[swapIndex]=sortedIndex[i];
            sortedIndex[i]=temp;
        }

        return sortedIndex;
    }

    public ArrayList<Double> eval(RankList rankList)
    {
        clearInputs();
        setInput(rankList);
        for(int i=1;i<layers.size();i++)
        {
            for(int j=0;j<rankList.listFeatures.size();j++)
            {
                layers.get(i).computeOutput(j);
                //System.out.println("computed output for index "+j);
            }
        }
        return layers.get(layers.size()-1).neurons.get(0).outputs;
    }

    public RankList rankedProducts(RankList rl)
    {
        ArrayList<Double> scoresOnModel=eval(rl);
        int [] sortIndex=getSortedIndices(scoresOnModel, false);
        return reorder(rl,sortIndex);
    }

    public String getName()
    {
        return "RankNet";
    }

    public int getInputSize() {
        return inputSize;
    }

    public void setInputSize(int inputSize) {
        this.inputSize = inputSize;
    }

    public int getOutputSize() {
        return outputSize;
    }

    public void setOutputSize(int outputSize) {
        this.outputSize = outputSize;
    }

    public void setnHiddenLayer(int number)
    {
        this.nHiddenLayer= number;
    }

    public int getnHiddenNodeperLayer() {
        return nHiddenNodeperLayer;
    }

    public void setnHiddenNodeperLayer(int nHiddenNodeperLayer) {
        this.nHiddenNodeperLayer = nHiddenNodeperLayer;
    }

    public void setScorer(Scorer scorer)
    {
        mrrScorer=scorer;
    }
}