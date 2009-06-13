package Poker;


public class Test
{

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        SimplePoker table = new FiveCardDraw();
        table.addPlayer(new HumanPlayer(), 10);
        table.addPlayer(new Randall(), 10);
        table.addPlayer(new OpenBook(), 10);
        
        int GAMES = 500;
        
        for (int i = 0; i < GAMES && table.countPlayers() > 1; i++)
        {
            table.playHand();
            System.out.println("\nAfter " + (i+1) + " hands:");
            System.out.println(table);
            System.out.println("");
        }
        if (table.countPlayers() == 1)
            table.endGame();
        else
            System.out.println("Tournament ends without a winner");
        
//        Deck deck = new Deck();
//        deck.shuffle();
//        Card[] cards = new Card[7];
//        for (int i = 0; i < cards.length; i++)
//        {
//            cards[i] = deck.dealOne();
//        }
//        Hand hand = new Hand(cards);
//        System.out.println("Hand #1:");
//        System.out.println(hand);
//        hand.evaluateAndSortHand();
//        System.out.println(hand);
//
//        for (int i = 0; i < cards.length; i++)
//        {
//            cards[i] = deck.dealOne();
//        }
//        Hand hand2 = new Hand(cards);
//        hand2 = new Hand("5H 2D 3S 4C aH 3H");
//        System.out.println("Hand #2:");
//        System.out.println(hand2);
//        hand2.evaluateAndSortHand();
//        System.out.println(hand2);
//        
//        int d = hand.compareTo(hand2);
//        if (d < 0)
//            System.out.println("Hand #1 loses");
//        else if (d > 0)
//            System.out.println("Hand #1 wins");
//        else
//            System.out.println("Hands tie");
    }

}
