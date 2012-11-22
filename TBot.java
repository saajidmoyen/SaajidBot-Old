// Version 0.2 of PennQBBot by Saajid Moyen, 11/22/2012

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.jibble.pircbot.*;

public class TBot extends PircBot
{
        //-------------------------DECLARE VARIABLES-----------------------------------------//
        //booleans
        private boolean isPlaying; //keeps track of whether the channel is in "playing mode"
        private boolean isJoining; //keeps track of whether the players are currently joining the game
        private boolean questionRight; //keeps track of thether the last question was correctly answered
        private boolean negs; //determines if negs are in play
        private boolean finishedReading; //notes if the bot has "finished reading" it's question.
        private boolean showCategory; //tells bot whether to show the category beforehand or not
        private boolean askBySent;  // determines if the bot posts in sentences or by X number of words
        private boolean hasNegged; // sees if someone has already negged this question
        private boolean paused;  // sees if the game is paused
        private boolean answeringBonus; //Whether teams are answering a bonus or not
        private boolean useBonuses; //Whether this is a game with bonuses or not
        private boolean bonusOver; //Whether the bonus is finished or not
        private boolean startBonus; //Whether a bonus should be started (i.e. a player got the question right)
        private boolean readFirstBonusPart; //Whether we should read the first bonus part or not... false if buzz on bonus leadin
        private boolean forceBonus; //Force a bonus to be read
        private boolean inOrder; //Whether we're reading the set in order
        private boolean forceMode; //Requires a '!next' command before moving to the next question
        private boolean bonusPaused; //Whether to start the next question or not
        private boolean attemptReconnect; //Whether the bot should try reconnecting after a disconnect

        private int roundNum, questionTime, seconds, rounds; //seconds = time since end of question
        private int playersPerTeam;
        private int numGuessed;
        private int questionNum;
        private int bonusNum;
        private int delay;
        private int halftime;
        private int answerTime, answerDelay;
        private int curBonusPart; //Current bonus part we're on
        private int startIndex; //Starting index for reading a tossup in order
        private int minMVP;
        private int gameswoReload, limwoReload;
        private int breakBtwQ;
        private int numWordsPerLine;
        private int timeToJoin;
        private int halftimeLength;
        private int breakBtwBonuses;
        private int questionState = START_TOSSUP; //What question to read next

        //mvpis
        private ArrayList<Question> questions;//ArrayList of our questions
        private ArrayList<BonusQuestion> bonuses; //ArrayList of our bonuses

        //HashSets
        private HashSet<Player> players;//our set of players in the current game
        private HashSet<Player> allPlayers;//our set of all of the players who have played - for stats
        private HashSet<String> comment;//our set of comments
        private HashSet<Pattern> prompts; //holds a list of answers the player has said that has been prompted

        //private HashMap sets; // holds our list of playable sets
        private String[] sets;  // holds our playable sets
        private String[] bonusSets; //holds our playable bonus sets

        //Strings
        // TODO: Get rid of botServer and botChannel?
        private String botChannel;      //channel we belong to; default is #scobowl.
        private String botPass;  // the bot's password
        private String botServer; // the bot's server
        private static final String SETTINGS_FILE = "settings.txt";     // location of the settings file
                                                                                // Note that while it's not a final, it stays constant after initialization
        private String answerer;                //represents the person answering the question
        private String qSetFile;        //represents the current set being played
        private String bSetFile;        //represents the current bonus set being played

        private String setsFile;        //represents the file that contains all of the playable sets
        private String bonusSetsFile; //Represents the file that contains all of the playable bonus sets
        private static String setsDirectory;   // represents the directory of the sets
        private static String statsDirectory;  // represents the stats directory

        //Regular Expressions (Patterns)
        private Pattern allowedQ; //Determines which question categories are allowed
        private Pattern unallowedQ; //Determines which question categories are not allowed.

        //Teams
        private Team[] teams = {new Team(),new Team()};//array of our two teams
        private Team bonusTeam; //Which team is answering this question

        private Player gamemaster;
        private Player curNegger;

        //GameThreads
        private static GameThread maingame; //Made static since there should only be one instance of it.

        //Joins
        private Join joining; //Used as a timer for the joining period.  --> make static in future?

        // Countdown timer
        private Countdown cdTimer;      // used as a timer to limit the amount of time to accept an answer after a buzz

        //Questions
        private Question curquestion; //the current question
        private BonusQuestion curbonus; //The current bonus question

        //Randoms
        private static Random gen = new Random();//a random object for all-purpose use

        // Colors - fixes a very strange bug
        private static final String WHITE = org.jibble.pircbot.Colors.WHITE;
        private static final String DARK_BLUE = org.jibble.pircbot.Colors.DARK_BLUE;

        //FINAL VARIABLES
        private static final int NORMRND = 11;
        private static final int MATHRND = 30;
        private static final int START_TOSSUP = 0;
        private static final int START_BONUS = 1;
        private static final int CONTINUE_BONUS = 2;
        private static final int BONUS_TIME = 6;
        private static final String VERSION = "2.0.8";
        
        //--------------------DEFAULT CONSTRUCTOR: tbot()------------------------------------//
        public TBot()
        {
                // Initialize all necessary variables
                init();
                connectToServer();
        }

        // Constructor that takes in the bot's name, password, channel, and server
        public TBot(String botName, String pass, String channel, String server)
        {
                // Initialize all necessary variables
                init();
                this.setName(botName);
                botPass = pass;
                botChannel = channel;
                botServer = server;
                connectToServer();
        }

        // initializes the bot (beyond the start-up parameters)
        private void init()
        {
                loadSettings();

                // generate the storage objects
                questions = new ArrayList<Question>();
                bonuses = new ArrayList<BonusQuestion>();
                players = new HashSet<Player>();
                allPlayers = new HashSet<Player>();
                comment = new HashSet<String>();
                prompts = new HashSet<Pattern>();
                sets = new String[1];
                
                // These boolean values only matter during each question or the beginning of the game
                isPlaying = false;
                questionRight = false;
                hasNegged = false;
                paused = false;
                bonusPaused = true;
                attemptReconnect = true;
                initSets(); // initialize sets before calling it.
                loadSet(qSetFile); // load the questions
                loadBonuses(bSetFile); //load the bonuses
        }
        
        private void loadSettings()
        {
                try{
                        BufferedReader reader = new BufferedReader(new FileReader(SETTINGS_FILE));
                        String line;
                        int i = 0;
                        while(true)
                        {
                                line = reader.readLine();
                                // Break out of the loop
                                if(line == null || line.equals("~"))
                                        break;
                                else{
                                        int lint = 0;
                                        boolean bint = false;
                                        
                                        // If we can't convert the line to an integer, then
                                        // it's a boolean, so convert it to that.
                                        try{
                                                lint = Integer.valueOf(line);
                                        }
                                        catch(Exception e)
                                        {       
                                                bint = Boolean.valueOf(line);
                                        }

                                        switch(i)
                                        {
                                                case 0: roundNum = lint;break;
                                                case 1: questionTime = lint;break;
                                                case 2: seconds = lint;break;
                                                case 3: rounds = lint;break;
                                                case 4: playersPerTeam = lint;break;
                                                case 5: delay = lint;break;
                                                case 6: answerTime = lint;break;
                                                case 7: answerDelay = lint;break;
                                                case 8: minMVP = lint;break;
                                                case 9: halftime = lint;break;
                                                case 10: gameswoReload = lint;break;
                                                case 11: limwoReload = lint;break;
                                                case 12: qSetFile = line;break;
                                                case 13: allowedQ = Pattern.compile(line,Pattern.CASE_INSENSITIVE);break;
                                                case 14: unallowedQ = Pattern.compile(line,Pattern.CASE_INSENSITIVE);break;
                                                case 15: showCategory = bint;break;
                                                case 16: negs = bint;break;
                                                case 17: setMessageDelay(lint);break;
                                                case 18: breakBtwQ = lint;break;
                                                case 19: askBySent = bint;break;
                                                case 20: numWordsPerLine = lint;break;
                                                case 21: setsFile = line;break;
                                                case 22: setsDirectory = line;break;
                                                case 23: timeToJoin = lint;break;
                                                case 24: bonusSetsFile = line;break;
                                                case 25: forceMode = bint;break;
                                                case 26: statsDirectory = line;break;
                                                case 27: halftimeLength=lint;break;
                                                case 28: bSetFile=line;break;
                                                case 29: breakBtwBonuses=lint;break;
                                                case 30: setName(line);break;
                                                case 31: botChannel=line;break;
                                                // TODO: Encrypt the bot's password in the settings file
                                                case 32: botPass=line;break;
                                                case 33: botServer=line;break;
                                                default: 
                                                        System.out.println("Problem with your settings.txt file.  " +
                                                                        "Shutting down the bot...");
                                                        msg("Problems with settings.txt, shutting down...");
                                        }
                                }
                                i++;
                        }
                        reader.close();
                        if(numWordsPerLine == 0)        // if this is true, then numWordsPerLine was never initialized, so we have a problem.
                        {
                                // TODO: Make into a private method to remove code duplication?
                                /*msg*/System.out.println("Incomplete settings.txt file.  Shutting down...");
                                disconnectError("Problems with settings.txt, shutting down...");
                        }
                } catch(IOException e) {
                        e.printStackTrace();
                }
        }
        
        private void connectToServer()
        {
                try {
                        connect(botServer);
                } catch (NickAlreadyInUseException e) {
                        e.printStackTrace();
                        disconnectError("Nickname already in use");
                } catch (IrcException e) {
                        e.printStackTrace();
                        disconnectError("IRCExcetpion when connecting");
                } catch(IOException e) {
                        e.printStackTrace();
                }
        }
        
        //--------------------msg(String text): emits a message in the current channel-------//
        private void msg(String text)
        {
                // sends a message on a white background with dark blue text
                // the package name is in here because I kept getting a weird "wrong class" error
                sendMessage(botChannel, WHITE + DARK_BLUE + text);
        }


        //--------------------write(BufferedWriter writer, String text): writes to the file w/ a new line-------//
        public static void write(BufferedWriter writer, String text)
        {
                try{
                        writer.write(text);
                        writer.flush();
                        writer.newLine();
                        writer.flush();
                }
                catch(IOException e){
                        e.printStackTrace();
                }
        }

        //--------------------loadSet(String): loads the questionset from setName------------//

        // loadSet --> Read qSets, add ".*" to the user's preference, and find any set that applies to it.
        // Then load all of those sets.

        // This is a regular expression version of loadSet.
        private void loadSet(String setName)
        {
                // Compile setName into a Pattern, and see which sets we need to load.
                // We add ".*" to setName so that it accepts everything containing setName + misc text
                // We do this so that people can play all of a set if they want to (so 
                // acfreg would play acfreg20xx-2008, for example)
                // We have a try-catch block to prevent people from crashing the bot by 
                // putting in invalid regexes
                Pattern setToLoad;
                try
                {
                        setToLoad = Pattern.compile(setName + ".*", Pattern.CASE_INSENSITIVE);
                }
                catch(PatternSyntaxException e)
                {
                        msg("Error: set parameter is not a valid regular expression.  Reverting to first set.");
                        return;
                }
                BufferedReader reader;
                int counter = 0;
                questions.clear();

                for(int i = 0; i < sets.length; i++)    // Go through the sets and see if they match the Pattern
                {
                        if(setToLoad.matcher(sets[i]).matches())
                        {
                                counter = 0;
                                try{
                                        reader = new BufferedReader(new FileReader(setsDirectory + sets[i].trim() + ".txt"));
                                        String line;
                                        while(true)
                                        {
                                                line = reader.readLine();
                                                if(line == null)
                                                        break;
                                                // Create a temporary question, and if it matches accepted questions (and not unallowed questions),
                                                // then ID it and add it to the questions list.
                                                Question temp = new Question(line);
                                                if(allowedQ.matcher(temp.getTopic()).matches() && !unallowedQ.matcher(temp.getTopic()).matches())
                                                {
                                                        temp.setQuestionNum(counter);
                                                        temp.setQuestionSet(sets[i]);
                                                        questions.add(temp);
                                                }
                                                counter++;
                                        }
                                        reader.close();
                                }
                                catch(IOException e){msg(sets[i].trim() + ".txt does not exist.");}
                        } // end of if
                } // end of for


                // check to see if the set is empty.  If it is, reset the regular expression to .*, and reload the set.
                if(questions.size() == 0 && !allowedQ.pattern().equals(".*"))
                {
                        msg(allowedQ.pattern() + " is not contained in this set.  Reloading set with all topics allowed.");
                        allowedQ = Pattern.compile(".*");
                        unallowedQ = Pattern.compile("");               // change later?
                        loadSet(setName);
                }
                else if(questions.size() == 0 && allowedQ.pattern().equals(".*"))
                {
                        if(qSetFile.equals(sets[0]))    // Something's broken!
                        {
                                disconnectError("IO Error: first set in setsFile does not exist on disk.  Terminating...");
                        }
                        else
                        {
                                msg("Set does not exist, reverting to a known set.");
                                loadSet(sets[0]); // first set should work
                        }
                }
                else
                {
                        msg("Number of questions: " + Integer.toString(questions.size()));
                        qSetFile = setName;
                }
        }

