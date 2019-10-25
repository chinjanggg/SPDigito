#!/usr/bin/env python
# coding: utf-8

import cv2
import numpy as np
import os, sys
from keras.models import model_from_json
import pandas as pd
from statistics import mode

from align_image import align_image
from find_monitor import find_monitor
from find_digit import find_digit
from get_place import get_place
from save_output import save_data

def main(argv):
    global sys
    final_res = {
        "sys": 0,
        "dia": 0,
        "pulse": 0
    }

    #Set up image
    img_file = argv
    img_name = img_file.split(".")[0]
    img_type = ".png"
    image_path = "./uploads/"
    mon_save_path = image_path+"monitor/"
    digit_save_path = image_path+"digits/"
    model_path = "../../model"

    #Load Image
    try:
        load_image = cv2.imread(image_path+img_file)
        if(load_image is None):
            sys.exit("Image is not loaded properly.")
    except:
        sys.exit("Image is not valid.")


    #Image alignment
    
    #Align Image
    ref_file = img_file
    ref_image = cv2.imread(image_path+ref_file)
    align_img = align_image(load_image, ref_image)


    #Find monitor
    
    #Set parameter
    #size[0], size[1] = too small w&h
    #size[2], size[3] = too large w&h
    require_width = 550
    lim_monitor_size = [125,125,250,300]

    blur_type = {
        1: None,
        2: "bblur",
        3: "gblur",
        4: "mblur",
        5: "nblur"
    }
    blur_opt = 2
    color_denoise = False
    gray_denoise = True

    #Find monitor
    print("Finding monitor(s)...")
    cnts, d_cnts, contour_img, threshold_images, erode_images = \
    find_monitor(align_img, blur_type[blur_opt], color_denoise, gray_denoise, require_width, lim_monitor_size)


    #Find digits

    #Compile all processed images to see which one will give the best result in finding digits
    monitor_images = threshold_images+erode_images

    #Create a list for collecting digits from each image
    digits = []
    group_cnts = []
    used_mon_image = []
    sort_cnts = []

    #Find digits
    for i, image in enumerate(monitor_images):
        #First parameter is contours
        if(find_digit(image, gray_denoise) is None):
            #Can't find all 3 rows
            continue
        else:
            _, _, group_con, cnt_digit, digit_image = find_digit(image, gray_denoise)
            group_cnts.append(group_con)
            digits.append(digit_image)
            used_mon_image.append(image)

            #cv2.imwrite(digit_save_path+img_name+" ndc"+str(i+1)+img_type, cnt_digit)

    #Print number of result        
    print("Get %d result(s)." %(len(digits)))
    
    #In case there is no image with 3 rows
    #Stop the program
    if(not digits):
        return print(final_res)

    
    #Find digit place

    #Because if we can't find all digits in each row, then find place for each digit
    #getPlace will return an array of 3x3 0/1s, while 0 means there is no digit in that place
    #If there is any problem with that row, it won't be used to calculate
    #If none of row could be used, that image SHOULD be deleted from the list
    digit_place = []

    #Start from the last item for not getting any problem with pop
    for i in range(len(group_cnts)-1, -1, -1):
        #print("Image", i+1)
        mon_width = used_mon_image[i].shape[1]
        place = get_place(group_cnts[i], mon_width)
        if((place == 0).all()):
            digits.pop(i)
            used_mon_image.pop(i)
        else:
            digit_place.insert(0, place)
    print("Total %d image(s) can be used for OCR." %(len(digits)))

    #In case there is no image left
    #Stop the program
    if(not digits):
        return print(final_res)
        
        
    #Select row of digit to be used
    
    #Select image to use with the model
    precon_digit = []
    precon = []
    for i, digit in enumerate(digits):
        #Check if row could be used
        for n, row in enumerate(digit_place[i]):
            if(not (row==0).all()):
                for image in digit[n]:
                    precon.append(image)
        precon_digit.append(precon)
        precon = []

        
    #Reshape image
    
    #Concat and reshape digit before using the model
    test_digits = []
    for i, digit in enumerate(precon_digit):
        no_of_digit = len(digit)
        #Concat vertically
        con_digit = digit[0].copy()
        for n in range(1, no_of_digit):
            con_digit = np.concatenate((con_digit, digit[n]), axis = 0)
        #Reshape
        cv2.imwrite(digit_save_path+img_name+" concat"+str(i+1)+img_type, con_digit)
        con_digit = con_digit.reshape(no_of_digit, 28*28)
        test_digit = np.array(con_digit, dtype = np.float32)
        test_digits.append(test_digit)


    #Load Model

    #Use OCR model to recognize digits and compare with each other whether the result is correct
    json_model = "model.json"
    weight = "model.h5"

    #Load model
    print("Loading model...")
    json_file = open(os.path.join(model_path, json_model), 'r')
    loaded_model_json = json_file.read()
    json_file.close()
    ocr_model = model_from_json(loaded_model_json)

    #Load weights into new model
    ocr_model.load_weights(os.path.join(model_path, weight))

    print("Loaded model from disk")


    #OCR

    #Use OCR model to predict digit value
    ocr_digits = []

    for i, test in enumerate(test_digits):
        no_of_digit = len(precon_digit[i])
        res = pd.DataFrame(ocr_model.predict(test,batch_size=no_of_digit))
        res = pd.DataFrame(res.idxmax(axis = 1))
        ocr_digits.append(list(res[0]))
        
    
    #Assign digit value
    
    #Flatten place for easier use
    flatten_place = []
    for i, d_place in enumerate(digit_place):
        f_place = d_place.flatten()
        flatten_place.append(f_place)
        
    #Assign Value
    digit_val = []
    for i, place in enumerate(flatten_place):
        d_val = place.copy()
        ocr_val = ocr_digits[i].copy()
        for n in range(len(d_val)):
            if(d_val[n] == 1):
                val = ocr_val.pop(0)
                #Digit at hundreds' place must not exceed 3, if it is, might be error, don't use
                if((n%3 ==0) and (val > 3)):
                    d_val[n] = -1
                else:
                    d_val[n] = d_val[n] * val
            else:
                d_val[n] = -1
        #print(d_val.reshape(3,3))

        if(ocr_val):
            #If there is some digit value left, which it shouldn't, something wrong
            print("Something wrong.")
        else:
            digit_val.append(d_val.reshape(3,3))


    #Compare result

    #Compare result and select value with the most frequency (mode)
    result = np.zeros((3,3), dtype = np.int32)
    row_res = np.zeros(3, dtype = np.int32)
    #No need to compare if there is only one result set
    if(len(digit_val) == 1):
        result = digit_val[0]
    else:
        #Compare 1 row a time in case there is cannot-be-used row
        for i in range(3):
            cmpr_row = [d_val[i] for d_val in digit_val if(not (d_val[i]==-1).all())]
            for j in range(3):
                cmpr_val = [val[j] for val in cmpr_row if(val[j] != -1)]
                #Maybe there is no digit in hundreds' or tens' place, set it to 0
                if(not cmpr_val):
                    cmpr_val = [0]
                print("Digit %d is" %((i*3)+j+1), cmpr_val)

                #Find mode
                try:
                    row_res[j] = mode(cmpr_val)
                except:
                    row_res[j] = 0

            #Set row result
            result[i] = row_res

    print("Result:\n", result)


    #Save output

    #Extract SYS DIA and PULSE and save
    #Convert int to str
    result = result.astype(str)
    key = {0:"sys",
           1:"dia",
           2:"pulse"}
    
    
    for i, row in enumerate(result):
        final_res[key[i]] = int(row[0]+row[1]+row[2])

    return print(final_res)

if __name__ == "__main__":
    if(len(sys.argv) > 1):
        main(str(sys.argv[1]))
    # else:
    #     sys.exit("Please specify image.")