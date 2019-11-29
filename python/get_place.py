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
    #print(left, x, x+w, right)
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
        #Find whether it is at ones', tens', or hundreds' place
        if(isOnes(row_con[0], tens_right, ones_right)):
            row_place[2] = 1
        elif(isTens(row_con[0], tens_right)):
            row_place[1] = 1
        else:
            row_place[0] = 1
                
    elif(digit_num == 2):
        if(isOnes(row_con[1], tens_right, ones_right)):
            #Last digit is at ones' place, then find first digit is at tens' or hundreds'
            if(isTens(row_con[0], tens_right)):
                #First digit is at tens' place
                row_place[1] = row_place[2] = 1
            else:
                #First digit is not at tens' place, so it must be at hundreds' place
                row_place[0] = row_place[2] = 1
        else:
            #Last digit is at tens' place, so first digit must be at hundreds' place
            row_place[0] = row_place[1] = 1
        
    return row_place

def get_place(contours, monitor_width):
    place = np.zeros((3, 3), dtype = np.int32)
    max_overlap = int(monitor_width * 0.10) #Allow 10% of monitor width to be overlap
    furthest_right = int(monitor_width * 3/4)
    middle = int(monitor_width/2)
    #print(monitor_width, middle, furthest_right)

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