        //Attempt to load the bonuses for a particular set
        private void loadBonuses(String setName)
        {
                if (setName == null || setName.equals(""))
                        return; //Ignore loading sets when no bonuses are specified

                // try-catch block here to prevent people from crashing the bot
                Pattern setToLoad;
                try
                {
                        setToLoad = Pattern.compile(setName + ".*");
                }
                catch(PatternSyntaxException e)
                {
                        msg("Error: bonus set parameter is not a valid regular expression.  Keeping same bonus set.");
                        return;
                }
                BufferedReader reader;
                int counter = 0;
                bonuses.clear();

                for(int i = 0; i < bonusSets.length; i++)       // Go through the sets and see if they match the Pattern
                {
                        if(setToLoad.matcher(bonusSets[i]).matches())
                        {
                                counter = 0;
                                try{
                                        reader = new BufferedReader(new FileReader(setsDirectory + bonusSets[i].trim() + ".txt"));
                                        String line;
                                        while(true)
                                        {
                                                line = reader.readLine();
                                                if(line == null)
                                                        break;
                                                // Create a temporary question, and if it matches accepted questions (and not unallowed questions),
                                                // then ID it and add it to the questions list.
                                                BonusQuestion temp = new BonusQuestion(line);
                                                if(allowedQ.matcher(temp.getTopic()).matches() && !unallowedQ.matcher(temp.getTopic()).matches())
                                                {
                                                        temp.setQuestionNum(counter);
                                                        temp.setQuestionSet(bonusSets[i]);
                                                        bonuses.add(temp);
                                                }
                                                counter++;
                                        }
                                        reader.close();
                                }
                                catch(IOException e){msg(sets[i].trim() + ".txt does not exist.");}
                        } // end of if
                } // end of for

                // check to see if the set is empty.  If it is, reset the regular expression to .*, and reload the set.
                if(bonuses.size() == 0 && !allowedQ.pattern().equals(".*"))
                {
                        msg(allowedQ.pattern() + " is not contained in this set.  Reloading set with all topics allowed.");
                        allowedQ = Pattern.compile(".*");
                        unallowedQ = Pattern.compile("");               // change later?
                        loadBonuses(setName);
                }
                else if(bonuses.size() == 0 && allowedQ.pattern().equals(".*"))
                {
                        if(bSetFile.equals(bonusSets[0]))       // Something's broken!
                                disconnectError("IO Error: first set in bonusSetsFile does not exist on disk.  Terminating...");
                        else
                        {
                                msg("Bonus set does not exist, not loading bonuses.");
                                bonuses.clear();
                        }
                }
                else
                {
                        if (useBonuses)
                                msg("Number of bonuses: " + Integer.toString(bonuses.size()));
                        bSetFile = setName;
                }
        }

        // This loads the set of possible sets.  We only call it in the beginning
        // or through a special command that reloads it.
        private void initSets()
        {
                // setsFile should just be a one line file with a list of sets split up by commas.
                try{
                        BufferedReader reader = new BufferedReader(new FileReader(setsDirectory + setsFile));
                        sets = reader.readLine().toLowerCase().split(",");
                        // Eliminate spaces
                        for(int i = 0; i < sets.length; i++)
                                sets[i] = sets[i].trim();
                        reader.close();

                        reader = new BufferedReader(new FileReader(setsDirectory + bonusSetsFile));
                        bonusSets = reader.readLine().toLowerCase().split(",");
                        // Eliminate spaces
                        for(int i = 0; i < bonusSets.length; i++)
                                bonusSets[i] = bonusSets[i].trim();
                        reader.close();

                }
                catch(IOException e)
                {
                        // If we can't find setsFile, there's no point in having the bot, so exit.
                        disconnectError("IO Error: setsFile or bonusSetsFile could not be found.  Terminating...");
                }
        }

        /********************************
        **IMPORTANT IMPORTANT IMPORTANT**
        ********************************/

        //------------onMessage(...): Calls on helper methods for various tasks--------------//
        // synchronized makes it slower, but it allows us to pause the game
        protected synchronized void onMessage(String channel, String sender, String login, String host, String text)
        {
                text = text.trim();

                //if "buzz" is entered during the round, prompts the user for an answer
                if(isPlaying && answerTime == 0 && !questionRight && !paused &&
                text.trim().toLowerCase().equals("buzz") && getPlayer(sender) != null
                && !getTeam(getPlayer(sender).getTeam()).hasGuessed())
                {
                        Player tempPlayer = getPlayer(sender);
                        Team tempTeam = getTeam(tempPlayer.getTeam());
                        if (!answeringBonus || tempTeam.equals(bonusTeam))
                                startAnswer(sender, text);
                }

                // checks for the validity of an entered answer and assigns points on that basis
                // All of the comments for commands can be found on the documentation page.
                else if(isPlaying && sender.equals(answerer) && !text.equalsIgnoreCase("buzz") && getPlayer(sender) != null && !getTeam(getPlayer(sender).getTeam()).hasGuessed() && questionRight == false) // every possible thing to do with an answer
                {
                        Player tempPlayer = getPlayer(sender);

                        if (!answeringBonus)
                                tempPlayer.guessed();

                        Question tempQuestion; //A tossup or bonus question
                        String tempID, tempSet;
                        if (!answeringBonus)
                        {
                                tempQuestion = curquestion;
                                tempID = Integer.toString(curquestion.getQuestionNum());
                                tempSet = curquestion.getQuestionSet();
                        }
                        else
                        {
                                tempQuestion = curbonus.getBonusPart(curBonusPart);
                                tempID = Integer.toString(curbonus.getQuestionNum());
                                tempSet = curbonus.getQuestionSet();
                        }

                        if(tempQuestion.isCorrect(text)) //&& questionRight == false)
                        {
                                stopQuestion();
                                msg(sender + " gets it with " + text + ".  Possible Answers: " + tempQuestion.getAnswersString() +
                                        " (ID: " + tempID +  " " + tempSet + ")");
                                
                                // add to the score
                                int points;
                                if (!answeringBonus)
                                {
                                        tempPlayer.correct();
                                        tempPlayer.addScore(10);
                                        points = 10;
                                        if (useBonuses)
                                        { //Set which team will use this bonus
                                                bonusTeam = getTeam(tempPlayer.getTeam());
                                                startBonus = true; //Successfully answered, so start the bonus
                                        }
                                }
                                else
                                {
                                        points = curbonus.getPoints(curBonusPart);
                                        bonusTeam.addBonusPoints(points);
                                        readFirstBonusPart = false; //Don't read the first bonus part if we already did
                                }
                                getTeam(tempPlayer.getTeam()).addScore(points);
                        }
                        // see if it's promptable
                        else if(tempQuestion.isPromptable(text) && !inPrompts(text))
                        {
                                // reset the time
                                msg("Prompt");
                                
                                // make the person be able to time out
                                cdTimer = new Countdown();
                                cdTimer.start();
                                // Replace spaces with /s* to prevent getting unlimited prompts
                                // by adding more spaces
                                text = text.replace(" ","\\s*");
                                prompts.add(Pattern.compile(text,Pattern.CASE_INSENSITIVE));
                                
                                answerTime = answerDelay;
                                answerer = sender;
                        }
                        else if(isPlaying && sender.equals(answerer) && text.length() > 0 && !text.substring(0,1).equals("!"))
                        {
                                answerer = "";
                                answerTime = 0;
                                msg(sender +" [" + tempPlayer.getTeam() + "] guessed " + text + ", which is incorrect.");
                                incorrectAnswer(sender);
                        }
                }

                // Here is where all of the commands are
                // We check for ! at the beginning to see if the bot should bother
                // figuring out what was just posted
                else if(text.length() >= 1 && text.charAt(0) == '!')
                {
                        regularCommands(channel, sender, login, host, text);
                        
                        User asker = retrieveUser(sender);
                        if(asker != null)
                        {
                                // Op-only commands: !clearcom, !exit, !loadplayer, !set messageDelay
                                if(asker.isOp())
                                        opCommands(channel, sender, login, host, text);
                                // Ops can also use gamemaster commands
                                if(getPlayer(sender) == gamemaster || asker.isOp() )
                                        gamemasterCommands(channel, sender, login, host, text);
                        }
                }
        }
        
