package Poker;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class HumanPlayer extends JPanel 
	implements Player, MouseListener, KeyListener
{
    private static final long serialVersionUID = 1L;
    JFrame frame;
    PlayerStats[] stats;
    Card[] cards;
    boolean[] discard;
    String[] bids;
    int callBid;
    int clickBid;
    int pot;
    Card[][] allCards;
    int ourIndex;
    int winnerIndex;
    double rotate;
    Color felt = new Color(0x00, 0x99, 0x33);
    
    public static final int INTRO = 0;
    public static final int DEAL = 1;
    public static final int BID = 2;
    public static final int REVEAL = 3;
    public static final int PAYOUT = 4;
    public static final int DRAW = 5;
    int state = INTRO;
    
    HumanPlayer()
    {   
        frame = new JFrame("Poker");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.getContentPane().add(this);
        frame.setVisible(true);

        addMouseListener(this);
        addKeyListener(this);
    }
    
    public String toString()
    {
        return "Human";
    }

    
    private Rectangle bounds;
    private int x0, y0, r;
    
    @Override
    public void paint(Graphics g)
    {
        bounds = this.getBounds();
        g.setColor(felt);
        g.fillRect(0, 0, bounds.width, bounds.height);
        
        x0 = bounds.width / 2;
        y0 = bounds.height / 2;
        r = Math.min(x0, y0) * 3 / 4;
        
        g.setColor(Color.BLACK);
        g.drawOval(x0 - r * 4 / 3, y0 - r * 4 / 3, r * 8 / 3, r * 8 / 3);
        
        switch(state)
        {
            case INTRO:
                paintIntro(g);
                return;
            case DEAL:
                paintDeal(g);
                return;
            case BID:
                paintBid(g);
                return;
            case REVEAL:
                paintReveal(g);
                return;
            case DRAW:
                paintDraw(g);
                return;
        }
    }
    
    private void paintIntro(Graphics g)
    {
        if (stats == null)
            return;
        g.setColor(Color.WHITE);
        for (int i = 0; i < stats.length; i++)
        {
            double angle = (i * Math.PI * 2) / stats.length + rotate;
            int x = x0 + (int)(Math.cos(angle) * r);
            int y = y0 + (int)(Math.sin(angle) * r);
            String str = stats[i] + " (" + stats[i].chips + ")";
            g.drawString(str, x - str.length() * 4, y);
        }
        
        if (stats.length == 1)
        {
            g.setFont(new Font("Cambria", 0, 48));
            g.setColor(Color.YELLOW);
            g.drawString("WINNER!", x0 - 24, y0);
        }
    }

    private void paintDeal(Graphics g)
    {
        paintIntro(g);
        for (int i = 0; i < cards.length; i++)
        {
            Point pt = cardPos(ourIndex, i);
            paintCard(cards[i], g, pt.x, pt.y, false);
        }
    }

    private void paintBid(Graphics g)
    {
        paintDeal(g);
        
        g.setColor(Color.WHITE);
        for (int i = 0; i < stats.length; i++)
        {
            if (bids[i] == null)
                continue;
            double angle = (i * Math.PI * 2) / stats.length + rotate;
            int x = x0 + (int)(Math.cos(angle) * r / 3);
            int y = y0 + (int)(Math.sin(angle) * r / 3);
            g.drawString(bids[i], x - bids[i].length() * 4, y);
        }
        
        // Create bidding buttons in the upper-left corner
        g.setColor(Color.LIGHT_GRAY);
        g.fill3DRect(2, 2, 100, 36, true/*raised*/);
        g.fill3DRect(2, 42, 100, 36, true/*raised*/);
        g.fill3DRect(2, 82, 100, 36, true/*raised*/);
        g.fill3DRect(2, 122, 100, 36, true/*raised*/);
        g.fill3DRect(2, 162, 100, 36, true/*raised*/);
        g.setColor(Color.BLACK);
        if (callBid == 0)
            g.drawString("Check", 20, 25);
        else
            g.drawString("Call (" + callBid + ")", 20, 25);
        g.drawString("Raise 1 (" + (callBid + 1) + ")", 20, 65);
        g.drawString("Raise 2 (" + (callBid + 2) + ")", 20, 105);
        g.drawString("Raise 3 (" + (callBid + 3) + ")", 20, 145);
        g.drawString("Fold", 20, 185);
    }

    private void paintReveal(Graphics g)
    {
        paintIntro(g);
        for (int iPlayer = 0; iPlayer < stats.length; iPlayer++)
        {
            if (allCards[iPlayer] == null)
                continue;
            for (int i = 0; i < allCards[iPlayer].length; i++)
            {
                Point pt = cardPos(iPlayer, i);
                paintCard(allCards[iPlayer][i], g, pt.x, pt.y, iPlayer == winnerIndex);
            }
        }
        g.setColor(Color.BLACK);
        g.drawString("Click anywhere to continue", x0 - 4 * 13, y0 - 14);
    }

    private void paintDraw(Graphics g)
    {
        paintIntro(g);
        for (int i = 0; i < cards.length; i++)
        {
            Point pt = cardPos(ourIndex, i);
            paintCard(cards[i], g, pt.x, pt.y, discard[i]);
        }

        // Create done button
        g.setColor(Color.LIGHT_GRAY);
        g.fill3DRect(2, 2, 100, 36, true/*raised*/);
        g.setColor(Color.BLACK);
        g.drawString("Done", 20, 25);

        g.setColor(Color.BLACK);
        g.drawString("Click on cards to discard", x0 - 4 * 13, y0 - 14);
    }
    
    
    private Point cardPos(int iPlayer, int index)
    {
        double angle = (iPlayer * Math.PI * 2) / stats.length + rotate;
        int x = x0 + (int)(Math.cos(angle) * r);
        int y = y0 + (int)(Math.sin(angle) * r);

        int yCards = y + r / 7;
        int dxCard = r / 4;
        int xCard0 = x - dxCard * (cards.length - 1) / 2;
        
        return new Point(xCard0 + dxCard * index, yCards);
    }
    
    private Rectangle cardRect(int iPlayer, int index, boolean big)
    {
    	Point center = cardPos(iPlayer, index);

        int dx = r / 10;
        int dy = r / 8;
        if (big)
        {
            dx = dx * 4 / 3;
            dy = dy * 4 / 3;
        }

    	return new Rectangle(center.x - dx, center.y - dy, dx * 2, dy * 2);
    }
    
    private int ptInCard(MouseEvent e)
    {
    	Point pt = new Point(e.getX(), e.getY());
    	for (int i = 0; i < cards.length; i++)
    	{
    		if (cardRect(ourIndex, i, discard[i]).contains(pt))
    			return i;
    	}
    	return -1;
    }
    
    static Color[] suitColors = { Color.BLUE, new Color(0x66,0x66,0x00), Color.RED, Color.BLACK };
    private void paintCard(Card c, Graphics g, int x, int y, boolean highlight)
    {
        int dx = r / 10;
        int dy = r / 8;
        if (highlight)
        {
            dx = dx * 4 / 3;
            dy = dy * 4 / 3;
        }
        g.setColor(Color.WHITE);
        g.fillRect(x - dx, y - dy, dx * 2, dy * 2);
        g.setColor(suitColors[c.getSuit()]);
        for (int i = 0; i < (highlight ? 5 : 2); i++)
            g.drawRect(x - dx - i, y - dy - i, (dx + i) * 2, (dy + i) * 2);
        
        g.drawString(""+Card.RANK_CHARS.charAt(c.getRank()), x - 3, y + 4);
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {}

    @Override
    public void mouseExited(MouseEvent e)
    {}

    @Override
    public void mousePressed(MouseEvent e)
    {
        int x = e.getX();
        int y = e.getY();
        if (state == BID)
        {
            if (x >= 100)
                return;
            if (y >= 200)
                return;
            if (y < 40)
                clickBid = callBid;
            else if (y < 80)
                clickBid = callBid + 1;
            else if (y < 120)
                clickBid = callBid + 2;
            else if (y < 160)
                clickBid = callBid + 3;
            else
                clickBid = -1; // fold
        }
        else if (state == DRAW)
        {
            if (x > 100 || y > 40)
            {
	        	int i = ptInCard(e);
	        	if (i >= 0)
	        	{
	        		discard[i] = !discard[i];
	        		repaint();
	        	}
	        	return;
            }
        }
        
        synchronized(this)
        {
            this.notify();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {}

	@Override
	public void keyPressed(KeyEvent e) 
	{
        if (state == BID)
        {
            if (e.getKeyCode() == KeyEvent.VK_C)
                clickBid = callBid;
            else if (e.getKeyCode() == KeyEvent.VK_1)
                clickBid = callBid + 1;
            else if (e.getKeyCode() == KeyEvent.VK_2)
                clickBid = callBid + 2;
            else if (e.getKeyCode() == KeyEvent.VK_3)
                clickBid = callBid + 3;
            else if (e.getKeyCode() == KeyEvent.VK_F)
                clickBid = -1; // fold
        }
        else if (state == DRAW)
        {
        	char ch = e.getKeyChar();
        	if (ch >= '1' && ch <= '0' + discard.length)
        	{
        		int i = ch - '1';
        		discard[i] = !discard[i];
        		repaint();
        	}
        	if (e.getKeyCode() != KeyEvent.VK_ENTER
        			&& e.getKeyCode() != KeyEvent.VK_SPACE)
        	{
        		return;  // only release the game on enter or space
        	}
        }
        
        synchronized(this)
        {
            this.notify();
        }
	}

	@Override
	public void keyReleased(KeyEvent e) 
	{}

	@Override
	public void keyTyped(KeyEvent e) 
	{}

	@Override
    public void deal(Card[] cards)
    {
        this.cards = cards;
        state = DEAL;
        repaint();
    }

	void waitForHuman()
	{
        try
        {
            synchronized(this)
            {
                this.wait();
            }
        }
        catch (InterruptedException e)
        {
            
        }
	}
	
    @Override
    public Card[] draw(PlayerStats[] stats)
    {
    	discard = new boolean[cards.length];  // initially all false
        state = DRAW;
        repaint();
        
        waitForHuman();

        Card[] draw = new Card[cards.length];
        for (int i = 0; i < discard.length; i++)
        {
        	if (discard[i])
        		draw[i] = cards[i];
        }
        return draw;
    }

    @Override
    public void initHand(PlayerStats[] stats, int yourIndex)
    {
        this.stats = stats;
        this.ourIndex = yourIndex;
        this.rotate = (Math.PI / 2) - yourIndex * (2 * Math.PI) / stats.length;
        if (stats.length == 5)
            rotate -= Math.PI / 4;
        if (stats.length == 7)
            rotate -= Math.PI / 4;
        state = INTRO;
        repaint();
    }

    @Override
    public int getBid(PlayerStats[] stats, int callBid)
    {
        this.callBid = callBid;
        this.pot = 0;
        bids = new String[stats.length];
        for (int i = 0; i < bids.length; i++)
        {
            int lastBid = stats[i].getBid(-1);
            if (stats[i].allIn)
                bids[i] = "All In";
            else if (stats[i].folded || lastBid == -1)
                bids[i] = "Folded";
            else
            {
                if (lastBid < -1)
                    bids[i] = "";
                else if (lastBid == 0)
                    bids[i] = "Check";
                else
                    bids[i] = "" + lastBid;
            }
            pot += stats[i].totalBid;
        }
        bids[ourIndex] = "<choose a bid>";
        
        state = BID;
        clickBid = 0;
        repaint();
        
        waitForHuman();
        return clickBid;
    }

    @Override
    public void outcome(Card[][] allCards, PlayerStats[] stats, int winnerIndex)
    {
        this.allCards = allCards;
        this.bids = new String[stats.length];
        this.winnerIndex = winnerIndex;
        state = REVEAL;
        repaint();

        try
        {
            synchronized(this)
            {
                this.wait();
            }
        }
        catch (InterruptedException e)
        {
            
        }
    }
}
