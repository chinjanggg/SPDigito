#!/usr/bin/env python
# coding: utf-8

import cv2
import numpy as np
import math
from sklearn.cluster import AgglomerativeClustering
from find_monitor import dist_contour

#Sort contour by (x,y) coordinate
def cluster_row(contours):
    group_list = []
    grouping = {}
    cnt_y = []
    
    contours = sorted(contours, key=lambda ctr: cv2.boundingRect(ctr)[1])
    
    #Collect only y-axis for clustering
    sum_h = 0
    for contour in contours:
        (x,y,w,h) = cv2.boundingRect(contour)
        cnt_y.append([y])
        sum_h = sum_h+h
    Y = np.asarray(cnt_y)
    avg_h = int(sum_h / (len(contours)))
    clustering = AgglomerativeClustering(n_clusters=None, distance_threshold=avg_h).fit(Y)
    
    n = clustering.labels_[0]
    group = []
    group.append(contours[0])
    for i in range(1, len(clustering.labels_)):
        if(n != clustering.labels_[i]):
            group_list.append([n, group])
            n = clustering.labels_[i]
            group = []
        group.append(contours[i])
    group_list.append([n, group])

    for i in range(len(group_list)):
        group_list[i][1] = sorted(group_list[i][1], key=lambda ctr: cv2.boundingRect(ctr)[0])
        grouping[i] = group_list[i][1]
            
    return grouping

#Make image to be square by adding border around
#arg[size] = full size of image eg. input size as 28, result image will be 28x28 px
def add_border(image, size, border):
    img_w = image.shape[1]
    img_h = image.shape[0]
    border = border
    BLACK = [0, 0, 0]
    new_size = size - (border * 2)

    #square image
    if (img_w == img_h):
        #set image size
        new_w = new_h = new_size
        
        #set image border
        top = bottom = left = right = border

    else:
        if(img_h > img_w):
            #set image size
            new_h = new_size
            new_w = math.ceil(new_size * image.shape[1] / image.shape[0])

            #set image border
            top = bottom = border
            left_px = new_size - new_w
            if(left_px % 2 == 0):
                left_px = int(left_px / 2)
                left = right = border + left_px
            else:
                left_px = int(left_px / 2)
                left = border + left_px
                right = left + 1

        elif(img_w > img_h):
            #set image size
            new_w = new_size
            new_h = math.ceil(new_size * image.shape[0] / image.shape[1])

            #set image border
            left = right = border
            left_px = new_size - new_h
            if(left_px % 2 == 0):
                left_px = int(left_px / 2)
                top = bottom = border + left_px
            else:
                left_px = int(left_px / 2)
                top = border + left_px
                bottom = top + 1

    dim = (new_w, new_h)
    res_image = cv2.resize(image, dim)

    border_img = cv2.copyMakeBorder(res_image, top, bottom, left, right, cv2.BORDER_CONSTANT, value=BLACK)
    
    return border_img

def find_digit(img, denoise_opt):
    new_mon_h = 250
    digit_limit_size = [5,40,70,100]
    image_size = 28
    border = 4
    
    #Grayscale
    gray_image = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    
    #Denoise
    denoise = cv2.fastNlMeansDenoising(gray_image, None, 10, 7, 21)
    dn_image = {
        True: denoise,
        False: gray_image
    }
    
    #Resize image to 250px-height
    new_mon_w = int(new_mon_h * img.shape[1] / img.shape[0])
    new_mon_dim = (new_mon_w, new_mon_h)
    image = cv2.resize(dn_image[denoise_opt], new_mon_dim)
    
    #For draw contour
    image2 = cv2.cvtColor(image, cv2.COLOR_GRAY2RGB)
    
    #Make canny edge for contour
    canny_edged = cv2.Canny(image, 30, 200)
    
    #Find Contour from canny edges img
    contours, _ = cv2.findContours(canny_edged, cv2.RETR_TREE, cv2.CHAIN_APPROX_NONE)
    
    #Get distinct contour
    d_contours = dist_contour(contours, digit_limit_size)
    
    #Sort and group contour into row
    #There must be at least 3 contours to get 3 rows
    if(len(d_contours) >= 3):
        g_cnts = cluster_row(d_contours)
    
        #Find digit
        group_con = g_cnts.copy()
        group_digit = {}
        row_digit = []

        #Check if could find 3 rows (of sys/dia/pulse)
        if(len(group_con) == 3):
            for key in group_con:
                #Check if each row doesn't contain more than 3 digits
                if(len(group_con.get(key)) <= 3):
                    for contour in group_con.get(key):
                        (x,y,w,h) = cv2.boundingRect(contour)
                        crop_image = image[y:y+h, x:x+w]

                        #Inverse image
                        bw_image = cv2.bitwise_not(crop_image)

                        #Create border
                        border_image = add_border(bw_image, image_size, border)

                        #Add digit to row
                        row_digit.append(border_image)

                        #Draw contour
                        cv2.rectangle(image2, (x,y), (x+w,y+h), (255,0,255), 2)
                    
                else:
                    #If get more than 3 digits, remove problem row from contour
                    group_con[key] = []
                
                #Add row
                group_digit[key] = row_digit
                row_digit = []

            #return all contours, group contours, group contour without problem row(s), contour image, and group digits
            return contours, g_cnts, group_con, image2, group_digit