        private void regularCommands(String channel, String sender, String login, String host, String text)
        {
                if(text.length() > 5 && text.substring(1,6).equalsIgnoreCase("start") && !isPlaying && !isJoining)
                        startJoining(text);

                //if "!join" is uttered by a channel member, followed by a team name, he is added to the team
                else if(text.length() > 5 && text.substring(1,5).equalsIgnoreCase("join") && (isJoining || isPlaying))
                        addToTeam(sender, text);

                else if(text.equalsIgnoreCase("!random") && (isJoining || isPlaying))
                        addToTeam(sender, "!join " + teams[gen.nextInt(teams.length)].getName());

                // Returns the size of the question set
                // TODO: Move to !get ?
                else if(text.equalsIgnoreCase("!size"))
                        msg("Number of tossups: " + Integer.toString(questions.size()) + ", " +
                                        "number of bonuses: " + Integer.toString(bonuses.size()));

                // more specific: gets the size of a certain category
                else if(text.length() > 5 && text.substring(1,5).equalsIgnoreCase("size"))
                {
                        String category = text.substring(5,text.length()).trim().toLowerCase();
                        int cases = 0;

                        // go through all of the questions, and see which questions have
                        for(int i =0; i < questions.size();i++)
                                if(category.equals( questions.get(i).getTopic().toLowerCase() ))
                                        cases++;
                        msg("Number of " + category + " tossups: " + Integer.toString(cases) + 
                                        ", number of " + category + " bonuses: " + Integer.toString(bonuses.size()));
                }

                // PAUSE FUNCTION: see if this needs to be regulated.  For now, make it !pause and !unpause
                else if(text.equalsIgnoreCase("!pause") && !paused && isPlaying)
                {
                        try{
                                maingame.pause();
                        }catch(Exception e){
                                msg("Error with !pause.  Message: " + e.toString());
                                e.printStackTrace();
                        }
                }

                else if(text.equalsIgnoreCase("!unpause") && paused && isPlaying)
                {
                        msg("Unpaused.");
                        maingame.unpause();
                }

                else if(text.equalsIgnoreCase("!score") && isPlaying)   // deprecated, but still here
                        msg("Team " + teams[0].getName() + ": " + Integer.toString(teams[0].getScore()) + ", Team " + teams[1].getName() + ": " + Integer.toString(teams[1].getScore()));

                else if(text.length() > 8 && text.substring(1,8).equalsIgnoreCase("comment"))
                {
                        comment.add(sender + ": " + text.substring(8,text.length()).trim());
                        msg("Comment added.");
                }

                else if(text.length() > 5 && text.substring(1,5).equalsIgnoreCase("team") && getPlayer(text.substring(6,text.length()).trim()) != null)
                {
                        Player temp = getPlayer(text.substring(6,text.length()));
                        msg(temp.getName() + "'s Team: " + temp.getTeam());
                }

                else if(text.equalsIgnoreCase("!help"))
                        sendMessage(sender,"http://www.aegisquestions.com/triviabot/");

                else if(text.equalsIgnoreCase("!teams"))        // deprecated, but still here
                        msg(teams[0].getName() + teams[0].getPlayers().toString() + " and " + teams[1].getName() + teams[1].getPlayers().toString());

                // get stats on a user
                else if(text.length() > 6 && text.substring(1,6).equalsIgnoreCase("stats"))
                {
                        if(getPlayerFromAllPlayers(text.substring(6,text.length()).trim()) != null)
                                msg(getPlayerFromAllPlayers(text.substring(6,text.length()).trim()).getStats());
                        else
                                msg(text.substring(6,text.length()).trim() + " does not have a stats file loaded.");
                }

                else if(text.length() > 9 && text.substring(1,9).equalsIgnoreCase("curstats"))
                {
                        if(getPlayer(text.substring(9,text.length()).trim()) != null)
                                msg(getPlayer(text.substring(9,text.length()).trim()).getCurStats());
                        else
                                msg(text.substring(9,text.length()).trim() + " is not playing in this game.");
                }

                // Exception: Players can remove themselves, gamemasters can remove players, and ops can remove players
                else if(text.length() > 7 && text.substring(1,7).equalsIgnoreCase("remove") &&
                                getPlayer(text.substring(7,text.length()).trim()) != null)
                {
                        Player rem = getPlayer(text.substring(7,text.length()).trim());
                        // Make sure the player is allowed to do this
                        if(sender.equals(rem.getName()) || getPlayer(sender).equals(gamemaster) ||
                                        isOp(sender))
                        {
                                if(rem != null)
                                {
                                        msg("Player " + rem.getName() + " removed.");
                                        // stops if no one is playing anymore--player is removed in stillPlayers
                                        stillPlayers(rem.getName());

                                        // If the player was not removed in stillPlayers, remove them now
                                        // Note that rem is still a valid player if the condition holds
                                        if(getPlayer(text.substring(7,text.length()).trim()) != null)
                                                removePlayer(rem);
                                }
                                else
                                        msg("Player does not exist.");
                        }
                }

                // retrieves certain variables.  Some variables can only be accessed by an op.
                // variables that use get: negs, rounds, delay, minMVP, gameswoReload, limwoReload
                // comment, players, allPlayers, allowedQ, unallowedQ
                // last part of if-statement there to allow for getq, which requires a parameter.
                else if(text.length() > 5 && text.substring(1,4).equalsIgnoreCase("get") && !text.substring(1,5).equalsIgnoreCase("getq"))
                {
                        String get = text.substring(5,text.length()).trim();
                        if(get.equalsIgnoreCase("negs"))
                                if(negs)
                                        msg("Negs are ON");
                                else
                                        msg("Negs are OFF");
                        else if(get.equalsIgnoreCase("rounds"))
                                msg("Rounds in game: " + Integer.toString(rounds) + ".");
                        else if(get.equalsIgnoreCase("teams"))
                                msg(teams[0].getName() + teams[0].getPlayers().toString() + " and " + teams[1].getName() + teams[1].getPlayers().toString());
                        else if(get.equalsIgnoreCase("score"))
                                msg("Team " + teams[0].getName() + ": " + Integer.toString(teams[0].getScore()) + ", Team " + teams[1].getName() + ": " + Integer.toString(teams[1].getScore()));
                        else if(get.equalsIgnoreCase("delay"))
                                msg("Character message delay: " + Integer.toString(delay)  + " milliseconds.");
                        else if(get.equalsIgnoreCase("minMVP") || get.equalsIgnoreCase("mvp"))
                                msg("Minimum number of players needed for MVP/LVP stats: " + Integer.toString(minMVP) + ".");
                        else if(get.equalsIgnoreCase("gamesworeload"))
                                msg("Current number of games without a reload: " + Integer.toString(gameswoReload) + ".");
                        else if(get.equalsIgnoreCase("limworeload"))
                                msg("Maximum number of games without a reload: " + Integer.toString(limwoReload) + ".");
                        else if(get.equalsIgnoreCase("players"))
                                msg("Current players: " + players.toString());
                        else if(get.equalsIgnoreCase("allplayers"))
                                msg("All players during uptime: " + allPlayers.toString());
                        else if(get.equalsIgnoreCase("comments") || get.equalsIgnoreCase("com"))
                                // This now iterates through the comments and sends them one by one to the sender
                                for(String com : comment)
                                        sendMessage(sender, com);
                        else if(get.equalsIgnoreCase("allowedQ"))
                                msg("Allowed questions: " + allowedQ.toString());
                        else if(get.equalsIgnoreCase("unallowedQ"))
                                msg("Unallowed questions: " + unallowedQ.toString());
                        else if(get.equalsIgnoreCase("showcategory") || get.equalsIgnoreCase("category"))
                                msg("Show Category is set to " + Boolean.toString(showCategory));
                        else if(get.equalsIgnoreCase("answerdelay"))
                                msg("Time for answering questions set to: " + Integer.toString(answerDelay) + " seconds.");
                        else if(get.equalsIgnoreCase("breakBtwQ"))
                                msg("Time delay between questions: " + Integer.toString(breakBtwQ) + " milliseconds.");
                        else if(get.equalsIgnoreCase("messageDelay"))
                                msg("Message delay set to " + Long.toString(getMessageDelay()) + " milliseconds.");
                        else if(get.equalsIgnoreCase("numWordsPerLine"))
                                msg("Number of words to send per line: " + Integer.toString(numWordsPerLine) + ".");
                        else if(get.equalsIgnoreCase("sets"))
                        {
                                String listOfSets = "[";
                                int counter = 1;
                                for(String s : sets)
                                {
                                        // TODO: Make into a final class variable later?
                                        final int SETS_PER_LINE = 20;
                                        listOfSets += s + ", ";
                                        if(counter == SETS_PER_LINE)
                                        {
                                                msg(listOfSets);
                                                listOfSets = "";
                                                counter = 0;
                                        }
                                        counter++;
                                }
                                msg(listOfSets.substring(0, listOfSets.length()-2) + "]");
                        }
                        else if (get.equalsIgnoreCase("bonussets"))
                        {
                                // TODO: Make into a final class variable later?
                                final int SETS_PER_LINE = 15;
                                int counter = 1;
                                String listOfSets = "[";
                                for(String s : bonusSets) 
                                {
                                        listOfSets += s + ", ";
                                        if(counter % SETS_PER_LINE == 0)
                                        {
                                                msg(listOfSets);
                                                listOfSets = "";
                                        }
                                        counter++;
                                }
                                // We do -2 to get rid of the last ", ".
                                msg(listOfSets.substring(0, listOfSets.length()-2) + "]");
                        }
                        else if(get.equalsIgnoreCase("paused"))
                                msg("Pause status: " + Boolean.toString(paused));
                        else if(get.equalsIgnoreCase("playersPerTeam"))
                                msg("Maximum number of players per team: " + Integer.toString(playersPerTeam));
                        else if(get.equalsIgnoreCase("timeToJoin"))
                                msg("Joining period length: " + Integer.toString(timeToJoin));
                        else if(get.equalsIgnoreCase("askBySent"))
                                if(askBySent)
                                        msg("Questions asked by splitting up by sentences.");
                                else
                                        msg("Questions asked based on the number of words.");

                        else if(get.equalsIgnoreCase("version"))
                                msg("TriviaBot " + VERSION + " © 2006-2009 Alejandro Lopez-Lago & Raghav Puranmalka");
                        else if(get.equalsIgnoreCase("gamemaster"))
                        {
                                if(gamemaster == null)
                                        msg("No gamemaster at the moment.");
                                else
                                        msg(gamemaster.getName() + " is the Gamemaster.");
                        }
                        else if(get.equalsIgnoreCase("set"))
                        {
                                msg("Current set: " +  qSetFile);
                                if (useBonuses)
                                        msg("Current bonus set: " + bSetFile);
                        }

                        // operator-only "getq" command
                        else if(get.length() > 8 && get.substring(0,8).trim().equalsIgnoreCase("question") 
                                        && isOp(sender))
                        {
                                try{
                                        msg( (questions.get(Integer.valueOf(get.substring(8, get.length()).trim()))).toString());
                                }
                                catch(IndexOutOfBoundsException e){msg("Invalid syntax");}
                                catch(NumberFormatException e) {msg("Invalid syntax");}
                        }
                        else if(get.length() > 1 && get.substring(0,2).equalsIgnoreCase("q ") && isOp(sender))
                        {
                                try{
                                        msg( (questions.get(Integer.valueOf(get.substring(2, get.length()).trim()))).toString());
                                }
                                catch(IndexOutOfBoundsException e){msg("Invalid syntax");}
                                catch(NumberFormatException e) {msg("Invalid syntax");}
                        }

                        // gets the question and answer frequency for a particular term - put into a different method next version?
                        else if(get.length() > 1 && get.substring(0,4).equalsIgnoreCase("freq"))
                        {
                                String search = get.substring(4,get.length()).trim().toLowerCase();
                                int Qfreq = 0;
                                int Afreq = 0;
                                for(Question q : questions)
                                {
                                        if(q.getQuestion().indexOf(search) >= 0)
                                        {
                                                int counter = 0;
                                                String quest = q.getQuestion().toLowerCase();

                                                quest = quest.replace('!',' ');
                                                quest = quest.replace('.',' ');
                                                quest = quest.replace('?',' ');
                                                quest = quest.replace(',',' ');
                                                quest = quest.replace(';',' ');

                                                while(counter < quest.length() && quest.indexOf(search+" ",counter) >= 0)
                                                {
                                                        counter = quest.indexOf(search+" ",counter) + 1;
                                                        Qfreq++;
                                                }
                                        }
                                        if(q.isCorrect(search))
                                                Afreq++;
                                }
                                msg("Question frequency: " + Integer.toString(Qfreq) + " | Answer Frequency: " + Integer.toString(Afreq) +
                                                " | Weighted Frequency: " + Double.toString((100*(Qfreq + 5.0*Afreq) / questions.size())));
                        }
                        else if (get.equalsIgnoreCase("forcemode"))
                                if (forceMode)
                                        msg("Force mode is on.  Gamemaster must type '!next' to start each question.");
                                else
                                        msg("Force mode is off.  Questions will be read automatically");
                        else if (get.equalsIgnoreCase("startindex"))
                                msg("Start index for in order reading is: " + startIndex);
                        else if (get.equalsIgnoreCase("inorder"))
                                if (inOrder)
                                        msg("Questions are being read in order.");
                                else
                                        msg("Questions are being read randomly.");
                        else if(get.equalsIgnoreCase("halftimeLength"))
                                msg("Halftime length: " + halftimeLength + " seconds.");
                        else if(get.equalsIgnoreCase("breakBtwBonuses"))
                                msg("Break between bonus parts: " + breakBtwBonuses + " milliseconds.");
                        else // tell the user their command does exist
                                msg("You can't use !get on that.");
                }
        }

