from comment_parser import comment_parser
import argparse


def get_all_comments(source_text):
    print(source_text)
    return False


if __name__ == "__main__":
    print("hello")
    parser = argparse.ArgumentParser(description='gets comments from source code',
                                     prog="speci-comment.py")
    parser.add_argument("input_file", type=str,
                        help="The input file")
    args = parser.parse_args()
    all_comments = comment_parser.extract_comments(args.input_file, "text/x-java-source")
    print(all_comments)
