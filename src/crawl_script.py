#!/usr/bin/env python3
import sqlite3
from crawl import *

path = "/home/pdywilson/db/rent.db"

#testing:
#path = "rent.db"
#num_pages = 1


table_urls = {
    "dublinrents_1rooms":'https://www.daft.ie/property-for-rent/dublin-city?numBeds_to=1&numBeds_from=1&sort=publishDateDesc&from={}&pageSize=20', 
    "dublinrents_2rooms":'https://www.daft.ie/property-for-rent/dublin-city?numBeds_to=2&numBeds_from=2&sort=publishDateDesc&from={}&pageSize=20', 
    "dublinrents_3rooms":'https://www.daft.ie/property-for-rent/dublin-city?numBeds_to=3&numBeds_from=3&sort=publishDateDesc&from={}&pageSize=20', 
    "dublinrents_4rooms":'https://www.daft.ie/property-for-rent/dublin-city?numBeds_from=4&sort=publishDateDesc&from={}&pageSize=20'
    }



# create db
for table in table_urls:
    sql = """ CREATE TABLE IF NOT EXISTS {} (
                                timestamp string PRIMARY KEY,
                                avg float NOT NULL,
                                median float NOT NULL,
                                number_of_properties float NOT NULL
                            ); """.format(table)
    execute_sql(sql,path)

for table in table_urls:
    result = crawl(num_pages = num_pages, path=path, url_blueprint = table_urls[table])
    print("Scraped {} rents.".format(len(result)))

    numbers = process_list(result)

    from statistics import mean, median
    avg = mean(numbers)
    median = median(numbers)
    properties = len(numbers)

    from datetime import datetime
    now = datetime.now()
    dt_string = now.strftime("%d/%m/%Y %H:%M:%S")
    print("Datestamp now: {}".format(dt_string))

    sql = "INSERT INTO {} VALUES(?,?,?,?)".format(table)
    execute_sql(sql, path, dt_string, avg, median, properties)

    curr_avg, curr_median,curr_timestamp,curr_properties = get_latest_stats(path, table=table)
    print("The current rent average is €{}, the median is €{} per month. - {} ({} properties)".format(curr_avg,curr_median,curr_timestamp,curr_properties))


