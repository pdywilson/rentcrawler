#!/usr/bin/env python3
import sqlite3
from crawl import *

path = "rent.db"

# create db
sql = """ CREATE TABLE IF NOT EXISTS dublinrents (
                            timestamp string PRIMARY KEY,
                            avg float NOT NULL,
                            median float NOT NULL
                        ); """
execute_sql(sql,path)

result = crawl()
print("Scraped {} rents.".format(len(result)))

numbers = process_list(result)

from statistics import mean, median
avg = mean(numbers)
median = median(numbers)

from datetime import datetime
now = datetime.now()
dt_string = now.strftime("%d/%m/%Y %H:%M:%S")
print("Datestamp now: {}".format(dt_string))

sql = "INSERT INTO dublinrents VALUES(?,?,?)"
execute_sql(sql, path, dt_string, avg, median)

curr_avg, curr_median,curr_timestamp = get_latest_stats(path)
print("The current rent average is €{}, the median is €{} per month. - {}".format(curr_avg,curr_median,curr_timestamp))


