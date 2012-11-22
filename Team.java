// Version 0.3 of PennQBBot by Saajid Moyen, 11/22/2012

import java.util.HashSet;

public class Team
{

        //***************
        //*Variable List*
        //***************
        /* 
         * name = the tean's name.
         * guesser = the last person on the team to answer a question (correctly or incorrectly)
         * players = stores all of the players on the team.
         * score = the team's score.
         * guessed = true if the team has already answered the current question (reset every question).
         */

        private String name, guesser;
        private HashSet<Player> players;
        private int score;
        private boolean guessed;
        private int bonusScore;
        private int bonusesHeard;

        public Team()
        {
                name = "";
                guesser = "";
                players = new HashSet<Player>();
                score = 0;
                guessed = false;
                bonusScore = 0;
                bonusesHeard = 0;
        }

        public Team(String teamName)
        {
                name = teamName;
                guesser = "";
                players = new HashSet<Player>();
                score = 0;
                guessed = false;
                bonusScore = 0;
                bonusesHeard = 0;
        }

        public String getName()
        {
                return name;
        }

        public String getGuesser()
        {
                return guesser;
        }

        public HashSet<Player> getPlayers()
        {
                return players;
        }

        public int getScore()
        {
                return score;
        }

        public boolean hasGuessed()
        {
                return guessed;
        }

        public void setName(String newName)
        {
                name = newName;
        }

        public void setGuesser(String player)
        {
                guesser = player;
        }

        public void setScore(int newScore)
        {
                score = newScore;
        }

        public void addScore(int points)
        {
                score += points;
        }
        
        public void addBonusPoints(int points)
        {
                this.bonusScore += points;
        }
        public void increaseBonusesHeard()
        {
                this.bonusesHeard++;
        }
        public int getBonusPoints()
        {
                return this.bonusScore;
        }
        public int getBonusesHeard()
        {
                return this.bonusesHeard;
        }
        public void setBonusPoints(int points)
        {
                this.bonusScore = points;
        }
        public void setBonusesHeard(int value)
        {
                this.bonusesHeard = value;
        }

        // Adds a player to the team (or rather, to it's HashSet).
        public void addPlayer(Player player)
        {
                players.add(player);
        }

        // Records who guessed on the team.
        public void setGuessed(boolean hasGuessed, String player)
        {
                guessed =hasGuessed;
                guesser = player;
        }

        // Removes a player from the team.  Add error message later?
        public void removePlayer(Player player)
        {
                if(players.contains(player))
                        players.remove(player);
        }

        // Returns the number of players
        public int size()
        {
                return players.size();
        }

        // Clears the players and reset the score
        public void clear()
        {
                players.clear();
                score = 0;
                bonusesHeard = 0;
                bonusScore = 0;
        }

        // Checks to see if a certain player is on the team
        public boolean contains(Player player)
        {
                return players.contains(player);
        }

        // Returns a list of players
        public String toString()
        {
                return "Score: " + Integer.toString(score) + "; Players: " + players.toString();
        }

}
