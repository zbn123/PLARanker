package NeuralNet;


import java.util.Random;

/**
 * Created by vibhor.go on 11/14/16.
 */
public class Synapse {
    protected Neuron outputNeuron;
    protected Neuron inputNeuron;
    private Double weight=0.0;
    public static Random random= new Random();
    private Double weightAdjustment=0.0;


    public Synapse(Neuron inputNeuron, Neuron outputNeuron)
    {
        this.outputNeuron= outputNeuron;
        this.inputNeuron= inputNeuron;
        this.weight= (random.nextInt(2)==0?1:-1)*random.nextDouble()/10;
        inputNeuron.outlinks.add(this);
        outputNeuron.inLinks.add(this);
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public void adjustWeight()
    {
        this.weight+=weightAdjustment;
        weightAdjustment=0.0;
    }

    public Double getWeightAdjustment() {
        return weightAdjustment;
    }

    public void setWeightAdjustment(Double weightAdjustment) {
        this.weightAdjustment = weightAdjustment;
    }
}