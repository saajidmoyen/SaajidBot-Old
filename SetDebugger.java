// Version 0.1 of PennQBBot by Saajid Moyen, 11/22/2012
//
// SetDebugger created by Alejandro López-Lago 2006
import java.util.ArrayList;
import java.io.*;
import java.util.regex.*;

public class SetDebugger
{
public static void main(String[] args)
{
        System.out.println("Checking TUs:");

        ArrayList<Object> questions = new ArrayList<Object>();
        // qSetFile can be any file, but since the bot usually uses set.txt, it is set to set.txt as the default.
        //String qSetFile = "rset.txt";
        String qSetFile = args[0];
        String bqSetFile = "";
        if(args.length > 1)
                bqSetFile = args[1];

        // This try-catch block
        try{
                BufferedReader reader = new BufferedReader(new FileReader(qSetFile));
                boolean eof = false;
                int counter = 0;
                String line;
                while(!eof)
                {
                        // Reads a line from the file.  If the line is null (and therefore at the end of the file), it exits the loop.
                                // Otherwise, it checks the question for possible errors.
                        line = reader.readLine();
                        if(line == null)
                                eof = true;

                        // Checks for errors.
                        else
                        {
                                // This part goes through the line making sure that it's properfly formatted (topic;;question;;answer).
                                // If it doesn't have the two semicolons, it prints out the question's ID and the first thirty characters of the line
                                int curpos = 0;
                                int c = 0;
                                while(line.indexOf(";;",curpos) > 0)
                                {
                                        curpos = line.indexOf(";;",curpos) + 1;
                                        c++;
                                }
                                if(c > 3)
                                        System.out.println(line.substring(0,30));

                                // This creates a question, and then checks to make sure that certain topics have been renamed and that the question isn't missing a period
                                        // or that the answer doesn't end in a parentheses.
                                Question temp;
                                try{
                                        temp = new Question(line);
                                }
                                catch(Exception e){
                                        System.out.println("Improperly formatted question at: " + Integer.toString(counter));
                                        break;
                                }
                                temp.setQuestionNum(counter);
                                questions.add(temp);
                                // check to see if "British/American" Lit is a category
                                //if(temp.getTopic().equalsIgnoreCase("American Literature") || temp.getTopic().equalsIgnoreCase("British Literature") || temp.getTopic().substring(0,2).equalsIgnoreCase("US"))
                                //      System.out.println(line.substring(0,30));
                                String q = temp.getQuestion();
                                if(!q.substring(q.length()-1,q.length()).equals(".") && !q.substring(q.length()-1,q.length()).equals("?"))
                                        System.out.println("Missing period; " + line.substring(0,30) + "\t" + counter);
                                //if(line.substring(line.length()-1,line.length()).equals(")"))
                                //      System.out.println(") " + counter + line.substring(0,30));
                                if(line.indexOf("") != -1)
                                        System.out.println("" + counter + line.substring(0,30));
                                if(line.charAt(line.length()-1) == ' ')
                                        System.out.println("space error: " + counter + " " + line.substring(0,30));
                                if(line.charAt(line.length()-1) == '.')
                                        System.out.println("Period at: " + counter + " " + line.substring(0,30));
                                if(temp.getAnswers().toString().indexOf("\\s*or\\s*") >= 0 || temp.getAnswers().toString().indexOf("(or)") >= 0)
                                        System.out.println("Possible 'or switched with |' mistake at: " + counter + " " + temp.getAnswersString());
                                if(Pattern.matches(".+? (I{1,3}V{0,1}|I{1,3}X{0,3})[^a-zA-z].*", temp.getAnswersString()) && Pattern.matches(temp.getPrompts().pattern(),""))
                                        System.out.println("Possible royalty question without prompt at " + counter + " " + temp.getAnswers().toString().substring(0,Math.min(30,temp.getAnswers().toString().length())));
                                //System.out.println(counter);
                                counter++;
                        }
                }
        }
        catch(IOException e)
        {
                // If the catch block is triggered, it means that the set we tried to open does not exist.
                System.out.println(qSetFile + " does not exist.");
        }

        // Prints out the size of the question set.
        System.out.println("TUs: " + questions.size());
        System.out.println("Checking Bonuses:");
        questions.clear();

        if(!bqSetFile.equals(""))       // if we can check a bonus file
        {
                try{
                        BufferedReader reader = new BufferedReader(new FileReader(bqSetFile));
                        boolean eof = false;
                        int counter = 0;
                        String line;
                        while(!eof)
                        {
                                // Reads a line from the file.  If the line is null (and therefore at the end of the file), it exits the loop.
                                        // Otherwise, it checks the question for possible errors.
                                line = reader.readLine();
                                if(line == null)
                                        eof = true;

                                // Checks for errors.
                                else
                                {
                                        // This part goes through the line making sure that it's properfly formatted (topic;;question;;answer).
                                        // If it doesn't have the two semicolons, it prints out the question's ID and the first thirty characters of the line
                                        String[] qparts = line.split("@@");
                                        if(qparts.length < 3)   // must be at least 1 part of the bonus question
                                                System.out.println(counter + ": not enough parts to make up a bonus");
                                        // This creates a question, and then checks to make sure that certain topics have been renamed and that the question isn't missing a period
                                                // or that the answer doesn't end in a parentheses.
                                        BonusQuestion btemp;
                                        try{
                                                btemp = new BonusQuestion(line);
                                        }
                                        catch(Exception e){
                                                System.out.println("Improperly formatted bonus question at: " + Integer.toString(counter));
                                                break;
                                        }
                                        btemp.setQuestionNum(counter);
                                        questions.add(btemp);

                                        Question[] bQuestions = btemp.getBonuses();

                                        for(int i = 0; i < bQuestions.length; i++)      // check each of the parts in the bonus
                                        {
                                                String q = bQuestions[i].getQuestion();
                                                // check to see if "British/American" Lit is a category
                                                if(btemp.getTopic().equalsIgnoreCase("American Literature") || btemp.getTopic().equalsIgnoreCase("British Literature") || btemp.getTopic().substring(0,2).equalsIgnoreCase("US"))
                                                        System.out.println(line.substring(0,30));
                                                //String q = temp.getQuestion();
                                                if(!q.substring(q.length()-1,q.length()).equals(".") && !q.substring(q.length()-1,q.length()).equals("?"))
                                                        System.out.println("Missing period; " + line.substring(0,30) + "\t" + counter);
                                                //if(line.substring(line.length()-1,line.length()).equals(")"))
                                                //      System.out.println(") " + counter + line.substring(0,30));
                                                if(line.indexOf("") != -1)
                                                        System.out.println("" + counter + line.substring(0,30));
                                                if(line.charAt(line.length()-1) == ' ')
                                                        System.out.println("space error: " + counter + " " + line.substring(0,30));
                                                if(line.charAt(line.length()-1) == '.')
                                                        System.out.println("Period at: " + counter + " " + line.substring(0,30));
                                        //System.out.println(counter);
                                        }
                                        counter++;
                                }
                        }
                }
                catch(IOException e)
                {
                        // If the catch block is triggered, it means that the set we tried to open does not exist.
                        System.out.println(bqSetFile + " does not exist.");
                        System.out.println("Exception message: " + e.toString());
                }
        }

        System.out.println("Bonuses: " + questions.size());

}
}