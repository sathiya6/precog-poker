/**
 * @author: Kevin Liu (kevin91liu@gmail.com)
 */

package precog;

import java.io.Serializable;
import java.util.LinkedList;


public class NeuralNet implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -2529332382699798332L;
    private static final double LRATE = 5.; //learning rate
    private static final double MAX_MSE = 0.01;
	private Neuron a1;
	private Neuron a2;
	private Neuron a3;
	private Neuron a4;
	private Neuron a5;
	private Neuron a6;
	private Neuron b1;
	private Neuron b2;
	private Neuron b3;
	private Neuron b4;
	private Neuron b5;
	private Neuron b6;
	private Neuron output;
	//between 0 and 1 as a percentage of current amount chips owned
	
	public NeuralNet()
	{
		/* 
		* 1. avg perc
	    * 2. our money amount in
	    * 3. raise amount (money we'd have to put in)
	    * 4. number of starting players in round
	    * 5. number of players left in round
	    * 6. potsize
	    */
		a1 = new Neuron("a1-avg-perc", 1); a1.init_input_node();
		a2 = new Neuron("a2-our-money-in", 2); a2.init_input_node();
		a3 = new Neuron("a3-raise-amt", 3); a3.init_input_node();
		a4 = new Neuron("a4-num-start-pl", 4); a4.init_input_node();
		a5 = new Neuron("a5-num-left-pl", 5); a5.init_input_node();
		a6 = new Neuron("a6-potsize", 6); a6.init_input_node();
		b1 = new Neuron("b1", 1); b1.init_hidden_node();
		b2 = new Neuron("b2", 2); b2.init_hidden_node();
		b3 = new Neuron("b3", 3); b3.init_hidden_node();
		b4 = new Neuron("b4", 4); b4.init_hidden_node();
		b5 = new Neuron("b5", 5); b5.init_hidden_node();
		b6 = new Neuron("b6", 6); b6.init_hidden_node();
		output = new Neuron("output", 1337); output.init_output_node();
		
		a1.addChild(b1, Math.random());
		a1.addChild(b2, Math.random());
		a1.addChild(b3, Math.random());
		a1.addChild(b4, Math.random());
		a1.addChild(b5, Math.random());
		a1.addChild(b6, Math.random());
		
		a2.addChild(b1, Math.random());
		a2.addChild(b2, Math.random());
		a2.addChild(b3, Math.random());
		a2.addChild(b4, Math.random());
		a2.addChild(b5, Math.random());
		a2.addChild(b6, Math.random());
		
		a3.addChild(b1, Math.random());
		a3.addChild(b2, Math.random());
		a3.addChild(b3, Math.random());
		a3.addChild(b4, Math.random());
		a3.addChild(b5, Math.random());
		a3.addChild(b6, Math.random());
		
		a4.addChild(b1, Math.random());
		a4.addChild(b2, Math.random());
		a4.addChild(b3, Math.random());
		a4.addChild(b4, Math.random());
		a4.addChild(b5, Math.random());
		a4.addChild(b6, Math.random());
		
		a5.addChild(b1, Math.random());
		a5.addChild(b2, Math.random());
		a5.addChild(b3, Math.random());
		a5.addChild(b4, Math.random());
		a5.addChild(b5, Math.random());
		a5.addChild(b6, Math.random());
		
		a6.addChild(b1, Math.random());
		a6.addChild(b2, Math.random());
		a6.addChild(b3, Math.random());
		a6.addChild(b4, Math.random());
		a6.addChild(b5, Math.random());
		a6.addChild(b6, Math.random());
		
		b1.addChild(output, Math.random());
		b2.addChild(output, Math.random());
		b3.addChild(output, Math.random());
		b4.addChild(output, Math.random());
		b5.addChild(output, Math.random());
		b6.addChild(output, Math.random());
	}
	
	/** 1. avg perc
    * 2. our money amount in
    * 3. raise amount (money we'd have to put in)
    * 4. number of starting players in round
    * 5. number of players left in round
    * 6. potsize
    * 
    * should return a double between -1 and 1. proportion of chips left to raise.
    * 0 means check/call, a negative number should mean to fold
    */
	public double execute(double avg_perc, double chips_in, double raise_amt, 
			int starting_players, int cur_players, double potsize)
	{
		resetAll();
		a1.receive(avg_perc); a2.receive(chips_in); a3.receive(raise_amt);
		a4.receive((double)starting_players); a5.receive((double)cur_players);
		a6.receive(potsize);
		evaluate(a1, a2, a3, a4, a5, a6);
		evaluate(b1, b2, b3, b4, b5, b6);
		return output.outputSmooth();
		//return 0.;
	}
	
    /**
     * maxMSE = max allowable mean squared error.
     */
	 protected void adjustNN(double actual_output, double desired_output)
	 {
	    double outputWeightDelta = desired_output - actual_output;
	    double[] hiddenWeightDelta = new double[6]; //6 hidden nodes.
	   	double MSE = outputWeightDelta * outputWeightDelta;
	    outputWeightDelta *= actual_output * (1 - actual_output);
	    	
	    if (MSE < MAX_MSE) 
	    {
	    	System.out.println("MSE < MAX_MSE. return early.");
	    	return;
	    }
	    LinkedList<Neuron> hiddenLayer = getHiddenLayer();
	    for (Neuron n : hiddenLayer)
	    {
	    	double sum = outputWeightDelta * n.getWeight(output);
	    	hiddenWeightDelta[n.getNumber() - 1] = sum * n.getLastOutput() * (1. - n.getLastOutput());
	    }
	    for (Neuron n : hiddenLayer)
	    {
	    	System.out.println("adjustNN: loop x: changing " + (LRATE * outputWeightDelta * n.getLastOutput()));
	    	n.setWeight(output, n.getWeight(output) + (LRATE * outputWeightDelta * n.getLastOutput()));
	    	for (Neuron i : getInputLayer())
	    	{
	    		System.out.println("adjustNN: loop y: changing " + (LRATE * hiddenWeightDelta[n.getNumber() - 1] * i.getLastOutput()));
	    		i.setWeight(n, i.getWeight(n) + (LRATE * hiddenWeightDelta[n.getNumber() - 1] * i.getLastOutput()));
	    	}
	    }
	    	
	 }
	
	private LinkedList<Neuron> getInputLayer()
	{
			LinkedList<Neuron> ret = new LinkedList<Neuron>();
			ret.add(a1); ret.add(a2); ret.add(a3);
			ret.add(a4); ret.add(a5); ret.add(a6);
			return ret;
	}
	
	private LinkedList<Neuron> getHiddenLayer()
	{
		LinkedList<Neuron> ret = new LinkedList<Neuron>();
		ret.add(b1); ret.add(b2); ret.add(b3);
		ret.add(b4); ret.add(b5); ret.add(b6);
		return ret;
	}
	
	private void evaluate(Neuron ... a)
	{
		for (Neuron p : a) p.evaluate();
	}
	
	//makes all curVals 0
	private void resetAll()
	{
		a1.reset(); a2.reset();
		a3.reset(); a4.reset();
		a5.reset(); a6.reset();
		b1.reset(); b2.reset();
		b3.reset(); b4.reset();
		b5.reset(); b6.reset(); 
		output.reset();
	}
	
	public void printWeights()
	{
		a1.printWeights(); a2.printWeights();
		a3.printWeights(); a4.printWeights();
		a5.printWeights(); a6.printWeights();
		
		b1.printWeights(); b2.printWeights();
		b3.printWeights(); b4.printWeights();
		b5.printWeights(); b6.printWeights();
	}
	
	
}