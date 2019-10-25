#!/usr/bin/env python
# coding: utf-8

import cv2
import numpy as np

def isXOverlap(contours, max_overlap):
    digit_num = len(contours)
    if(digit_num == 1):
        #No overlap if there is only 1 digit
        return False
    
    for i in range(digit_num-1):
        (x1,y1,w1,h1) = cv2.boundingRect(contours[i])
        for j in range(i+1, digit_num):
            (x2,y2,w2,h2) = cv2.boundingRect(contours[j])
            if(x1+w1-x2 > max_overlap):
                #If 1st digit's right is overlap with 2nd digit's left more than max overlap allowed
                #Then we define them as overlap
                return True
    return False

def isOnes(digit, left, right):
    (x,y,w,h) = cv2.boundingRect(digit)
    if((x > left) and (x+w >= right)):
        return True
    else:
        return False

def isTens(digit, tens_right):
    (x,y,w,h) = cv2.boundingRect(digit)
    if(x+w >= tens_right):
        return True
    else:
        return False

def find_place(row_con, ones_right, tens_right):
    row_place = np.zeros(3, dtype = np.int32)
    digit_num = len(row_con)
    
    if(digit_num == 1):
        #If there's only 1 digit, it should be at ones' place, else, don't use this row
        if(isOnes(row_con[0], tens_right, ones_right)):
            row_place[2] = 1
        else:
            #"This only 1 digit is not at ones' place."
            pass
                
    elif(digit_num == 2):
        #Check if the last digit is at ones' place and the first digit is at tens' place?
        #If not, don't use this row
        if(isOnes(row_con[1], tens_right, ones_right)):
            if(isTens(row_con[0], tens_right)):
                row_place[1] = row_place[2] = 1
            else:
                #"First digit is not at tens' place."
                pass
        else:
            #"Second digit is not at ones' place."
            pass
        
    return row_place

def get_place(contours, monitor_width):
    place = np.zeros((3, 3), dtype = np.int32)
    max_overlap = int(monitor_width * 0.10) #Allow 10% of monitor width to be overlap
    furthest_right = int(monitor_width * 3/4)
    middle = int(monitor_width/2)

    for row in contours:
        row_con = contours[row]
        if(row_con):
            #If there is some contour in row
            if(not isXOverlap(row_con, max_overlap)):
                #If there is no overlap contour, then find pos of each digit
                if(len(row_con) == 3):
                    #No need to find position as they are sorted
                    row_place = np.ones(3, dtype = np.int32)
                else:
                    row_place = find_place(row_con, furthest_right, middle)
                place[row] = row_place       

    return place