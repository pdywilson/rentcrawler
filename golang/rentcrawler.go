package main

import (
    "fmt"
    "io"
    "log"
    "net/http"
    "regexp"
    "strconv"
    "strings"
    "sort"
)

func main() {
	matches := fetchMatches("2")
	values := extractValues(matches)
	printStatistics(values)
}

func fetchMatches(beds string) [][]string {
    re := regexp.MustCompile(`€(\d{1,3}(,\d{3})*(\.\d+)?)( per month)`)
    var matches [][]string

    client := &http.Client{}
    for i := 0; i < 10; i++ {
        url := fmt.Sprintf("https://www.daft.ie/property-for-rent/dublin/apartments?numBeds_to=%s&numBeds_from=%s&page=%d", beds, beds, i+1)
		fmt.Println(url)
        req, err := http.NewRequest("GET", url, nil)
        if err != nil {
            log.Fatal(err)
        }
        req.Header.Set("User-Agent", "Mozilla/5.0")
        resp, err := client.Do(req)
        if err != nil {
            log.Fatal(err)
        }
        body, err := io.ReadAll(resp.Body)
        resp.Body.Close()
        if err != nil {
            fmt.Println("Read Error :-S", err)
            return nil
        }
        match := re.FindAllStringSubmatch(string(body), -1)
        matches = append(matches, match...)
    }

    return matches
}

func extractValues(matches [][]string) []int {
    var values []int
    for _, match := range matches {
        valueStr := strings.ReplaceAll(match[1], ",", "")
        value, err := strconv.Atoi(valueStr)
        if err != nil {
            fmt.Printf("Error converting string to ing: %v\n", err)
            continue
        }
        values = append(values, value)
    }
    return values
}

func printStatistics(values []int) {
    if len(values) == 0 {
        fmt.Println("No data found")
        return
    }

    fmt.Println(values)

    var sum int
    for _, value := range values {
        sum += value
    }
    average := float64(sum) / float64(len(values))

    fmt.Printf("Average: %.2f\n", average)

    sort.Ints(values)
    mid := len(values) / 2
    var median float64
    if len(values)%2 == 0 {
        median = float64(values[mid-1]+values[mid]) / 2
    } else {
        median = float64(values[mid])
    }

    fmt.Printf("Median: %.2f\n", median)
}