        private void gamemasterCommands(String channel, String sender, String login, String host, String text)
        {
                if(text.equalsIgnoreCase("!reload"))
                {
                        loadSet(qSetFile);
                        loadBonuses(bSetFile);
                        msg("Reload successful");
                }
                // TODO: Replace unpause() with a similar function for forcemode
                // TODO: Make into a regular command?
                else if (forceMode && text.equalsIgnoreCase("!next"))
                        maingame.bonusUnpause();
                // Stops the current game.
                else if(text.equalsIgnoreCase("!stop"))
                {
                        stopGame();
                        msg("Game stopped.");
                        joining = null;
                        cdTimer = null;
                }
                // Resets bot variables to their default state
                else if(text.equalsIgnoreCase("!default"))
                        loadSettings();
                // variables that use get: negs, delay, minMVP, gamesworeReload, limwoReload, allowedQ, unallowedQ, showCategory,
                // answerDelay, breakBtwQ, [messageDelay for ops]
                else if(text.length() > 5 && text.substring(1,4).equalsIgnoreCase("set"))
                {
                        String[] arguments = text.split(" "); // this should create an array with size 3
                        if(arguments.length < 3) // make sure it's formatted properly
                        {
                                msg("Error: improper !set command.");
                                return; // otherwise, things will break
                        }
                        String set = arguments[1];
                        String param = arguments[2];
                        // if the parameters had spaces in them, add them back in
                        for(int i = 3; i < arguments.length; i++)
                                param += " " + arguments[i];
                        // the first part sees what variable needs to be modified, and the second part
                        // is the value the variable will be set to.
                        // Catch any NumberFormatExceptions
                        try {
                        
                        if(set.equalsIgnoreCase("negs"))
                                if(param.equalsIgnoreCase("On") || param.equalsIgnoreCase("True"))
                                {
                                        negs = true;
                                        msg("Negs are ON");
                                }
                                else if(param.equalsIgnoreCase("Off") || param.equalsIgnoreCase("False"))
                                {
                                        negs = false;
                                        msg("Negs are OFF");
                                }
                                else
                                        msg("Invalid parameter (expected on or off)");
                        else if(set.equalsIgnoreCase("delay") && Integer.valueOf(param).intValue() > 0
                                        && Integer.valueOf(param).intValue() < Integer.MAX_VALUE)
                        {
                                delay = Integer.valueOf(param).intValue();
                                msg("Character message delay set to: " + Integer.toString(delay) + " milliseconds.");
                        }
                        else if((set.equalsIgnoreCase("minMVP") || set.equalsIgnoreCase("mvp")) && Integer.valueOf(param).intValue() > 1)
                        {
                                minMVP = Integer.valueOf(param).intValue();
                                msg("Minimum number of players needed for MVP/LVP stats set to: " + Integer.toString(minMVP) + ".");
                        }
                        else if(set.equalsIgnoreCase("gamesworeload") && Integer.valueOf(param).intValue() > 0)
                        {
                                gameswoReload = Integer.valueOf(param).intValue();
                                msg("Current number of games without a reload set to: " + Integer.toString(gameswoReload));
                        }
                        else if(set.equalsIgnoreCase("limworeload") && Integer.valueOf(param).intValue() > 0)
                        {
                                limwoReload = Integer.valueOf(param).intValue();
                                msg("Maximum number of games without a reload set to: " + Integer.toString(limwoReload));
                        }
                        else if(set.equalsIgnoreCase("allowedQ"))
                        {
                                // TODO: Figure out exception and catch it explicitly
                                try {
                                        allowedQ = Pattern.compile(param,Pattern.CASE_INSENSITIVE);
                                        loadSet(qSetFile);
                                        loadBonuses(bSetFile);
                                        msg("Allowed questions: " + allowedQ.toString());
                                }
                                catch(Exception e) {
                                        msg("Improper regular expression used.");
                                        System.out.println("Improper regex used in allowedQ");
                                        e.printStackTrace();
                                }
                        }
                        else if(set.equalsIgnoreCase("unallowedQ"))
                        {
                                try{
                                        unallowedQ = Pattern.compile(param,Pattern.CASE_INSENSITIVE);
                                        loadSet(qSetFile);
                                        loadBonuses(bSetFile);
                                        msg("Unallowed questions: " + unallowedQ.toString());
                                }
                                catch(PatternSyntaxException e) {
                                        msg("Improper regular expression used.");
                                        System.out.println("Improper regex used in unallowedQ");
                                        e.printStackTrace();
                                }
                        }
                        else if(set.equalsIgnoreCase("showCategory") || set.equalsIgnoreCase("category"))
                        {
                                showCategory = Boolean.valueOf(param);
                                msg("ShowCategory is set to: " + Boolean.toString(showCategory));
                        }
                        else if(set.equalsIgnoreCase("answerDelay") && Integer.valueOf(param).intValue() > 0
                                        && Integer.valueOf(param).intValue() < Integer.MAX_VALUE)
                        {
                                answerDelay = Integer.valueOf(param).intValue();
                                msg("Time for answering questions set to: " + Integer.toString(answerDelay) + " seconds.");
                        }
                        // TODO: Get rid of magic number
                        else if(set.equalsIgnoreCase("breakBtwQ") && Integer.valueOf(param).intValue() > 0 
                                        && Integer.valueOf(param).intValue() < 60000)
                        {
                                breakBtwQ = Integer.valueOf(param).intValue();
                                msg("Break between questions is now: " + Integer.toString(breakBtwQ) + " milliseconds.");
                        }
                        else if(set.equalsIgnoreCase("askBySent"))
                        {
                                askBySent = Boolean.valueOf(param);
                                if(askBySent)
                                        msg("Questions will be split up by sentences.");
                                else
                                        msg("Questions will be split up based on the number of words.");
                        }
                        else if(set.equalsIgnoreCase("numWordsPerLine") && Integer.valueOf(param).intValue() > 0 && Integer.valueOf(param).intValue() < Integer.MAX_VALUE)
                        {
                                numWordsPerLine = Integer.valueOf(param).intValue();
                                msg("Number of words sent per line is now: " + Integer.toString(numWordsPerLine) + ".");
                        }
                        else if(set.equalsIgnoreCase("rounds") && Integer.valueOf(param).intValue() >= roundNum && Integer.valueOf(param).intValue() > 0)// && Integer.valueOf(param).intValue() < Integer.MAX_VALUE)
                        {
                                int oldRounds = rounds;
                                rounds = Integer.valueOf(param).intValue();
                                // See if we won't run out of questions
                                if(rounds < questions.size() && rounds > 0 && (!useBonuses || rounds < bonuses.size()) )
                                {
                                        // change the halftime variable - MAYBE BREAKABLE LOCATION?
                                        halftime = rounds / 2;
                                        msg("Number of rounds in the game: " + Integer.toString(rounds) + ".");
                                }
                                else
                                {
                                        rounds = oldRounds;
                                        msg("Improper input: number of rounds must be less than the size of the question database.");
                                }
                        }
                        else if(set.equalsIgnoreCase("playersPerTeam") && Integer.valueOf(param).intValue() > 0 
                                        && Integer.valueOf(param).intValue() < Integer.MAX_VALUE)
                        {
                                playersPerTeam = Integer.valueOf(param).intValue();
                                msg("Maximum number of players per team: " + Integer.toString(playersPerTeam) + ".");
                        }
                        else if(set.equalsIgnoreCase("timeToJoin") && Integer.valueOf(param).intValue() > 0 
                                        && Integer.valueOf(param).intValue() < Integer.MAX_VALUE)
                        {
                                timeToJoin = Integer.valueOf(param).intValue();
                                msg("Joining period length: " + Integer.toString(timeToJoin) + ".");
                        }
                        else if(set.equalsIgnoreCase("gamemaster") && players.contains(getPlayer(param)) 
                                        && retrieveUser(param) != null)
                        {
                                gamemaster = getPlayer(param);
                                msg("New gamemaster: " + param);
                        }
                        else if (set.equalsIgnoreCase("bonuses"))
                        {
                                //Allow bonuses to be one
                                if (param.equalsIgnoreCase("on"))
                                {
                                        if (bSetFile == null || bSetFile == "")
                                        {
                                                msg("No bonus set is currently loaded.");
                                                useBonuses = false;
                                        }
                                        else
                                        {
                                                msg("Bonuses turned on.");
                                                useBonuses = true;
                                        }

                                }
                                else if (param.equalsIgnoreCase("off"))
                                {
                                        useBonuses = false;
                                        msg("Bonuses turned off.");
                                }
                                else
                                        msg("Invalid paramater (expected on or off)");
                        }
                        else if (set.equalsIgnoreCase("inorder"))
                        {
                                if (param.equalsIgnoreCase("on"))
                                {
                                        msg("Reading the set in order.");
                                        inOrder = true;
                                }
                                else if (param.equalsIgnoreCase("off"))
                                {
                                        msg("Reading the set randomly.");
                                        inOrder = false;
                                }
                                else
                                        msg("Invalid paramater (expected on or off)");
                        }
                        else if (set.equalsIgnoreCase("startindex"))
                        {
                                try
                                {
                                        int tempIndex = Integer.parseInt(param);
                                        if (tempIndex < 0 || tempIndex >= questions.size())
                                                msg("Invalid paramater, must be a postiive integer and less than the size of the question set.");
                                        else
                                        {
                                                startIndex = tempIndex;
                                                msg("startIndex is now " + Integer.toString(startIndex));
                                        }
                                }
                                catch (NumberFormatException e)
                                {
                                        msg("Invalid paramater, must be a positive integer.");
                                }

                        }
                        else if (set.equalsIgnoreCase("forcemode"))
                        {
                                // nextCalled is set to false so that we don't interrupt an ongoing question
                                if (param.equals("on"))
                                {
                                        forceMode = true;
                                        bonusPaused = false;
                                        msg("Force mode on.  Gamemaster must type '!next' after each question to continue.");
                                }
                                else if (param.equals("off"))
                                {
                                        forceMode = false;
                                        bonusPaused = true;
                                        msg("Force mode off.  Questions will continue automatically.");
                                }
                                else
                                        msg("Invalid paramater (expected on or off)");
                        }
                        else if(set.equalsIgnoreCase("halftimeLength") && Integer.valueOf(param).intValue() >= 0
                                        && Integer.valueOf(param).intValue() < Integer.MAX_VALUE )
                        {
                                halftimeLength = Integer.valueOf(param);
                                msg("New halftime length: " + halftimeLength + " seconds.");
                        }
                        else if(set.equalsIgnoreCase("BreakBtwBonuses") && Integer.valueOf(param).intValue() > 0
                                        && Integer.valueOf(param).intValue() < Integer.MAX_VALUE )
                        {
                                breakBtwBonuses = Integer.valueOf(param);
                                msg("Break between bonus parts now " + breakBtwBonuses + " milliseconds.");
                        }
                        else
                                msg("Error: Cannot set " + param); // tell them that !set failed
                        }
                        catch(NumberFormatException e)
                        {
                                msg("Invalid argument.");
                                System.out.println("Someone tried to use an invalid paramter (integer) in !set");
                                e.printStackTrace();
                        }
                }
                else if(isPlaying && text.length() > 8 && text.substring(1,8).equalsIgnoreCase("correct"))
                {
                        int spaceIndex = text.indexOf(" ", 9);
                        int points = Integer.valueOf(text.substring(spaceIndex,text.length()).trim());
                        String playerName = text.substring(9,spaceIndex).trim();
                        Player tempPlayer = getPlayer(playerName);
                        // getMaxCorrect prevents overflows or other oddities from occurring
                        if(tempPlayer != null && points > 0 && points <= Player.getMaxCorrect())
                        {
                                Team tempTeam = getTeam(tempPlayer.getTeam());
                                // adds points to the player and the team
                                tempTeam.addScore(points);
                                tempPlayer.addScore(points);
                                tempPlayer.correct();
                                // If they were the negger, unneg them.
                                if(tempPlayer == curNegger)
                                        tempPlayer.unneg();
                                msg(tempPlayer.getName() + " was correct. " + Integer.toString(points) +
                                                 " points given.");
                                
                                // If bonuses are enabled, give the person a bonus
                                if(useBonuses && !answeringBonus)
                                {
                                        // Don't increase the round number, so re-read that tossup
                                        // So long as no one has already answered...
                                        if (!teams[0].hasGuessed() && !teams[1].hasGuessed())
                                                roundNum--;
                                        
                                        //Give this team a bonus
                                        stopQuestion();
                                        bonusTeam = tempTeam;
                                        startBonus = true;
                                        forceBonus = true; //Force a bonus to be read
                                        questionState = TBot.START_BONUS;
                                }
                                // TODO: Get rid of useBonuses check?
                                else if (useBonuses && answeringBonus)
                                        msg("Already asking a bonus!  If the wrong team is answering a bonus, use !kill to end the bonus and try again.");
                        }
                        else if(tempPlayer == null)
                                msg("Player " + playerName + " does not exist");
                        else
                                msg("Invalid score to add with !correct");
                }
                // bcorrect corrects bonus parts
                else if (isPlaying && text.length() > 9 && text.toLowerCase().startsWith("!bcorrect"))
                {
                        if(useBonuses)
                        {
                                int spaceIndex = text.indexOf(" ", 10);
                                int points = Integer.valueOf(text.substring(spaceIndex,text.length()).trim());
                                String teamName = text.substring(10, spaceIndex).trim();
                                Team tempTeam = getTeam(teamName);
                                if(tempTeam.getName().equals(teamName) && points > 0 && points <= BonusQuestion.getMaxPoints())
                                {
                                        tempTeam.addBonusPoints(points);
                                        tempTeam.addScore(points);
                                        msg(tempTeam.getName() + " was correct; they were given " + 
                                                        Integer.toString(points) + " points.");
                                }
                                else if(!tempTeam.getName().equals(teamName))
                                        msg("Team " + teamName + " doesn't exist.");
                                else
                                        msg("Invalid score to add with !bcorrect");
                        }
                        else
                                msg("Bonuses are not enabled.");
                }

                else if(isPlaying && text.length() > 10 && text.substring(1,10).equalsIgnoreCase("incorrect"))
                {
                        int points = -1 * Integer.valueOf(text.substring(text.indexOf(" ",11),text.length()).trim());
                        String playerName = text.substring(11,text.indexOf(" ",11)).trim();
                        Player tempPlayer = getPlayer(playerName);
                        if(tempPlayer != null && points < 0 && points >= -1*Player.getMaxCorrect()) // getMaxCorrect prevents overflows or other oddities from occurring
                        {
                                Team tempTeam = getTeam(tempPlayer.getTeam());

                                // subtracts points to the player and the team
                                tempTeam.addScore(points);
                                tempPlayer.addScore(points);
                                tempPlayer.incorrect();
                                if(tempPlayer == curNegger) // for double !corrects
                                        tempPlayer.neg();
                                msg(tempPlayer.getName() + " was incorrect; " + Integer.toString(-1*points) + 
                                                " points were taken away.");
                        }
                        else if(tempPlayer == null)
                                msg("Player " + playerName + " does not exist");
                        else
                                msg("Invalid score to use with !incorrect");
                }
                else if(isPlaying && text.length() > 11 && text.substring(1,11).equalsIgnoreCase("bincorrect"))
                {
                        if(useBonuses)
                        {
                                int spaceIndex = text.indexOf(" ",12);
                                int points = -1 * Integer.valueOf(text.substring(spaceIndex,text.length()).trim());
                                String teamName = text.substring(11,spaceIndex).trim();
                                Team tempTeam = getTeam(teamName);
                                if(tempTeam.getName().equals(teamName) && points < 0 && points >= -1*BonusQuestion.getMaxPoints())
                                {
                                        tempTeam.addBonusPoints(points);
                                        tempTeam.addScore(points);
                                        msg(tempTeam.getName() + " was incorrect; " + Integer.toString(points) +
                                        " points were taken away.");
                                }
                                else if(!tempTeam.getName().equals(teamName))
                                        msg("Team " + teamName + " does not exist.");
                                else
                                        msg("Invalid score to use with !bincorrect");
                        }
                        else
                                msg("Bonuses are not enabled");
                }
                
                // loads a set
                else if(text.length() > 8 && text.substring(1,8).equalsIgnoreCase("loadset"))
                {
                        qSetFile = text.substring(8,text.length()).trim();
                        msg("New set: " + qSetFile);
                        loadSet(qSetFile);
                        questionNum = questions.size();         // prevents an IndexOutofBounds error
                }

                //Load bonuses
                else if (text.toLowerCase().startsWith("!loadbonuses") && text.length() > 13)
                {
                        bSetFile = text.substring(13, text.length()).trim();
                        msg("New bonus set: " + bSetFile);
                        loadBonuses(bSetFile);
                        bonusNum = bonuses.size();
                }

                // skips the rest of a question
                else if(text.equalsIgnoreCase("!kill") && !questionRight)
                {
                        stopQuestion();
                        if (useBonuses && answeringBonus)
                        {
                                //End the bonus
                                answeringBonus = false;
                                bonusOver = true;
                                startBonus = false;
                                msg("Bonus killed.");
                        }
                        else
                        {
                                msg("Question " + Integer.toString(roundNum) + " killed.");
                                msg("Possible Answers: " + curquestion.getAnswersString() +
                                                " (ID: " + Integer.toString(curquestion.getQuestionNum()) +  " " + curquestion.getQuestionSet() + ")");
                        }
                }
                else if (text.toLowerCase().startsWith("!givebonus") && text.length() > 11)
                {
                        if (useBonuses)
                        {
                                if (!answeringBonus)
                                {
                                        String giveTeam = text.substring(11);
                                        for (int i=0; i<teams.length; i++)
                                        {
                                                if (teams[i].getName().equalsIgnoreCase(giveTeam))
                                                {
                                                        //Give this team a bonus
                                                        stopQuestion();
                                                        bonusTeam = teams[i];
                                                        startBonus = true;
                                                        roundNum--; //Don't increase the round number... maybe need to decrease by 2 in certain situations
                                                        return;
                                                }
                                        }
                                        //If we reached this point, no team was found
                                        msg("Invalid team name (" + giveTeam + ") specified.");
                                }
                                else
                                        msg("Already asking a bonus!  If the wrong team is answering a bonus, use !kill to end the bonus and try again.");
                        }
                        else
                                msg("Bonuses are not enabled.");
                }
        }
        

