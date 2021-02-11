#!/usr/bin/env python3
import sqlite3

conn = sqlite3.connect('/home/pdywilson/rentcrawler/db/rent.db')

cur = conn.cursor()

cur.execute("SELECT * FROM sqlite_master WHERE type='table'")

create_table_sql = """ CREATE TABLE IF NOT EXISTS dublinrents (
                                        timestamp string PRIMARY KEY,
                                        avg float NOT NULL,
                                        median float NOT NULL
                                    ); """

if conn is not None:
    try:
        c = conn.cursor()
        c.execute(create_table_sql)
    except Error as e:
        print(e)

else:
    print("Error! cannot create the database connection.")

from autoscraper import AutoScraper
import re

wanted_list = [re.compile('€.*per month')]

url_blueprint = 'https://www.daft.ie/property-for-rent/dublin-city/apartments?numBeds_to=2&sort=publishDateDesc&from={}&pageSize=20'
url_list = list(map(lambda i: url_blueprint.format(i),range(0,2000,20)))

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
#print(result)
print("Scraped {} rents.".format(len(result)))

numbers = []
for elt in result:
    try:
        numbers.append(int(elt[1:].split(" ")[0]))
    except:
        try:
            numbers.append(int(elt[1:].split(" ")[0].split(",")[0]+elt[1:].split(" ")[0].split(",")[1]))
        except:
            pass
print("Processed {} rents.".format(len(numbers)))

avg = sum(numbers)/len(numbers)
median = sorted(numbers)[round(len(numbers)/2)]

from datetime import datetime
now = datetime.now()
dt_string = now.strftime("%d/%m/%Y %H:%M:%S")
print("Datestamp now: {}".format(dt_string))

#import average mean median etc to sql
def insert_to_db(dt_string, avg, median):
    sql = ''' INSERT INTO dublinrents
                VALUES(?,?,?) '''
    cur = conn.cursor()
    cur.execute(sql, (dt_string,avg,median))
    conn.commit()

insert_to_db(dt_string,avg,median)
print("updated db")


sql = ''' SELECT * FROM dublinrents ORDER BY timestamp DESC LIMIT 1'''
cur = conn.cursor()
r = cur.execute(sql)
current = r.fetchall()
curr_timestamp = current[0][0]
curr_avg = round(current[0][1])
curr_median = round(current[0][2])

print("The current rent average is €{}, the median is €{} per month. - {}".format(curr_avg,curr_median,curr_timestamp))