import pytest
from src.crawl import crawl, process_list

def test_crawl():
    result = crawl(1)
    assert type(result) is list

def test_process_list():
    temp_list = ['€123 per month','€1,123 per month','abcde']
    result = process_list(temp_list)
    print(result)
    assert len(result) == 2
