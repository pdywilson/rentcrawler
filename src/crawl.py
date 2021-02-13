#!/usr/bin/env python3
import sqlite3
    #cur.execute("SELECT * FROM sqlite_master WHERE type='table'")

def execute_sql(sql, path = '/var/db/rentcrawler/rent.db', *args):
    conn = sqlite3.connect(path)
    cur = conn.cursor()
    if args:
        cur.execute(sql, args)
        conn.commit()
    else:
        cur.execute(sql)
    return cur

def get_latest_stats(path = '/var/db/rentcrawler/rent.db'):
    sql = ''' SELECT * FROM dublinrents ORDER BY timestamp DESC LIMIT 1'''
    r = execute_sql(sql, path)
    current = r.fetchall()
    curr_timestamp = current[0][0]
    curr_avg = round(current[0][1])
    curr_median = round(current[0][2])
    return curr_avg, curr_median, curr_timestamp

def process_list(list_of_strings):
    numbers = []
    for elt in list_of_strings:
        try:
            numbers.append(int(elt[1:].split(" ")[0]))
        except:
            try:
                numbers.append(int(elt[1:].split(" ")[0].split(",")[0]+elt[1:].split(" ")[0].split(",")[1]))
            except:
                pass
    
    return numbers

def crawl(num_pages=100):
    from autoscraper import AutoScraper
    import re
    wanted_list = [re.compile('€.*per month')]

    url_blueprint = 'https://www.daft.ie/property-for-rent/dublin-city/apartments?numBeds_to=2&sort=publishDateDesc&from={}&pageSize=20'
    url_list = list(map(lambda i: url_blueprint.format(i),range(0,num_pages*20,20)))

    result = []
    scraper = AutoScraper()

    def speedy(url):
        return scraper.build(url, wanted_list)

    import concurrent.futures
    MAX_THREADS = 30
    threads = min(MAX_THREADS, len(url_list))
        
    with concurrent.futures.ThreadPoolExecutor(max_workers=threads) as executor:
        results = executor.map(speedy, url_list)

    result = [item for sublist in results for item in sublist]

    return result

