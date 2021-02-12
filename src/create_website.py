#check most recent
import sqlite3
def get_latest_stats()
    conn = sqlite3.connect('/var/db/rentcrawler/rent.db')
    cur = conn.cursor()
    sql = ''' SELECT * FROM dublinrents ORDER BY timestamp DESC LIMIT 1'''
    r = cur.execute(sql)
    current = r.fetchall()
    curr_timestamp = current[0][0]
    curr_avg = round(current[0][1])
    curr_median = round(current[0][2])
    return curr_avg, curr_median, curr_timestamp

curr_avg, curr_median,curr_timestamp = get_latest_stats()
print("The current rent average is €{}, the median is €{} per month. - {}".format(curr_avg,curr_median,curr_timestamp))

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
  </div>



</body>


</html>""".format(curr_avg,curr_median,curr_timestamp)

f = open("/var/rentman/public/index.html", "w")
f.write(website)
f.close()