        private void opCommands(String channel, String sender, String login, String host, String text)
        {
                // Exit now disconnects and disables reconnecting
                if(text.equalsIgnoreCase("!exit"))
                {
                        setAttemptReconnect(false);
                        disconnect();
                }

                else if(text.equalsIgnoreCase("!clearcom"))
                        comment.clear();

                // loads a player
                else if(text.length() > 12 && text.substring(1,11).equalsIgnoreCase("loadplayer"))
                        if(getPlayerFromAllPlayers(text.substring(11,text.length()).trim()) == null)
                        {
                                Player player = new Player(text.substring(11,text.length()).trim());
                                allPlayers.add(player);
                                msg("Player added.");
                        }
                        else
                                msg("Player already in Players list.");
                else if(text.length() > 5 && text.substring(1,4).equalsIgnoreCase("set"))
                {
                        String[] arguments = text.split(" "); // this should create an array with size 3
                        if(arguments.length < 3) // make sure it's formatted properly
                        {
                                msg("Error: improper !set command.");
                                return;
                        }
                        String set = arguments[1];
                        String param = arguments[2];
                        if(set.equalsIgnoreCase("messageDelay") && Integer.valueOf(param).intValue() > 0
                                        && Integer.valueOf(param).intValue() < Integer.MAX_VALUE)
                        {
                                setMessageDelay(Long.valueOf(param));
                                msg("Message delay is now: " + Long.toString(getMessageDelay()) + " milliseconds.");
                        }
                }
                else if(text.equalsIgnoreCase("!loadqset") || text.equalsIgnoreCase("!loadqsets"))
                {
                        initSets();
                        msg("qSets file reloaded.");
                }
        }
        
        //------------startJoining(): starts the joining process--------------//
        protected void startJoining(String text)
        {
                start(text);                                    //call the start method to set up team and game info
                msg("Joining period begins!  " + Integer.toString(timeToJoin) + " seconds!");   //alert the channel members of insuing game
                joining = new Join(1,text);                     //make a new joining object to accomodate joining
                joining.start();
                gen.setSeed(System.nanoTime() + gen.nextLong());  //reseed the randomizer
        }

        // Adds a player to a team
        protected void addToTeam(String sender, String text)
        {
                // A player can join a team if it's during the joining period or if the player hasn't been in the game yet
                if(isJoining || isPlaying && getPlayer(sender) == null)
                {
                        String temp = text.substring(5,text.length()).trim().toLowerCase();
                        Player player;
                        if(getPlayerFromAllPlayers(sender) != null)
                        {
                                player = getPlayerFromAllPlayers(sender);
                                player.setTeam(temp);
                        }
                        else
                                player = new Player(sender,temp);

                        // Make sure the team they're joining is valid, // and make sure they're not on another team. --> No longer valid!
                        for(int i = 0; i < teams.length; i++)
                        {
                                if(temp.equals(teams[i].getName().toLowerCase()) && teams[i].size() < playersPerTeam)
                                {
                                        teams[i].addPlayer(player);
                                        teams[1-i].removePlayer(player);        // Gets the other team; we've assumed that there are only two teams in this case
                                        players.add(player);
                                        allPlayers.add(player);
                                        msg(sender + " is on Team " + teams[i].getName());
                                        
                                        // First person to join is the gamemaster
                                        if(players.size() == 1)
                                        {
                                                gamemaster = player;
                                                msg(player.getName() + " is the Gamemaster.");
                                        }
                                        return;
                                }
                        }

                        if(!temp.equalsIgnoreCase(teams[1].getName()) && !temp.equalsIgnoreCase(teams[0].getName()))
                                msg(sender + ": That team does not exist.");
                        else
                                msg(sender + ": Too many people on team.");
                }
        }

        // Keeps track of nick changes
        protected void onNickChange(String oldNick, String login, String hostname, String newNick)
        {
                // If the player exists, add his/her name to the game
                if(getPlayer(oldNick) != null)
                {
                        getPlayer(oldNick).setName(newNick);
                        msg(oldNick + " is now " + newNick);
                }
        }

        protected void startAnswer(String sender, String text )
        {
                // answerTime originally after msg
                answerTime = answerDelay;
                answerer = sender;
                msg(sender + ", your answer:");
                cdTimer = new Countdown();
                cdTimer.start();
        }

