from comment_parser import comment_parser
import argparse


def output(all_comments, file_name):
    
    for comment in all_comments:
        # give the comment to the model
        # get a value for the specificity
        if True:
            # print(comment._line_number)
            print("File \"{}\", Line {} ".format(file_name, comment.line_number()))
            print("\"{}\"".format(comment.text()))
            print("UnspecificCommentSuggestion: This comment ranks {0} in specificity\n".format(0))



if __name__ == "__main__":
    print("hello")
    parser = argparse.ArgumentParser(description='gets comments from source code',
                                     prog="speci-comment.py")
    parser.add_argument("input_file", type=str,
                        help="The input file")
    args = parser.parse_args()
    all_comments = comment_parser.extract_comments(args.input_file, "text/x-java-source")
    output(all_comments, args.input_file)
