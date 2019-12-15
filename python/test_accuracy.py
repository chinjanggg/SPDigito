#!/usr/bin/env python
# coding: utf-8

import numpy as np

def test_accuracy(no_of_res, expect_val, test):
    accuracy = 0
    
    if(no_of_res > 0):
        no_of_val = len(expect_val.flatten())
        for i, e_row in enumerate(expect_val):
            cmpr_row = [t_val[i] for t_val in test if(not (t_val[i]==-1).all())]
            for j, e_val in enumerate(e_row):
                cmpr_val = [val[j] for val in cmpr_row]
                accuracy = accuracy + cmpr_val.count(e_val)
            
        #Make it percent
        accuracy = (accuracy / (no_of_val * no_of_res)) * 100
        
    return accuracy