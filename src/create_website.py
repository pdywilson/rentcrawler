#check most recent
import sqlite3
from crawl import get_latest_stats

path = '/home/pdywilson/db/rent.db'
website_path = '/home/pdywilson/rentmanhost/public/index.html'

curr_avg, curr_median,curr_timestamp,curr_properties = get_latest_stats(path = path, table = "dublinrents_2rooms")
print("The current rent average is €{}, the median is €{} per month. - {} ({} properties)".format(curr_avg,curr_median,curr_timestamp, curr_properties))

website = """<!DOCTYPE html>
<html>

<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Dublin Rents</title>
</head>

<body>
  <div id="message">
    <h2>Average Monthly Rent in Dublin</h2>
    <h1>Rents are on 🔥</h1>
    <p>The average rent in Dublin is: €{}</p>
    <p>The median rent in Dublin is: €{}</p>
    <p>Last updated: {}</p>
    <p>(Based on {} 2 bedroom Dublin-City properties)</p>
  </div>



</body>


</html>""".format(curr_avg,curr_median,curr_timestamp, curr_properties)

f = open(website_path, "w")
f.write(website)
f.close()