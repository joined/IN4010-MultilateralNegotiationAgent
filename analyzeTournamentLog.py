#!/usr/bin/env python3
import csv
import sys
from collections import Counter, defaultdict

inputfile = sys.argv[1]

winners = defaultdict(int)
sessions = 0
with open(inputfile, newline='') as csvfile:
    next(csvfile)

    csvreader = csv.reader(csvfile, delimiter=';')

    for row in csvreader:

        result = {
            row[12].split('@')[0]: row[15],
            row[13].split('@')[0]: row[16],
            row[14].split('@')[0]: row[17]
        }

        winner = max(result.keys(), key=(lambda key: result[key]))
        winners[winner] += 1
        sessions += 1

counts = Counter(winners)

print("Total sessions: {}".format(sessions))

for winner, win_count in counts.items():
    print("{} -> {}".format(winner, win_count))
