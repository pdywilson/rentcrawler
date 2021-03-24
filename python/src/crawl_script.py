#!/usr/bin/env python3
import sqlite3
from crawl import *

path = "/home/pdywilson/db/rent.db"
num_pages = 100

#testing:
#path = "rent.db"
#num_pages = 1


table_urls = {
    "dublinrents":'', 
    "dublinrents_1rooms":'', 
    "dublinrents_2rooms":'', 
    "dublinrents_3rooms":'', 
    "dublinrents_4rooms":''
    }



# create db
for table in table_urls:
    sql = """ CREATE TABLE IF NOT EXISTS {} (
                                timestamp datetime PRIMARY KEY,
                                avg float,
                                median float,
                                number_of_properties float
                            ); """.format(table)
    execute_sql(sql,path)

for table in table_urls:
    result = crawl(url_blueprint = table_urls[table], num_pages = num_pages, path=path)
    print("Scraped {} rents.".format(len(result)))

    numbers = process_list(result)

    from statistics import mean, median
    avg = mean(numbers)
    median = median(numbers)
    properties = len(numbers)

    from datetime import datetime
    dt = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    print("Datestamp now: {}".format(dt))

    sql = "INSERT INTO {} VALUES(?,?,?,?)".format(table)
    execute_sql(sql, path, dt, avg, median, properties)

    curr_avg, curr_median,curr_timestamp,curr_properties = get_latest_stats(path, table=table)
    print("The current rent average is €{}, the median is €{} per month. - {} ({} properties)".format(curr_avg,curr_median,curr_timestamp,curr_properties))


