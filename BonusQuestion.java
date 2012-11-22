// Version 0.3 of PennQBBot by Saajid Moyen, 11/22/2012

// bonus format:  use @@ to separate questions
// (scores,delimited,by,commas,optional)topic@@leadin@@Q1@@Q2@@Q3..
// since no topic should start with (), take out whatever is in the (), and parse them.
// If no () exists, assume all parts are worth 10 points

public class BonusQuestion
{
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