package com.project1;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;

public class AppTierRunner {

	private static final String ACCESS_KEY="xxx";
	private static final String SECRET_KEY="yyy";

	public static void main(String[] args) {
		AWSStaticCredentialsProvider credentials = new AWSStaticCredentialsProvider(new BasicAWSCredentials(
				ACCESS_KEY,                
				SECRET_KEY
				));
		

		AmazonSQS amznSqs = AmazonSQSClientBuilder.standard().withRegion(Regions.US_EAST_1).withCredentials(credentials).build();
		
		AmazonS3 amznS3 = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_1).withCredentials(credentials).build();
		
		AmazonEC2 amznEc2 = AmazonEC2ClientBuilder.standard().withRegion(Regions.US_EAST_1).withCredentials(credentials).build();

		AppTier appTier = new AppTier(amznSqs, amznS3, amznEc2);
		appTier.performAppTierTasks();

	}

}
