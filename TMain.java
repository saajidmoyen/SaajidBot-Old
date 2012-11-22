// Version 0.1 of PennQBBot by Saajid Moyen, 11/22/2012
//
// #scobowl bot © 2006 by Alejandro López-Lago
// Bot started Feb. 26 2006
/*
Class descriptions:

tmain: the driver class.  Creates the bot and connects it to a server
tbot: the main code of the bot.  It runs the games, asking questions and creating players/teams.
Player: It stores information about a player in the game.
Team: It stores information about the two teams in the game.
Question: Stores a question from the question set.
*/
//import org.jibble.pircbot.*;

public class TMain{

public static void main(String[] args) throws Exception
{

                //***************
                //*Variable List*
                //***************
                //bot = the bot.  Self-explanatory.
                //final String SERVER = "irc.slashnet.org";

                TBot bot = new TBot();

        // Enable debugging output.
        //bot.setVerbose(true);

}//end of main
}//end of class