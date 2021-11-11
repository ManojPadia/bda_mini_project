#!/bin/bash

function download_abstract {
    if [ ! -f enwiki-latest-abstract.xml ]; then
        wget https://dumps.wikimedia.org/enwiki/latest/enwiki-latest-abstract.xml.gz
        echo 'extract abstract dataset...'
        gzip -d enwiki-latest-abstract.xml.gz
    fi
    
    if [ ! -f abstract.json ]; then
        python3 -c 'from text_extraction import process_abstract; process_abstract()'
    fi
}

function download_pages_articles {
    if [ ! -f enwiki-latest-pages-articles1.xml-p10p30302 ]; then
        wget https://dumps.wikimedia.org/enwiki/latest/enwiki-latest-pages-articles1.xml-p10p30302.bz2
        echo 'extract pages articles dataset...'
        bzip2 -d enwiki-latest-pages-articles1.xml-p10p30302.bz2
    fi
    
    if [ ! -f categories.json ]; then
        python3 -c 'from text_extraction import process_pages_articles; process_pages_articles()'
        cat categories_raw.json | grep "categories\": \[" >> categories.json
    fi
}

download_abstract &
download_pages_articles &
wait

echo 'copying results into the project'
cp abstract.json ../WikipediaTopicLabeling/data
cp categories.json ../WikipediaTopicLabeling/data

echo 'cleaning...'
rm -rf __pycache__ categories_raw.json 

