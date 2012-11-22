// Version 0.1 of PennQBBot by Saajid Moyen, 11/22/2012
//
/* possibilities:

-class extended from Question (bad idea, in my opinion)
-not a separate class: make the ArrayList hold a 3-Question array.
        Problems: might be prissy about variable bonuses, somewhat inflexible
-Make it have an Array/Linked List of Questions, with another variable to keep
 track of variable-points, if applicable.  If we need variable-point bonuses,
 maybe extend the question class to have a point-value variable?
-Make it a separate class entirely (probably not good either)

Needs:
something needs to keep track of what team (or player) answered the question.  If I have enough
motivation, code an ACF vs IHSA bonus feature.
Bot reads initial phrase; then starts asking parts, giving an allotted amount of time for the team
to buzz in and answer.  Player to answer will type in "buzz"; if that player is from the team that
won the bonus, ask it for an answer.
Repeat three times, then ask the next question.
*/

/* What we can do:
-Alter the Question class so that it has a points variable - default is 10 (put in settings?)
-Change tbot so that it gives out the question's point total
-Implement bonuses as a LinkedList of Questions
 --Con: larger memory footprint: you need a point value for every Question, even if it's not a bonus
Other way:
-In the bonus questions' file, put point totals if it's not the default value (10)
-Have a second LinkedList in BonusQuestions that keeps track of scores
*/

/*
 * Issues not handled in the current bonus format:
 * Bonus parts that ask for multiple answers.  For example:
 * [5, 5] Name the last two kings of France for five points each.
 * 
 * Bonuses in the format of 5 for one, 10 for two, 20 for three, 30 for all four.
 * 
 * Bonuses in the format of 30-20-10.
 * 
 * I suggest we just don't support these bonuses, at least for now.  They're not
 * very common in modern sets.
 * 
 * -Mike Bentley
 */

// bonus format:  use @@ to separate questions
// (scores,delimited,by,commas,optional)topic@@leadin@@Q1@@Q2@@Q3..
// since no topic should start with (), take out whatever is in the (), and parse them.
// If no () exists, assume all parts are worth 10 points

//import java.util.LinkedList;

public class BonusQuestion
{
        //private LinkedList<Question> bonuses;
        //private LinkedList<Integer> scores;
        private Question[] bonuses;
        private int[] scores;
        private int questionNum;
        private String leadin, topic;
        private static  final int NORMAL_BPTS = 10;
        private static int maxPoints;
        private String qSet;
        private boolean invalid; //Whether this is an invalid bonus

        public BonusQuestion()
        {
                //bonuses = new LinkedList<Question>();
                //scores = new LinkedList<Integer>();
                bonuses = new Question[0];
                scores = new int[0];
                leadin = "";
                topic = "";
                qSet = "";
                questionNum = 0;
                maxPoints = 0;
                invalid = true;
        }

        public BonusQuestion(String line)
        {
                //bonuses = new LinkedList<Question>();
                //scores = new LinkedList<Integer>();
                line.trim();
                boolean scoreSpecific = false;
                // Get scores, if they're there.  Scores start with a ( and end with a )
                if(line.charAt(0) == '(') // if it's there
                {
                        if(line.indexOf(")") > 0)
                        {
                                scoreSpecific = true;
                                String[] theScores = line.substring(1,line.indexOf(")")).split(",");    // we skip the [ which is at 0
                                scores = new int[theScores.length];
                                for(int i = 0; i < scores.length; i++)
                                        scores[i] = Integer.valueOf(theScores[i].trim());
                                line = line.substring(line.indexOf(")")+1,line.length());       // go one past ]; this shouldn't trigger any IndexOutOfBounds errors if the question is properly formatted
                        }
                        else
                        {
                                System.out.println("Formatting error: no ] to match [.");
                                return;
                        }
                }

                // Get the questions.  Split them up by @@
                String[] theQuestions = line.split("@@");
                if (theQuestions.length > 1)
                {
                        topic = theQuestions[0];                // first part is the topic
                        leadin = theQuestions[1];               // second part is the description
                        bonuses = new Question[theQuestions.length-2];
                        for(int i = 0; i < bonuses.length; i++)
                                bonuses[i] = new Question(theQuestions[i+2]);
                        if(!scoreSpecific)      // if scores aren't manually defined, set all scores to NORMAL_BPTS points
                        {
                                scores = new int[bonuses.length];
                                for(int i = 0; i < scores.length; i++)
                                        scores[i] = NORMAL_BPTS;
                        }
                        invalid = false;
                }
                qSet = "";
                updateMaxPoints();
        }
        
        public boolean getInvalid()
        {
                return invalid;
        }

        public String getLeadin()
        {
                return leadin;
        }

        public String getTopic()
        {
                return topic;
        }

        public Question[] getBonuses()
        {
                return bonuses;
        }
        public Question getBonusPart(int i)
        {
                if (i < bonuses.length) 
                        return bonuses[i];
                else
                        return null;
        }
        public int getPoints(int i)
        {
                if (i >= scores.length || i < 0)
                        return 0;
                else
                        return scores[i];
        }

        public int[] getScores()
        {
                return scores;
        }

        public int getQuestionNum()
        {
                return questionNum;
        }

        public void setQuestionNum(int num)
        {
                questionNum = num;
        }
        public void setQuestionSet( String questionSet)
        {
                 qSet = questionSet;
        }
        public String getQuestionSet() 
        {
                return qSet;
        }
        public static int getMaxPoints()
        {
                return maxPoints;
        }
        
        
        private void updateMaxPoints()
        {
                // Find the total score possible, see if it's greater than what we have now
                int total = 0;
                for(int i : scores)
                        total += i;
                if(total > maxPoints)
                        maxPoints = total;
        }
        
}