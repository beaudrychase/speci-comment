import glob
import json
from comment_parser import comment_parser

count = 0
for filename in glob.iglob("/home/pi/java_projects" + '**/*.java', recursive=True):
     print(filename)
     json_obj = json.dump("{'text':[]}")
     all_comments = comment_parser.extract_comments(filename, "text/x-java-source")
     for comment in all_comments:
     	json_obj[text].add(comment.text())
        with open("./results/" + str(count), 'w') as file:
            json.dump(json_obj, file)
	count += 1

