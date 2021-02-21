#check most recent
import sqlite3
from crawl import get_latest_stats

path = '/home/pdywilson/db/rent.db'
website_path = '/home/pdywilson/rentmanhost/public/index.html'

#path = '/Users/pdywilson/my/projects/rentcrawler/src/rent.db'
#print("The current rent average is €{}, the median is €{} per month. - {} ({} properties)".format(curr_avg,curr_median,curr_timestamp, curr_properties))

avg_1, median_1, timestamp_1, properties_1 = get_latest_stats(path = path, table = "dublinrents_1rooms")
avg_2, median_2, timestamp_2, properties_2 = get_latest_stats(path = path, table = "dublinrents_2rooms")
avg_3, median_3, timestamp_3, properties_3 = get_latest_stats(path = path, table = "dublinrents_3rooms")
avg_4, median_4, timestamp_4, properties_4 = get_latest_stats(path = path, table = "dublinrents_4rooms")
website = """<!DOCTYPE html>
<html>

<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Dublin Rents</title>
</head>

<body>"""+\
    """<div>
      <h2>Average Monthly Rent in Dublin</h2>
      <h1>Rents are on 🔥</h1>
      </div>
      <div>
      <h2>{}-bedroom</h2>
      <p>The average rent is: €{}</p>
      <p>The median rent is: €{}</p>
      <p>(Based on {} {}-bedroom Dublin-City properties)</p>
      </div>
    """.format(1, avg_1, median_1, properties_1, 1)+\
    """<div>
      <h2>{}-bedroom</h2>
      <p>The average rent is: €{}</p>
      <p>The median rent is: €{}</p>
      <p>(Based on {} {}-bedroom Dublin-City properties)</p>
      </div>
    """.format(2, avg_2, median_2, properties_2, 2)+\
    """<div>
      <h2>{}-bedroom</h2>
      <p>The average rent is: €{}</p>
      <p>The median rent is: €{}</p>
      <p>(Based on {} {}-bedroom Dublin-City properties)</p>
      </div>
    """.format(3, avg_3, median_3, properties_3, 3)+\
    """<div>
      <h2>{}-bedroom</h2>
      <p>The average rent is: €{}</p>
      <p>The median rent is: €{}</p>
      <p>(Based on {} {}-bedroom Dublin-City properties)</p>
      <p>Last updated: {}</p>
      </div>
    """.format("4+", avg_4, median_4, properties_4, "4+", timestamp_4)+\
"""
</body>
</html>"""

f = open(website_path, "w")
f.write(website)
f.close()