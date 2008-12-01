/**
 * @author: Kevin Liu (kevin91liu@gmail.com)
 */

package precog;

import java.io.Serializable;
import java.util.ArrayList;

public class Perceptron implements Serializable
{

	private static final long serialVersionUID = 3941480927560107580L;
	private static final int INITIAL_CAPACITY = 6; //default begin size of arraylist
	//need learning rate when doing backprop
	private ArrayList<Perceptron> successors;
	private ArrayList<Double> weights;
	private ArrayList<Perceptron> parents;
	private ArrayList<Double> parent_weights;
	private double bias;
	private transient double curVal;
	private double threshold;
	private String name;
	
	public Perceptron(String _name)
	{
		successors = new ArrayList<Perceptron>(INITIAL_CAPACITY);
		weights = new ArrayList<Double>(INITIAL_CAPACITY);
		parents = new ArrayList<Perceptron>(INITIAL_CAPACITY);
		parent_weights = new ArrayList<Double>(INITIAL_CAPACITY);
		curVal = 0.;
		threshold = 0.;
		bias = 0.;
		name = _name;
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
	
	//must add to same index
	public void addChild(Perceptron p, double _weight)
	{
		successors.add(p);
		weights.add(_weight);
		p.addParent(this, _weight);
	}
	
	public void addParent(Perceptron p, double _weight)
	{
		parents.add(p);
		parent_weights.add(_weight);
	}
	
	public void setWeight(Perceptron p, double _weight)
	{
		weights.set(successors.indexOf(p), _weight);
	}
	
	public void printWeights()
	{
		for (Perceptron p : successors)
		{
			System.out.println(this.name + " to " + p.getName() + " weight: " + weights.get(successors.indexOf(p)));
		}
	}
	
	public String getName()
	{
		return name;
	}
	
	public ArrayList<Perceptron> getSuccessors()
	{
		return successors;
	}
	
	public String formattedWeights()
	{
		return "implement this";
	}
	
	public double getWeight(Perceptron successor)
	{
		return weights.get(successors.indexOf(successor));
	}
	
	
	public void evaluate()
	{
		if (curVal + bias >= threshold) fire(); //sigmoid...
	}
	
	public void fire()
	{
		for (Perceptron s : successors)
		{ //multiply by curVal or 1?
			s.receive(weights.get(successors.indexOf(s)));
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
		threshold = Math.random();
	}
}