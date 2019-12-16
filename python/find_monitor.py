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

def find_monitor(img, blur_opt, color_denoise_opt, gray_denoise_opt, require_width, limit_monitor_size, save_info):
    
    #Initialize parameters
    new_w = require_width
    lim_mon_size = limit_monitor_size
    save_path = save_info["path"]
    img_name = save_info["name"]
    img_type = save_info["type"]

    threshold_images = []
    dilate_images = []
    
    #Convert image to jpg
    print("Convert image to jpg encoding")
    jpg_enc = cv2.imencode(".jpg", img)
    enc_arr = np.array(jpg_enc[1], np.uint8)
    jpg_img = cv2.imdecode(enc_arr, cv2.IMREAD_COLOR)
    
    #Resize image to 550px-width
    print("Resize image to 550px-width")
    new_h = int(new_w * img.shape[0] / img.shape[1])
    dim = (new_w, new_h)
    res_img = cv2.resize(jpg_img, dim)
    cv2.imwrite(save_path+img_name+" 02 resize"+img_type, res_img)
    
    #Blur image
    print("Blur image")
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

    cv2.imwrite(save_path+img_name+" 03 "+blur_opt+img_type, blur[blur_opt])
    
    #Denoise colored
    color_denoise = cv2.fastNlMeansDenoisingColored(blur[blur_opt], None, 10, 10, 7, 21)
    dn_color = {
        True: color_denoise,
        False: blur[blur_opt]
    }
    
    image = dn_color[color_denoise_opt]
    #cv2.imwrite(save_path+img_name+" 04 color denoise"+img_type, image)
    
    #Image for crop
    image2 = image.copy()
    #Inverse color
    image2 = cv2.bitwise_not(image2)

    #Pre-processing image
    #Grayscale
    gray_image = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    cv2.imwrite(save_path+img_name+" 05 gray"+img_type, gray_image)
    
    #Denoise grayscale
    print("Denoise image")
    gray_denoise = cv2.fastNlMeansDenoising(gray_image, None, 10, 7, 21)
    dn_gray = {
        True: gray_denoise,
        False: gray_image
    }
    cv2.imwrite(save_path+img_name+" 06 gray_denoise"+img_type, gray_denoise)

    #Canny Edges
    canny_edged = cv2.Canny(dn_gray[gray_denoise_opt], 30, 200)
    cv2.imwrite(save_path+img_name+" 07 canny"+img_type, canny_edged)
    
    #HSV
    hsv = cv2.cvtColor(image, cv2.COLOR_BGR2HSV)
    lower_blue = np.array([85, 100, 20])
    upper_blue = np.array([130, 255, 255])
    mask = cv2.inRange(hsv, lower_blue, upper_blue)
    cv2.imwrite(save_path+img_name+" 08 hsv"+img_type, hsv)
    cv2.imwrite(save_path+img_name+" 08 hsv_mask"+img_type, mask)
    
    #Find Contour from canny edge and hsv
    print("Find monitor")
    canny_cnt, _ = cv2.findContours(canny_edged, cv2.RETR_TREE, cv2.CHAIN_APPROX_NONE)    
    hsv_cnt, _ = cv2.findContours(mask, cv2.RETR_TREE, cv2.CHAIN_APPROX_NONE)
    contours = canny_cnt + hsv_cnt

    #Draw canny contour
    image3 = image.copy()
    canny2 = canny_edged.copy()
    for i, contour in enumerate(canny_cnt):
        (x,y,w,h) = cv2.boundingRect(contour)
        #Draw contour
        cv2.rectangle(image3, (x,y), (x+w,y+h), (255,0,255), 2)
        cv2.rectangle(canny2, (x,y), (x+w,y+h), (255,0,255), 2)
    cv2.imwrite(save_path+img_name+" 09 canny_cnt_color"+img_type, image3)
    cv2.imwrite(save_path+img_name+" 09 canny_cnt"+img_type, canny2)

    #Draw hsv contour
    image4 = image.copy()
    hsv2 = mask.copy()
    for i, contour in enumerate(hsv_cnt):
        (x,y,w,h) = cv2.boundingRect(contour)
        #Draw contour
        cv2.rectangle(image4, (x,y), (x+w,y+h), (255,0,255), 2)
        cv2.rectangle(hsv2, (x,y), (x+w,y+h), (255,0,255), 2)
    cv2.imwrite(save_path+img_name+" 10 hsv_cnt_color"+img_type, image4)
    cv2.imwrite(save_path+img_name+" 10 hsv_cnt"+img_type, hsv2)

    #Draw all contour
    image5 = image.copy()
    for i, contour in enumerate(contours):
        (x,y,w,h) = cv2.boundingRect(contour)
        #Draw contour
        cv2.rectangle(image5, (x,y), (x+w,y+h), (255,0,255), 2)
    cv2.imwrite(save_path+img_name+" 11 all_cnt"+img_type, image5)
    
    #Get distinct contour
    print("Find possible monitor")
    d_contours = dist_contour(contours, lim_mon_size)   
    
    #Find Monitor
    print("Process monitor")
    for i, contour in enumerate(d_contours):
        (x,y,w,h) = cv2.boundingRect(contour)
        
        crop_image = image2[y:y+h, x:x+w]

        #Gaussian Theshold cropped image
        crop_image = cv2.medianBlur(crop_image, 5)
        _, th_image = cv2.threshold(crop_image, 127, 255, cv2.THRESH_BINARY)

        #Add threshold image to list
        threshold_images.append(th_image)
        cv2.imwrite(save_path+img_name+" 13 threshold"+str(i+1)+img_type, th_image)

        #Dilate image
        
        for n in range(2,8):
            kernel = np.ones((n,n), np.uint8)
            dilate_image = cv2.dilate(th_image, kernel, iterations=1)
            dilate_images.append(dilate_image)
            cv2.imwrite(save_path+img_name+" 14 dilate"+str(i+1)+" kernel"+str(n)+img_type, dilate_image)
        
        #print("Monitor image",i+1,x,y,w,h)

        #Draw contour
        cv2.rectangle(image, (x,y), (x+w,y+h), (255,0,255), 2)

    cv2.imwrite(save_path+img_name+" 12 dist_cnt"+img_type, image)
    
    #return all contours, distinct contours, contoured img, list of threshold imgs, and list of dilated imgs
    return contours, d_contours, image, threshold_images, dilate_images