        //start(String text): parses team name and start data; uses default if none available//
        //format should be as follows: !start(team1name, team2name, numPeoplePerTeam, numQuestions)
        // TODO: Refactor this code.
        protected void start(String text)
        {
                //if teams' names and other start data not available
                //we use the default ones
                if(text.equalsIgnoreCase("!start"))
                        // TODO: Replace magic value with setting or constant
                        //Adds the most current set file
                        // this uses some magic values: 24 is the current rounds value in settings
                        //text += " Home,Away,24,5,.*,(1){0},"+qSetFile;        //Adds the most current set file
                        text += " " + teams[0].getName() + "," + teams[1].getName() + "," + rounds +
                                        "," + playersPerTeam + "," + allowedQ.pattern() + "," + unallowedQ.pattern() +
                                        qSetFile;

                // Trims the text we're working with
                text = text.substring(7,text.length()).trim();

                // set up the default parameters
                // Set up all of the params beforehand
                teams[0].setName("Home");
                teams[1].setName("Away");
                int curpos = 0;
                int numCommas = 0;
                ArrayList<String> params = new ArrayList<String>();

                // We convert all of the values to Strings for uniformity
                // Also, we get all but the last four characters of qSetFile because we do not want the ".txt" portion of it, as that will get added on at the end
                // Note: we set allowedQ to anything and unallowedQ to nothing for the first start
                params.add(teams[0].getName());
                params.add(teams[1].getName());
                params.add(new Integer(rounds).toString());
                params.add(new Integer(playersPerTeam).toString());
                params.add(".*");
                params.add("");
                params.add(qSetFile);

                while(text.indexOf(",",curpos) >= 0 && curpos >= 0 && numCommas < params.size())
                {
                        // If the parameter is not null, then replace the default value with the parameter
                        if(!text.substring(curpos,text.indexOf(",",curpos)).trim().equals(""))
                                params.set(numCommas,text.substring(curpos,text.indexOf(",",curpos)).trim());
                        // Get the position of the next parameter
                        curpos = text.indexOf(",", curpos) + 1;
                        numCommas++;
                }
                // Adds the final parameter
                if(numCommas < params.size() && !text.substring(curpos,text.length()).trim().equals(""))
                        params.set(numCommas,text.substring(curpos,text.length()).trim());

                // Get the values from params and assign them to their proper values.
                teams[0].setName(params.get(0));
                teams[1].setName(params.get(1));
                // If the user enters an invalid input, then rounds and playersPerTeam goes back to its previous value
                try{
                        rounds = new Integer(params.get(2)).intValue();
                } catch(NumberFormatException e){}
                try{
                        playersPerTeam = new Integer(params.get(3)).intValue();
                }catch(NumberFormatException e){}
                String oldAllowedQ = allowedQ.pattern();
                String oldUnallowedQ = unallowedQ.pattern();
                allowedQ = Pattern.compile(params.get(4),Pattern.CASE_INSENSITIVE);
                unallowedQ = Pattern.compile(params.get(5),Pattern.CASE_INSENSITIVE);
                if(!allowedQ.pattern().equals(oldAllowedQ) || !unallowedQ.pattern().equals(oldUnallowedQ))
                {
                        loadSet(qSetFile);
                        loadBonuses(bSetFile);
                }
                qSetFile = params.get(6);

                // fixes invalid inputs for playersPerTeam and rounds
                if(playersPerTeam < 1)
                        playersPerTeam = 5;
                if(rounds < 1)
                        rounds = 30;
                if(rounds > questions.size())
                        rounds = questions.size();
                if(useBonuses && rounds > bonuses.size())
                        rounds = bonuses.size();
                if(teams[0].getName().equalsIgnoreCase(teams[1].getName()))
                {
                        msg("Team names too similar: reverting back to Home and Away.");
                        teams[0].setName("Home");
                        teams[1].setName("Away");
                }

                halftime = rounds / 2;
        }

        // Stops the game by resetting all of the day to its default state
        private void stopGame()
        {
                // Stop the current question, then clear everything else.
                stopQuestion();
                isPlaying = false;
                isJoining = false;
                paused = false;
                answeringBonus = false;
                bonusOver = false;
                startBonus = false;
                bonusPaused = false;
                roundNum = 0;
                teams[0].clear();
                teams[1].clear();
                gameswoReload++;
                bonusTeam = null;

                for(Player temp : players)
                        temp.clearStats();
                
                gamemaster = null;
                players.clear();
        }

        private void stopQuestion()
        {
                questionRight = true;
                finishedReading = true;
                hasNegged = false;
                answerer = "";
                answerTime = 0;
                numGuessed = 0;
                
                // End the round (question) now, and stop the countdown timer
                questionTime = seconds-1;
                cdTimer = null;
                curNegger = null;
                prompts.clear();
        }

        // This method is called after every incorrect answer
        // it resets the answering variables (answerer, answerTime), and it sets the scores if there are negs
        public void incorrectAnswer(String sender)
        {
                // NEW ADDITION: kills the timer.  Hopefully stops bug where it goes to the next person who buzzed?
                // TODO: Check to see if this is the source of the new NullPointerException.  tempPlayer
                        // might also be the source.
                cdTimer = null;
                answerer = "";
                answerTime = 0;
                // We make an assumption that sender is a player, since he had to buzz in and answer
                Player tempPlayer = getPlayer(sender);
                // if we're playing with negs, subtract five from the team score
                if(!answeringBonus && negs && !finishedReading && !hasNegged)
                {
                        // if we use !correct, then if that person is curNegger we can fix their neg
                        tempPlayer.addScore(-5);
                        tempPlayer.neg();
                        curNegger = tempPlayer;
                        getTeam(tempPlayer.getTeam()).addScore(-5);
                        hasNegged = true;
                }

                // Set "Has answered" to true
                getTeam(tempPlayer.getTeam()).setGuessed(true,answerer);
                numGuessed++;
                
                // Both people guessed wrong
                if(numGuessed == teams.length || answeringBonus)
                        everyoneWrong();
        }
        
        // Check to see if the text is already in prompts
        private boolean inPrompts(String text)
        {
                for(Pattern p : prompts)
                        // There won't be many previous prompts, so we can keep rebuilding the regexes
                        if(p.matcher(text).matches())
                                return true;
                return false;
        }

        private void everyoneWrong()    // call this when both teams run out of time or get it wrong
        {
                stopQuestion();

                Question tempQuestion;
                if (answeringBonus)
                {
                        tempQuestion = curbonus.getBonusPart(curBonusPart);
                        msg("The correct possible answers: " + tempQuestion.getAnswersString() +
                                        " (ID: " + Integer.toString(curbonus.getQuestionNum()) + " " + curbonus.getQuestionSet() + ")");
                }
                else
                {
                        tempQuestion = curquestion;
                        msg("The correct possible answers: " + tempQuestion.getAnswersString() +
                                        " (ID: " + Integer.toString(tempQuestion.getQuestionNum()) + " " + tempQuestion.getQuestionSet() + ")");
                }
        }
        
        // Give the appropriate information
        @Override
        protected void onConnect()
        {
                joinChannel(botChannel);
                // TODO: Replace with identify() once that starts working
                sendMessage("nickserv","identify " + botPass);
        }
        
        // Used by TGUI to force a bot to quit
        public void setAttemptReconnect(boolean recon)
        {
                attemptReconnect = recon;
        }
        
        // Prints out the error in the terminal and on IRC (if possible), and then
        // forcibly disconnects itself.
        protected void disconnectError(String message)
        {
                System.out.println(message);
                msg(message);
                setAttemptReconnect(false);
                disconnect();
        }

        // reconnect if TriviaBot was disconnected  --> does not work sometimes.
        protected void onDisconnect()
        {
                // attemptReconnect allows us to "kill" the bot from other processes
                if(attemptReconnect)
                {
                        // reconnect if kicked
                        try{
                                reconnect();
                                joinChannel(botChannel);
                                sendMessage("nickserv","identify " + botPass);
                        }
                        // If the nickname is already in use, ghost it, and rejoin
                        catch(NickAlreadyInUseException e)
                        {
                                sendMessage("nickserv","ghost " + getName() + " " + botPass);
                                try{
                                        reconnect();
                                }
                                catch(Exception ie){
                                        ie.printStackTrace();
                                }
                        }
                        // If we get some other error (such as an inability to connect to the server), 
                        // we log it and close TriviaBot.
                        catch(IrcException e)
                        {
                                e.printStackTrace();
                                writeErrorAndQuit(e.toString());
                        } 
                        catch (IOException e2) {
                                e2.printStackTrace();
                                writeErrorAndQuit(e2.toString());
                        }
                }
        }
        
        private void writeErrorAndQuit(String err)
        {
                BufferedWriter filer;
                try {
                        filer = new BufferedWriter(new FileWriter("errlog.txt",true));
                        // Write the date
                        Date day = new Date();
                        write(filer,day.toString());
                        // Now go through the error and record it
                        int i = 0;
                        while(err.indexOf("\n",i) != -1)
                        {
                                write(filer,err.substring(i,err.indexOf("\n",i)));
                                i = err.indexOf("\n",i)+1;
                        }
                        filer.newLine();
                        filer.flush();
                        filer.close();
                        // Disconnect is failing, so don't call disconnectError
                        System.exit(1);
                } catch (IOException e) {
                        e.printStackTrace();
                        // Disconnect is failing, so don't call disconnectError
                        System.exit(2);
                }
        }

