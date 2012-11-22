PennQBBot - A Quiz Bowl Question Reading IRC Bot based on the QBBot by Alejandro López-Lago.
Written by Saajid Moyen

Version History:

0.0.2 
- Cleaned documentation

0.0.1 
- Cloned the bot from the old repository

The QBBot was originally started on February 26, 2006 by Alejandro López-Lago.
It has since been edited by Raghav Puranmalka and Mike Bentley. 
The changelog and todos for TBot.java as of their last update is included below the class descriptions.

Class descriptions:

TGUI: Runs the GUI from which the bot is initialized.
TMain: Creates the bot.
TBot: Runs the games, asking questions and creating players/teams.
Player: Stores information about a player in the game.
Team: Stores information about the two teams in the game.
Question: Stores a question from the question set.
BonusQuestion: Stores a bonus question from the bonus set.

TBot.java changelog:

2.0.0
- Bonuses! (thanks to Mike Bentley)

2.0.1
- !stop is now a gamemaster-only command
- Make an "update qSets" function
- Change the ask by sentence code to use split instead of indexOf's.

2.0.2
- Fixed bug where the answer and score would not be displayed at the end of the game.
- Fixed bug where halftime wouldn't be updated if rounds was an improper value (in start())
- !remove now works.  Furthermore, players can now remove themselves.
- Fixed bug where !stop would show the next question's category

2.0.3
- Fixed bug where first question would always run down the countdown timer.

2.0.4
- Fixed force mode.

2.0.5
- Fixed bug where the game could be started with more rounds than bonus questions
- Fixed bug where !remove didn't work properly for the gamemaster.

2.0.6?
- loadSet now ignores case
- added additional checks for some set options.
- !get sets and !get bonussets now break up the list into separate lines
- bot now exits if settings.txt file is incomplete
- Added !default flag to revert settings back to default ones.
- loadSet catches invalid regular expressions [does not handle them completely correctly]
- If !correct, !incorrect, or !bcorrect fails, the name used is now shown.
- Made !set gamemaster case insensitive
- ???? Gamemaster is no longer removed automatically if they leave
- Using !remove now saves the players stats
- Stats directory now determined by settings.txt
- Fixed bug where game ended immediately after the last question was read if a player negged
- Added a halftimeLength variable that determines how long halftime runs for in seconds
- Modified seed to be a little less predictable
- Made MVP/LVP code more efficient
- Fixed force mode pausing issues.
- Timer now counts down correctly after !bcorrect is used

2.0.7
- Merged !bcorrect with !correct; !bcorrect now add points to teams (used for bonuses).
- Added !bincorrect
- Made !correct/!incorrect print more detailed information.
- Fixed bug where people could get unlimited prompts by adding spaces
- !loadqset now works without having to supply arguments
- ???? !stop can no longer display the next question?
- Time between bonuses can now be set with breakBtwBonuses
- Nickname, channel, password, and server are now determined by the settings file

2.0.8
- Bot now only takes "buzz" (or buzz surrounded with whitespace characters) for buzzing in.
- !start by itself now uses the same settings as the previous game
- if allowedQ/unallowedQ is different and changed as a !start parameter, the set is reloaded.
- Possibly fixed ghosting code?

TBot.java todos:

To Do:
-  use toLowerCase() before calling loadSet()
-  fixed problem caused by doing !get number
-  Create an option to run 20/20 on a regular quizbowl distribution
-  Maybe allow ops to modify questions in a set... i.e. to fix spelling, category
- Make a variable so that you can remove yourself without adding to "games played"
- Rename bcorrect/bincorrect/other bonus stuff to make more intuitive
- Scores may not be adding up correctly?
- Make password encrypted in settings.txt file
- Updated score is not shown at the end of rounds sometimes
- Redo scobowl.html to document all of the changes

- Possible speedup: Load all questions from qSets into Array/Linked Lists at start-up; then just
    add together the necessary A/L lists when loadSet() is called.  This will reduce the amount of times
    we need to read from the disk.  Remember to reload all questions if we reload qSets!  [memory trade-off]
- Fix problems with !get freq?  (Very minor, since no one uses it)
- Make bot automatically ghost itself (current code doesn't work)
- Make questions a (Abstract)List; use LinkedList when questions are in order and an ArrayList when questions are randomized.
- if you set a category (i.e. only biology questions), end a round, and then load a new set, the set will report
        it has fewer questions than it actually does... although it's fine once the game starts [minor]
- Bot started reading a question during a joining period... see why
- Make NICK and PASS not a constant

- RapidFlash: Timer shouldn't reset completely after someone buzzes and gets it wrong  --> or should it?
- Made !correct assign the bonus to the player's team  (done with !bcorrect?)
- Gaurav: The bot started a question before another was done   ---> need to find another example before this can be fixed
- Possible idea: store questions in a set as a list of lists separated by category.  Then go through
        the lists and pick out the appropriate number of questions needed.
- Add !bincorrect ?
- Make !correct give a bonus?

- GOAL FOR THE SUMMER: Make a TriviaBot+otherbots that can run a tournament on IRC!
        This requires: bonuses, updated website, ability to add replacement
        questions, and sequential question asking.

Later version?
- Allow for more than two teams
- Add error() method that logs an error.

-!start Team1,Team2,numRounds,maxPlayersPerTeam,allowedQ,unallowedQ,set

IDEAS: Log-in system, monthly stats, more than 2 teams?, keep player stats after disconnect
