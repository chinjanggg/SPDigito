#!/usr/bin/env python
# coding: utf-8

import cv2
import numpy as np

def dist_contour(contours, size):
    #size[0], size[1] = too small w&h
    #size[2], size[3] = too large w&h
    
    #Get distinct contour only into set
    dist_con = []
    checkXY = set()

    for contour in contours:
        #Get bounding rect of contour
        (x,y,w,h) = cv2.boundingRect(contour)
        if (x,y,w,h) not in checkXY:
            checkXY.add((x,y,w,h))
            #print([x,y,w,h])

            #Discard too small area
            if (w<size[0] or h<size[1]):
                continue

            #Discard too large area
            if (w>size[2] or h>size[3]):
                continue

            #Else add to set
            dist_con.append(contour)
    
    return dist_con

def find_monitor(img, blur_opt, color_denoise_opt, gray_denoise_opt, require_width, limit_monitor_size):
    
    #Initialize parameters
    new_w = require_width
    lim_mon_size = limit_monitor_size
    threshold_images = []
    erode_images = []
    
    #Convert image to jpg
    jpg_enc = cv2.imencode(".jpg", img)
    enc_arr = np.array(jpg_enc[1], np.uint8)
    jpg_img = cv2.imdecode(enc_arr, cv2.IMREAD_COLOR)
    
    #Resize image to 550px-width
    new_h = int(new_w * img.shape[0] / img.shape[1])
    dim = (new_w, new_h)
    res_img = cv2.resize(jpg_img, dim)   
    
    #Blur image
    nblur = cv2.blur(res_img,(5,5))
    gblur = cv2.GaussianBlur(res_img,(5,5),0)
    mblur = cv2.medianBlur(res_img,5)
    bblur = cv2.bilateralFilter(res_img,9,75,75)
    
    blur = {
        None: res_img,
        "nblur": nblur,
        "gblur": gblur,
        "mblur": mblur,
        "bblur": bblur
    }
    
    #Denoise colored
    color_denoise = cv2.fastNlMeansDenoisingColored(blur[blur_opt], None, 10, 10, 7, 21)
    dn_color = {
        True: color_denoise,
        False: blur[blur_opt]
    }
    
    image = dn_color[color_denoise_opt]
    
    #Image for crop
    image2 = image.copy()

    #Pre-processing image
    #Grayscale
    gray_image = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    
    #Denoise grayscale
    gray_denoise = cv2.fastNlMeansDenoising(gray_image, None, 10, 7, 21)
    dn_gray = {
        True: gray_denoise,
        False: gray_image
    }
    #Canny Edges
    canny_edged = cv2.Canny(dn_gray[gray_denoise_opt], 30, 200)
    
    #HSV
    hsv = cv2.cvtColor(image, cv2.COLOR_BGR2HSV)
    lower_blue = np.array([38, 86, 0])
    upper_blue = np.array([121, 255, 255])
    mask = cv2.inRange(hsv, lower_blue, upper_blue)
    
    #Find Contour from canny edge and hsv
    canny_cnt, _ = cv2.findContours(canny_edged, cv2.RETR_TREE, cv2.CHAIN_APPROX_NONE)    
    hsv_cnt, _ = cv2.findContours(mask, cv2.RETR_TREE, cv2.CHAIN_APPROX_NONE)
    contours = canny_cnt + hsv_cnt
    
    #Get distinct contour
    d_contours = dist_contour(contours, lim_mon_size)   
    
    #Find Monitor
    for i, contour in enumerate(d_contours):
        (x,y,w,h) = cv2.boundingRect(contour)
        
        crop_image = image2[y:y+h, x:x+w]

        #Gaussian Theshold cropped image
        crop_image = cv2.medianBlur(crop_image, 5)
        _, th_image = cv2.threshold(crop_image, 127, 255, cv2.THRESH_BINARY)

        #Add threshold image to list
        threshold_images.append(th_image)

        #Erode image
        
        for n in range(2,8):
            kernel = np.ones((n,n), np.uint8)
            erode_image = cv2.erode(th_image, kernel, iterations=1)
            erode_images.append(erode_image)
        
        #print("Monitor image",i+1,x,y,w,h)

        #Draw contour
        cv2.rectangle(image, (x,y), (x+w,y+h), (255,0,255), 2)
    
    #return all contours, distinct contours, contoured img, list of threshold imgs, and list of eroded imgs
    return contours, d_contours, image, threshold_images, erode_images