        //** Rejoin the channel if its kicked
        protected void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname, String recipientNick, String reason)
        {
                joinChannel(botChannel);
        }

        //** Here so that the bot stops playing the game when the only real person quits.  Assume that dummy isn't a real person.
        protected void onQuit(String sourceNick, String sourceLogin,String sourceHostname, String reason)
        {
                if(isPlaying)
                        stillPlayers(sourceNick);
        }

        // If someone parts, then we may have no more players left.  We check this by calling onQuit
        protected void onPart(String channel, String sender, String login, String hostname)
        {
                if(isPlaying)
                        stillPlayers(sender);
        }

        // Checks to see if anyone's still playing; if they are, don't stop
        // nick is the nickname of the person who left
        protected void stillPlayers(String nick)
        {
                Player rem = getPlayer(nick);

                // If the player exists and is the gamemaster, then appoint a new gamemaster.
                if(rem != null && gamemaster.equals(getPlayer(nick)) )
                {
                        // If we removed the gamemaster, find a new one.
                        // Note that there must always be a gamemaster; otherwise, there
                        // are no people in the room.
                        // Remove the gamemaster
                        removePlayer(rem);
                        for(Player temp : players)
                        {
                                // If there's still a player that's in the room, don't stop the game
                                // Make sure that player isn't the gamemaster
                                if(retrieveUser(temp.getName()) != null ) 
                                {
                                        //possgm = temp;
                                        gamemaster = temp;
                                        msg(gamemaster.getName() + " is now the Gamemaster.");
                                        return;
                                }
                        }
                        // call stop method if there's no one left
                        msg("No more players in room... stopping game.");
                        stopGame();
                        joining = null;
                        cdTimer = null;
                }
        }

        // Note: removePlayer expects a valid player (non-null)
        private void removePlayer(Player p)
        {
                updateStats(p);
                getTeam(p.getTeam()).removePlayer(p);
                players.remove(p);
        }

        private Player getPlayer(String name)
        {
                for(Player temp : players)
                        if(temp.getName().equalsIgnoreCase(name))
                                return temp;
                // If player doesn't exist, indicate that by returning null
                return null;
        }

        private Player getPlayerFromAllPlayers(String name)
        {
                for(Player temp : allPlayers)
                        if(temp.getName().equalsIgnoreCase(name))
                                return temp;
                return null;
        }

        // TODO: Make more general in future by looping through array of teams.
        // TODO: Make sure player with the given name exists.
        private Team getTeam(String name)
        {
                if(teams[0].getName().toLowerCase().equals(name.toLowerCase()))
                        return teams[0];
                return teams[1];
        }
        
        private boolean isOp(String name)
        {
                return retrieveUser(name) != null && retrieveUser(name).isOp();
        }

        //Finds the user given the UserList
        public User retrieveUser(String name)
        {
                User[] users = getUsers(botChannel);
                for(int i = 0; i < users.length; i++)
                {
                        if(users[i].getNick().equalsIgnoreCase(name))
                                return users[i];
                }
                return null;
        }
        
        // Ask question picks a question from the set and splits it up into pieces
        // that can be read over IRC.  It also reads the entire question.
        // Might be wise to split it up into two different helper methods
        // (depending on the method used to split the question)?
        public void askQuestion()
        {       
                if (inOrder == false)
                        questionNum = gen.nextInt(questions.size());
                else
                {
                        // TODO: Thoroughly test this method
                        questionNum = roundNum + startIndex;
                        if (questionNum >= questions.size())
                        {
                                questionNum = gen.nextInt(questions.size()); //Just get a random question
//                              msg("No more questions.  Game ends in a tie.");
//                              maingame.endOfGame();
                        }
                }
                curquestion = questions.get(questionNum);
                if(curquestion.getTopic().length() > 13 && curquestion.getTopic().substring(0,13).equalsIgnoreCase("computational"))
                        seconds = MATHRND;
                else
                        seconds = NORMRND;
                
                // first line just temporary.  Next two lines start the game
                // if showCategory is false, then cut off the category part of the question.
                String question = curquestion.toString().trim();
                if(!showCategory)   
                        question = question.substring(question.indexOf(".")+1,question.length());

                finishedReading = false;
                
                // Stop if we're no longer playing
                if(!isPlaying) {
                        stopQuestion();
                        return;
                }

                if(askBySent)
                {
                        int curSentence = 0;
                        String[] sentences = question.split("\\. |\\? ");
                        String punctuation = getPunctuation(question);
                        int punctLoc = 0;

                        if(sentences.length == 0)
                                System.out.println("Split is the source of the NullPointerException");

                        while(curSentence < sentences.length)
                        {
                                TBot.maingame.checkPause();
                                if(answerTime == 0 && !questionRight)
                                {
                                        // Display the sentence and it's corresponding punctuation mark
                                        msg(sentences[curSentence] + punctuation.substring(punctLoc, punctLoc+1) );

                                        try{Thread.sleep(sentences[curSentence].length() * delay);}
                                        catch(Exception e){System.out.println("Exception in askQuestion/askBySent: " + e);}

                                        if(answerTime == 0 && !questionRight)
                                        {
                                                ++curSentence;
                                                ++punctLoc;
                                        }
                                }
                                if(questionRight || !isPlaying)
                                        break;
                        }
                }
                // Mike Bentley's askQuestion method; modified to work with the current version of the bot
                else
                {
                        String category = "";
                        if(showCategory)                // if we're showing the category, add a space
                        {
                                category = question.substring(0,question.indexOf(".")+1);
                                msg(category);
                                question = question.substring(question.indexOf(".")+1,question.length());
                                try{Thread.sleep(category.length() * delay);}catch(Exception e){}
                        }

                        int curWord = 0;
                        String[] words = question.split(" ");
                        if(words.length == 0)
                                System.out.println("Split is the source of the NullPointerException");

                        while(curWord < words.length)
                        {
                                TBot.maingame.checkPause();
                                if(answerTime == 0 && !questionRight)
                                {
                                        int maxWords = curWord + numWordsPerLine;
                                        if (maxWords >= words.length)
                                                maxWords = words.length;
                                        
                                        String curQ = "";
                                        for (int i=curWord; i<maxWords; i++)
                                                curQ = curQ + words[i] + " ";

                                        if(answerTime == 0 && !questionRight)
                                                curWord = curWord + numWordsPerLine;

                                        msg(curQ.trim());
                                        try
                                        {
                                                Thread.sleep(curQ.length() * delay);
                                        }
                                        catch(Exception e)
                                        {
                                                System.out.println("Exception in askQuestion/!askBySent: " + e);
                                                e.printStackTrace();
                                        }
                                }
                                if(questionRight || !isPlaying)
                                        break;
                        }
                }
                finishedReading = true;
        } // end of askQuestion

        // Returns a string of punctuation characters in the order in which they
        // appear.  Used in askQuestion when askBySent is true.
        // TODO: Make punctuation a list of characters instead of magic variables.
        private static String getPunctuation(String q)
        {
                // For the moment, punctuation is only . or ?
                String retString = "";
                for(int i = 0; i < q.length()-1; ++i)
                        // Make sure there's a space after the punctuation to prevent breaking
                        // up abbreviations.
                        if(q.substring(i,i+2).equals(". ") || q.substring(i,i+2).equals("? "))
                        {
                                retString += q.charAt(i);
                                ++i;
                        }

                // split doesn't take away the last punctuation mark, but an StringIndexOutOfBounds error
                // occurs if nothing is added to retString, so we'll just add a space.
                retString += " ";

                return retString;
        }

        // Asks bonuses up until the bonus is over
        public void askBonus()
        {
                if (curBonusPart == 0)
                {
                        //Ask the entire question for the particular part of this bonus
                        if (inOrder == false)
                                bonusNum = gen.nextInt(bonuses.size());
                        else
                        {
                                bonusNum = roundNum + startIndex;
                                 //Just get a random question
                                if (bonusNum >= bonuses.size())
                                        bonusNum = gen.nextInt(bonuses.size());
                        }
                        curbonus = bonuses.get(bonusNum);
                        bonuses.remove(bonusNum);
                        finishedReading = false;

                        HashSet<Player> tempPlayers = bonusTeam.getPlayers();
                        Iterator<Player> i = tempPlayers.iterator();
                        String playersString = "[";
                        while (i.hasNext())
                        {
                                Player p = i.next();
                                if (i.hasNext())
                                        playersString += p.getName() + ", ";
                                else
                                        playersString += p.getName() + "]";
                        }
                        if (playersString.equals("["))
                                playersString = "[]";

                        String bonusTeamText = "Bonus for " + bonusTeam.getName() + " Team " + playersString + ":";
                        msg(bonusTeamText);
                        try{Thread.sleep(bonusTeamText.length() * delay);}catch(Exception e){}

                        //Optionally show the question category
                        if(showCategory)                
                        {
                                // if we're showing the category, add a space
                                String category = curbonus.getTopic().trim();
                                msg("Category: " + category);
                                try{Thread.sleep(category.length() * delay);}catch(Exception e){}
                        }

                        //Now read the leadin
                        msg(curbonus.getLeadin());
                        questionRight = false; //Allow buzzes now
                        // Fixes a bug when correct is used between questions which makes
                        // questionTime continue to increment
                        questionTime = 0;
                        try{Thread.sleep(curbonus.getLeadin().length() * delay);}catch(Exception e){}

                }

                if (answerTime == 0 && !questionRight)
                {               
                        //Don't read stuff if someone buzzed already
                        Question bonusPart = curbonus.getBonusPart(curBonusPart);
                        String bonusString = "";
                        bonusString = bonusPart.getQuestion();
                        
                        //Read the question in normal circumstances
                        if (curBonusPart != 0 || readFirstBonusPart)
                                msg(bonusString);
                        
                        //Delay to give teams time to read the question
                        //It's probable that slowness in between bonuses is due to this holding us up after a correct answer
                        try{Thread.sleep(bonusString.length() * (delay / 2));}catch(Exception e){}

                        // Give teams time to answer
                        finishedReading = true;
                        seconds = BONUS_TIME;
                }
        }


        // Determines who is the mvp and lvp
        public void MvpLvp()
        {
                // mvp/lvp score will be set during the first iteration
                int mvpscore = 0;
                int lvpscore = 0;
                int score;
                ArrayList<Player> lvpList = new ArrayList<Player>();
                ArrayList<Player> mvpList = new ArrayList<Player>();
                
                for(Player player : players)
                {
                        // computes the player's score.  If it is greater than or matches the mvp's score, (s)he is added to the MVP list.
                        // If it is lower than or matches the lvp's score, (s)he is added to the LVP list.
                        // Formula: 10 * correct - 5 * guessed
                        score = 10 * player.getCurCorrect() - 5 * player.getCurNegs();
                        if(lvpList.size() > 0)
                        {
                                if(score >= mvpscore)
                                {
                                        if(score > mvpscore)
                                                mvpList.clear();
                                        mvpList.add(player);
                                        mvpscore = score;
                                } 
                                else if(score <= lvpscore)
                                {
                                        if(score < lvpscore)
                                                lvpList.clear();
                                        lvpList.add(player);
                                        lvpscore = score;
                                }
                        }
                        // If it's the first person being checked, they get put on both lists.
                        else
                        {
                                lvpList.add(player);
                                mvpList.add(player);
                                lvpscore = score;
                                mvpscore = score;
                        }
                }
                
                // Displays the MVPS and LVPS of the game.
                for(Player player : mvpList)
                {
                        player.mvp();
                        msg("MVP: " + player.getName() + " on team " + player.getTeam() + " with " + Integer.toString(mvpscore) + " points.");
                }
                for(Player player: lvpList)
                {
                        player.lvp();
                        msg("LVP: " + player.getName() + " on team " + player.getTeam() + " with " + Integer.toString(lvpscore) + " points.");
                }
        }

        private static void updateStats(Player p)
        {
                // TODO: Find a way to switch teams so that it doesn't require making
                // them play a game to do so.
                p.playedGame();
                p.updateStats();
                BufferedWriter filer;
                try{
                        filer = new BufferedWriter(new FileWriter(statsDirectory + p.getName() + ".txt",false));
                        int[] writevars = {p.getGuessed(), p.getCorrect(), p.getGames(),
                                p.getWins(), p.getMVP(), p.getLVP(), p.getNegs()};
                        for(int i = 0; i < writevars.length; i++)
                                write(filer,Integer.toString(writevars[i]));
                        filer.close();
                }
                catch(IOException e){
                        e.printStackTrace();
                }
        }
        
        private class GameThread extends Thread
        {
                public GameThread(int rnum)
                {}

                // The main game.
                public void run()
                {
                        // Makes the bot read at least one round (question); fixes a pause bug
                        askQuestion();

                        // Makes the first question have a countdown: the -1 becomes 0 when it goes through the first iteration
                        if(!questionRight)
                                questionTime = -1;

                        //If there's a game, play the round (question).  If there's no game, do nothing.
                        while(isPlaying)
                        {

                                // Adds one second to the round (question) timer.
                                questionTime++;

                                // This is the whole answer delay bit, when it's after askQuestion()
                                //
                                // Code that handles if a player has run out of time to answer the question
                                // after buzzing in.
                                if(answerTime == 1)
                                { //We've run out of time to answer a question after buzzing

                                        Player tempPlayer = getPlayer(answerer);
                                        msg(answerer + " [" + tempPlayer.getTeam() + "] ran out of time.");
                                        answerer = "";
                                        answerTime = 0;

                                        // We make an assumption that sender is a player, since he had to buzz in and answer
                                        // if we're playing with negs, subtract five from the team score
                                        if (!answeringBonus)
                                        {
                                                if(negs && !finishedReading)
                                                {
                                                        tempPlayer.addScore(-5);
                                                        getTeam(tempPlayer.getTeam()).addScore(-5);
                                                }

                                                // Set "Has answered" to true
                                                getTeam(tempPlayer.getTeam()).setGuessed(true,answerer);
                                                numGuessed++;
                                                
                                                // Both people guessed wrong
                                                if(numGuessed == teams.length)
                                                        everyoneWrong();
                                        }
                                        else
                                        {
                                                //This bonus part was incorrect
                                                //Automatically move to the next bonus part
                                                endOfQuestion();
                                                checkPause();
                                        }
                                }
                                //Player is still answering
                                else if(answerTime > 1)
                                        questionTime--;
                                
                                // This part warns players when time is running out.
                                else if(questionTime + 10 == seconds && answerTime == 0)
                                                msg("10 seconds left");
                                else if(questionTime + 5 == seconds && answerTime == 0)
                                                msg("5 seconds left.");

                                //** END OF THE ROUND (i.e. question)
                                if(questionTime >= seconds)
                                {
                                        endOfQuestion();
                                        checkPause();   // see if we've paused for the moment
//                                      if(forceMode && bonusTeam == null)
//                                      {
//                                              bonusPause();
//                                              checkBonusPause();
//                                      }
                                        
                                        //** END OF THE GAME... unless there's a tie
                                        // Check here so that games don't end on a neg
                                        // The second part ends when there's no more questions
                                        if(roundNum >= rounds + 1 && teams[0].getScore() != teams[1].getScore() || (questions.size() <= 1 && !answeringBonus))
                                        {
                                                //If there are no bonuses, check if the game is over since this tossup was read
                                                if (!useBonuses)
                                                        endOfGame();
                                                else if (bonusOver || !startBonus)
                                                        //bonus is done, or no one got the last tossup
                                                        endOfGame();
                                        }
                                }

                                // make the thread sleep for a second
                                try{Thread.sleep(1000);}catch(Exception e){}
                        } // end of while
                } // end of run

                // this pauses and resumes the thread; following example from http://www.devarticles.com/c/a/Java/Multithreading-in-Java/9/
                public void pause()
                {
                        paused = true;
                }

                // needs to be synchronized in order to avoid an IllegalMonitorException
                public synchronized void unpause()
                {
                        paused = false;
                        notify();
                }

                public synchronized void checkPause()
                {
                        if(paused)
                                msg("Paused.");
                        
                        // needed for wait
                        try{
                                synchronized(this)
                                {
                                        while(paused)
                                                this.wait();
                                }
                        }
                        catch(Exception e){
                                msg(e.toString() + " -- pause failed.");
                                e.printStackTrace();
                        }
                }

                // TODO: Merge this with the pause methods?
                public void bonusPause()
                {
                        bonusPaused = false;
                }
                
                public synchronized void bonusUnpause()
                {
                        bonusPaused = true;
                        notify();
                }
                
                public synchronized void checkBonusPause()
                {
                        if(!bonusPaused)
                                msg("Use !next now");
                        
                        try
                        {
                                synchronized(this)
                                {
                                        while(!bonusPaused)
                                                this.wait();
                                }
                        }
                        catch(Exception e){
                                msg(e.toString() + " -- bonusPause failed.");
                                e.printStackTrace();
                        }
                }
                
                private void endOfQuestion()
                {
                        // this occurs when time runs out.
                        if(!questionRight)
                        {
                                questionRight = true;
                                finishedReading = true;
                                hasNegged = false;
                                answerer = "";
                                answerTime = 0;
                                prompts.clear();
                                if (!answeringBonus)
                                        msg("Time's up.  The correct answer(s): " + curquestion.getAnswersString() +
                                                " (ID: " + Integer.toString(curquestion.getQuestionNum()) + " " + curquestion.getQuestionSet() + ")");
                                else
                                {
                                        Question tempQuestion = curbonus.getBonusPart(curBonusPart);
                                        msg("Time's up.  The correct answer(s): " + tempQuestion.getAnswersString() +
                                                        " (ID: " + Integer.toString(curbonus.getQuestionNum()) + " " + curbonus.getQuestionSet() + ")");
                                }
                        }
                        if (answeringBonus)
                        {
                                curBonusPart++;
                                if (curBonusPart >= curbonus.getBonuses().length)
                                {
                                        //End this bonus
                                        bonusOver = true;
                                        answeringBonus = false;
                                        curBonusPart = 0;
                                        bonusTeam.increaseBonusesHeard();
                                        
                                        // Give time for corrects after a bonus is finished
                                        if(forceMode)
                                        {
                                                bonusPause();
                                                checkBonusPause();
                                        }
                                }
                        }

                        if (!useBonuses || bonusOver || (!answeringBonus && !startBonus))
                        {
                                //** HALF-TIME
                                if(roundNum == halftime)
                                {
                                        msg("Halftime!  " + halftimeLength + " second break.");
                                        // halftimeLength is in seconds, so convert to milliseconds
                                        int halftimeInMillis = halftimeLength * 1000;
                                        try{
                                                if(breakBtwQ < halftimeInMillis)
                                                        Thread.sleep(halftimeInMillis-breakBtwQ);
                                        }catch(Exception e){
                                                e.printStackTrace();
                                        }
                                }

                                //Increase the question number
                                roundNum++;
                        }
                        questionTime = 0;       // restart the timer
                        
                        // prints out the score (only do on ends of bonuses or tossups)
                        // TODO: Iterate over all the teams to make the code more general
                        if (!answeringBonus || bonusOver)
                                msg(teams[0].getName() + ": " + Integer.toString(teams[0].getScore()) + 
                                                ", " + teams[1].getName() + ": " + Integer.toString(teams[1].getScore()));

                        // Gives a break between every tossup (but not bonuses)
                        if (useBonuses && answeringBonus && !bonusOver)
                        {
                                questionState = TBot.CONTINUE_BONUS;
                                // Add some time for the reader to see the answer
                                try{Thread.sleep(breakBtwBonuses);}catch(Exception e){}
                        }
                        else if (useBonuses && startBonus)
                                questionState = TBot.START_BONUS;
                        else
                                questionState = TBot.START_TOSSUP;
                        
                        if (questionState == TBot.START_TOSSUP)
                                try{
                                        Thread.sleep(breakBtwQ);
                                }catch(Exception e){
                                        e.printStackTrace();
                                }

                        // Starts the next question
                        //Reset everyone's guesses
                        for(int i =0; i < teams.length; i++)
                                teams[i].setGuessed(false,"");
                        numGuessed = 0;

                        if (forceBonus)
                        {
                                questionState = TBot.START_BONUS;
                                forceBonus = false;
                        }

                        // Find a way to pause a question here when forceMode is on
                        // doing a while(true) loop causes java to use an absurd amount
                        // of processing, so find another way.
                        // For now, the same as !pause
                        // TODO: Replace with a similar method that pauses reading so that
                                // people can't simply just use !unpause
//                      if(forceMode)
//                              pause();
                        
                        // Choose a question if we're still playing
                        if(isPlaying)
                                chooseQuestion();
                }

                // TODO: Get rid of magic values?
                private void chooseQuestion()
                {
                        // Make sure the question isn't printed out if !stop was used
                        if (questionState == TBot.START_TOSSUP)
                        {
                                if((roundNum < rounds + 1 || teams[0].getScore() == teams[1].getScore()) && questions.size() > 1)
                                {
                                        if (!forceBonus)
                                        {
                                                // Fixed a bug caused by loadSet (loading while questionNum
                                                        // is being chosen).
                                                if(questionNum < questions.size())
                                                        questions.remove(questionNum);

                                                msg("Question #" + Integer.toString(roundNum));
                                                questionRight = false;
                                                // give 2 seconds between question # and actual question (2 seconds arbitrary)
                                                // TODO: Get rid of magic value?
                                                try{Thread.sleep(2000);}catch(Exception e){}
                                                //Reset bonus values
                                                bonusOver = false;
                                                startBonus = false;
                                                answeringBonus = false;
                                                bonusTeam = null;
                                                readFirstBonusPart = true;
                                                curBonusPart = 0;
                                                askQuestion();
                                        }
                                        else
                                                askBonus();
                                }
                        }
                        else if (questionState == TBot.START_BONUS)
                        { 
                                //Start reading a bonus
                                curBonusPart = 0;
                                answeringBonus = true;
                                bonusOver = false;
                                startBonus = false;
                                readFirstBonusPart = true; //read the first bonus part unless interupted by a buzz
                                askBonus();
                        }
                        else
                        { 
                                //Continue reading a bonus
                                questionRight = false;
                                askBonus();
                        }
                }

                private void endOfGame()
                {
                        msg("You've reached the end of the game!  Score: " + teams[0].getName() + " " + Integer.toString(teams[0].getScore()) + ", " + teams[1].getName() + " " +Integer.toString(teams[1].getScore()));
                        // Tell them who the winner is --> this will need to be modified when we increases the # of teams
                        if(teams[0].getScore() > teams[1].getScore())
                        {
                                msg(teams[0].getName() + " wins!");
                                // get the winners
                                HashSet<Player> tempSet = teams[0].getPlayers();
                                for(Player temp : tempSet)
                                        temp.wonGame();
                        }
                        else if(teams[0].getScore() < teams[1].getScore())
                        {
                                msg(teams[1].getName() + " wins!");
                                // get the winners
                                HashSet<Player> tempSet = teams[1].getPlayers();
                                for(Player temp : tempSet)
                                        temp.wonGame();
                        }
                        else
                                msg("Tied game!  Everybody loses!");    //Here for tied games
                        
                        // TODO: Refactor this code?
                        if (useBonuses)
                        {
                                msg("Bonus statistics:");
                                String ppb = "";
                                if (teams[0].getBonusesHeard() == 0)
                                        ppb = "0";
                                else
                                {
                                        ppb = Integer.toString(teams[0].getBonusPoints() / teams[0].getBonusesHeard());
                                }
                                msg(teams[0].getName() + " Team: Bonuses heard: " + teams[0].getBonusesHeard() + " Bonus points: " + teams[0].getBonusPoints() + " Points per bonus: " + ppb);
                                if (teams[1].getBonusesHeard() == 0)
                                        ppb = "0";
                                else
                                        ppb = Integer.toString(teams[1].getBonusPoints() / teams[1].getBonusesHeard());
                                msg(teams[1].getName() + " Team: Bonuses heard: " + teams[1].getBonusesHeard() + " Bonus points: " + teams[1].getBonusPoints() + " Points per bonus: " + ppb);
                        }

                        // Write stats!
                        if(players.size() >= minMVP)
                                MvpLvp();
                        msg("Writing stats...");
                        writeStats();

                        // stops the game, then kill this thread
                        stopGame();
                }

                private void writeStats()
                {
                        // Write to the stats folder
                        for(Player tempPlayer : players)
                        {
                                msg(tempPlayer.getCurStats());
                                updateStats(tempPlayer);
                        }
                        msg("Writing completed.");
                }
        } // end of GameThread class

        private class Join extends Thread
        {
                // wait for a minute for everyone to join.
                public Join(int rnum, String text)
                {}

                public void run()
                {
                        final int SECOND = 1000;        // 1000 ms in a second
                        if(answerTime == 0 && !isPlaying)
                        {
                                isJoining = true;
                                boolean cancelled = false;
                                // makes it easier to stop it
                                // CHANGE BACK TO 15 and giv
                                for(int i = 0; i < timeToJoin / 2; i++)
                                {
                                        try{Thread.sleep(SECOND);}catch(Exception e){}
                                        if(!isJoining)
                                                return;
                                }
                                // break the game if it's been stopped
                                if(!isJoining)
                                        cancelled = true;
                                else
                                {
                                        msg(Integer.toString(timeToJoin/2) + " seconds!");
                                        // makes it easier to stop it
                                        for(int i = 0; i < timeToJoin / 2; i++)
                                        {
                                                try{Thread.sleep(SECOND);}catch(Exception e){}
                                                if(!isJoining)
                                                        return;
                                        }
                                }

                                // break the game if it's been stopped
                                if(!isJoining)
                                        cancelled = true;
                                isJoining = false;
                                
                                // Check to see if each team has at least one person.  If not, end this start
                                if(players.size() > 0 && !cancelled)
                                {
                                        // load the set if gameswoReload != limwoReload, or if the set is lower than the number of rounds
                                        if(gameswoReload == limwoReload || questions.size() < rounds)// ||
                                        {
                                                loadSet(qSetFile);
                                                loadBonuses(bSetFile);
                                                gameswoReload = 0;
                                        }

                                        for(int i = 0; i < teams.length;i++)
                                        {
                                                teams[i].setScore(0);
                                                teams[i].setGuessed(false,"");
                                        }
                                        // set halftime
                                        halftime = rounds / 2;
                                        roundNum = 1;
                                        questionRight = false;
                                        msg("Let's Begin!");
                                        // Gets the first question.  If it's a math question (computational or not), make it 30 seconds
                                        //Change in the future so that it only goes 30 seconds for computational math?
                                        isPlaying = true;

                                        TBot.maingame = new GameThread(1);
                                        TBot.maingame.start();
                                }
                                else if(!cancelled)
                                {
                                        msg("Game cancelled - there must be somebody playing on a team.");
                                        for(int i = 0; i < teams.length; i++)
                                                teams[i].clear();
                                        players.clear();
                                }
                                // reset isJoining
                                isJoining = false;
                        }
                }
        } // end of Join

        // This class counts down the timer for buzzing in.  
        // This replaces the awkward/buggy overloaded Join class
        private class Countdown extends Thread
        {
                private final int SECOND = 1000;                // 1000 ms in a second
                public Countdown(){}    // constructor

                public void run()
                {
                        while(answerTime > 1)   // while they can still answer, count down.
                        {
                                answerTime--;
                                try{Thread.sleep(SECOND);}catch(Exception e){}
                                if(questionTime > 0)
                                        questionTime--;
                        }

                        // if it's gone till 1, then it's too late.
                        if(answerTime == 1)
                        {
                                msg(answerer + " [" + (getPlayer(answerer)).getTeam() + "] ran out of time.");
                                incorrectAnswer(answerer);
                                answerTime = 0;
                                cdTimer = null;
                        }
                }


        } // end of Countdown
}  // end of tbot