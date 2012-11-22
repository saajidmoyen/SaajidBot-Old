// Version 0.2 of PennQBBot by Saajid Moyen, 11/22/2012

import java.io.*;

public class Player implements Comparable<Player>
{

        //***************
        //*Variable List*
        //***************
        /*name = the player's name.
        team = the player's team.
        score = the player's overall score.
        stat vars -->
        numguessed = total number of questions answered.
        numcorrect = total number of questions answered correctly.
        numgames = total number of games played.
        mvp = number of games where (s)he has won the mvp award.
        lvp = number of games where (s)he has won the lvp award.*/

        private String name, team;
        private int score, numguessed, numcorrect, numgames, numwon, curguessed, curcorrect, mvp, lvp, curnegs, numnegs;
        // Used in hashCode().  37 was chosen because I've seen it used as an example
        // in my CS70 class at Harvey Mudd.
        private static final int HASH_MULTIPLIER = 37;
        private static final int TUPTS = 10;    // Note: TUPTS and NEGPTS may be temporary.
        private static final int NEGPTS = 5;

        // Most of these functions are self explanatory.  getName() will get the name, etc.
        // Every variable does not have a get/set function here because it's not necessary for the bot (although I may add it later).

        public Player()
        {
                name = "";
                team = "";
                score = 0;
                numguessed = 0;
                numcorrect = 0;
                numgames = 0;
                numwon=0;
                mvp = 0;
                lvp = 0;
                numnegs = 0;
                curnegs = 0;
        }

        public Player(String sender)
        {
                name = sender;
                team = "";
                score = 0;
                setStats();
        }

        public Player(String sender, String t)
        {
                name = sender;
                team = t;
                score = 0;
                setStats();
        }

        public Player(String sender, String t, int scr)
        {
                name = sender;
                team = t;
                score = scr;
                setStats();
        }

        // all of the get statements
        public String getName()
        {
                return name;
        }

        public int getScore()
        {
                return score;
        }

        public String getTeam()
        {
                return team;
        }

        public int getGuessed()
        {
                return numguessed;
        }

        public int getCorrect()
        {
                return numcorrect;
        }

        public int getCurGuessed()
        {
                return curguessed;
        }

        public int getCurCorrect()
        {
                return curcorrect;
        }
        public int getGames()
        {
                return numgames;
        }

        public int getWins()
        {
                return numwon;
        }

        public int getMVP()
        {
                return mvp;
        }

        public int getLVP()
        {
                return lvp;
        }

        public int getCurNegs()
        {
                return curnegs;
        }

        public int getNegs()
        {
                return numnegs;
        }

        // for finding out the maximum number of points to give to a correct
        public static int getMaxCorrect()
        {
                return NEGPTS + TUPTS;
        }

        // The rest of the set statements.
        public void setTeam(String word)
        {
                team = word;
        }

        public void setName(String newName)
        {
                name = newName;
        }

        /*public void setGamemaster(boolean gm)
        {
                gamemaster = gm;
        }*/

        // Adds the number of points the player got in the round to their overall score
        public void addScore(int scr)
        {
                score+=scr;
        }

        public void guessed()
        {
                curguessed++;
        }

        public void correct()
        {
                curcorrect++;
        }

        public void incorrect()
        {
                curcorrect--;
        }

        public void neg()
        {
                curnegs++;
        }

        // use for !correct
        public void unneg()
        {
                curnegs--;
        }

        // Used when the player has earned an mvp
        public void mvp()
        {
                mvp++;
        }

        // Used when the player has earned an lvp
        public void lvp()
        {
                lvp++;
        }

        public void playedGame()
        {
                numgames++;
        }

        public void wonGame()
        {
                numwon++;
        }

        // updates the numguessed and numcorrect variable
        public void updateStats()
        {
                numguessed += curguessed;
                numcorrect += curcorrect;
                numnegs += curnegs;
                curguessed = 0;
                curcorrect = 0;
                curnegs = 0;
        }

        // Clears the player's current stats (called at the end of the game).
        public void clearStats()
        {
                curguessed = 0;
                curcorrect = 0;
                curnegs = 0;
        }

        // If the player exists, set up his stats
        private void setStats()
        {
                // Load the stats
                int counter = 0;
                int[] readvars = {0,0,0,0,0,0,0};
                try{
                        BufferedReader reader = new BufferedReader(new FileReader("stats\\" + name + ".txt"));
                        boolean eof = false;
                        while(!eof)
                        {
                                String line = reader.readLine();
                                if(line == null)
                                        eof = true;
                                else if(line != null && counter < readvars.length) // 4!  See the 4!  That's important!
                                {
                                        readvars[counter] = Integer.parseInt(line);
                                }
                                counter++;
                        }

                numguessed = readvars[0];
                numcorrect = readvars[1];
                numgames = readvars[2];
                numwon = readvars[3];
                mvp = readvars[4];
                lvp = readvars[5];
                numnegs = readvars[6];
                reader.close();
                }
                catch(IOException e){}
        }

        // Get stats about the player
        public String getStats()
        {
                double totalscore = (1.0*Player.TUPTS)*numcorrect - (1.0*Player.NEGPTS)*numnegs;
                // Split up the string in tbot with ;
                // Make sure the player has played a game; otherwise, we could get a division by zero error
                if(numgames != 0)
                        return name + ": Wins: " + numwon + ", Games played: " + numgames + ", Percent won: " +
                        Integer.toString((int) (numwon * 1.0 / numgames * 100)) + " % , MVPs: " + mvp + ", LVPs: " + lvp + " -- " +
                        "TUs earned: " + numcorrect + ", Negs: " + numnegs + ", TUs answered: " + numguessed + " (" +
                        Integer.toString((int) (numcorrect * 1.0 / numguessed * 100)) +"% correct), \"PPG\": " +
                        ((int) totalscore/numgames) + ", Total pts: " + ((int) totalscore);
                return name + " has no stats yet, since " + name + " has not completed any games.";
        }

        // Gets the stats for the current game
        public String getCurStats()
        {
                return name + ": TUs earned: " + curcorrect + ", Negs: " + curnegs + ", TUs answered: " +
                curguessed + ", Points: " + (Player.TUPTS * curcorrect - Player.NEGPTS * curnegs) + ".";
        }

        // Let case not matter
        public boolean equals(Object obj)
        {
                return getName().equalsIgnoreCase(((Player) obj).getName());
        }

        // this gets around HashSet's equals() problem
        // This uses Knuth's hash function.
        public int hashCode()
        {
                int hash = 0;
                for(int i = 0; i < name.length(); i++)
                        hash = HASH_MULTIPLIER * hash + name.charAt(i);
                return hash;
        }

        // Compare their score.  This is used for ranking.
        // Problem: if !a.equals(b), a.compareTo(b) can still be 0.
        public int compareTo(Player o)
        {
                return o.getScore() - score;
        }

        public String toString()
        {
                //return name + " is on team " + team + " and has " + Integer.toString(score) + " points";
                return name;
        }

}//end of class