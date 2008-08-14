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
    }
}
