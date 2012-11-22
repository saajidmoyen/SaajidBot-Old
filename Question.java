// Version 0.1 of PennQBBot by Saajid Moyen, 11/22/2012
//
// Created by Alejandro López-Lago 2006

import java.util.regex.*;

public class Question
{

        //***************
        //*Variable List*
        //***************
        /* question = the actual question itself.
        topic = The category of the question
        questionNum = The question's ID
        answers = A regular expression of possible answers.*/

        String question, topic;
        int questionNum;
        Pattern answers, prompts;
        String qSet;
        private boolean invalid; //Whether this is an invalid question or not

        public Question()
        {
                question = "";
                topic = "";
                questionNum = 0;
                answers = Pattern.compile("");
                prompts = Pattern.compile("");
                qSet = "";
                invalid = true;
        }

        // Questions set like this: Category;;Question;;Answer(s)(;;Promptable Answer(s))
        public Question(String line)
        {
                 //Someone tried to load bonus sets as tossups
                if (line.indexOf("@@") != -1)
                        invalid = true;
                else
                        invalid = false;

                // split up the line
                // remember, the line format is Category;;Question;;Answer(s)(;;Promptable Answer(s))
                String[] parts = line.split(";;");
                topic = parts[0];
                question = parts[1];

                // correct some formatting for the answer; order is important!
                for(int i = 2; i < parts.length; i++)
                {
                        parts[i] = parts[i].replace("-","(-) ");
                        parts[i] = parts[i].replace("", "'");
                        parts[i] = parts[i].replace("'", "(') ");
                        parts[i] = parts[i].replace(")",")??");         // makes items in parantheses optional
                        parts[i] = parts[i].replace(")??{","){");       // makes items that were in parantheses followed by { mandatory
                        parts[i] = parts[i].replace(" ","\\s*");
                        parts[i] = parts[i].replace(".","\\x2E");       // Makes matching decimals work
                }

                answers = Pattern.compile(parts[2], Pattern.CASE_INSENSITIVE);
                // check to see if there's a promptable part
                if(parts.length > 3)
                        prompts = Pattern.compile(parts[3], Pattern.CASE_INSENSITIVE);
                else
                        prompts = Pattern.compile("");
                questionNum = 0;
                qSet = "";
        }

        public boolean getInvalid()
        {
                return invalid;
        }

        // Check to see if the answer is in the regular expression
        public boolean isCorrect(String answer)
        {
                return answers.matcher(answer).matches();
        }

        public boolean isPromptable(String answer)
        {
                return prompts.matcher(answer).matches();
        }

        // all of the get/set questions
        public String getQuestion()
        {
                return question;
        }

        public String getTopic()
        {
                return topic;
        }

        public Pattern getPrompts()
        {
                return prompts;
        }

        //** STANDARD ANSWER FORMAT:
        // (first)*\\s*(names)*\\s*Necessary\\s*(answer)*"
        public Pattern getAnswers()
        {
                return answers;
        }

        // get rid of the parts of the regular expression you do not wish to display (like *\\s*).
        public String getAnswersString()
        {
                String ans = answers.toString();
                if(prompts.toString() != "")
                        ans += " (prompt on: " + prompts.toString() + ")";
                //get rid of the \\s*, ??, |, and \\x2E
                // basically, this replaces the tokens with a more reader-friendly version of the token
                String[] tokens = {"\\s*","??","|","\\x2E","{1}?"};
                String[] replace ={" ",""," or ",".","+"};
                for(int i = 0; i < tokens.length; i++)
                        ans = ans.replace(tokens[i],replace[i]);

                return ans;
        }

        public int getQuestionNum()
        {
                return questionNum;
        }

        public String getQuestionSet()
        {
                return qSet;
        }

        public void setQuestionSet( String questionSet)
        {
                 qSet = questionSet;
         }

        public void setQuestionNum(int id)
        {
                questionNum = id;
        }

        public String toString()
        {
                return "Category: " + topic + ". " + question;
        }

        public String getFormat()
        {
                return topic + ";;" + question + ";;" + answers.toString() + ";;" + prompts.toString();
        }


}
