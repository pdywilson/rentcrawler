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
    matches := fetchMatches("1")
    values := extractValues(matches)
    printStatistics(values)

	matches2 := fetchMatches("2")
	values2 := extractValues(matches2)
	printStatistics(values2)
}

func fetchMatches(beds string) [][]string {
    re := regexp.MustCompile(`€(\d{1,3}(,\d{3})*(\.\d+)?)( per month)`)
    var matches [][]string

    for i := 0; i < 10; i++ {
        url := fmt.Sprintf("https://www.daft.ie/property-for-rent/ireland/apartments?numBeds_to=%s&numBeds_from=%s&from=%s&pageSize=20", beds, beds, strconv.Itoa(i*20))
		fmt.Println(url)
        resp, err := http.Get(url)
        if err != nil {
            log.Fatal(err)
        }
        defer resp.Body.Close()
        body, err := io.ReadAll(resp.Body)
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
