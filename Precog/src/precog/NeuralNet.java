/**
 * @author: Kevin Liu (kevin91liu@gmail.com)
 */

package precog;

import java.io.Serializable;


public class NeuralNet implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -2529332382699798332L;
	private Perceptron a1;
	private Perceptron a2;
	private Perceptron a3;
	private Perceptron a4;
	private Perceptron a5;
	private Perceptron a6;
	private Perceptron b1;
	private Perceptron b2;
	private Perceptron b3;
	private Perceptron b4;
	private Perceptron b5;
	private Perceptron b6;
	private Perceptron output;
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
		a1 = new Perceptron("a1-avg-perc"); a1.randomize();
		a2 = new Perceptron("a2-our-money-in"); a2.randomize();
		a3 = new Perceptron("a3-raise-amt"); a3.randomize();
		a4 = new Perceptron("a4-num-start-pl"); a4.randomize();
		a5 = new Perceptron("a5-num-left-pl"); a5.randomize();
		a6 = new Perceptron("a6-potsize"); a6.randomize();
		b1 = new Perceptron("b1"); b1.randomize();
		b2 = new Perceptron("b2"); b2.randomize();
		b3 = new Perceptron("b3"); b3.randomize();
		b4 = new Perceptron("b4"); b4.randomize();
		b5 = new Perceptron("b5"); b5.randomize();
		b6 = new Perceptron("b6"); b6.randomize();
		output = new Perceptron("output"); output.randomize();
		
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
	
	private void evaluate(Perceptron ... a)
	{
		for (Perceptron p : a) p.evaluate();
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