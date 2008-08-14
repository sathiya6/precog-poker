/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package precog;
import poker.engine.*;
import poker.players.*;

/**
 *
 * @author kevinl
 */
public class PrecogMain 
{
    public static void main(String... args)
    {
        Player a = new Precog("precog");
        Player b = new Stupid("bush");
        Player[] c = {a, b};
        Game g = new Game(c);
        g.begin();
        
        //assert false : "assertions on";
        
        /*Card a = new Card(Card.Suit.CLUBS, Card.TWO);
        Card b = new Card(Card.Suit.SPADES, Card.THREE);
        Card c = new Card(Card.Suit.HEARTS, Card.FOUR);
        Card d = new Card(Card.Suit.CLUBS, Card.FIVE);
        Card e = new Card(Card.Suit.CLUBS, Card.SIX);
        Card[] cards = {a,b,c,d,e};
        Hand z = new Hand(cards);
        System.out.println(Precog.suitlessHand(z.getBitCards()));*/
    }
}
