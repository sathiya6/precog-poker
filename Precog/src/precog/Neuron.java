/**
 * @author: Kevin Liu (kevin91liu@gmail.com)
 */

package precog;

import java.io.Serializable;
import java.util.ArrayList;

public class Neuron implements Serializable
{

	private static final long serialVersionUID = 3941480927560107580L;
	private static final int INITIAL_CAPACITY = 6; //default begin size of arraylist
	//need learning rate when doing backprop
	private ArrayList<Neuron> successors;
	private ArrayList<Double> weights;
	private ArrayList<Neuron> parents; 
	private ArrayList<Double> parent_weights;
	private double bias;
	private transient double curVal;
	private double threshold; //important for this to be in interval [0,1) 
	private String name;
	private int number;
	
	private transient double last_output;
	
	public Neuron(String _name, int _number)
	{
		successors = new ArrayList<Neuron>(INITIAL_CAPACITY);
		weights = new ArrayList<Double>(INITIAL_CAPACITY);
		parents = new ArrayList<Neuron>(INITIAL_CAPACITY);
		parent_weights = new ArrayList<Double>(INITIAL_CAPACITY);
		curVal = 0.;
		threshold = 0.;
		bias = 0.;
		name = _name;
		number = _number;
	}
	
	public int getNumber()
	{
		return number;
	}
	
	public double getLastOutput()
	{
		return last_output;
	}
	
	/**
	 * when getting output from the output field of NeuralNet, use this function
	 * to smooth the values (in the form of a sigmoid) so that the return value of
	 * this function will be either negative, 0, or if positive, < 1.0
	 * 
	 * the graph of this function is a sigmoid with asymptotes y = +1 and y = -1
	 * and f(0) = 0
	 */
	public double outputSmooth()
	{
		System.out.println("output curVal: " + curVal);
		double smoothed = (2. / (1. + Math.exp(-curVal))) - 1.; //maybe -curVal / 2 to stretch
		System.out.println("output smoothed val: " + smoothed);
		return smoothed;
	}
	
	/**
	 * sigmoid function with asymptotes y=0 and y=1. f(0) = 1/2
	 * @param value value to be smoothed
	 * @return double between 0 and 1
	 */
	public static double sigmoidSmooth(double value)
	{
		return (1. / (1. + Math.exp(-value)));
	}
	
	//must add to same index
	public void addChild(Neuron p, double _weight)
	{
		successors.add(p);
		weights.add(_weight);
		p.addParent(this, _weight);
	}
	
	public void addParent(Neuron p, double _weight)
	{
		parents.add(p);
		parent_weights.add(_weight);
	}
	
	public void setWeight(Neuron p, double _weight)
	{
		weights.set(successors.indexOf(p), _weight);
	}
	
	public void setParentWeight(Neuron p, double _weight)
	{
		parent_weights.set(parents.indexOf(p), _weight);
	}
	
	public void printWeights()
	{
		for (Neuron p : successors)
		{
			System.out.println(this.name + " to " + p.getName() + " weight: " + weights.get(successors.indexOf(p)));
		}
	}
	
	public String getName()
	{
		return name;
	}
	
	public ArrayList<Neuron> getSuccessors()
	{
		return successors;
	}
	
	public String formattedWeights()
	{
		return "implement this (Perceptron.formattedWeights)";
	}
	
	public double getWeight(Neuron successor)
	{
		return weights.get(successors.indexOf(successor));
	}
	
	
	public void evaluate()
	{
		last_output = sigmoidSmooth(curVal + bias);
		if (sigmoidSmooth(curVal + bias) >= threshold) fire(); //sigmoid...
		else last_output = 0.;
	}
	
	public void fire()
	{
		for (Neuron s : successors)
		{ //multiply by curVal. rationale: transmit strength of confidence
			s.receive(weights.get(successors.indexOf(s)) * last_output);
		}
	}
	
	public void receive(double val)
	{
		curVal += val;
	}
	
	public void setThreshold(double val)
	{
		threshold = val;
	}
	
	public void setBias(double val)
	{
		bias = val;
	}
	
	public double getCurVal()
	{
		return curVal;
	}
	
	public void reset()
	{
		curVal = 0.;
	}
	
	public void randomize()
	{
		bias = 0.;//Math.random(); for now, ignore bias to simplify life
		threshold = 0.8; //abitrary value... //Math.random();
	}
	
	public void init_input_node()
	{
		bias = 0.0;
		threshold = 0.0;
	}
	
	public void init_hidden_node()
	{
		bias = 0.;
		threshold = 0.8;
	}
	
	public void init_output_node()
	{
		bias = 0.;
		threshold = 0.8;
	}
}