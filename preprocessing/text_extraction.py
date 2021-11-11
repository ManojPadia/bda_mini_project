import os.path
import re


def clean_string(string):
    return re.sub(r'([^\s\w]|_)+', '', string).replace("\t", " ")

def process_abstract():
    print('preprocessing abstract...')
    with open('enwiki-latest-abstract.xml', 'r') as f:
        with open('abstract.json', 'w') as json:
            for line in f:
                if re.search("<title>", line):
                #  if line[0:7] == '<title>':
                    title = clean_string(line[18:-9])
                    print('{"title": "', title, '", ', sep='', end='', file=json)
                elif re.search("<abstract>", line):
                #  elif line[0:10] == '<abstract>':
                    text = clean_string(line[10:-12])
                    print('"text": "', text, '"}', file=json)

def process_pages_articles():
    print('preprocessing pages articles...')
    with open('enwiki-latest-pages-articles1.xml-p10p30302', 'r') as f:
        with open('categories_raw.json', 'w') as json:
            first = True
            for line in f:
                line = line.lstrip(' ')

                if re.search("<title>", line):
                    if not first:
                        # remove last ',' of the categories list 
                        json.seek(json.tell() - 1, os.SEEK_SET)
                        json.write('')
                        # close the line and start a new one
                        json.write(']}\n')
                        json.flush()
                    else:
                        first = False
                    title = clean_string(line[7:-9])
                    json.write('{"title": "')
                    json.write(title)
                    json.write('", "categories": [')
                if re.search("\[\[Category:", line):
                    category = clean_string(line[10:-1])
                    if category[-4::] == 'text':
                        category = category[:-4]
                    json.write(' "')
                    json.write(category)
                    json.write('",')

            json.seek(json.tell() - 1, os.SEEK_SET)
            json.write(']}')
            #  json.write('}')
            json.flush()
   

if __name__ == '__main__':
    print('preprocessing abstract...')
    process_abstract()
    print('preprocessing pages articles...')
    process_pages_articles()

