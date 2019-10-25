#!/usr/bin/env python
# coding: utf-8

import cv2
import numpy as np

FEATURES = 5000
MATCH_PERCENT = 1.00

def align_image(aligned, ref):
    img1 = cv2.cvtColor(aligned, cv2.COLOR_BGR2GRAY)
    img2 = cv2.cvtColor(ref, cv2.COLOR_BGR2GRAY)
    
    orb_detector = cv2.ORB_create(FEATURES)
    
    #Find keypoints and descriptors
    keypoint1, descriptor1 = orb_detector.detectAndCompute(img1, None)
    keypoint2, descriptor2 = orb_detector.detectAndCompute(img2, None)
    
    matcher = cv2.BFMatcher(cv2. NORM_HAMMING, crossCheck = True)
    matches = matcher.match(descriptor1, descriptor2)
    
    matches.sort(key = lambda x: x.distance)
    
    #Remove not good matches - 10%
    numGoodMatch = int(len(matches) * MATCH_PERCENT)
    matches = matches[:numGoodMatch]
    
    point1 = np.zeros((len(matches), 2))
    point2 = np.zeros((len(matches), 2))
    
    for i, match in enumerate(matches):
        point1[i, :] = keypoint1[match.queryIdx].pt
        point2[i, :] = keypoint2[match.trainIdx].pt
        
    #Find homography
    homography, mask = cv2.findHomography(point1, point2, cv2.RANSAC)
    
    height, width = img2.shape
    
    result_image = cv2.warpPerspective(aligned, homography, (width, height), borderMode=cv2.BORDER_REPLICATE)
    
    return result_image