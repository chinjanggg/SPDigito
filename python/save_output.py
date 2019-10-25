#!/usr/bin/env python
# coding: utf-8

import sys
import json
import datetime

json_file = './data/res-data.json'

def save_data(result):
    save_res = {}
    res = result.copy()

    #Add patient ID
    save_res['pid'] = res['pid']
    res.pop('pid')

    #Add datetime
    save_res['datetime'] = str(datetime.datetime.now())

    #Add sys info
    #Convert str to int
    for key in res:
        save_res[key] = int(res[key])

    #Load file
    with open(json_file) as f:
        data = json.load(f)
    
    #Append data
    data['result'].append(save_res)
    
    #Write to json file
    with open(json_file, 'w') as f:
        json.dump(data, f)
    
    print("save pid:%s datetime:%s sys:%d dia:%d pulse:%d" \
    %(save_res['pid'], save_res['datetime'], save_res['sys'], save_res['dia'], save_res['pulse']))

if __name__ == "__main__":
    if(len(sys.argv) > 1):
        save_data(json.loads(sys.argv[1]))