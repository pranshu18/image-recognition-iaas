package com.cse546.project1;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
public class AppController {
    final static SqsUtil SQS = new SqsUtil();

    @PostMapping("/")
    public ResponseEntity<String> uploadImage(@RequestPart("myfile") MultipartFile imageData){
        byte[] imageBytes;
        try {
            imageBytes = imageData.getBytes();
        } catch (IOException e) {
            return new ResponseEntity<>("Error reading image!", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        String fileName = imageData.getOriginalFilename();
        SQS.sendImage(fileName, imageBytes);
        String result = SQS.readResult(fileName);
        if(result == null){
            return new ResponseEntity<>("Result not found!", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/test")
    public String testMessage(@RequestPart("myfile") MultipartFile imageData){
        return "Test: " + imageData.getOriginalFilename();
    }
}
