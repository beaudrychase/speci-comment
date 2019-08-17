Speci-comment

End Goal:
	
	Create a plugin of some sort that gives you real time feedback on the specificity of your comments in code with the idea being that more specific comments are better comments.
	OR
	Create a command line tool that you give your source code to and it spits out some sort of output that tells you which lines of comments are very vague. (This seems easier to do and more flexible)


Problems:
	Getting a model that can predict specificity for comments
		-[Model Already Exists](https://github.com/wjko2/Domain-Agnostic-Sentence-Specificity-Prediction#domain-agnostic-real-valued-specificity-prediction): This model was built to predict specificity of sentences. It is not domain specific. I will need a lot of unlabeled data, and some labeled data (They used mechanical turk, but that is expensive, but what else could I do.)
		- Getting Data: [Here is data](https://github.com/src-d/awesome-machine-learning-on-source-code#datasets) That link has links to a bunch of different sources of aggregated source code (with the comments in it.)
			- If the files I'm given are source code, I need to extract all of the comments. I'm thinking that a regex would do the trick to get all of the text, but I might need to do something more clever. Right now I think that line splits are going to be hard to deal with when trying to figure out where sentences are. Sometimes a line is a sentence, or there are multiple sentences per line, or the sentence is on multiple lines.
			- Labeled data: Mechanical Turk or Manually do it myself.
	Building a tool that uses the model from the command line:
		- Takes source code as input
		- Outputs lines with vague comments.


First Steps:
	- Use one of the models that already exists and get the rest of the tool working with it
	- only try to get python working. 
